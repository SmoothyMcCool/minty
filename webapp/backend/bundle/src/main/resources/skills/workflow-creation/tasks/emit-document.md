# Task: Emit Document

## Identity
```
taskName:   "Emit Document"
numInputs:  0
numOutputs: 1
```

## What it does
Source task. Reads an uploaded file (PDF, Word, spreadsheet, etc.) and emits its text content as a packet.
The file is converted to Markdown internally before emission.
Runs once immediately when the workflow starts.

## Configuration

| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `File` | Document | yes | The uploaded file to read. Stored as a base64-encoded object with `name` and `file` fields. |

The `File` configuration value is set via the UI file upload widget — it is not a plain string.
When generating JSON for this task, set `"File": {}` as a placeholder and note that the user must upload the file via the UI.

## Output packet structure
```json
{ "id": null, "text": ["<file contents as markdown>"], "data": null }
```

Note: Emit Document is the only task that produces `null` for `id` and `data`. The `text` field is still an array containing one string — the full file contents.

## Connections
- Has NO inputs. Never set this step as a `readerId` in connections.
- Has 1 output (index 0).

## Step template

```json
{
  "taskName": "Emit Document",
  "stepName": "<descriptive label>",
  "id": "1",
  "loggingActive": true,
  "configuration": {
    "File": {}
  },
  "layout": { "x": 0, "y": 0, "numInputs": 0, "numOutputs": 1 }
}
```

## Common uses
- Reading a PDF or Word document into a workflow for AI processing
- Extracting spreadsheet data (combine with Read TSV Data for structured extraction)
- Any workflow that starts with a user-uploaded file
