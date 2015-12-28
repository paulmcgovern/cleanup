package ca.pmcgovern.cleanup.model;

import android.util.Log;

/**
 * Created by mcgovern on 8/22/15.
 */
public class DiscardEvent {
    private long roundId;
    private long discardDate;


    public DiscardEvent() {
        this.discardDate = System.currentTimeMillis();
    }

    public long getRoundId()
    {
        return roundId;
    }

    public void setRoundId(long roundId) {

        this.roundId = roundId;
    }

    public long getDiscardDate() {
        return discardDate;
    }

    public void setDiscardDate(long discardDate) {
        this.discardDate = discardDate;
    }
}
