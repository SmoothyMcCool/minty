# Task: Merge

## Identity
```
taskName:   "Merge"
numInputs:  <configured value - default 2>
numOutputs: 1
```

`numInputs` is set by configuration and must be reflected in `layout.numInputs`.

## What it does
Waits for one packet from each input, then combines their `text` and `data`
arrays into a single output packet. Runs once per set of inputs.

Use Merge when two parallel branches each produce one packet and you need their contents
combined before continuing - for example, reuniting an LLM response with the original source
data after a Broadcast → Query LLM branch. Configure `Number of Inputs` to match the number
of branches feeding into it.

Merge is synchronised: it waits for all inputs before producing output. If one branch produces
no packet, that port contributes empty content. For passing packets through without combining,
use Funnel. For accumulating a stream of many packets, use Collect.

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

## Output
A single packet whose `text` is the concatenation of all input `text` arrays, and `data` is the concatenation of all input `data` arrays.

## Connections
- Has N inputs (indices 0 through N-1).
- Has 1 output (index 0).
- `layout.numInputs` must match the configured number.

## Step template

```json
{
  "taskName": "Merge",
  "stepName": "<descriptive label>",
  "id": 1,
  "loggingActive": true,
  "configuration": {
    "Number of Inputs": "2"
  },
  "layout": { "x": 0, "y": 0, "numInputs": 2, "numOutputs": 1 }
}
```

## Common uses
- Reuniting two parallel branches into one packet after a Split, when you want the combined content
- Combining an LLM response with the original input data before formatting output
