CREATE TABLE IF NOT EXISTS outbox (
  id UUID PRIMARY KEY,
  aggregate_type VARCHAR(100) NOT NULL,
  aggregate_id VARCHAR(100) NOT NULL,
  event_type VARCHAR(100) NOT NULL,
  payload JSONB NOT NULL, 
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  processed BOOLEAN NOT NULL DEFAULT FALSE,
  attempts INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  processed_at TIMESTAMP WITH TIME ZONE,
  last_error TEXT
);

CREATE INDEX idx_outbox_polling 
ON outbox (created_at) 
WHERE status = 'PENDING' OR (status = 'FAILED' AND attempts < 5);
