package tom.tasks.transform.pipeline.model;

import java.util.List;
import java.util.Map;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.node.JsonNodeType;

public class PipelineOperationConfigurationDeserializer extends StdDeserializer<PipelineOperationConfiguration> {

	public PipelineOperationConfigurationDeserializer() {
		super(PipelineOperationConfiguration.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public PipelineOperationConfiguration deserialize(JsonParser parser, DeserializationContext ctxt)
			throws JacksonException {

		JsonNode node = ctxt.readTree(parser);

		if (node.getNodeType() == JsonNodeType.ARRAY) {
			List<Object> list = ctxt.readTreeAsValue(node, List.class);
			return PipelineOperationConfiguration.ofList(list);
		}

		if (node.getNodeType() == JsonNodeType.OBJECT) {
			Map<String, Object> map = ctxt.readTreeAsValue(node, Map.class);
			return PipelineOperationConfiguration.ofMap(map);
		}

		if (node.getNodeType() == JsonNodeType.STRING) {
			return PipelineOperationConfiguration.ofString(node.asString());
		}

		ctxt.reportInputMismatch(PipelineOperationConfiguration.class,
				"Expected JSON array, object or string but got %s", node.getNodeType());

		return null;
	}
}