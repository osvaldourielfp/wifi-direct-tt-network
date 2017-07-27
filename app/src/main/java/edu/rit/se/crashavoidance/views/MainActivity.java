package edu.rit.se.crashavoidance.views;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.apache.commons.lang3.SerializationUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import edu.rit.se.crashavoidance.R;
import edu.rit.se.crashavoidance.db.DBManager;
import edu.rit.se.crashavoidance.db.User;
import edu.rit.se.crashavoidance.db.UserType;
import edu.rit.se.crashavoidance.network.Device;
import edu.rit.se.crashavoidance.network.DeviceType;
import edu.rit.se.crashavoidance.network.Message;
import edu.rit.se.crashavoidance.network.MessageType;
import edu.rit.se.crashavoidance.network.ObjectType;
import edu.rit.se.crashavoidance.network.WifiP2pDeviceStatus;
import edu.rit.se.crashavoidance.tasks.SendMessageTask;
import edu.rit.se.wifibuddy.CommunicationManager;
import edu.rit.se.wifibuddy.DnsSdService;
import edu.rit.se.wifibuddy.DnsSdTxtRecord;
import edu.rit.se.wifibuddy.WifiDirectHandler;

/**
 * The main Activity of the application, which is a container for Fragments and the ActionBar.
 * Contains WifiDirectHandler, which is a service
 * MainActivity has a Communication BroadcastReceiver to handle Intents fired from WifiDirectHandler.
 */
public class MainActivity extends AppCompatActivity implements WiFiDirectHandlerAccessor {

    private WifiDirectHandler wifiDirectHandler;
    private boolean wifiDirectHandlerBound = false;
    private ChatFragment chatFragment = null;
    private LogsDialogFragment logsDialogFragment;
    private MainFragment mainFragment;
    public TextView deviceInfoTextView, deviceTypeTextView;
    private static final String TAG = WifiDirectHandler.TAG + "MainActivity";

    Gson json = new Gson();

    MainActivity activity;

    public Device curDevice;
    public DeviceType deviceType = DeviceType.EMITTER;

    private DnsSdService targetService;

    private static final int ACCESS_LOCATION = 100;
    private long MIN_LOCATION_REQUESTS_TIME = 5000; // IN MS
    private float MIN_LOCATION_REQUESTS_DISTANCE = 1; // IN MTS

    Location curLocation;
    private LocationManager locationManager;
    LocationListener locationListener = new MyLocationListener();

    User user;
    DBManager dbManager;

    /**
     * Sets the UI layout for the Activity.
     * Registers a Communication BroadcastReceiver so the Activity can be notified of
     * intents fired in WifiDirectHandler, like Service Connected and Messaged Received.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Creating MainActivity");
        setContentView(R.layout.activity_main);

        activity = this;

        // Initialize ActionBar
        Toolbar toolbar = (Toolbar) findViewById(R.id.mainToolbar);
        setSupportActionBar(toolbar);

        deviceInfoTextView = (TextView) findViewById(R.id.thisDeviceInfoTextView);

        registerCommunicationReceiver();
        Log.i(TAG, "MainActivity created");

        Intent intent = new Intent(this, WifiDirectHandler.class);
        bindService(intent, wifiServiceConnection, BIND_AUTO_CREATE);

        locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);

        registerLocationUpdates();
        databaseSetup();
    }

    private void databaseSetup() {
        dbManager = new DBManager(activity);
        dbManager.open();
    }

    private void setupInitialWifiP2p() {
        getWifiHandler().cancelConnect();
        getWifiHandler().removeGroup();
    }

    private void registerLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    ACCESS_LOCATION);
            return;
        }
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_LOCATION_REQUESTS_TIME,
                MIN_LOCATION_REQUESTS_DISTANCE, locationListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case ACCESS_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay!
                    registerLocationUpdates();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    /**
     * Set the CommunicationReceiver for receiving intents fired from the WifiDirectHandler
     * Used to update the UI and receive communication messages
     */
    private void registerCommunicationReceiver() {
        CommunicationReceiver communicationReceiver = new CommunicationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiDirectHandler.Action.SERVICE_CONNECTED);
        filter.addAction(WifiDirectHandler.Action.MESSAGE_RECEIVED);
        filter.addAction(WifiDirectHandler.Action.DEVICE_CHANGED);
        filter.addAction(WifiDirectHandler.Action.WIFI_STATE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(communicationReceiver, filter);
        Log.i(TAG, "Communication Receiver registered");
    }

