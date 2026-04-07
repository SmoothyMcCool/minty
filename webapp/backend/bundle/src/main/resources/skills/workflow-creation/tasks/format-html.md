# Task: Format Html

## Identity
```
taskName:   "Format Html"
numInputs:  1
numOutputs: 1
```

## What it does
Renders a Pug template you provide using the entire input packet as the template context.
Produces an HTML string stored in the output packet's `text` field.

The entire packet (id, text array, data array) is passed to the template at once.
This task does NOT iterate over the `data` array automatically — iteration is done inside the template.

To use a pre-built system template instead of writing your own Pug, use Format Html (Template).

## Configuration

| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `Pug Template` | string | yes | A Pug template string |

### Pug template context variables
- `id` — the packet ID
- `text` — the text array
- `data` — the data array (list of maps)

### Basic Pug example
```pug
h1 Report for #{id}
ul
  each item in data
    li #{item.name}: #{item.value}
```

## Configuration example

```json
{
  "Pug Template": "h1 Results\nul\n  each item in data\n    li #{item.name} - #{item.status}"
}
```

## Output
- `id`: copied from the input packet
- `text`: single-element list containing the rendered HTML string
- `data`: empty

## Connections
- 1 input (index 0).
- 1 output (index 0).

## Step template

```json
{
  "taskName": "Format Html",
  "stepName": "<descriptive label>",
  "id": "1",
  "loggingActive": true,
  "configuration": {
    "Pug Template": "<pug template string>"
  },
  "layout": { "x": 0, "y": 0, "numInputs": 1, "numOutputs": 1 }
}
```
