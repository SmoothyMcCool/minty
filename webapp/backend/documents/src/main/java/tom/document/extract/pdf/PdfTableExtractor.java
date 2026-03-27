package tom.document.extract.pdf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PdfTableExtractor {

    // Minimum number of multi-space-separated columns to consider a line a table row
    private static final int MIN_TABLE_COLUMNS = 3;

    // Minimum consecutive table-like rows before we commit to a table block
    private static final int MIN_TABLE_ROWS = 2;

    // Header/footer lines must be shorter than this to be candidates
    private static final int MAX_HEADER_FOOTER_LENGTH = 60;

    public List<PdfBlock> detectBlocks(List<PdfLine> lines, int pageCount) {

        Set<String> headerFooters = detectRepeatedHeaderFooters(lines, pageCount);
        List<PdfLine> filtered = lines.stream()
            .filter(l -> !headerFooters.contains(l.getText().trim()))
            .collect(Collectors.toList());

        return detectBlocks(filtered);
    }

    /**
     * Detects lines that appear repeatedly across pages — these are almost certainly
     * running headers or footers and should be stripped before block detection.
     *
     * A line is considered a header/footer if:
     * - It is short (under MAX_HEADER_FOOTER_LENGTH chars)
     * - It appears on at least 30% of pages (minimum 2 occurrences)
     */
    private Set<String> detectRepeatedHeaderFooters(List<PdfLine> lines, int pageCount) {
        Map<String, Integer> frequency = new HashMap<>();

        for (PdfLine line : lines) {
            String text = line.getText().trim();
            if (text.length() < MAX_HEADER_FOOTER_LENGTH) {
                frequency.merge(text, 1, Integer::sum);
            }
        }

        int threshold = Math.max(2, pageCount / 3);

        Set<String> headerFooters = new HashSet<>();
        for (Map.Entry<String, Integer> entry : frequency.entrySet()) {
            if (entry.getValue() >= threshold) {
                headerFooters.add(entry.getKey());
            }
        }

        return headerFooters;
    }

    private List<PdfBlock> detectBlocks(List<PdfLine> lines) {

        List<PdfBlock> blocks = new ArrayList<>();
        float bodyFont = detectBodyFont(lines);

        int i = 0;

        while (i < lines.size()) {

            PdfLine line = lines.get(i);

            // Heading detection
            if (line.getFontSize() > bodyFont * 1.15f) {
                int level = headingLevel(line.getFontSize(), bodyFont);
                blocks.add(new PdfBlock.Heading(level, line.getText()));
                i++;
                continue;
            }

            // List detection
            if (isBullet(line.getText())) {
                List<String> items = new ArrayList<>();
                while (i < lines.size() && isBullet(lines.get(i).getText())) {
                    items.add(lines.get(i).getText().replaceFirst("^[\\-*•]\\s+", ""));
                    i++;
                }
                blocks.add(new PdfBlock.ListBlock(items));
                continue;
            }

            // Table detection — only commit if we see MIN_TABLE_ROWS consecutive rows
            if (looksLikeTableRow(line)) {
                int tableEnd = i;
                while (tableEnd < lines.size() && looksLikeTableRow(lines.get(tableEnd))) {
                    tableEnd++;
                }
                int rowCount = tableEnd - i;

                if (rowCount >= MIN_TABLE_ROWS) {
                    List<List<String>> rows = new ArrayList<>();
                    while (i < tableEnd) {
                        rows.add(splitRow(lines.get(i).getText()));
                        i++;
                    }
                    blocks.add(new PdfBlock.Table(rows));
                    continue;
                }
                // Fewer than MIN_TABLE_ROWS — fall through to paragraph
            }

            // Paragraph — merge consecutive non-structural lines
            StringBuilder paragraph = new StringBuilder(line.getText());
            i++;

            while (i < lines.size() && !isStructural(lines.get(i), bodyFont)) {
                String next = lines.get(i).getText();
                // Join with space; if previous ends with hyphen, strip it (hyphenated line break)
                if (paragraph.charAt(paragraph.length() - 1) == '-') {
                    paragraph.deleteCharAt(paragraph.length() - 1);
                } else {
                    paragraph.append(" ");
                }
                paragraph.append(next);
                i++;
            }

            blocks.add(new PdfBlock.Paragraph(paragraph.toString().trim()));
        }

        return blocks;
    }

    /**
     * Uses the mode (most frequent) font size as the body font rather than the
     * minimum, which would be skewed by footnotes and superscripts.
     */
    private float detectBodyFont(List<PdfLine> lines) {
        return lines.stream()
            .collect(Collectors.groupingBy(
                l -> Math.round(l.getFontSize()),
                Collectors.counting()
            ))
            .entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(e -> (float) e.getKey())
            .orElse(12f);
    }

    /**
     * Maps font size ratio to heading level.
     */
    private int headingLevel(float fontSize, float bodyFont) {
        float ratio = fontSize / bodyFont;
        if (ratio >= 1.6f) return 1;
        if (ratio >= 1.3f) return 2;
        return 3;
    }

    private boolean isBullet(String text) {
        return text.matches("^[\\-*•]\\s+.*");
    }

    private boolean isStructural(PdfLine line, float bodyFont) {
        return line.getFontSize() > bodyFont * 1.15f
            || isBullet(line.getText())
            || looksLikeTableRow(line);
    }

    /**
     * A line looks like a table row if it has at least MIN_TABLE_COLUMNS segments
     * when split on 2+ consecutive spaces.
     */
    private boolean looksLikeTableRow(PdfLine line) {
        String[] parts = line.getText().split(" {2,}");
        return parts.length >= MIN_TABLE_COLUMNS;
    }

    private List<String> splitRow(String row) {
        String[] parts = row.trim().split(" {2,}");
        List<String> cells = new ArrayList<>();
        for (String p : parts) {
            cells.add(p.trim());
        }
        return cells;
    }
}
