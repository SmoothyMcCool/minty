---
name: requirements-writing
description: >
  Use this skill whenever the user wants to write, generate, review, improve, or reformat
  system engineering requirements. Triggers include: "write requirements", "generate a
  requirements document", "turn this into requirements", "review my requirements",
  "check my shall statements", "create an SRS", "draft system requirements", "write
  functional requirements", "write non-functional requirements", "decompose requirements",
  or any request to produce or refine requirements artifacts for a system or subsystem.
  Also use when the user provides a ConOps, SOW, customer spec, or problem statement and
  wants requirements derived from it. Always use this skill — even for a single requirement
  — to ensure INCOSE-aligned quality and correct output format.
metadata:
  target_model: "small (≤20B parameters)"
---

# Requirements Writing

This skill produces INCOSE-aligned system requirements in Markdown format.

---

## SESSION START — do this first, every time

Before responding to the user, read the project state files to determine where to resume:

**Step 1:** Call `readFile` for `req-session.json`
- If the file is **missing or empty** → start at Step 1 (load `steps/step-1-collect.md`)
- If the file **exists** → read the `step` field and jump using the RESUME TABLE below

**Step 2:** If `step` ≥ 3, also call `readFile` for `req-classified.json` (recover needs list)

**Step 3:** If `step` ≥ 3, also call `readFile` for `req-requirements.json` (recover written requirements)

Never ask the user for information already present in these files.

---

## RESUME TABLE

| `step` in req-session.json | Action |
|---|---|
| `1` | Inputs incomplete — load `steps/step-1-collect.md` |
| `2` | Inputs collected — load `steps/step-2-classify.md` |
| `3` | Classification confirmed — read req-classified.json, resume Step 3 loop at index `needs_written + 1`, load `steps/step-3a-template.md` |
| `3d` | All requirements written — read both JSONs, load `steps/step-3d-review.md` |
| `4` | Review confirmed — read all JSONs, load `steps/step-4-output.md` |
| `done` | Inform user the document was already generated. Offer to regenerate or revise. |

When resuming mid-session, tell the user briefly where you're picking up:
> "Resuming — inputs collected, 2 of 5 requirements written. Continuing with requirement 3."

---

## FLOW MAP

```
[SESSION START] → read req-session.json → RESUME TABLE
        │
        ▼ (no state file)
[STEP 1] steps/step-1-collect.md
  Collect: system name, prefix, level, source material.
  Scan conversation first — only ask for genuinely missing values.
  → writeFile: req-session.json (step: 2)
        │
        ▼
[STEP 2] steps/step-2-classify.md
  Classify each need (F / P / I / C / Q).
  Show list → wait for user confirmation.
  → writeFile: req-classified.json
  → update req-session.json (step: 3, needs_total: N, needs_written: 0)
        │
        ▼
[STEP 3 LOOP] ──────────────────────────────────────────┐
  steps/step-3a-template.md   write requirement text     │
        ↓                                                 │
  steps/step-3b-checklist.md  quality checks             │
        ↓                                                 │
  steps/step-3c-verify.md     assign ID + verification   │
  → append to req-requirements.json                       │
  → update req-session.json (increment needs_written)     │
  → more needs? ──────────────────────────────────────────┘
        │ (all done)
        ▼
[STEP 3D] steps/step-3d-review.md
  Present full requirements table → wait for user confirmation.
  → update req-requirements.json (review_confirmed: true)
  → update req-session.json (step: 4)
        │
        ▼
[STEP 4] steps/step-4-output.md
  Generate and save the Markdown requirements document.
  → update req-session.json (step: "done")
  END — do not load any further files.
```

---

## PROJECT STATE FILES

Three JSON files are maintained in the project throughout this skill:

### `req-session.json`
Tracks overall progress and collected inputs.
```json
{
  "step": "3",
  "system_name": "Ground Control Station",
  "prefix": "GCS",
  "level": "System",
  "source_summary": "One-sentence description of source material",
  "needs_total": 5,
  "needs_written": 2
}
```

### `req-classified.json`
The confirmed list of classified needs from Step 2.
```json
{
  "confirmed": true,
  "needs": [
    { "index": 1, "type": "F", "description": "The system must log all operator commands" },
    { "index": 2, "type": "P", "description": "Response time must be under 2 seconds" }
  ]
}
```

### `req-requirements.json`
Requirements written so far in the Step 3 loop.
```json
{
  "review_confirmed": false,
  "requirements": [
    {
      "index": 1,
      "id": "GCS-F-001",
      "text": "The Ground Control Station shall record all operator commands to non-volatile storage.",
      "type": "F",
      "verification": "Test",
      "tbd": false
    }
  ]
}
```
