package ca.pmcgovern.cleanup;

import android.app.AlertDialog;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Paint;
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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import ca.pmcgovern.cleanup.model.DBHelper;
import ca.pmcgovern.cleanup.model.DiscardEvent;
import ca.pmcgovern.cleanup.model.Round;
import ca.pmcgovern.cleanup.util.IntegerValueFormatter;


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
        dataSets.add( generateQuotaData() );


        if( this.currentRound != null ) {

            LineDataSet discarded = this.generateDiscardedData( this.currentRound );

            if( discarded != null ) {
                dataSets.add(generateDiscardedData(this.currentRound));
            }
        }

        LineData data = new LineData( xVals, dataSets );
        this.chart.setBackgroundColor(Color.rgb(238,238,238));
        Paint bgPaint = this.chart.getPaint(Chart.PAINT_GRID_BACKGROUND );
        bgPaint.setColor( Color.rgb( 200, 200, 200 ));
        this.chart.setData( data );

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

        Map<Integer,Integer> countByDate = this.db.getDiscardEventCountByDay( round );

        if( countByDate == null || countByDate.isEmpty() ) {
            return null;
        }

        ArrayList<Entry> entries = new ArrayList<>();

        for( Map.Entry<Integer,Integer> kv : countByDate.entrySet() ) {

            Log.i(TAG, "Discard: " + kv.getKey() + " -> " + kv.getValue());
            entries.add( new Entry( kv.getValue(), kv.getKey()) ); // Value, x-axis
        }

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
            menu.findItem( R.id.action_clear ).setVisible(false);

        } else if ( roundState == Round.Status.IN_PROGRESS ) {

            menu.findItem( R.id.action_stop ).setVisible( true );
            menu.findItem( R.id.action_clear ).setVisible(true);

        } else if( roundState == Round.Status.DONE ) {

            menu.findItem( R.id.action_stop ).setVisible( false );
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

        updateStatusText( Round.Status.DONE );
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
