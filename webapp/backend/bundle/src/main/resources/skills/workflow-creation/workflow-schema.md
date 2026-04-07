# Workflow JSON schema

## Top-level structure

```json
{
  "id": "",
  "owned": true,
  "name": "<workflow name>",
  "description": "<optional>",
  "steps": [ ],
  "connections": [ ],
  "outputStep": { }
}
```

---

## Step object (TaskRequest)

```json
{
  "taskName": "<exact registered name — see task file>",
  "stepName": "<unique human label for this step>",
  "id": "<unique string — e.g. \"1\", \"2\", \"3\">",
  "loggingActive": true,
  "configuration": { },
  "layout": {
    "x": 0,
    "y": 0,
    "numInputs": <integer — must match task declaration>,
    "numOutputs": <integer — must match task declaration>
  }
}
```

`layout.x` and `layout.y` are display positions only. Set them to 0 if not specified.

---

## Connection object

```json
{
  "writerId": "<id of the upstream step — the step sending data>",
  "writerPort": <zero-based output index on the upstream step>,
  "readerId": "<id of the downstream step — the step receiving data>",
  "readerPort": <zero-based input index on the downstream step>
}
```

### How data flows
- `writerId` / `writerPort` — the upstream step — which output it sends from.
- `readerId` / `readerPort` — the downstream step — which input it receives on.
- One connection = one directed edge from one step's output to another step's input.
- A step waits for data on each of its inputs. When all upstream steps finish sending, the step completes.

### Port indexing
- Ports are zero-based.
- A step with `numOutputs: 2` has outputs `0` and `1`.
- A step with `numInputs: 1` has only input `0`.
- Never use an index >= the step's numInputs or numOutputs.

### Unconnected inputs and outputs
- An unconnected input receives a NullConnector — the step sees it as immediately terminated (empty).
- An unconnected output is a NullConnector — data written there is discarded.
- Steps with no inputs (`numInputs: 0`) start immediately when the workflow runs.

### Concurrency
- All steps run concurrently. There is no sequential ordering outside of what connections enforce.
- A step starts as soon as its upstream steps begin sending data.
- Topology must be a DAG (no cycles).

---

## outputStep

The `outputStep` is a single task that formats the final `ExecutionResult` after all steps complete.
It is NOT listed in the `steps` array. It is a separate top-level field.
Use `JSON Output` unless another output format is needed.

```json
"outputStep": {
  "taskName": "JSON Output",
  "stepName": "JSON Output",
  "id": "1",
  "loggingActive": true,
  "configuration": {},
  "layout": { "x": 0, "y": 0, "numInputs": 0, "numOutputs": 0 }
}
```

---

## Generation checklist

Before finalising the JSON, verify:

- [ ] Every `writerId` and `readerId` references a step `id` in `steps`
- [ ] No `writerPort` >= that step's `numOutputs`
- [ ] No `readerPort` >= that step's `numInputs`
- [ ] No step with `numInputs: 0` appears as a `readerId`
- [ ] All `stepName` values are unique
- [ ] `outputStep` is NOT inside the `steps` array
- [ ] All configuration values are strings (even numbers)
- [ ] Every step has a unique `id` (use simple strings like "1", "2", "3")
- [ ] `layout.numInputs` and `layout.numOutputs` match the task's declared values

---

## Worked example

**Request**: "Emit test records, remove null fields, then log the result."

**Plan**:
- Emit Packet (id: `1`) — no inputs, 1 output
- Transform (id: `2`) — remove nulls, 1 input, 1 output
- Log Packets (id: `3`) — pass-through, 1 input, 1 output

**Data flow**: 1 → 2 → 3

**Connections**:
- writerId: 1, writerPort: 0 → readerId: 2, readerPort: 0
- writerId: 2, writerPort: 0 → readerId: 3, readerPort: 0

```json
{
  "id": "",
  "owned": true,
  "name": "null removal example",
  "description": "",
  "steps": [
    {
      "taskName": "Emit Packet",
      "stepName": "Source data",
      "id": "1",
      "loggingActive": true,
      "configuration": {
        "Data to Emit": "[{\"id\":\"1\",\"text\":[],\"data\":[{\"value\":null,\"name\":\"Alice\"}]}]"
      },
      "layout": { "x": 0, "y": 0, "numInputs": 0, "numOutputs": 1 }
    },
    {
      "taskName": "Transform",
      "stepName": "Remove nulls",
      "id": "2",
      "loggingActive": true,
      "configuration": {
        "Definition": "{\"operations\":[{\"name\":\"Remove Null Fields\",\"configuration\":null}]}"
      },
      "layout": { "x": 0, "y": 100, "numInputs": 1, "numOutputs": 1 }
    },
    {
      "taskName": "Log Packets",
      "stepName": "Log output",
      "id": "3",
      "loggingActive": true,
      "configuration": {},
      "layout": { "x": 0, "y": 200, "numInputs": 1, "numOutputs": 1 }
    }
  ],
  "connections": [
    { "writerId": "1", "writerPort": 0, "readerId": "2", "readerPort": 0 },
    { "writerId": "2", "writerPort": 0, "readerId": "3", "readerPort": 0 }
  ],
  "outputStep": {
    "taskName": "JSON Output",
    "stepName": "JSON Output",
    "id": "4",
    "loggingActive": true,
    "configuration": {},
    "layout": { "x": 0, "y": 0, "numInputs": 0, "numOutputs": 0 }
  }
}
```
