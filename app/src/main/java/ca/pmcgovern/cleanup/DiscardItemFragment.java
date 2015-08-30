package ca.pmcgovern.cleanup;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.app.Activity;
import android.net.Uri;
import android.widget.Button;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

import ca.pmcgovern.cleanup.model.DBHelper;
import ca.pmcgovern.cleanup.model.Round;


public class DiscardItemFragment extends Fragment {

    public DiscardItemFragment() {
    }

  //  TextView today

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        LayoutInflater lf = getActivity().getLayoutInflater();
        View view =  lf.inflate( R.layout.fragment_discard_item, container, false);

      //  updateCount( view );
        // need to initialize with current count.
        updateCount( view, this.roundProvider.getDiscardedTodayCount(), this.roundProvider.getDiscardedTotal());

        return view;
    }


    public void updateCount( int discardedToday, int discardedTotal ) {
        updateCount( getView(), discardedToday, discardedTotal );
    }

    private void updateCount( View view, int discardedToday, int discardedTotal ) {

        // Update quote text, quota status
        TextView t = (TextView)view.findViewById(R.id.todaysQuota);

        Round currentRound = this.roundProvider.getCurrentRound();


        // TODO: Jodatime instead
        long daysElapsed = 1 + TimeUnit.DAYS.convert( System.currentTimeMillis() - currentRound.getStartDate(), TimeUnit.MILLISECONDS );

      //  int discardedToday = db.getDiscardedToday();
        ///int discardedToday = prefs.getInt( DISCARDED_TODAY, 0 );

        toggleUndo(view, discardedToday);

        // TODO: get cound of items we've missed.
        t.setText("Today: " + discardedToday + " / " +  daysElapsed );


        t = (TextView)view.findViewById( R.id.totals );

        t.setText( "Round: " + discardedTotal + " / " + 999 );
/**
        t = (TextView)view.findViewById( R.id.quotaStatus );

        if( discardedToday < daysElapsed  ) {
            t.setText( daysElapsed - discardedToday + " items to go" );
        } else if( discardedToday == daysElapsed ) {
            t.setText( "Today's quota met." );
        } else {
            t.setText( "Over Quota!");
        }
**/

      //  ((TextView)getView().findViewById( R.id.todaysQuota  )).setText( Integer.toString( count ));
    }

    public void toggleUndo( View parentView, int itemCount ) {

        Button undo = (Button)parentView.findViewById( R.id.undoButton );

        if( itemCount > 0 ) {
            undo.setEnabled( true );
            undo.setClickable(true);
        } else {
            undo.setEnabled( false );
            undo.setClickable( false );
        }
    }


    public interface RoundProvider {
        public Round getCurrentRound();
        public int getDiscardedTodayCount();
        public int getDiscardedTotal();
    }
    private RoundProvider roundProvider;

    @Override
    public void onAttach( Activity activity ) {
        super.onAttach(activity);
        this.roundProvider = (RoundProvider)activity;
    }
}