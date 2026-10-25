package com.example.leave.service;

import java.time.DayOfWeek;
import java.time.LocalDate;

/** Pure business rules (no Spring, no database) so they are easy to unit-test. */
public final class LeaveRules {

    /** Example entitlement created the first time an employee is seen. */
    public static final int DEFAULT_ANNUAL_DAYS = 20;

    private LeaveRules() {}

    /** Result of the validation step: {@code valid} plus a message shown to the employee. */
    public record Result(boolean valid, String message) {}

    /** Number of working days (Mon-Fri) from start to end, both included. 0 if the range is empty/invalid. */
    public static int workingDays(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            return 0;
        }
        int days = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            DayOfWeek day = d.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                days++;
            }
        }
        return days;
    }

    /** Validation used by the validate-leave worker. Only ANNUAL leave is checked against the balance. */
    public static Result validate(String leaveType, LocalDate start, LocalDate end, int days, int remainingDays) {
        if (start == null || end == null) {
            return new Result(false, "Start and end dates are required");
        }
        if (end.isBefore(start)) {
            return new Result(false, "End date is before the start date");
        }
        if (days <= 0) {
            return new Result(false, "The selected period contains no working days");
        }
        if ("ANNUAL".equalsIgnoreCase(leaveType) && days > remainingDays) {
            return new Result(false, "Insufficient annual leave balance: requested " + days
                    + " day(s), remaining " + remainingDays);
        }
        return new Result(true, "OK");
    }
}
