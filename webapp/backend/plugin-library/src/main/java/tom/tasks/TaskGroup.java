package tom.tasks;

public enum TaskGroup {

	TRANSFORM("Transform"), FLOW_CONTROL("Flow Control"), EMIT("Emit");

	private final String code;

	TaskGroup(String code) {
		this.code = code;
	}

	@Override
	public String toString() {
		return code;
	}
}
