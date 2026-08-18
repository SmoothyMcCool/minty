package tom.project.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tom.api.ProjectId;
import tom.api.UserId;
import tom.api.model.document.Document;
import tom.api.model.document.DocumentSearchMatch;
import tom.api.model.document.DocumentSearchResult;
import tom.api.model.project.FileSearchMatch;
import tom.api.model.project.FileSearchResult;
import tom.api.model.project.KnowledgeGrepResult;
import tom.api.model.project.KnowledgeItemInfo;
import tom.api.model.project.KnowledgeItemType;
import tom.api.model.project.KnowledgeSearchMatch;
import tom.api.model.project.KnowledgeSearchResult;
import tom.api.model.project.NodeInfo;
import tom.api.services.DocumentService;
import tom.api.services.KnowledgeService;
import tom.api.services.ProjectService;

@Service
public class KnowledgeServiceImpl implements KnowledgeService {

	private final ProjectService projectService;
	private final DocumentService documentService;

	public KnowledgeServiceImpl(ProjectService projectService, DocumentService documentService) {
		this.projectService = projectService;
		this.documentService = documentService;
	}

	@Override
	public List<KnowledgeItemInfo> find(UserId userId, ProjectId projectId, String query, int maxResults) {

		if (query == null || query.isBlank()) {
			return List.of();
		}

		if (maxResults < 1) {
			return List.of();
		}

		String normalizedQuery = query.toLowerCase(Locale.ROOT);
		List<KnowledgeItemInfo> results = new ArrayList<>();

		// Files
		for (NodeInfo node : projectService.searchByFilter(userId, projectId, query)) {
			if (node.getFileType() == null) {
				continue;
			}

			String path = node.getPath();
			String name = extractName(path);

			results.add(new KnowledgeItemInfo(KnowledgeItemType.FILE, name, path, null));

			if (results.size() >= maxResults) {
				return results;
			}
		}

		// Documents
		for (Document document : documentService.listDocuments(userId, projectId)) {
			if (document.title() == null) {
				continue;
			}
			if (!document.title().toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
				continue;
			}

			results.add(new KnowledgeItemInfo(KnowledgeItemType.DOCUMENT, document.title(),
					"documents/" + document.title(), document.summary()));

			if (results.size() >= maxResults) {
				return results;
			}
		}

		return results;
	}

	@Override
	@Transactional(readOnly = true)
	public KnowledgeGrepResult grep(UserId userId, ProjectId projectId, String path, String pattern,
			boolean caseSensitive, int maxResults, int contextBefore, int contextAfter) {

		if (pattern == null || pattern.isBlank()) {
			return new KnowledgeGrepResult(List.of(), false);
		}

		if (maxResults < 1) {
			return new KnowledgeGrepResult(List.of(), false);
		}

		if (contextBefore < 0 || contextAfter < 0) {
			return new KnowledgeGrepResult(List.of(), false);
		}

		List<KnowledgeSearchResult> results = new ArrayList<>();

		int remaining = maxResults;
		boolean truncated = false;

		/*
		 * Request one additional result from the underlying service so that we can
		 * determine whether the unified search contains more matches than requested.
		 */
		int searchLimit = maxResults + 1;

		// Search project files.
		List<FileSearchResult> fileResults = projectService.grep(userId, projectId, path, pattern, caseSensitive,
				searchLimit, contextBefore, contextAfter);

		for (FileSearchResult file : fileResults) {
			List<KnowledgeSearchMatch> matches = new ArrayList<>();

			for (FileSearchMatch match : file.getMatches()) {
				if (matches.size() >= remaining) {
					truncated = true;
					break;
				}

				matches.add(new KnowledgeSearchMatch(match.getLine(), null, null, match.getText(), match.getContext(),
						null));
			}

			if (!matches.isEmpty()) {
				String filePath = file.getPath();
				results.add(
						new KnowledgeSearchResult(KnowledgeItemType.FILE, extractName(filePath), filePath, matches));
				remaining -= matches.size();
			}

			if (truncated) {
				break;
			}
		}

		/*
		 * If the file search already proved that there are more file matches than the
		 * requested limit, there is no need to search documents.
		 */
		if (truncated) {
			return new KnowledgeGrepResult(results, true);
		}

		/*
		 * If the file search exactly filled the result limit, we still need to check
		 * whether at least one document match exists. A single document match is
		 * sufficient to establish that the unified result set is truncated.
		 */
		if (remaining == 0) {
			List<DocumentSearchResult> documentResults = documentService.grep(userId, projectId, pattern, caseSensitive,
					1, contextBefore, contextAfter);
			if (!documentResults.isEmpty()) {
				return new KnowledgeGrepResult(results, true);
			}
			return new KnowledgeGrepResult(results, false);
		}

		/*
		 * Search knowledge-base documents with whatever portion of the result limit
		 * remains. Request one additional match so that we can detect truncation.
		 */
		int documentSearchLimit = remaining + 1;

		List<DocumentSearchResult> documentResults = documentService.grep(userId, projectId, pattern, caseSensitive,
				documentSearchLimit, contextBefore, contextAfter);

		for (DocumentSearchResult document : documentResults) {
			List<KnowledgeSearchMatch> matches = new ArrayList<>();

			for (DocumentSearchMatch match : document.getMatches()) {
				if (matches.size() >= remaining) {
					truncated = true;
					break;
				}
				matches.add(new KnowledgeSearchMatch(null, match.getSection(), match.getSectionTitle(), match.getText(),
						null, match.getContext()));
			}

			if (!matches.isEmpty()) {
				results.add(new KnowledgeSearchResult(KnowledgeItemType.DOCUMENT, document.getTitle(),
						"documents/" + document.getTitle(), matches));
				remaining -= matches.size();
			}

			if (truncated) {
				break;
			}
		}

		return new KnowledgeGrepResult(results, truncated);
	}

	private String extractName(String path) {
		if (path == null || path.isBlank() || "/".equals(path)) {
			return path;
		}

		int separator = path.lastIndexOf('/');
		if (separator < 0) {
			return path;
		}

		return path.substring(separator + 1);
	}
}
