package tom.tasks.transform.pipeline.operations;

import java.util.HashMap;
import java.util.Map;

public class PipelineOperationRegistry {

	private static final Map<String, TransformOperation> registry = new HashMap<>();

	static {
		register(new ExpressionOperation());
		register(new KeepFieldsOperation());
		register(new RemoveEmptyOperation());
		register(new RemoveFieldsOperation());
		register(new RemoveNullsOperation());
		register(new RenameFieldsOperation());
		register(new SetFieldOperation());
		register(new FlattenOperation());
	}

	public static void register(TransformOperation op) {
		registry.put(op.getName().toUpperCase(), op);
	}

	public static TransformOperation get(String name) {
		return registry.get(name.toUpperCase());
	}
}
