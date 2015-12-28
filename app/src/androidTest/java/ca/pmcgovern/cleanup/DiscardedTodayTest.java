package ca.pmcgovern.cleanup;

import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;

import junit.framework.Assert;

import net.danlew.android.joda.JodaTimeAndroid;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import ca.pmcgovern.cleanup.model.DBHelper;
import ca.pmcgovern.cleanup.model.DiscardEvent;
import ca.pmcgovern.cleanup.model.Round;

/**
 * Created by mcgovern on 12/19/15.
 * http://stackoverflow.com/questions/8499554/android-junit-test-for-sqliteopenhelper
 */
public class DiscardedTodayTest extends AndroidTestCase {

    private DBHelper db;

    private Round selectedRound;


    /**
     * Create a round starting TODAY
     * @throws Exception
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        RenamingDelegatingContext context = new RenamingDelegatingContext(getContext(), "test_");
        db = new DBHelper(context);

        JodaTimeAndroid.init(context);

        Round insertRound = new Round();
        insertRound.setName("test");
        insertRound.setDurationDays(7);

        db.insertRound(insertRound);

        this.selectedRound = db.getCurrentRound();
    }


    @Override
    public void tearDown() throws Exception {
        db.close();
        super.tearDown();
    }



    public void testNoDiscardedToday() {

        Assert.assertEquals("Bad count for items discarded today", 0, db.getDiscardedToday());
    }



    public void testDiscardedToday() {

        DiscardEvent discard = new DiscardEvent();

        db.saveDiscardEvent(this.selectedRound, new DiscardEvent());
        db.saveDiscardEvent(this.selectedRound, new DiscardEvent());

        Assert.assertEquals("Bad count for items discarded today", 2, db.getDiscardedToday());
    }


}
