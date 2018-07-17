CREATE TABLE AIAI_IDS (
  SEQUENCE_NAME       VARCHAR(50),
  SEQUENCE_NEXT_VALUE DECIMAL(10)
);

CREATE UNIQUE INDEX AIAI_IDS_SEQUENCE_NAME_IDX
  ON AIAI_IDS (SEQUENCE_NAME);

CREATE UNIQUE INDEX AIAI_IDS_SEQUENCE_NAME_NEXT_VAL
  ON AIAI_IDS
  (SEQUENCE_NAME, SEQUENCE_NEXT_VALUE);

CREATE TABLE AIAI_LP_STATION (
  ID          NUMERIC(10, 0) NOT NULL,
  VERSION     NUMERIC(5, 0)  NOT NULL,
  IP          VARCHAR(30),
  UPDATE_TS   TIMESTAMP DEFAULT 0 ON UPDATE CURRENT_TIMESTAMP,
  DESCRIPTION VARCHAR(250)
);

CREATE TABLE AIAI_LOG_DATA (
  ID          INT(10) NOT NULL AUTO_INCREMENT  PRIMARY KEY,
  REF_ID      NUMERIC(10, 0) NOT NULL,
  VERSION     NUMERIC(5, 0)  NOT NULL,
  UPDATE_TS   TIMESTAMP DEFAULT 0 ON UPDATE CURRENT_TIMESTAMP,
  LOG_TYPE    NUMERIC(5, 0)  NOT NULL,
  LOG_DATA    MEDIUMTEXT not null
);

CREATE TABLE AIAI_LP_DATASET (
  ID          NUMERIC(10, 0) NOT NULL,
  VERSION     NUMERIC(5, 0)  NOT NULL,
  DESCRIPTION VARCHAR(250),
  IS_EDITABLE   tinyint(1) not null default 1,
  CMD_ASSEMBLE         VARCHAR(250),
  DATASET_FILE         VARCHAR(250)
);

CREATE TABLE AIAI_LP_DATASET_GROUP (
  ID          NUMERIC(10, 0) NOT NULL,
  DATASET_ID  NUMERIC(10, 0) NOT NULL,
  VERSION     NUMERIC(5, 0)  NOT NULL,
  GROUP_NUMBER  NUMERIC(3, 0) NOT NULL,
  DESCRIPTION VARCHAR(250),
  CMD         VARCHAR(250),
  IS_ID_GROUP tinyint(1) not null default 0,
  IS_FEATURE  tinyint(1) not null default 0,
  IS_LABEL    tinyint(1) not null default 0
  FEATURE_FILE         VARCHAR(250),
  IS_META    tinyint(1) not null default 0,
  STATUS     tinyint(1) not null default 0
);

CREATE TABLE AIAI_LP_DATASET_COLUMN (
  ID          NUMERIC(10, 0) NOT NULL,
  DATASET_GROUP_ID          NUMERIC(10, 0) NOT NULL,
  VERSION     NUMERIC(5, 0)  NOT NULL,
  NAME    VARCHAR(50),
  DESCRIPTION VARCHAR(250)
);

CREATE TABLE AIAI_LP_DATASET_PATH (
  ID          NUMERIC(10, 0) NOT NULL,
  DATASET_ID  NUMERIC(10, 0) NOT NULL,
  VERSION     NUMERIC(5, 0)  NOT NULL,
  PATH_NUMBER NUMERIC(3, 0) NOT NULL,
  PATH        VARCHAR(200),
  REG_TS      TIMESTAMP NOT NULL,
  CHECKSUM    VARCHAR(200),
  IS_FILE     tinyint(1) not null default 1,
  IS_VALID    tinyint(1) not null default 0
);

CREATE TABLE AIAI_LP_EXPERIMENT (
  ID          INT(10) NOT NULL AUTO_INCREMENT  PRIMARY KEY,
  VERSION     NUMERIC(5, 0)  NOT NULL,
  NAME        VARCHAR(50)   NOT NULL,
  DESCRIPTION VARCHAR(250)  NOT NULL,
  EPOCH       VARCHAR(100)  NOT NULL,
  EPOCH_VARIANT tinyint(1),
  SEED          INT(10)
);

CREATE TABLE AIAI_S_ENV (
  ID          NUMERIC(10, 0) NOT NULL,
  VERSION     NUMERIC(5, 0)  NOT NULL,
  NAME        VARCHAR(50),
  DESCRIPTION VARCHAR(250)
);
