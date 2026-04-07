# Step 3D — Review All Requirements

Read `req-requirements.json` and present every requirement in this table:

```
| ID        | Requirement                         | Type | Verification | TBD? |
|-----------|-------------------------------------|------|--------------|------|
| GCS-F-001 | The system shall ...                | F    | Test         | No   |
| GCS-P-001 | The system shall ... [TBD — ...]    | P    | Analysis     | Yes  |
```

TBD? = Yes if the requirement's `tbd` field is true. Otherwise No.

---

Show the table to the user and ask:
> "Are these correct? Any changes before I write the document?"

If the user requests changes:
- Update the relevant requirement(s) in the table
- Call `readFile` for `req-requirements.json`, apply the changes, call `writeFile` to save
- Show the updated table and ask again

When the user confirms with no further changes:

## Save state

**1.** Call `readFile` for `req-requirements.json`. Set `"review_confirmed": true`. Call `writeFile` to save.

**2.** Call `readFile` for `req-session.json`. Set `"step": "4"`. Call `writeFile` to save.

---

NEXT: call `get_skill_file` with filename `steps/step-4-output.md`
