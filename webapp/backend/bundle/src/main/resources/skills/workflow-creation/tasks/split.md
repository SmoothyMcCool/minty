# Task: Split

## Identity
```
taskName:   "Split"
numInputs:  1
numOutputs: 1
```

## What it does
Expands a single packet that contains multiple items in its `text` or `data` arrays
into a stream of individual packets - one per item. All output packets share the input packet's ID.

Use Split to fan out a multi-record packet so each record can be processed individually
by downstream steps - for example, sending each item to Query LLM for its own LLM call.
Pair with Collect at the end of the pipeline to reassemble the results into one packet.

To clone a packet identically to multiple outputs, use Broadcast instead.

## Configuration
No configuration required.

```json
{}
```

## Example
Input: `{ "id": "a", "text": ["hello", "world"], "data": [{"x": 1}, {"x": 2}] }`

Output:
- Packet 1: `{ "id": "a", "text": ["hello"], "data": [{"x": 1}] }`
- Packet 2: `{ "id": "a", "text": ["world"], "data": [{"x": 2}] }`

## Connections
- 1 input (index 0).
- 1 output (index 0).

## Step template

```json
{
  "taskName": "Split",
  "stepName": "<descriptive label>",
  "id": 1,
  "loggingActive": true,
  "configuration": {},
  "layout": { "x": 0, "y": 0, "numInputs": 1, "numOutputs": 1 }
}
```

## Common uses
- Expanding a multi-record packet so each record is processed individually downstream
- Fan-out before a Query LLM step so each item gets its own LLM call
- Pair with Collect to process items individually then reassemble
