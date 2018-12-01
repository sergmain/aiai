CREATE TABLE AIAI_LP_STATION (
  ID          INT(10) NOT NULL AUTO_INCREMENT  PRIMARY KEY,
  VERSION     NUMERIC(5, 0)  NOT NULL,
  IP          VARCHAR(30),
  UPDATE_TS   TIMESTAMP DEFAULT 0 ON UPDATE CURRENT_TIMESTAMP,
  DESCRIPTION VARCHAR(250),
  ENV       MEDIUMTEXT,
  ACTIVE_TIME VARCHAR(250)
);

CREATE TABLE AIAI_LOG_DATA (
  ID          INT(10) NOT NULL AUTO_INCREMENT  PRIMARY KEY,
  REF_ID      NUMERIC(10, 0) NOT NULL,
  VERSION     NUMERIC(5, 0)  NOT NULL,
  UPDATE_TS   TIMESTAMP DEFAULT 0 ON UPDATE CURRENT_TIMESTAMP,
  LOG_TYPE    NUMERIC(5, 0)  NOT NULL,
  LOG_DATA    MEDIUMTEXT not null
);

CREATE TABLE AIAI_LP_DATA (
  ID          INT(10) NOT NULL AUTO_INCREMENT  PRIMARY KEY,
  CODE        VARCHAR(100),
  POOL_CODE   VARCHAR(250),
  DATA_TYPE   NUMERIC(2, 0) NOT NULL,
  VERSION     NUMERIC(5, 0) NOT NULL,
  FLOW_INSTANCE_ID  NUMERIC(10, 0),
  UPLOAD_TS   TIMESTAMP DEFAULT 0 ON UPDATE CURRENT_TIMESTAMP,
  DATA        LONGBLOB,
  CHECKSUM    VARCHAR(2048),
  IS_VALID    tinyint(1) not null default 0,
  IS_MANUAL   tinyint(1) not null default 0,
  FILENAME    VARCHAR(150)
);

CREATE TABLE AIAI_LP_EXPERIMENT (
  ID          INT(10) NOT NULL AUTO_INCREMENT  PRIMARY KEY,
  VERSION     NUMERIC(5, 0)  NOT NULL,
  FLOW_INSTANCE_ID  NUMERIC(10, 0),
  NAME        VARCHAR(50)   NOT NULL,
  DESCRIPTION VARCHAR(250)  NOT NULL,
  CODE        VARCHAR(50)   NOT NULL,
  EPOCH       VARCHAR(100)  NOT NULL,
  EPOCH_VARIANT tinyint(1)  NOT NULL,
  SEED          INT(10),
  NUMBER_OF_TASK          INT(10) not null default 0,
  IS_ALL_TASK_PRODUCED   tinyint(1) not null default 0,
  IS_FEATURE_PRODUCED   tinyint(1) not null default 0,
  CREATED_ON   bigint not null
);

CREATE TABLE AIAI_LP_EXPERIMENT_HYPER_PARAMS (
  ID          INT(10) NOT NULL AUTO_INCREMENT  PRIMARY KEY,
  EXPERIMENT_ID          NUMERIC(10, 0) NOT NULL,
  VERSION     NUMERIC(5, 0)  NOT NULL,
  HYPER_PARAM_KEY    VARCHAR(50),
  HYPER_PARAM_VALUES  VARCHAR(250)
);

CREATE TABLE AIAI_LP_EXPERIMENT_FEATURE (
  ID          INT(10) NOT NULL AUTO_INCREMENT  PRIMARY KEY,
  EXPERIMENT_ID          NUMERIC(10, 0) NOT NULL,
  VERSION     NUMERIC(5, 0)  NOT NULL,
  RESOURCE_CODES   VARCHAR(2048) not null,
  CHECKSUM_ID_CODES   VARCHAR(100) not null,
  EXEC_STATUS  tinyint(1) not null default 0
);

CREATE UNIQUE INDEX AIAI_LP_EXPERIMENT_FEATURE_UNQ_IDX
  ON AIAI_LP_EXPERIMENT_FEATURE (EXPERIMENT_ID, CHECKSUM_ID_CODES);

