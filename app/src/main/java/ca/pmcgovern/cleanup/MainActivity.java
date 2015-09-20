package ca.pmcgovern.cleanup;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import com.github.mikephil.charting.utils.ValueFormatter;

import net.danlew.android.joda.JodaTimeAndroid;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ca.pmcgovern.cleanup.receiver.AlarmReceiver;
import ca.pmcgovern.cleanup.model.DBHelper;
import ca.pmcgovern.cleanup.model.DiscardEvent;
import ca.pmcgovern.cleanup.model.Round;
import ca.pmcgovern.cleanup.util.IntegerValueFormatter;
import ca.pmcgovern.cleanup.util.RoundUtilities;


public class MainActivity extends ActionBarActivity implements DiscardItemFragment.RoundProvider {

    public static final String TAG = "Main";

    private LineChart chart;
    private Round currentRound;
    private DBHelper db;
    private int roundTotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        JodaTimeAndroid.init(this);

        this.db = new DBHelper( this );

        this.currentRound = db.getCurrentRound();

        if( this.currentRound == null ) {
            Log.i( TAG, "No round found.");
        } else {
            Log.i(TAG, "Current round:" + this.currentRound.getRoundId());
        }


        this.chart = (LineChart)findViewById( R.id.chart);
        this.chart.setTouchEnabled( false );
        this.chart.setBackgroundColor( Color.BLACK );
        //LineChart chart = (LineChart) findViewById(R.id.chart);

//        int count = 21;
        initChart();


        Round.Status roundState = Round.Status.NEW;

        if( this.currentRound != null ) {

            this.roundTotal = this.db.getDiscardedTotal(this.currentRound.getRoundId());

           roundState = this.currentRound.getStatus();

            Log.i(TAG, "ROUND COMPLETE: " + (this.currentRound.getStatus() == Round.Status.DONE));
        }

        setupFragments( roundState );

