create table mh_ids
(
    ID      bigint NOT NULL PRIMARY KEY,
    STUB    varchar(1) null
);

create table mh_gen_ids
(
    SEQUENCE_NAME       varchar(50) not null,
    SEQUENCE_NEXT_VALUE bigint  NOT NULL
);

CREATE UNIQUE INDEX mh_gen_ids_sequence_name_unq_idx
    ON mh_gen_ids (SEQUENCE_NAME);

CREATE TABLE mh_dispatcher
(
    ID          bigint generated by default as identity (start with 1) PRIMARY KEY,
    VERSION     NUMERIC(5, 0) NOT NULL,
    CODE        VARCHAR(50)   NOT NULL,
    PARAMS      LONGTEXT null
);

CREATE UNIQUE INDEX mh_dispatcher_code_unq_idx
    ON mh_dispatcher (CODE);

CREATE TABLE mh_company
(
    ID          bigint generated by default as identity (start with 1) PRIMARY KEY,
    VERSION     NUMERIC(5, 0) NOT NULL,
    UNIQUE_ID   NUMERIC(10, 0) not null,
    NAME        VARCHAR(50)   NOT NULL,
    PARAMS      LONGTEXT null
);

CREATE UNIQUE INDEX mh_company_unique_id_unq_idx
    ON mh_company (UNIQUE_ID);

insert into mh_company
(version, UNIQUE_ID, name, params)
VALUES
(0, 1, 'master company', '');

-- !!! this insert must be executed after creating 'master company'
insert into mh_gen_ids
(SEQUENCE_NAME, SEQUENCE_NEXT_VALUE)
select 'mh_ids', max(UNIQUE_ID) from mh_company;

create table mh_account
(
    ID                  bigint generated by default as identity (start with 1) PRIMARY KEY,
    VERSION             NUMERIC(5, 0) NOT NULL,
    COMPANY_ID          NUMERIC(10, 0) NOT NULL,
    USERNAME            varchar(30)   not null,
    PASSWORD            varchar(100)  not null,
    ROLES               varchar(100),
    PUBLIC_NAME         varchar(100),

    is_acc_not_expired  BOOLEAN not null default true,
    is_not_locked       BOOLEAN not null default false,
    is_cred_not_expired BOOLEAN not null default false,
    is_enabled          BOOLEAN not null default false,

    mail_address        varchar(100),
    PHONE               varchar(100),
    PHONE_AS_STR        varchar(100),

    CREATED_ON          bigint        not null,
    UPDATED_ON          bigint not null,
    SECRET_KEY          varchar(25),
    TWO_FA              BOOLEAN not null default false
);

CREATE INDEX mh_account_company_id_idx
    ON mh_account (COMPANY_ID);

CREATE UNIQUE INDEX mh_account_username_unq_idx
    ON mh_account (USERNAME);

CREATE TABLE mh_processor
(
    ID          bigint generated by default as identity (start with 1) PRIMARY KEY,
    VERSION     NUMERIC(10, 0) NOT NULL,
    UPDATED_ON  bigint         not null,
    IP          VARCHAR(30),
    DESCRIPTION VARCHAR(250),
    STATUS      LONGTEXT           not null
);

CREATE TABLE mh_log_data
(
    ID        bigint generated by default as identity (start with 1) PRIMARY KEY,
    REF_ID    NUMERIC(10, 0) NOT NULL,
    VERSION   NUMERIC(5, 0)  NOT NULL,
    UPDATE_TS TIMESTAMP DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    LOG_TYPE  NUMERIC(5, 0)  NOT NULL,
    LOG_DATA  MEDIUMTEXT     not null
);

