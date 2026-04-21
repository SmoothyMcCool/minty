# Task: Format Text

## Identity
```
taskName:   "Format Text"
numInputs:  1
numOutputs: 1
```

## What it does
Renders a template string by substituting values from the input packet's data and text.

Template placeholders use the syntax `{path.to.value}` where the path navigates the packet's JSON structure.
The rendered result is added to the output packet's `text` list.
Even if the input `data` contains an array of items, only one rendered string is produced - but the template can reference different array indices.

## Configuration

| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `Format` | string | yes | The template string with `{...}` substitution placeholders |

### Placeholder syntax
Placeholders use `{path}` where path follows the same rules as SetId path expressions.

| Placeholder | Inserts |
|-------------|---------|
| `{id}` | The packet's id string |
| `{text}` | All text entries joined by newline |
| `{text[0]}` | The first text string |
| `{data[0].fieldName}` | Value of a field in the first data record |
| `{data.fieldName}` | Shorthand for `{data[0].fieldName}` |
| `{data[1].fieldName}` | Value of a field in the second data record |

## Configuration examples

```json
{
  "Format": "Customer {data[0].name} (ID: {data[0].customerId}) has status: {data[0].status}"
}
```
```json
{
  "Format": "Summary for {id}:\n{text[0]}"
}
```
```json
{
  "Format": "Score: {data.score} - Category: {data.category}"
}
```

## Output
- A copy of the input packet with one additional entry appended to `text`, containing the rendered string.

## Connections
- 1 input (index 0).
- 1 output (index 0).

## Step template

```json
{
  "taskName": "Format Text",
  "stepName": "<descriptive label>",
  "id": "1",
  "loggingActive": true,
  "configuration": {
    "Format": "<your template string>"
  },
  "layout": { "x": 0, "y": 0, "numInputs": 1, "numOutputs": 1 }
}
```

## Common uses
- Constructing a prompt string from structured data before a Query LLM step
- Formatting a human-readable summary from data fields
