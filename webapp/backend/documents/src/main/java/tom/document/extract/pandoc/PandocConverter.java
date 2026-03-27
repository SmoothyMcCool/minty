package tom.document.extract.pandoc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PandocConverter {

	private static final Logger logger = LogManager.getLogger(PandocConverter.class);

	private final String pandocPath;
	private final String outputFormat;
	private final String wrap;
	private final boolean noHighlight;
	private final boolean stripComments;
	private final String luaFilter;
	private final List<String> extraArgs;

	public PandocConverter(String pandocPath, String outputFormat, String luaFilter, boolean noHighlight,
			boolean stripComments, String wrap, List<String> extraArgs) {
		this.pandocPath = pandocPath;
		this.outputFormat = outputFormat;
		this.wrap = wrap;
		this.noHighlight = noHighlight;
		this.stripComments = stripComments;
		this.luaFilter = luaFilter;
		this.extraArgs = extraArgs;
	}

	/**
	 * Converts an HTML string to Markdown by writing it to a temporary file and
	 * passing it through Pandoc with --from=html.
	 */
	public String convertHtmlToMarkdown(String html) throws IOException, InterruptedException {
		File tmp = Files.createTempFile("html-", ".html").toFile();
		try {
			Files.writeString(tmp.toPath(), html, StandardCharsets.UTF_8);
			return convert(tmp, "html");
		} finally {
			tmp.delete();
		}
	}

	/**
	 * Runs pandoc on the given file, inferring the input format from the file
	 * extension/content.
	 */
	public String convert(File file) throws IOException, InterruptedException {
		return convert(file, null);
	}

	/**
	 * Runs pandoc on the given file. If inputFormat is non-null,
	 * --from=&lt;inputFormat&gt; is added to the command (e.g. "html" when
	 * converting Tika HTML output).
	 *
	 * Stdout and stderr are read concurrently to prevent the process blocking on a
	 * full pipe buffer, which can deadlock when pandoc emits large amounts of
	 * warnings on stderr while stdout is not yet being drained.
	 */
	public String convert(File file, String inputFormat) throws IOException, InterruptedException {
		List<String> command = buildCommand(file, inputFormat);
		ProcessBuilder pb = new ProcessBuilder(command);
		pb.redirectErrorStream(false);
		logger.info("Executing command {}", String.join(" ", pb.command()));

		Process process = pb.start();

		CompletableFuture<String> stdoutFuture = readStreamAsync(process.getInputStream());
		CompletableFuture<String> stderrFuture = readStreamAsync(process.getErrorStream());

		String output = stdoutFuture.join();
		String errors = stderrFuture.join();
		int exitCode = process.waitFor();

		if (exitCode != 0) {
			throw new RuntimeException(
					"Pandoc failed (exit " + exitCode + ") for file: " + file.getName() + "\n" + errors);
		}

		if (!errors.isBlank()) {
			logger.warn("Pandoc warnings for {}: {}", file.getName(), errors);
		}

		return output;
	}

	private static CompletableFuture<String> readStreamAsync(InputStream stream) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
			} catch (IOException e) {
				throw new RuntimeException("Failed to read pandoc output stream", e);
			}
		});
	}

	private List<String> buildCommand(File file, String inputFormat) {
		List<String> cmd = new ArrayList<>();
		cmd.add(pandocPath);

		if (inputFormat != null && !inputFormat.isBlank()) {
			cmd.add("--from=" + inputFormat);
		}

		cmd.add("--to=" + outputFormat);
		cmd.add("--wrap=" + wrap);

		if (noHighlight) {
			cmd.add("--syntax-highlighting=none");
		}

		if (stripComments) {
			cmd.add("--strip-comments");
		}

		if (luaFilter != null && !luaFilter.isBlank()) {
			cmd.add("--lua-filter=" + luaFilter);
		}

		if (extraArgs != null && !extraArgs.isEmpty()) {
			cmd.addAll(extraArgs);
		}

		cmd.add(file.getAbsolutePath());
		return cmd;
	}
}