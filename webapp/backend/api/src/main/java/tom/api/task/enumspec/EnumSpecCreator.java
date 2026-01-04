package tom.api.task.enumspec;

import tom.api.UserId;
import tom.api.services.PluginServices;

public interface EnumSpecCreator {

	EnumSpec getEnumList(UserId userId);

	String getName();

	default void setPluginServices(PluginServices pluginServices) {

	}

}
