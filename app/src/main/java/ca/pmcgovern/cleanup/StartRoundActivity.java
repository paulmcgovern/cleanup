package ca.pmcgovern.cleanup;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Spinner;

import java.util.Calendar;
import java.util.Date;

import ca.pmcgovern.cleanup.model.DBHelper;
import ca.pmcgovern.cleanup.model.Round;
import ca.pmcgovern.cleanup.receiver.AlarmReceiver;

public class StartRoundActivity extends ActionBarActivity {

    public static final String TAG = "StartRound";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_round);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        CheckBox reminderCkbx = (CheckBox)findViewById( R.id.reminderEnable );

        Log.i( TAG, "Reminders enabled:" +  prefs.getBoolean( Constants.REMINDERS_ENABLED, false ));

        if( prefs.getBoolean( Constants.REMINDERS_ENABLED, false )) {
            reminderCkbx.setChecked( true );
        } else {
            reminderCkbx.setChecked( false );
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_start_round, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * Go back to main, don't change anything
     * @param view
     */
    public void cancelToMain( View view ) {
        Intent intent = new Intent( this, MainActivity.class );
        startActivity( intent );
    }

    /**
     * Set preferences for current round params and return to main
     * @param view
     */
    public void toMain( View view ) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = prefs.edit();

        CheckBox reminderCkbx = (CheckBox)findViewById( R.id.reminderEnable );

        boolean remindersEnabled = reminderCkbx.isChecked();

        if( remindersEnabled ) {
            editor.putBoolean( Constants.REMINDERS_ENABLED, true );
            Log.i( TAG, "Reminders enabled." );
        } else {
            editor.putBoolean( Constants.REMINDERS_ENABLED, false );
            Log.i( TAG, "Reminders disabled." );
        }


        Spinner durationSelect = (Spinner)findViewById( R.id.dayCount );

        int duration = 7 * (durationSelect.getSelectedItemPosition() + 1);

        if( duration < 1 || duration > Constants.DEFAULT_DAY_COUNT ) {
            duration = Constants.DEFAULT_DAY_COUNT;
        }

        Log.i(TAG, "Selected duration: " + duration);

        Round r = new Round();
        r.setDurationDays( duration );
        r.setName( new Date().toString() );
        r.setStatus(Round.Status.IN_PROGRESS);
        r.setStartDate( System.currentTimeMillis() );

        DBHelper db = new DBHelper( this );
        db.insertRound( r );

        editor.commit();

        // Kill any outstanding notifications.
        cancelCurrentReminders();

        // If reminders have been enabled kick off the first alarm
        if( remindersEnabled ) {
            initReminderAlarm();
        }

        Intent intent = new Intent( this, MainActivity.class );
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity( intent );
        finish();
    }


    private void cancelCurrentReminders() {

        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.cancel( Constants.REMINDER_NOTIFICATION_ID );

        Intent downloader = new Intent( this, AlarmReceiver.class );
        PendingIntent notificationAlarm = PendingIntent.getBroadcast( this,0, downloader, PendingIntent.FLAG_CANCEL_CURRENT );
        AlarmManager alarms = (AlarmManager) this.getSystemService( Context.ALARM_SERVICE );

        alarms.cancel( notificationAlarm );
    }

    private void initReminderAlarm() {


        // TODO: calculate reminder time with Jodatime: 2 hrs from round start.

        Calendar updateTime = Calendar.getInstance();

        updateTime.add( Calendar.MINUTE, 2 );

        Log.i(TAG, "Alarm at " + new Date( updateTime.getTimeInMillis()) );
        Log.i( TAG, "setting alarm...." );
        Intent downloader = new Intent(this, AlarmReceiver.class);

        PendingIntent recurringDownload = PendingIntent.getBroadcast(this,0, downloader, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarms = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

        // TODO: set in window
        alarms.set(AlarmManager.RTC, updateTime.getTimeInMillis(), recurringDownload);
    }
}
