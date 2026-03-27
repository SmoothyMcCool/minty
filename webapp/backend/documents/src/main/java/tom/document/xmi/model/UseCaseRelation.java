package tom.document.xmi.model;

public class UseCaseRelation {
	public enum Type {
		ASSOCIATION, INCLUDE, EXTEND
	}

	public Type type;
	public String from;
	public String to;
}
