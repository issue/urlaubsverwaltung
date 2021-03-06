package org.synyx.urlaubsverwaltung.web.statistics;

import java.math.BigDecimal;

import java.util.HashMap;
import java.util.Map;


/**
 * Represents number of days for specific sick note types.
 *
 * @author  Aljona Murygina - murygina@synyx.de
 */
public class SickDays {

    public enum SickDayType {

        TOTAL,
        WITH_AUB
    }

    private final Map<String, BigDecimal> days;

    public SickDays() {

        days = new HashMap<>();

        days.put(SickDayType.TOTAL.name(), BigDecimal.ZERO);
        days.put(SickDayType.WITH_AUB.name(), BigDecimal.ZERO);
    }

    public Map<String, BigDecimal> getDays() {

        return days;
    }


    public void addDays(SickDayType type, BigDecimal days) {

        BigDecimal addedDays = this.days.get(type.name()).add(days);

        this.days.put(type.name(), addedDays);
    }
}
