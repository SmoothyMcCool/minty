package tom.meta.controller;

import java.util.List;
import java.util.stream.StreamSupport;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import tom.controller.ResponseWrapper;
import tom.meta.repository.MetadataRepository;
import tom.meta.repository.UserMeta;

@Controller
@RequestMapping("/api/metadata")
public class MetadataController {

    private final MetadataRepository metadataRepository;

	public MetadataController(MetadataRepository metadataRepository) {
		this.metadataRepository = metadataRepository;
	}

	@RequestMapping(value = { "/all" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<List<UserMeta>>> getAllStats() {

		List<UserMeta> metadata = StreamSupport.stream(metadataRepository.findAll().spliterator(), false).toList();

		ResponseWrapper<List<UserMeta>> response = ResponseWrapper.SuccessResponse(metadata);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
