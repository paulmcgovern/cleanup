package ca.pmcgovern.cleanup.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import org.joda.time.DateTime;
import org.joda.time.Days;

import java.util.Calendar;
import java.util.Date;

import ca.pmcgovern.cleanup.Constants;
import ca.pmcgovern.cleanup.MainActivity;
import ca.pmcgovern.cleanup.R;
import ca.pmcgovern.cleanup.model.DBHelper;
import ca.pmcgovern.cleanup.model.Round;
import ca.pmcgovern.cleanup.receiver.AlarmReceiver;
import ca.pmcgovern.cleanup.util.RoundUtilities;

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


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if( prefs == null ) {
            throw new IllegalStateException( "Unable to find shared preferences." );
        }

        // If notifications are not enabled, do nothing.
        if( !prefs.getBoolean(Constants.REMINDERS_ENABLED, false )) {
            Log.i( TAG, "Reminders disabled." );
            stopSelf();
            return START_NOT_STICKY;
        }





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
        }

        if( !validRound ) {
            stopSelf();
            return START_NOT_STICKY;
        }



        int dayOfRound = RoundUtilities.getCurrentDayInRound(curRound);

        if( dayOfRound < 0 ) {
            Log.w( TAG, "Round is out of bounds" );
            dayOfRound = 1; // Just so test looks ok
        }



        // Display notification...

        // TODO: lookup alarms settings in preferences


        // Set next alarm, if any.
        setNextAlarm(curRound.getDurationDays(), curRound.getStartDate());


        setNotification( RoundUtilities.getCurrentDayInRound( curRound ), db.getDiscardedToday() );

        stopSelf(startId);
        // Let it continue running until it is stopped.
      //  Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
        return START_NOT_STICKY;
    }


    private void setNotification( int roundDay, int discardedToday ) {

        StringBuilder message = new StringBuilder();


        int itemsToGo = (roundDay + 1) - discardedToday;

        if( itemsToGo < 1 ) {
            message.append( "You have met your target for today!" );
        } else {

            if( itemsToGo == 1 ) {
                message.append( "You must discard one more item today." );
            } else {
                message.append( "You must discard " + itemsToGo + " items today." );
            }
        }


        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("Declutter Day " + (roundDay + 1))
                        .setContentText( message.toString() );


        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity( this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT );


        mBuilder.setContentIntent(resultPendingIntent);

       // int mNotificationId = 1;
            // Gets an instance of the NotificationManager service
            NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Notification notification = mBuilder.build();

        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.flags |= Notification.FLAG_INSISTENT;

        mNotifyMgr.notify(Constants.REMINDER_NOTIFICATION_ID,  notification );
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
