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

CREATE TABLE IF NOT EXISTS UserAssistantLinks (
    userId      UUID NOT NULL,
    assistantId UUID NOT NULL,
    PRIMARY KEY (userId, assistantId),
    FOREIGN KEY (userId)      REFERENCES User(id)      ON DELETE CASCADE,
    FOREIGN KEY (assistantId) REFERENCES Assistant(id) ON DELETE CASCADE
);

-- =============================================================
-- CONVERSATION & MEMORY
-- =============================================================

CREATE TABLE IF NOT EXISTS Conversation (
    title                  TEXT,
    conversationId         UUID NOT NULL,
    ownerId                UUID,
    associatedAssistantId  UUID,
    associatedWorkflow     TEXT,
    lastUsed               TIMESTAMP,
    PRIMARY KEY (conversationId)
);

CREATE TABLE IF NOT EXISTS SPRING_AI_CHAT_MEMORY (
    conversation_id TEXT,
    content         TEXT,
    type            TEXT,
    timestamp       TIMESTAMP
);

-- =============================================================
-- DOCUMENTS & TAGS
-- =============================================================

CREATE TABLE IF NOT EXISTS Document (
    documentId UUID NOT NULL,
    title      TEXT,
    state      INTEGER,
    ownerId    UUID,
    PRIMARY KEY (documentId)
);

CREATE TABLE IF NOT EXISTS DocumentAssistantLinks (
    assistantId UUID NOT NULL,
    documentId  UUID NOT NULL,
    PRIMARY KEY (assistantId, documentId),
    FOREIGN KEY (assistantId) REFERENCES Assistant(id),
    FOREIGN KEY (documentId)  REFERENCES Document(documentId)
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
    FOREIGN KEY (documentId) REFERENCES Document(documentId)
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

CREATE TABLE IF NOT EXISTS UserWorkflowLinks (
    userId     UUID NOT NULL,
    workflowId UUID NOT NULL,
    PRIMARY KEY (userId, workflowId),
    FOREIGN KEY (userId)     REFERENCES User(id)     ON DELETE CASCADE,
    FOREIGN KEY (workflowId) REFERENCES Workflow(id) ON DELETE CASCADE
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
    fileType  ENUM('code', 'markdown', 'text', 'diagram', 'json') NULL,
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

CREATE TABLE IF NOT EXISTS UserSkillsLinks (
    userId  UUID NOT NULL,
    skillId UUID NOT NULL,
    PRIMARY KEY (userId, skillId),
    FOREIGN KEY (userId)  REFERENCES User(id)   ON DELETE CASCADE,
    FOREIGN KEY (skillId) REFERENCES Skills(id) ON DELETE CASCADE
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
        ON UPDATE CASCADE
);

CREATE INDEX idx_requests_userId         ON LlmRequests (userId);
CREATE INDEX idx_requests_conversationId ON LlmRequests (conversationId);
CREATE INDEX idx_requests_status         ON LlmRequests (status);
CREATE INDEX idx_requests_createdAt      ON LlmRequests (createdAt);

-- -------------------------------------------------------------

CREATE TABLE IF NOT EXISTS LlmRequestMetrics (
    id                UUID            NOT NULL,
    queueWaitUs       BIGINT UNSIGNED NULL,
    ttftUs            BIGINT UNSIGNED NULL,
    totalTimeUs       BIGINT UNSIGNED NULL,
    promptTokens      INT UNSIGNED    NULL,
    completionTokens  INT UNSIGNED    NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_metrics_request
        FOREIGN KEY (id) REFERENCES LlmRequests(id)
        ON DELETE CASCADE
);

-- =============================================================
-- TRIGGERS
-- =============================================================

DELIMITER $$

CREATE TRIGGER trg_computeMetrics
AFTER UPDATE ON LlmRequests
FOR EACH ROW
BEGIN
    IF NEW.status = 'completed' AND OLD.status <> 'completed' THEN
        INSERT INTO LlmRequestMetrics (
            id,
            queueWaitUs,
            ttftUs,
            totalTimeUs
        )
        VALUES (
            NEW.id,
            TIMESTAMPDIFF(MICROSECOND, NEW.queuedAt,  NEW.dequeuedAt),
            TIMESTAMPDIFF(MICROSECOND, NEW.dequeuedAt, NEW.firstTokenAt),
            TIMESTAMPDIFF(MICROSECOND, NEW.createdAt,  NEW.completedAt)
        )
        ON DUPLICATE KEY UPDATE
            queueWaitUs = VALUES(queueWaitUs),
            ttftUs      = VALUES(ttftUs),
            totalTimeUs = VALUES(totalTimeUs);
    END IF;
END$$

DELIMITER ;

-- =============================================================
-- VIEWS
-- =============================================================

CREATE OR REPLACE VIEW DailyLlmMetrics AS
SELECT
    DATE(r.createdAt)   AS day,
    COUNT(*)            AS totalRequests,
    -- Queue wait
    ROUND(AVG(m.queueWaitUs) / 1000, 2) AS avgQueueMs,
    ROUND(PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY m.queueWaitUs)
        OVER (PARTITION BY DATE(r.createdAt)) / 1000, 2) AS p50QueueMs,
    ROUND(PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY m.queueWaitUs)
        OVER (PARTITION BY DATE(r.createdAt)) / 1000, 2) AS p95QueueMs,
    -- TTFT
    ROUND(AVG(m.ttftUs) / 1000, 2) AS avgTtftMs,
    ROUND(PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY m.ttftUs)
        OVER (PARTITION BY DATE(r.createdAt)) / 1000, 2) AS p50TtftMs,
    ROUND(PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY m.ttftUs)
        OVER (PARTITION BY DATE(r.createdAt)) / 1000, 2) AS p95TtftMs,
    -- Total time
    ROUND(AVG(m.totalTimeUs) / 1000, 2) AS avgTotalMs,
    ROUND(PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY m.totalTimeUs)
        OVER (PARTITION BY DATE(r.createdAt)) / 1000, 2) AS p50TotalMs,
    ROUND(PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY m.totalTimeUs)
        OVER (PARTITION BY DATE(r.createdAt)) / 1000, 2) AS p95TotalMs
