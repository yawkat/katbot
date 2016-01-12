CREATE TABLE roles (
  username VARCHAR,
  host     VARCHAR,
  role     VARCHAR,
  PRIMARY KEY (username, host, role)
);
INSERT INTO roles (username, host, role) VALUES ('yawkat', 'cats.coffee', 'ADMIN');
