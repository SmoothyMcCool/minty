package tom.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class Packet {

	private String id;
	private List<String> text;
	private List<Map<String, Object>> data;

	public Packet() {
		id = "";
		text = new ArrayList<>();
		data = new ArrayList<>();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<String> getText() {
		return text;
	}

	public void setText(List<String> text) {
		this.text = text;
	}

	public void addText(String text) {
		this.text.add(text);
	}

	public List<Map<String, Object>> getDataList() {
		return data;
	}

	public Map<String, Object> getData() {

		if (data.size() > 0) {
			return data.get(0);
		}
		return null;
	}

	public void setData(List<Map<String, Object>> data) {
		this.data = data;
	}

	public void addData(Map<String, Object> data) {
		this.data.add(data);
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<>();
		map.put("id", id);
		if (!text.isEmpty()) {
			map.put("text", text);
		}
		if (!data.isEmpty()) {
			map.put("data", data);
		}
		return map;
	}

	@Override
	public String toString() {
		String dataStr;
		try {
			dataStr = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(data);
		} catch (JsonProcessingException e) {
			dataStr = "<<Failed to stringify data>>";
		}
		return "Packet [id=" + id + ", text=" + text + ", data=" + dataStr + "]";
	}

	public void addDataList(List<Map<String, Object>> dataList) {
		data.addAll(dataList);
	}
}
