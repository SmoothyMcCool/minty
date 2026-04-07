# Task: Emit Packet

## Identity
```
taskName:   "Emit Packet"
numInputs:  0
numOutputs: 1
```

## What it does
Source task. Emits one or more hardcoded packets into the workflow at startup.
Runs once immediately — use this as a source step when data is known at design time.
For plain text use Emit Text; for uploaded files use Emit Document.

## Configuration

| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `Data to Emit` | string | yes | A JSON array of Packet objects, encoded as a string |

### IMPORTANT: Data to Emit is an array of Packets, not plain records

Each element of the array must be a complete Packet object with `id`, `text`, and `data` fields.
This is the most common mistake — do not put plain data objects here.

**Correct** — array of Packet objects:
```json
{
  "Data to Emit": "[{\"id\":\"1\",\"text\":[],\"data\":[{\"name\":\"Alice\",\"score\":42}]},{\"id\":\"2\",\"text\":[],\"data\":[{\"name\":\"Bob\",\"score\":17}]}]"
}
```

**Wrong** — plain data records (will fail):
```json
{
  "Data to Emit": "[{\"id\":\"1\",\"name\":\"Alice\",\"score\":42}]"
}
```

The decoded value must match this structure:
```json
[
  {
    "id":   "<string — use empty string if no meaningful ID>",
    "text": ["<optional text strings>"],
    "data": [{ "<field>": "<value>", ... }]
  }
]
```

## Configuration examples

Single packet with one data record:
```json
{
  "Data to Emit": "[{\"id\":\"1\",\"text\":[],\"data\":[{\"name\":\"Alice\",\"score\":42,\"status\":\"active\"}]}]"
}
```

Multiple packets, each with one data record:
```json
{
  "Data to Emit": "[{\"id\":\"1\",\"text\":[],\"data\":[{\"name\":\"Alice\",\"score\":42}]},{\"id\":\"2\",\"text\":[],\"data\":[{\"name\":\"Bob\",\"score\":17}]}]"
}
```

Single packet with text only (e.g. a prompt to feed to Query LLM):
```json
{
  "Data to Emit": "[{\"id\":\"\",\"text\":[\"Summarise the quarterly results and list the key risks.\"],\"data\":[]}]"
}
```

Single packet with both text and data:
```json
{
  "Data to Emit": "[{\"id\":\"report-q3\",\"text\":[\"Q3 revenue exceeded targets by 12%.\"],\"data\":[{\"quarter\":\"Q3\",\"revenue\":1200000,\"target\":1070000}]}]"
}
```

## Connections
- Has no inputs. Never set this step as a `readerId` in connections.
- Has 1 output (index 0). Connect `writerPort: 0` to the next step.

## Step template

```json
{
  "taskName": "Emit Packet",
  "stepName": "<descriptive label>",
  "id": "1",
  "loggingActive": true,
  "configuration": {
    "Data to Emit": "<json array of Packet objects, encoded as a string>"
  },
  "layout": { "x": 0, "y": 0, "numInputs": 0, "numOutputs": 1 }
}
```

## Common uses
- Injecting test data or seed records at the start of a workflow
- Seeding a workflow with a fixed prompt before a Query LLM step
