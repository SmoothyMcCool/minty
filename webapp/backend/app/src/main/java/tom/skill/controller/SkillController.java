package tom.skill.controller;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import tom.ApiError;
import tom.api.model.user.ResourceSharingSelection;
import tom.api.model.user.UserSelection;
import tom.api.skill.Skill;
import tom.api.skill.SkillMetadata;
import tom.controller.ResponseWrapper;
import tom.model.security.UserDetailsUser;
import tom.skill.service.SkillServiceInternal;

@Controller
@RequestMapping("/api/skills")
public class SkillController {

	private final Logger logger = LogManager.getLogger(SkillController.class);

	private final SkillServiceInternal skillService;

	public SkillController(SkillServiceInternal skillsService) {
		this.skillService = skillsService;
	}

	@RequestMapping(value = { "/list" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<List<SkillMetadata>>> listSkills(
			@AuthenticationPrincipal UserDetailsUser user) {

		List<SkillMetadata> skills = skillService.listSkills(user.getId());
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

		Skill skill = skillService.getSkill(user.getId(), name);

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

		try {
			skillService.addSkill(user.getId(), mpf.getBytes());

		} catch (Exception e) {
			logger.error("Failed to add skill: ", e);
			ResponseWrapper<String> response = ResponseWrapper.FailureResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
					e.getMessage());
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		ResponseWrapper<String> response = ResponseWrapper.SuccessResponse("Skill added.");
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@DeleteMapping(value = { "/delete" })
	public ResponseEntity<ResponseWrapper<String>> removeSkill(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("name") String name) {

		try {
			skillService.removeSkill(user.getId(), name);

		} catch (Exception e) {
			logger.error("Failed to delete skill: ", e);
			ResponseWrapper<String> response = ResponseWrapper.FailureResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
					e.getMessage());
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		ResponseWrapper<String> response = ResponseWrapper.SuccessResponse("Skill removed.");
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping(value = { "/share" })
	public ResponseEntity<ResponseWrapper<String>> shareSkill(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody() ResourceSharingSelection selection) {
		try {
			skillService.shareSkill(user.getId(), selection);

		} catch (Exception e) {
			logger.error("Failed to share skill: ", e);
			ResponseWrapper<String> response = ResponseWrapper.FailureResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
					e.getMessage());
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		ResponseWrapper<String> response = ResponseWrapper.SuccessResponse("Skill shared.");
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping(value = { "/getsharing" })
	public ResponseEntity<ResponseWrapper<UserSelection>> getSharing(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("name") String name) {
		try {
			UserSelection selection = skillService.getSharingFor(user.getId(), name);
			ResponseWrapper<UserSelection> response = ResponseWrapper.SuccessResponse(selection);
			return new ResponseEntity<>(response, HttpStatus.OK);

		} catch (Exception e) {
			logger.error("Failed to list users for skill sharing: ", e);
			ResponseWrapper<UserSelection> response = ResponseWrapper
					.FailureResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
	}
}
