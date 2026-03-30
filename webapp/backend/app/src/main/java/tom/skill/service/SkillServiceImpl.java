package tom.skill.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import tom.api.UserId;
import tom.api.services.exception.NotFoundException;
import tom.api.services.exception.NotOwnedException;
import tom.api.skill.SkillFile;
import tom.api.skill.SkillMetadata;
import tom.skill.model.Skill;
import tom.skill.model.joins.UserSkillId;
import tom.skill.model.joins.UserSkillLink;
import tom.skill.repository.SkillRepository;
import tom.skill.repository.UserSkillLinkRepository;
import tom.user.model.ResourceSharingSelection;
import tom.user.model.UserSelection;
import tom.user.service.UserServiceInternal;

@Service
public class SkillServiceImpl implements SkillServiceInternal {

	private final Logger logger = LogManager.getLogger(SkillServiceImpl.class);

	private static final ObjectMapper YamlMapper = new ObjectMapper(new YAMLFactory());

	private final UserServiceInternal userService;
	private final SkillRepository skillRepository;
	private final UserSkillLinkRepository linkRepository;
	private List<tom.api.skill.Skill> skills;

	public SkillServiceImpl(SkillRepository skillRepository, UserSkillLinkRepository linkRepository,
			UserServiceInternal userService) {
		this.userService = userService;
		this.skillRepository = skillRepository;
		this.linkRepository = linkRepository;
		skills = new ArrayList<>();
	}

	@PostConstruct
	public void initialize() {
		logger.info("Searching for skills...");

		List<Skill> dbSkills = skillRepository.findAll();
		for (Skill skill : dbSkills) {
			try {
				skills.add(readAndValidate(skill.getFile()));
				logger.info("Found skill " + skill.getName());
			} catch (IOException e) {
				logger.error("Invalid skill found.", e);
			}
		}
	}

	@Override
	@Transactional
	public tom.api.skill.Skill addSkill(UserId userId, byte[] file) throws IOException, NotOwnedException {
		tom.api.skill.Skill sk = readAndValidate(file);

		Skill skill = new Skill();
		skill.setName(sk.name());
		skill.setFile(file);
		skill.setOwnerId(userId);

		Skill dbSkill = skillRepository.findByName(sk.name());

		if (dbSkill != null) {
			throw new NotOwnedException(sk.name());
		}

		skills.add(sk);
		skill = skillRepository.save(skill);
		UserSkillLink usl = new UserSkillLink(userId, skill);
		linkRepository.save(usl);
		return sk;
	}

	@Override
	@Transactional
	public void removeSkill(UserId userId, String name) throws IOException, NotFoundException, NotOwnedException {
		Skill dbSkill = skillRepository.findByName(name);

		if (dbSkill == null) {
			throw new NotFoundException(name);
		}

		if (!dbSkill.getOwnerId().equals(userId)) {
			throw new NotOwnedException(name);
		}

		skillRepository.delete(dbSkill);

		skills.removeIf(skill -> skill.name().equals(name));
	}

	@Override
	@Transactional
	public List<SkillMetadata> listSkills(UserId userId) {
		List<SkillMetadata> result = new ArrayList<>();

		List<UserSkillLink> links = linkRepository
				.findById_UserIdIn(List.of(userId.getValue(), ResourceSharingSelection.AllUsersId.getValue())).stream()
				.collect(Collectors.toMap(usl -> usl.getSkill().getId(), usl -> usl, (a, b) -> a, LinkedHashMap::new))
				.values().stream().toList();

		for (UserSkillLink usl : links) {

			Skill skill = usl.getSkill();
			try {
				SkillMetadata metadata = skills.stream().filter(sk -> sk.name().equals(skill.getName())).findFirst()
						.orElseThrow().metadata();
				result.add(
						new SkillMetadata(metadata.name(), metadata.description(), skill.getOwnerId().equals(userId)));
			} catch (NoSuchElementException e) {
				// Nothing to do, just doesn't exist.
			}
		}
		return result;
	}

	@Override
	@Transactional
	public String getFile(UserId userId, String skillName, String filename) {
		Skill dbSkill = checkAccessAndGet(userId, skillName);
		if (dbSkill == null) {
			return null;
		}

		return skills.stream().filter(skill -> skillName.equals(skill.metadata().name())).findFirst()
				.flatMap(skill -> skill.files().stream().filter(f -> f.relativePath().equals(filename)).findFirst()
						.map(SkillFile::content))
				.orElse(null);
	}

	@Override
	public tom.api.skill.Skill getSkill(UserId userId, String name) {
		Skill dbSkill = checkAccessAndGet(userId, name);
		if (dbSkill == null) {
			return null;
		}

		return skills.stream().filter(sk -> name.equals(sk.metadata().name())).findFirst().orElse(null);
	}

