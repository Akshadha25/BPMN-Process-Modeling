package edu.srmist.outpass_workflow.worker;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OutpassWorkers {

    private static final Logger log = LoggerFactory.getLogger(OutpassWorkers.class);

    /**
     * Service task "Check request" (type check-outpass).
     * Reads : regNo, outDate, returnDate
     * Writes: nights, valid, approved (true only for a valid day-out, i.e. nights = 0)
     */
    @JobWorker(type = "check-outpass")
    public Map<String, Object> checkOutpass(@Variable String regNo,
                                            @Variable String outDate,
                                            @Variable String returnDate) {
        LocalDate out = LocalDate.parse(outDate);
        LocalDate back = LocalDate.parse(returnDate);

        long nights = ChronoUnit.DAYS.between(out, back);
        boolean valid = !back.isBefore(out);          // return before out date => invalid
        boolean approved = valid && nights == 0;      // day-out is auto-approved

        log.info("Checked out-pass for {}: nights={}, valid={}, auto-approved={}",
                regNo, nights, valid, approved);
        return Map.of("nights", nights, "valid", valid, "approved", approved);
    }

    /**
     * Service task "Issue out-pass" (type issue-outpass).
     * Reads : regNo, outDate, studentName, parentPhone, approved, wardenRemarks
     * Writes: passStatus (ISSUED / REJECTED), passNumber (only when ISSUED)
     */
    @JobWorker(type = "issue-outpass")
    public Map<String, Object> issueOutpass(@Variable String regNo,
                                            @Variable String outDate,
                                            @Variable String studentName,
                                            @Variable String parentPhone,
                                            @Variable Boolean approved,
                                            @Variable String wardenRemarks) {
        boolean issued = Boolean.TRUE.equals(approved);
        String passStatus = issued ? "ISSUED" : "REJECTED";

        Map<String, Object> result = new HashMap<>();
        result.put("passStatus", passStatus);
        if (issued) {
            result.put("passNumber", "OP-" + regNo + "-" + outDate);
        }

        log.info("SMS to parent {} ({}, {}): out-pass {}{}. Warden remarks: {}",
                parentPhone, studentName, regNo, passStatus,
                issued ? " (" + result.get("passNumber") + ")" : "",
                wardenRemarks);
        return result;
    }
}
