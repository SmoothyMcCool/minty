# Step 3A - Write the Requirement Text

## Which requirement to write next

Read `req-session.json` → check `needs_written`.
Read `req-classified.json` → take the need at index `needs_written + 1`.

Example: if `needs_written` is 2, write the need at index 3.

---

## Fill the template for this need's type code

**F - Functional**
```
The [SYSTEM NAME] shall [ACTION VERB] [OBJECT] [CONDITION].
```
Use specific verbs: transmit, record, display, calculate, reject, store, generate.
Do NOT use: support, handle, process, manage.

**P - Performance**
```
The [SYSTEM NAME] shall [ACTION VERB] [OBJECT] within [NUMBER] [UNITS] under [OPERATING CONDITION].
```
NUMBER + UNITS required. Unknown → `[TBD - reason]`.
Default OPERATING CONDITION: "nominal operating conditions".

**I - Interface**
```
The [SYSTEM NAME] shall [receive from / transmit to / exchange with] [EXTERNAL ENTITY] [DATA] via [PROTOCOL].
```
Unknown PROTOCOL → `[TBD - protocol to be defined]`.

**C - Constraint**
```
The [SYSTEM NAME] shall comply with [STANDARD NAME AND VERSION].
```
or: `The [SYSTEM NAME] shall [operate within / not exceed] [LIMIT with units].`

**Q - Quality**
```
The [SYSTEM NAME] shall achieve [METRIC NAME] of [NUMBER] [UNITS] as measured by [MEASUREMENT METHOD].
```
Unknown NUMBER → `[TBD - to be determined during preliminary design]`.

---

Output the requirement text you wrote.

NEXT: call `get_skill_file` with filename `steps/step-3b-checklist.md`
