package ca.pmcgovern.cleanup.util;

import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.Days;

import java.util.Date;

import ca.pmcgovern.cleanup.model.DiscardEvent;
import ca.pmcgovern.cleanup.model.Round;

/**
 * Created by mcgovern on 19/09/2015.
 */
public final class RoundUtilities {

    private RoundUtilities(){}

    /**
     * Zero indexed. Returns -1 if current day is outside round.
     * @param r
     * @return
     */
    public static int getCurrentDayInRound( Round r ) {

        if( r == null ) {
            throw new IllegalArgumentException( "Round is null." );
        }

        DateTime start = new DateTime( r.getStartDate() );

        DateTime now   = new DateTime( System.currentTimeMillis() )
                .hourOfDay().setCopy( 0 )
                .minuteOfHour().setCopy( 0 )
                .secondOfMinute().setCopy( 0 )
                .millisOfSecond().setCopy( 0 );

        DateTime end   = start.plusDays( r.getDurationDays() );

        Log.i( "TEST", "start :" +  start.toString() );
        Log.i( "TEST", "end:   " +  end.toString() );
        Log.i( "TEST", "now:   " + now.toString() );


        if( now.isBefore( start )) {
            Log.i( "TEST", "Now is before start");
        }

        if( now.isAfter( end )) {
            Log.i( "TEST", "Now is after end");
        }
        if( now.isBefore( start ) || now.isAfter( end )) {
            return -1;
        }

        return Math.abs( Days.daysBetween(start.toLocalDate(), now.toLocalDate()).getDays() );
    }


    public static final long getRoundTargetItems( long durationDays ) {

        if( durationDays < 1 ) {
            throw new IllegalArgumentException( "Round duration out of range: " + durationDays );
        }
        return (durationDays * (  durationDays + 1 ))/2;
    }

    public static final boolean discardIsWithinRound( Round round, DiscardEvent de) {

        DateTime start = new DateTime( round.getStartDate() );
        DateTime end   = start.plusDays( round.getDurationDays() );

        DateTime discardDate = new DateTime( de.getDiscardDate() );
Log.i( "TEST", "Start: " + start );
Log.i( "TEST", "Eend:  " + end );
Log.i( "TEST", "DSC: " + discardDate );
        return (start.isEqual( discardDate ) || start.isBefore( discardDate ))
                && ( end.isEqual( discardDate ) || end.isAfter( discardDate ) );
    }
}
