package exchange.core2.core.utils;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Range {

    private long low;
    private long high;

    public static boolean isValidRange(Range r) {

        if (r == null) {
            return false;
        }

        if (r.getHigh() == 0 && r.getLow() == 0) {
            return false;
        }

        return true;
    }

    public boolean isInRange(long value) {
        return low <= value && value <= high && (low != high && high != 0);
    }

}
