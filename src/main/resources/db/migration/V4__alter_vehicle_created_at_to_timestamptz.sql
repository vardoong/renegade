ALTER TABLE vehicle
    ALTER COLUMN created_at
    TYPE TIMESTAMPTZ
    USING created_at AT TIME ZONE 'Asia/Seoul';