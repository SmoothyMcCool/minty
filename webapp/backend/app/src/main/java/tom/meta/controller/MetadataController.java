package tom.meta.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import tom.controller.ResponseWrapper;
import tom.meta.repository.MetadataRepository;
import tom.meta.repository.UserMeta;
import tom.user.model.User;
import tom.user.service.UserServiceInternal;

@Controller
@RequestMapping("/api/metadata")
public class MetadataController {

	private final MetadataRepository metadataRepository;
	private final UserServiceInternal userService;

	public MetadataController(MetadataRepository metadataRepository, UserServiceInternal userService) {
		this.metadataRepository = metadataRepository;
		this.userService = userService;
	}

	@GetMapping({ "/all" })
	public ResponseEntity<ResponseWrapper<List<Metadata>>> getAllStats() {

		List<UserMeta> metadata = StreamSupport.stream(metadataRepository.findAll().spliterator(), false).toList();
		List<Metadata> ret = new ArrayList<>();
		metadata.forEach(md -> {
			Optional<User> user = userService.getUserFromId(md.getUserId());
			if (user.isPresent()) {
				ret.add(new Metadata(user.get().getName(), md.getTotalAssistantsCreated(), md.getTotalConversations(),
						md.getTotalWorkflowsCreated(), md.getTotalWorkflowRuns(), md.getTotalLogins(),
						md.getLastLogin()));
			}
		});

		ResponseWrapper<List<Metadata>> response = ResponseWrapper.SuccessResponse(ret);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
