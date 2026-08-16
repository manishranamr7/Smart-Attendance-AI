-- Seed Enrollment Data for Identity Resolution Testing

MERGE INTO enrollments (user_id, name, face_template, enrollment_date, status) KEY(user_id) VALUES 
('USR_1001', 'John Doe', 'FT_JOHNDOE_V1', CURRENT_TIMESTAMP - INTERVAL '10' DAY, 'ACTIVE'),
('USR_1002', 'Jane Smith', 'FT_JANESMITH_V1', CURRENT_TIMESTAMP - INTERVAL '5' DAY, 'ACTIVE'),
('USR_1003', 'Alex Johnson (Expired Window)', 'FT_ALEXJ_V1', CURRENT_TIMESTAMP - INTERVAL '45' DAY, 'EXPIRED_WINDOW'),
('USR_1004', 'Sarah Connor', 'FT_SARAHC_V1', CURRENT_TIMESTAMP - INTERVAL '2' DAY, 'ACTIVE');
