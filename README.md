# minty
Modular Intelligence for Next-gen Tasking by You

## Builds

### Prerequisites

#### For Building

The build environment requires (or at least my build environment uses):
- Angular 19 and prerequisites: https://v19.angular.dev/installation
- Maven 3.9
- Java 21 JDK

#### For Running

Make sure you have the following software installed in order to run Minty:
- MariaDB 11.8 or newer is required in order to be able to host the document vector store.
- Tomcat 11.
- An Ollama instance to connect to.

### Build

#### For Development

The build assumes that Tomcat is installed in D:\projects\Tomcat. It will automatically place the built war file in the webapps folder here.
Customize your Tomcat location at D:\projects\Minty-GitHub\webapp\backend\bundle\pom.xml: outputDirectory element

It's also assumed that Tomcat is running on port 8080.

Frontend (Angular CLI)
```
webapp\frontend\app\ng serve --proxy-config proxy.conf.json
```

Backend (Tomcat Webapp):
Install parent into your local .m2:
```webapp\backend\parent\mvn install```
Install task jar into your local .m2:
```webapp\backend\task\mvn install```
Build the App:
```webapp\backend\solution\mvn install```

Now build any task jars you've written, and copy them to the directory specified by the "taskLibrary" property (see Configuration below).

#### For Deployment

Build frontend for production:
Change webapp\frontend\app\src\index.html:
```
Change base href from
<base href="/">
to
<base href="/Minty/">
```

Run the solution POM. This will automatically bundle in the Angular build.
```
```webapp\backend\solution\mvn install```
```

## Configuration

All config is stored in **vwebapp\backend\bundle\src\main\resources\application.properties**. Fill your boots

## Defining Your Own Workflows

Make a new project that includes the parent and task JARs. Here is a sample POM:
```
<project xmlns="http://maven.apache.org/POM/4.0.0"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>

	<groupId>tvm.tasks</groupId>
	<artifactId>tasklib</artifactId>
	<version>0.0.1-SNAPSHOT</version>

	<properties>
		<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
	</properties>

	<dependencies>

		<!-- Mr. Ai-->
		<dependency>
			<groupId>tom.minty</groupId>
			<artifactId>parent</artifactId>
			<version>0.0.1-SNAPSHOT</version>
			<type>pom</type>
		</dependency>
		<dependency>
			<groupId>tom.minty</groupId>
			<artifactId>task</artifactId>
			<version>0.0.1-SNAPSHOT</version>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>
				<version>3.14.0</version>
				<configuration>
					<release>21</release>
				</configuration>
			</plugin>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-jar-plugin</artifactId>
				<version>3.4.2</version>
				<configuration>
					<outputDirectory>D:/projects/Minty/taskLib</outputDirectory>
				</configuration>
			</plugin>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-resources-plugin</artifactId>
				<version>3.3.1</version>
				<executions>
					<execution>
						<id>copy-pug-templates</id>
						<phase>install</phase>
						<goals>
							<goal>copy-resources</goal>
						</goals>
						<configuration>
							<outputDirectory>D:/projects/Minty/taskLib</outputDirectory>
							<resources>
								<resource>
									<directory>/src/main/resources/templates</directory>
									<includes>
										<include>*.pug</include>
									</includes>
								</resource>
							</resources>
						</configuration>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>

</project>
```

### Classes

#### Public Workflows
Workflows are run by implementing the ```tom.task.AiTask``` interface.
If you want your workflow publicly available in the GUI, annotate your class with
```@PublicWorkflow(name = "{{NameOfYourWorkflow}}", configClass = "classThatDefinesUserSelectableParameters")```
If you would like to make use of the built-in services, implement the tom.task.ServiceConsumer interface.

Public workflows must implement:
- default constructor
- constructor that takes an instance of the specified configuration class.

Put the work your task will do in the doWork() method. It will be scheduled to run in a thread pool. If you need to run child tasks, return a list of AiTask objects to be run.
The order in which tasks run is not guaranteed.

#### Configuration

Configuration classes hold all the user-selectable parameters for a task. Parameters can be:
- number
- string
- Ai Assistant.

Configuration classes must implement logic in their constructors to read a ```Map<String, String>```.
They must also implement a getConfig method that create a meta-object describing the required user configuraiton.
Here is an example of a Config class that asks the user for two strings (a query ID, a prompt) and a pre-defined assistant to use:
```
package tom.tasks.TestTask;

import java.util.HashMap;
import java.util.Map;

import tom.task.AiTaskConfig;
import tom.task.AiTaskConfigTypes;

public class TestTaskConfig implements AiTaskConfig {

	private int assistant;
	private String queryId;
	private String prompt;

	public AthenaTaskConfig() {
		assistant = 0;
		rqeryId = "";
		prompt = "";
	}

	public AthenaTaskConfig(Map<String, String> config) {
		assistant = Integer.parseInt(config.get("assistant"));
		qeryId = config.get("Query ID");
		prompt = config.get("prompt");
	}

	@Override
	public Map<String, AiTaskConfigTypes> getConfig() {
		Map<String, AiTaskConfigTypes> cfg = new HashMap<>();
		cfg.put("assistant", AiTaskConfigTypes.AssistantIdentifier);
		cfg.put("Query ID", AiTaskConfigTypes.String);
		cfg.put("prompt", AiTaskConfigTypes.String);
		return cfg;
	}

	public int getAssistant() {
		return assistant;
	}

	public String getQueryId() {
		return queryId;
	}

	public String getPrompt() {
		return prompt;
	}
}

```

#### Task Output

Tasks define a pug HTML template file to format their output. Tasks must implement a getResult method that returns a Map<StringKey, StringValue> of data to be used to render the pug tempalte.
If your task has children, be sure to return the results from children as well.

Here is an example of a Task:
```
package tom.tasks.athena;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import tom.task.AiTask;
import tom.task.ServiceConsumer;
import tom.task.annotations.PublicWorkflow;
import tom.task.model.AssistantQuery;
import tom.task.services.TaskServices;

@PublicWorkflow(name = "TestTask", configClass = "tom.tasks.TestTask.TestTaskConfig")
public class AthenaTask implements AiTask, ServiceConsumer {

	private TaskServices taskServices;
	private UUID uuid;
	private String result;
	private TestTaskConfig config;
	private int userId;

	public AthenaTask(TestTaskConfig data) {
		config = data;
	}

	@Override
	public void setTaskServices(TaskServices taskServices) {
		this.taskServices = taskServices;
		uuid = UUID.randomUUID();
	}

	@Override
	public String taskName() {
		return "EvaluatePlan-" + uuid;
	}

	@Override
	public Map<String, Object> getResult() {
		Map<String, Object> result = new HashMap<>();
		result.put("result", this.result);
		return result;
	}

	@Override
	public String getResultTemplateFilename() {
		return "default.pug";
	}

	@Override
	public List<AiTask> doWork() {
		AssistantQuery query = new AssistantQuery();
		query.setAssistantId(config.getAssistant());
		query.setQuery(config.getPrompt());

		result = taskServices.getAssistantService().ask(userId, query);
		return null;
	}

	@Override
	public void setUserId(int userId) {
		this.userId = userId;
	}

}

```