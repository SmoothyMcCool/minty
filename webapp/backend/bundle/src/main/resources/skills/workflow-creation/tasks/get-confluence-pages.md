# Task: Get Confluence Pages

## Identity
```
taskName:   "Get Confluence Pages"
numInputs:  1
numOutputs: 1
```

## What it does
Fetches the text content of one or more Confluence pages by their page IDs.
The page body is returned as text. Pages are cached to avoid redundant requests.

Page IDs can be specified in configuration, or overridden at runtime via the input packet's data.
If the input packet's `data[0]` contains a `"Page IDs"` key (a JSON array of page ID strings), those IDs are used instead of (or in addition to) the configured ones.

Input `data` must contain exactly one element.

## Configuration

| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `Confluence Base URL` | string | yes | Base URL of your Confluence instance (e.g. `https://mycompany.atlassian.net`) |
| `Username` | string | yes | Confluence username (email address) |
| `Confluence Access Token` | string | yes | API token or personal access token |
| `Confluence Use Bearer Authorization` | string | no | `"true"` for bearer token auth, `"false"` for basic auth (default) |
| `Page IDs` | JSON string array | yes | JSON array of Confluence page ID strings |
| `Maximum Characters to Read from Page` | string | no | Max characters per page. Default `"20000"` |
| `Page Concatenation Strategy` (enum) | string | no | How to combine multiple pages — see below |

### Page Concatenation Strategy options
- `"Concatenated"` — all pages joined into one text string (default)
- `"Array"` — each page's text added as a separate entry in the text array
- `"MultiPacket"` — one packet emitted per page

## Configuration example

```json
{
  "Confluence Base URL": "https://mycompany.atlassian.net",
  "Username": "user@example.com",
  "Confluence Access Token": "your-api-token",
  "Confluence Use Bearer Authorization": "false",
  "Page IDs": "[\"123456\", \"789012\"]",
  "Maximum Characters to Read from Page": "20000",
  "Page Concatenation Strategy": "Concatenated"
}
```

## Output
Depends on the concatenation strategy:
- `Concatenated`: one packet, `text[0]` = all pages joined with `\n\n\n`
- `Array`: one packet, `text` has one entry per page
- `MultiPacket`: one packet per page, each with `text[0]` = that page's content

In all cases, input `data` and `id` are preserved in the output packet.

## Connections
- 1 input (index 0) — input packet data may override page IDs
- 1 output (index 0)

## Step template

```json
{
  "taskName": "Get Confluence Pages",
  "stepName": "<descriptive label>",
  "id": "1",
  "loggingActive": true,
  "configuration": {
    "Confluence Base URL": "<base url>",
    "Username": "<username>",
    "Confluence Access Token": "<token>",
    "Confluence Use Bearer Authorization": "false",
    "Page IDs": "[\"<page-id>\"]",
    "Maximum Characters to Read from Page": "20000",
    "Page Concatenation Strategy": "Concatenated"
  },
  "layout": { "x": 0, "y": 0, "numInputs": 1, "numOutputs": 1 }
}
```
