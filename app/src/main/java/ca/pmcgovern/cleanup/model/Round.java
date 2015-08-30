package ca.pmcgovern.cleanup.model;

import ca.pmcgovern.cleanup.Constants;

/**
 * Created by mcgovern on 8/21/15.
 */
public class Round {

    public enum Status { NEW, IN_PROGRESS, SUSPENDED, DONE };

    private int roundId;
    private String name;
    private int durationDays = Constants.DEFAULT_DAY_COUNT;
    private long startDate = System.currentTimeMillis();
    private Status status = Status.NEW;
    boolean sendReminders;

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

    public void setStartDate(long startDate) {
        this.startDate = startDate;
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
