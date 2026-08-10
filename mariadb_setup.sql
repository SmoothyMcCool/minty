-- =============================================================
-- Minty Database Setup Script
-- Requires: MariaDB 11.7+
-- =============================================================

-- -------------------------------------------------------------
-- Database
-- -------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS Minty;
USE Minty;

-- =============================================================
-- CORE TABLES
-- =============================================================

CREATE TABLE IF NOT EXISTS User (
    id       UUID         NOT NULL,
    account  VARCHAR(50),
    password VARCHAR(100),
    crypt    TEXT,
    salt     TEXT,
    PRIMARY KEY (id)
);

INSERT INTO `User` (id, account, password, crypt, salt)
VALUES ('00000000-0000-0000-0000-000000000000', 'dummy', 'dummy', NULL, NULL);

-- -------------------------------------------------------------

CREATE TABLE IF NOT EXISTS UserMeta (
    id                      INTEGER   NOT NULL AUTO_INCREMENT,
    userId                  UUID,
    totalAssistantsCreated  INTEGER,
    totalConversations      INTEGER,
    totalWorkflowsCreated   INTEGER,
    totalWorkflowRuns       INTEGER,
    totalLogins             INTEGER,
    lastLogin               TIMESTAMP,
    PRIMARY KEY (id)
);

-- =============================================================
-- ASSISTANT
-- =============================================================

CREATE TABLE IF NOT EXISTS Assistant (
    id          UUID         NOT NULL,
    name        VARCHAR(50),
    state       VARCHAR(20),
    prompt      TEXT,
    model       TEXT,
    contextSize INTEGER,
    temperature DOUBLE,
    topK        INTEGER,
    tools       JSON,
    ownerId     UUID,
    hasMemory   BOOLEAN,
    PRIMARY KEY (id)
);

-- =============================================================
-- CONVERSATION & MEMORY
-- =============================================================

