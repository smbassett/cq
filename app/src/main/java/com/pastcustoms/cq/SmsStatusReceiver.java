package com.pastcustoms.cq;


import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

public class SmsStatusReceiver extends BroadcastReceiver {

    private SharedPreferences mSharedPrefs;

    // Check if ComposeMessageActivity is running. If so, show error dialog; if not,
    // send notification.

    @Override
    public void onReceive(Context context, Intent intent){

        // Check if ComposeMessageActivity is running in foreground.
        // Do this by checking Shared Preferences.
        mSharedPrefs = context.getSharedPreferences(
                context.getString(R.string.shared_prefs_file_key), Context.MODE_PRIVATE);
        boolean cqIsForeground = mSharedPrefs.getBoolean(
                context.getString(R.string.prefs_is_foreground_app), false);

        String actionName = intent.getAction();
        //String phoneNumOrName = intent.getStringExtra("PHONE_OR_NAME");

        Bundle extras = intent.getExtras();
        String phoneNumOrName = extras.getString("PHONE_OR_NAME");



        int messageId = intent.getIntExtra("MSG_ID", 0);
        String errorMessage;

        if (actionName.equals("com.pastcustoms.cq.SMS_SENT")){
            switch(getResultCode()) {
                case Activity.RESULT_OK:
                    // Only display "SMS sent" toast if CQ is in foreground.
                    // todo: debugging code -- remove when no longer necessary:
                    errorNotification(context, "This is just a test", messageId);

                    if (cqIsForeground) {
                        Log.d("CQ receiver", "phoneNumOrName: " + phoneNumOrName);
                        Toast.makeText(context, "SMS sent to " + phoneNumOrName, Toast.LENGTH_LONG).show();
                    }
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    // Display error dialog if CQ is in foreground. If not, create a notification.
                    errorMessage = "SMS to " + phoneNumOrName + " was not sent! Please make sure that your phone is not in flight mode and try again.";
                    if (cqIsForeground) {
                        errorDialog(context, errorMessage);
                    } else {
                        errorNotification(context, errorMessage, messageId);
                    }
                    break;
                default:
                    errorMessage = "SMS to " + phoneNumOrName + " was not sent! Please try again.";
                    if (cqIsForeground) {
                        errorDialog(context, errorMessage);
                    } else {
                        errorNotification(context, errorMessage, messageId);
                    }
            }
        }

        if (actionName.equals("com.pastcustoms.cq.SMS_DELIVERED")) {
            switch(getResultCode()) {
                case Activity.RESULT_OK:
                    if (cqIsForeground) {
                        Toast.makeText(context, "SMS delivered to " + phoneNumOrName, Toast.LENGTH_LONG).show();
                    }
                    break;
                default:
                    errorMessage = "SMS to " + phoneNumOrName + " was not successfully delivered! Please try again.";
                    if (cqIsForeground) {
                        errorDialog(context, errorMessage);
                    } else {
                        errorNotification(context, errorMessage, messageId);
                    }
            }
        }
    }

    // todo: set high priority for these notifications
    private void errorNotification(Context context, String errorMessage, int messageId) {

        // Build notification based on provided error message
        NotificationCompat.Builder mNotificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.alert)
                .setContentTitle("CQ could not send your location SMS")
                .setContentText(errorMessage);

        // Send notification to user (can modify later using messageId)
        NotificationManager mNotificationManager
                = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(messageId, mNotificationBuilder.build());
    }

    private void errorDialog(Context context, String message) {
        Intent smsErrorDialogIntent = new Intent(context, SmsErrorDialogActivity.class);
        smsErrorDialogIntent.putExtra("error_message", message);
        smsErrorDialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(smsErrorDialogIntent);
    }
}
