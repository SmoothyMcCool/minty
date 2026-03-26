package tom.document.service.extract.pdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

public class StructuredPdfStripper extends PDFTextStripper {

    // Fragments on the same visual line are within this many points of each other
    private static final float Y_THRESHOLD = 2.0f;

    private final List<LineFragment> fragments = new ArrayList<>();
    private final List<PdfLine> lines = new ArrayList<>();
    private int fragmentCount = 0;
    private int pageCount = 0;

    public StructuredPdfStripper() throws IOException {
        setSortByPosition(true);
    }

    public void process(PDDocument doc) throws IOException {
        this.pageCount = doc.getNumberOfPages();
        getText(doc);
        groupFragmentsIntoLines();
    }

    public List<PdfLine> getLines() {
        return lines;
    }

    public int getFragmentCount() {
        return fragmentCount;
    }

    public int getPageCount() {
        return pageCount;
    }

    @Override
    protected void writeString(String text, List<TextPosition> positions) {
        if (text.isBlank() || positions.isEmpty()) {
            return;
        }

        fragmentCount++;
        TextPosition first = positions.get(0);
        fragments.add(new LineFragment(
            text,
            first.getFontSizeInPt(),
            first.getFont().getName(),
            first.getXDirAdj(),
            first.getYDirAdj()
        ));
    }

    /**
     * Groups raw text fragments into logical lines by proximity of y coordinate.
     * PDFBox calls writeString once per text fragment (word, run, etc.) — multiple
     * fragments share the same visual line but have slightly differing y values due
     * to floating point. We bucket them within Y_THRESHOLD and concatenate.
     */
    private void groupFragmentsIntoLines() {
        if (fragments.isEmpty()) {
            return;
        }

        // Use a TreeMap so buckets are processed top-to-bottom (ascending y)
        TreeMap<Float, List<LineFragment>> buckets = new TreeMap<>();

        for (LineFragment fragment : fragments) {
            float bucketKey = findOrCreateBucket(buckets, fragment.y);
            buckets.get(bucketKey).add(fragment);
        }

        for (Map.Entry<Float, List<LineFragment>> entry : buckets.entrySet()) {
            List<LineFragment> group = entry.getValue();

            // Sort fragments left-to-right within the line
            group.sort((a, b) -> Float.compare(a.x, b.x));

            StringBuilder text = new StringBuilder();
            float maxFontSize = 0f;
            String fontName = group.get(0).fontName;
            float x = group.get(0).x;
            float y = entry.getKey();

            for (LineFragment f : group) {
                if (!text.isEmpty()) {
                    text.append(" ");
                }
                text.append(f.text.trim());
                if (f.fontSize > maxFontSize) {
                    maxFontSize = f.fontSize;
                    fontName = f.fontName;
                }
            }

            String lineText = text.toString().trim();
            if (!lineText.isBlank()) {
                lines.add(new PdfLine(lineText, maxFontSize, fontName, x, y));
            }
        }
    }

    /**
     * Finds an existing bucket whose key is within Y_THRESHOLD of the given y,
     * or creates a new bucket at y.
     */
    private float findOrCreateBucket(TreeMap<Float, List<LineFragment>> buckets, float y) {
        Map.Entry<Float, List<LineFragment>> floor = buckets.floorEntry(y);
        if (floor != null && Math.abs(floor.getKey() - y) <= Y_THRESHOLD) {
            return floor.getKey();
        }
        Map.Entry<Float, List<LineFragment>> ceiling = buckets.ceilingEntry(y);
        if (ceiling != null && Math.abs(ceiling.getKey() - y) <= Y_THRESHOLD) {
            return ceiling.getKey();
        }
        buckets.put(y, new ArrayList<>());
        return y;
    }

    // Internal fragment record - not exposed outside this class
    private static class LineFragment {
        final String text;
        final float fontSize;
        final String fontName;
        final float x;
        final float y;

        LineFragment(String text, float fontSize, String fontName, float x, float y) {
            this.text = text;
            this.fontSize = fontSize;
            this.fontName = fontName;
            this.x = x;
            this.y = y;
        }
    }
}
