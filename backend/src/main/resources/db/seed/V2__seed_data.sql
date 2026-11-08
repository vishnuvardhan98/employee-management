-- Demo data for local and Docker runs (MySQL only; tests load just db/migration).
-- All people below are fictional.

INSERT INTO departments (name) VALUES
    ('Engineering'), ('Design'), ('People'), ('Marketing'), ('Finance'), ('Operations');

-- Passwords: admin / Admin@123  (ADMIN, full access)
--            viewer / Viewer@123 (USER, read-only employee directory)
INSERT INTO users (username, password_hash, full_name, role) VALUES
    ('admin',  '$2a$10$mbaR07UY4KU6JbjAAbVdqeLNMLzAKwvIRI6RSA9QjkF/Ig4Tll0Ga', 'Alex Morgan', 'ADMIN'),
    ('viewer', '$2a$10$RsFqFk85qYB.YOpg419BRuTHtpUfBQGmKmEH.vfNV//5bePaRp4H2', 'Riley Brooks', 'USER');

-- Join dates are relative to the first migration run so the report's default
-- "current month" range always has data to show.
SET @month_start = DATE_FORMAT(CURDATE(), '%Y-%m-01');

INSERT INTO employees (name, email, phone, title, department_id, status, joined_on)
SELECT p.name, p.email, p.phone, p.title, d.id, p.status, p.joined_on
FROM (
    SELECT 'Olivia Chen' AS name, 'olivia.chen@example.com' AS email, '202-555-0100' AS phone, 'Frontend Developer' AS title, 'Engineering' AS dept, 'ACTIVE' AS status, LEAST(CURDATE(), @month_start) AS joined_on
    UNION ALL SELECT 'James Wilson', 'james.wilson@example.com', '202-555-0101', 'Product Designer', 'Design', 'ACTIVE', LEAST(CURDATE(), @month_start + INTERVAL 1 DAY)
    UNION ALL SELECT 'Ava Patel', 'ava.patel@example.com', '202-555-0102', 'People Partner', 'People', 'ACTIVE', LEAST(CURDATE(), @month_start + INTERVAL 2 DAY)
    UNION ALL SELECT 'Noah Williams', 'noah.williams@example.com', '202-555-0103', 'Content Strategist', 'Marketing', 'ON_LEAVE', LEAST(CURDATE(), @month_start + INTERVAL 3 DAY)
    UNION ALL SELECT 'Sophia Garcia', 'sophia.garcia@example.com', '202-555-0104', 'Financial Analyst', 'Finance', 'ACTIVE', LEAST(CURDATE(), @month_start + INTERVAL 4 DAY)
    UNION ALL SELECT 'Liam Anderson', 'liam.anderson@example.com', '202-555-0105', 'Backend Developer', 'Engineering', 'ACTIVE', LEAST(CURDATE(), @month_start + INTERVAL 5 DAY)
    UNION ALL SELECT 'Isabella Thomas', 'isabella.thomas@example.com', '202-555-0106', 'UX Researcher', 'Design', 'ACTIVE', CURDATE() - INTERVAL 40 DAY
    UNION ALL SELECT 'Ethan Martinez', 'ethan.martinez@example.com', '202-555-0107', 'QA Engineer', 'Engineering', 'ACTIVE', CURDATE() - INTERVAL 55 DAY
    UNION ALL SELECT 'Mia Robinson', 'mia.robinson@example.com', '202-555-0108', 'Marketing Manager', 'Marketing', 'ACTIVE', CURDATE() - INTERVAL 75 DAY
    UNION ALL SELECT 'Lucas Taylor', 'lucas.taylor@example.com', '202-555-0109', 'Recruiter', 'People', 'ON_LEAVE', CURDATE() - INTERVAL 98 DAY
    UNION ALL SELECT 'Amelia Moore', 'amelia.moore@example.com', '202-555-0110', 'Accountant', 'Finance', 'ACTIVE', CURDATE() - INTERVAL 120 DAY
    UNION ALL SELECT 'Benjamin Lee', 'benjamin.lee@example.com', '202-555-0111', 'DevOps Engineer', 'Engineering', 'ACTIVE', CURDATE() - INTERVAL 150 DAY
    UNION ALL SELECT 'Charlotte Davis', 'charlotte.davis@example.com', '202-555-0112', 'Design Lead', 'Design', 'ACTIVE', CURDATE() - INTERVAL 180 DAY
    UNION ALL SELECT 'Henry Jackson', 'henry.jackson@example.com', '202-555-0113', 'SEO Specialist', 'Marketing', 'ACTIVE', CURDATE() - INTERVAL 210 DAY
    UNION ALL SELECT 'Harper White', 'harper.white@example.com', '202-555-0114', 'HR Coordinator', 'People', 'INACTIVE', CURDATE() - INTERVAL 240 DAY
    UNION ALL SELECT 'Alexander Harris', 'alexander.harris@example.com', '202-555-0115', 'Engineering Manager', 'Engineering', 'ACTIVE', CURDATE() - INTERVAL 270 DAY
    UNION ALL SELECT 'Evelyn Clark', 'evelyn.clark@example.com', '202-555-0116', 'Finance Manager', 'Finance', 'ACTIVE', CURDATE() - INTERVAL 300 DAY
    UNION ALL SELECT 'Daniel Lewis', 'daniel.lewis@example.com', '202-555-0117', 'Visual Designer', 'Design', 'ACTIVE', CURDATE() - INTERVAL 330 DAY
    UNION ALL SELECT 'Grace Walker', 'grace.walker@example.com', '202-555-0118', 'Operations Lead', 'Operations', 'ACTIVE', CURDATE() - INTERVAL 20 DAY
    UNION ALL SELECT 'Samuel Young', 'samuel.young@example.com', '202-555-0119', 'Data Engineer', 'Engineering', 'ACTIVE', CURDATE() - INTERVAL 35 DAY
    UNION ALL SELECT 'Chloe King', 'chloe.king@example.com', '202-555-0120', 'Brand Designer', 'Design', 'ON_LEAVE', CURDATE() - INTERVAL 62 DAY
    UNION ALL SELECT 'Jack Wright', 'jack.wright@example.com', '202-555-0121', 'Payroll Specialist', 'Finance', 'ACTIVE', CURDATE() - INTERVAL 88 DAY
    UNION ALL SELECT 'Lily Scott', 'lily.scott@example.com', '202-555-0122', 'Social Media Manager', 'Marketing', 'INACTIVE', CURDATE() - INTERVAL 365 DAY
    UNION ALL SELECT 'Owen Green', 'owen.green@example.com', '202-555-0123', 'Logistics Coordinator', 'Operations', 'ACTIVE', CURDATE() - INTERVAL 400 DAY
    UNION ALL SELECT 'Zoe Baker', 'zoe.baker@example.com', '202-555-0124', 'Mobile Developer', 'Engineering', 'ACTIVE', CURDATE() - INTERVAL 450 DAY
) AS p
JOIN departments d ON d.name = p.dept;
