-- ============================================================
-- pandoc-clean.lua
-- Post-processing filter for Word → Markdown conversion.
-- ============================================================

-- IMPORTANT: do not touch Header elements
function Header(el)
    return el
end

-- ============================================================
-- Lists
-- ============================================================

-- Normalize ordered lists to use the default style and delimiter
-- (prevents pandoc guessing wrong styles from Word e.g. OneParen).
-- This is the primary fix for broken "1 text" list output.
function OrderedList(el)
    -- Guard: listAttributes must exist before we touch it
    if not el.listAttributes then
        io.stderr:write("[pandoc-clean] WARNING: OrderedList has no listAttributes, skipping.\n")
        return el
    end

    el.listAttributes.style     = "DefaultStyle"
    el.listAttributes.delimiter = "DefaultDelim"
    return el
end

-- ============================================================
-- Inline formatting
-- ============================================================

-- Flatten spans (coloured text, underlines, custom character styles).
-- Returning el.content (a list) tells Pandoc to splice the inlines
-- in place - do not change this to return el.
function Span(el)
    return el.content
end

-- Convert underline to emphasis (rather than raw <u> tags)
function Underline(el)
    return pandoc.Emph(el.content)
end

-- Flatten SmallCaps - Word uses these for certain heading styles;
-- they produce awkward output in plain markdown.
-- Returns a list so Pandoc splices correctly (Pandoc >= 3.x).
function SmallCaps(el)
    return el.content
end

-- ============================================================
-- Block formatting
-- ============================================================

-- Flatten Divs (text boxes, frames, custom block styles).
-- Returning el.content (a list of blocks) splices them in place.
function Div(el)
    return el.content
end

-- Remove empty paragraphs that pandoc generates from Word page
-- breaks, empty lines, and spacing-only paragraphs.
function Para(el)
    if not el.content or #el.content == 0 then
        return {}
    end

    -- Drop paragraphs that contain only a LineBreak (not SoftBreak)
    if #el.content == 1 and el.content[1].t == "LineBreak" then
        return {}
    end

    return el
end

-- ============================================================
-- Tables
-- ============================================================

-- Strip inline formatting noise from table cells (Word tables
-- commonly carry Bold, Italic, and Span clutter on every cell).
function Table(el)
    -- Guard: walk_block can fail on malformed tables from Word
    local ok, result = pcall(function()
        return pandoc.walk_block(el, {
            Strong = function(s) return s.content end,
            Emph   = function(e) return e.content end,
            Span   = function(s) return s.content end,
        })
    end)

    if not ok then
        io.stderr:write("[pandoc-clean] WARNING: Table walk failed, returning table unmodified. Error: "
            .. tostring(result) .. "\n")
        return el
    end

    return result
end