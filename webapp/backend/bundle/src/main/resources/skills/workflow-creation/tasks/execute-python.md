# Task: Execute Python

## Identity
```
taskName:   "Execute Python"
numInputs:  1
numOutputs: 1
```

## What it does
Executes a Python script against the input packet.
The input packet is passed to the script as a JSON file (argv[1]).
The script must write its output to a second JSON file (argv[2]).
The output must conform to the Packet JSON structure.

## Configuration

| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `Python Code` | string | yes | The full Python script source code |

### Python script contract
The script receives two command-line arguments:
- `sys.argv[1]` - path to a JSON file containing the input packet
- `sys.argv[2]` - path where the script must write the output JSON

The output JSON must be a valid Packet with all three fields present:
```json
{
  "id": "<string - use empty string if no meaningful ID>",
  "text": ["<string>", ...],
  "data": [{ "<key>": "<value>", ... }, ...]
}
```

`text` must be an array of strings (not a bare string). `data` must be an array of objects (not a bare object). Use `[]` for either field if there is no content - never omit them.

### Script template
```python
import json, sys

def read_dict(inFile):
    try:
        with open(inFile, 'r') as f:
            return json.load(f)
    except: exit(-1)

def write_to_file(data, outFile):
    with open(outFile, "w") as file:
        json.dump(data, file, indent=4)

def process_data(data):
    # YOUR CODE HERE
    return data

def main():
    if len(sys.argv) == 3:
        data = read_dict(sys.argv[1])
        result = process_data(data)
        write_to_file(result, sys.argv[2])

if __name__ == "__main__":
    main()
```

## Configuration example

```json
{
  "Python Code": "import json, sys\n\ndef main():\n    with open(sys.argv[1]) as f:\n        data = json.load(f)\n    data['text'] = [t.upper() for t in data.get('text', [])]\n    with open(sys.argv[2], 'w') as f:\n        json.dump(data, f)\n\nif __name__ == '__main__':\n    main()"
}
```

## Connections
- 1 input (index 0).
- 1 output (index 0).

## Step template

```json
{
  "taskName": "Execute Python",
  "stepName": "<descriptive label>",
  "id": "1",
  "loggingActive": true,
  "configuration": {
    "Python Code": "<python script as a single escaped string>"
  },
  "layout": { "x": 0, "y": 0, "numInputs": 1, "numOutputs": 1 }
}
```
