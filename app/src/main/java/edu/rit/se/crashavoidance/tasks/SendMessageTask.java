package edu.rit.se.crashavoidance.tasks;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.lang3.SerializationUtils;

import edu.rit.se.crashavoidance.network.Message;
import edu.rit.se.crashavoidance.network.MessageType;
import edu.rit.se.crashavoidance.network.ObjectType;
import edu.rit.se.crashavoidance.views.MainActivity;
import edu.rit.se.wifibuddy.CommunicationManager;

/**
 * Created by osvaldo on 20/05/17.
 */

public class SendMessageTask extends AsyncTask<Object, Void, String> {
    String TAG = "SendMessageTask";

    protected String doInBackground(Object... objects) {
        MainActivity activity = (MainActivity) objects[0];
        String message = objects[1].toString();
        ObjectType objectType = (ObjectType) objects[2];

        Log.d(TAG, "Sending: " + message);
        CommunicationManager communicationManager = activity.getWifiHandler().getCommunicationManager();
        if (communicationManager != null && !message.equals("")) {
            // Gets first word of device name
            //String author = getWifiHandler().getThisDevice().deviceName.split(" ")[0];
            byte[] messageBytes = (message).getBytes();
            Message finalMessage = new Message(MessageType.TEXT, messageBytes);
            finalMessage.objectType = objectType;
            communicationManager.write(SerializationUtils.serialize(finalMessage));
        } else {
            Log.e(TAG, "Communication Manager is null");
        }
        Log.d(TAG, "SendMessageTask");
        return "";
    }

    protected void onProgressUpdate(Void... progress) {
    }

    protected void onPostExecute(String result) {
    }
}
