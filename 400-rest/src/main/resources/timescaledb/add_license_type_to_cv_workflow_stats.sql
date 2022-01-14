BEGIN;

ALTER TABLE VERIFICATION_WORKFLOW_STATS ADD COLUMN IF NOT EXISTS LICENSE_TYPE VARCHAR(20);
ALTER TABLE VERIFICATION_WORKFLOW_STATS ADD COLUMN IF NOT EXISTS ACCOUNT_NAME TEXT;

COMMIT;

BEGIN;

CREATE INDEX IF NOT EXISTS VWS_LICENSE_TYPE_INDEX ON VERIFICATION_WORKFLOW_STATS(LICENSE_TYPE, END_TIME DESC);
CREATE INDEX IF NOT EXISTS VWS_ACCOUNT_NAME_INDEX ON VERIFICATION_WORKFLOW_STATS(ACCOUNT_NAME, END_TIME DESC);

COMMIT;