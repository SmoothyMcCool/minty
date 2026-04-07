# Task: Broadcast

## Identity
```
taskName:   "Broadcast"
numInputs:  1
numOutputs: <configured value — default 2>
```

`numOutputs` is set by configuration and must be reflected in `layout.numOutputs`.

## What it does
Sends an identical copy of each incoming packet to every connected output.
All outputs receive the same packet — data is not divided, filtered, or modified.

Use Broadcast when you need the same data to flow through multiple independent processing branches
in parallel — for example, sending records to both a transform pipeline and a logging step simultaneously.
Connect each output to a different downstream step.

To divide a multi-item packet into individual packets, use Split instead.
To route conditionally to one of two paths, use Branch instead.

## Configuration

| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `Number of Outputs` | string | yes | How many outputs to create. Must be `"2"` or higher. |

## Configuration example

```json
{
  "Number of Outputs": "2"
}
```

## Connections
- Has 1 input (index 0).
- Has N outputs (indices 0 through N-1), where N is the configured number.
- `layout.numOutputs` must match the configured number.
- Unconnected outputs are silently discarded.

## Step template

```json
{
  "taskName": "Broadcast",
  "stepName": "<descriptive label>",
  "id": "1",
  "loggingActive": true,
  "configuration": {
    "Number of Outputs": "2"
  },
  "layout": { "x": 0, "y": 0, "numInputs": 1, "numOutputs": 2 }
}
```

## Connecting a Broadcast with 2 outputs

```json
{ "writerId": "<upstream-id>",  "writerPort": 0, "readerId": "<broadcast-id>", "readerPort": 0 },
{ "writerId": "<broadcast-id>", "writerPort": 0, "readerId": "<branch-a-id>",  "readerPort": 0 },
{ "writerId": "<broadcast-id>", "writerPort": 1, "readerId": "<branch-b-id>",  "readerPort": 0 }
```

## Common uses
- Sending the same data through two different transform pipelines in parallel
- Creating a logging branch alongside a processing branch
- Feeding multiple downstream steps from a single source
