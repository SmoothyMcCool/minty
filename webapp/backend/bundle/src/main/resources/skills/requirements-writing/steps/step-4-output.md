# Step 4 — Generate the Requirements Document

Read `req-session.json` and `req-requirements.json`. Use only the confirmed requirements.
Do not change any requirement text. Omit sections that have zero requirements.
Only include Appendix A if at least one requirement has `tbd: true`.

Write the document using this structure:

```markdown
# [SYSTEM NAME] System Requirements Specification

**Project:** [PREFIX]
**Level:** [System / Subsystem / Component]
**Status:** Draft
**Date:** [today's date]

---

## 1. Introduction

[1–2 sentences: what the system is and what this document covers.]

---

## 2. Functional Requirements

| ID | Requirement | Verification |
|----|-------------|--------------|
| [PREFIX]-F-001 | The system shall ... | Test |

## 3. Performance Requirements

| ID | Requirement | Verification |
|----|-------------|--------------|

## 4. Interface Requirements

| ID | Requirement | Verification |
|----|-------------|--------------|

## 5. Constraint Requirements

| ID | Requirement | Verification |
|----|-------------|--------------|

## 6. Quality Requirements

| ID | Requirement | Verification |
|----|-------------|--------------|

---

## Appendix A: Open TBD Items

| TBD ID  | Req ID          | What Is Unknown | Action Required |
|---------|-----------------|-----------------|-----------------|
| TBD-001 | [PREFIX]-P-001  | [description]   | [action]        |
```

TBD IDs are numbered TBD-001, TBD-002, etc. One row per `[TBD...]` marker in any requirement.

---

## Save the document

Call `writeFile` with `fileType: markdown` to save the document to the project.
Use a filename like `[PREFIX]-SRS.md`.

## Mark skill complete

Call `readFile` for `req-session.json`. Set `"step": "done"`. Call `writeFile` to save.

---

## Clean up intermediate files

The following state files were used during this session and are no longer needed:

- `req-session.json`
- `req-classified.json`
- `req-requirements.json`

Ask the user:
> "The requirements document has been saved. Would you like me to delete the 3 intermediate
> state files (`req-session.json`, `req-classified.json`, `req-requirements.json`)?
> They are no longer needed but can be kept if you want a record of the process."

If the user confirms:
- Call `deleteFile` for each of the three paths above, one at a time.
- Confirm to the user once all three are deleted.

If the user declines:
- Leave the files in place. Do not delete them.

---

This skill is now complete. Do not load any further files.
