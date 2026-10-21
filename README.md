# Ex 6 – Start a Camunda 8 Process Using an API Call

**Course:** 21CSE735P – Workflow Automation Engineering | **Tools:** Camunda Modeler, Camunda 8 Run, Postman, Operate

A simple **Leave Request** process (Start Event → User Task **Approve Leave** → End Event, Process ID `leave-request`) is deployed to Camunda 8 Run from Modeler and started with a REST call from Postman.

## Files
- `leave-request.bpmn` – the process model
- `Ex6.postman_collection.json` – Postman collection (start instance, wrong-ID demo)
- `postman-request.txt` – the same request as plain text
- `screenshots/` – submission screenshots (below)

## Steps
1. Open `leave-request.bpmn` in Camunda Modeler. Confirm **Process ID = `leave-request`**, then **Deploy** to the local C8 Run cluster.
2. In Postman, send:
   ```
   POST http://localhost:8080/v2/process-instances
   Content-Type: application/json

   { "processDefinitionId": "leave-request",
     "variables": { "employeeName": "Ravi", "days": 3 } }
   ```
   The response contains the `processInstanceKey`. (If you get 401, use Basic Auth `demo` / `demo`.)
3. Open Operate (`http://localhost:8080/operate`): the instance is **Active**, waiting at *Approve Leave*, with `employeeName` and `days` visible.

## Screenshots

**1. BPMN diagram**

![BPMN diagram](screenshots/1-bpmn-diagram.png)

**2. Postman request and response (`processInstanceKey`)**

![Postman request and response](screenshots/2-postman-request-response.png)

**3. Operate – running instance at "Approve Leave" with both variables**

![Operate running instance](screenshots/3-operate-running-instance.png)

## Viva questions

**1. What is the difference between `processDefinitionId` and `processInstanceKey`?**
`processDefinitionId` is the Process ID of the model (`leave-request`). You set it in Modeler and every instance shares it, like a class. `processInstanceKey` is the unique number Camunda generates for one running instance, like an object. You start a process with the ID and find or act on one instance with the key.

**2. What response do you get if the Process ID is spelled wrong?**
**HTTP 404 Not Found** (`NOT_FOUND`) with a message like *"Expected to find process definition with process ID 'leave-reqest', but none found"*. No instance is created.

**3. Start the process 3 times. How many instances appear in Operate?**
**Three.** Each call creates a separate instance with its own `processInstanceKey`, and all three wait at *Approve Leave*.
