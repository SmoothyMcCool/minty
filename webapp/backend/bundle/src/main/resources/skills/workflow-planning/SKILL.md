---
name: workflow-planner
description: >
  Use this skill when planning whenever the user wants to create, build, generate, or modify a workflow
  for the tom workflow engine. Triggers include: "create a workflow", "build a workflow",
  "generate workflow JSON", "add a step to my workflow", "connect these tasks", "wire up
  a workflow", "make a workflow that does X", or any request to describe a desired data
  pipeline and get a workflow JSON definition back. Also use when the user describes what
  they want a workflow to do in plain English and needs it translated into the tom workflow
  JSON format. Always use this skill for any workflow generation request to ensure correct
  task names, connection wiring, and JSON structure.
metadata:
  target_model: "small (â‰¤30B parameters)"
---
## Workflow specialist agents

You have access to three specialist agents for workflow tasks: Design, Build, and Validate.
Use them in order whenever the user wants to create or modify a workflow.

**Workflow Design agent**
Hand off to this agent when: the user describes something they want a workflow to do,
asks to modify an existing workflow, or asks for advice on how to structure a workflow.
Give it: the user's request in plain English, plus any existing workflow JSON if modifying.
It returns: a plain-English workflow plan (steps, data flow, task selection).
Do not ask it to write JSON.

**Workflow Build agent**
Hand off to this agent when: the Design agent has produced an approved plan.
Give it: the complete design plan exactly as returned by the Design agent.
It returns: the complete workflow JSON ready to submit.
Do not hand off to this agent without a completed design plan.

**Workflow Validate agent**
Hand off to this agent when: the Build agent has produced workflow JSON.
Give it: the workflow JSON exactly as returned by the Build agent.
It returns: either a confirmation that the workflow is valid, or a list of specific errors.
If errors are returned, send the JSON and the error list back to the Build agent for correction.
Repeat until the Validate agent confirms the workflow is valid, then submit it.

**Workflow Submit agent**
Hand off to this agent when: the Validate agent has validated the workflow JSON.
Give it: the workflow JSON exactly as returned by the Validate agent.
It returns: the workflow JSON, and information on where the workflow is stored.

**Submission**
Only call create_workflow or update_workflow after the Validate agent has confirmed the JSON is valid.
Never submit workflow JSON that has not passed validation.