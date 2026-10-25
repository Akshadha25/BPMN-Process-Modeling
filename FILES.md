# Ex 7 – File reference

Every file in the project and what it is for.

| File | Purpose |
|---|---|
| `README.md` | Project guide: requirements, what was built, architecture, run instructions, test scenarios, decisions, verification status, summary |
| `FILES.md` | This file: description of every file |
| `.gitignore` | Keeps build output, node_modules, local DB files and Camunda data out of Git |
| `docker-compose.yml` | Step 5: runs the whole system (frontend, backend, PostgreSQL + Camunda via include). Command: docker compose up --build |
| `bpmn/leave-request.bpmn` | Step 1: the BPMN process (ID leave-request): validate -> valid? -> manager approval -> approved? -> finalize approved / merge -> finalize rejected |
| `bpmn/leave-approval.form` | Step 1: manager form (ID leave-approval): checkbox 'approved', text area 'managerComment', request summary |
| `camunda/docker-compose.yml` | Camunda 8.8 Self-Managed (orchestration = Zeebe+Operate+Tasklist, plus Elasticsearch); UI/REST on 8080, gRPC 26500 |
| `camunda/.env` | Camunda and Elasticsearch image versions used by the compose file |
| `camunda/config/application.yaml` | Camunda server config mounted into the container (open API for local dev, demo/demo login, Elasticsearch storage) |
| `camunda/data/zeebe/.gitkeep` | Keeps the Zeebe data folder in Git (bind mount target) |
| `camunda/data/elasticsearch/.gitkeep` | Keeps the Elasticsearch data folder in Git (bind mount target) |
| `backend/pom.xml` | Maven build: Spring Boot 3.5.6, Java 21, Web, JPA, Validation, H2, PostgreSQL, Camunda starter 8.8.15, test |
| `backend/Dockerfile` | Multi-stage image: Maven build, then JRE 21 runtime on port 8081 |
| `backend/.dockerignore` | Excludes target/ and local data from the backend image build |
| `backend/src/main/resources/application.yml` | Port 8081, database (H2 by default, env vars for PostgreSQL), Camunda addresses (env overridable) |
| `backend/src/main/resources/bpmn/leave-request.bpmn` | Copy of the process; deployed to Camunda on startup |
| `backend/src/main/resources/bpmn/leave-approval.form` | Copy of the manager form; deployed to Camunda on startup |
| `backend/src/main/java/com/example/leave/LeaveApplication.java` | Spring Boot main class with @Deployment of the BPMN and form |
| `backend/src/main/java/com/example/leave/controller/LeaveController.java` | REST API: POST /api/leaves, GET /api/leaves, /pending, /balance, POST /{id}/decision |
| `backend/src/main/java/com/example/leave/controller/SubmitLeaveRequest.java` | Validated request body for submitting leave |
| `backend/src/main/java/com/example/leave/controller/DecisionRequest.java` | Manager decision body (approved, comment) |
| `backend/src/main/java/com/example/leave/service/LeaveService.java` | Core logic: submit (save then start process), decide (complete manager task), validate, finalize (idempotent), balance |
| `backend/src/main/java/com/example/leave/service/LeaveRules.java` | Pure business rules: working days, no-working-days / end-before-start / insufficient annual balance |
| `backend/src/main/java/com/example/leave/service/WorkflowGateway.java` | The only class calling Camunda: start the process, find and complete the manager user task |
| `backend/src/main/java/com/example/leave/worker/LeaveWorkers.java` | Job workers for task types validate-leave and finalize-leave |
| `backend/src/main/java/com/example/leave/model/LeaveRequest.java` | JPA entity: a leave request with status, message and process instance key |
| `backend/src/main/java/com/example/leave/model/LeaveBalance.java` | JPA entity: remaining annual days per employee email |
| `backend/src/main/java/com/example/leave/model/LeaveStatus.java` | Enum: SUBMITTED, PENDING_APPROVAL, APPROVED, REJECTED |
| `backend/src/main/java/com/example/leave/repository/LeaveRequestRepository.java` | Spring Data repository (by email, by status, update only the process instance key) |
| `backend/src/main/java/com/example/leave/repository/LeaveBalanceRepository.java` | Spring Data repository for balances |
| `backend/src/test/java/com/example/leave/LeaveRulesTest.java` | JUnit tests for the business rules (scenarios A, C, D, F) |
| `frontend/package.json` | Frontend dependencies and scripts (dev, build) |
| `frontend/package-lock.json` | Locked dependency versions (used by npm ci in Docker) |
| `frontend/tsconfig.json` | TypeScript settings (strict) |
| `frontend/vite.config.ts` | Vite config: port 5173, proxies /api to http://localhost:8081 |
| `frontend/index.html` | HTML entry page for the React app |
| `frontend/Dockerfile` | Multi-stage: Node builds the app, Nginx serves it |
| `frontend/nginx.conf` | Nginx config: serves the app and proxies /api to the backend container |
| `frontend/.dockerignore` | Excludes node_modules and dist from the image build |
| `frontend/src/main.tsx` | React entry point |
| `frontend/src/vite-env.d.ts` | Vite type declarations |
| `frontend/src/App.tsx` | App shell: name/email, tabs Apply / My requests / Manager inbox |
| `frontend/src/ApplyForm.tsx` | Employee leave application form |
| `frontend/src/MyRequests.tsx` | Employee's requests with status badges and remaining balance (auto-refresh) |
| `frontend/src/ManagerInbox.tsx` | Pending requests with comment and Approve / Reject |
| `frontend/src/api.ts` | Typed API client for the Spring Boot endpoints (retries HTTP 409 on decisions) |
| `frontend/src/App.css` | Styles for the portal |
