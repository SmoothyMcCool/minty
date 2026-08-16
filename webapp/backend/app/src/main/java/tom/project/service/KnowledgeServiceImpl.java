package tom.project.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import tom.api.ProjectId;
import tom.api.UserId;
import tom.api.model.document.Document;
import tom.api.model.document.DocumentSearchMatch;
import tom.api.model.document.DocumentSearchResult;
import tom.api.model.project.FileSearchMatch;
import tom.api.model.project.FileSearchResult;
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
	public List<KnowledgeSearchResult> grep(UserId userId, ProjectId projectId, String path, String pattern,
			boolean caseSensitive, int maxResults, int contextBefore, int contextAfter) {

		if (pattern == null || pattern.isBlank()) {
			return List.of();
		}

		if (maxResults < 1) {
			return List.of();
		}

		if (contextBefore < 0 || contextAfter < 0) {
			return List.of();
		}

		List<KnowledgeSearchResult> results = new ArrayList<>();

		int remaining = maxResults;

		// Search project files.
		if (remaining > 0) {

			List<FileSearchResult> fileResults = projectService.grep(userId, projectId, path, pattern, caseSensitive,
					remaining, contextBefore, contextAfter);

			for (FileSearchResult file : fileResults) {

				if (remaining <= 0) {
					break;
				}

				List<KnowledgeSearchMatch> matches = new ArrayList<>();

				for (FileSearchMatch match : file.getMatches()) {

					if (remaining <= 0) {
						break;
					}

					matches.add(new KnowledgeSearchMatch(match.getLine(), null, null, match.getText(),
							match.getContext(), null));

					remaining--;
				}

				if (!matches.isEmpty()) {

					String filePath = file.getPath();

					results.add(new KnowledgeSearchResult(KnowledgeItemType.FILE, extractName(filePath), filePath,
							matches));
				}
			}
		}

		// Search knowledge-base documents with whatever portion of the result limit
		// remains.
		if (remaining > 0) {

			List<DocumentSearchResult> documentResults = documentService.grep(userId, projectId, pattern, caseSensitive,
					remaining, contextBefore, contextAfter);

			for (DocumentSearchResult document : documentResults) {

				if (remaining <= 0) {
					break;
				}

				List<KnowledgeSearchMatch> matches = new ArrayList<>();

				for (DocumentSearchMatch match : document.getMatches()) {

					if (remaining <= 0) {
						break;
					}

					matches.add(new KnowledgeSearchMatch(null, match.getSection(), match.getSectionTitle(),
							match.getText(), null, match.getContext()));

					remaining--;
				}

				if (!matches.isEmpty()) {

					results.add(new KnowledgeSearchResult(KnowledgeItemType.DOCUMENT, document.getTitle(),
							"documents/" + document.getTitle(), matches));
				}
			}
		}

		return results;
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
