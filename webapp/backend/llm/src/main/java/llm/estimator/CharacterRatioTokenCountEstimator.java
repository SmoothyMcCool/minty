package llm.estimator;

import org.springframework.ai.content.MediaContent;
import org.springframework.ai.tokenizer.TokenCountEstimator;

public class CharacterRatioTokenCountEstimator implements TokenCountEstimator {

	// ~4.5 chars/token is a published benchmark figure for Gemma 3's English
	// tokenization.
	// Treat this as a starting point, not a verified number for Gemma 4
	// specifically —
	// calibrate against real usage below once you have data.
	private final double charsPerToken;

	public CharacterRatioTokenCountEstimator() {
		this(4.5);
	}

	public CharacterRatioTokenCountEstimator(double charsPerToken) {
		this.charsPerToken = charsPerToken;
	}

	@Override
	public int estimate(String text) {
		return (text == null || text.isEmpty()) ? 0 : (int) Math.ceil(text.length() / charsPerToken);
	}

	@Override
	public int estimate(MediaContent content) {
		return estimate(content.getText());
	}

	@Override
	public int estimate(Iterable<MediaContent> messages) {
		int total = 0;
		for (MediaContent m : messages) {
			total += estimate(m);
		}
		return total;
	}
}
