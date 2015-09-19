package ca.pmcgovern.cleanup.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import ca.pmcgovern.cleanup.model.DBHelper;
import ca.pmcgovern.cleanup.model.Round;
import ca.pmcgovern.cleanup.services.NotificationService;

public class AlarmReceiver extends BroadcastReceiver {


    public static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.i(TAG, "Received alarm broadcast...");

        // Start notification service
        Intent i = new Intent(context, NotificationService.class);
        context.startService(i);

/**
        // Get preferences. If reminders not enabled, do nothing

        DBHelper db = new DBHelper( context );

        Round curRound = db.getCurrentRound();

        if( curRound == null ) {
            Log.w(TAG, "Unable to find current round in DB");
            return;
        }

        // If the current round is finished, do nothing
        if( curRound.getStatus() != Round.Status.IN_PROGRESS ) {
            Log.w(TAG, "Current round is not in progress");
            return;
        }

        int discardedToday = db.getDiscardedToday();
        String message = null;

        if( discardedToday < 1 ) {
            message = "Don't forget to discard something today.";
        } else {
            message = "You have discarded " + discardedToday + " things. Keep it up!";
        }

        buildNotification( context, message );

**/
        // Schedule next alarm
    }

  //  private void buildNotification(Context context, String message ) {
  //      Log.i( TAG, "Build notificaiton...");
  //  }
}
