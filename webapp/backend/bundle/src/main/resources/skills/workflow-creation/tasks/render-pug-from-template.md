# Task: Render Pug from Template

## Identity
```
taskName:   "Render Pug from Template"
```

## IMPORTANT: This task is an outputStep only
- Do NOT place this in the `steps` array.
- Place it only in the top-level `outputStep` field.
- It receives the entire `ExecutionResult`, not a connector-routed packet.

## What it does
Renders the workflow's final output using a pre-built system output template.
You specify which step results to include and which template to use.
Produces an HTML page as the workflow's final output.

The results from the named steps are amalgamated into a single `"Task Results"` list that the template receives.

## Configuration

| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `Output Template` (enum) | string | yes | Name of the pre-built output template to use |
| `Result Tasks` | JSON string array | yes | Step names whose results should be included in the output |

The `Result Tasks` value is a JSON array of `stepName` strings (not task names - the unique step labels you set).

## Configuration example

```json
{
  "Output Template": "standard-report",
  "Result Tasks": "[\"Format results\", \"Final summary\"]"
}
```

## outputStep template

```json
"outputStep": {
  "taskName": "Render Pug from Template",
  "stepName": "Render Pug from Template",
  "id": 1,
  "loggingActive": true,
  "configuration": {
    "Output Template": "<template name>",
    "Result Tasks": "[\"<stepName1>\", \"<stepName2>\"]"
  },
  "layout": { "x": 0, "y": 0, "numInputs": 0, "numOutputs": 0 }
}
```
