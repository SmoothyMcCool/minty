package tom.assistant.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;

import tom.ApiError;
import tom.api.AssistantId;
import tom.api.ConversationId;
import tom.api.model.assistant.Assistant;
import tom.api.model.assistant.AssistantQuery;
import tom.api.model.assistant.AssistantSpec;
import tom.api.services.assistant.AssistantManagementService;
import tom.api.services.assistant.AssistantQueryService;
import tom.api.services.assistant.LlmResult;
import tom.api.services.assistant.LlmResultState;
import tom.api.services.assistant.QueueFullException;
import tom.api.services.assistant.StreamResult;
import tom.config.model.ChatModelConfig;
import tom.controller.ResponseWrapper;
import tom.conversation.service.ConversationServiceInternal;
import tom.llm.service.LlmService;
import tom.meta.service.MetadataService;
import tom.model.security.UserDetailsUser;

@Controller
@RequestMapping("/api/assistant")
public class AssistantController {

	private static final Logger logger = LogManager.getLogger(AssistantController.class);

	private final MetadataService metadataService;
	private final AssistantManagementService assistantManagementService;
	private final LlmService llmService;
	private final ConversationServiceInternal conversationService;
	private final AssistantQueryService assistantQueryService;
	private final ObjectMapper mapper;

	public AssistantController(AssistantManagementService assistantManagementService,
			AssistantQueryService assistantQueryService, MetadataService metadataService, LlmService llmService,
			ConversationServiceInternal conversationService) {
		this.assistantManagementService = assistantManagementService;
		this.metadataService = metadataService;
		this.llmService = llmService;
		this.conversationService = conversationService;
		this.assistantQueryService = assistantQueryService;
		this.mapper = new ObjectMapper();
	}

