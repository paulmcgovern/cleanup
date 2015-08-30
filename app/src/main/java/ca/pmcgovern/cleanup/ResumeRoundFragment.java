package ca.pmcgovern.cleanup;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ResumeRoundFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_resume_round, container, false);
    }


    public interface ResumeFragmentHandler {
        public void doResume();
    }

    private ResumeFragmentHandler parentViewDerp;

    @Override
    public void onAttach( Activity activity ) {
        super.onAttach(activity);
        this.parentViewDerp = (ResumeFragmentHandler)activity;
    }

}
