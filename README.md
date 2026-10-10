# Ex 5 – Hostel Out-Pass Request (Camunda 8 SaaS + Spring Boot)

A **Hostel Out-Pass Request** process runs on a Camunda 8 SaaS cluster; all business logic is in a Spring Boot app (deploys the model from code, starts instances over REST, runs service tasks as job workers, and uses a human task in Tasklist).

**Process logic**
1. A student submits a request with `POST /outpass` (`studentName`, `regNo`, `hostelBlock`, `outDate`, `returnDate`, `parentPhone`, `purpose`).
2. **Check request** (`check-outpass` worker) computes `nights` and sets `valid = false` if the return date is before the out date.
3. Gateway: invalid → reject (`=not(valid)`); day out → auto-approve (`=valid and nights = 0`); 1+ nights → **Warden approval** form in Tasklist (`=valid and nights >= 1`).
4. **Issue out-pass** (`issue-outpass` worker) creates pass number `OP-<regNo>-<outDate>` for approved requests and logs the "SMS to parent". The process ends with `passStatus = ISSUED` or `REJECTED`.

**Variables**

| Worker | Reads | Writes |
|---|---|---|
| `check-outpass` | `outDate`, `returnDate` (and `regNo` for the log) | `nights`, `valid`, `approved` |
| `issue-outpass` | `regNo`, `outDate`, `studentName`, `parentPhone`, `approved`, `wardenRemarks` | `passStatus`, `passNumber` |

The Warden form (`warden-approval-form`) writes `approved` and `wardenRemarks`.

**Files**
- `src/main/resources/bpmn/hostel-outpass.bpmn` – process (ID `hostel-outpass`)
- `src/main/resources/bpmn/warden-approval.form` – Warden form
- `src/main/resources/application.yaml` – SaaS config (reads env vars)
- `src/main/java/edu/srmist/outpass_workflow/` – application, `api/OutpassController`, `worker/OutpassWorkers`

**Run**
```bash
export CAMUNDA_CLIENT_ID=...  CAMUNDA_CLIENT_SECRET=...  CAMUNDA_CLUSTER_ID=...  CAMUNDA_CLUSTER_REGION=...
mvn spring-boot:run
```
Test (dates `yyyy-MM-dd`):
```bash
# day out -> auto-approved
curl -X POST localhost:8080/outpass -H "Content-Type: application/json" -d '{"studentName":"Priya","regNo":"RA2311003010001","hostelBlock":"A","outDate":"2026-10-20","returnDate":"2026-10-20","parentPhone":"9876543210","purpose":"Medical"}'
# 2 nights -> Warden approval in Tasklist (change returnDate to 2026-10-22)
# invalid -> rejected (returnDate before outDate)
```
