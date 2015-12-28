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

/**
 * Created by mcgovern on 12/19/15.
 * http://stackoverflow.com/questions/8499554/android-junit-test-for-sqliteopenhelper
 */
public class DiscardEventTest extends AndroidTestCase {
    private DBHelper db;

    private Round selectedRound;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        RenamingDelegatingContext context = new RenamingDelegatingContext(getContext(), "test_");


        JodaTimeAndroid.init(context);

        db = new DBHelper(context);

        // Create a round in the past

        Calendar c = Calendar.getInstance();
        c.setTime(new Date());

        c.add(Calendar.YEAR, - 1 );

        Round insertRound = new Round();
        insertRound.setName("test");
        insertRound.setDurationDays(7);
        insertRound.setStartDate(c.getTimeInMillis());

        db.insertRound(insertRound);

        this.selectedRound = db.getCurrentRound();


    }


    @Override
    public void tearDown() throws Exception {
        db.close();
        super.tearDown();
    }


    public void testZeroIndexed() {

        DiscardEvent discard = new DiscardEvent();
        discard.setDiscardDate( this.selectedRound.getStartDate() );

        db.saveDiscardEvent(this.selectedRound, discard);

        Map<Integer,Integer> countByDay = db.getDiscardEventCountByDay( selectedRound );

        Assert.assertNotNull( countByDay );
        Assert.assertTrue(countByDay.size() == this.selectedRound.getDurationDays() );
        Assert.assertNotNull(countByDay.get(0));
    }



    public void testDiscardOnePerDay() {

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(this.selectedRound.getStartDate());

        for( int i = 0; i < 7; i++ ) {

            DiscardEvent discard = new DiscardEvent();
            discard.setDiscardDate(c.getTimeInMillis());

            db.saveDiscardEvent(this.selectedRound, discard);
            c.add( Calendar.DAY_OF_MONTH, 1 );
        }

        Map<Integer,Integer> countByDay = db.getDiscardEventCountByDay( selectedRound );

        Assert.assertNotNull(countByDay);
        Assert.assertTrue(countByDay.size() == 7);

        for( int i = 0; i < 7; i++ ) {
            Assert.assertTrue(countByDay.get(i) == 1);
        }
    }


    public void testDiscardMultiplePerDay() {

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(this.selectedRound.getStartDate());

        for( int i = 0; i < 7; i++ ) {

            DiscardEvent discard = new DiscardEvent();
            discard.setDiscardDate(c.getTimeInMillis());

            for (int j = 0; j < ( i + 1 ); j++) {
                db.saveDiscardEvent( this.selectedRound, discard);
            }

            c.add( Calendar.DAY_OF_MONTH, 1 );
        }

        Map<Integer,Integer> countByDay = db.getDiscardEventCountByDay( selectedRound );

        Assert.assertNotNull(countByDay);
        Assert.assertTrue(countByDay.size() == 7);

        for( int i = 0; i < 7; i++ ) {
            Assert.assertTrue( countByDay.get(i) == i + 1 );
        }
    }


    public void testDiscardZeroFirstDay() {

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis( this.selectedRound.getStartDate() );
        c.add(Calendar.DAY_OF_MONTH, 1);

        for( int i = 0; i < 6; i++ ) {

            DiscardEvent discard = new DiscardEvent();
            discard.setDiscardDate(c.getTimeInMillis());

            db.saveDiscardEvent(this.selectedRound, discard);
            c.add(Calendar.DAY_OF_MONTH, 1);
        }

        Map<Integer,Integer> countByDay = db.getDiscardEventCountByDay( selectedRound );

        Assert.assertNotNull(countByDay);
        Assert.assertEquals( this.selectedRound.getDurationDays(), countByDay.size() ) ;
        Assert.assertTrue( countByDay.get(0) == 0 );
    }


    public void testDiscardZeroLastDay() {

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(this.selectedRound.getStartDate());

        for( int i = 0; i < 6; i++ ) {

            DiscardEvent discard = new DiscardEvent();
            discard.setDiscardDate(c.getTimeInMillis());

            db.saveDiscardEvent( this.selectedRound, discard);
            c.add(Calendar.DAY_OF_MONTH, 1);
        }

        Map<Integer,Integer> countByDay = db.getDiscardEventCountByDay( selectedRound );

        Assert.assertNotNull(countByDay);
        Assert.assertEquals( this.selectedRound.getDurationDays(), countByDay.size() );

        for( int i = 0; i < 6; i++ ) {
            Assert.assertTrue(countByDay.get(i) == 1);
        }

        Assert.assertTrue( countByDay.get( 6 ) == 0 );
    }

    public void testDiscardBeforeRound() {

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(this.selectedRound.getStartDate());

        c.add(Calendar.SECOND, -1);


        DiscardEvent discard = new DiscardEvent();
        discard.setDiscardDate(c.getTimeInMillis());

        try {
            db.saveDiscardEvent(this.selectedRound, discard);
        } catch( IllegalArgumentException e ) {
            return;
        }

        Assert.fail("Saved an event before round start");
    }


    public void testDiscardAfterRound() {

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(this.selectedRound.getStartDate());

        c.add(Calendar.DAY_OF_MONTH, this.selectedRound.getDurationDays());
        c.add( Calendar.SECOND, 1 );


        DiscardEvent discard = new DiscardEvent();
        discard.setDiscardDate(c.getTimeInMillis());

        try {
            db.saveDiscardEvent(this.selectedRound, discard);
        } catch( IllegalArgumentException e ) {
            return;
        }

        Assert.fail( "Saved an event after round start");
    }

    /**
     * If no events, should return zero count for every day.
     */
    public void testNoEvents() {

        Map<Integer,Integer> countByDay = db.getDiscardEventCountByDay( selectedRound );

        Assert.assertNotNull(countByDay);
        Assert.assertEquals( "Count by day missing days", this.selectedRound.getDurationDays(), countByDay.size() );

        for( int i = 0; i < this.selectedRound.getDurationDays(); i++ ) {
            Assert.assertEquals( "Bad count", 0, (int)countByDay.get( i ));
        }

    }


    public void testDiscardEveryOtherDay() {

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(this.selectedRound.getStartDate());


        for( int i = 0; i < 7; i += 2 ) {

            DiscardEvent discard = new DiscardEvent();
            discard.setDiscardDate(c.getTimeInMillis());

            db.saveDiscardEvent(this.selectedRound, discard);
            c.add( Calendar.DAY_OF_MONTH, 2 );
        }

        Map<Integer,Integer> countByDay = db.getDiscardEventCountByDay( selectedRound );

        Assert.assertNotNull(countByDay);
        Assert.assertTrue(countByDay.size() == 7);

        for( int i = 0; i < 7; i++ ) {

            if( i % 2 == 1) {
                Assert.assertTrue( countByDay.get(i) == 0 );
            } else {
                Assert.assertTrue( countByDay.get(i) == 1 );
            }
        }
    }


}
// TODO: set discard date to END date of round
// TODO: trailing zeroes on expired Round