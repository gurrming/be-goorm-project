package com.example.heartbit.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PriceUtil {

    private PriceUtil() {}

    public static BigDecimal normalize(BigDecimal price) {
        if (price == null) return BigDecimal.ZERO;

        if (price.compareTo(new BigDecimal("100")) >= 0)
            return price.setScale(0, RoundingMode.FLOOR);

        if (price.compareTo(new BigDecimal("10")) >= 0)
            return price.setScale(1, RoundingMode.FLOOR);

        return price.setScale(2, RoundingMode.FLOOR);
    }
}

