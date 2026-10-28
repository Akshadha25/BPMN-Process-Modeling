# Exercise 8 — Hospital Appointment Booking (Camunda 8)

A hospital appointment-booking process built from a **parent process**, a **reusable child process**
(`send-notification`) called through **call activities**, a **DMN decision** (`slot_decision`) and an
**event subprocess** for cancellation at any time.

![Parent process](diagrams/parent_process.png)
![Child process](diagrams/send_notification.png)

## Repository structure

```
exercise-8-appointment-booking/
├── README.md
├── models/
│   ├── send-notification.bpmn               # child process (start → REST connector → end)
│   ├── slot_decision.dmn                    # DMN, hit policy UNIQUE
│   ├── hospital-booking-propagate-all.bpmn  # parent, variant A: propagate ALL variables
│   └── hospital-booking-mapped-only.bpmn    # parent, variant B: propagation OFF, mapped only
├── tests/                                   # JSON payloads for the three test cases
├── diagrams/                                # PNG previews (render_diagrams.py)
├── screenshots/                             # Operate screenshots (see screenshots/README.md)
└── render_diagrams.py
```

## 1. Child process — `send-notification`

Start event → **REST Outbound Connector** task → end event.

| Setting | Value |
|---|---|
| Process id | `send-notification` |
| Connector | REST Outbound Connector (`io.camunda:http-json:1`) |
| Method / URL | `POST` `https://api.telegram.org/bot{{secrets.TELEGRAM_BOT_TOKEN}}/sendMessage` |
| Body (FEEL) | `={chat_id: recipientId, text: messageText}` |
| Inputs it needs | `recipientId`, `messageText` |
| Result | `telegramResponse` (full response), plus `notificationStatus` and `telegramMessageId` |

Telegram expects the bot token inside the URL path, so the secret `{{secrets.TELEGRAM_BOT_TOKEN}}` is
part of the URL rather than a bearer token.

**Set up the secret and bot before running**
1. Create a bot with `@BotFather` and copy its token.
2. Send any message to your bot once, so it is allowed to message you.
3. Find your numeric chat id (for example with `@userinfobot`). **Use it as `patientId`** in the tests,
   because `patientId` becomes `recipientId` and then Telegram's `chat_id`. The test files use
   `9004121457`; replace it with your own chat id (in all four JSON files, including the cancel message).
4. Add the secret named `TELEGRAM_BOT_TOKEN`: in SaaS under *Console → your cluster → Connector secrets*;
   in self-managed or C8 Run, as an environment variable of the connectors runtime.

If the secret or chat id is wrong, the connector raises an incident in the **child** instance. That is
useful to know, but the tests below then stop at the call activity.

## 2. DMN — `slot_decision` (hit policy UNIQUE)

Inputs: `department` (string), `bookedCount` (number). Outputs: `slotStatus`, `suggestedAction`.

| department | bookedCount | slotStatus |
|---|---|---|
| Cardiology | < 10 / >= 10 | AVAILABLE / **FULL** |
| Neurology | < 8 / >= 8 | AVAILABLE / FULL |
| Orthopedics | < 15 / >= 15 | AVAILABLE / FULL |
| any other department | < 20 / >= 20 | AVAILABLE / FULL |

`suggestedAction` is "Confirm booking" when AVAILABLE and "Offer next available slot or join waiting
list" when FULL. The rules never overlap, which UNIQUE requires.

## 3. Parent process

| Element | Type | Notes |
|---|---|---|
| Booking requested | Start event | Variables come from the start payload |
| Check slot | Business Rule Task | `slot_decision` → result variable `slotDecision` |
| Slot available? | Exclusive gateway | `AVAILABLE`: `=slotDecision.slotStatus = "AVAILABLE"`, `FULL`: `= ... = "FULL"` |
| Book slot | Script task | Sets `bookingStatus = "BOOKED"` (no worker needed) |
| Send booking confirmation | Call activity | Calls `send-notification` |
| Wait 15 min (demo timer) | Intermediate timer event | `PT15M` after the confirmation; a booking is in progress during this wait |
| Patient attends appointment | User task | Keeps the booking "in progress" so a cancellation can arrive |
| Send slot-full notice | Call activity | FULL branch, then end "Booking rejected: slot full" |
| **Handle cancellation** | **Event subprocess** | Interrupting message start `cancel_booking`, correlation key `=patientId` |
| Release slot | Script task | Sets `releaseStatus = "RELEASED"` |
| Send cancellation notice | Call activity | Calls `send-notification` again |

*Book slot* and *Release slot* are script tasks so the exercise runs without writing job workers. In a
real hospital system you would replace them with service tasks backed by your booking system.

**About the timer.** It is a plain 15-minute duration that starts when the confirmation has been sent. It is not
tied to `preferredSlot`. That keeps the exercise testable. For a real system that waits until 15 minutes after the
appointment, change the timer to a *Date* and use a FEEL expression such as
`=date and time(replace(preferredSlot, " ", "T") + ":00+05:30") + duration("PT15M")`
(this needs `preferredSlot` in the form `2026-11-03 09:00` and an appointment in the future).
To test faster, change `PT15M` to `PT1M` in Modeler before deploying.