        updateStatusText(roundState);

    }



    public int getDiscardedTotal() {
        return this.roundTotal;
    }


    private void initChart() {

        chart.setDescription("");
        chart.setBackgroundColor(Color.WHITE);
        chart.setDrawGridBackground(true);


        YAxis rightAxis = chart.getAxisRight();
        YAxis leftAxis = chart.getAxisLeft();

        ValueFormatter intFormat = new IntegerValueFormatter();

        rightAxis.setValueFormatter( intFormat );
        leftAxis.setValueFormatter( intFormat );


        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        int maxXVal = this.currentRound == null ? Constants.DEFAULT_DAY_COUNT: this.currentRound.getDurationDays();

        String[] xVals = new String[ maxXVal ];

        for( int i = 0; i < maxXVal; i++ ) {
            xVals[i] = "Day "+ Integer.toString( i+1);
        }

        Log.i(TAG, "X value length: " + xVals.length );


        ArrayList<LineDataSet> dataSets = new ArrayList<>();
        dataSets.add(generateQuotaData());


        if( this.currentRound != null ) {

            LineDataSet discarded = this.generateDiscardedData( this.currentRound );

            if( discarded != null ) {
                dataSets.add(generateDiscardedData(this.currentRound));
            }
        }

        LineData data = new LineData( xVals, dataSets );
        this.chart.setBackgroundColor(Color.rgb(238, 238, 238));
        Paint bgPaint = this.chart.getPaint(Chart.PAINT_GRID_BACKGROUND );
        bgPaint.setColor(Color.rgb(200, 200, 200));
        this.chart.setData(data);

        this.chart.invalidate();
    }




    /**
     * Geometric progression
     * @return
     */
    private LineDataSet generateQuotaData() {

        int roundDuration = Constants.DEFAULT_DAY_COUNT;

        if( this.currentRound != null ) {
            roundDuration = this.currentRound.getDurationDays();
        }

        ArrayList<Entry> entries = new ArrayList<Entry>();

        Log.i(TAG, "Generating quota data for " + roundDuration + " days");
        for (int day = 0; day < roundDuration; day++) {
            entries.add( new Entry( day + 1, day ));
        }

        LineDataSet quotaDataSet = new LineDataSet(entries, "Quota");
        quotaDataSet.setColor(Color.rgb(240, 0, 0));
        quotaDataSet.setLineWidth(1);

        quotaDataSet.setDrawCircles(false);
        quotaDataSet.setDrawValues(false);
        quotaDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        quotaDataSet.enableDashedLine(10, 10, 0);

        return quotaDataSet;
    }



    private LineDataSet generateDiscardedData( Round round ) {

        if( round == null ) {
            return null;
        }

        LinkedHashMap<Integer,Integer> countByDate = this.db.getDiscardEventCountByDay( round );

        if( countByDate == null || countByDate.isEmpty() ) {
            return null;
        }

        // Insert zeros between entries more than one day apart
        List<Integer> days = new ArrayList<>();

        int prevDay = -1;

        for( Integer day : countByDate.keySet() ) {

            if( prevDay == -1 ) {
                prevDay = day;
                days.add( day );
                continue;
            }

            int daysElapsed = Math.abs( day - prevDay );

            for( int i = 1; i < daysElapsed; i++ ) {
                days.add(prevDay + i );
            }

            days.add( day );
            prevDay = day;
        }

        ArrayList<Entry> entries = new ArrayList<>();

        for( int day : days ) {

            if( countByDate.containsKey( day )) {
                Log.i( TAG, "Count: " + day + " " + countByDate.get( day ));
                entries.add( new Entry( countByDate.get( day ), day ));
            } else {
                Log.i( TAG, "Count: " + day + " force 0" );
                entries.add( new Entry( 0, day ));
            }
        }




       /*
        for( Map.Entry<Integer,Integer> kv : countByDate.entrySet() ) {

            Log.i(TAG, "Discard: " + kv.getKey() + " -> " + kv.getValue());
            entries.add( new Entry( kv.getValue(), kv.getKey()) ); // Value, x-axis
        }
*/
        LineDataSet discardedDataSet = new LineDataSet(entries, "Discarded");
        discardedDataSet.setColor(Color.rgb(60, 200, 255));
        discardedDataSet.setLineWidth(2);
        discardedDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        discardedDataSet.setCircleColor(Color.rgb(60, 200, 255));
        discardedDataSet.setCircleSize(5);
        discardedDataSet.setDrawCircleHole(false);
        discardedDataSet.setValueFormatter(new IntegerValueFormatter());

        return discardedDataSet;
    }



    private void setupFragments( Round.Status roundState ) {

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        Fragment newFragment  = null;

        if( roundState == Round.Status.NEW ) {
            newFragment = new GetStartedFragment();
        } else if( roundState == Round.Status.IN_PROGRESS ) {
            newFragment = new DiscardItemFragment();
        } else if( roundState ==  Round.Status.DONE ) {
            newFragment = new DoneRoundFragment();
        }
        if( newFragment == null ) {
            throw new IllegalArgumentException( "Unknown round state");
        }

        ft.replace(R.id.your_placeholder, newFragment);
        ft.commit();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // TODO: call invalidateOptionsMenu to force redraw.
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // If a round is not in progress
        // hide the option to stop thr round.

        Round.Status roundState = null;

        if( this.currentRound == null ) {
            roundState = Round.Status.NEW;
        } else {
            roundState = this.currentRound.getStatus();
        }

        if( roundState == Round.Status.NEW ) {

            menu.findItem( R.id.action_stop ).setVisible( false );
            menu.findItem( R.id.enable_reminders ).setVisible( false );
            menu.findItem( R.id.disable_reminders ).setVisible( false );

        } else if ( roundState == Round.Status.IN_PROGRESS ) {

            menu.findItem( R.id.action_stop ).setVisible( true );

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

            if( prefs.getBoolean( Constants.REMINDERS_ENABLED, true )) {
                menu.findItem( R.id.enable_reminders ).setVisible( false );
                menu.findItem( R.id.disable_reminders ).setVisible( true );
            } else {
                menu.findItem( R.id.enable_reminders ).setVisible( true );
                menu.findItem( R.id.disable_reminders ).setVisible( false );
            }


        } else if( roundState == Round.Status.DONE ) {

            menu.findItem( R.id.action_stop ).setVisible( false );
            menu.findItem( R.id.enable_reminders ).setVisible( false );
            menu.findItem( R.id.disable_reminders ).setVisible( false );
        }

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
            toSettings();
            return true;
        } else if( id == R.id.action_help ) {
            toHelp();
            return true;
        } else if( id == R.id.action_stop ) {
            stopRoundAlert();
            return true;
        } else if( id == R.id.enable_reminders ) {
            enableReminders();
            return true;
        } else if( id == R.id.disable_reminders ) {
            disableReminders();
            return true;
        } else if ( id == R.id.action_start_over ) {
            startOverAlert();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * Halt the current round.
     * Resets option menu and sets the 'get started' nav fragment
     */
    public void stopRound() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        if( this.currentRound == null ) {
            throw new IllegalStateException( "Current round is null");
        }

        //this.currentRound.setStatus(Round.Status.SUSPENDED);
        this.currentRound.setStatus(Round.Status.DONE);
        //DBHelper db = new DBHelper( this );
        this.db.updateRound( this.currentRound );

        invalidateOptionsMenu();

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.your_placeholder, new DoneRoundFragment() );
        ft.commit();

        // TODO: clear alarms and notifications
        cancelCurrentReminders();


        updateStatusText(Round.Status.DONE);
    }


    private void enableReminders() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = prefs.edit();

        editor.putBoolean( Constants.REMINDERS_ENABLED, true );

        editor.commit();

        // TODO: calculate reminder time with Jodatime: 2 hrs from round start.

        Calendar updateTime = Calendar.getInstance();

        // TODO: calculate reminder time
        updateTime.add(Calendar.MINUTE, 2);

        Log.i(TAG, "Alarm at " + new Date( updateTime.getTimeInMillis()) );
        Log.i( TAG, "setting alarm...." );

        Intent reminderIntent = new Intent(this, AlarmReceiver.class);

        PendingIntent reminder = PendingIntent.getBroadcast(this, 0, reminderIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarms = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

        // TODO: set in window
        alarms.set(AlarmManager.RTC, updateTime.getTimeInMillis(), reminder);

        invalidateOptionsMenu();
        Log.i( TAG, "Reminders enabled." );
    }

    private void disableReminders() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = prefs.edit();

        editor.putBoolean(Constants.REMINDERS_ENABLED, false);
        editor.commit();

        cancelCurrentReminders();
        invalidateOptionsMenu();
        Log.i( TAG, "Reminders disabled." );
    }


    /** Clear current state and go to start round activity */
    public void startOver() {

        // Wipe DB
        // DBHelper db = new DBHelper( this );
        this.db.clearAll();

        // reset app state
        this.currentRound = null;

      //  invalidateOptionsMenu();

       // FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
       // ft.replace(R.id.your_placeholder, new GetStartedFragment());
       // ft.commit();

        cancelCurrentReminders();

       // updateStatusText(Round.Status.NEW);

        Intent intent = new Intent( this, StartRoundActivity.class );
        startActivity(intent);
    }



    /**
     * Cancel notifications and alarms
     */
    private void cancelCurrentReminders() {

        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(Constants.REMINDER_NOTIFICATION_ID);

        Intent downloader = new Intent( this, AlarmReceiver.class );
        PendingIntent notificationAlarm = PendingIntent.getBroadcast(this, 0, downloader, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarms = (AlarmManager) this.getSystemService( Context.ALARM_SERVICE );

        alarms.cancel(notificationAlarm);
    }

    public void updateStatusText( Round.Status roundState ) {

        TextView statusText = (TextView)findViewById(R.id.statusText);

        if( statusText == null ) {
            throw new IllegalArgumentException( "Failed to find status text view" );
        }



        if( roundState == Round.Status.NEW ) {
            statusText.setText( "Waiting to get started..." );
            return;
        }



        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        long roundStartTime = this.currentRound.getStartDate();

        long roundDuration = this.currentRound.getDurationDays();

        // TODO: Jodatime instead
        long daysElapsed = 1 + TimeUnit.DAYS.convert( System.currentTimeMillis() - roundStartTime, TimeUnit.MILLISECONDS );

       // Date startDate = new Date( roundStartTime );//  XXXX
       // Date today = new Date();

        if ( roundState == Round.Status.IN_PROGRESS ) {


            long roundTargetItems = RoundUtilities.getRoundTargetItems( roundDuration );

            statusText.setText( "Round started. Day " + daysElapsed + " of " + roundDuration  + ". Target for round:" + roundTargetItems );



  //      } //else if ( roundState == Round.Status.SUSPENDED ) {
//
 //           statusText.setText("Current round suspended on day " + daysElapsed + " of " + roundDuration);
        } else if ( roundState == Round.Status.DONE ) {
            statusText.setText( "Round Finished");
        } else {

            throw new IllegalArgumentException( "Unknown round state:" + roundState );
        }


        statusText.invalidate();
    }





    public void stopRoundAlert() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder( this );
        alertDialogBuilder.setTitle( "Stop Current Round" );

        alertDialogBuilder
                .setMessage("End current round?")
                .setCancelable(false)
                .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        MainActivity.this.stopRound();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });


        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }



    public void startOverAlert() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder( this );
        alertDialogBuilder.setTitle("Start Over");

        alertDialogBuilder
                .setMessage("Clear all data and start over?")
                .setCancelable(false)
                .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        MainActivity.this.startOver();
                    }
                })
                .setNegativeButton("No",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }


    public void toHelp() {
        Intent intent = new Intent( this, HelpActivity.class );
        startActivity(intent);
    }

    public void toSettings() {
        Intent intent = new Intent( this, SettingsActivity.class );
        startActivity(intent);
    }

    public void toStartRound( View view ) {
        Intent intent = new Intent( this, StartRoundActivity.class );
        startActivity(intent);
    }

    /**
     * Handler for "Discard Item" button
     * @param view
     */
    public void discardItem( View view ) {


        if( this.currentRound == null ) {
            throw new IllegalStateException( "Attempt to discard when Round in null." );
        }
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        new DiscardItemTask().execute( true );
    }

    protected void updateDiscardedCount( int discardedToday, int discardedTotal ) {

        Log.i( TAG, "Update discard count:" + discardedToday );
        Fragment f = getSupportFragmentManager().findFragmentById( R.id.your_placeholder );

        if( f != null && f instanceof DiscardItemFragment ) {
            ((DiscardItemFragment)f).updateCount( discardedToday, discardedTotal );
        }

    }



    public void undoDiscardItem( View view ) {

        if( this.currentRound == null ) {
            throw new IllegalStateException( "Current round is null." );
        }
        new DiscardItemTask().execute(false);
    }


    public Round getCurrentRound() {
        return this.currentRound;
    }

    @Override
    public int getDiscardedTodayCount() {
        return this.db.getDiscardedToday();
    }


    /**
     * IF execute with 'true': increments. 'false' calls undo
     */
    class DiscardItemTask extends AsyncTask<Boolean,Void,Integer> {

        private DBHelper db;

        protected DiscardItemTask() {
            this.db = MainActivity.this.db;
        }

        @Override
        protected Integer doInBackground(Boolean... params) {

            if( this.isCancelled() ) {
                return 0;
            }

            if( params[ 0 ]) {

                DiscardEvent de = new DiscardEvent();
                de.setRoundId(MainActivity.this.currentRound.getRoundId());
                de.setDiscardDate(System.currentTimeMillis());
                this.db.saveDiscardEvent(de);

            } else {

                this.db.undoDiscardEvent();
            }

            int discardedToday = this.db.getDiscardedToday();

            return discardedToday;
        }

        @Override
        protected void onPostExecute(Integer newCount ) {

            // Get new total
            int total = this.db.getDiscardedTotal( MainActivity.this.currentRound.getRoundId() );
            MainActivity.this.roundTotal = total;

            MainActivity.this.updateDiscardedCount( newCount, total );
            MainActivity.this.initChart();
        }
    }
}
