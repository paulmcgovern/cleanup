package ca.pmcgovern.cleanup.services;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import org.joda.time.DateTime;
import org.joda.time.Days;

import java.util.Calendar;
import java.util.Date;

import ca.pmcgovern.cleanup.MainActivity;
import ca.pmcgovern.cleanup.R;
import ca.pmcgovern.cleanup.model.DBHelper;
import ca.pmcgovern.cleanup.model.Round;
import ca.pmcgovern.cleanup.receiver.AlarmReceiver;

/**
 * Created by mcgovern on 9/19/15.
 */
public class NotificationService extends Service {

    public static final String TAG = "NotificaitonService";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG, "Service started");


      //  Context context = getApplicationContext();

        DBHelper db = new DBHelper( this );

        Round curRound = db.getCurrentRound();
        boolean validRound = true;

        if( curRound == null ) {
            Log.w(TAG, "Unable to find current round in DB");
            validRound = false;
        } else if ( curRound.getStatus() != Round.Status.IN_PROGRESS ) {
            Log.w(TAG, "Current round is not in progress");
            validRound = false;
           // return;
        }

        if( !validRound ) {
            stopSelf();
            return START_NOT_STICKY;
        }

        // Display notification...

        // TODO: lookup alarms settings in preferences


        // Set next alarm, if any.
        setNextAlarm(curRound.getDurationDays(), curRound.getStartDate());
        setNotification();
        stopSelf(startId);
        // Let it continue running until it is stopped.
      //  Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
        return START_NOT_STICKY;

    }

    private void setNotification() {


        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("Declutter Reminder")
                        .setContentText("Don't forget to discard some items today");


        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );


        mBuilder.setContentIntent(resultPendingIntent);

        int mNotificationId = 1;
            // Gets an instance of the NotificationManager service
            NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            // Builds the notification and issues it.
            mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }


    // TODO: cancel all


    private void setNextAlarm( int roundDurationDays, long startDateMs ) {

        DateTime start = new DateTime(startDateMs);
        DateTime now   = new DateTime( System.currentTimeMillis() );
        DateTime end   = start.plusDays(roundDurationDays);

        if( now.isAfter( end )) {

            Log.i( TAG, "Round has ended on " + end );
            return;
        }


        int daysLeft = Math.abs( Days.daysBetween(end.toLocalDate(), now.toLocalDate()).getDays() );


        // If this is the last day of the round, do nothing.

        if( daysLeft <= 1 ) {
            Log.i( TAG, "Round ended today. " + end );
            return;
        }


        // TODO: calc alarm time with Joda time
        Calendar updateTime = Calendar.getInstance();
        //updateTime.setTimeZone(TimeZone.getTimeZone("GMT"));
        updateTime.add( Calendar.MINUTE, 2 );//set(Calendar.HOUR_OF_DAY, 11);
        //  updateTime.set(Calendar.MINUTE, 45);
        Log.i(TAG, "Alarm at " + new Date( updateTime.getTimeInMillis()) );
        Log.i( TAG, "setting alarm...." );
        Intent downloader = new Intent(this, AlarmReceiver.class);

        PendingIntent notificationAlarm = PendingIntent.getBroadcast(this,0, downloader, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarms = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

        // TODO: make this inexact
        alarms.set(AlarmManager.RTC, updateTime.getTimeInMillis(), notificationAlarm );
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Service destroyed");
       // Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
    }

}
