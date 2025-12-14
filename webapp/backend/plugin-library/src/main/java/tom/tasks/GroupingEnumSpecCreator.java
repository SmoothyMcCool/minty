package tom.tasks;

import java.util.List;

import tom.api.UserId;
import tom.api.task.enumspec.EnumSpec;
import tom.api.task.enumspec.EnumSpecCreator;
import tom.api.task.enumspec.NameValuePair;

public class GroupingEnumSpecCreator implements EnumSpecCreator {

	private final String enumName;

	public GroupingEnumSpecCreator() {
		enumName = "Grouping";
	}

	@Override
	public EnumSpec getEnumList(UserId userId) {

		List<NameValuePair> pairs = List.of(new NameValuePair(Grouping.ById.toString(), Grouping.ById.toString()),
				new NameValuePair(Grouping.All.toString(), Grouping.All.toString()));

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
