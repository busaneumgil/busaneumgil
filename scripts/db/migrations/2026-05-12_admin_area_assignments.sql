CREATE TABLE IF NOT EXISTS admin_area_assignments (
  assignment_id BIGSERIAL PRIMARY KEY,
  gu VARCHAR(50) NOT NULL,
  dong VARCHAR(50) NOT NULL,
  assignee_user_id UUID REFERENCES users(user_id),
  status VARCHAR(30) NOT NULL DEFAULT 'NOT_STARTED',
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now(),
  CONSTRAINT uk_admin_area_assignments_area UNIQUE (gu, dong)
);

CREATE INDEX IF NOT EXISTS idx_admin_area_assignments_assignee
  ON admin_area_assignments(assignee_user_id);

CREATE INDEX IF NOT EXISTS idx_admin_area_assignments_status
  ON admin_area_assignments(status);
