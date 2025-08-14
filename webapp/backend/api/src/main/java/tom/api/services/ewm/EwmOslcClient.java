package tom.api.services.ewm;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

public interface EwmOslcClient {

	void login() throws IOException;

	/**
	 * Discover the Project Area UUID by human-readable name using /rootservices.
	 * This is a pragmatic approach; for production you may prefer parsing RDF
	 * properly.
	 */
	String discoverProjectAreaUUID(String projectAreaName) throws IOException;

	List<WorkItem> queryAllWorkItems(String projectAreaUUID, String whereClause, int pageSize,
			List<String> customAttributeIds) throws IOException;

	/**
	 * Retrieves all links (OSLC resource references) of a work item.
	 * 
	 * @param workItemId numeric ID or UUID of the work item
	 * @return Map where key = link type predicate (e.g.,
	 *         rtc_cm:com.ibm.team.workitem.linktype.parentworkitem.parent), value =
	 *         list of target resource URIs
	 */
	Map<String, List<URI>> getAllLinks(String workItemId) throws IOException;

	/**
	 * Fetch work items for a given saved query ID in the project area. You can get
	 * the query ID from the EWM Web UI URL or by querying the OSLC queries list.
	 */
	List<WorkItem> fetchByQueryId(String projectAreaUUID, String queryId) throws IOException;

	/**
	 * Fetch all open defects in the given project area.
	 */
	List<WorkItem> fetchOpenDefects(String projectAreaUUID) throws IOException;

	/**
	 * Fetch work items owned by a given user. ownerUserId is the short user ID, not
	 * UUID (e.g., 'jsmith').
	 */
	List<WorkItem> fetchByOwner(String projectAreaUUID, String ownerUserId) throws IOException;

	/**
	 * Generic where-based query helper.
	 */
	List<WorkItem> queryWithWhere(String projectAreaUUID, String whereClause) throws IOException;

}