CREATE TABLE IF NOT EXISTS Conversation (
    title                  TEXT,
    id                     UUID NOT NULL,
    ownerId                UUID,
    associatedAssistantId  UUID,
    projectId              UUID,
    lastUsed               TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS SPRING_AI_CHAT_MEMORY (
    sequence_id     BIGINT NOT NULL,
    conversation_id TEXT,
    content         TEXT,
    type            TEXT,
    timestamp       TIMESTAMP
);

CREATE INDEX IF NOT EXISTS SPRING_AI_CHAT_MEMORY_CONVERSATION_ID_SEQUENCE_ID_IDX
    ON SPRING_AI_CHAT_MEMORY (conversation_id(36), sequence_id);

-- =============================================================
-- DOCUMENTS & TAGS
-- =============================================================

CREATE TABLE IF NOT EXISTS Document (
    id         UUID      NOT NULL,
    title      TEXT,
    state      INTEGER,
    ownerId    UUID,
    projectId  UUID,
    vectorized BOOLEAN   DEFAULT FALSE,
    summary    LONGTEXT,
    created    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE DocumentSegment (
    id            UUID     NOT NULL,
    documentId    UUID     NOT NULL,
    content       LONGTEXT NOT NULL,
    sequenceOrder INT      NOT NULL,
    parentIndex   INT,
    level         INT,
    title         TEXT,
    created       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (id),
	FOREIGN KEY (docId) REFERENCES Document(id) ON DELETE CASCADE,
    INDEX       idx_doc_segments_order (docId, sequenceOrder)
);

CREATE TABLE IF NOT EXISTS Tag (
    id  UUID         NOT NULL,
    tag VARCHAR(255),
    PRIMARY KEY (id, tag)
);

CREATE TABLE IF NOT EXISTS TagToDoc (
    tagId      UUID NOT NULL,
    documentId UUID NOT NULL,
    PRIMARY KEY (tagId, documentId),
    FOREIGN KEY (tagId)      REFERENCES Tag(id),
    FOREIGN KEY (documentId) REFERENCES Document(id)
);

-- =============================================================
-- WORKFLOWS
-- =============================================================

CREATE TABLE IF NOT EXISTS Workflow (
    id          UUID         NOT NULL,
    ownerId     UUID,
    name        VARCHAR(255),
    description TEXT,
    shared      BOOLEAN,
    steps       JSON         NOT NULL,
    connections JSON         NOT NULL,
    outputStep  JSON,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS WorkflowRecord (
    id           UUID NOT NULL,
    result       JSON,
    output       LONGTEXT,
    outputFormat TEXT,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS WorkflowExecution (
    id       UUID NOT NULL,
    ownerId  UUID,
    name     TEXT,
    state    JSON,
    failed   BOOLEAN,
    recordId UUID,
    PRIMARY KEY (id),
    CONSTRAINT fk_workflow_execution_workflow_record
        FOREIGN KEY (recordId)
        REFERENCES WorkflowRecord(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS ResultTemplate (
    id      UUID NOT NULL,
    ownerId UUID,
    name    TEXT,
    content LONGTEXT
);

-- =============================================================
-- PROJECTS
-- =============================================================

CREATE TABLE IF NOT EXISTS Project (
    id      UUID      NOT NULL,
    ownerId UUID      NOT NULL,
    name    TEXT,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS ProjectNode (
    id        UUID          NOT NULL,
    projectId UUID          NOT NULL,
    parentId  UUID,
    ownerId   UUID          NOT NULL,
    name      VARCHAR(255)  NOT NULL,
    path      VARCHAR(512)  NOT NULL,
    type      ENUM('Folder', 'File') NOT NULL,
    fileType  ENUM('code', 'markdown', 'text', 'diagram', 'json', 'yaml','html') NULL,
    version   INT           NOT NULL DEFAULT 0,
    created   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (parentId)  REFERENCES ProjectNode(id) ON DELETE CASCADE,
    FOREIGN KEY (projectId) REFERENCES Project(id)     ON DELETE CASCADE,
    UNIQUE KEY  uk_node_project_path   (projectId, path),
    INDEX       idx_node_project_parent (projectId, parentId),
    INDEX       idx_node_project_path   (projectId, path)
);

CREATE TABLE IF NOT EXISTS ProjectFileContent (
    id      UUID     NOT NULL,
    nodeId  UUID     NOT NULL,
    ownerId UUID     NOT NULL,
    version INT      NOT NULL,
    content LONGTEXT NOT NULL,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (nodeId) REFERENCES ProjectNode(id) ON DELETE CASCADE,
    UNIQUE KEY uk_node_version  (nodeId, version),
    INDEX      idx_node_version (nodeId, version)
);

-- =============================================================
-- SKILLS
-- =============================================================

CREATE TABLE IF NOT EXISTS Skills (
    id      UUID         NOT NULL,
    name    VARCHAR(255) NOT NULL,
    file    LONGBLOB,
    ownerId UUID         NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_skills_owner (ownerId),
    INDEX idx_skills_name  (name)
);

-- =============================================================
-- VECTOR STORE
-- =============================================================

CREATE TABLE IF NOT EXISTS vector_store (
    doc_id    VARCHAR(255) PRIMARY KEY,
    text      TEXT,
    embedding VECTOR(768),
    meta      JSON
);

-- =============================================================
-- SPRING SESSION
-- =============================================================

CREATE TABLE IF NOT EXISTS SPRING_SESSION (
    PRIMARY_ID           CHAR(36)     NOT NULL,
    SESSION_ID           CHAR(36)     NOT NULL,
    CREATION_TIME        BIGINT       NOT NULL,
    LAST_ACCESS_TIME     BIGINT       NOT NULL,
    MAX_INACTIVE_INTERVAL INT         NOT NULL,
    EXPIRY_TIME          BIGINT       NOT NULL,
    PRINCIPAL_NAME       VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC;

CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX        SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX        SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE IF NOT EXISTS SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36)     NOT NULL,
    ATTRIBUTE_NAME     VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES    BLOB         NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK
        PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK
        FOREIGN KEY (SESSION_PRIMARY_ID)
        REFERENCES SPRING_SESSION(PRIMARY_ID)
        ON DELETE CASCADE
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC;

-- =============================================================
-- LLM REQUEST TRACKING
-- =============================================================

CREATE TABLE IF NOT EXISTS LlmRequestStatus (
    status VARCHAR(20) NOT NULL,
    PRIMARY KEY (status)
);

INSERT IGNORE INTO LlmRequestStatus (status) VALUES
    ('queued'),
    ('processing'),
    ('completed'),
    ('failed');

-- -------------------------------------------------------------

CREATE TABLE IF NOT EXISTS LlmRequests (
    id             UUID         NOT NULL,
    userId         UUID         NOT NULL,
    conversationId UUID         NOT NULL,
    assistantId    UUID         NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'queued',
    createdAt      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    queuedAt       DATETIME(6)  NULL,
    dequeuedAt     DATETIME(6)  NULL,
    firstTokenAt   DATETIME(6)  NULL,
    completedAt    DATETIME(6)  NULL,
    error          TEXT         NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_requests_status
        FOREIGN KEY (status) REFERENCES LlmRequestStatus(status)
        ON UPDATE CASCADE,
    CONSTRAINT fk_requests_assistant
        FOREIGN KEY (assistantId)
        REFERENCES Assistant(id)
        ON UPDATE CASCADE
);

CREATE INDEX idx_requests_userId         ON LlmRequests (userId);
CREATE INDEX idx_requests_conversationId ON LlmRequests (conversationId);
CREATE INDEX idx_requests_status         ON LlmRequests (status);
CREATE INDEX idx_requests_createdAt      ON LlmRequests (createdAt);
CREATE INDEX idx_requests_assistantId    ON LlmRequests(assistantId);

-- -------------------------------------------------------------

CREATE TABLE IF NOT EXISTS LlmRequestMetrics (
    id                UUID            NOT NULL,
    queueWaitUs       BIGINT UNSIGNED NULL,
    ttftUs            BIGINT UNSIGNED NULL,
    totalTimeUs       BIGINT UNSIGNED NULL,
    promptTokens      INT UNSIGNED    NULL,
    completionTokens  INT UNSIGNED    NULL,
    totalTokens       INT UNSIGNED    NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_metrics_request
        FOREIGN KEY (id) REFERENCES LlmRequests(id)
        ON DELETE CASCADE
);

-- =============================================================
-- LINK TABLES
-- =============================================================

-- -------------------------------------------------------------

CREATE TABLE IF NOT EXISTS UserAssistantLinks (
    userId      UUID NOT NULL,
    assistantId UUID NOT NULL,
    PRIMARY KEY (userId, assistantId),
    FOREIGN KEY (userId)      REFERENCES User(id)      ON DELETE CASCADE,
    FOREIGN KEY (assistantId) REFERENCES Assistant(id) ON DELETE CASCADE
);

-- -------------------------------------------------------------

CREATE TABLE IF NOT EXISTS UserSkillsLinks (
    userId  UUID NOT NULL,
    skillId UUID NOT NULL,
    PRIMARY KEY (userId, skillId),
    FOREIGN KEY (userId)  REFERENCES User(id)   ON DELETE CASCADE,
    FOREIGN KEY (skillId) REFERENCES Skills(id) ON DELETE CASCADE
);

-- -------------------------------------------------------------

CREATE TABLE IF NOT EXISTS UserWorkflowLinks (
    userId     UUID NOT NULL,
    workflowId UUID NOT NULL,
    PRIMARY KEY (userId, workflowId),
    FOREIGN KEY (userId)     REFERENCES User(id)     ON DELETE CASCADE,
    FOREIGN KEY (workflowId) REFERENCES Workflow(id) ON DELETE CASCADE
);

-- =============================================================
-- LLM REQUEST TRACKING
-- =============================================================

-- =============================================================
-- ANALYTICS
-- =============================================================


-- -------------------------------------------------------------
-- CONVERSATION STATISTICS
-- -------------------------------------------------------------

CREATE TABLE IF NOT EXISTS ConversationStatistics
(
    conversationId UUID NOT NULL,
    userId UUID NOT NULL,
    assistantId UUID NOT NULL,
    started DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed DATETIME NULL,
    lastActivity DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    messageCount INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (conversationId),
    CONSTRAINT fk_conversation_statistics_conversation
        FOREIGN KEY (conversationId)
        REFERENCES Conversation(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_conversation_statistics_user
        FOREIGN KEY (userId)
        REFERENCES User(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_conversation_statistics_assistant
        FOREIGN KEY (assistantId)
        REFERENCES Assistant(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_conversation_statistics_user
    ON ConversationStatistics(userId);

CREATE INDEX idx_conversation_statistics_assistant
    ON ConversationStatistics(assistantId);

CREATE INDEX idx_conversation_statistics_user_assistant
    ON ConversationStatistics(userId, assistantId);

CREATE INDEX idx_conversation_statistics_last_activity
    ON ConversationStatistics(lastActivity);


CREATE OR REPLACE VIEW DailySystemStatistics AS
SELECT
    DATE(occurredAt) AS day,
    SUM(actionType = 'UserLoggedIn') AS logins,
    SUM(actionType = 'ConversationStarted') AS conversations,
    SUM(actionType = 'MessageSent') AS messages
FROM UserAction
GROUP BY DATE(occurredAt);

CREATE OR REPLACE VIEW AssistantDailyUsage AS
SELECT
    DATE(ua.occurredAt) AS day,
    ua.assistantId,
    a.name AS name,
    COUNT(DISTINCT CASE
        WHEN ua.actionType = 'ConversationStarted'
        THEN ua.conversationId
    END) AS conversationCount,
    SUM(
        ua.actionType = 'MessageSent'
    ) AS messageCount,
    COUNT(DISTINCT CASE
        WHEN ua.actionType IN ('ConversationStarted', 'MessageSent')
        THEN ua.userId
    END) AS uniqueUsers
FROM UserAction ua
JOIN Assistant a
    ON a.id = ua.assistantId
WHERE ua.assistantId IS NOT NULL
GROUP BY
    DATE(ua.occurredAt),
    ua.assistantId,
    a.name;

CREATE OR REPLACE VIEW UserLoginSummary AS
SELECT
    u.id                                    AS userId,
    u.account                               AS account,
    COUNT(ua.id)                            AS totalLogins,
    MAX(ua.occurredAt)                      AS lastLogin,
    SUM(
        CASE
            WHEN ua.occurredAt >= NOW() - INTERVAL 7 DAY
            THEN 1 ELSE 0
        END
    )                                       AS loginsLastWeek,
    SUM(
        CASE
            WHEN ua.occurredAt >= NOW() - INTERVAL 30 DAY
            THEN 1 ELSE 0
        END
    )                                       AS loginsLastMonth,
    SUM(
        CASE
            WHEN ua.occurredAt >= NOW() - INTERVAL 365 DAY
            THEN 1 ELSE 0
        END
    )                                       AS loginsLastYear
FROM User u
LEFT JOIN UserAction ua
       ON ua.userId = u.id AND ua.actionType = 'UserLoggedIn'
GROUP BY
    u.id,
    u.account;


CREATE OR REPLACE VIEW UserActivitySummary AS
SELECT
    cs.userId,
    u.account,
    COUNT(*)                                    AS conversationCount,
    SUM(cs.messageCount)                         AS messageCount,
    ROUND(AVG(cs.messageCount),2)                AS averageMessages,
    MAX(cs.lastActivity)                         AS lastConversation,
    SUM(cs.completed IS NULL)                    AS openConversations,
    SUM(cs.completed IS NOT NULL)                AS completedConversations
FROM ConversationStatistics cs
JOIN User u
ON u.id = cs.userId
GROUP BY
    cs.userId,
    u.account;

CREATE OR REPLACE VIEW UserAssistantSummary AS
SELECT
    cs.userId,
    u.account,
    cs.assistantId,
    a.name                                     AS assistantName,
    COUNT(*)                                   AS conversationCount,
    SUM(cs.messageCount)                        AS messageCount,
    ROUND(AVG(cs.messageCount),2)               AS averageMessages,
    MAX(cs.lastActivity)                        AS lastUsed
FROM ConversationStatistics cs
JOIN Assistant a
ON a.id = cs.assistantId
JOIN User u
ON u.id = cs.userId
GROUP BY
    cs.userId,
    u.account,
    cs.assistantId,
    a.name;

CREATE OR REPLACE VIEW AssistantPopularity AS
SELECT
    a.id,
    a.name,
    COUNT(cs.conversationId) AS conversationCount,
    COALESCE(SUM(cs.messageCount), 0) AS messageCount,
    COUNT(DISTINCT cs.userId) AS uniqueUsers,
    ROUND(AVG(cs.messageCount), 2) AS averageMessages,
    MAX(cs.lastActivity) AS lastUsed
FROM Assistant a
LEFT JOIN ConversationStatistics cs
    ON cs.assistantId = a.id
GROUP BY
    a.id,
    a.name;

CREATE OR REPLACE VIEW SystemOverview AS
SELECT
    1                                                AS id,
    (SELECT COUNT(*) FROM User)                      AS users,
    (SELECT COUNT(*) FROM Assistant)                 AS assistants,
    (SELECT COUNT(*) FROM ConversationStatistics)    AS conversations,
    (SELECT COALESCE(SUM(messageCount),0)
        FROM ConversationStatistics)                 AS messages,
    (SELECT COUNT(*)
	    FROM UserAction
		WHERE actionType = 'UserLoggedIn')           AS logins,
    (SELECT COUNT(*) FROM WorkflowExecution)         AS workflowRuns,
    (SELECT COUNT(*) FROM LlmRequests)               AS llmRequests;

CREATE OR REPLACE VIEW AssistantLeaderboard AS
SELECT
    ROW_NUMBER() OVER (
        ORDER BY
            COUNT(cs.conversationId) DESC,
            COALESCE(SUM(cs.messageCount), 0) DESC
    ) AS ranking,
    a.id,
    a.name,
    COUNT(cs.conversationId) AS conversations,
    COALESCE(SUM(cs.messageCount), 0) AS messages,
    COUNT(DISTINCT cs.userId) AS users,
    ROUND(AVG(cs.messageCount), 2) AS averageConversationLength
FROM Assistant a
LEFT JOIN ConversationStatistics cs
    ON cs.assistantId = a.id
GROUP BY
    a.id,
    a.name;

CREATE OR REPLACE VIEW LlmDailyMetrics AS
SELECT
    DATE(r.createdAt) AS day,
    COUNT(*) AS requests,
    COALESCE(SUM(r.status = 'completed'), 0) AS completedRequests,
    COALESCE(SUM(r.status = 'failed'), 0) AS failedRequests,
    ROUND(AVG(m.queueWaitUs) / 1000, 2) AS avgQueueMs,
    ROUND(AVG(m.ttftUs) / 1000, 2) AS avgTTFTMs,
    ROUND(AVG(m.totalTimeUs) / 1000, 2) AS avgTotalMs,
    ROUND(MAX(m.queueWaitUs) / 1000, 2) AS maxQueueMs,
    ROUND(MAX(m.ttftUs) / 1000, 2) AS maxTTFTMs,
    ROUND(MAX(m.totalTimeUs) / 1000, 2) AS maxTotalMs,
    COALESCE(SUM(m.promptTokens), 0) AS promptTokens,
    COALESCE(SUM(m.completionTokens), 0) AS completionTokens,
    COALESCE(SUM(m.totalTokens), 0) AS totalTokens
FROM LlmRequests r
JOIN LlmRequestMetrics m
    ON m.id = r.id
GROUP BY
    DATE(r.createdAt);

CREATE OR REPLACE VIEW LlmHourlyMetrics AS
SELECT
    DATE(r.createdAt) AS day,
    HOUR(r.createdAt) AS hour,
    COUNT(*) AS requests,
    COALESCE(SUM(r.status = 'completed'), 0) AS completedRequests,
    COALESCE(SUM(r.status = 'failed'), 0) AS failedRequests,
    ROUND(AVG(m.ttftUs) / 1000, 2) AS avgTTFTMs,
    ROUND(AVG(m.totalTimeUs) / 1000, 2) AS avgTotalMs
FROM LlmRequests r
JOIN LlmRequestMetrics m
    ON r.id = m.id
GROUP BY
    DATE(r.createdAt),
    HOUR(r.createdAt);

CREATE OR REPLACE VIEW LlmUserMetrics AS
SELECT
    u.id,
    u.account,
    COUNT(r.id) AS requests,
    COALESCE(SUM(r.status = 'completed'), 0) AS completedRequests,
    COALESCE(SUM(r.status = 'failed'), 0) AS failedRequests,
    COALESCE(SUM(m.promptTokens), 0) AS promptTokens,
    COALESCE(SUM(m.completionTokens), 0) AS completionTokens,
    COALESCE(SUM(m.totalTokens), 0) AS totalTokens,
    ROUND(AVG(m.ttftUs) / 1000, 2) AS avgTTFTMs,
    ROUND(AVG(m.totalTimeUs) / 1000, 2) AS avgLatencyMs
FROM User u
LEFT JOIN LlmRequests r
    ON r.userId = u.id
LEFT JOIN LlmRequestMetrics m
    ON m.id = r.id
GROUP BY
    u.id,
    u.account;

CREATE OR REPLACE VIEW LlmAssistantMetrics AS
SELECT
    a.id,
    a.name,
    COUNT(r.id) AS requests,
    COALESCE(SUM(r.status = 'completed'), 0) AS completedRequests,
    COALESCE(SUM(r.status = 'failed'), 0) AS failedRequests,
    COALESCE(SUM(m.promptTokens), 0) AS promptTokens,
    COALESCE(SUM(m.completionTokens), 0) AS completionTokens,
    COALESCE(SUM(m.totalTokens), 0) AS totalTokens,
    ROUND(AVG(m.ttftUs) / 1000, 2) AS avgTTFTMs,
    ROUND(AVG(m.totalTimeUs) / 1000, 2) AS avgLatencyMs
FROM Assistant a
LEFT JOIN LlmRequests r
    ON r.assistantId = a.id
LEFT JOIN LlmRequestMetrics m
    ON m.id = r.id
GROUP BY
    a.id,
    a.name;

CREATE OR REPLACE VIEW LlmSystemOverview AS
SELECT
    1                                        AS id,
    COUNT(r.id)                              AS requests,
    COALESCE(SUM(r.status = 'completed'), 0) AS completedRequests,
    COALESCE(SUM(r.status = 'failed'), 0)    AS failedRequests,
    ROUND(AVG(m.ttftUs) / 1000, 2)           AS avgTTFTMs,
    ROUND(AVG(m.totalTimeUs) / 1000, 2)      AS avgLatencyMs,
    COALESCE(SUM(m.totalTokens), 0)          AS totalTokens
FROM LlmRequests r
LEFT JOIN LlmRequestMetrics m
    ON m.id = r.id;

-- -------------------------------------------------------------
-- USER ACTIONS
-- -------------------------------------------------------------

CREATE TABLE UserAction
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    userId UUID NOT NULL,
    actionType VARCHAR(50) NOT NULL,
	conversationId UUID NULL,
    assistantId UUID NULL,
    workflowId UUID NULL,
    occurredAt DATETIME(6) NOT NULL,
	metadata JSON NULL,
    PRIMARY KEY (id),
    INDEX ix_UserAction_User_Time
        (userId, occurredAt),
    INDEX ix_UserAction_Type_Time
        (actionType, occurredAt),
    INDEX ix_UserAction_Conversation
        (conversationId),
    INDEX ix_UserAction_Assistant
        (assistantId),
    INDEX ix_UserAction_Workflow
        (workflowId)
);

CREATE OR REPLACE VIEW UserActionSummary AS
SELECT
    ua.userId,
    COUNT(CASE
        WHEN ua.actionType = 'AssistantCreated'
        THEN 1
    END) AS assistantCreationCount,
    COUNT(CASE
        WHEN ua.actionType = 'WorkflowCreated'
        THEN 1
    END) AS workflowCreationCount,
    COUNT(CASE
        WHEN ua.actionType = 'WorkflowExecuted'
        THEN 1
    END) AS workflowExecutionCount,
    MAX(CASE
        WHEN ua.actionType = 'AssistantCreated'
        THEN ua.occurredAt
    END) AS lastAssistantCreation,
    MAX(CASE
        WHEN ua.actionType = 'WorkflowCreated'
        THEN ua.occurredAt
    END) AS lastWorkflowCreation,
    MAX(CASE
        WHEN ua.actionType = 'WorkflowExecuted'
        THEN ua.occurredAt
    END) AS lastWorkflowExecution
FROM UserAction ua
GROUP BY ua.userId;

CREATE OR REPLACE VIEW UserWorkflowSummary AS
SELECT
    ua.userId,
    ua.workflowId,
    w.name AS workflowName,
    COUNT(CASE
        WHEN ua.actionType = 'WorkflowCreated'
        THEN 1
    END) AS creationCount,
    COUNT(CASE
        WHEN ua.actionType = 'WorkflowExecuted'
        THEN 1
    END) AS executionCount,
    MIN(CASE
        WHEN ua.actionType = 'WorkflowCreated'
        THEN ua.occurredAt
    END) AS createdAt,
    MAX(CASE
        WHEN ua.actionType = 'WorkflowExecuted'
        THEN ua.occurredAt
    END) AS lastExecutedAt
FROM UserAction ua
LEFT JOIN Workflow w
    ON w.id = ua.workflowId
WHERE ua.workflowId IS NOT NULL
GROUP BY
    ua.userId,
    ua.workflowId,
    w.name;

CREATE OR REPLACE VIEW WorkflowPopularity AS
SELECT
    ua.workflowId,
    w.name AS workflowName,
    COUNT(DISTINCT CASE
        WHEN ua.actionType = 'WorkflowCreated'
        THEN ua.userId
    END) AS creators,
    COUNT(CASE
        WHEN ua.actionType = 'WorkflowCreated'
        THEN 1
    END) AS creationCount,
    COUNT(DISTINCT CASE
        WHEN ua.actionType = 'WorkflowExecuted'
        THEN ua.userId
    END) AS uniqueUsers,
    COUNT(CASE
        WHEN ua.actionType = 'WorkflowExecuted'
        THEN 1
    END) AS executionCount,
    MIN(CASE
        WHEN ua.actionType = 'WorkflowCreated'
        THEN ua.occurredAt
    END) AS firstCreatedAt,
    MAX(CASE
        WHEN ua.actionType = 'WorkflowExecuted'
        THEN ua.occurredAt
    END) AS lastExecutedAt
FROM UserAction ua
LEFT JOIN Workflow w
    ON w.id = ua.workflowId
WHERE ua.workflowId IS NOT NULL
GROUP BY
    ua.workflowId,
    w.name;

-- =============================================================
-- USERS & PERMISSIONS
-- =============================================================

CREATE USER IF NOT EXISTS 'vectorUser'@'%' IDENTIFIED BY 'Password123';
GRANT ALL PRIVILEGES ON *.* TO 'vectorUser'@'%' WITH GRANT OPTION;

CREATE USER IF NOT EXISTS 'MintyUser'@'%' IDENTIFIED BY 'hothamcakes';
GRANT ALL PRIVILEGES ON *.* TO 'MintyUser'@'%' WITH GRANT OPTION;

FLUSH PRIVILEGES;