CREATE TABLE AIAI_LP_EXPERIMENT_SNIPPET (
  ID          INT(10) NOT NULL AUTO_INCREMENT  PRIMARY KEY,
  EXPERIMENT_ID          NUMERIC(10, 0) NOT NULL,
  VERSION     NUMERIC(5, 0)  NOT NULL,
  SNIPPET_CODE   VARCHAR(100) NOT NULL,
  SNIPPET_TYPE   VARCHAR(20) not null
);

CREATE INDEX AIAI_LP_EXPERIMENT_SNIPPET_EXPERIMENT_ID_IDX
  ON AIAI_LP_EXPERIMENT_SNIPPET (EXPERIMENT_ID);

CREATE TABLE AIAI_LP_TASK (
  ID            INT(10) NOT NULL AUTO_INCREMENT  PRIMARY KEY,
  VERSION       NUMERIC(5, 0)  NOT NULL,
  PARAMS          MEDIUMTEXT not null,
  STATION_ID          NUMERIC(10, 0),
  ASSIGNED_ON    bigint,
  IS_COMPLETED   tinyint(1) not null default 0,
  COMPLETED_ON   bigint,
  SNIPPET_EXEC_RESULTS  MEDIUMTEXT,
  METRICS      MEDIUMTEXT,
  TASK_ORDER   smallint not null,
  FLOW_INSTANCE_ID          NUMERIC(10, 0)   NOT NULL,
  EXEC_STATE        tinyint(1) not null default 0,
  IS_RESULT_RECEIVED  tinyint(1) not null default 0,
  RESULT_RESOURCE_SCHEDULED_ON bigint,
  PROCESS_TYPE tinyint(1) not null
);

CREATE TABLE AIAI_LP_TASK_FEATURE (
  ID            INT(10) NOT NULL AUTO_INCREMENT  PRIMARY KEY,
  VERSION       NUMERIC(5, 0)  NOT NULL,
  FLOW_INSTANCE_ID       NUMERIC(10, 0)   NOT NULL,
  TASK_ID       NUMERIC(10, 0)   NOT NULL,
  FEATURE_ID    NUMERIC(10, 0)   NOT NULL
);

CREATE TABLE AIAI_LP_SNIPPET (
  ID          INT(10) NOT NULL AUTO_INCREMENT  PRIMARY KEY,
  VERSION     NUMERIC(5, 0)  NOT NULL,
  NAME      VARCHAR(50) not null,
  SNIPPET_TYPE      VARCHAR(50) not null,
  SNIPPET_VERSION   VARCHAR(20) not null,
  FILENAME  VARCHAR(250),
  CHECKSUM    VARCHAR(2048),
  IS_SIGNED   tinyint(1) not null default 0,
  IS_REPORT_METRICS   tinyint(1) not null default 0,
  ENV         VARCHAR(50) not null,
  PARAMS         VARCHAR(1000),
  CODE_LENGTH integer not null,
  IS_FILE_PROVIDED   tinyint(1) not null default 0
);

CREATE UNIQUE INDEX AIAI_LP_SNIPPET_UNQ_IDX
  ON AIAI_LP_SNIPPET (NAME, SNIPPET_VERSION);
);

CREATE TABLE AIAI_LP_FLOW (
  ID            INT(10) NOT NULL AUTO_INCREMENT  PRIMARY KEY,
  VERSION       NUMERIC(5, 0)  NOT NULL,
  CODE      varchar(50)  NOT NULL,
  CREATED_ON    bigint NOT NULL,
  PARAMS        TEXT not null,
  IS_LOCKED      BOOLEAN not null default false,
  IS_VALID      BOOLEAN not null default false
);

CREATE TABLE AIAI_LP_FLOW_INSTANCE (
  ID            INT(10) NOT NULL AUTO_INCREMENT  PRIMARY KEY,
  VERSION       NUMERIC(5, 0)  NOT NULL,
  FLOW_ID       NUMERIC(10, 0) NOT NULL,
  CREATED_ON    bigint NOT NULL,
  COMPLETED_ON  bigint,
  INPUT_POOL_CODE  varchar(50) NOT NULL,
  PRODUCING_ORDER integer not null NOT NULL,
  IS_VALID      BOOLEAN not null default false,
  EXEC_STATE   smallint not null default 0
);
