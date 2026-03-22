package tom.skill.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import tom.ApiError;
import tom.api.services.SkillService;
import tom.api.skill.Skill;
import tom.api.skill.SkillMetadata;
import tom.config.MintyConfiguration;
import tom.controller.ResponseWrapper;
import tom.model.security.UserDetailsUser;

@Controller
@RequestMapping("/api/skills")
public class SkillController {

	private final Logger logger = LogManager.getLogger(SkillController.class);

	private final SkillService skillsService;
	private final MintyConfiguration config;

	public SkillController(SkillService skillsService, MintyConfiguration config) {
		this.skillsService = skillsService;
		this.config = config;
	}

	@RequestMapping(value = { "/list" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<List<SkillMetadata>>> listSkills(
			@AuthenticationPrincipal UserDetailsUser user) {

		List<SkillMetadata> skills = skillsService.listSkills();
		if (skills == null) {
			ResponseWrapper<List<SkillMetadata>> response = ResponseWrapper.ApiFailureResponse(HttpStatus.OK.value(),
					List.of(ApiError.NOT_FOUND));
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		ResponseWrapper<List<SkillMetadata>> response = ResponseWrapper.SuccessResponse(skills);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<Skill>> getSkill(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("name") String name) {

		Skill skill = skillsService.getSkill(name);

		if (skill == null) {
			ResponseWrapper<Skill> response = ResponseWrapper.ApiFailureResponse(HttpStatus.OK.value(),
					List.of(ApiError.NOT_FOUND));
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		ResponseWrapper<Skill> response = ResponseWrapper.SuccessResponse(skill);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping(value = { "/upload" }, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ResponseWrapper<String>> addSkill(@AuthenticationPrincipal UserDetailsUser user,
			@RequestPart("file") MultipartFile mpf) throws IOException {

		Path tempFile = null;

		try {
			tempFile = Files.createTempFile(config.getConfig().fileStores().temp(), null, mpf.getOriginalFilename());
			mpf.transferTo(tempFile);
			if (skillsService.validate(tempFile)) {
				skillsService.addSkill(tempFile);
			} else {
				throw new Exception("Skill is not valid");
			}

		} catch (Exception e) {
			logger.error("Failed to store file: ", e);
			ResponseWrapper<String> response = ResponseWrapper.FailureResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
					e.getMessage());
			return new ResponseEntity<>(response, HttpStatus.OK);
		} finally {
			Files.delete(tempFile);
		}

		ResponseWrapper<String> response = ResponseWrapper.SuccessResponse("Skill added");
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
