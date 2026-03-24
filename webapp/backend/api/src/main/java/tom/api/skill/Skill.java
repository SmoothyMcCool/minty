package tom.api.skill;

import java.util.List;

public record Skill(String name, SkillMetadata metadata, List<SkillFile> files) {
	SkillFile mainFile() {
		return files.stream().filter(f -> f.relativePath().equals("SKILL.md")).findFirst()
				.orElseThrow(() -> new IllegalStateException("No SKILL.md in skill: " + name));
	}
}
