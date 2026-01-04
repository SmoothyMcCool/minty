package tom.assistant.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

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
import tom.api.services.assistant.LlmMetric;
import tom.api.services.assistant.LlmResult;
import tom.api.services.assistant.QueueFullException;
import tom.api.services.assistant.StreamResult;
import tom.config.model.ChatModelConfig;
import tom.controller.ResponseWrapper;
import tom.conversation.service.ConversationServiceInternal;
import tom.meta.service.MetadataService;
import tom.model.security.UserDetailsUser;
import tom.ollama.service.OllamaService;

@Controller
@RequestMapping("/api/assistant")
public class AssistantController {

	private final MetadataService metadataService;
	private final AssistantManagementService assistantManagementService;
	private final OllamaService ollamaService;
	private final ConversationServiceInternal conversationService;
	private final AssistantQueryService assistantQueryService;
	private final ObjectMapper mapper;

	public AssistantController(AssistantManagementService assistantManagementService,
			AssistantQueryService assistantQueryService, MetadataService metadataService, OllamaService ollamaService,
			ConversationServiceInternal conversationService) {
		this.assistantManagementService = assistantManagementService;
		this.metadataService = metadataService;
		this.ollamaService = ollamaService;
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
		List<ChatModelConfig> models = ollamaService.listModels();
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

	@PostMapping(value = { "/response" }, produces = MediaType.APPLICATION_NDJSON_VALUE)
	public Callable<StreamingResponseBody> ask(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody ConversationId streamId) {

		return () -> {
			LlmResult llmResult = assistantQueryService.getResultFor(streamId);
			StreamResult streamResult = (StreamResult) llmResult;

			if (streamResult == null || streamResult == LlmResult.THINKING_STREAM_NOT_READY) {
				int queuePosition = assistantQueryService.getQueuePositionFor(streamId);
				return outputStream -> {
					if (queuePosition == -1) {
						return; // Nothing is running, so end the stream.
					} else {
						LlmStatus status = new LlmStatus(RequestProcessingState.NOT_READY, queuePosition);
						writeResponse(outputStream, new StreamingResponse(status, null, null, null));
					}
					outputStream.flush();
				};
			}

			AtomicBoolean readyToStop = new AtomicBoolean(false);

			return outputStream -> {

				while (!readyToStop.get()) {

					try {

						String chunk = streamResult.takeChunk();

						if ((chunk == null || chunk.isEmpty()) && !streamResult.isComplete()) {
							continue;
						}

						LlmStatus status = new LlmStatus(RequestProcessingState.RUNNING, 0);
						LlmMetric metric = null;
						List<String> sources = null;

						if (streamResult.isComplete()) {
							status = new LlmStatus(RequestProcessingState.COMPLETE, 0);
							metric = streamResult.getUsage();
							sources = streamResult.getSources();
							readyToStop.set(true);
						}

						writeResponse(outputStream, new StreamingResponse(status, metric, sources, chunk));

					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						throw new RuntimeException("Streaming thread got interrupted. Aborting.");
					}
				}

			};
		};
	}

	private void writeResponse(OutputStream outputStream, StreamingResponse response) throws IOException {
		byte[] bytes = (mapper.writeValueAsString(response) + "\n").getBytes(StandardCharsets.UTF_8);
		outputStream.write(bytes);
		outputStream.flush();
	}
}
