package ca.pmcgovern.cleanup;

import android.app.AlertDialog;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.charts.CombinedChart.DrawOrder;
import com.github.mikephil.charting.utils.ValueFormatter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import ca.pmcgovern.cleanup.model.DBHelper;
import ca.pmcgovern.cleanup.model.DiscardEvent;
import ca.pmcgovern.cleanup.model.Round;
import ca.pmcgovern.cleanup.util.IntegerValueFormatter;
import ca.pmcgovern.cleanup.util.Prefs;

public class MainActivity extends ActionBarActivity implements DiscardItemFragment.RoundProvider, GetStartedFragment.Derp, ResumeRoundFragment.ResumeFragmentHandler {

    public static final String TAG = "Main";

    private CombinedChart chart;
    private Round currentRound;
    private DBHelper db;
    private int roundTotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        this.db = new DBHelper( this );

        this.currentRound = db.getCurrentRound();

        if( this.currentRound == null ) {
            Log.i( TAG, "No round found.");
        } else {
            Log.i( TAG, "Current round:" + this.currentRound.getRoundId() );
        }

        this.roundTotal = db.getDiscardedTotal( this.currentRound.getRoundId() );
       // SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());


        this.chart = (CombinedChart)findViewById( R.id.chart);
        //LineChart chart = (LineChart) findViewById(R.id.chart);

//        int count = 21;
        initChart();


        // Set initial interface
        //final int roundState = prefs.getInt(Constants.ROUND_STATE, Constants.ROUND_STATE_NEW);


        Round.Status roundState = Round.Status.NEW;

        if( this.currentRound != null ) {
            roundState = this.currentRound.getStatus();
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
        chart.setDrawGridBackground(false);
        chart.setDrawBarShadow(false);

        // draw bars behind lines
        chart.setDrawOrder(new DrawOrder[] {
                DrawOrder.BAR, DrawOrder.BUBBLE, DrawOrder.CANDLE, DrawOrder.LINE, DrawOrder.SCATTER
        });

        YAxis rightAxis = chart.getAxisRight();
        YAxis leftAxis = chart.getAxisLeft();

        ValueFormatter intFormat = new IntegerValueFormatter();

        rightAxis.setValueFormatter(intFormat);
        leftAxis.setValueFormatter(intFormat);

        rightAxis.setAxisMaxValue(this.currentRound == null ? Constants.DEFAULT_DAY_COUNT : this.currentRound.getDurationDays());
        leftAxis.setAxisMaxValue(this.currentRound == null ? Constants.DEFAULT_DAY_COUNT : this.currentRound.getDurationDays());



        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        int maxXVal = this.currentRound == null ? Constants.DEFAULT_DAY_COUNT: this.currentRound.getDurationDays();

        String[] xVals = new String[maxXVal];

        for( int i = 0; i < maxXVal; i++ ) {
            xVals[i] = "Day "+ Integer.toString( i+1 );
        }
Log.i( TAG, "X value length: " + xVals.length );
        CombinedData data = new CombinedData( xVals );

        data.setData( generateQuotaData() );

        BarData discardedItemData = generateBarData( this.currentRound );

        if( discardedItemData != null ) {
            data.setData( discardedItemData );
        }

        chart.setData(data);
        chart.invalidate();
    }




    /**
     * Geometric progression
     * @return
     */
    private LineData generateQuotaData() {

        int roundDuration = Constants.DEFAULT_DAY_COUNT;

        if( this.currentRound != null ) {
            roundDuration = this.currentRound.getDurationDays();
        }

        ArrayList<Entry> entries = new ArrayList<Entry>();

        Log.i( TAG, "Generating quota data for " + roundDuration + " days" );
        for (int day = 0; day < roundDuration; day++) {
            entries.add( new Entry( day + 1, day ));
        }
Log.i( TAG, "Quota data length:" + entries.size() );
        LineDataSet set = new LineDataSet(entries, "Quota");
        set.setColor(Color.rgb(240, 0, 0 ));
        set.setLineWidth(2.5f);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);

        LineData d = new LineData();
        d.addDataSet(set);

