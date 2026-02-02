package tom.tasks.transform.confluence;

import java.util.ArrayList;
import java.util.List;

import tom.api.UserId;
import tom.api.task.enumspec.EnumSpec;
import tom.api.task.enumspec.EnumSpecCreator;
import tom.api.task.enumspec.NameValuePair;

public class ConfluenceConcatenationEnumSpecCreator implements EnumSpecCreator {

	public static final String EnumName = "Multi Page Behaviour";
	private final String enumName;

	public ConfluenceConcatenationEnumSpecCreator() {
		enumName = EnumName;
	}

	@Override
	public EnumSpec getEnumList(UserId userId) {
		List<NameValuePair> pairs = new ArrayList<>();
		pairs.add(new NameValuePair(ConfluencePageConcatenationStrategy.Array.toString(),
				ConfluencePageConcatenationStrategy.Array.name()));
		pairs.add(new NameValuePair(ConfluencePageConcatenationStrategy.Concatenated.toString(),
				ConfluencePageConcatenationStrategy.Concatenated.name()));
		pairs.add(new NameValuePair(ConfluencePageConcatenationStrategy.MultiPacket.toString(),
				ConfluencePageConcatenationStrategy.MultiPacket.name()));

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
