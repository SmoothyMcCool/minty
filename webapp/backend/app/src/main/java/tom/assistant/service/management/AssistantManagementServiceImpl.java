package tom.assistant.service.management;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import tom.api.services.assistant.AssistantManagementService;
import tom.assistant.repository.AssistantRepository;
import tom.conversation.service.ConversationServiceInternal;
import tom.document.service.AssistantDocumentLinkService;
import tom.model.Assistant;
import tom.ollama.service.OllamaService;

@Service
public class AssistantManagementServiceImpl implements AssistantManagementServiceInternal {

	private static final Logger logger = LogManager.getLogger(AssistantManagementServiceImpl.class);

	private final AssistantRepository assistantRepository;
	private final AssistantDocumentLinkService assistantDocumentLinkService;
	private ConversationServiceInternal conversationService;
	private final OllamaService ollamaService;

	public AssistantManagementServiceImpl(AssistantRepository assistantRepository,
			AssistantDocumentLinkService assistantDocumentLinkService, OllamaService ollamaService) {
		this.assistantRepository = assistantRepository;
		this.assistantDocumentLinkService = assistantDocumentLinkService;
		this.ollamaService = ollamaService;
	}

	@Override
	public Assistant createAssistant(int userId, Assistant assistant) {
		tom.assistant.repository.Assistant repoAsst = new tom.assistant.repository.Assistant(assistant);
		repoAsst.setOwnerId(userId);
		repoAsst.setId(null);

		return assistantRepository.save(repoAsst).toTaskAssistant();
	}

	@Override
	@Transactional
	public Assistant updateAssistant(int userId, Assistant assistant) {
		Optional<tom.assistant.repository.Assistant> maybeAssistant = assistantRepository.findById(assistant.id());

		if (maybeAssistant.isEmpty()) {
			logger.warn("No assistant with id " + assistant.id() + " exists.");
			return null;
		}

		tom.assistant.repository.Assistant repoAssistant = maybeAssistant.get();
		if (repoAssistant.getOwnerId() != userId) {
			logger.warn("Assistant Id " + assistant.id() + " not owned by user id " + userId);
			return null;
		}

		assistantDocumentLinkService.updateLinksForAssistant(assistant.id(), assistant.documentIds());

		repoAssistant = assistantRepository.save(repoAssistant.updateWith(assistant));

		List<String> docs = assistantDocumentLinkService.getDocumentIdsForAssistant(repoAssistant.getId());
		repoAssistant.setAssociatedDocuments(docs);

		return repoAssistant.toTaskAssistant();
	}

	@Override
	public List<Assistant> listAssistants(int userId) {
		List<tom.assistant.repository.Assistant> asstList = assistantRepository.findAllByOwnerIdOrSharedTrue(userId);

		if (asstList == null || asstList.size() == 0) {
			return new ArrayList<>();
		}

		asstList = asstList.stream().map(assistant -> {
			if (assistant.getOwnerId() == userId) {
				// Don't mark this as shared since it's owned by the current user.
				assistant.setShared(false);
			}
			List<String> documentIds = assistantDocumentLinkService.getDocumentIdsForAssistant(assistant.getId());
			assistant.setAssociatedDocuments(documentIds);
			return assistant;
		}).toList();

		return asstList.stream().map(asst -> asst.toTaskAssistant()).toList();
	}

	@Override
	@Transactional
	public Assistant findAssistant(int userId, int assistantId) {
		if (assistantId == AssistantManagementService.DefaultAssistantId) {
			return Assistant.CreateDefaultAssistant(ollamaService.getDefaultModel().toString());
		}

		try {
			tom.assistant.repository.Assistant assistant = assistantRepository.findById(assistantId).get();
			if (assistant.isShared() || assistant.getOwnerId() == userId) {
				List<String> docIds = assistantDocumentLinkService.getDocumentIdsForAssistant(assistant.getId());
				assistant.setAssociatedDocuments(docIds);
				return assistant.toTaskAssistant();
			}
		} catch (Exception e) {
			logger.warn("Could not find assistant: " + assistantId);
			return null;
		}

		return null;
	}

	@Override
	public Assistant unrestrictedFindAssistant(Integer assistantId) {
		tom.assistant.repository.Assistant assistant = assistantRepository.findById(assistantId).get();
		assistant.setAssociatedDocuments(assistantDocumentLinkService.getDocumentIdsForAssistant(assistantId));
		return assistant.toTaskAssistant();
	}

	@Override
	@Transactional
	public boolean deleteAssistant(int userId, int assistantId) {
		Assistant assistant = findAssistant(userId, assistantId);

		if (assistant == null) {
			logger.warn("Tried to delete an assistant that doesn't exist or user cannot access. User: " + userId
					+ ", assistant: " + assistantId);
			return false;
		}

		assistantDocumentLinkService.removeAllLinksToAssistant(assistantId);
		conversationService.deleteConversationsForAssistant(userId, assistantId);
		assistantRepository.deleteById(assistantId);

		return true;

	}

	@Override
	public String getModelForAssistant(int userId, int assistantId) {
		Assistant assistant = findAssistant(userId, assistantId);
		if (assistant == null) {
			logger.warn("Tried to access an assistant that does not exist or user has no permission. User " + userId
					+ ", assistant: " + assistantId);
			return "";
		}

		try {
			OllamaModel.valueOf(assistant.model()); // Just to make sure the value is valid.
			return assistant.model();
		} catch (Exception e) {
			logger.warn("Invalid model: " + assistant.model());
			return "";
		}
	}

	@Override
	public void setConversationService(ConversationServiceInternal conversationService) {
		this.conversationService = conversationService;
	}

}
