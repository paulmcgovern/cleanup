package ca.pmcgovern.cleanup.util;

import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.Days;

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
        DateTime now   = new DateTime( System.currentTimeMillis() );
        DateTime end   = start.plusDays( r.getDurationDays() );

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
}
