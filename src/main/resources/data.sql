-- Seed data for Service Request Processing System

INSERT INTO customers (id, name, email, created_at)
VALUES 
    (1, 'Ravi Kumar', 'ravi.kumar@example.com', CURRENT_TIMESTAMP),
    (2, 'Priya Sharma', 'priya.sharma@example.com', CURRENT_TIMESTAMP),
    (3, 'Amit Patel', 'amit.patel@example.com', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO service_requests (id, request_number, customer_id, category, description, priority, status, assigned_to, created_at, updated_at)
VALUES 
    (1, 'SR-1001', 1, 'Account Access', 'Unable to access account after password reset', 'HIGH', 'OPEN', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'SR-1002', 2, 'Billing Issue', 'Double charge on subscription payment', 'MEDIUM', 'ASSIGNED', 'agent_john', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
