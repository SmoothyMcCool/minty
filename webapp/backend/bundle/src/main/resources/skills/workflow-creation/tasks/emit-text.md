# Task: Emit Text

## Identity
```
taskName:   "Emit Text"
numInputs:  0
numOutputs: 1
```

## What it does
Source task. Emits a single hardcoded text string as a packet.
Simpler than Emit Packet - use this when you only need to inject a text value, not structured data.
Runs once immediately when the workflow starts.

## Configuration

| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `Text` | string | yes | The text string to emit |

## Configuration example

```json
{
  "Text": "Summarise the following document and list the key action items."
}
```

## Output packet structure
```json
{ "id": "", "text": ["<your text string>"], "data": [] }
```

`text` is always an array. `data` is always an empty array for this task. `id` is always an empty string.

## Connections
- Has NO inputs. Never set this step as a `readerId` in connections.
- Has 1 output (index 0).

## Step template

```json
{
  "taskName": "Emit Text",
  "stepName": "<descriptive label>",
  "id": "1",
  "loggingActive": true,
  "configuration": {
    "Text": "<your text here>"
  },
  "layout": { "x": 0, "y": 0, "numInputs": 0, "numOutputs": 1 }
}
```

## Common uses
- Injecting a prompt or instruction string at the start of a workflow
- Seeding a workflow with a fixed text value before an LLM query
