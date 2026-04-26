---
name: workflow-builder
description: >
  Use this skill whenever the user wants to create, build, generate, or modify a workflow
  for the tom workflow engine. Triggers include: "create a workflow", "build a workflow",
  "generate workflow JSON", "add a step to my workflow", "connect these tasks", "wire up
  a workflow", "make a workflow that does X", or any request to describe a desired data
  pipeline and get a workflow JSON definition back. Also use when the user describes what
  they want a workflow to do in plain English and needs it translated into the tom workflow
  JSON format. Always use this skill for any workflow generation request to ensure correct
  task names, connection wiring, and JSON structure.
metadata:
  target_model: "small (<=30B parameters)"
---

# Workflow builder skill

## What this skill does
Generates workflow JSON definitions for the `minty` workflow engine from plain-English descriptions.

## How to use this skill

You have access to three specialist agents for workflow tasks: Design, Build, and Validate.
Use them in order whenever the user wants to create or modify a workflow.

### When planning
Use the `workflow_design` agent to perform step 1: the user describes
something they want a workflow to do, asks to modify an existing workflow, or asks for
advice on how to structure a workflow.

Use the `workflow_build` agent to perform step 2: when the `workflow_design`
agent has produced a plan.

Use the `workflow_validate` agent to perform step 3: when the `workflow_build`
agent has produced workflow JSON.

Use the `workflow_submit` agent to perform step 4: when the `workflow_validate`
agent has validated the workflow JSON.

**Workflow Submit agent**
Hand off to this agent when: the Validate agent has validated the workflow JSON.
Give it: the workflow JSON exactly as returned by the Validate agent.
It returns: the workflow JSON, and information on where the workflow is stored.

**Submission**
Only call create_workflow or update_workflow after the Validate agent has confirmed the JSON is valid.
Never submit workflow JSON that has not passed validation.

### Step 1 - identify which tasks are needed
Read the user's request and decide which tasks are required.
Available tasks are listed in the TASK INDEX below.
Load the task file for each task you intend to use before writing any JSON.

### Step 2 - load supporting files
Always load:
- `workflow-schema.md` - the full JSON schema and connection rules
- `packet-schema.md` - Packet structure, path syntax, and SpEL expression reference

Load only the task files you need:
- `tasks/<filename>` - one file per task

Do not load task files you will not use. Keep context small.

Follow the instructions in `workflow-schema.md`.
Use the loaded task files for exact taskName values, numInputs, numOutputs, and configuration format.

### Step 4




## Task index

### Emit (source tasks - no inputs, start immediately)
| Task | File | Purpose |
|------|------|---------|
| Emit Packet | `tasks/emit-packet.md` | Injects hardcoded JSON packet data |
| Emit Text | `tasks/emit-text.md` | Injects a plain text string as a packet |
| Emit Document | `tasks/emit-document.md` | Reads an uploaded file and emits its text content |

### Flow control
| Task | File | Purpose |
|------|------|---------|
| Broadcast | `tasks/broadcast.md` | Copies a packet identically to N output branches |
| Funnel | `tasks/funnel.md` | Routes packets from N inputs to 1 output, no combining |
| Merge | `tasks/merge.md` | Combines one packet from each of N inputs into one packet |
| Split | `tasks/split.md` | Expands a multi-item packet into one packet per item |
| Collect | `tasks/collect.md` | Accumulates a stream of packets into one |
| Branch | `tasks/branch.md` | Routes to output 0 (true) or output 1 (false) via SpEL |
| Loop | `tasks/loop.md` | Loops while a SpEL condition is true; exits on output 1 |
| GroupBy | `tasks/group-by.md` | Groups packets from port 1 by matching ID from port 0 |
| Sort | `tasks/sort.md` | Buffers all packets then emits them sorted by field |
| SetId | `tasks/set-id.md` | Sets packet ID from a data field value |
| Log Packets | `tasks/log-packets.md` | Pass-through that logs packet contents |

### Transform
| Task | File | Purpose |
|------|------|---------|
| Transform | `tasks/transform.md` | Applies a sequence of named data operations |
| Query LLM | `tasks/query-llm.md` | Sends a prompt to an AI assistant |
| Format Text | `tasks/format-text.md` | Renders a template string with packet data substitutions |
| Rename Data | `tasks/rename-data.md` | Renames fields in packet data |
| Execute Python | `tasks/execute-python.md` | Runs a Python script on the packet |
| Read TSV Data | `tasks/read-tsv-data.md` | Parses TSV text into structured data records |
| Format Html | `tasks/format-html.md` | Renders a custom Pug template to HTML |
| Format Html (Template) | `tasks/format-html-template.md` | Renders a pre-made Pug template to HTML |

### External
| Task | File | Purpose |
|------|------|---------|
| Get Confluence Pages | `tasks/get-confluence-pages.md` | Fetches Confluence page content by page ID |

### Output steps (use in outputStep only, never in steps array)
| Task | File | Purpose |
|------|------|---------|
| JSON Output | `tasks/json-output.md` | Formats ExecutionResult as JSON |
| Render Pug | `tasks/render-pug.md` | Renders ExecutionResult with a custom Pug template |
| Render Pug from Template | `tasks/render-pug-from-template.md` | Renders using a pre-made output template |

---

## Choosing between similar tasks

### Fan-out (one → many)
| Goal | Task |
|------|------|
| Send the same packet to N parallel branches | **Broadcast** |
| Expand a multi-item packet into one packet per item | **Split** |
| Route to one of two branches based on a condition | **Branch** |

### Fan-in (many → one)
| Goal | Task |
|------|------|
| Pass packets from N branches through to one step, unchanged | **Funnel** |
| Wait for one packet from each of N branches, combine contents | **Merge** |
| Accumulate all packets from a single stream into one | **Collect** |

### Split + Collect is the standard fan-out/fan-in pair
Use Split to expand, process each item individually, then Collect to reassemble.

---

## Golden rules - never violate these

1. `writerId` is the upstream step (the step sending data). `readerId` is the downstream step (the step receiving data).
2. Indices are zero-based. `writerPort` must be within `[0, numOutputs-1]`. `readerPort` must be within `[0, numInputs-1]`.
3. `stepName` must be unique across all steps.
4. Every `writerId` and `readerId` in `connections` must match a step `id` in the `steps` array.
5. Steps with no inputs (`numInputs: 0`) must never appear as a `readerId` in connections.
6. `numInputs` and `numOutputs` in `layout` must exactly match the task's declared values.
7. The `outputStep` is NOT in the `steps` array. It is a separate top-level field.
8. All `configuration` values are strings, even numbers (e.g. `"2"` not `2`).
9. Give every step a unique `id`. Use simple incrementing integers: "1", "2", "3", etc. Never reuse an ID within the same workflow.
10. The `Transform` task `Definition` config is a JSON string inside a JSON string - it must be escaped.
11. Every Packet has exactly three fields: `id` (string), `text` (array of strings), `data` (array of objects). Never omit any field.
12. `text` and `data` are always arrays - never bare strings or bare objects.
13. The `Emit Packet` `Data to Emit` value is a JSON array of Packet objects - each element must have `id`, `text`, and `data` fields.
