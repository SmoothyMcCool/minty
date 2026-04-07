# Task: Rename Data

## Identity
```
taskName:   "Rename Data"
numInputs:  1
numOutputs: 1
```

## What it does
Renames fields in the packet's `data` records according to a mapping.
Fields not listed in the mapping are kept with their original names.
The `text` field is passed through unchanged.

## Configuration

| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `Input Renamings` | string | yes | Comma-separated list of `oldName: newName` pairs |

### Format
```
oldFieldName: newFieldName, anotherOld: anotherNew
```
Whitespace around `:` and `,` is trimmed automatically.

## Configuration examples

```json
{ "Input Renamings": "customerId: id, customerName: name" }
```
```json
{ "Input Renamings": "A: Alpha, B: Beta, C: Gamma" }
```

## Example transformation
Config: `"A: Aa, B: Bb"`
Input data: `{ "A": "Antelope", "C": "Cantaloupe" }`
Output data: `{ "Aa": "Antelope", "C": "Cantaloupe" }` (C is unchanged, not in the mapping)

## Connections
- 1 input (index 0).
- 1 output (index 0).

## Step template

```json
{
  "taskName": "Rename Data",
  "stepName": "<descriptive label>",
  "id": 1,
  "loggingActive": true,
  "configuration": {
    "Input Renamings": "oldName: newName"
  },
  "layout": { "x": 0, "y": 0, "numInputs": 1, "numOutputs": 1 }
}
```

## Common uses
- Normalising field names before a downstream step that expects specific keys
- Mapping source field names to destination names before output formatting