	@Override
	public void shareSkill(UserId userId, ResourceSharingSelection selection) throws NotFoundException {
		Skill dbSkill = checkAccessAndGet(userId, selection.getResource());
		if (dbSkill == null) {
			throw new NotFoundException(selection.getResource());
		}

		UserSkillLink usl = new UserSkillLink();
		usl.setSkill(dbSkill);

		// Remove any other sharing first.
		List<UserSkillLink> skills = linkRepository.findById_SkillId(dbSkill.getId());
		skills = skills.stream().filter(link -> !link.getUserId().equals(userId.getValue())).toList();
		skills.forEach(link -> {
			linkRepository.delete(link);
		});

		// Now share to those that should get it.
		if (selection.getUserSelection().isAllUsers()) {
			UserId sharingTargetUser = ResourceSharingSelection.AllUsersId;
			UserSkillId usi = new UserSkillId();
			usi.setSkillId(dbSkill.getId());
			usi.setUserId(sharingTargetUser.getValue());
			usl.setId(usi);
			linkRepository.save(usl);

		} else {

			for (String username : selection.getUserSelection().getSelectedUsers()) {
				try {
					UserId sharingTargetUser = userService.getUserFromName(username).orElseThrow().getId();
					UserSkillId usi = new UserSkillId();
					usi.setSkillId(dbSkill.getId());
					usi.setUserId(sharingTargetUser.getValue());
					usl.setId(usi);
					linkRepository.save(usl);
				} catch (Exception e) {
					// oh well. sharing failed to that user.
				}
			}
		}
	}

	@Override
	@Transactional
	public UserSelection getSharingFor(UserId userId, String name) throws NotOwnedException, NotFoundException {
		Skill skill = skillRepository.findByName(name);
		if (skill == null) {
			throw new NotFoundException(name);
		}

		if (!skill.getOwnerId().equals(userId)) {
			throw new NotOwnedException(name);
		}

		List<UserSkillLink> sharedWith = linkRepository.findById_SkillId(skill.getId());

		UserSelection selection = new UserSelection();
		selection.setAllUsers(false);
		selection.setSelectedUsers(new ArrayList<>());

		for (UserSkillLink share : sharedWith) {
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

	private Skill checkAccessAndGet(UserId userId, String skillName) {
		Skill skill = skillRepository.findByName(skillName);
		if (skill == null) {
			return null;
		}

		UserSkillId userSkillId = new UserSkillId(ResourceSharingSelection.AllUsersId.getValue(), skill.getId());
		if (linkRepository.findById(userSkillId).isPresent()) {
			return skill;
		}

		userSkillId = new UserSkillId(userId.getValue(), skill.getId());
		if (linkRepository.findById(userSkillId).isEmpty()) {
			return null;
		}

		return skill;
	}

	private tom.api.skill.Skill readAndValidate(byte[] skillFile) throws IOException {

		SkillMetadata metadata = null;
		List<SkillFile> files = new ArrayList<>();

		try (ByteArrayInputStream bis = new ByteArrayInputStream(skillFile);
				ZipInputStream zis = new ZipInputStream(bis)) {

			ZipEntry entry;

			while ((entry = zis.getNextEntry()) != null) {

				if (entry.isDirectory()) {
					continue;
				}

				try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
					byte[] buffer = new byte[1024];
					int len;
					while ((len = zis.read(buffer)) > 0) {
						baos.write(buffer, 0, len);
					}

					byte[] entryBytes = baos.toByteArray();
					List<String> contents = Arrays.asList(new String(entryBytes, StandardCharsets.UTF_8).split("\\R"));

					if (entry.getName().equalsIgnoreCase("SKILL.md")) {
						metadata = parseSkillsMetadata(contents);
					}
					files.add(new SkillFile(entry.getName(), String.join("\n", contents)));
				}

				zis.closeEntry();
			}

		}

		if (metadata == null) {
			throw new IOException("No metadata found.");
		}

		final String name = metadata.name();

		if (metadata.name() == null || metadata.name().isBlank() || metadata.name().length() > 64) {
			throw new IOException(
					"SKILL.md does not contain a valid skill name. Must be present, less than 64 characters.");
		}
		if (!metadata.name().matches("[a-z0-9-]+")) {
			throw new IOException("Skill name can only contain lowercase letters, numbers, and hyphens.");
		}
		if (metadata.description() == null || metadata.description().isBlank()
				|| metadata.description().length() > 1024) {
			throw new IOException(
					"SKILL.md does not contain a valid skill description. Must be present, less than 1024 characters.");
		}
		if (skills.stream().anyMatch(skill -> skill.name().equalsIgnoreCase(name))) {
			throw new IOException("A skill with this name already exists.");
		}

		return new tom.api.skill.Skill(name, metadata, files);
	}

	private SkillMetadata parseSkillsMetadata(List<String> lines) {
		// Extract only the front-matter block
		if (lines.isEmpty() || !lines.get(0).trim().equals("---")) {
			return new SkillMetadata(null, null, true);
		}

		String frontmatter = lines.stream().skip(1).takeWhile(line -> !line.trim().equals("---"))
				.collect(Collectors.joining("\n"));

		try {
			JsonNode root = YamlMapper.readTree(frontmatter);
			String name = textOrNull(root, "name");
			String description = textOrNull(root, "description");
			return new SkillMetadata(name, description, true);
		} catch (JsonProcessingException e) {
			return new SkillMetadata(null, null, true);
		}
	}

	private String textOrNull(JsonNode node, String field) {
		JsonNode child = node.get(field);
		return child != null && !child.isNull() ? child.asText() : null;
	}

}
