package ca.pmcgovern.cleanup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Spinner;

import java.util.Date;

import ca.pmcgovern.cleanup.model.DBHelper;
import ca.pmcgovern.cleanup.model.Round;

public class StartRoundActivity extends ActionBarActivity {

    public static final String TAG = "StartRound";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_round);
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

        Intent intent = new Intent( this, MainActivity.class );
        startActivity( intent );
    }


}
