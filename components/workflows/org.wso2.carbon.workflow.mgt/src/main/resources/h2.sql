--Todo: This is the schema for the workflow feature. Need to move it to the feature

CREATE TABLE IF NOT EXISTS WF_REQUESTS (
            UUID VARCHAR (45),
            CREATED_AT TIMESTAMP,
            UPDATED_AT TIMESTAMP,
            REQUEST BLOB,
            STATUS VARCHAR (15),
            PRIMARY KEY (UUID)
);

CREATE TABLE IF NOT EXISTS WF_WS_SERVICES (
            ALIAS VARCHAR (45),
            WS_ACTION VARCHAR(100),
            SERVICE_EP VARCHAR(100),
            CONDITION VARCHAR(1024),
            PRIORITY INTEGER DEFAULT 100,
            USERNAME VARCHAR(30),
            PASSWORD VARCHAR(30)
            PRIMARY KEY (UUID)
);