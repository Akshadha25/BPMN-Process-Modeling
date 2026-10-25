# Ex 7 – Leave Application System (Camunda 8 + Spring Boot + React + Docker)

An end-to-end leave workflow: an employee applies in a **React** portal, a **Spring Boot** backend stores the request and drives a **Camunda 8** BPMN process, a manager approves or rejects, and the final status and leave balance are updated. The whole system finally runs with **Docker Compose**.

## 1. What is required

The task is the five-step roadmap from the guides (Step 1 BPMN, Step 2 React, Step 3 Spring Boot, Step 4 Integration & Testing, Step 5 Docker) and the architecture diagram:

| Step | Requirement | Delivered in |
|---|---|---|
| 1 Design BPMN | `leave-request.bpmn` (Process ID `leave-request`): service tasks, user task, gateways, end events; `leave-approval.form` (keys `approved`, `managerComment`); define process variables | `bpmn/` (copy in `backend/src/main/resources/bpmn/`) |
| 2 React frontend | Vite + TypeScript; Apply form, My Requests, Manager Inbox; calls Spring Boot REST | `frontend/` |
| 3 Spring Boot backend | Spring Boot 3.5 + Java 21; REST API; Camunda client; job workers `validate-leave`, `finalize-leave`; H2/PostgreSQL; deploy BPMN + form on startup | `backend/` |
| 4 Integrate & test | Run all components, scenarios A–F, check Operate/Tasklist/H2 | Sections 5–6 below |
| 5 Docker | Dockerfile for backend and frontend, Camunda 8 Docker Compose, `docker-compose.yml`, run the complete system | `docker-compose.yml`, `camunda/`, `backend/Dockerfile`, `frontend/Dockerfile` |

## 2. Architecture

```
Browser ──HTTP──> React (Vite 5173 | Nginx 3000) ──/api──> Spring Boot (8081) ──gRPC 26500 / REST 8080──> Camunda 8
                                                                  │                                        (Zeebe + Operate + Tasklist + Elasticsearch)
                                                                  └──JPA──> H2 (local) / PostgreSQL 5432 (Docker)
```
The browser talks only to Spring Boot; React never calls Camunda directly.

| Component | Local development | Docker |
|---|---|---|
| React frontend | http://localhost:5173 | http://localhost:3000 |
| Spring Boot | http://localhost:8081 | http://localhost:8081 |
| Camunda Operate / Tasklist | http://localhost:8080/operate · `/tasklist` (login `demo` / `demo`) | same |
| Camunda REST / gRPC | 8080 / 26500 | same |
| Database | H2 file `./data/leavedb` (console `/h2-console`) | PostgreSQL on 5432 |

## 3. What was built

**Step 1 – BPMN (`bpmn/leave-request.bpmn`).** Start *Leave requested* → service task *Validate leave request* (`validate-leave`) → gateway *Request valid?* (`=valid` to manager; **default** flow to merge) → user task *Manager approval* (Camunda user task, form `leave-approval`) → gateway *Approved?* (`=approved` → *Finalize as approved*; **default** → merge) → merge gateway → *Finalize as rejected*. Both finalize tasks use type `finalize-leave` and an input mapping `outcome` = `"APPROVED"` / `"REJECTED"`. Two end events: *Leave approved*, *Leave rejected*. The form has a checkbox `approved`, a text area `managerComment`, and a read-only summary of the request.

| Variable | Set by | Used by |
|---|---|---|
| `requestId, employeeEmail, leaveType, days, startDate, endDate` | Spring Boot at process start | workers |
| `valid, validationMessage` | `validate-leave` worker | *Request valid?* gateway, rejection message |
| `approved, managerComment` | manager (React or Tasklist) | *Approved?* gateway, finalize worker |
| `outcome` | input mapping on each finalize task | `finalize-leave` worker |

