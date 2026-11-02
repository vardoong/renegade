ALTER TABLE telemetry
    ALTER COLUMN timestamp
        TYPE TIMESTAMPTZ
        USING timestamp AT TIME ZONE 'Asia/Seoul',
    ALTER COLUMN created_at
        TYPE TIMESTAMPTZ
        USING created_at AT TIME ZONE 'Asia/Seoul';