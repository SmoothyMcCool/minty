# Step 2 - Classify Each Need

Read the source material from `req-session.json` (`source_summary`) and the original conversation.
Identify each distinct need and assign exactly one type code.

| Code | Type | Assign if the need describes... |
|------|------|---------------------------------|
| F | Functional | An action or behavior the system performs |
| P | Performance | A numeric measure - speed, accuracy, capacity, latency |
| I | Interface | A connection to something external: another system, user, or hardware |
| C | Constraint | A rule the system must obey: standard, regulation, physical limit |
| Q | Quality | Reliability, safety, availability, security, or maintainability |

When a need could fit two types, prefer the more specific one (P over F, Q over F).

---

## Output the classified list

```
1. [F] The system must log all operator commands
2. [P] Response time must be under 2 seconds
3. [I] The system connects to the GPS receiver via RS-422
```

Show this list to the user and ask:
> "Does this look right? Any needs to add, remove, or reclassify?"

Wait for confirmation. Apply any changes the user requests, then show the updated list and confirm once more.

---

## Save state

Once the user confirms, make two file writes:

**1.** Call `writeFile` with filename `req-classified.json`:

```json
{
  "confirmed": true,
  "needs": [
    { "index": 1, "type": "F", "description": "The system must log all operator commands" },
    { "index": 2, "type": "P", "description": "Response time must be under 2 seconds" }
  ]
}
```
*(include every confirmed need, numbered from 1)*

**2.** Call `writeFile` with filename `req-session.json`, preserving all existing fields and updating:
- `"step": "3"`
- `"needs_total": [total number of needs]`
- `"needs_written": 0`

---

NEXT: call `get_skill_file` with filename `steps/step-3a-template.md`
