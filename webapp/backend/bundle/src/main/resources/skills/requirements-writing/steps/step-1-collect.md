# Step 1 - Collect Inputs

## SCAN FIRST - before asking anything

Check the current conversation for these 4 values. Do not ask for a value that is already present.

| # | Input | What to look for |
|---|-------|-----------------|
| 1 | System name | "the system is...", "for a...", "called...", or any named system |
| 2 | Project prefix | any 2–5 letter code the user mentions; if absent, derive an obvious one from the system name and confirm it |
| 3 | Requirements level | "system-level", "subsystem", "component", or similar |
| 4 | Source material | any ConOps, SOW, problem statement, or draft requirements text provided |

If ALL 4 are present → skip to the confirmation list below. Do NOT ask questions.

Only ask about items that are genuinely missing. Combine all missing questions into a single message.

Do not guess or invent any values.

---

## Confirmation list

Output this before proceeding:

> **Inputs confirmed:**
> - System name: [value]
> - Prefix: [value]
> - Level: [value]
> - Source: [one sentence describing what was provided]

---

## Save state

Call `writeFile` with filename `req-session.json` and this content (fill in actual values):

```json
{
  "step": "2",
  "system_name": "[system name]",
  "prefix": "[prefix]",
  "level": "[level]",
  "source_summary": "[one sentence description of source material]",
  "needs_total": 0,
  "needs_written": 0
}
```

---

NEXT: call `get_skill_file` with filename `steps/step-2-classify.md`
