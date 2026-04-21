# Step 3C - Assign ID and Verification Method, Save Progress

## Assign an ID

Format: `[PREFIX]-[TYPE CODE]-[NUMBER]`
Number sequentially within each type, starting at 001.
Read `req-requirements.json` to determine the next number for this type code.
Examples: GCS-F-001, GCS-F-002, GCS-P-001

## Assign a Verification Method

Apply these rules in order. Stop at the first match.

1. Has a numeric value that can be measured with equipment → **Test**
2. Requires a calculation, simulation, or model to verify → **Analysis**
3. Can be verified by looking at it or reading a document → **Inspection**
4. None of the above → **Demonstration**

---

Output one line:
`[ID] | [Verification Method]`
Example: `GCS-F-001 | Test`

---

## Save progress

**1.** Call `readFile` for `req-requirements.json`.
   If the file does not exist, start with `{ "review_confirmed": false, "requirements": [] }`.

**2.** Append this requirement to the `requirements` array:

```json
{
  "index": [need index from req-classified.json],
  "id": "[assigned ID]",
  "text": "[final requirement text after checklist]",
  "type": "[F/P/I/C/Q]",
  "verification": "[Test/Analysis/Inspection/Demonstration]",
  "tbd": [true if text contains "[TBD" anywhere, otherwise false]
}
```

**3.** Call `writeFile` with filename `req-requirements.json` with the updated content.

**4.** Call `readFile` for `req-session.json`. Increment `needs_written` by 1. Call `writeFile` to save it back.

---

## Loop or advance

Read the updated `needs_written` and `needs_total` from `req-session.json`.

- If `needs_written` < `needs_total`:
  → NEXT: call `get_skill_file` with filename `steps/step-3a-template.md`

- If `needs_written` == `needs_total`:
  → Update `req-session.json`: set `"step": "3d"`
  → NEXT: call `get_skill_file` with filename `steps/step-3d-review.md`
