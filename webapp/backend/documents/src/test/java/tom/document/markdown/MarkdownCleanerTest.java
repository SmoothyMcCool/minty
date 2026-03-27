package tom.document.markdown;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MarkdownCleanerTest {

	// =========================================================================
	// Pass 1 — Smart quotes
	// =========================================================================

	@Test
	void replacesOpenAndCloseDoubleQuotes() {
		assertEquals("\"hello\"", MarkdownCleaner.fixSmartQuotes("\u201Chello\u201D"));
	}

	@Test
	void replacesOpenAndCloseSingleQuotes() {
		assertEquals("it's fine", MarkdownCleaner.fixSmartQuotes("it\u2019s fine"));
	}

	@Test
	void replacesMixedSmartQuotes() {
		String input = "\u201CIt\u2019s a test\u201D";
		String result = MarkdownCleaner.fixSmartQuotes(input);
		assertEquals("\"It's a test\"", result);
	}

	@Test
	void leavesPlainQuotesUntouched() {
		String input = "\"already straight\" and it's fine";
		assertEquals(input, MarkdownCleaner.fixSmartQuotes(input));
	}

	// =========================================================================
	// Pass 2 — Dashes
	// =========================================================================

	@Test
	void replacesEmDash() {
		assertEquals("before - after", MarkdownCleaner.fixDashes("before \u2014 after"));
	}

	@Test
	void replacesEnDash() {
		assertEquals("2020 - 2024", MarkdownCleaner.fixDashes("2020 \u2013 2024"));
	}

	@Test
	void replacesBothDashTypes() {
		String input = "em\u2014dash and en\u2013dash";
		String result = MarkdownCleaner.fixDashes(input);
		assertEquals("em-dash and en-dash", result);
	}

	@Test
	void leavesHyphenMinusUntouched() {
		String input = "already-hyphenated";
		assertEquals(input, MarkdownCleaner.fixDashes(input));
	}

	// =========================================================================
	// Pass 3 — Non-breaking spaces
	// =========================================================================

	@Test
	void replacesNonBreakingSpace() {
		// U+00A0 between words
		String input = "hello\u00A0world";
		String result = MarkdownCleaner.fixNonBreakingSpaces(input);
		assertEquals("hello world", result);
	}

	@Test
	void replacesMultipleNonBreakingSpaces() {
		String input = "a\u00A0b\u00A0c";
		String result = MarkdownCleaner.fixNonBreakingSpaces(input);
		assertEquals("a b c", result);
	}

	@Test
	void leavesRegularSpacesUntouched() {
		String input = "normal spaces here";
		assertEquals(input, MarkdownCleaner.fixNonBreakingSpaces(input));
	}

	// =========================================================================
	// Pass 4 — Soft hyphens
	// =========================================================================

	@Test
	void removesSoftHyphen() {
		// U+00AD is invisible in Word, meaningless in markdown
		String input = "un\u00ADnec\u00ADes\u00ADsary";
		String result = MarkdownCleaner.fixSoftHyphens(input);
		assertEquals("unnecessary", result);
	}

	@Test
	void removesSoftHyphenAtWordBoundary() {
		String input = "some\u00AD word";
		String result = MarkdownCleaner.fixSoftHyphens(input);
		assertEquals("some word", result);
	}

	// =========================================================================
	// Pass 5 — Broken ordered lists (carried over from MarkdownListCleaner)
	// =========================================================================

	@Test
	void fixesSingleContiguousBlock() {
		String input = "1 asdf\n2 asdf\n3 asdf\n";
		assertEquals("1. asdf\n2. asdf\n3. asdf\n", MarkdownCleaner.clean(input));
	}

	@Test
	void renumbersNonSequentialItems() {
		String input = "1 asdf\n2 asdf\n55 asdf\n";
		assertEquals("1. asdf\n2. asdf\n3. asdf\n", MarkdownCleaner.clean(input));
	}

	@Test
	void handlesTwoBlocksWithTextBetween() {
		String input = "1 asdf\n2 asdf\n55 asdf\nSome more text\n3 asdf\n4 asdf\n7 asdf\n";
		String result = MarkdownCleaner.clean(input);
		assertTrue(result.contains("1. asdf\n2. asdf\n3. asdf\nSome more text\n1. asdf\n2. asdf\n3. asdf"));
	}

	@Test
	void doesNotTouchAlreadyCorrectList() {
		String input = "1. already good\n2. also good\n3. fine\n";
		assertEquals(input, MarkdownCleaner.clean(input));
	}

	@Test
	void doesNotTouchUnorderedList() {
		String input = "- item one\n- item two\n- item three\n";
		assertEquals(input, MarkdownCleaner.clean(input));
	}

	// =========================================================================
	// Pass 6 — Heading spacing
	// =========================================================================

	@Test
	void fixesDoubleSpaceAfterSingleHash() {
		assertEquals("# Title\n", MarkdownCleaner.clean("#  Title\n"));
	}

	@Test
	void fixesDoubleSpaceAfterMultipleHashes() {
		assertEquals("## Section\n", MarkdownCleaner.clean("##  Section\n"));
		assertEquals("### Sub\n", MarkdownCleaner.clean("###   Sub\n"));
	}

	@Test
	void fixesHeadingSpacingAcrossMultipleLines() {
		String input = "#  First\n##  Second\n###  Third\n";
		String result = MarkdownCleaner.fixHeadingSpacing(input);
		assertEquals("# First\n## Second\n### Third\n", result);
	}

	@Test
	void leavesCorrectHeadingsUntouched() {
		String input = "# Good\n## Also Good\n";
		assertEquals(input, MarkdownCleaner.fixHeadingSpacing(input));
	}

	@Test
	void doesNotMatchHashInsideParagraph() {
		// A hash not at start of line should not be affected
		String input = "Some text with a #tag inside it\n";
		assertEquals(input, MarkdownCleaner.fixHeadingSpacing(input));
	}

	// =========================================================================
	// Pass 7 — Normalize (whitespace / blank lines / trim)
	// =========================================================================

	@Test
	void stripsTrailingWhitespace() {
		assertEquals("hello\nworld\n", MarkdownCleaner.clean("hello   \nworld\t\n"));
	}

	@Test
	void collapsesExcessiveBlankLines() {
		assertEquals("para one\n\npara two\n", MarkdownCleaner.clean("para one\n\n\n\n\npara two\n"));
	}

	@Test
	void trimsLeadingAndTrailingBlankLines() {
		assertEquals("some content\n", MarkdownCleaner.clean("\n\n\nsome content\n\n\n"));
	}

	// =========================================================================
	// Integration — all passes together
	// =========================================================================

	@Test
	void fullDocumentWithAllIssues() {
		String input = String.join("\n", "#  Document Title", // pass 6: extra heading space
				"", "The system\u2014as designed\u2013works.", // pass 2: em+en dash
				"", "\u201CHello\u201D said the user.", // pass 1: smart double quotes
				"It\u2019s\u00A0working fine.", // pass 1+3: smart apostrophe + NBSP
				"un\u00ADnec\u00ADes\u00ADsary hyphens.", // pass 4: soft hyphens
				"", "1 First item", // pass 5: broken list
				"2 Second item", "55 Third item", "", "Some prose in between.", "", "4 Alpha", // pass 5: second broken
																								// list, renumbered
				"5 Beta", "");

		String result = MarkdownCleaner.clean(input);

		// Pass 1
		assertTrue(result.contains("\"Hello\""), "smart double quotes");
		assertTrue(result.contains("It's"), "smart apostrophe");

		// Pass 2
		assertTrue(result.contains("system-as"), "em dash → hyphen");
		assertTrue(result.contains("designed-works"), "en dash → hyphen");

		// Pass 3
		assertFalse(result.contains("\u00A0"), "no NBSP remaining");

		// Pass 4
		assertTrue(result.contains("unnecessary"), "soft hyphens removed");

		// Pass 5
		assertTrue(result.contains("1. First item"), "list item 1");
		assertTrue(result.contains("2. Second item"), "list item 2");
		assertTrue(result.contains("3. Third item"), "list item 3 renumbered");
		assertTrue(result.contains("1. Alpha"), "second list renumbered from 1");
		assertTrue(result.contains("2. Beta"), "second list item 2");

		// Pass 6
		assertTrue(result.startsWith("# Document"), "heading space fixed");

		// Pass 7
		assertFalse(result.matches("(?s).*\\n{3,}.*"), "no 3+ consecutive newlines");
		assertTrue(result.endsWith("\n"), "ends with single newline");
	}
}
