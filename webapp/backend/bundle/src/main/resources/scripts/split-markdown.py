#!/usr/bin/env python3
"""
Markdown section splitter — equivalent to MarkdownSectionSplitter.java.

Usage:
    split_markdown.py <markdown_file> <max_heading_level>

Output:
    JSON array written to stdout, one object per section:
    [
      {
        "index":       1,
        "level":       2,
        "title":       "Introduction",
        "content":     "...",
        "parentIndex": null
      },
      ...
    ]

Exit codes:
    0  success
    1  bad arguments / file not found
    2  unexpected error
"""

import json
import sys


def is_heading(line: str, max_heading_level: int) -> bool:
    if not line.startswith("#"):
        return False
    i = 0
    while i < len(line) and line[i] == "#":
        i += 1
    # Must be 1–max_heading_level hashes followed by whitespace
    return i <= max_heading_level and i < len(line) and line[i].isspace()


def count_leading_hashes(line: str) -> int:
    count = 0
    while count < len(line) and line[count] == "#":
        count += 1
    return count


def find_parent_index(sections: list[dict], i: int) -> int | None:
    current_level = sections[i]["level"]
    if current_level == 0:
        return None  # preamble has no parent
    for j in range(i - 1, -1, -1):
        if sections[j]["level"] < current_level:
            return sections[j]["index"]
    return None


def split(markdown: str, max_heading_level: int) -> list[dict]:
    sections: list[dict] = []

    # Normalise: strip BOM, normalise line endings
    normalized = (
        markdown.replace("\uFEFF", "")
        .replace("\r\n", "\n")
        .replace("\r", "\n")
    )

    lines = normalized.split("\n")

    current: dict | None = None
    preamble_lines: list[str] = []

    for line in lines:
        trimmed = line.rstrip()  # strip trailing whitespace (mirrors stripTrailing)

        if is_heading(trimmed, max_heading_level):
            if current is not None:
                current["content"] = current["content"].strip()
                sections.append(current)

            level = count_leading_hashes(trimmed)
            import re
            title = re.sub(r"^#{1," + str(max_heading_level) + r"}\s+", "", trimmed).strip()

            current = {
                "level":   level,
                "title":   title,
                "content": "",
                "index":   0,           # filled in below
                "parentIndex": None,    # filled in below
            }
        elif current is not None:
            current["content"] += line + "\n"
        else:
            preamble_lines.append(line)

    # Flush the last section
    if current is not None:
        current["content"] = current["content"].strip()
        sections.append(current)

    # Prepend preamble if non-empty
    preamble_text = "\n".join(preamble_lines).strip()
    if preamble_text:
        sections.insert(0, {
            "level":       0,
            "title":       "Preamble",
            "content":     preamble_text,
            "index":       0,
            "parentIndex": None,
        })

    # Assign 1-based indices
    for i, s in enumerate(sections):
        s["index"] = i + 1

    # Resolve parent indices
    for i in range(len(sections)):
        sections[i]["parentIndex"] = find_parent_index(sections, i)

    return sections


def main() -> None:
    if len(sys.argv) != 3:
        print(
            "Usage: split_markdown.py <markdown_file> <max_heading_level>",
            file=sys.stderr,
        )
        sys.exit(1)

    markdown_path = sys.argv[1]
    try:
        max_heading_level = int(sys.argv[2])
    except ValueError:
        print(f"max_heading_level must be an integer, got: {sys.argv[2]}", file=sys.stderr)
        sys.exit(1)

    try:
        with open(markdown_path, encoding="utf-8") as fh:
            markdown = fh.read()
    except FileNotFoundError:
        print(f"File not found: {markdown_path}", file=sys.stderr)
        sys.exit(1)
    except OSError as exc:
        print(f"Could not read file: {exc}", file=sys.stderr)
        sys.exit(1)

    try:
        sections = split(markdown, max_heading_level)
        print(json.dumps(sections, ensure_ascii=False, indent=2))
    except Exception as exc:  # noqa: BLE001
        print(f"Unexpected error: {exc}", file=sys.stderr)
        sys.exit(2)


if __name__ == "__main__":
    main()