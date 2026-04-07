# Task: Render Pug

## Identity
```
taskName:   "Render Pug"
```

## IMPORTANT: This task is an outputStep only
- Do NOT place this in the `steps` array.
- Place it only in the top-level `outputStep` field.
- It receives the entire `ExecutionResult` (all step results combined), not a connector-routed packet.

## What it does
Renders a custom Pug template using the workflow's `ExecutionResult` as context.
Produces an HTML string as the workflow's final output.

Use this when you want to write your own Pug template for the final output.
For a pre-built system output template, use Render Pug from Template instead.
For JSON output, use JSON Output instead.

## Configuration

| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `Pug Template` | string | yes | A Pug template string. The ExecutionResult is available as the template context. |

## Configuration example

```json
{
  "Pug Template": "h1 Workflow Results\np Name: #{name}\n"
}
```

## outputStep template

```json
"outputStep": {
  "taskName": "Render Pug",
  "stepName": "Render Pug",
  "id": 1,
  "loggingActive": true,
  "configuration": {
    "Pug Template": "<pug template string>"
  },
  "layout": { "x": 0, "y": 0, "numInputs": 0, "numOutputs": 0 }
}
```