The event subprocess is **interrupting**: when the patient cancels, the waiting timer or user task is
cancelled and the process continues only on the cancellation path. For a non-interrupting version, untick
*Interrupting* on the message start event; the main flow would then continue as well.

## 4. Variable propagation — the comparison

On every call activity, the input mappings are the same in both variants:

| Target in child | Source in parent |
|---|---|
| `recipientId` | `=patientId` |
| `messageText` | FEEL text built from `patientName`, `department`, `preferredSlot` (and `slotDecision.suggestedAction` for FULL) |

| | **A: propagate all** (`hospital-booking-propagate-all.bpmn`) | **B: mapped only** (`hospital-booking-mapped-only.bpmn`) |
|---|---|---|
| Call activity setting | `propagateAllParentVariables="true"`, `propagateAllChildVariables="true"` | both `"false"` |
| What the child can see | Everything in the parent (`patientName`, `department`, `bookedCount`, `slotDecision`, …) plus `recipientId`, `messageText` | Only `recipientId` and `messageText` |
| What comes back to the parent | Every child variable: `recipientId`, `messageText`, `telegramResponse`, `notificationStatus`, `telegramMessageId` | Only what the output mapping names: `notificationStatus` → `bookingNotificationStatus` / `fullNotificationStatus` / `cancelNotificationStatus` |
| Risk | Child variables can overwrite parent ones (for example `messageText` and `notificationStatus` are replaced on each call); the full Telegram response leaks into the parent | None of that; each call has its own named result |
| Effort | Least configuration | One output mapping per call activity |

Variant B is the safer default for a reusable child process. Variant A is convenient for quick
prototypes. **Fill in your own observations in the table at the end after running both.**

> Test one variant at a time. Both use the message name `cancel_booking`, so finish or cancel the
> waiting instances of one variant before starting the other.

## 5. Deploy and test

1. Open the models in Camunda Modeler. In Web Modeler, import all four into one project.
2. **Deploy together:** `send-notification.bpmn`, `slot_decision.dmn` and the parent variant you are testing.
3. **Start instances** of `hospital-appointment-booking` (variant A) or `hospital-appointment-booking-mapped`
   (variant B), pasting the JSON from `tests/` as variables. Make sure `patientId` is your Telegram chat id first.

| # | Payload | Expected result |
|---|---|---|
| 1 | `tests/case1-cardiology-3-booking-succeeds.json` | DMN → AVAILABLE → *Book slot* → child instance sends the confirmation → token waits on the *Wait 15 min* timer, then on *Patient attends appointment* |
| 2 | `tests/case2-cardiology-12-slot-full.json` | DMN → FULL → child sends the "slot full" message → ends at *Booking rejected: slot full* |
| 3 | `tests/case3-booking-then-cancel.json`, then the cancellation message | Booking as in case 1, then the cancel message interrupts the wait → *Release slot* → child sends the cancellation message → *Booking cancelled* |

**Run order:** case 2 (ends at once) → case 1 (take screenshots while it waits) → finish case 1, either by
cancelling the instance in Operate or by waiting for the timer and completing *Patient attends appointment*
in Tasklist → case 3 and its cancellation message.
A message is correlated to only one waiting instance per process and key, and all cases share one
`patientId`, so no other instance of that process should be waiting during case 3. Publish the cancel message
while case 3 is still at the timer or the user task.

**Publish the cancellation message** (from Operate you cannot publish messages). Self-managed / C8 Run, 8.8+ REST API
(check the API docs of your version; SaaS needs an API client token instead of `-u`):

```bash
curl -u demo:demo -X POST http://localhost:8080/v2/messages/publication \
  -H "Content-Type: application/json" -d @tests/cancel-message.json
```

or with the CLI: `zbctl publish message cancel_booking --correlationKey 9004121457`.

## 6. Observe in Operate

Capture the screenshots listed in [`screenshots/README.md`](screenshots/README.md): the parent instance,
the child instance reached from the call activity, and the cancellation path. Then add them here:

```
![Parent instance](screenshots/01-case1-parent-instance.png)
![Child instance](screenshots/02-case1-child-instance.png)
![Cancellation path](screenshots/04-case3-cancellation-path.png)
```

### My observations (fill in after running)

| Check | Variant A (propagate all) | Variant B (mapped only) |
|---|---|---|
| Variables visible in child instance | | |
| Variables added to parent after the call | | |
| Anything overwritten? | | |

## Publishing to its own GitHub branch

```bash
git checkout -b exercise-8-appointment-booking
git add exercise-8-appointment-booking/
git commit -m "Exercise 8: appointment booking with call activity, DMN and cancellation event subprocess"
git push -u origin exercise-8-appointment-booking
```
