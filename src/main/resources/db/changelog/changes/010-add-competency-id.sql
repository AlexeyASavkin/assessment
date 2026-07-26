ALTER TABLE employees ADD COLUMN IF NOT EXISTS competency_id uuid;
ALTER TABLE employees DROP CONSTRAINT IF EXISTS fk_employee_competency;
ALTER TABLE employees ADD CONSTRAINT fk_employee_competency
  FOREIGN KEY (competency_id) REFERENCES competencies(id);
