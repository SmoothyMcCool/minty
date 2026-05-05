package tom.api;

import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

public class MintyObjectMapper {

	public static final JsonMapper StandardJsonMapper = JsonMapper.builder().enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
			.build();

	public static final JsonMapper PrettyPrinterJsonMapper = StandardJsonMapper.rebuild()
			.enable(SerializationFeature.INDENT_OUTPUT).build();

	public static final YAMLMapper StandardYamlMapper = YAMLMapper.builder().build();

	public static final XmlMapper StandardXmlMapper = XmlMapper.builder().build();

	public static final XmlMapper PrettyPrinterXmlMapper = XmlMapper.builder()
			.enable(SerializationFeature.INDENT_OUTPUT).build();
}
