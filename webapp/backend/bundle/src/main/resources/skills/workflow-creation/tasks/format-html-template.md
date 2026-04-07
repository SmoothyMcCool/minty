# Task: Format Html (Template)

## Identity
```
taskName:   "Format Html (Template)"
numInputs:  1
numOutputs: 1
```

## What it does
Renders a pre-built system Pug template using the input packet as context.
Templates are selected by name from a system-defined list.
Produces an HTML string stored in the output packet's `text` field.

Use this when you want a built-in template rather than writing your own Pug.
To supply your own Pug template, use Format Html instead.

## Configuration

| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `Inline Template` | string | yes | The name of a pre-built system template (shown as a dropdown in the UI) |

## Configuration example

```json
{
  "Inline Template": "standard-report"
}
```

## Output
- `id`: copied from the input packet
- `text`: single-element list containing the rendered HTML string

## Connections
- 1 input (index 0).
- 1 output (index 0).

## Step template

```json
{
  "taskName": "Format Html (Template)",
  "stepName": "<descriptive label>",
  "id": "1",
  "loggingActive": true,
  "configuration": {
    "Inline Template": "<template name>"
  },
  "layout": { "x": 0, "y": 0, "numInputs": 1, "numOutputs": 1 }
}
```