CREATE TABLE mh_variable
(
    ID          bigint generated by default as identity (start with 1) PRIMARY KEY,
    VERSION     NUMERIC(5, 0)  NOT NULL,
    IS_INITED   BOOLEAN not null default false,
    NAME        VARCHAR(250)  not null,
    TASK_CONTEXT_ID  VARCHAR(250)  not null,
    EXEC_CONTEXT_ID NUMERIC(10, 0) not null,
    UPLOAD_TS   TIMESTAMP     NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    DATA        LONGBLOB,
    FILENAME    VARCHAR(150),
    PARAMS      MEDIUMTEXT    not null
);

CREATE INDEX mh_variable_exec_context_id_idx
    ON mh_variable (EXEC_CONTEXT_ID);

CREATE INDEX mh_variable_name_idx
    ON mh_variable (NAME);

CREATE UNIQUE INDEX mh_variable_name_all_context_ids_unq_idx
    ON mh_variable (NAME, TASK_CONTEXT_ID, EXEC_CONTEXT_ID);

-- its name is VARIABLE_GLOBAL, not GLOBAL_VARIABLE because I want these tables to be in the same spot in scheme
CREATE TABLE mh_variable_global
(
    ID          bigint generated by default as identity (start with 1) PRIMARY KEY,
    VERSION     NUMERIC(5, 0)  NOT NULL,
    NAME        VARCHAR(250)  not null,
    UPLOAD_TS   TIMESTAMP     NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    DATA        LONGBLOB,
    FILENAME    VARCHAR(150),
    PARAMS      MEDIUMTEXT    not null
);

CREATE UNIQUE INDEX mh_variable_global_name_unq_idx
    ON mh_variable_global (NAME);

CREATE TABLE mh_function_data
(
    ID              bigint generated by default as identity (start with 1) PRIMARY KEY,
    VERSION         NUMERIC(5, 0)  NOT NULL,
    FUNCTION_CODE    VARCHAR(100) not null,
    UPLOAD_TS       TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    DATA            LONGBLOB,
    PARAMS          MEDIUMTEXT not null
);

CREATE UNIQUE INDEX mmh_function_data_function_code_idx
    ON mh_function_data (FUNCTION_CODE);

CREATE TABLE mh_experiment
(
    ID          bigint generated by default as identity (start with 1) PRIMARY KEY,
    VERSION     NUMERIC(5, 0) NOT NULL,
    EXEC_CONTEXT_ID NUMERIC(10, 0),
    CODE        VARCHAR(255)   NOT NULL,
    PARAMS      MEDIUMTEXT    not null
);

CREATE UNIQUE INDEX mh_experiment_code_unq_idx
    ON mh_experiment (CODE);

CREATE TABLE mh_task
(
    ID                           bigint generated by default as identity (start with 1) PRIMARY KEY,
    VERSION                      NUMERIC(5, 0)  NOT NULL,
    PARAMS                       MEDIUMTEXT     not null,
    PROCESSOR_ID                   NUMERIC(10, 0),
    ASSIGNED_ON                  bigint,
    IS_COMPLETED                 tinyint(1)     not null default 0,
    COMPLETED_ON                 bigint,
    FUNCTION_EXEC_RESULTS        MEDIUMTEXT,
    EXEC_CONTEXT_ID              NUMERIC(10, 0) NOT NULL,
    EXEC_STATE                   tinyint(1)     not null default 0,
    IS_RESULT_RECEIVED           tinyint(1)     not null default 0,
    RESULT_RESOURCE_SCHEDULED_ON bigint
);

CREATE INDEX mh_task_exec_context_id_idx
    ON mh_task (EXEC_CONTEXT_ID);

CREATE TABLE mh_function
(
    ID              bigint generated by default as identity (start with 1) PRIMARY KEY,
    VERSION         NUMERIC(5, 0) NOT NULL,
    FUNCTION_CODE    VARCHAR(100)  not null,
    FUNCTION_TYPE    VARCHAR(50)   not null,
    PARAMS          MEDIUMTEXT    not null
);

CREATE UNIQUE INDEX mh_function_function_code_unq_idx
    ON mh_function (FUNCTION_CODE);

