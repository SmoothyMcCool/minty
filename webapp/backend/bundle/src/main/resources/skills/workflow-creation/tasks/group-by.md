# Task: GroupBy

## Identity
```
taskName:   "GroupBy"
numInputs:  2
numOutputs: 1
```

## What it does
Joins two sorted streams on a shared packet ID, grouping matching records together.
Port 0 receives key packets one at a time. Port 1 receives a stream of detail packets.
For each key packet, all detail packets whose ID matches are combined into one output packet.

Use GroupBy when you have two related streams - for example, a list of categories on port 0
and a larger sorted list of items on port 1 - and you want to group the items under their
category. Both streams must be sorted by ID before reaching GroupBy. Add Sort and SetId
steps upstream on each branch if needed.

## Configuration
No configuration required.

```json
{}
```

## Connections
- Input 0: receives the key packet (one at a time)
- Input 1: receives the stream of packets to group
- Output 0: emits one grouped packet per unique key

## Step template

```json
{
  "taskName": "GroupBy",
  "stepName": "<descriptive label>",
  "id": 1,
  "loggingActive": true,
  "configuration": {},
  "layout": { "x": 0, "y": 0, "numInputs": 2, "numOutputs": 1 }
}
```

## Common uses
- Joining two sorted streams on a shared ID field
- Aggregating detail records under their parent key
