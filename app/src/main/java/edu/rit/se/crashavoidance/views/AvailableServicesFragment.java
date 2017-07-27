package edu.rit.se.crashavoidance.views;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import edu.rit.se.crashavoidance.R;
import edu.rit.se.crashavoidance.network.DeviceType;
import edu.rit.se.wifibuddy.DnsSdService;
import edu.rit.se.wifibuddy.DnsSdTxtRecord;
import edu.rit.se.wifibuddy.WifiDirectHandler;

/**
 * ListFragment that shows a list of available discovered services
 */
public class AvailableServicesFragment extends Fragment{

    private WiFiDirectHandlerAccessor wifiDirectHandlerAccessor;
    private List<DnsSdService> services = new ArrayList<>();
    private AvailableServicesListViewAdapter servicesListAdapter;
    private ListView deviceList;
    private Toolbar toolbar;
    private ProgressBar progressBar;
    private TextView deviceType;
    private static final String TAG = WifiDirectHandler.TAG + "ServicesFragment";

    MainActivity activity;
    String serviceKey;

    Gson json = new Gson();

    /**
     * Sets the Layout for the UI
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();

        View rootView = inflater.inflate(R.layout.fragment_available_services, container, false);
        toolbar = (Toolbar) getActivity().findViewById(R.id.mainToolbar);
        deviceList = (ListView)rootView.findViewById(R.id.device_list);
        progressBar = (ProgressBar)rootView.findViewById(R.id.progressBar);
        deviceType = (TextView)rootView.findViewById(R.id.deviceType);
        activity.deviceTypeTextView = deviceType;

        deviceType.setText(activity.deviceType.toString());
        prepareResetButton(rootView);
        setServiceList();
        services.clear();
        servicesListAdapter.notifyDataSetChanged();
        Log.d("TIMING", "Discovering started " + (new Date()).getTime());
        registerLocalP2pReceiver();
        getHandler().continuouslyDiscoverServices();
        return rootView;
    }

    private void prepareResetButton(View view){
        Button resetButton = (Button)view.findViewById(R.id.reset_button);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetServiceDiscovery();

            }
        });
    }

    /**
     * Sets the service list adapter to display available services
     */
    private void setServiceList() {
        servicesListAdapter = new AvailableServicesListViewAdapter((MainActivity) getActivity(), services);
        deviceList.setAdapter(servicesListAdapter);
    }

    /**
     * Onclick Method for the the reset button to clear the services list
     * and start discovering services again
     */
    private void resetServiceDiscovery(){
        // Clear the list, notify the list adapter, and start discovering services again
        Log.i(TAG, "Resetting Service discovery");
        services.clear();
        servicesListAdapter.notifyDataSetChanged();
        getHandler().resetServiceDiscovery();
    }

    private void restartServiceDiscovery(){
        // Clear the list, notify the list adapter, and start discovering services again
        Log.i(TAG, "Restarting Service discovery");
        getHandler().resetServiceDiscovery();
    }

    private void stopServiceDiscovery(){
        // Clear the list, notify the list adapter, and start discovering services again
        Log.i(TAG, "Stopping Service discovery");
        getHandler().stopServiceDiscovery();
    }