        return d;
    }

    private BarData generateBarData( Round round ) {

        if( round == null ) {
            return null;
        }

        Map<Integer,Integer> countByDate = this.db.getDiscardEventCountByDay( round );


//SELECT cast(julianday(datetime( d.date / 1000, 'unixepoch', 'localtime' )) - julianday( datetime( r.start_date / 1000, 'unixepoch', 'localtime' )) AS INTEGER) AS round_day, COUNT(*) count FROM  discard_event d INNER JOIN cleanup_round  r  ON r.id=10 AND d.round_id=r.id GROUP BY round_day
Log.i( TAG, "CDB: " + countByDate );
        if( countByDate == null || countByDate.isEmpty() ) {
            return null;
        }

        BarData d = new BarData();

        ArrayList<BarEntry> entries = new ArrayList<BarEntry>();

        for( Map.Entry<Integer,Integer> kv : countByDate.entrySet() ) {
            Log.i( TAG, "Discard: " + kv.getKey() + " -> " + kv.getValue() );
            // Value, x-axis
            entries.add( new BarEntry( kv.getValue(), kv.getKey()) );
        }

        BarDataSet set = new BarDataSet(entries, "Discarded Items");
        set.setColor(Color.rgb(60, 220, 78));
        set.setValueTextColor(Color.rgb(60, 220, 78));
        set.setValueTextSize(10f);
        d.addDataSet(set);

        set.setAxisDependency(YAxis.AxisDependency.LEFT);

        return d;
    }

    private void setupFragments( Round.Status roundState ) {

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        Fragment newFragment  = null;

        if( roundState == Round.Status.NEW ) {
            newFragment = new GetStartedFragment();
        } else if( roundState == Round.Status.IN_PROGRESS ) {
            newFragment = new DiscardItemFragment();
        } else if( roundState == Round.Status.SUSPENDED ) {
            newFragment = new ResumeRoundFragment();

        } else if( roundState ==  Round.Status.DONE ) {
            // TODO: done case
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

       // SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());


        // If a round is not in progress
        // hide the option to stop thr round.

       // int roundState = prefs.getInt( Constants.ROUND_STATE, Constants.ROUND_STATE_NEW );

        Round.Status roundState = null;

        if( this.currentRound == null ) {
            roundState = Round.Status.NEW;
        } else {
            roundState = this.currentRound.getStatus();
        }

        if( roundState == Round.Status.NEW ) {

            menu.findItem( R.id.action_stop ).setVisible( false );
            menu.findItem( R.id.action_resume ).setVisible( false );
            menu.findItem( R.id.action_clear ).setVisible(false);

        } else if ( roundState == Round.Status.IN_PROGRESS ) {

            menu.findItem( R.id.action_stop ).setVisible( true );
            menu.findItem( R.id.action_resume ).setVisible( false );
            menu.findItem( R.id.action_clear ).setVisible(true);

        } else if( roundState == Round.Status.SUSPENDED ) {

            menu.findItem( R.id.action_stop ).setVisible( false );
       //     menu.findItem( R.id.action_resume ).setVisible( true );
            menu.findItem( R.id.action_clear ).setVisible(true);
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
        } else if( id == R.id.action_clear ) {
            clearAllAlert();
            return true;
        } else if( id == R.id.action_resume ) {
            resumeRoundAlert();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void resumeRound() {

        if( this.currentRound == null ) {
            throw new IllegalStateException( "Current round is null." );
        }

        this.currentRound.setStatus( Round.Status.IN_PROGRESS );
        DBHelper db = new DBHelper( this );

        db.updateRound( this.currentRound );

        invalidateOptionsMenu();

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.your_placeholder, new DiscardItemFragment() );
        ft.commit();

        updateStatusText( Round.Status.IN_PROGRESS );
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

        this.currentRound.setStatus(Round.Status.SUSPENDED);

        //DBHelper db = new DBHelper( this );
        this.db.updateRound( this.currentRound );

        invalidateOptionsMenu();

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.your_placeholder, new ResumeRoundFragment() );
        ft.commit();

        updateStatusText( Round.Status.SUSPENDED );
    }

    public void clearAll() {

        // Wipe preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.commit();

        // Wipe DB
       // DBHelper db = new DBHelper( this );
       this.db.clearAll();

        // reset app state
        this.currentRound = null;

        invalidateOptionsMenu();

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.your_placeholder, new GetStartedFragment() );
        ft.commit();

        updateStatusText(Round.Status.NEW);
    }




    public void updateStatusText( Round.Status roundState ) {

        TextView statusText = (TextView)findViewById( R.id.statusText );

        if( statusText == null ) {
            throw new IllegalArgumentException( "Failed to find status text view" );
        }



        if( roundState == Round.Status.NEW ) {
            statusText.setText( "Waiting to get started..." );
            return;
        }



        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        long roundStartTime = this.currentRound.getStartDate();//prefs.getLong(Constants.CURRENT_ROUND_START_DATE, System.currentTimeMillis());

        long roundDuration = this.currentRound.getDurationDays(); //prefs.getInt(Constants.CURRENT_ROUND_DAYS, Constants.DEFAULT_DAY_COUNT);

        // TODO: Jodatime instead
        long daysElapsed = 1 + TimeUnit.DAYS.convert( System.currentTimeMillis() - roundStartTime, TimeUnit.MILLISECONDS );

       // Date startDate = new Date( roundStartTime );//  XXXX
       // Date today = new Date();

        if ( roundState == Round.Status.IN_PROGRESS ) {


            long roundTargetItems = (roundDuration * (  roundDuration + 1 ))/2;

            statusText.setText( "Round started. Day " + daysElapsed + " of " + roundDuration  + ". Target for round:" + roundTargetItems );



        } else if ( roundState == Round.Status.SUSPENDED ) {

            statusText.setText( "Current round suspended on day " + daysElapsed +" of " + roundDuration );

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
                .setNegativeButton("No",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                });


        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }


    public void resumeRoundAlert() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder( this );
        alertDialogBuilder.setTitle( "Resume Current Round" );

        alertDialogBuilder
                .setMessage("Resume the current round?")
                .setCancelable(false)
                .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        MainActivity.this.resumeRound();
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






    public void clearAllAlert() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder( this );
        alertDialogBuilder.setTitle( "Clear All Data" );

        alertDialogBuilder
                .setMessage("Clear all data and start over?")
                .setCancelable(false)
                .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        MainActivity.this.clearAll();
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
        startActivity( intent );
    }

    public void toStartRound( View view ) {
        Intent intent = new Intent( this, StartRoundActivity.class );
        startActivity(intent);
    }

    public void resumeRound( View view ) {
        resumeRoundAlert();
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
        new DiscardItemTask().execute( false );
    }

    @Override
    public void doDerp() {
        Log.i( "XXX", "Derping....");
       // toStartRound();
    }

    @Override
    public void doResume() {
        Log.i( "XXXX", "do resume..." );
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