**Step 2 – React (`frontend/`).** `Apply` (type, dates, reason), `My requests` (status badges, remaining annual leave, auto-refresh every 3 s) and `Manager inbox` (approve/reject with comment; retries HTTP 409 for a few seconds while Camunda indexes the new task). Vite proxies `/api` to port 8081. Code follows the Step 2 guide.

**Step 3 – Spring Boot (`backend/`).** Packages `controller`, `service`, `worker`, `model`, `repository` (as in the project-structure diagram).
- REST: `POST /api/leaves` (201), `GET /api/leaves?employeeEmail=`, `GET /api/leaves/pending`, `GET /api/leaves/balance?employeeEmail=`, `POST /api/leaves/{id}/decision` (204).
- `LeaveRules` – pure business rules: working days (Mon–Fri), no-working-days / end-before-start / insufficient *annual* balance checks.
- `WorkflowGateway` – the only class that calls Camunda (start process, find and complete the manager user task).
- `LeaveService` – saves the request **before** starting the process, validates, finalises, deducts annual leave only after approval, and is **idempotent** (a retried job never deducts twice); new employees get 20 annual days.
- `LeaveWorkers` – `@JobWorker` types `validate-leave` and `finalize-leave`.
- `@Deployment` deploys the BPMN and form on startup. Config reads DB and Camunda addresses from environment variables, so the same jar runs locally (H2) and in Docker (PostgreSQL).

**Step 5 – Docker.** `backend/Dockerfile` (Maven build → JRE 21), `frontend/Dockerfile` (Node build → Nginx, proxies `/api` to the backend), `camunda/docker-compose.yml` (adapted from the official Camunda 8.8 compose: Zeebe + Operate + Tasklist + Elasticsearch) and the root `docker-compose.yml`, which includes it and adds PostgreSQL, backend and frontend.

## 4. Project structure

```
Ex7/
├── bpmn/                       leave-request.bpmn, leave-approval.form
├── camunda/                    docker-compose.yml, .env, config/application.yaml, data/{elasticsearch,zeebe}/
├── backend/                    pom.xml, Dockerfile, src/main/java/com/example/leave/{controller,service,worker,model,repository}
│                               src/main/resources/{application.yml, bpmn/}
├── frontend/                   package.json, vite.config.ts, Dockerfile, nginx.conf, src/{api.ts,App.tsx,ApplyForm.tsx,MyRequests.tsx,ManagerInbox.tsx,App.css,main.tsx}
├── docker-compose.yml          full system (frontend + backend + PostgreSQL + Camunda)
└── README.md
```

## 5. Run it

**Prerequisites:** Docker (Compose v2.20+), Java 21, Maven 3.9+, Node 20+.

### A. Development mode (Step 4) – 3 terminals
```bash
# 1. Camunda
cd camunda && docker compose up -d && docker compose ps      # wait until orchestration is healthy
# 2. Backend (H2)
cd backend && mvn spring-boot:run                            # check Operate for the process "Leave request"
# 3. Frontend
cd frontend && npm install && npm run dev                    # open http://localhost:5173
```
*Linux only:* if Elasticsearch cannot write to `camunda/data`, run `chmod -R 777 camunda/data` once.

### B. Full Docker deployment (Step 5)
Stop mode A first (same ports), then from the project root:
```bash
docker compose up --build
```
Open **http://localhost:3000**. First start takes a few minutes (image downloads, Maven/npm builds, Camunda startup).

## 6. Test scenarios (Step 4)

Use future weekday dates (2 Nov 2026 is a Monday). Use the UI, or curl against `http://localhost:8081`:

```bash
curl -X POST http://localhost:8081/api/leaves -H "Content-Type: application/json" \
  -d '{"employeeName":"Alice","employeeEmail":"alice@example.com","leaveType":"ANNUAL","startDate":"2026-11-02","endDate":"2026-11-04","reason":"Personal leave"}'
```

