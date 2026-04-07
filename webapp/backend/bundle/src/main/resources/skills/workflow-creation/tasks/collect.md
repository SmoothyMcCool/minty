# Task: Collect

## Identity
```
taskName:   "Collect"
numInputs:  1
numOutputs: 1
```

## What it does
Accumulates a stream of incoming packets into a single combined packet by appending
their `text` and `data` arrays together. Emits one packet when the stream ends.

Use Collect to reassemble packets after a Split → process fan-out pattern. It is the natural
pair to Split: Split expands one packet into many, Collect gathers many back into one.

Two grouping modes: `All` waits for the entire stream then emits once. `ById` emits a combined
packet each time the packet ID changes — useful for processing pre-sorted groups. Use `ById`
mode only when input is sorted by ID (add a Sort step upstream if needed).

## Configuration

| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `Grouping` | enum string | yes | `"All"` or `"ById"` |

## Configuration examples

```json
{ "Grouping": "All" }
```
```json
{ "Grouping": "ById" }
```

## Behaviour detail
- `All` mode: the task waits until all upstream data has arrived, then emits one combined packet.
- `ById` mode: emits a combined packet each time the packet ID changes. Input must be sorted by ID — add a Sort step upstream if needed.

## Connections
- 1 input (index 0).
- 1 output (index 0).

## Step template

```json
{
  "taskName": "Collect",
  "stepName": "<descriptive label>",
  "id": "1",
  "loggingActive": true,
  "configuration": {
    "Grouping": "All"
  },
  "layout": { "x": 0, "y": 0, "numInputs": 1, "numOutputs": 1 }
}
```

## Common uses
- Gathering all results from a Split → process pipeline back into one packet
- Grouping sorted records by ID (use `ById` mode after Sort + SetId)
