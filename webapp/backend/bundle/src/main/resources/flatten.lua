-- IMPORTANT: do not touch Header elements
function Header(el)
    return el  -- pass through unchanged
end

-- Flatten spans (coloured text, underlines, custom character styles)
function Span(el)
    return el.content  -- just return the text content, drop the formatting
end

-- Flatten Divs (text boxes, frames, custom block styles)
function Div(el)
    return el.content
end

-- Convert underline to emphasis (rather than <u> tags)
function Underline(el)
    return pandoc.Emph(el.content)
end

-- Strip custom styles from tables but keep the table
function Table(el)
    return el
end