CREATE TABLE mh_source_code
(
    ID              bigint generated by default as identity (start with 1) PRIMARY KEY,
    VERSION         NUMERIC(5, 0) NOT NULL,
    COMPANY_ID      NUMERIC(10, 0) NOT NULL,
    UID             varchar(50) NOT NULL,
    CREATED_ON      bigint        NOT NULL,
    PARAMS          LONGTEXT          not null,
    IS_LOCKED       BOOLEAN not null default false,
    IS_VALID        BOOLEAN not null default false
);

CREATE UNIQUE INDEX mh_source_code_uid_unq_idx
    ON mh_source_code (UID);

CREATE TABLE mh_exec_context
(
    ID                   bigint generated by default as identity (start with 1) PRIMARY KEY,
    VERSION              NUMERIC(5, 0)  NOT NULL,
    SOURCE_CODE_ID       NUMERIC(10, 0) NOT NULL,
    COMPANY_ID           NUMERIC(10, 0) NOT NULL,
    CREATED_ON           bigint         NOT NULL,
    COMPLETED_ON         bigint,
    PARAMS               LONGTEXT       NOT NULL,
    IS_VALID             BOOLEAN        default false not null,
    STATE                smallint       not null default 0
);

CREATE TABLE mh_experiment_result
(
    ID              bigint generated by default as identity (start with 1) PRIMARY KEY,
    VERSION         NUMERIC(5, 0) NOT NULL,
    COMPANY_ID      NUMERIC(10, 0) NOT NULL,
    NAME            VARCHAR(50)   NOT NULL,
    DESCRIPTION     VARCHAR(250)  NOT NULL,
    CODE            VARCHAR(50)   NOT NULL,
    CREATED_ON      bigint        not null,
    EXPERIMENT      LONGTEXT      NOT NULL
);

CREATE TABLE mh_experiment_task
(
    ID                      bigint generated by default as identity (start with 1) PRIMARY KEY,
    VERSION                 NUMERIC(5, 0)  NOT NULL,
    EXPERIMENT_RESULT_ID    NUMERIC(10, 0) NOT NULL,
    TASK_ID                 NUMERIC(10, 0) NOT NULL,
    PARAMS                  MEDIUMTEXT     not null
);

CREATE INDEX mh_experiment_task_experiment_result_id_idx
    ON mh_experiment_task (EXPERIMENT_RESULT_ID);

CREATE UNIQUE INDEX mh_experiment_task_experiment_result_id_task_id_idx
    ON mh_experiment_task (EXPERIMENT_RESULT_ID, TASK_ID);

create table mh_batch
(
    ID         bigint generated by default as identity (start with 1) PRIMARY KEY,
    VERSION    NUMERIC(5, 0)  NOT NULL,
    COMPANY_ID          NUMERIC(10, 0) NOT NULL,
    ACCOUNT_ID          NUMERIC(10, 0) NOT NULL,
    SOURCE_CODE_ID         NUMERIC(10, 0) NOT NULL,
    EXEC_CONTEXT_ID     NUMERIC(10, 0),
    DATA_ID    NUMERIC(10, 0),
    CREATED_ON bigint         NOT NULL,
    EXEC_STATE tinyint(1)     not null default 0,
    PARAMS     MEDIUMTEXT,
    IS_DELETED BOOLEAN not null default false
);

CREATE INDEX mh_batch_exec_context_id_idx
    ON mh_batch (EXEC_CONTEXT_ID);

CREATE TABLE mh_event
(
    ID          bigint generated by default as identity (start with 1) PRIMARY KEY,
    VERSION     NUMERIC(5, 0)  NOT NULL,
    -- company_id can be null
    COMPANY_ID          NUMERIC(10, 0) NULL,
    CREATED_ON      BIGINT NOT NULL,
    PERIOD          INT not null ,
    EVENT           VARCHAR(50)     not null,
    PARAMS          MEDIUMTEXT      not null
);

CREATE INDEX mh_event_period_idx
    ON mh_event (PERIOD);

