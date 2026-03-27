package tom.document.xmi;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import tom.document.xmi.model.Actor;
import tom.document.xmi.model.NodeWithParent;
import tom.document.xmi.model.UseCase;
import tom.document.xmi.model.UseCaseModel;
import tom.document.xmi.model.UseCaseRelation;

public class EnterpriseArchitectUseCaseMapper {

	public UseCaseModel parse(InputStream is) throws Exception {
		XmlMapper xmlMapper = new XmlMapper();
		JsonNode root = xmlMapper.readTree(is);

		UseCaseModel model = new UseCaseModel();
		Map<String, String> idToName = new HashMap<>();

		List<JsonNode> elements = findNodes(root, "packagedElement");

		// --- 1. Actors + Use Cases ---
		for (JsonNode node : elements) {
			String type = node.path("@xmi:type").asText(null);
			String id = node.path("@xmi:id").asText(null);
			String name = node.path("@name").asText(null);

			if (id == null || name == null || name.isEmpty())
				continue;

			if ("uml:Actor".equals(type)) {
				Actor a = new Actor();
				a.id = id;
				a.name = name;
				model.actors.put(id, a);
				idToName.put(id, name);
			} else if ("uml:UseCase".equals(type)) {
				UseCase uc = new UseCase();
				uc.id = id;
				uc.name = name;
				model.useCases.put(id, uc);
				idToName.put(id, name);
			}
		}

		// --- 2. Associations (Actor ↔ UseCase) ---
		for (JsonNode node : elements) {
			String type = node.path("@xmi:type").asText();
			if (!"uml:Association".equals(type))
				continue;

			List<String> ends = new ArrayList<>();

			JsonNode ownedEnds = node.get("ownedEnd");
			if (ownedEnds != null) {
				if (ownedEnds.isArray()) {
					for (JsonNode end : ownedEnds) {
						addEndRef(end, idToName, ends);
					}
				} else {
					addEndRef(ownedEnds, idToName, ends);
				}
			}

			if (ends.size() == 2) {
				String a = ends.get(0);
				String b = ends.get(1);

				if (model.actors.containsKey(a) && model.useCases.containsKey(b)) {
					addRelation(model, UseCaseRelation.Type.ASSOCIATION, a, b);
				} else if (model.actors.containsKey(b) && model.useCases.containsKey(a)) {
					addRelation(model, UseCaseRelation.Type.ASSOCIATION, b, a);
				}
			}
		}

		// --- 3. Include ---
		List<NodeWithParent> includeNodes = findNodesWithParent(root, "include");

		for (NodeWithParent np : includeNodes) {
			JsonNode parent = np.parent;
			JsonNode n = np.node;

			if (!isUseCase(parent))
				continue;

			String from = parent.path("@xmi:id").asText(null);
			String to = n.path("@addition").asText(null);

			if (validRelation(model, from, to)) {
				addRelation(model, UseCaseRelation.Type.INCLUDE, from, to);
			}
		}

		// --- 4. Extend ---
		List<NodeWithParent> extendNodes = findNodesWithParent(root, "extend");

		for (NodeWithParent np : extendNodes) {
			JsonNode parent = np.parent;
			JsonNode n = np.node;

			if (!isUseCase(parent))
				continue;

			String from = parent.path("@xmi:id").asText(null);
			String to = n.path("@extendedCase").asText(null);

			if (validRelation(model, from, to)) {
				addRelation(model, UseCaseRelation.Type.EXTEND, from, to);
			}
		}

		return model;
	}

	// --- Helpers ---

	private void addEndRef(JsonNode end, Map<String, String> idToName, List<String> ends) {
		String ref = end.path("@type").asText(null);
		if (ref != null && idToName.containsKey(ref)) {
			ends.add(ref);
		}
	}

	private boolean isUseCase(JsonNode node) {
		return node != null && "uml:UseCase".equals(node.path("@xmi:type").asText());
	}

	private boolean validRelation(UseCaseModel model, String from, String to) {
		return from != null && to != null && model.useCases.containsKey(from) && model.useCases.containsKey(to);
	}

	private void addRelation(UseCaseModel model, UseCaseRelation.Type type, String from, String to) {
		UseCaseRelation r = new UseCaseRelation();
		r.type = type;
		r.from = from;
		r.to = to;
		model.relations.add(r);
	}

	private List<JsonNode> findNodes(JsonNode root, String name) {
		List<JsonNode> result = new ArrayList<>();
		traverse(root, null, name, result, false);
		return result;
	}

	private List<NodeWithParent> findNodesWithParent(JsonNode root, String name) {
		List<NodeWithParent> result = new ArrayList<>();
		traverse(root, null, name, result, true);
		return result;
	}

	@SuppressWarnings("unchecked")
	private void traverse(JsonNode node, JsonNode parent, String name, List<?> result, boolean includeParent) {
		if (node.isObject()) {
			node.fields().forEachRemaining(e -> {
				if (e.getKey().equals(name)) {
					if (e.getValue().isArray()) {
						for (JsonNode child : e.getValue()) {
							addResult(result, child, node, includeParent);
						}
					} else {
						addResult(result, e.getValue(), node, includeParent);
					}
				}
				traverse(e.getValue(), node, name, result, includeParent);
			});
		} else if (node.isArray()) {
			for (JsonNode child : node) {
				traverse(child, parent, name, result, includeParent);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void addResult(List<?> result, JsonNode node, JsonNode parent, boolean includeParent) {
		if (includeParent) {
			((List<NodeWithParent>) result).add(new NodeWithParent(node, parent));
		} else {
			((List<JsonNode>) result).add(node);
		}
	}
}