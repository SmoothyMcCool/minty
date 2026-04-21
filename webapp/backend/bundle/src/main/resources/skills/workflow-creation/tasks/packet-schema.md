# Packet schema

Every step in a workflow sends and receives Packets. This file defines the Packet
structure and how to reference values inside one.

---

## Packet JSON structure

```json
{
  "id":   "<string>",
  "text": ["<string>", "<string>", ...],
  "data": [
    { "fieldName": <value>, ... },
    { "fieldName": <value>, ... }
  ]
}
```

| Field  | Type             | Default | Notes |
|--------|------------------|---------|-------|
| `id`   | string           | `""`    | Plain string - never a UUID object |
| `text` | array of strings | `[]`    | Always an array, never a bare string |
| `data` | array of objects | `[]`    | Always an array of objects, never a bare object |

All three fields must always be present. Use empty arrays `[]` for `text` and `data`
when there is no content, and `""` for `id` when there is no meaningful identifier.

---

## Valid packet examples

Packet with structured data only:
```json
{
  "id": "customer-42",
  "text": [],
  "data": [
    { "name": "Alice", "score": 95, "status": "active" }
  ]
}
```

Packet with text only:
```json
{
  "id": "",
  "text": ["Summarise the following document and extract action items."],
  "data": []
}
```

Packet with multiple data records:
```json
{
  "id": "batch-1",
  "text": [],
  "data": [
    { "name": "Alice", "score": 95 },
    { "name": "Bob",   "score": 72 },
    { "name": "Carol", "score": 88 }
  ]
}
```

Packet with both text and data (e.g. after Query LLM with source data):
```json
{
  "id": "customer-42",
  "text": ["Alice is an active customer with a high engagement score."],
  "data": [
    { "name": "Alice", "score": 95, "status": "active" }
  ]
}
```

---

## Path syntax for referencing packet values

Used in: SetId (`ID Element`), Sort (`ID Element`), Format Text (`Format`),
Transform (`SpEL Expression`), Branch (`Branch Expression`), Loop (`Branch Expression`).

### Simple shorthands
| Path   | Returns |
|--------|---------|
| `id`   | The packet's id string |
| `text` | All text entries joined by newline |

### JSONPath (for data and indexed access)
The `$` prefix is optional - it is added automatically if omitted.

| Path | Returns |
|------|---------|
| `data[0].fieldName` | Value of `fieldName` in the first data record |
| `data[1].fieldName` | Value of `fieldName` in the second data record |
| `text[0]`           | The first text string |
| `text[1]`           | The second text string |
| `data.fieldName`    | Shorthand for `data[0].fieldName` |
| `text.value`        | Shorthand for `text[0].value` |

---

## SpEL expressions - Branch vs Loop

Branch and Loop both use SpEL but with **different root objects**.

### Branch - root is `#p` (the Packet)
The packet is bound to the variable `#p`. Access fields via `#p.`:

```
#p.id == 'customer-42'
#p.text.size() > 0
#p.data[0]['status'] == 'active'
#p.data[0]['score'] > 80
#p.text[0].contains('error')
```

### Loop - root IS the packet itself (no `#p`)
The packet itself is the root object. Access fields directly:

```
data[0]['retryCount'] < 3
data[0]['status'] != 'complete'
text[0].length() < 500
data.score > 0
```

Shorthand expansion applies in Loop: `data.fieldName` → `data[0].fieldName`.
