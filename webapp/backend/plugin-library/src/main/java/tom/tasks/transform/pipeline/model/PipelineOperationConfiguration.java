package tom.tasks.transform.pipeline.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class PipelineOperationConfiguration {
	private final List<Object> list;
	private final Map<String, Object> map;
	private final String string;

	private PipelineOperationConfiguration(List<Object> list, Map<String, Object> map, String string) {
		this.list = list;
		this.map = map;
		this.string = string;
	}

	public static PipelineOperationConfiguration ofList(List<Object> list) {
		return new PipelineOperationConfiguration(Objects.requireNonNull(list), null, null);
	}

	public static PipelineOperationConfiguration ofMap(Map<String, Object> map) {
		return new PipelineOperationConfiguration(null, Objects.requireNonNull(map), null);
	}

	public static PipelineOperationConfiguration ofString(String string) {
		return new PipelineOperationConfiguration(null, null, Objects.requireNonNull(string));
	}

	public boolean isList() {
		return list != null;
	}

	public boolean isMap() {
		return map != null;
	}

	public boolean isString() {
		return string != null;
	}

	public Optional<List<Object>> getList() {
		return Optional.ofNullable(list);
	}

	public Optional<Map<String, Object>> getMap() {
		return Optional.ofNullable(map);
	}

	public Optional<String> getString() {
		return Optional.ofNullable(string);
	}

	public Object unwrap() {
		return list != null ? list : map != null ? map : string;
	}

	@Override
	public String toString() {
		return String.valueOf(unwrap());
	}
}
