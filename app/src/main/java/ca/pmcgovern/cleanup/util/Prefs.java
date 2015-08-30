package ca.pmcgovern.cleanup.util;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by pmcgovern on 8/10/2015.
 */
public class Prefs {

    private static Prefs instance;

    private Prefs() {
     //   SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

      //  String strUserName = SP.getString("username", "NA");
      //  boolean bAppUpdates = SP.getBoolean("applicationUpdates",false);
      //  String downloadType = SP.getString("downloadType","1");
    }


    public boolean isRoundStarted() {
        return true;
    }

    public static Prefs getInstance() {
        if( instance == null ) {
            instance = new Prefs();
        }

        return instance;
    }

}
