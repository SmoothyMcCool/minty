package tom.tasks.transform.pipeline;

import java.util.ArrayList;
import java.util.List;

import tom.api.UserId;
import tom.api.task.enumspec.EnumSpec;
import tom.api.task.enumspec.EnumSpecCreator;
import tom.api.task.enumspec.NameValuePair;
import tom.tasks.transform.pipeline.operations.KeepFieldsOperation;
import tom.tasks.transform.pipeline.operations.RemoveEmptyOperation;
import tom.tasks.transform.pipeline.operations.RemoveFieldsOperation;
import tom.tasks.transform.pipeline.operations.RemoveNullsOperation;

public class TransformOperationEnumSpecCreator implements EnumSpecCreator {

	public static final String EnumName = "Transform Operation";

	public static final String NoParams = "";
	public static final String ListParam = "List";

	public TransformOperationEnumSpecCreator() {
	}

	@Override
	public EnumSpec getEnumList(UserId userId) {

		List<NameValuePair> pairs = new ArrayList<>();
		pairs.add(new NameValuePair(RemoveNullsOperation.OperationName, NoParams));
		pairs.add(new NameValuePair(RemoveEmptyOperation.OperationName, NoParams));
		pairs.add(new NameValuePair(RemoveFieldsOperation.OperationName, ListParam));
		pairs.add(new NameValuePair(KeepFieldsOperation.OperationName, ListParam));

		EnumSpec spec = new EnumSpec();
		spec.setName(EnumName);
		spec.setValues(pairs);
		return spec;
	}

	@Override
	public String getName() {
		return EnumName;
	}

}
