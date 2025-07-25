package tom.task.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PublicTask {

	String name();

	// Specified class must implement tom.task.TaskConfig
	// Must be a fully-qualified class name "e.g. tom.task.TaskConfig"
	// Leave at default value if no configuration information is necessary.
	String configClass() default "tom.task.NullTaskConfig";
}
