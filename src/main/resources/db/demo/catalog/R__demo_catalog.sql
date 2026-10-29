INSERT INTO repair_service (id, name, description, price, estimated_minutes, active, created_at, updated_at) VALUES
    ('0198a000-0000-7000-8000-000000000201', 'Oil change', 'Engine oil and filter replacement', 150.00, 45, TRUE, now() - interval '90 days', now() - interval '90 days'),
    ('0198a000-0000-7000-8000-000000000202', 'Wheel alignment', 'Alignment and balancing of the four wheels', 120.00, 60, TRUE, now() - interval '90 days', now() - interval '90 days'),
    ('0198a000-0000-7000-8000-000000000203', 'Brake pad replacement', 'Replacement of the front brake pads', 180.00, 90, TRUE, now() - interval '90 days', now() - interval '90 days'),
    ('0198a000-0000-7000-8000-000000000204', 'Full inspection', 'Checklist of engine, brakes, suspension and electrics', 220.00, 120, TRUE, now() - interval '90 days', now() - interval '90 days')
ON CONFLICT (id) DO NOTHING;
