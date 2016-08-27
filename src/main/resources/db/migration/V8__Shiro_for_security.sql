CREATE LOCAL TEMPORARY TABLE roles_migr (
  username VARCHAR,
  host     VARCHAR,
  role     VARCHAR,
  PRIMARY KEY (username, host, role)
);

INSERT INTO roles_migr (username, host, role)
  SELECT
    username,
    host,
    role
  FROM roles;
DROP TABLE roles;

CREATE TABLE roles (
  role VARCHAR NOT NULL,
  PRIMARY KEY (role)
);

CREATE TABLE user_roles (
  username VARCHAR NOT NULL,
  host     VARCHAR NOT NULL,
  role     VARCHAR NOT NULL,
  PRIMARY KEY (username, host, role),
  FOREIGN KEY (role) REFERENCES roles ON DELETE CASCADE
);

CREATE TABLE role_permissions (
  role       VARCHAR NOT NULL,
  server     VARCHAR NOT NULL DEFAULT '',
  channel    VARCHAR NOT NULL DEFAULT '',
  permission VARCHAR NOT NULL,
  PRIMARY KEY (role, permission, server, channel),
  FOREIGN KEY (role) REFERENCES roles ON DELETE CASCADE
);

INSERT INTO roles (role) VALUES
  ('ADD_FACTOIDS'),
  ('DELETE_FACTOIDS'),
  ('EDIT_INTERACT'),
  ('EDIT_WOSCH'),
  ('EDIT_MARKOV'),
  ('IGNORE_THROTTLE'),
  ('IGNORE_RESTRICT'),
  ('INVITE'),
  ('ADMIN'),
  ('DEFAULT');
INSERT INTO user_roles (username, host, role) SELECT username, host, role
                                              FROM roles_migr;
INSERT INTO role_permissions (role, permission) VALUES
  ('ADD_FACTOIDS', 'addFactoids'),
  ('DELETE_FACTOIDS', 'deleteFactoids'),
  ('EDIT_INTERACT', 'editInteract'),
  ('EDIT_WOSCH', 'editWosch'),
  ('EDIT_MARKOV', 'editMarkov'),
  ('IGNORE_THROTTLE', 'ignoreThrottle'),
  ('IGNORE_RESTRICT', 'ignoreRestrict'),
  ('INVITE', 'invite'),
  ('ADMIN', 'addFactoids'),
  ('ADMIN', 'deleteFactoids'),
  ('ADMIN', 'editInteract'),
  ('ADMIN', 'editWosch'),
  ('ADMIN', 'editMarkov'),
  ('ADMIN', 'ignoreThrottle'),
  ('ADMIN', 'ignoreRestrict'),
  ('ADMIN', 'invite'),
  ('ADMIN', 'admin');

DROP TABLE roles_migr;
