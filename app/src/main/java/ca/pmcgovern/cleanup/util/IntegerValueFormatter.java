package ca.pmcgovern.cleanup.util;

import com.github.mikephil.charting.utils.ValueFormatter;

/**
 * Created by mcgovern on 8/26/15.
 */
public class IntegerValueFormatter implements ValueFormatter {
    @Override
    public String getFormattedValue(float value) {
        return String.valueOf((int) Math.floor(value));
    }
}
