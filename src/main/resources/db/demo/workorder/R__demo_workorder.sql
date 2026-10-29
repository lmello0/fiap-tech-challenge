INSERT INTO work_order (id, number, status, customer_id, customer_name, customer_document, customer_email, vehicle_id, vehicle_plate, vehicle_brand, vehicle_model, vehicle_year, complaint, created_at, updated_at) VALUES
    ('0198a000-0000-7000-8000-000000000501', 'OS-DEMO-000001', 'RECEIVED', '0198a000-0000-7000-8000-000000000001', 'Carlos Customer', '52998224725', 'customer@workshop.local',
     '0198a000-0000-7000-8000-000000000101', 'ABC1D23', 'Fiat', 'Argo', 2022, 'Periodic maintenance', now() - interval '2 hours', now() - interval '2 hours'),
    ('0198a000-0000-7000-8000-000000000502', 'OS-DEMO-000002', 'WAITING_APPROVAL', '0198a000-0000-7000-8000-000000000001', 'Carlos Customer', '52998224725', 'customer@workshop.local',
     '0198a000-0000-7000-8000-000000000102', 'XYZ9A87', 'Volkswagen', 'Polo', 2021, 'Noise when braking', now() - interval '1 day', now() - interval '20 hours'),
    ('0198a000-0000-7000-8000-000000000503', 'OS-DEMO-000003', 'DELIVERED', '0198a000-0000-7000-8000-000000000002', 'Marina Souza', '11144477735', 'marina.souza@example.com',
     '0198a000-0000-7000-8000-000000000103', 'QWE4R56', 'Chevrolet', 'Onix', 2020, 'Oil change', now() - interval '10 days', now() - interval '9 days')
ON CONFLICT (id) DO NOTHING;

INSERT INTO work_order_item (id, work_order_id, position, type, reference_id, name, unit_price, quantity) VALUES
    ('0198a000-0000-7000-8000-000000000601', '0198a000-0000-7000-8000-000000000501', 0, 'SERVICE', '0198a000-0000-7000-8000-000000000201', 'Oil change', 150.00, 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO budget (id, work_order_id, sequence, kind, status, previous_budget_id, total, issued_at, decided_at, decided_by, decided_role, decision_channel) VALUES
    ('0198a000-0000-7000-8000-000000000701', '0198a000-0000-7000-8000-000000000502', 1, 'INITIAL', 'PENDING', NULL, 300.00, now() - interval '20 hours', NULL, NULL, NULL, NULL),
    ('0198a000-0000-7000-8000-000000000702', '0198a000-0000-7000-8000-000000000503', 1, 'INITIAL', 'APPROVED', NULL, 185.00, now() - interval '10 days' + interval '1 hour',
     now() - interval '10 days' + interval '2 hours', 'demo-customer', 'CUSTOMER', 'APP')
ON CONFLICT (id) DO NOTHING;

INSERT INTO budget_line (id, budget_id, position, type, reference_id, name, unit_price, quantity) VALUES
    ('0198a000-0000-7000-8000-000000000801', '0198a000-0000-7000-8000-000000000701', 0, 'SERVICE', '0198a000-0000-7000-8000-000000000203', 'Brake pad replacement', 180.00, 1),
    ('0198a000-0000-7000-8000-000000000802', '0198a000-0000-7000-8000-000000000701', 1, 'PART', '0198a000-0000-7000-8000-000000000302', 'Front brake pads', 120.00, 1),
    ('0198a000-0000-7000-8000-000000000803', '0198a000-0000-7000-8000-000000000702', 0, 'SERVICE', '0198a000-0000-7000-8000-000000000201', 'Oil change', 150.00, 1),
    ('0198a000-0000-7000-8000-000000000804', '0198a000-0000-7000-8000-000000000702', 1, 'PART', '0198a000-0000-7000-8000-000000000301', 'Oil filter', 35.00, 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO work_order_status_history (id, work_order_id, from_status, to_status, occurred_at, actor_subject, actor_role, note) VALUES
    ('0198a000-0000-7000-8000-000000000901', '0198a000-0000-7000-8000-000000000501', NULL, 'RECEIVED', now() - interval '2 hours', 'demo-attendant', 'ATTENDANT', 'Work order opened'),
    ('0198a000-0000-7000-8000-000000000902', '0198a000-0000-7000-8000-000000000502', NULL, 'RECEIVED', now() - interval '1 day', 'demo-attendant', 'ATTENDANT', 'Work order opened'),
    ('0198a000-0000-7000-8000-000000000903', '0198a000-0000-7000-8000-000000000502', 'RECEIVED', 'IN_DIAGNOSIS', now() - interval '23 hours', 'demo-mechanic', 'MECHANIC', 'Diagnosis started'),
    ('0198a000-0000-7000-8000-000000000904', '0198a000-0000-7000-8000-000000000502', 'IN_DIAGNOSIS', 'WAITING_APPROVAL', now() - interval '20 hours', 'demo-mechanic', 'MECHANIC', 'Budget 1 issued'),
    ('0198a000-0000-7000-8000-000000000905', '0198a000-0000-7000-8000-000000000503', NULL, 'RECEIVED', now() - interval '10 days', 'demo-attendant', 'ATTENDANT', 'Work order opened'),
    ('0198a000-0000-7000-8000-000000000906', '0198a000-0000-7000-8000-000000000503', 'RECEIVED', 'IN_DIAGNOSIS', now() - interval '10 days' + interval '10 minutes', 'demo-mechanic', 'MECHANIC', 'Diagnosis started'),
    ('0198a000-0000-7000-8000-000000000907', '0198a000-0000-7000-8000-000000000503', 'IN_DIAGNOSIS', 'WAITING_APPROVAL', now() - interval '10 days' + interval '1 hour', 'demo-mechanic', 'MECHANIC', 'Budget 1 issued'),
    ('0198a000-0000-7000-8000-000000000908', '0198a000-0000-7000-8000-000000000503', 'WAITING_APPROVAL', 'IN_EXECUTION', now() - interval '10 days' + interval '2 hours', 'demo-customer', 'CUSTOMER', 'Budget 1 approved'),
    ('0198a000-0000-7000-8000-000000000909', '0198a000-0000-7000-8000-000000000503', 'IN_EXECUTION', 'COMPLETED', now() - interval '10 days' + interval '2 hours 45 minutes', 'demo-mechanic', 'MECHANIC', 'Service completed'),
    ('0198a000-0000-7000-8000-000000000910', '0198a000-0000-7000-8000-000000000503', 'COMPLETED', 'DELIVERED', now() - interval '9 days', 'demo-attendant', 'ATTENDANT', 'Vehicle delivered')
ON CONFLICT (id) DO NOTHING;
