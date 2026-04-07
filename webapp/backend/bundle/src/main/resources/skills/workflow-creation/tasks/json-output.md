# Task: JSON Output

## Identity
```
taskName:   "JSON Output"
numInputs:  0
numOutputs: 0
```

## What it does
Formats the workflow's final `ExecutionResult` as JSON.
This task runs automatically after all steps have completed.
It is the standard output formatter for most workflows.

## IMPORTANT: This task is an outputStep only
- Do NOT place this task in the `steps` array.
- Place it only in the top-level `outputStep` field.
- It receives no connections — it reads directly from the ExecutionResult, not from connectors.

## Configuration
No configuration required.

```json
{}
```

## outputStep template

```json
"outputStep": {
  "taskName": "JSON Output",
  "stepName": "JSON Output",
  "id": 1,
  "loggingActive": true,
  "configuration": {},
  "layout": { "x": 0, "y": 0, "numInputs": 0, "numOutputs": 0 }
}
```

## When to use
Use JSON Output as the `outputStep` for any workflow where you want results returned as JSON.
If no `outputStep` is configured, the workflow produces no formatted output.
