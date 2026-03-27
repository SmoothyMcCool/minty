package tom.document.xmi.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UseCaseModel {
	public Map<String, Actor> actors = new LinkedHashMap<>();
	public Map<String, UseCase> useCases = new LinkedHashMap<>();
	public List<UseCaseRelation> relations = new ArrayList<>();
}
