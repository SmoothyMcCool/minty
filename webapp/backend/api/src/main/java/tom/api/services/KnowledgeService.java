package tom.api.services;

import java.util.List;

import tom.api.ProjectId;
import tom.api.UserId;
import tom.api.model.project.KnowledgeGrepResult;
import tom.api.model.project.KnowledgeItemInfo;

public interface KnowledgeService {

	List<KnowledgeItemInfo> find(UserId userId, ProjectId projectId, String query, int maxResults);

	KnowledgeGrepResult grep(UserId userId, ProjectId projectId, String path, String pattern, boolean caseSensitive,
			int maxResults, int contextBefore, int contextAfter);
}