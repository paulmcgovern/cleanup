package ca.pmcgovern.cleanup.model;

import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.Days;

import java.util.Calendar;
import java.util.Date;

import ca.pmcgovern.cleanup.Constants;

/**
 * Created by mcgovern on 8/21/15.
 */
public class Round {

    public enum Status { NEW, IN_PROGRESS, DONE };

    private int roundId;
    private String name;
    private int durationDays = Constants.DEFAULT_DAY_COUNT;
    private long startDate;
    private Status status = Status.NEW;
    boolean sendReminders;


    public Round() {

        this.setStartDate( System.currentTimeMillis() );
    }

    public boolean isSendReminders() {
        return sendReminders;
    }

    public void setSendReminders(boolean sendReminders) {
        this.sendReminders = sendReminders;
    }

    public int getRoundId() {
        return roundId;
    }

    public void setRoundId(int roundId) {
        this.roundId = roundId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getStartDate() {
        return startDate;
    }

    /**
     * Normalize to midnight
     * @param startDate
     */
    public void setStartDate(long startDate) {

        DateTime today = new DateTime( startDate )
                .hourOfDay().setCopy( 0 )
                .minuteOfHour().setCopy( 0 )
                .secondOfMinute().setCopy( 0 )
                .millisOfSecond().setCopy( 0 );

        this.startDate = today.getMillis();
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus( Status status) {
        this.status = status;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(int durationDays) {
        this.durationDays = durationDays;
    }


}
