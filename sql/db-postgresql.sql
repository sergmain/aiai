CREATE TABLE AIAI_LP_STATION (
  ID          SERIAL PRIMARY KEY,
  VERSION     NUMERIC(5, 0)  NOT NULL,
  IP          VARCHAR(30),
  UPDATE_TS   TIMESTAMP DEFAULT to_timestamp(0),
  DESCRIPTION VARCHAR(250),
  ENV       TEXT
);

CREATE TABLE AIAI_LOG_DATA (
  ID          SERIAL PRIMARY KEY,
  REF_ID      NUMERIC(10, 0) NOT NULL,
  VERSION     NUMERIC(5, 0)  NOT NULL,
  UPDATE_TS   TIMESTAMP DEFAULT to_timestamp(0),
  LOG_TYPE    NUMERIC(5, 0)  NOT NULL,
  LOG_DATA    TEXT not null
);

CREATE TABLE AIAI_LP_DATASET (
  ID          SERIAL PRIMARY KEY,
  VERSION     NUMERIC(5, 0)  NOT NULL,
  NAME        VARCHAR(40)  NOT NULL,
  DESCRIPTION VARCHAR(250) NOT NULL,
  IS_EDITABLE   BOOLEAN not null default true,
  ASSEMBLY_SNIPPET_ID  NUMERIC(10, 0),
  DATASET_SNIPPET_ID  NUMERIC(10, 0),
  IS_LOCKED   BOOLEAN not null default false,
  RAW_ASSEMBLING_STATUS   smallint not null default 0,
  DATASET_PRODUCING_STATUS   smallint not null default 0,
  dataset        BYTEA
);

CREATE TABLE AIAI_LP_DATASET_GROUP (
  ID          SERIAL PRIMARY KEY,
  DATASET_ID  NUMERIC(10, 0) NOT NULL,
  VERSION     NUMERIC(5, 0)  NOT NULL,
  GROUP_NUMBER  NUMERIC(3, 0) NOT NULL,
  DESCRIPTION VARCHAR(250),
  CMD         VARCHAR(250),
  SNIPPET_ID  NUMERIC(10, 0),
  IS_ID_GROUP BOOLEAN not null default false,
  IS_FEATURE  BOOLEAN not null default false,
  IS_LABEL    BOOLEAN not null default false,
  IS_REQUIRED BOOLEAN not null default false,
  FEATURE_FILE         VARCHAR(250),
  STATUS     smallint not null default 0,
  feature        BYTEA
);

CREATE TABLE AIAI_LP_EXPERIMENT (
  ID          SERIAL PRIMARY KEY,
  VERSION     NUMERIC(5, 0)  NOT NULL,
  DATASET_ID  NUMERIC(10, 0),
  NAME        VARCHAR(50)   NOT NULL,
  DESCRIPTION VARCHAR(250)  NOT NULL,
  EPOCH       VARCHAR(100)  NOT NULL,
  EPOCH_VARIANT smallint  NOT NULL,
  SEED          integer,
  NUMBER_OF_SEQUENCE          integer not null default 0,
  IS_ALL_SEQUENCE_PRODUCED   BOOLEAN not null default false,
  IS_FEATURE_PRODUCED   BOOLEAN not null default false,
  IS_LAUNCHED   BOOLEAN not null default false,
  EXEC_STATE        smallint not null default 0,
  CREATED_ON   bigint not null,
  LAUNCHED_ON   bigint
);

CREATE TABLE AIAI_LP_EXPERIMENT_HYPER_PARAMS (
  ID          SERIAL PRIMARY KEY,
  EXPERIMENT_ID          NUMERIC(10, 0) NOT NULL,
  VERSION     NUMERIC(5, 0)  NOT NULL,
  HYPER_PARAM_KEY    VARCHAR(50),
  HYPER_PARAM_VALUES  VARCHAR(250)
);

