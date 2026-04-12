package tom.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tom.assistant.service.agent.worker.GeneralWorker;
import tom.assistant.service.agent.worker.MermaidGeneratorWorker;
import tom.assistant.service.agent.worker.MermaidValidatorWorker;
import tom.assistant.service.agent.worker.ParseDiagramWorker;
import tom.assistant.service.agent.worker.WorkerRegistry;
import tom.assistant.service.agent.worker.WorkflowPlannerWorker;

@Configuration
public class AgentConfig {

	@Bean
	public WorkerRegistry workerRegistry() {

		WorkerRegistry registry = new WorkerRegistry();

		registry.register("general", new GeneralWorker());
		registry.register("workflow_planner", new WorkflowPlannerWorker());
		registry.register("mermaid_generator", new MermaidGeneratorWorker());
		registry.register("mermaid_validator", new MermaidValidatorWorker());
		registry.register("diagram_parser", new ParseDiagramWorker());

		return registry;
	}

}
