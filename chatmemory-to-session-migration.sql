-- Step 1: one AI_SESSION row per conversation that has chat memory, aged/expiry-adjusted.
INSERT INTO AI_SESSION (id, user_id, created_at, expires_at, metadata, event_version)
SELECT
    scm.conversation_id,
    CAST(c.ownerId AS CHAR),
    MIN(scm.timestamp),                             -- Conversation has no createdAt column;
                                                    -- earliest message timestamp is the best proxy.
    DATE_ADD(MIN(scm.timestamp), INTERVAL 10000 DAY),
    NULL,
    COUNT(*)
FROM SPRING_AI_CHAT_MEMORY scm
INNER JOIN Conversation c
    ON c.id = CAST(scm.conversation_id AS CHAR CHARACTER SET utf8mb4)
WHERE c.ownerId IS NOT NULL                         -- AI_SESSION.user_id is NOT NULL; can't port these
GROUP BY scm.conversation_id, c.ownerId
ON DUPLICATE KEY UPDATE id = id;                    -- makes the script safely re-runnable

-- Step 2: one AI_SESSION_EVENT row per old message, in original order.
INSERT INTO AI_SESSION_EVENT
    (`id`, `session_id`, `timestamp`, `message_type`, `message_content`, `message_data`, `synthetic`, `archived`, `branch`, `metadata`)
SELECT
    CONCAT(scm.conversation_id, ':', scm.sequence_id),
    scm.conversation_id,
    scm.`timestamp`,
    scm.type,
    scm.content,
    NULL,
    0,
    0,
    NULL,
    NULL
FROM SPRING_AI_CHAT_MEMORY scm
INNER JOIN AI_SESSION s ON s.id = scm.conversation_id
ORDER BY scm.conversation_id, scm.sequence_id
ON DUPLICATE KEY UPDATE AI_SESSION_EVENT.`id` = AI_SESSION_EVENT.`id`;