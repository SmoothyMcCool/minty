package tom.assistant.service.management;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import tom.api.AssistantId;
import tom.api.DocumentId;
import tom.api.UserId;
import tom.api.model.assistant.Assistant;
import tom.api.model.user.ResourceSharingSelection;
import tom.api.model.user.UserSelection;
import tom.api.services.assistant.AssistantManagementService;
import tom.api.services.exception.NotFoundException;
import tom.api.services.exception.NotOwnedException;
import tom.assistant.model.joins.UserAssistantId;
import tom.assistant.model.joins.UserAssistantLink;
import tom.assistant.repository.AssistantRepository;
import tom.assistant.repository.UserAssistantLinkRepository;
import tom.config.MintyConfiguration;
import tom.conversation.service.ConversationServiceInternal;
import tom.document.service.AssistantDocumentLinkService;
import tom.llm.service.LlmService;
import tom.user.service.UserServiceInternal;

@Service
public class AssistantManagementServiceImpl implements AssistantManagementServiceInternal {

	private static final Logger logger = LogManager.getLogger(AssistantManagementServiceImpl.class);

	private final AssistantRepository assistantRepository;
	private final UserAssistantLinkRepository linkRepository;
	private final AssistantDocumentLinkService assistantDocumentLinkService;
	private final AssistantRegistry assistantRegistry;
	private final UserServiceInternal userService;
	private final LlmService llmService;
	private ConversationServiceInternal conversationService;

	public AssistantManagementServiceImpl(AssistantRepository assistantRepository,
			UserAssistantLinkRepository linkRepository, AssistantDocumentLinkService assistantDocumentLinkService,
			AssistantRegistry assistantRegistry, UserServiceInternal userService, LlmService llmService,
			MintyConfiguration properties) {
		this.assistantRepository = assistantRepository;
		this.linkRepository = linkRepository;
		this.assistantDocumentLinkService = assistantDocumentLinkService;
		this.assistantRegistry = assistantRegistry;
		this.userService = userService;
		this.llmService = llmService;
	}

	@Override
	@Transactional
	public Assistant createAssistant(UserId userId, Assistant assistant) throws NotOwnedException {
		if (assistantRepository.findByName(assistant.name()).isPresent()) {
			throw new NotOwnedException(assistant.name());
		}

		tom.assistant.model.Assistant repoAsst = new tom.assistant.model.Assistant(assistant);
		repoAsst.setOwnerId(userId);
		repoAsst.setId(null);
		repoAsst = assistantRepository.save(repoAsst);

		UserAssistantLink ual = new UserAssistantLink();
		UserAssistantId uai = new UserAssistantId();
		uai.setUserId(userId.getValue());
		uai.setAssistantId(repoAsst.getId().getValue());
		ual.setId(uai);
		ual.setAssistant(repoAsst);
		linkRepository.save(ual);

		return repoAsst.toTaskAssistant(userId);
	}

	@Override
	@Transactional
	public Assistant updateAssistant(UserId userId, Assistant assistant) {
		Optional<tom.assistant.model.Assistant> maybeAssistant = assistantRepository.findById(assistant.id().value());

		if (maybeAssistant.isEmpty()) {
			logger.warn("No assistant with id " + assistant.id() + " exists.");
			return null;
		}

		tom.assistant.model.Assistant repoAssistant = maybeAssistant.get();
		if (!repoAssistant.getOwnerId().equals(userId)) {
			logger.warn("Assistant Id " + assistant.id() + " not owned by user id " + userId);
			return null;
		}

		assistantDocumentLinkService.updateLinksForAssistant(assistant.id(), assistant.documentIds());

		repoAssistant = assistantRepository.save(repoAssistant.updateWith(assistant));

		List<DocumentId> docs = assistantDocumentLinkService.getDocumentIdsForAssistant(repoAssistant.getId());
		repoAssistant.setAssociatedDocuments(docs);

		return repoAssistant.toTaskAssistant(userId);
	}

	@Override
	@Transactional
	public List<Assistant> listAssistants(UserId userId) {
		List<tom.assistant.model.Assistant> asstList = linkRepository
				.findById_UserIdIn(List.of(userId.getValue(), ResourceSharingSelection.AllUsersId.getValue())).stream()
				.map(link -> link.getAssistant()).collect(Collectors.toMap(tom.assistant.model.Assistant::getId,
						assistant -> assistant, (a, b) -> a, LinkedHashMap::new))
				.values().stream().toList();

		List<Assistant> result = new ArrayList<>();
		if (asstList != null && asstList.size() > 0) {
			asstList = asstList.stream().map(assistant -> {
				List<DocumentId> documentIds = assistantDocumentLinkService
						.getDocumentIdsForAssistant(assistant.getId());
				assistant.setAssociatedDocuments(documentIds);
				return assistant;
			}).toList();
			result = asstList.stream().map(asst -> asst.toTaskAssistant(userId))
					.collect(Collectors.toCollection(ArrayList::new));
		}

		result.add(findAssistant(null, AssistantManagementService.AgenticAssistantId));
		return result;
	}