    /**
     * Adds the Main Menu to the ActionBar
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    /**
     * Called when a MenuItem in the Main Menu is selected
     * @param item Item selected
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_view_logs:
                // View Logs MenuItem tapped
                if (logsDialogFragment == null) {
                    logsDialogFragment = new LogsDialogFragment();
                }
                logsDialogFragment.show(getFragmentManager(), "dialog");
                return true;
            case R.id.action_exit:
                // Exit MenuItem tapped
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // TODO: BRETT, add JavaDoc
    // Note: This is used to run WifiDirectHandler as a Service instead of being coupled to an
    //          Activity. This is NOT a connection to a P2P service being broadcast from a device
    private ServiceConnection wifiServiceConnection = new ServiceConnection() {

        /**
         * Called when a connection to the Service has been established, with the IBinder of the
         * communication channel to the Service.
         * @param name The component name of the service that has been connected
         * @param service The IBinder of the Service's communication channel
         */
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "Binding WifiDirectHandler service");
            Log.i(TAG, "ComponentName: " + name);
            Log.i(TAG, "Service: " + service);
            WifiDirectHandler.WifiTesterBinder binder = (WifiDirectHandler.WifiTesterBinder) service;

            wifiDirectHandler = binder.getService();
            wifiDirectHandlerBound = true;
            Log.i(TAG, "WifiDirectHandler service bound");

            // Add MainFragment to the 'fragment_container' when wifiDirectHandler is bound
            mainFragment = new MainFragment();
            replaceFragment(mainFragment);

            deviceInfoTextView.setText(wifiDirectHandler.getThisDeviceInfo());

            //setUser(getWifiHandler().getThisDeviceAddress());
            setupInitialWifiP2p();
        }

        /**
         * Called when a connection to the Service has been lost.  This typically
         * happens when the process hosting the service has crashed or been killed.
         * This does not remove the ServiceConnection itself -- this
         * binding to the service will remain active, and you will receive a call
         * to onServiceConnected when the Service is next running.
         */
        @Override
        public void onServiceDisconnected(ComponentName name) {
            wifiDirectHandlerBound = false;
            Log.i(TAG, "WifiDirectHandler service unbound");
        }
    };

    /**
     * Replaces a Fragment in the 'fragment_container'
     * @param fragment Fragment to add
     */
    public void replaceFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
    }

    /**
     * Returns the wifiDirectHandler
     * @return The wifiDirectHandler
     */
    @Override
    public WifiDirectHandler getWifiHandler() {
        return wifiDirectHandler;
    }

    /**
     * Initiates a P2P connection to a service when a Service ListItem is tapped.
     * An invitation appears on the other device to accept or decline the connection.
     * @param service The service to connect to
     */
    public void onServiceClick(final DnsSdService service) {
        Log.i(TAG, "\nService List item tapped");
        targetService = service;

        if (service.getSrcDevice().status == WifiP2pDevice.CONNECTED) {
            retryTimes = 0;
            Log.d(TAG, "Service connected");
            //TODO: check if processConnectionIn is sufficient
            //processConnectionTo(getRecordProperty(targetService, "DeviceType"));
            /*if (chatFragment == null) {
                chatFragment = new ChatFragment();
            }
            replaceFragment(chatFragment);
            Log.i(TAG, "Switching to Chat fragment");*/
        } else if (service.getSrcDevice().status == WifiP2pDevice.AVAILABLE) {
            String sourceDeviceName = service.getSrcDevice().deviceName;
            if (sourceDeviceName.equals("")) {
                sourceDeviceName = "other device";
            }
            Log.d(TAG, "Inviting " + sourceDeviceName + " to connect");
            //Toast.makeText(this, "Inviting " + sourceDeviceName + " to connect", Toast.LENGTH_LONG).show();
            wifiDirectHandler.initiateConnectToService(service);
        } else {
            Log.e(TAG, "Service not available " + (WifiP2pDeviceStatus.get(service.getSrcDevice().status)).toString());
            //Toast.makeText(this, "Service not available", Toast.LENGTH_LONG).show();
            if(retryTimes > 5) {
                Log.e(TAG, "MAX OF RETRIES REACHED!");
                return;
            }

            retryTimes++;
            TimerTask doAsynchronousTask = new TimerTask() {
                @Override
                public void run() {
                    handler.post(new Runnable() {
                        public void run() {
                            try {
                                RetryConnectionTask performBackgroundTask = new RetryConnectionTask();
                                // PerformBackgroundTask this class is the class that extends AsynchTask
                                performBackgroundTask.execute(service);
                            } catch (Exception e) {
                                // TODO Auto-generated catch block
                            }
                        }
                    });
                }
            };
            timer.schedule(doAsynchronousTask, 20000); //execute in every 50000 ms
        }
    }

    Timer timer = new Timer();
    final Handler handler = new Handler();

    int retryTimes = 0;

    private class RetryConnectionTask extends AsyncTask<DnsSdService, Void, String> {
        protected String doInBackground(DnsSdService... objects) {
            Log.d(TAG, "Retrying connection task");
            DnsSdService service = objects[0];

            //TODO: check integrity, may work sometimes and others won't
            activity.getWifiHandler().cancelConnect();
            activity.onServiceClick(service);
            return "";
        }

        protected void onProgressUpdate(Void... progress) {
        }

        protected void onPostExecute(String result) {
        }
    }

    protected void onPause() {
        super.onPause();
//        Log.i(TAG, "Pausing MainActivity");
//        if (wifiDirectHandlerBound) {
//            Log.i(TAG, "WifiDirectHandler service unbound");
//            unbindService(wifiServiceConnection);
//            wifiDirectHandlerBound = false;
//        }
        stopLocationUpdates();
        Log.i(TAG, "MainActivity paused");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "Resuming MainActivity");
