# Step 3B — Quality Checklist

Run these checks on the requirement you just wrote in Step 3A.
If any check FAILS, rewrite the requirement and show the corrected version before continuing.

```
CHECK 1: More than one "shall" in the sentence?
  YES → Split into two separate requirements. FAIL.

CHECK 2: Contains a forbidden word?
  [fast, quickly, soon, timely, slow, rapid,
   adequate, sufficient, robust,
   reliable (with no number), easy, simple, user-friendly, intuitive,
   support (as main verb), handle,
   maximize, minimize, optimize (with no number),
   and/or, etc., state-of-the-art]
  YES → Replace it using the table below. FAIL.

CHECK 3 (P and Q types only): Missing a number and no [TBD]?
  YES → Add the number or write [TBD — reason]. FAIL.

CHECK 4: Names a specific technology, vendor, or language?
  YES → Remove it. Describe what the system does, not how. FAIL.

CHECK 5: Uses "shall not" where a positive form is possible?
  YES → Rewrite it positively. FAIL.
```

Forbidden word replacements:
| Word | Replace with |
|------|-------------|
| support | provide / transmit / display / store |
| handle | receive / reject / route |
| fast / quickly / soon | within [N] [units] |
| reliable (no number) | achieve MTBF ≥ [N] hours |
| robust | [describe the specific failure mode it must survive] |
| adequate / sufficient | ≥ [N] [units] |
| user-friendly | [specific measurable behavior] |
| and/or | split into two requirements |
| optimize / maximize / minimize | achieve ≥ / ≤ [N] [units] |

---

Output PASS or the corrected requirement text.

NEXT: call `get_skill_file` with filename `steps/step-3c-verify.md`
