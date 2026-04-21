# Task: Funnel

## Identity
```
taskName:   "Funnel"
numInputs:  <configured value - default 2>
numOutputs: 1
```

`numInputs` is set by configuration and must be reflected in `layout.numInputs`.

## What it does
Routes packets from N inputs to a single output, passing each through unchanged.
Packets are forwarded individually as they arrive - no waiting, buffering, or combining of contents.

Use Funnel to reconnect branches that were separated by a Broadcast, when you want all branches
to continue into the same downstream step but do not need to combine their packet contents.

If you need to combine the contents of one packet from each branch into a single packet, use Merge.
If you need to accumulate all packets from a single stream into one, use Collect.

## Configuration

| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `Number of Inputs` | string | yes | How many inputs. Default `"2"`. |

## Configuration example

```json
{
  "Number of Inputs": "2"
}
```

## Connections
- Has N inputs (indices 0 through N-1).
- Has 1 output (index 0).
- `layout.numInputs` must match the configured number.

## Step template

```json
{
  "taskName": "Funnel",
  "stepName": "<descriptive label>",
  "id": "1",
  "loggingActive": true,
  "configuration": {
    "Number of Inputs": "2"
  },
  "layout": { "x": 0, "y": 0, "numInputs": 2, "numOutputs": 1 }
}
```

## Common uses
- Reuniting two branches after a Broadcast into one downstream step
- Routing output from multiple parallel steps into one downstream step without combining packet contents
