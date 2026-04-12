package tom.assistant.service.agent.worker;

public class WorkflowPlannerWorker implements WorkerHandler {

	public WorkflowPlannerWorker() {
	}

	@Override
	public WorkerDecision handle(WorkerContext ctx) {
		return WorkerDecision.llmCall("workflow_planner");
	}

}
