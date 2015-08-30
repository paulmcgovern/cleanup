package ca.pmcgovern.cleanup.model;

/**
 * Created by mcgovern on 8/22/15.
 */
public class DiscardEvent {
    private int roundId;
    private long discardDate;

    public int getRoundId() {
        return roundId;
    }

    public void setRoundId(int roundId) {
        this.roundId = roundId;
    }

    public long getDiscardDate() {
        return discardDate;
    }

    public void setDiscardDate(long discardDate) {
        this.discardDate = discardDate;
    }
}
