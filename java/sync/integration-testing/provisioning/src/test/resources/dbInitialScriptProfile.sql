CREATE DOMAIN batch_status AS VARCHAR CHECK VALUE IN ('IDLE', 'RUNNING', 'ERROR', 'SUCCESS');
CREATE DOMAIN batch_entity_type AS VARCHAR CHECK VALUE IN ('GROUP', 'USER');
CREATE DOMAIN http_verb AS VARCHAR CHECK VALUE IN ('PUT', 'PATCH', 'GET', 'POST', 'DELETE');

CREATE TABLE domain (
    domain_id integer PRIMARY KEY AUTO_INCREMENT,
    domain_timeupdate timestamp,
    domain_timecreate timestamp,
    domain_usercreate integer,
    domain_userupdate integer,
    domain_label character varying(32) NOT NULL,
    domain_description character varying(255),
    domain_name character varying(128),
    domain_alias text,
    domain_global boolean,
    domain_uuid character(36) NOT NULL
);

CREATE TABLE batch
(
  id integer PRIMARY KEY AUTO_INCREMENT,
  status batch_status NOT NULL,
  timecreate timestamp NOT NULL DEFAULT NOW(),
  timecommit timestamp,
  domain integer NOT NULL,
  CONSTRAINT batch_batch_domain_id_fkey FOREIGN KEY (domain)
      REFERENCES domain (domain_id)
      ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE batch_operation
(
  id integer PRIMARY KEY AUTO_INCREMENT,
  status batch_status NOT NULL,
  timecreate timestamp NOT NULL DEFAULT NOW(),
  timecommit timestamp,
  error text,
  url text NOT NULL,
  body text,
  verb http_verb NOT NULL,
  entity_type batch_entity_type NOT NULL,
  batch integer NOT NULL,
  CONSTRAINT batch_operation_batch_fkey FOREIGN KEY (batch)
      REFERENCES batch (id)
      ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE batch_operation_param
(
  id integer PRIMARY KEY AUTO_INCREMENT,
  key text NOT NULL,
  value text NOT NULL,
  operation integer NOT NULL,
  CONSTRAINT batch_operation_param_operation_fkey FOREIGN KEY (operation)
      REFERENCES batch_operation (id)
      ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE profile (
    profile_id integer PRIMARY KEY AUTO_INCREMENT,
    profile_domain_id integer NOT NULL,
    profile_timeupdate timestamp,
    profile_timecreate timestamp,
    profile_userupdate integer,
    profile_usercreate integer,
    profile_name character varying(64)
);

CREATE TABLE domainentity (
    domainentity_entity_id integer NOT NULL,
    domainentity_domain_id integer NOT NULL
);

CREATE TABLE serviceproperty (
    serviceproperty_id integer PRIMARY KEY AUTO_INCREMENT,
    serviceproperty_service character varying(255) NOT NULL,
    serviceproperty_property character varying(255) NOT NULL,
    serviceproperty_entity_id integer NOT NULL,
    serviceproperty_value text
);

CREATE TABLE host (
    host_id integer PRIMARY KEY AUTO_INCREMENT,
    host_domain_id integer NOT NULL,
    host_timeupdate timestamp,
    host_timecreate timestamp DEFAULT now(),
    host_userupdate integer,
    host_usercreate integer,
    host_uid integer,
    host_gid integer,
    host_archive smallint DEFAULT 0 NOT NULL,
    host_name character varying(32) NOT NULL,
    host_fqdn character varying(255),
    host_ip character varying(16),
    host_delegation character varying(256) DEFAULT '',
    host_description character varying(128)
);


INSERT INTO domain (domain_name, domain_uuid, domain_label) VALUES ('test.tlse.lng', 
'ac21bc0c-f816-4c52-8bb9-e50cfbfec5b6', 'test.tlse.lng');
INSERT INTO domain (domain_name, domain_uuid, domain_label) VALUES ('test2.tlse.lng', '3a2ba641-4ae0-4b40-aa5e-c3fd3acb78bf', 'test2.tlse.lng');
INSERT INTO domain (domain_name, domain_uuid, domain_label) VALUES ('test3.tlse.lng', '68936f0f-2bb5-447c-87f5-efcd46f58122', 'test3.tlse.lng');

INSERT INTO profile (profile_domain_id, profile_name) VALUES (1, 'admin');
INSERT INTO profile (profile_domain_id, profile_name) VALUES (1, 'user');
INSERT INTO profile (profile_domain_id, profile_name) VALUES (2, 'editor');