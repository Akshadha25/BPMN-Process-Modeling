package com.example.leave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.leave.service.LeaveRules;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LeaveRulesTest {

    @Test
    void threeWeekdaysCountAsThreeDays() {              // scenario A
        assertEquals(3, LeaveRules.workingDays(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 4)));
    }

    @Test
    void weekendCountsAsZeroDays() {                    // scenario D (Sat-Sun)
        assertEquals(0, LeaveRules.workingDays(LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 8)));
    }

    @Test
    void spanningAWeekendSkipsSaturdayAndSunday() {
        assertEquals(2, LeaveRules.workingDays(LocalDate.of(2026, 11, 6), LocalDate.of(2026, 11, 9)));
    }

    @Test
    void endBeforeStartIsInvalid() {                    // scenario F
        var r = LeaveRules.validate("ANNUAL", LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 2), 0, 20);
        assertFalse(r.valid());
    }

    @Test
    void weekendOnlyIsRejected() {                      // scenario D
        var r = LeaveRules.validate("ANNUAL", LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 8), 0, 20);
        assertFalse(r.valid());
    }

    @Test
    void insufficientAnnualBalanceIsRejected() {        // scenario C
        var r = LeaveRules.validate("ANNUAL", LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 6), 5, 3);
        assertFalse(r.valid());
    }

    @Test
    void sickLeaveIsNotCheckedAgainstAnnualBalance() {
        assertTrue(LeaveRules.validate("SICK", LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 6), 5, 0).valid());
    }

    @Test
    void validAnnualRequestPasses() {                   // scenarios A/B/E
        assertTrue(LeaveRules.validate("ANNUAL", LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 4), 3, 20).valid());
    }
}