FROM LlmRequests r
JOIN LlmRequestMetrics m ON m.id = r.id
WHERE r.status = 'completed'
GROUP BY day, m.queueWaitUs, m.ttftUs, m.totalTimeUs;

-- -------------------------------------------------------------

CREATE OR REPLACE VIEW DailyLlmMetricsCompat AS
SELECT
    DATE(r.createdAt)                    AS day,
    COUNT(*)                             AS total_requests,
    ROUND(AVG(m.queueWaitUs)  / 1000, 2) AS avgQueueMs,
    ROUND(AVG(m.ttftUs)       / 1000, 2) AS avgTtftMs,
    ROUND(AVG(m.totalTimeUs)  / 1000, 2) AS avgTotalMs,
    ROUND(MAX(m.queueWaitUs)  / 1000, 2) AS maxQueueMs,
    ROUND(MAX(m.ttftUs)       / 1000, 2) AS maxTtftMs,
    ROUND(MAX(m.totalTimeUs)  / 1000, 2) AS maxTotalMs
FROM LlmRequests r
JOIN LlmRequestMetrics m ON m.id = r.id
WHERE r.status = 'completed'
GROUP BY DATE(r.createdAt);

-- =============================================================
-- USERS & PERMISSIONS
-- =============================================================

CREATE USER IF NOT EXISTS 'vectorUser'@'%' IDENTIFIED BY 'Password123';
GRANT ALL PRIVILEGES ON *.* TO 'vectorUser'@'%' WITH GRANT OPTION;

CREATE USER IF NOT EXISTS 'MintyUser'@'%' IDENTIFIED BY 'hothamcakes';
GRANT ALL PRIVILEGES ON *.* TO 'MintyUser'@'%' WITH GRANT OPTION;

FLUSH PRIVILEGES;