    private void registerLocalP2pReceiver() {
        Log.i(TAG, "Registering local P2P broadcast receiver");
        WifiDirectReceiver p2pBroadcastReceiver = new WifiDirectReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiDirectHandler.Action.DNS_SD_SERVICE_AVAILABLE);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(p2pBroadcastReceiver, intentFilter);
        Log.i(TAG, "Local P2P broadcast receiver registered");
    }

    /**
     * Receiver for receiving intents from the WifiDirectHandler to update UI
     * when Wi-Fi Direct commands are completed
     */
    public class WifiDirectReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get the intent sent by WifiDirectHandler when a service is found
            if (intent.getAction().equals(WifiDirectHandler.Action.DNS_SD_SERVICE_AVAILABLE)) {
                serviceKey = intent.getStringExtra(WifiDirectHandler.SERVICE_MAP_KEY);
                DnsSdService service = getHandler().getDnsSdServiceMap().get(serviceKey);
                Log.d("TIMING", "Service Discovered and Accessed " + (new Date()).getTime());
                // Add the service to the UI and update
                if (servicesListAdapter.addUnique(service)){
                    // TODO Capture an intent that indicates the peer list has changed
                    // and see if we need to remove anything from our list

                    stopServiceDiscovery();
                    new EnlaceTask().execute(activity.deviceType, service);
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (toolbar != null) {
            toolbar.setTitle("Service Discovery");
        }
    }

    /**
     * Shortcut for accessing the wifi handler
     */
    private WifiDirectHandler getHandler() {
        return wifiDirectHandlerAccessor.getWifiHandler();
    }

    /**
     * This is called when the Fragment is opened and is attached to MainActivity
     * Sets the ListAdapter for the Service List and initiates the service discovery
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            wifiDirectHandlerAccessor = ((WiFiDirectHandlerAccessor) getActivity());
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement WiFiDirectHandlerAccessor");
        }
    }

    private class EnlaceTask extends AsyncTask<Object, Void, String> {
        protected String doInBackground(Object... objects) {
            DeviceType type = (DeviceType) objects[0];
            DnsSdService service = (DnsSdService) objects[1];

            String deviceAddress = service.getSrcDevice().deviceAddress;

            DnsSdTxtRecord txtRecord = getHandler().getDnsSdTxtRecordMap().get(deviceAddress);

            Log.d(TAG, json.toJson(service));
            Log.d(TAG, json.toJson(txtRecord));
            Log.d(TAG, json.toJson(deviceAddress));

            switch (type) {
                case EMITTER:
                    Log.i(TAG, "EMITTER");
                    activity.onServiceClick(service);
                    break;
                case QUERIER:
                    Log.i(TAG, "QUERIER_ASK");
                    activity.onServiceClick(service);
                    break;
                case RANGE_EXTENDER:
                    Log.i(TAG, "RANGE_EXTENDER");
                    activity.onServiceClick(service);
                    break;
                default:
                    Log.e(WifiDirectHandler.TAG, "Undefined value for Group Owner Intent: " + activity.deviceType);
            }

            /*DeviceRequest request = activity.curRequest;
            DeviceResponse response = activity.curResponse;
            switch (type) {
                case EMITTER:
                    Log.i(TAG, "EMITTER");
                    if (!activity.visited.containsKey(deviceAddress)) {
                        Log.i(TAG, "EMITTER Available Services not visited");
                        activity.visited.put(deviceAddress, service);

                        // Visit non already visited service
                        activity.onServiceClick(service);
                    }
                    break;
                case QUERIER: //TODO: add timer to determine that we don't get a response (50 secs.)
                    Log.i(TAG, "QUERIER");
                    if (request != null) {
                        Log.i(TAG, "QUERIER Available Services request != null");

                        // Verify if ACCESS_POINT is not already visited
                        if (!activity.visited.containsKey(deviceAddress)) {
                            Log.i(TAG, "QUERIER Available Services not visited");
                            activity.visited.put(deviceAddress, service);

                            // Visit non already visited service
                            activity.onServiceClick(service);
                        }
                    }
                    break;
                case QUERIER_ASK:
                    Log.i(TAG, "QUERIER_ASK");
                    if (request != null) {
                        Log.i(TAG, "QUERIER_ASK Available Services request != null");

                        // Verify if ACCESS_POINT is not already visited
                        if (!activity.visited.containsKey(deviceAddress)) {
                            Log.i(TAG, "QUERIER_ASK Available Services not visited");
                            activity.visited.put(deviceAddress, service);

                            // Visit non already visited service
                            activity.onServiceClick(service);
                        }
                    }
                    break;
                case RANGE_EXTENDER:
                    Log.i(TAG, "RANGE_EXTENDER");
                    DnsSdTxtRecord txtRecord = getHandler().getDnsSdTxtRecordMap().get(serviceKey);
                    if(txtRecord != null) {
                        Map record = txtRecord.getRecord();
                        String deviceType = (String) record.get("DeviceType");
                        Log.i(TAG, deviceType);

                        if (deviceType.equals(ACCESS_POINT_WREQ.toString())) {
                            Log.i(TAG, "Pop request.");
                            activity.visited.put(deviceAddress, service);
                            activity.onServiceClick(service);
                        } else if (deviceType.equals(ACCESS_POINT_WRES.toString())) {
                            Log.i(TAG, "Pop response.");
                            activity.visited.put(deviceAddress, service);
                            activity.onServiceClick(service);
                        } else if (deviceType.equals(ACCESS_POINT.toString())) {
                            if (response != null) {
                                Log.i(TAG, "Push response.");
                                if (request.inRequest(deviceAddress)) {
                                    activity.onServiceClick(service);
                                    activity.visited.put(deviceAddress, service);
                                    Log.i(TAG, "Founded " + deviceAddress + " is part of route.");
                                } else {
                                    Log.i(TAG, "Founded " + deviceAddress + " is not part of route.");
                                }
                            } else if (request != null) {
                                Log.i(TAG, "Push request.");
                                if (!activity.visited.containsKey(deviceAddress)) {
                                    Log.i(TAG, "RANGE_EXTENDER Available Services not visited");
                                    activity.visited.put(deviceAddress, service);
                                    activity.onServiceClick(service);
                                }
                            }
                        }
                    } else {
                        servicesListAdapter.removeItem(service);
                        Log.i(TAG, "DnsSdTxtRecord is null.");
                    }
                    break;
                default:
                    Log.e(WifiDirectHandler.TAG, "Undefined value for Group Owner Intent: " + activity.deviceType);
            }*/
            return "";
        }

        protected void onProgressUpdate(Void... progress) {
        }

        protected void onPostExecute(String result) {
        }
    }
}
