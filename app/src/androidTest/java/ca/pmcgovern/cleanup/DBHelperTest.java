package ca.pmcgovern.cleanup;

import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;
import android.util.Log;

import junit.framework.Assert;

import net.danlew.android.joda.JodaTimeAndroid;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import ca.pmcgovern.cleanup.model.DBHelper;
import ca.pmcgovern.cleanup.model.DiscardEvent;
import ca.pmcgovern.cleanup.model.Round;
import ca.pmcgovern.cleanup.util.RoundUtilities;

/**
 * Created by mcgovern on 12/19/15.
 * http://stackoverflow.com/questions/8499554/android-junit-test-for-sqliteopenhelper
 */
public class DBHelperTest extends AndroidTestCase {
    private DBHelper db;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        RenamingDelegatingContext context = new RenamingDelegatingContext(getContext(), "test_");
        JodaTimeAndroid.init(context);
        db = new DBHelper(context);
    }


    @Override
    public void tearDown() throws Exception {
        db.close();
        super.tearDown();
    }

    public void testClearAll() {

        Round insertRound = new Round();
        insertRound.setName("test");
        insertRound.setDurationDays(7);

        db.insertRound(insertRound);

        db.clearAll();

        Round selectedRound = db.getCurrentRound();

        Assert.assertNull(selectedRound);
    }


    public void testAddRound() {

        Round insertRound = new Round();
        insertRound.setName("test");
        insertRound.setDurationDays(7);

        long id = db.insertRound(insertRound);

        Assert.assertTrue("Bad insert id " + id, id >= 1);

        Round selectedRound = db.getCurrentRound();

        Assert.assertNotNull("Current round is null", selectedRound);

        Assert.assertEquals(id, (long) selectedRound.getRoundId());
        Assert.assertEquals(insertRound.getDurationDays(), selectedRound.getDurationDays());
        Assert.assertEquals(insertRound.getName(), selectedRound.getName());

        Log.i("TEST", "XXXXXXXXX " + insertRound);

        db.clearAll();
    }


    public void testStartDateDefault() {

        Round insertRound = new Round();
        insertRound.setName("test");
        insertRound.setDurationDays(7);

        //insertRound.setStatus(Round.Status.IN_PROGRESS);
        db.insertRound(insertRound);

        Round selectedRound = db.getCurrentRound();

        Calendar c = Calendar.getInstance();
        c.setTime(new Date());

        int day = c.get( Calendar.DAY_OF_MONTH);
        int month = c.get(Calendar.MONTH);
        int year = c.get(Calendar.YEAR);

        c.setTimeInMillis(selectedRound.getStartDate());

        Assert.assertEquals(day, c.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(month, c.get(Calendar.MONTH));
        Assert.assertEquals( year,  c.get(Calendar.YEAR));

        Assert.assertEquals(Round.Status.NEW, selectedRound.getStatus());

        db.clearAll();
    }


    public void testStartDate() {

        Calendar c = Calendar.getInstance();
        c.setTime(new Date());

        c.add(Calendar.YEAR, -1);

        int day = c.get(Calendar.DAY_OF_MONTH);
        int month = c.get(Calendar.MONTH);
        int year = c.get( Calendar.YEAR );


        Round insertRound = new Round();
        insertRound.setName("test");
        insertRound.setDurationDays(7);

        insertRound.setStartDate(c.getTimeInMillis());


        db.insertRound(insertRound);

        Round selectedRound = db.getCurrentRound();

        c.setTime(new Date(selectedRound.getStartDate()));

        Assert.assertEquals(day, c.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(month, c.get(Calendar.MONTH));
        Assert.assertEquals(year, c.get(Calendar.YEAR));
    }

    public void testRoundFinishedNoDiscard() {

        Round insertRound = new Round();
        insertRound.setName("test");
        insertRound.setDurationDays(7);

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(insertRound.getStartDate());
        c.add(Calendar.MONTH, -1);
        c.set(Calendar.DAY_OF_MONTH, 1);

        insertRound.setStartDate(c.getTimeInMillis());

        db.insertRound(insertRound);
        Round selectedRound = db.getCurrentRound();

        Map<Integer,Integer> countByDay = db.getDiscardEventCountByDay( selectedRound );

        Assert.assertEquals( "Missing counts for days", 7, countByDay.size() );
    }


    public void testRoundInProgressNoDiscard() {

        Round insertRound = new Round();
        insertRound.setName("test");
        insertRound.setDurationDays(7);

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(insertRound.getStartDate());

        c.add(Calendar.DAY_OF_MONTH, -3);

        insertRound.setStartDate(c.getTimeInMillis());

        db.insertRound(insertRound);
        Round selectedRound = db.getCurrentRound();

        RoundUtilities.getCurrentDayInRound( selectedRound );

        Map<Integer,Integer> countByDay = db.getDiscardEventCountByDay( selectedRound );

        Assert.assertEquals( "Missing counts for days", 3, countByDay.size() );
    }



}
