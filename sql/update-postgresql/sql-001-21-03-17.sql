truncate table mh_exec_context;

truncate table mh_variable;

truncate table mh_task;

alter table mh_exec_context
    add CTX_GRAPH_ID      NUMERIC(10, 0) NOT NULL;

alter table mh_exec_context
    add CTX_TASK_STATE_ID NUMERIC(10, 0) NOT NULL;

alter table mh_exec_context
    add CTX_VARIABLE_STATE_ID NUMERIC(10, 0) NOT NULL;

CREATE TABLE mh_exec_context_graph
(
    ID                SERIAL PRIMARY KEY,
    VERSION           NUMERIC(5, 0)  NOT NULL,
    EXEC_CONTEXT_ID   NUMERIC(10, 0) default NULL,
    PARAMS            TEXT NOT NULL
);

CREATE TABLE mh_exec_context_task_state
(
    ID                SERIAL PRIMARY KEY,
    VERSION           NUMERIC(5, 0)  NOT NULL,
    EXEC_CONTEXT_ID   NUMERIC(10, 0) default NULL,
    PARAMS            TEXT NOT NULL
);

CREATE TABLE mh_exec_context_variable_state
(
    ID                SERIAL PRIMARY KEY,
    VERSION           NUMERIC(5, 0)  NOT NULL,
    EXEC_CONTEXT_ID   NUMERIC(10, 0) default NULL,
    PARAMS            TEXT NOT NULL
);

