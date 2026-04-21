# Task: Transform

## Identity
```
taskName:   "Transform"
numInputs:  1
numOutputs: 1
```

## What it does
Applies a configurable sequence of data operations to each incoming packet.
Operations run in order and can be chained to clean, reshape, and transform data records.

Use Transform for common data preparation tasks: removing null or empty fields, renaming or
filtering fields, flattening arrays to strings, or applying SpEL expressions for computed values.
Build the operation sequence in the Definition field - each operation has a name and optional
configuration. Multiple operations in one Transform step run more efficiently than chaining
multiple separate tasks.

## Configuration

| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `Definition` | string | yes | A JSON-encoded string containing an `operations` array |

### IMPORTANT: Double-encoding
The `Definition` value is a **JSON string inside a JSON string**.
You must JSON-encode the inner object - all quotes become `\"`, backslashes become `\\`.

Inner object structure (before encoding):
```json
{
  "operations": [
    { "name": "<operation name>", "configuration": <operation config> },
    ...
  ]
}
```

## Available operations

### Remove Null Fields
Deletes any field whose value is `null`. Does not affect empty strings or zero values.
```json
{ "name": "Remove Null Fields", "configuration": null }
```

### Remove Empty Records
Deletes entire records where all field values are null, empty string, or missing.
```json
{ "name": "Remove Empty Records", "configuration": null }
```

### Flatten Lists
Joins array-valued fields into a single string using a separator.
`precedence` controls which value wins if the same field appears in multiple records: `"last"` or `"first"`.
```json
{ "name": "Flatten Lists", "configuration": { "separator": "\n----\n", "precedence": "last" } }
```

### Remove Fields
Deletes the named fields from every record.
```json
{ "name": "Remove Fields", "configuration": ["field1", "field2"] }
```

### Keep Fields
Retains only the named fields. All other fields are deleted.
```json
{ "name": "Keep Fields", "configuration": ["field1", "field2"] }
```

### Rename Fields
Renames fields. Keys are old names, values are new names.
```json
{ "name": "Rename Fields", "configuration": { "oldName": "newName" } }
```

### Set Field
Sets a specific field to a fixed value on every record.
```json
{ "name": "Set Field", "configuration": { "field": "status", "value": "approved" } }
```

### SpEL Expression
Evaluates a SpEL expression against the packet's `data` list.
The expression receives `data` as the root object - the array of data records.
Use `data.![expression]` to project (map) over every record in the list.
Inside `data.![...]`, `#this` refers to the current record (a Map).
```json
{ "name": "SpEL Expression", "configuration": "data.![#this.put('field', #this['field']?.toUpperCase())]" }
```
```json
{ "name": "SpEL Expression", "configuration": "data.![#this.put('count', (#this['count'] ?: 0) + 1)]" }
```

## Configuration examples

### Single operation - remove nulls
```json
{
  "Definition": "{\"operations\":[{\"name\":\"Remove Null Fields\",\"configuration\":null}]}"
}
```

### Multiple operations - remove nulls then flatten lists
```json
{
  "Definition": "{\"operations\":[{\"name\":\"Remove Null Fields\",\"configuration\":null},{\"name\":\"Flatten Lists\",\"configuration\":{\"separator\":\"\\n----\\n\",\"precedence\":\"last\"}}]}"
}
```

### Multiple operations - SpEL uppercase then keep specific fields
```json
{
  "Definition": "{\"operations\":[{\"name\":\"SpEL Expression\",\"configuration\":\"data.![#this.put('name', #this['name']?.toUpperCase())]\"},{\"name\":\"Keep Fields\",\"configuration\":[\"id\",\"name\"]}]}"
}
```

## Step template

```json
{
  "taskName": "Transform",
  "stepName": "<descriptive label>",
  "id": "1",
  "loggingActive": true,
  "configuration": {
    "Definition": "<escaped json string>"
  },
  "layout": { "x": 0, "y": 0, "numInputs": 1, "numOutputs": 1 }
}
```

## Common uses
- Cleaning data: remove nulls, remove empty records
- Reshaping: rename fields, keep/remove specific fields
- Transforming values: SpEL expressions for computed fields
- Normalising: flatten arrays before further processing