CREATE TABLE AIAI_LP_EXPERIMENT_FEATURE (
  ID          SERIAL PRIMARY KEY,
  EXPERIMENT_ID          NUMERIC(10, 0) NOT NULL,
  VERSION     NUMERIC(5, 0)  NOT NULL,
  FEATURE_IDS   VARCHAR(512) not null,
  IS_IN_PROGRESS    BOOLEAN not null default false,
  IS_FINISHED   BOOLEAN not null default false,
  EXEC_STATUS  smallint not null default 0
);

CREATE UNIQUE INDEX AIAI_LP_EXPERIMENT_FEATURE_UNQ_IDX
  ON AIAI_LP_EXPERIMENT_FEATURE (EXPERIMENT_ID, FEATURE_IDS);


CREATE TABLE AIAI_LP_EXPERIMENT_SNIPPET (
  ID          SERIAL PRIMARY KEY,
  EXPERIMENT_ID          NUMERIC(10, 0) NOT NULL,
  VERSION     NUMERIC(5, 0)  NOT NULL,
  SNIPPET_CODE   VARCHAR(100) NOT NULL,
  SNIPPET_TYPE   VARCHAR(20) not null,
  SNIPPET_ORDER  NUMERIC(3, 0) NOT NULL  default 0
);

CREATE UNIQUE INDEX AIAI_LP_EXPERIMENT_SNIPPET_UNQ_IDX
  ON AIAI_LP_EXPERIMENT_SNIPPET (EXPERIMENT_ID, SNIPPET_ORDER);

CREATE TABLE AIAI_LP_EXPERIMENT_SEQUENCE (
  ID            SERIAL PRIMARY KEY,
  EXPERIMENT_ID          NUMERIC(10, 0) NOT NULL,
  FEATURE_ID          NUMERIC(10, 0) NOT NULL,
  VERSION       NUMERIC(5, 0)  NOT NULL,
  PARAMS          TEXT not null,
  STATION_ID          NUMERIC(10, 0),
  ASSIGNED_ON   bigint,
  IS_COMPLETED  BOOLEAN not null default false,
  COMPLETED_ON   bigint,
  SNIPPET_EXEC_RESULTS  TEXT,
  METRICS      TEXT,
  IS_ALL_SNIPPETS_OK  BOOLEAN not null default false
);

CREATE TABLE AIAI_LP_SNIPPET (
  ID          SERIAL PRIMARY KEY,
  VERSION     NUMERIC(5, 0)  NOT NULL,
  NAME      VARCHAR(50) not null,
  SNIPPET_TYPE      VARCHAR(50) not null,
  SNIPPET_VERSION   VARCHAR(20) not null,
  FILENAME  VARCHAR(250) not null,
  CODE        BYTEA not null,
  CHECKSUM    VARCHAR(2048),
  IS_SIGNED   BOOLEAN not null default false,
  ENV         VARCHAR(50) not null,
  PARAMS         VARCHAR(1000),
  CODE_LENGTH integer not null
);

CREATE UNIQUE INDEX AIAI_LP_SNIPPET_UNQ_IDX
  ON AIAI_LP_SNIPPET (NAME, SNIPPET_VERSION);

CREATE TABLE AIAI_LP_DATASET_PATH (
  ID          SERIAL PRIMARY KEY,
  DATASET_ID  NUMERIC(10, 0) NOT NULL,
  VERSION     NUMERIC(5, 0)  NOT NULL,
  PATH_NUMBER NUMERIC(3, 0) NOT NULL,
  PATH        VARCHAR(200),
  REG_TS      TIMESTAMP NOT NULL,
  CHECKSUM    VARCHAR(2048),
  IS_FILE     BOOLEAN not null default true,
  IS_VALID    BOOLEAN not null default false
);

CREATE TABLE AIAI_LP_ENV (
  ID          SERIAL PRIMARY KEY,
  VERSION     NUMERIC(5, 0)  NOT NULL,
  ENV_KEY     VARCHAR(50)  NOT NULL,
  ENV_VALUE   VARCHAR(500)  NOT NULL,
  SIGNATURE   varchar(1000)
);

CREATE UNIQUE INDEX AIAI_LP_ENV_UNQ_IDX
  ON AIAI_LP_ENV (ENV_KEY);
