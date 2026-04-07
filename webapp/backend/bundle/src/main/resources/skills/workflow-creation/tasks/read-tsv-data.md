# Task: Read TSV Data

## Identity
```
taskName:   "Read TSV Data"
numInputs:  1
numOutputs: 1
```

## What it does
Parses TSV (tab-separated values) text from the input packet's `text` field into structured data records.
The first row of the TSV is treated as the header row — its values become the field names in the output data.
Each subsequent row becomes one data record (a map of field name to value).
All resulting records are collected into a single output packet's `data` list.

Pair with Emit Document to process uploaded spreadsheet or TSV files.

## Configuration
No configuration required.

```json
{}
```

## Input requirement
- The input packet must have at least one item in `text`.
- Each `text` item is parsed as a separate TSV block.
- All parsed records from all text items are merged into the output `data`.

## Example
Input text:
```
name\tage\tcity
Alice\t30\tOttawa
Bob\t25\tToronto
```

Output data:
```json
[
  { "name": "Alice", "age": "30", "city": "Ottawa" },
  { "name": "Bob",   "age": "25", "city": "Toronto" }
]
```

## Connections
- 1 input (index 0).
- 1 output (index 0).

## Step template

```json
{
  "taskName": "Read TSV Data",
  "stepName": "<descriptive label>",
  "id": 1,
  "loggingActive": true,
  "configuration": {},
  "layout": { "x": 0, "y": 0, "numInputs": 1, "numOutputs": 1 }
}
```

## Common uses
- After Emit Document when the uploaded file is a spreadsheet — converts its TSV content into structured data records
- Parsing any tab-delimited text produced by an upstream step
