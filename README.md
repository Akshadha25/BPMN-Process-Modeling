# Ex 6 – Start a Camunda 8 Process Using an API Call

A simple **Leave Request** process (Start Event → User Task **Approve Leave** → End Event, Process ID `leave-request`) deployed to Camunda 8 Run from Modeler and started with a REST call from Postman.

**Files**
- `leave-request.bpmn` – the process
- `Ex6.postman_collection.json` – Postman requests (start instance, and a wrong-ID demo)

**Steps**
1. Open `leave-request.bpmn` in Camunda Modeler, confirm Process ID = `leave-request`, and deploy to the local cluster.
2. In Postman:
   ```
   POST http://localhost:8080/v2/process-instances
   Content-Type: application/json

   { "processDefinitionId": "leave-request",
     "variables": { "employeeName": "Ravi", "days": 3 } }
   ```
   The response contains the `processInstanceKey`. (If you get 401, use Basic Auth `demo` / `demo`.)
3. In Operate, the instance is **Active**, waiting at *Approve Leave*, with `employeeName` and `days` visible.