//        Intent intent = new Intent(this, WifiDirectHandler.class);
//        if(!wifiDirectHandlerBound) {
//            bindService(intent, wifiServiceConnection, BIND_AUTO_CREATE);
//        }
        Log.i(TAG, "MainActivity resumed");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "Starting MainActivity");
//        Intent intent = new Intent(this, WifiDirectHandler.class);
//        bindService(intent, wifiServiceConnection, BIND_AUTO_CREATE);
        Log.i(TAG, "MainActivity started");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "Stopping MainActivity");
//        if(wifiDirectHandlerBound) {
//            Intent intent = new Intent(this, WifiDirectHandler.class);
//            stopService(intent);
//            unbindService(wifiServiceConnection);
//            wifiDirectHandlerBound = false;
//        }
        Log.i(TAG, "MainActivity stopped");
        stopLocationUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Destroying MainActivity");
        if (wifiDirectHandlerBound) {
            Log.i(TAG, "WifiDirectHandler service unbound");
            unbindService(wifiServiceConnection);
            wifiDirectHandlerBound = false;
            Log.i(TAG, "MainActivity destroyed");
        }
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            return;
        }
        locationManager.removeUpdates(locationListener);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "Image captured");
//        if (requestCode == 1 && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            chatFragment.pushImage(imageBitmap);
//        }
    }

    /**
     * BroadcastReceiver used to receive Intents fired from the WifiDirectHandler when P2P events occur
     * Used to update the UI and receive communication messages
     */
    public class CommunicationReceiver extends BroadcastReceiver {

        private static final String TAG = WifiDirectHandler.TAG + "CommReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            // Get the intent sent by WifiDirectHandler when a service is found
            if (intent.getAction().equals(WifiDirectHandler.Action.SERVICE_CONNECTED)) {
                // This device has connected to another device broadcasting the same service
                Log.i(TAG, "Service connected onReceive()");
                processConnectionIn(activity.deviceType);
                /*if (chatFragment == null) {
                    chatFragment = new ChatFragment();
                }
                replaceFragment(chatFragment);
                Log.i(TAG, "Switching to Chat fragment");*/
            } else if (intent.getAction().equals(WifiDirectHandler.Action.DEVICE_CHANGED)) {
                // This device's information has changed
                Log.i(TAG, "This device changed");
                deviceInfoTextView.setText(wifiDirectHandler.getThisDeviceInfo());
            } else if (intent.getAction().equals(WifiDirectHandler.Action.MESSAGE_RECEIVED)) {
                // A message from the Communication Manager has been received
                Log.i(TAG, "Message received");
                new ReceiveMessageTask().execute(intent.getByteArrayExtra(WifiDirectHandler.MESSAGE_KEY));
                /*if(chatFragment != null) {
                    chatFragment.pushMessage(intent.getByteArrayExtra(WifiDirectHandler.MESSAGE_KEY));
                }*/
            } else if (intent.getAction().equals(WifiDirectHandler.Action.WIFI_STATE_CHANGED)) {
                // Wi-Fi has been enabled or disabled
                Log.i(TAG, "Wi-Fi state changed");
                mainFragment.handleWifiStateChanged();
            }
        }
    }

    public String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String currentDateandTime = sdf.format(new Date());
        return currentDateandTime;
    }

    public String getCurrentLocation(){
        if(curLocation == null)
            return "";
        return String.valueOf(curLocation.getLatitude()) + "," + String.valueOf(curLocation.getLongitude());
    }

    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location loc) {
            String latitude = "Latitude: " + loc.getLatitude();
            String longitude = "Longitude: " + loc.getLongitude();
            Log.v(TAG, latitude + " " + longitude);
            curLocation = loc;
        }

        @Override
        public void onProviderDisabled(String provider) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    }

    public User getUser() {
        ArrayList<User> usrs = dbManager.fetchTypeUsers(new String[] { String.valueOf(UserType.MYSELF.getCode()) });
        this.user = usrs.get(0);
        Log.d(TAG, "getUser(); Resultado: " + json.toJson(usrs) + ".");
        return this.user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void enableGps() {
        Intent intent=new Intent("android.location.GPS_ENABLED_CHANGE");
        intent.putExtra("enabled", true);
        sendBroadcast(intent);
    }

    public void disableGps() {
        Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
        intent.putExtra("enabled", false);
        sendBroadcast(intent);
    }

    private void processConnectionTo(String type) {
        Log.d(TAG, "processConnectionTo(); " + type);
        switch (type) {
            case "ACCESS_POINT":
                Log.i(TAG, "ACCESS_POINT");
                new SendMessageTask().execute(activity, json.toJson(activity.curDevice), ObjectType.HELLO);
                break;
            case "ACCESS_POINT_WREQ":
                Log.i(TAG, "ACCESS_POINT_WREQ");
                new SendMessageTask().execute(activity, json.toJson(activity.curDevice), ObjectType.HELLO);
                break;
            case "ACCESS_POINT_WRES":
                Log.i(TAG, "ACCESS_POINT_WRES");
                new SendMessageTask().execute(activity, json.toJson(activity.curDevice), ObjectType.HELLO);
                break;
            default:
                Log.d(TAG, "Waiting for incoming messages.");
        }
    }

    private void processConnectionIn(DeviceType deviceType) {
        Log.d(TAG, "processConnectionIn(); " + deviceType.toString());
        switch (deviceType) {
            case ACCESS_POINT:
                Log.i(TAG, "ACCESS_POINT");
                Log.d(TAG, "Waiting for incoming messages.");
                break;
            case ACCESS_POINT_WREQ:
                Log.i(TAG, "ACCESS_POINT_WREQ");
                Log.d(TAG, "Waiting for incoming messages.");
                break;
            case ACCESS_POINT_WRES:
                Log.i(TAG, "ACCESS_POINT_WRES");
                Log.d(TAG, "Waiting for incoming messages.");
                break;
            case RANGE_EXTENDER:
                new SendMessageTask().execute(activity, json.toJson(activity.curDevice), ObjectType.DISCOVERY);
                break;
            /*case RANGE_EXTENDER_WREQ:
                new SendMessageTask().execute(activity, json.toJson(activity.curDevice), ObjectType.REQUEST);
                break;
            case RANGE_EXTENDER_WRES:
                new SendMessageTask().execute(activity, json.toJson(activity.curDevice), ObjectType.RESPONSE);
                break;*/
            case EMITTER:
                new SendMessageTask().execute(activity, json.toJson(activity.curDevice), ObjectType.HELLO);
                break;
            case QUERIER:
                new SendMessageTask().execute(activity, json.toJson(activity.curDevice), ObjectType.REQUEST);
                break;
            default:
                //new SendMessageTask().execute(activity, json.toJson(activity.curDevice), ObjectType.HELLO);
                break;
        }
    }

    private class ReceiveMessageTask extends AsyncTask<byte[], Void, Message> {
        protected Message doInBackground(byte[]... objects) {
            byte[] readMessage = objects[0];
            Message message = SerializationUtils.deserialize(readMessage);
            switch(message.messageType) {
                case TEXT:
                    Log.i(TAG, "Text message received");
                    //return new String(message.message);
                    return message;
            }
            Log.d(TAG,"ReceiveMessageTask");
            return null;
        }

        protected void onProgressUpdate(Void... progress) {
        }

        protected void onPostExecute(Message result) {
            processReceivedMessage(result);
        }
    }

    private void processReceivedMessage(Message msg) {
        String s = new String(msg.message);

        if (s.equals("")) return;
        Log.d(TAG, "Received: " + s);

        switch (msg.objectType) {
            case HELLO:
                activity.getWifiHandler().continuouslyDiscoverServices();
                if (activity.getWifiHandler().isGroupOwner())
                {
                    activity.getWifiHandler().removeGroup();
                    Log.d(TAG, "Removing group.");
                }
                break;
            case REQUEST:
                //TODO: define a block for answerable and another for unanswerable requests
                Log.d(TAG, "removingService()");
                activity.getWifiHandler().removeService();
                Log.d(TAG, "registeringService()");
                activity.deviceType = DeviceType.ACCESS_POINT_WREQ;
                activity.registerService(activity.deviceType);

                Log.d(TAG, "restartingServiceDiscovery()");
                activity.getWifiHandler().continuouslyDiscoverServices();
                if (activity.getWifiHandler().isGroupOwner())
                {
                    activity.getWifiHandler().removeGroup();
                    Log.d(TAG, "Removing group.");
                }
                break;
            case DISCOVERY:
                //TODO: query to get results
                new SendMessageTask().execute(activity, json.toJson(activity.curDevice), ObjectType.RESULT);
                break;
            case RESULT: //This is gotten by RANGE_EXTENDER only
                new SendMessageTask().execute(activity, json.toJson(activity.curDevice), ObjectType.OK);
                break;
            case OK:
                activity.getWifiHandler().continuouslyDiscoverServices();
                if (activity.getWifiHandler().isGroupOwner())
                {
                    activity.getWifiHandler().removeGroup();
                    Log.d(TAG, "Removing group.");
                }
                break;
            default:
        }
    }

    public String getRecordProperty(DnsSdService service, String propName) {
        Map<String, String> mapTxtRecord;
        if (activity.getWifiHandler() != null) {
            DnsSdTxtRecord txtRecord = activity.getWifiHandler().getDnsSdTxtRecordMap().get(service.getSrcDevice().deviceAddress);
            if (txtRecord != null) {
                mapTxtRecord = txtRecord.getRecord();
                for (Map.Entry<String, String> record : mapTxtRecord.entrySet()) {
                    if (record.getKey().equals(propName)) {
                        return record.getValue();
                    }
                }
            }
        }
        return "";
    }

    public boolean registerService(DeviceType deviceType){
        // Add local service
        if (getWifiHandler().getWifiP2pServiceInfo() == null) {
            HashMap<String, String> record = new HashMap<>();
            record.put("Name", getWifiHandler().getThisDevice().deviceName);
            record.put("Address", getWifiHandler().getThisDevice().deviceAddress);
            record.put("DeviceType", deviceType.toString());
            getWifiHandler().addLocalService("Wi-Fi Buddy", record);

            activity.deviceTypeTextView.setText(deviceType.toString());
            return true;
        } else {
            Log.w(TAG, "Service already added");
            return false;
        }
    }
}
