package ca.pmcgovern.cleanup;

import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;
import android.util.Log;

import junit.framework.Assert;

import net.danlew.android.joda.JodaTimeAndroid;

import java.util.Calendar;
import java.util.Date;

import ca.pmcgovern.cleanup.model.DBHelper;
import ca.pmcgovern.cleanup.model.DiscardEvent;
import ca.pmcgovern.cleanup.model.Round;
import ca.pmcgovern.cleanup.util.RoundUtilities;

/**
 * Created by mcgovern on 12/19/15.
 * http://stackoverflow.com/questions/8499554/android-junit-test-for-sqliteopenhelper
 */
public class RoundUtilitiesTest extends AndroidTestCase {

    Round round;

    @Override
    public void setUp() throws Exception {

        RenamingDelegatingContext context = new RenamingDelegatingContext(getContext(), "test_");
        JodaTimeAndroid.init( context );

        this.round = new Round();

        super.setUp();
    }


    @Override
    public void tearDown() throws Exception {
         super.tearDown();
    }


    public void testCurrentDayInRoundNow() {

        round.setDurationDays(7);

        Assert.assertEquals(0, RoundUtilities.getCurrentDayInRound(round));
    }


    /**
     * round starts tomorrow
     */
    public void testCurrentDayInRoundRoundAhead() {

        round.setDurationDays( 7 );

        Calendar c = Calendar.getInstance();
        c.setTime( new Date() );
        c.add( Calendar.DAY_OF_MONTH, 1 );
        round.setStartDate(c.getTimeInMillis());

        Assert.assertEquals( -1, RoundUtilities.getCurrentDayInRound(round) );
    }


    /**
     * Round started 7 days ago and is finished.
     */
    public void testCurrentDayInRoundPast() {

        round.setDurationDays(7);

        Calendar c = Calendar.getInstance();
        c.setTime( new Date() );
        c.add(Calendar.DAY_OF_MONTH, -8);

        round.setStartDate(c.getTimeInMillis());

        Assert.assertEquals( -1, RoundUtilities.getCurrentDayInRound( round ));
    }


    public void testDiscardInRoundNow() {

        Assert.assertTrue(RoundUtilities.discardIsWithinRound(round, new DiscardEvent()));
    }


    public void testDiscardInRoundPast() {

        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DAY_OF_MONTH, -1);

        DiscardEvent discard = new DiscardEvent();
        discard.setDiscardDate( c.getTimeInMillis() );

        Assert.assertFalse(RoundUtilities.discardIsWithinRound(round, discard));
    }

    public void testDiscardInRoundFuture() {

        round.setDurationDays( 7 );

        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DAY_OF_MONTH, 8 );

        DiscardEvent discard = new DiscardEvent();
        discard.setDiscardDate( c.getTimeInMillis() );

        Assert.assertFalse( RoundUtilities.discardIsWithinRound( round, discard ));
    }


}
