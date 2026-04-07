# Task: Loop

## Identity
```
taskName:   "Loop"
numInputs:  1
numOutputs: 2
```

## What it does
Evaluates a SpEL condition on each incoming packet and routes it to one of two outputs.
While the condition is true, the packet is sent to output 0 (continue looping).
When the condition is false, the packet is sent to output 1 (exit) and the task terminates.

Use Loop when you need a step to repeat — connect output 0 back to an earlier step to form
a cycle. The condition typically checks whether some target state has been reached, for example
whether an LLM response meets a quality threshold or a counter has reached its limit.
Output 1 is the exit path; connect it to the next step after the loop.

Loop is the only task designed for cycles. For a one-time conditional fork, use Branch instead.

## Configuration

| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `Branch Expression` | string | yes | A SpEL expression returning boolean. TRUE = keep looping, FALSE = exit. |

### SpEL expression context
In Loop, the packet itself is the root object — there is no `#p` variable.
Access fields directly by name:
- `data[0]['fieldName']` — value of a field in the first data record
- `data[0]['fieldName'] < 3` — numeric comparison
- `text[0]` — the first text string
- `text[0].length() < 500` — string operation on first text

Shorthand: `data.fieldName` is automatically expanded to `data[0].fieldName`.

Note: Loop uses the packet as root (no `#p`). Branch uses `#p` — they are different.

## Configuration examples

```json
{ "Branch Expression": "data[0]['retryCount'] < 3" }
```
```json
{ "Branch Expression": "data[0]['status'] != 'complete'" }
```
```json
{ "Branch Expression": "text[0].length() < 500" }
```
```json
{ "Branch Expression": "data.score > 0" }
```

## Connections
- 1 input (index 0).
- 2 outputs: output 0 = continue looping, output 1 = exit loop.
- Output 0 is typically connected back to an earlier step (creating the loop cycle).
- Output 1 connects to the next downstream step.

## Step template

```json
{
  "taskName": "Loop",
  "stepName": "<descriptive label>",
  "id": "1",
  "loggingActive": true,
  "configuration": {
    "Branch Expression": "<spel expression — true = keep looping>"
  },
  "layout": { "x": 0, "y": 0, "numInputs": 1, "numOutputs": 2 }
}
```
