package tom.api.task.enumspec;

import java.util.List;

public class EnumSpec {

	protected String name;
	protected List<NameValuePair> values;

	public EnumSpec() {
		name = "";
		values = List.of();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<NameValuePair> getValues() {
		return values;
	}

	public void setValues(List<NameValuePair> values) {
		this.values = values;
	}

}
