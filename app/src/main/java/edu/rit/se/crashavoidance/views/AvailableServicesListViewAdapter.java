package edu.rit.se.crashavoidance.views;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

import edu.rit.se.crashavoidance.R;
import edu.rit.se.crashavoidance.network.DeviceType;
import edu.rit.se.crashavoidance.network.NetworkUtil;
import edu.rit.se.wifibuddy.DnsSdService;
import edu.rit.se.wifibuddy.DnsSdTxtRecord;

/**
 *
 */
class AvailableServicesListViewAdapter extends BaseAdapter {

    String TAG = "ServiceListAdapter";

    private List<DnsSdService> serviceList;
    private final MainActivity context;

    public AvailableServicesListViewAdapter(MainActivity context, List<DnsSdService> serviceList) {
        this.context = context;
        this.serviceList = serviceList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final DnsSdService service = getItem(position);

        // Inflates the template view inside each ListView item
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.service_item, parent, false);
        }

        TextView deviceNameTextView = (TextView) convertView.findViewById(R.id.deviceName);
        TextView deviceInfoTextView = (TextView) convertView.findViewById(R.id.deviceInfo);
        TextView connectTextView = (TextView) convertView.findViewById(R.id.connect);
        connectTextView.setText("Connect");

        String sourceDeviceName = service.getSrcDevice().deviceName;
        if (sourceDeviceName.equals("")) {
            sourceDeviceName = "Android Device";
        }
        deviceNameTextView.setText(sourceDeviceName);

        Map<String, String> mapTxtRecord;
        String strTxtRecord = "";
        if (context.getWifiHandler() != null) {
            DnsSdTxtRecord txtRecord = context.getWifiHandler().getDnsSdTxtRecordMap().get(service.getSrcDevice().deviceAddress);
            if (txtRecord != null) {
                mapTxtRecord = txtRecord.getRecord();
                for (Map.Entry<String, String> record : mapTxtRecord.entrySet()) {
                    strTxtRecord += record.getKey() + ": " + record.getValue() + "\n";
                }
            }
        }
        String status = context.getWifiHandler().deviceStatusToString(context.getWifiHandler().getThisDevice().status);
        String strDeviceInfo = status + "\n" + strTxtRecord;
        deviceInfoTextView.setText(strDeviceInfo);

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                context.onServiceClick(service);
            }
        });

        return convertView;
    }

    /**
     * Add service to the Services list if it has not already been added
     * @param service Service to be added to list
     * @return false if item was already in the list
     */
    // TODO: the returned boolean of this method is never checked
    public Boolean addUnique(DnsSdService service) {
        if (service == null) return false;

        String deviceName = service.getSrcDevice().deviceName;
        Log.d(TAG, String.valueOf(deviceName));

        if (serviceList.contains(service)) {
            return false;
        }

        //TODO: test if we need this condition
        if (deviceName.equals("Android Device") || deviceName.equals("")) {
            return false;
        }

        NetworkUtil networkUtil = NetworkUtil.getInstance(context.deviceType);
        String deviceType = getRecordProperty(service, "DeviceType");
        Log.d(TAG, "Encontrado: " + String.valueOf(deviceType));

        if(networkUtil.canDiscoverTo(deviceType)) {
            serviceList.add(service);
            this.notifyDataSetChanged();
            return true;
        }

        return false;
    }

    public String getRecordProperty(DnsSdService service, String propName) {
        Map<String, String> mapTxtRecord;
        if (context.getWifiHandler() != null) {
            DnsSdTxtRecord txtRecord = context.getWifiHandler().getDnsSdTxtRecordMap().get(service.getSrcDevice().deviceAddress);
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

    public Boolean isDiscoverableForMe(String deviceType) {
        DeviceType myType = context.deviceType;
        Log.d(TAG, String.valueOf(myType));


        if(myType == DeviceType.EMITTER) {
            // ACCESS_POINT, because it has to send its information to a free ACCESS_POINT
            return deviceType.equals(DeviceType.ACCESS_POINT.toString());
        } else if(myType == DeviceType.ACCESS_POINT_WREQ || myType == DeviceType.ACCESS_POINT_WRES) {
            // RANGE_EXTENDER, because it has to redirect its information to the RANGE_EXTENDER
            // !!! but we don't need to see the RANGE_EXTENDER, it has to see the ACCESS_POINT
            //return deviceType.equals(DeviceType.RANGE_EXTENDER.toString());
            return false;
        } else if(myType == DeviceType.QUERIER) {
            // ACCESS_POINT, because the target ACCESS_POINT must not be involved in a searching process
            return deviceType.equals(DeviceType.ACCESS_POINT.toString());
        } else if(myType == DeviceType.QUERIER_ASK) {
            // ACCESS_POINT_WRES, because the target ACCESS_POINT must gives us a response
            return deviceType.equals(DeviceType.ACCESS_POINT_WRES.toString());
        } else if(myType == DeviceType.RANGE_EXTENDER) {
            // ACCESS_POINT_WREQ && ACCESS_POINT_WRES && !ACCESS_POINT
            // because the target must have interesting information (a request or response)
            // or must be a clean ACCESS_POINT to send it a request ONLY
            String str = DeviceType.ACCESS_POINT.toString();
            return deviceType.startsWith(str) && !deviceType.endsWith(str);
        }
        return false;
    }

    @Override
    public int getCount() {
        return serviceList.size();
    }

    @Override
    public DnsSdService getItem(int position) {
        return serviceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }
}
