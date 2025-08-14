package tom.tasks.ewm;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.api.services.TaskServices;
import tom.api.services.ewm.EwmOslcClient;
import tom.api.services.ewm.WorkItem;
import tom.task.AiTask;
import tom.task.ServiceConsumer;
import tom.task.annotations.PublicTask;

@PublicTask(name = "Get All EWM Work Items From a Query", configClass = "tom.tasks.ewm.WorkItemsFromQueryConfig")
public class WorkItemsFromQuery implements AiTask, ServiceConsumer {

	private WorkItemsFromQueryConfig configuration;
	private UUID uuid = UUID.randomUUID();
	private List<WorkItem> workitems;
	String queryId = "";
	private TaskServices taskServices;

	public WorkItemsFromQuery() {
		configuration = new WorkItemsFromQueryConfig();
	}

	public WorkItemsFromQuery(WorkItemsFromQueryConfig configuration) {
		this.configuration = configuration;
	}

	@Override
	public String taskName() {
		return "Items-From-EWM-Query-" + uuid.toString();
	}

	@Override
	public Map<String, Object> getResult() {
		Map<String, Object> results = new HashMap<>();
		ObjectMapper mapper = new ObjectMapper();

		workitems.forEach(workitem -> {
			try {
				String strObj = mapper.writeValueAsString(workitem);
				results.put(workitem.title(), (Object) strObj);
			} catch (JsonProcessingException e) {
				// Nothing to do. Ignore and keep going.
			}
		});

		return results;
	}

	@Override
	public List<Map<String, String>> runTask() {
		String queryToUse = configuration.getQueryId();
		if (!queryId.isBlank()) {
			queryToUse = queryId;
		}

		EwmOslcClient client = taskServices.getEwmOslcQueryService().createClient(configuration.getServer(),
				configuration.getUsername(), configuration.getPassword());

		List<Map<String, String>> results = new ArrayList<>();

		try {
			client.login();
			String projectAreaUuid = client.discoverProjectAreaUUID(configuration.getProjectArea());
			List<WorkItem> workitems = client.fetchByQueryId(projectAreaUuid, queryToUse);

			workitems.forEach(workitem -> {
				Map<String, String> workitemExtract = attributesAsMap(workitem, configuration.getFields());
				results.add(workitemExtract);
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return results;
	}

	@Override
	public void setInput(Map<String, String> input) {
		if (input.containsKey("Data")) {
			queryId = input.get("Data");
		}
	}

	@Override
	public String expects() {
		return "A string queryId in the \"Data\" field. Any input received will override any queryId specified in the task configuration.";
	}

	@Override
	public String produces() {
		return " A JSON object representing the Work Item, including the fields specified in the configuration.";
	}

	@Override
	public void setTaskServices(TaskServices taskServices) {
		this.taskServices = taskServices;
	}

	private static Map<String, String> attributesAsMap(WorkItem wi, List<String> fields) {
		Map<String, String> attrs = new HashMap<>();
		List<RecordComponent> components = Arrays.asList(WorkItem.class.getRecordComponents());

		List<RecordComponent> matchedComponents = components.stream().filter(rc -> fields.contains(rc.getName()))
				.toList();

		matchedComponents.forEach(rc -> {
			try {
				Object value = rc.getAccessor().invoke(wi);
				if (value != null && !value.toString().isBlank()) {
					attrs.put(rc.getName(), value.toString());
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		// Let's see if the unmatched ones are found in the custom attributes.
		fields.forEach(field -> {
			try {
				if (wi.custom().containsKey(field)) {
					Object value = wi.custom().get(field);
					if (value != null && !value.toString().isBlank()) {
						attrs.put(field, value.toString());
					}
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		return attrs;
	}
}
