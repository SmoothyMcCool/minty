package tom.skill.service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import jakarta.annotation.PostConstruct;
import tom.api.services.SkillService;
import tom.api.skill.Skill;
import tom.api.skill.SkillFile;
import tom.api.skill.SkillMetadata;
import tom.config.MintyConfiguration;

@Service
public class SkillServiceImpl implements SkillService {

	private final Logger logger = LogManager.getLogger(SkillServiceImpl.class);

	private static final ObjectMapper YamlMapper = new ObjectMapper(new YAMLFactory());

	private final Path skillPath;
	private List<Skill> skills;
	private final MintyConfiguration config;

	public SkillServiceImpl(MintyConfiguration config) {
		skillPath = config.getConfig().fileStores().skills();
		this.config = config;
	}

	@PostConstruct
	public void initialize() {
		logger.info("Searching for skills...");
		try {
			findSkillsFiles(skillPath);
		} catch (IOException e) {
			logger.error("Failed to load skills: ", e);
		}
	}

	@Override
	public List<SkillMetadata> listSkills() {
		return skills.stream().map(skill -> skill.metadata()).toList();
	}

	@Override
	public String getFile(String skillName, String filename) {
		return skills.stream().filter(skill -> skillName.equals(skill.metadata().name())).findFirst()
				.flatMap(skill -> skill.files().stream().filter(f -> f.relativePath().equals(filename)).findFirst()
						.map(SkillFile::content))
				.orElse(null);
	}

	@Override
	public Skill getSkill(String name) {
		return skills.stream().filter(skill -> name.equals(skill.metadata().name())).findFirst().orElse(null);
	}

	private void findSkillsFiles(Path rootPath) throws IOException {
		skills = new ArrayList<>();

		List<Path> allPaths = new ArrayList<>();
		Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
				allPaths.add(file);
				return FileVisitResult.CONTINUE;
			}
		});

		for (Path file : allPaths) {
			String fileName = file.getFileName().toString();

			if (fileName.equals("SKILL.md")) {
				Path skillFolder = file.getParent();
				List<SkillFile> skillFiles = new ArrayList<>();
				for (Path p : allPaths.stream().filter(p -> p.startsWith(skillFolder)).sorted().toList()) {
					String relative = skillFolder.relativize(p).toString().replace('\\', '/');
					skillFiles.add(new SkillFile(relative, Files.readString(p)));
				}
				SkillMetadata metadata = parseSkillsMetadata(Files.readAllLines(file));
				logger.info("Found skill: " + metadata.name());
				skills.add(new Skill(metadata.name(), metadata, skillFiles));

			} else if (fileName.endsWith(".skill")) {
				addSkillFromZip(file);
			}
		}
	}

	@Override
	public boolean validate(Path skillFile) throws IOException {
		URI zipUri = URI.create("jar:" + skillFile.toUri());
		try (FileSystem zipFs = FileSystems.newFileSystem(zipUri, Map.of())) {
			Path zipRoot = zipFs.getRootDirectories().iterator().next();
			Path skillMd = Files.walk(zipRoot)
					.filter(p -> p.getFileName() != null && p.getFileName().toString().equals("SKILL.md")).findFirst()
					.orElseThrow(() -> new IOException("No SKILL.md found in .skill file."));

			Path skillFolder = skillMd.getParent();
			List<SkillFile> skillFiles = new ArrayList<>();
			for (Path p : Files.walk(skillFolder).filter(Files::isRegularFile).sorted().toList()) {
				String relative = skillFolder.relativize(p).toString().replace('\\', '/');
				skillFiles.add(new SkillFile(relative, Files.readString(p)));
			}
			SkillMetadata metadata = parseSkillsMetadata(
					skillFiles.stream().filter(f -> f.relativePath().equals("SKILL.md")).findFirst().orElseThrow()
							.content().lines().toList());
			if (metadata.name() == null || metadata.name().isBlank() || metadata.name().length() > 64) {
				throw new IOException(
						"SKILL.md does not contain a valid skill name. Must be present, less than 64 characters.");
			}
			if (!metadata.name().matches("[a-z0-9-]+")) {
				throw new IOException("Skill name can only contain lowercase letters, numbers, and hyphens.");
			}
			if (skills.stream().anyMatch(skill -> skill.name().equalsIgnoreCase(metadata.name()))) {
				throw new IOException("A skill with this name already exists.");
			}
			if (metadata.description() == null || metadata.description().isBlank()
					|| metadata.description().length() > 1024) {
				throw new IOException(
						"SKILL.md does not contain a valid skill description. Must be present, less than 1024 characters.");
			}
		}
		return true;
	}

	@Override
	public void addSkill(Path skillFile) throws IOException {
		Path target = config.getConfig().fileStores().skills();

		if (Files.exists(target.resolve(skillFile.getFileName()))) {
			throw new IOException("Cannot overwrite an existing skill.");
		}

		Path resultFile = Files.copy(skillFile, target.resolve(skillFile.getFileName()));

		addSkillFromZip(resultFile);
	}

	private void addSkillFromZip(Path file) throws IOException {
		URI zipUri = URI.create("jar:" + file.toUri());
		try (FileSystem zipFs = FileSystems.newFileSystem(zipUri, Map.of())) {
			Path zipRoot = zipFs.getRootDirectories().iterator().next();
			Path skillMd = Files.walk(zipRoot)
					.filter(p -> p.getFileName() != null && p.getFileName().toString().equals("SKILL.md")).findFirst()
					.orElseThrow(() -> new IOException("No SKILL.md found in .skill file: " + file));

			Path skillFolder = skillMd.getParent();
			List<SkillFile> skillFiles = new ArrayList<>();
			for (Path p : Files.walk(skillFolder).filter(Files::isRegularFile).sorted().toList()) {
				String relative = skillFolder.relativize(p).toString().replace('\\', '/');
				skillFiles.add(new SkillFile(relative, Files.readString(p)));
			}

			SkillMetadata metadata = parseSkillsMetadata(
					skillFiles.stream().filter(f -> f.relativePath().equals("SKILL.md")).findFirst().orElseThrow()
							.content().lines().toList());
			logger.info("Found packaged skill: " + metadata.name());
			skills.add(new Skill(metadata.name(), metadata, skillFiles));
		}

	}

	private SkillMetadata parseSkillsMetadata(List<String> lines) {
		// Extract only the front-matter block
		if (lines.isEmpty() || !lines.get(0).trim().equals("---")) {
			return new SkillMetadata(null, null);
		}

		String frontmatter = lines.stream().skip(1).takeWhile(line -> !line.trim().equals("---"))
				.collect(Collectors.joining("\n"));

		try {
			JsonNode root = YamlMapper.readTree(frontmatter);
			String name = textOrNull(root, "name");
			String description = textOrNull(root, "description");
			return new SkillMetadata(name, description);
		} catch (JsonProcessingException e) {
			return new SkillMetadata(null, null);
		}
	}

	private String textOrNull(JsonNode node, String field) {
		JsonNode child = node.get(field);
		return child != null && !child.isNull() ? child.asText() : null;
	}

}
