package ca.pmcgovern.cleanup;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A placeholder fragment containing a simple view.
 */
public class GetStartedFragment extends Fragment {

    public GetStartedFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_get_started, container, false);
    }




   // public void  toStartRound( View view ) {
   //     this.parentViewDerp.doDerp();
   // }

    public interface Derp {
        public void doDerp();
    }

    private Derp parentViewDerp;

    @Override
    public void onAttach( Activity activity ) {
        super.onAttach(activity);
        this.parentViewDerp = (Derp)activity;
    }
}
