CREATE TABLE karma_history (
  id            BIGINT             AUTO_INCREMENT,

  canonicalName VARCHAR   NOT NULL,
  timestamp     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),

  delta         INT       NOT NULL,
  actor         VARCHAR   NULL     DEFAULT NULL,
  comment       VARCHAR   NULL     DEFAULT NULL,
);

-- INSERT INTO karma_history (canonicalName, delta, comment)
--   SELECT
--     canonicalName,
--     karma,
--     'Imported from old schema'
-- FROM karma;