	@Override
	@Transactional
	public Assistant findAssistant(UserId userId, AssistantId assistantId) {
		if (assistantId.equals(AssistantManagementService.DefaultAssistantId)) {
			Assistant assistant = assistantRegistry.getAssistant("default");
			return assistant.toBuilder().id(AssistantManagementService.DefaultAssistantId).build();
		} else if (assistantId.equals(AssistantManagementService.ConversationNamingAssistantId)) {
			Assistant assistant = assistantRegistry.getAssistant("conversation_namer");
			return assistant.toBuilder().id(AssistantManagementService.ConversationNamingAssistantId).build();
		} else if (assistantId.equals(AssistantManagementService.DocumentSummarizingAssistantId)) {
			Assistant assistant = assistantRegistry.getAssistant("document_summarizer");
			return assistant.toBuilder().id(AssistantManagementService.DocumentSummarizingAssistantId).build();
		} else if (assistantId.equals(AssistantManagementService.AgenticAssistantId)) {
			Assistant assistant = assistantRegistry.getAssistant("AgentChat");
			return assistant.toBuilder().id(AssistantManagementService.AgenticAssistantId).build();
		}

		try {
			tom.assistant.model.Assistant assistant = linkRepository
					.findFirstById_AssistantIdAndId_UserIdIn(assistantId.getValue(),
							List.of(userId.getValue(), ResourceSharingSelection.AllUsersId.getValue()))
					.orElseThrow().getAssistant();

			List<DocumentId> docIds = assistantDocumentLinkService.getDocumentIdsForAssistant(assistant.getId());
			assistant.setAssociatedDocuments(docIds);
			return assistant.toTaskAssistant(userId);

		} catch (Exception e) {
			logger.warn("Could not find assistant: " + assistantId);
			return null;
		}

	}

	@Override
	@Transactional
	public void shareAssistant(UserId userId, ResourceSharingSelection selection)
			throws NotFoundException, NotOwnedException {
		Optional<tom.assistant.model.Assistant> maybeAssistant = assistantRepository
				.findById(UUID.fromString(selection.getResource()));

		if (maybeAssistant.isEmpty()) {
			throw new NotFoundException(selection.getResource());
		}

		tom.assistant.model.Assistant assistant = maybeAssistant.get();

		if (!assistant.getOwnerId().equals(userId)) {
			throw new NotOwnedException(selection.getResource());
		}

		UserAssistantLink ual = new UserAssistantLink();
		ual.setAssistant(assistant);

		// Remove any other sharing first.
		List<UserAssistantLink> assistants = linkRepository
				.findById_AssistantId(UUID.fromString(selection.getResource()));
		assistants = assistants.stream().filter(link -> !link.getUserId().equals(userId.getValue())).toList();
		assistants.forEach(link -> {
			linkRepository.delete(link);
		});

		// Now share to those that should get it.
		if (selection.getUserSelection().isAllUsers()) {
			UserId sharingTargetUser = ResourceSharingSelection.AllUsersId;
			UserAssistantId uai = new UserAssistantId();
			uai.setAssistantId(assistant.getId().getValue());
			uai.setUserId(sharingTargetUser.getValue());
			ual.setId(uai);
			linkRepository.save(ual);

		} else {
			for (String username : selection.getUserSelection().getSelectedUsers()) {
				try {
					UserId sharingTargetUser = userService.getUserFromName(username).orElseThrow().getId();
					UserAssistantId uai = new UserAssistantId();
					uai.setAssistantId(assistant.getId().getValue());
					uai.setUserId(sharingTargetUser.getValue());
					ual.setId(uai);
					linkRepository.save(ual);
				} catch (Exception e) {
					// oh well. sharing failed to that user.
				}
			}
		}
	}

	@Override
	@Transactional
	public UserSelection getSharingFor(UserId userId, AssistantId assistantId)
			throws NotOwnedException, NotFoundException {
		Optional<tom.assistant.model.Assistant> maybeAssistant = assistantRepository.findById(assistantId.getValue());
		if (maybeAssistant.isEmpty()) {
			throw new NotFoundException(assistantId.toString());
		}

		tom.assistant.model.Assistant assistant = maybeAssistant.get();

		if (!assistant.getOwnerId().equals(userId)) {
			throw new NotOwnedException(assistant.getName());
		}

		List<UserAssistantLink> sharedWith = linkRepository.findById_AssistantId(assistant.getId().getValue());

		UserSelection selection = new UserSelection();
		selection.setAllUsers(false);
		selection.setSelectedUsers(new ArrayList<>());

		for (UserAssistantLink share : sharedWith) {
			try {
				if (share.getUserId().equals(ResourceSharingSelection.AllUsersId.getValue())) {
					selection.setAllUsers(true);
					selection.setSelectedUsers(null);
					return selection;
				}
				selection.getSelectedUsers()
						.add(userService.getUserFromId(new UserId(share.getUserId())).orElseThrow().getName());
			} catch (Exception e) {
				// Just ignore. Bad name in given list.
			}
		}

		return selection;
	}

	@Override
	@Transactional
	public boolean deleteAssistant(UserId userId, AssistantId assistantId) {
		UserAssistantLink ual = linkRepository
				.findById_AssistantIdAndId_UserId(assistantId.getValue(), userId.getValue()).orElseThrow();

		tom.assistant.model.Assistant assistant = ual.getAssistant();

		if (assistant == null || !assistant.getOwnerId().equals(userId)) {
			logger.warn("Tried to delete an assistant that doesn't exist or user doesn't own. User: " + userId
					+ ", assistant: " + assistantId);
			return false;
		}

		assistantDocumentLinkService.removeAllLinksToAssistant(assistantId);
		conversationService.deleteConversationsForAssistant(userId, assistantId);
		assistantRepository.deleteById(assistantId.value());

		return true;

	}

	@Override
	public String getModelForAssistant(UserId userId, AssistantId assistantId) {
		Assistant assistant = findAssistant(userId, assistantId);
		if (assistant == null) {
			logger.warn("Tried to access an assistant that does not exist or user has no permission. User " + userId
					+ ", assistant: " + assistantId);
			return "";
		}

		try {
			llmService.isModelValid(assistant.model()); // Just to make sure the value is valid.
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

	@Override
	public boolean isAssistantConversational(AssistantId assistantId) {
		tom.assistant.model.Assistant assistant = assistantRepository.findById(assistantId.value()).get();
		return assistant.isHasMemory();
	}
}
