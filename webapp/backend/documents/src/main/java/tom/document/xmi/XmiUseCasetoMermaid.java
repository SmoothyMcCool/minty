package tom.document.xmi;

import tom.document.xmi.model.Actor;
import tom.document.xmi.model.UseCaseModel;
import tom.document.xmi.model.UseCaseRelation;

public class XmiUseCasetoMermaid {

	public String write(UseCaseModel model) {
		StringBuilder sb = new StringBuilder();
		sb.append("sequenceDiagram\n");

		// Actors
		for (Actor a : model.actors.values()) {
			sb.append("actor ").append(safe(a.name)).append("\n");
		}

		sb.append("participant System\n");

		// Associations (main flows)
		for (UseCaseRelation r : model.relations) {
			if (r.type == UseCaseRelation.Type.ASSOCIATION) {
				String actor = safe(model.actors.get(r.from).name);
				String useCase = safe(model.useCases.get(r.to).name);

				sb.append(actor).append("->>System: ").append(useCase).append("\n");

				// include / extend nested handling
				appendNested(sb, model, r.to, "  ");
			}
		}

		return sb.toString();
	}

	private void appendNested(StringBuilder sb, UseCaseModel model, String useCaseId, String indent) {
		for (UseCaseRelation r : model.relations) {
			if (r.from.equals(useCaseId)) {
				String name = safe(model.useCases.get(r.to).name);

				if (r.type == UseCaseRelation.Type.INCLUDE) {
					sb.append(indent).append("System->>System: ").append(name).append(" <<include>>\n");

					appendNested(sb, model, r.to, indent + "  ");
				}

				if (r.type == UseCaseRelation.Type.EXTEND) {
					sb.append(indent).append("opt ").append(name).append(" <<extend>>\n").append(indent)
							.append("  System->>System: ").append(name).append("\n").append(indent).append("end\n");
				}
			}
		}
	}

	private String safe(String s) {
		return s.replaceAll("[^a-zA-Z0-9_]", "_");
	}
}
