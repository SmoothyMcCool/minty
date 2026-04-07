# Task: Sort

## Identity
```
taskName:   "Sort"
numInputs:  1
numOutputs: 1
```

## What it does
Buffers ALL incoming packets until the input stream is exhausted, then sorts them and emits them in order.

Sort is a blocking task — it will not emit anything until every upstream packet has arrived.
Sorting is done by comparing the specified field(s) as strings, in the order listed (primary sort first, then secondary, etc.).

## Configuration

| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `ID Element` | JSON string array | yes | List of packet field paths to sort by, in priority order |

The value must be a JSON array of strings, encoded as a string.

### Field path syntax
Uses the same path syntax as SetId. The `$` prefix is optional.

| Path | Sorts by |
|------|----------|
| `id` | The packet's id string |
| `data[0].fieldName` | A field in the first data record |
| `data.fieldName` | Shorthand for `data[0].fieldName` |

Sort compares values as strings. Numeric fields sort correctly only if zero-padded.

## Configuration example

Sort by packet ID:
```json
{ "ID Element": "[\"id\"]" }
```

Sort by a data field:
```json
{ "ID Element": "[\"data[0].category\"]" }
```

Sort by a data field, then by ID as tiebreaker:
```json
{ "ID Element": "[\"data.category\", \"id\"]" }
```

## Connections
- 1 input (index 0).
- 1 output (index 0).

## Step template

```json
{
  "taskName": "Sort",
  "stepName": "<descriptive label>",
  "id": "1",
  "loggingActive": true,
  "configuration": {
    "ID Element": "[\"id\"]"
  },
  "layout": { "x": 0, "y": 0, "numInputs": 1, "numOutputs": 1 }
}
```

## Common uses
- Sorting before GroupBy or Collect with `ById` mode (both require sorted input)
- Producing deterministic output order from a parallel processing step