| Test | Steps | Expected | Pass? |
|---|---|---|---|
| A. Approved | Alice, ANNUAL, 2–4 Nov (3 days). Manager inbox → comment → **Approve** | `APPROVED`; balance 20 → 17 | ☐ |
| B. Manager rejection | Alice, another valid request → **Reject** with comment | `REJECTED`; balance unchanged | ☐ |
| C. Insufficient balance | `bob@example.com`, ANNUAL, 2 Nov – 4 Dec (25 days) | Rejected before the manager; message *Insufficient annual leave balance…* | ☐ |
| D. Weekend only | Saturday 7 Nov → Sunday 8 Nov | Rejected: *no working days* | ☐ |
| E. Tasklist approval | Valid request → http://localhost:8080/tasklist → claim → tick *Approve this request* → Complete | Same instance completes, status `APPROVED` | ☐ |
| F. Invalid dates (API) | POST with `startDate` 2026-11-05, `endDate` 2026-11-02 | Process rejects it; no balance change | ☐ |

Then check **Operate** (Processes → *Leave request* → instance: path on the diagram and variables `requestId, valid, validationMessage, approved, managerComment, outcome`), and the database (H2 console `http://localhost:8081/h2-console`, JDBC `jdbc:h2:file:./data/leavedb`, user `sa`, empty password; in Docker: `docker exec -it leave-db psql -U leave -d leavedb`).

**Acceptance checklist:** ☐ all components start ☐ process visible in Operate ☐ React submits and lists requests ☐ validation and gateways route correctly ☐ valid requests wait at *Manager approval* ☐ approve/reject complete the task ☐ finalize updates status and balance ☐ invalid requests never reach the manager ☐ no open incidents ☐ DB, UI and process variables agree.

## 7. Decisions and differences from the guides

- **Camunda version:** the guides mention both 8.8.9 and 8.9.12 (and two different starter artifacts). I used the **8.8 line** end to end: `camunda-spring-boot-starter` 8.8.15 and Camunda server images 8.8.43 (same minor, as the guides require). Change `camunda.version` and `camunda/.env` together if your cluster differs.
- **Port 8080:** the official 8.8 compose publishes the UI on 8088; I mapped it to **8080** so all URLs in the guides work. Connectors were dropped (unused).
- **Packages** follow the structure diagram (`controller/service/worker/model/repository`) rather than the `domain/api` names in the Step 3 text; Lombok is not used (plain getters/setters).
- **PostgreSQL** is used in Docker (per the architecture diagram), H2 locally; no code change, only environment variables.
- Camunda calls are isolated in `WorkflowGateway` so any client-API difference between versions is a one-file fix.

## 8. Verification status

Checked here: the BPMN and form (structure, flow references, default flows); all YAML files; the **frontend builds** (`tsc` + `vite build`, strict TypeScript); the **business rules run** (working days, weekend, insufficient balance, invalid dates; all pass); the backend Java files are syntactically valid and the record DTOs compile.
**Not run here** (no Docker/Maven Central access in the build environment): the full Maven build against the Camunda starter, the containers, and the live end-to-end scenarios. Run `mvn clean package` and the scenarios above on your machine; if a Camunda client import differs for your starter version, the only place to adjust is `WorkflowGateway.java`.

## 9. Security note

This is a learning MVP with **no authentication or role checks** – anyone who can reach the decision endpoint can act as manager. Use test data only and do not expose it to a network. Local Camunda runs with `unprotectedApi`; use real credentials, secrets management and PostgreSQL backups for anything beyond a demo.

## 10. Summary

Delivered a working-by-design, five-part leave system: a Camunda 8 BPMN process with two service-task types, a manager user task with a form, default-flow rejection handling and two outcomes; a React portal (Apply, My Requests, Manager Inbox); a Spring Boot backend with REST API, JPA persistence, idempotent job workers and automatic deployment; integration test scenarios A–F; and a Docker Compose setup that runs frontend (3000), backend (8081), PostgreSQL (5432) and Camunda 8 (8080/26500) together.