	@PostMapping({ "/new" })
	public ResponseEntity<ResponseWrapper<Assistant>> addAssistant(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody Assistant assistant) {

		Assistant newAssistant = assistantManagementService.createAssistant(user.getId(), assistant);
		metadataService.newAssistant(user.getId());

		ResponseWrapper<Assistant> response = ResponseWrapper.SuccessResponse(newAssistant);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping({ "/edit" })
	public ResponseEntity<ResponseWrapper<Assistant>> editAssistant(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody Assistant assistant) {

		Assistant updatedAssistant = assistantManagementService.updateAssistant(user.getId(), assistant);

		ResponseWrapper<Assistant> response = ResponseWrapper.SuccessResponse(updatedAssistant);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping({ "/list" })
	public ResponseEntity<ResponseWrapper<List<Assistant>>> listAssistants(
			@AuthenticationPrincipal UserDetailsUser user) {
		List<Assistant> assistants = assistantManagementService.listAssistants(user.getId());
		ResponseWrapper<List<Assistant>> response = ResponseWrapper.SuccessResponse(assistants);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping({ "/models" })
	public ResponseEntity<ResponseWrapper<List<ChatModelConfig>>> listModels(
			@AuthenticationPrincipal UserDetailsUser user) {
		List<ChatModelConfig> models = llmService.listModels();
		ResponseWrapper<List<ChatModelConfig>> response = ResponseWrapper.SuccessResponse(models);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping({ "/get" })
	public ResponseEntity<ResponseWrapper<Assistant>> getAssistant(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("id") AssistantId assistantId) {
		Assistant assistant = assistantManagementService.findAssistant(user.getId(), assistantId);

		if (assistant == null) {
			ResponseWrapper<Assistant> response = ResponseWrapper.ApiFailureResponse(HttpStatus.FORBIDDEN.value(),
					List.of(ApiError.NOT_OWNED));
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		ResponseWrapper<Assistant> response = ResponseWrapper.SuccessResponse(assistant);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping({ "/conversation" })
	public ResponseEntity<ResponseWrapper<Assistant>> getAssistantForChat(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("conversationId") ConversationId conversationId) {

		if (conversationService.conversationOwnedBy(user.getId(), conversationId)) {
			AssistantId assistantId = conversationService.getAssistantIdFromConversationId(user.getId(),
					conversationId);
			return getAssistant(user, assistantId);
		}

		ResponseWrapper<Assistant> response = ResponseWrapper.ApiFailureResponse(HttpStatus.FORBIDDEN.value(),
				List.of(ApiError.NOT_OWNED));
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@DeleteMapping({ "/delete" })
	public ResponseEntity<ResponseWrapper<Boolean>> deleteAssistant(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("id") AssistantId assistantId) {

		boolean success = assistantManagementService.deleteAssistant(user.getId(), assistantId);

		if (!success) {
			ResponseWrapper<Boolean> response = ResponseWrapper.ApiFailureResponse(HttpStatus.BAD_REQUEST.value(),
					List.of(ApiError.REQUEST_FAILED));
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		ResponseWrapper<Boolean> response = ResponseWrapper.SuccessResponse(true);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping(value = "/ask", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ResponseWrapper<ConversationId>> ask(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("conversationId") String conversationId,
			@RequestPart("assistant") AssistantSpec assistantSpec, @RequestParam("query") String query,
			@RequestParam("contextSize") int contextSize,
			@RequestParam(value = "image", required = false) MultipartFile imageFile) throws IOException {

		Assistant assistant;
		if (assistantSpec.useId()) {
			assistant = assistantManagementService.findAssistant(user.getId(), assistantSpec.getAssistantId());

			if (assistant == null) {
				ResponseWrapper<ConversationId> response = ResponseWrapper
						.ApiFailureResponse(HttpStatus.FORBIDDEN.value(), List.of(ApiError.NOT_OWNED));
				return new ResponseEntity<>(response, HttpStatus.OK);
			}

			if (!assistant.ownerId().equals(user.getId()) && !assistant.shared()) {
				ResponseWrapper<ConversationId> response = ResponseWrapper
						.ApiFailureResponse(HttpStatus.FORBIDDEN.value(), List.of(ApiError.NOT_OWNED));
				return new ResponseEntity<>(response, HttpStatus.OK);
			}

		} else {
			assistant = assistantSpec.getAssistant();
		}

		ConversationId cId = new ConversationId(conversationId);
		if (conversationService.conversationOwnedBy(user.getId(), cId)) {
			ConversationId requestUuid;
			try {

				AssistantQuery aq = new AssistantQuery();
				aq.setAssistantSpec(assistantSpec);
				aq.setConversationId(cId);
				aq.setQuery(query);
				aq.setContextSize(contextSize);

				if (imageFile != null) {
					byte[] bytes = imageFile.getBytes();
					aq.setImage(new ByteArrayResource(bytes), imageFile.getContentType());
				} else {
					aq.setImage(null, null);
				}

				requestUuid = assistantQueryService.askStreaming(user.getId(), aq);
				conversationService.updateLastUsed(cId);

			} catch (QueueFullException e) {
				ResponseWrapper<ConversationId> response = ResponseWrapper
						.ApiFailureResponse(HttpStatus.SERVICE_UNAVAILABLE.value(), List.of(ApiError.LLM_BUSY));
				return new ResponseEntity<>(response, HttpStatus.OK);
			}

			ResponseWrapper<ConversationId> response = ResponseWrapper.SuccessResponse(requestUuid);
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		ResponseWrapper<ConversationId> response = ResponseWrapper.ApiFailureResponse(HttpStatus.FORBIDDEN.value(),
				List.of(ApiError.NOT_OWNED));
		return new ResponseEntity<>(response, HttpStatus.OK);

	}

	@PostMapping(value = "/response", produces = MediaType.APPLICATION_NDJSON_VALUE)
	public Callable<ResponseEntity<StreamingResponseBody>> ask(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody ConversationId streamId) {

		return () -> {
			if (assistantQueryService.getQueuePositionFor(streamId) == -1) {
				return ResponseEntity.noContent().build();
			}

			AtomicReference<StreamResult> streamResult = new AtomicReference<>();

			StreamingResponseBody body = outputStream -> {

				while (streamResult.get() == null || streamResult.get().getState() == LlmResultState.QUEUED) {
					// Check for queued / no-result state
					LlmResult result = assistantQueryService.getResultAndRemoveIfComplete(streamId);
					if (result instanceof StreamResult sr) {
						streamResult.set(sr);
						if (sr.getState() != LlmResultState.QUEUED) {
							break;
						}
					}

					int queuePosition = assistantQueryService.getQueuePositionFor(streamId);
					if (queuePosition == -1) {
						return;
					}

					RequestProcessingState state = queuePosition == 0 ? RequestProcessingState.RUNNING
							: RequestProcessingState.NOT_READY;

					writeResponse(outputStream,
							new StreamingResponse(new LlmStatus(state, queuePosition), null, null, null));
					outputStream.flush();

					try {
						Thread.sleep(250);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						logger.warn("Streaming thread interrupted while streaming chunks", e);
						return;
					}
				}

				// Stream actual result
				try {
					while (true) {
						String chunk;
						try {
							chunk = streamResult.get().takeChunk();
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							logger.warn("Streaming thread interrupted, closing response", e);
							return;
						}

						if (chunk == null) {
							writeResponse(outputStream,
									new StreamingResponse(new LlmStatus(RequestProcessingState.COMPLETE, 0),
											streamResult.get().getUsage(), streamResult.get().getSources(), ""));
							outputStream.flush();
							assistantQueryService.getResultAndRemoveIfComplete(streamId);
							break;
						}

						writeResponse(outputStream, new StreamingResponse(
								new LlmStatus(RequestProcessingState.RUNNING, 0), null, null, chunk));
						outputStream.flush();
					}
				} catch (Exception e) {
					logger.error("Error streaming response for " + streamId, e);
				}
			};

			return ResponseEntity.ok().contentType(MediaType.APPLICATION_NDJSON).body(body);
		};
	}

	private void writeResponse(OutputStream outputStream, StreamingResponse response) throws IOException {
		byte[] bytes = (mapper.writeValueAsString(response) + "\n").getBytes(StandardCharsets.UTF_8);
		outputStream.write(bytes);
		outputStream.flush();
	}
}
