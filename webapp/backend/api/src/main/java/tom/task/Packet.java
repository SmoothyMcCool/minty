package tom.task;

import java.util.HashMap;
import java.util.Map;

public class Packet {

	private String id;
	private String text;
	private Map<String, Object> data;

	public Packet() {
		id = "";
		text = "";
		data = Map.of();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Map<String, Object> getData() {
		return data;
	}

	public void setData(Map<String, Object> data) {
		this.data = data;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<>();
		map.put("id", id);
		if (!text.isBlank()) {
			map.put("text", text);
		}
		if (!data.isEmpty()) {
			map.put("data", data);
		}
		return map;
	}

	@Override
	public String toString() {
		return "Packet [id=" + id + ", text=" + text + ", data=" + data + "]";
	}
}
