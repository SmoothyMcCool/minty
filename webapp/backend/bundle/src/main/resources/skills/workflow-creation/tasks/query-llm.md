# Task: Query LLM

## Identity
```
taskName:   "Query LLM"
numInputs:  1
numOutputs: 1
```

## What it does
Sends a prompt to an AI assistant and emits the response as a packet.

The prompt is built by combining the configured `Query` text with the `text` content of the
input packet. If the input has multiple text items, one LLM call is made per item.
The response is stored in `data` if it parses as a JSON array of objects, otherwise in `text`.

Feed structured data through Format Text first if you need to construct a prompt from data fields.
For multi-turn conversations, choose an assistant with memory enabled - the task will
automatically continue the conversation using the `Conversation ID` carried in the packet.

## Before generating this task - ask the user

You must ask the user for these two values before writing the configuration:

1. **Which model?** Suggested default: `gpt-oss:120b`
2. **Context size?** Suggested default: `32768` (32k). Common values: `16384` (16k), `32768` (32k).

You may choose a reasonable `temperature` yourself - `0.7` is a good general default.
Lower values (0.1–0.3) for factual/structured output, higher (0.7–1.0) for creative tasks.

## Configuration

The `Assistant` object is inline - it is not a reference to a saved assistant.
The `id` and `name` fields inside `assistant` do not matter; use empty string and any label.
The `prompt` field is the **system prompt** (instruction to the model).
The `Query` field is the **first user message** - this is only used if the step has no
upstream input, or if you want a fixed opening message. Most of the time leave it empty
and supply text from an upstream step instead.

All other fields must be set exactly as shown in the template - do not change them.

| Field | Set by user | Notes |
|-------|------------|-------|
| `model` | yes | Ask the user - suggest `gpt-oss:120b` |
| `contextSize` | yes | Ask the user - suggest `32768` |
| `temperature` | you decide | 0.7 is a good default |
| `prompt` | yes | The system prompt / instruction |
| `Query` | yes | First user message - leave `""` if fed from upstream |
| `topK` | fixed | Always `5` |
| `tools` | fixed | Always `[]` |
| `owned` | fixed | Always `false` |
| `hasMemory` | fixed | Always `false` |
| `documentIds` | fixed | Always `[]` |
| `assistantId` | fixed | Always `null` |
| `id` (assistant) | fixed | Always `""` |
| `name` (assistant) | fixed | Any string - use `"assistant"` |

## Configuration template

```json
{
  "Query": "",
  "Assistant": {
    "assistantId": null,
    "assistant": {
      "id": "",
      "name": "assistant",
      "prompt": "<system prompt - your instruction to the model>",
      "model": "<ask user - suggest gpt-oss:120b>",
      "contextSize": <ask user - suggest 32768>,
      "temperature": <you decide - 0.7 is a good default>,
      "topK": 5,
      "tools": [],
      "owned": false,
      "hasMemory": false,
      "documentIds": []
    }
  }
}
```

## Configuration examples

Query LLM used mid-pipeline (text fed from upstream, Query left empty):
```json
{
  "Query": "",
  "Assistant": {
    "assistantId": null,
    "assistant": {
      "id": "",
      "name": "assistant",
      "prompt": "You are a helpful assistant. Summarise the text given and extract the key action items as a JSON array.",
      "model": "gpt-oss:120b",
      "contextSize": 32768,
      "temperature": 0.3,
      "topK": 5,
      "tools": [],
      "owned": false,
      "hasMemory": false,
      "documentIds": []
    }
  }
}
```

Query LLM used as a starting step (Query provides the first user message):
```json
{
  "Query": "What are the most important recent developments in battery technology?",
  "Assistant": {
    "assistantId": null,
    "assistant": {
      "id": "",
      "name": "assistant",
      "prompt": "You are a knowledgeable research assistant. Be concise and factual.",
      "model": "gpt-oss:120b",
      "contextSize": 16384,
      "temperature": 0.5,
      "topK": 5,
      "tools": [],
      "owned": false,
      "hasMemory": false,
      "documentIds": []
    }
  }
}
```

## Input constraints
- Input `data` array must contain 0 or 1 objects - multiple records are not supported.
- Input `text` array may contain multiple strings - one LLM call is made per text item.
- Both `text` and `data` must be arrays even when empty.

## Output
- `id`: copied from the input packet ID
- `text`: LLM response text (if response is not parseable as a JSON array of objects)
- `data`: LLM response as structured data (if response is a valid JSON array of objects)

## Connections
- 1 input (index 0).
- 1 output (index 0).

## Step template

```json
{
  "taskName": "Query LLM",
  "stepName": "<descriptive label>",
  "id": "1",
  "loggingActive": true,
  "configuration": {
    "Query": "",
    "Assistant": {
      "assistantId": null,
      "assistant": {
        "id": "",
        "name": "assistant",
        "prompt": "<system prompt>",
        "model": "gpt-oss:120b",
        "contextSize": 32768,
        "temperature": 0.7,
        "topK": 5,
        "tools": [],
        "owned": false,
        "hasMemory": false,
        "documentIds": []
      }
    }
  },
  "layout": { "x": 0, "y": 0, "numInputs": 1, "numOutputs": 1 }
}
```

## Common uses
- Summarising, classifying, or extracting information from text
- Generating structured JSON output from unstructured input
- Answering questions or generating content based on upstream data
