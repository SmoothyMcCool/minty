package tom.tasks;

import java.util.List;

import tom.api.UserId;
import tom.task.enumspec.EnumSpec;
import tom.task.enumspec.EnumSpecCreator;
import tom.task.enumspec.NameValuePair;

public class GroupingEnumSpecCreator implements EnumSpecCreator {

	private final String enumName;

	public GroupingEnumSpecCreator() {
		enumName = "Grouping";
	}

	@Override
	public EnumSpec getEnumList(UserId userId) {

		List<NameValuePair> pairs = List.of(new NameValuePair("By ID", "By ID"), new NameValuePair("All", "All"));

		EnumSpec spec = new EnumSpec();
		spec.setName(enumName);
		spec.setValues(pairs);
		return spec;
	}

	@Override
	public String getName() {
		return enumName;
	}

}
