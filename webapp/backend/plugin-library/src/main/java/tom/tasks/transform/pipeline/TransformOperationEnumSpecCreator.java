package tom.tasks.transform.pipeline;

import java.util.ArrayList;
import java.util.List;

import tom.api.UserId;
import tom.api.task.enumspec.EnumSpec;
import tom.api.task.enumspec.EnumSpecCreator;
import tom.api.task.enumspec.NameValuePair;

public class TransformOperationEnumSpecCreator implements EnumSpecCreator {

	public static final String EnumName = "Transform Operation";

	public static final String RemoveNulls = "Remove Nulls";
	public static final String RemoveEmpty = "Remove Empty Elements";
	public static final String RemoveFields = "Remove Fields";
	public static final String KeepFields = "Keep Fields";

	public static final String NoParams = "";
	public static final String ListParam = "List";

	private final String enumName;

	public TransformOperationEnumSpecCreator() {
		enumName = EnumName;
	}

	@Override
	public EnumSpec getEnumList(UserId userId) {

		List<NameValuePair> pairs = new ArrayList<>();
		pairs.add(new NameValuePair(RemoveNulls, NoParams));
		pairs.add(new NameValuePair(RemoveEmpty, NoParams));
		pairs.add(new NameValuePair(RemoveFields, ListParam));
		pairs.add(new NameValuePair(KeepFields, ListParam));

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
