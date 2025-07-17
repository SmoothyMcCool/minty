package tom.meta.repository;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class UserMeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private int userId;
    private int totalAssistantsCreated;
    private int totalConversations;
    private int totalWorkflowsCreated;
    private int totalWorkflowRuns;
    private int totalLogins;
    @Column(name = "lastLogin", columnDefinition = "DATE")
    private LocalDate lastLogin;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getTotalAssistantsCreated() {
        return totalAssistantsCreated;
    }

    public void setTotalAssistantsCreated(int totalAssistantsCreated) {
        this.totalAssistantsCreated = totalAssistantsCreated;
    }

    public int getTotalConversations() {
        return totalConversations;
    }

    public void setTotalConversations(int totalConversations) {
        this.totalConversations = totalConversations;
    }

    public int getTotalWorkflowsCreated() {
        return totalWorkflowsCreated;
    }

    public void setTotalWorkflowsCreated(int totalWorkflowsCreated) {
        this.totalWorkflowsCreated = totalWorkflowsCreated;
    }

    public int getTotalWorkflowRuns() {
        return totalWorkflowRuns;
    }

    public void setTotalWorkflowRuns(int totalWorkflowRuns) {
        this.totalWorkflowRuns = totalWorkflowRuns;
    }

    public int getTotalLogins() {
        return totalLogins;
    }

    public void setTotalLogins(int totalLogins) {
        this.totalLogins = totalLogins;
    }

    public LocalDate getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDate lastLogin) {
        this.lastLogin = lastLogin;
    }

}
