package tom.workflow.controller;

import java.util.Map;

public class WorkflowRequest {

    private String request;
    private Map<String, String> data;

    public WorkflowRequest() {

    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }

}
