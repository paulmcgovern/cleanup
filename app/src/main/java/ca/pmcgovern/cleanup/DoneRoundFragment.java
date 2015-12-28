package ca.pmcgovern.cleanup;
import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.NumberFormat;

import ca.pmcgovern.cleanup.model.Round;


public class DoneRoundFragment extends DiscardItemFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_done_round, container, false);

        updateCount( view, this.roundProvider.getDiscardedTotal());

        return view;
    }


    public void updateCount( View view, int discardedTotal ) {

        Round currentRound = this.roundProvider.getCurrentRound();

        int roundDuration = currentRound.getDurationDays();
        long roundTargetItems = ( roundDuration * (  roundDuration + 1 ))/2;

        double pc = ( (double)discardedTotal / (double)roundTargetItems );

        NumberFormat pcformat = NumberFormat.getPercentInstance();
        pcformat.setMinimumFractionDigits( 1 );
        pcformat.setMaximumFractionDigits(1);

        TextView t = (TextView)view.findViewById( R.id.totals );
        t.setText( "Discarded " + discardedTotal + " of " + roundTargetItems + " items.");

        t = (TextView)view.findViewById( R.id.percent );
        t.setText( pcformat.format( pc ) + " complete");
    }
}
