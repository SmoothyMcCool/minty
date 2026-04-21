# Task: SetId

## Identity
```
taskName:   "SetId"
numInputs:  1
numOutputs: 1
```

## What it does
Sets the packet's `id` field by extracting a value from the packet's data or text using a path expression.
The packet is then forwarded unchanged (except for the updated ID) to the output.

If the specified path cannot be resolved, a warning is logged and the ID is left unmodified.

## Configuration

| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `ID Element` | string | yes | A path expression pointing to the field whose value should become the packet ID |

### Path expression syntax
Paths use JSONPath notation. The `$` prefix is optional - it is added automatically.

| Path | Returns |
|------|---------|
| `id` | The packet's current id string |
| `text` | All text entries joined by newline |
| `data[0].fieldName` | Value of a field in the first data record |
| `data.fieldName` | Shorthand for `data[0].fieldName` |
| `text[0]` | The first text string |

## Configuration examples

```json
{ "ID Element": "data[0].customerId" }
```
```json
{ "ID Element": "data.pageId" }
```
```json
{ "ID Element": "text[0]" }
```

## Connections
- 1 input (index 0).
- 1 output (index 0).

## Step template

```json
{
  "taskName": "SetId",
  "stepName": "<descriptive label>",
  "id": "1",
  "loggingActive": true,
  "configuration": {
    "ID Element": "data[0].fieldName"
  },
  "layout": { "x": 0, "y": 0, "numInputs": 1, "numOutputs": 1 }
}
```

## Common uses
- Assigning meaningful IDs to packets before Sort or GroupBy (both operate on packet IDs)
- Extracting a business key from data to use as the packet identifier
