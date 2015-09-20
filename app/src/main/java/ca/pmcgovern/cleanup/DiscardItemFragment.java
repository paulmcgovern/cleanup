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
import ca.pmcgovern.cleanup.util.RoundUtilities;


public class DiscardItemFragment extends Fragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        LayoutInflater lf = getActivity().getLayoutInflater();
        View view =  lf.inflate( R.layout.fragment_discard_item, container, false);


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

        long roundTargetItems = RoundUtilities.getRoundTargetItems( currentRound.getDurationDays() );

        t.setText( "Round: " + discardedTotal + " / " + roundTargetItems );
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

    protected RoundProvider roundProvider;

    @Override
    public void onAttach( Activity activity ) {
        super.onAttach(activity);
        this.roundProvider = (RoundProvider)activity;
    }
}
