# Task: Branch

## Identity
```
taskName:   "Branch"
numInputs:  1
numOutputs: 2
```

## What it does
Evaluates a Spring Expression Language (SpEL) condition against the incoming packet.
- If the condition is TRUE: packet is sent to output 0
- If the condition is FALSE: packet is sent to output 1

Use Branch when you want a one-time routing decision (not a loop).
For repeated conditional evaluation (looping), use Loop instead.

## Configuration

| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `Branch Expression` | string | yes | A SpEL expression that must evaluate to a boolean |

### SpEL expression context
The variable `#p` refers to the current packet. Access all three packet fields via `#p`:
- `#p.id` - the packet's id string
- `#p.text` - the packet's text list (List<String>)
- `#p.text[0]` - the first text string
- `#p.data` - the packet's data list (List<Map>)
- `#p.data[0]['fieldName']` - value of a field in the first data record

The expression MUST return a boolean. If it returns anything else, the task fails.

Note: Branch uses `#p` as the packet variable. Loop uses a different context - see loop.md.

## Configuration examples

```json
{ "Branch Expression": "#p.id == 'customer-42'" }
```
```json
{ "Branch Expression": "#p.data[0]['status'] == 'approved'" }
```
```json
{ "Branch Expression": "#p.data[0]['score'] > 80" }
```
```json
{ "Branch Expression": "#p.text.size() > 0" }
```
```json
{ "Branch Expression": "#p.text[0].contains('error')" }
```

## Connections
- 1 input (index 0).
- 2 outputs: port 0 = condition true, port 1 = condition false.
- Both outputs can be connected, or only one if the other branch is not needed.

## Step template

```json
{
  "taskName": "Branch",
  "stepName": "<descriptive label>",
  "id": "1",
  "loggingActive": true,
  "configuration": {
    "Branch Expression": "<spel expression>"
  },
  "layout": { "x": 0, "y": 0, "numInputs": 1, "numOutputs": 2 }
}
```
