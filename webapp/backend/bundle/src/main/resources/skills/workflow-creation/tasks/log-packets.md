# Task: Log Packets

## Identity
```
taskName:   "Log Packets"
numInputs:  1
numOutputs: 1
```

## What it does
Logs the content of each received packet to the workflow log file.
Passes each packet through to its output unchanged.
Has no effect on data - purely observational.

## Configuration
No configuration required.

```json
{}
```

## Connections
- Has 1 input (index 0) and 1 output (index 0).
- Can be placed anywhere in a pipeline as a tap - connect the upstream step to its input and the downstream step to its output.
- If placed at the end of a branch, the output can be left unconnected.

## Step template

```json
{
  "taskName": "Log Packets",
  "stepName": "<descriptive label>",
  "id": 1,
  "loggingActive": true,
  "configuration": {},
  "layout": { "x": 0, "y": 0, "numInputs": 1, "numOutputs": 1 }
}
```

## Common uses
- Inspecting data at an intermediate point in a pipeline without affecting flow
- Terminal step on a branch that doesn't need further processing
- Debugging: insert between any two steps to see what data looks like at that point
