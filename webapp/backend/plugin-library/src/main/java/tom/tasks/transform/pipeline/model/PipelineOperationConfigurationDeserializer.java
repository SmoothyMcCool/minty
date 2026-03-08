package tom.tasks.transform.pipeline.model;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;

public class PipelineOperationConfigurationDeserializer extends JsonDeserializer<PipelineOperationConfiguration> {

	@SuppressWarnings("unchecked")
	@Override
	public PipelineOperationConfiguration deserialize(JsonParser parser, DeserializationContext ctx)
			throws IOException {

		ObjectMapper mapper = (ObjectMapper) parser.getCodec();
		JsonNode node = mapper.readTree(parser);

		if (node.getNodeType() == JsonNodeType.ARRAY) {
			List<Object> list = mapper.convertValue(node, List.class);
			return PipelineOperationConfiguration.ofList(list);
		}

		if (node.getNodeType() == JsonNodeType.OBJECT) {
			Map<String, Object> map = mapper.convertValue(node, Map.class);
			return PipelineOperationConfiguration.ofMap(map);
		}

		if (node.getNodeType() == JsonNodeType.STRING) {
			return PipelineOperationConfiguration.ofString(node.asText());
		}

		ctx.reportInputMismatch(PipelineOperationConfiguration.class,
				"Expected JSON array, object or string but got %s", node.getNodeType());

		return null;
	}
}
