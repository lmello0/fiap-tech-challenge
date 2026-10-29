INSERT INTO part (id, sku, name, description, unit_price, minimum_quantity, on_hand, reserved, active, created_at, updated_at) VALUES
    ('0198a000-0000-7000-8000-000000000301', 'FLT-OIL-001', 'Oil filter', 'Oil filter for 1.0 and 1.3 engines', 35.00, 5, 20, 0, TRUE, now() - interval '90 days', now() - interval '30 days'),
    ('0198a000-0000-7000-8000-000000000302', 'BRK-PAD-014', 'Front brake pads', 'Ceramic front brake pad set', 120.00, 4, 3, 0, TRUE, now() - interval '90 days', now() - interval '30 days'),
    ('0198a000-0000-7000-8000-000000000303', 'FLT-AIR-007', 'Air filter', 'Engine air filter', 45.00, 2, 0, 0, TRUE, now() - interval '90 days', now() - interval '30 days')
ON CONFLICT (id) DO NOTHING;

INSERT INTO stock_movement (id, part_id, type, quantity, balance_after, reason, performed_by, created_at) VALUES
    ('0198a000-0000-7000-8000-000000000401', '0198a000-0000-7000-8000-000000000301', 'ENTRY', 20, 20, 'Initial stock', 'seed', now() - interval '30 days'),
    ('0198a000-0000-7000-8000-000000000402', '0198a000-0000-7000-8000-000000000302', 'ENTRY', 3, 3, 'Initial stock', 'seed', now() - interval '30 days')
ON CONFLICT (id) DO NOTHING;
