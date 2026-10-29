INSERT INTO customer (id, name, document, email, phone, active, created_at, updated_at) VALUES
    ('0198a000-0000-7000-8000-000000000001', 'Carlos Customer', '52998224725', 'customer@workshop.local', '11999990001', TRUE, now() - interval '60 days', now() - interval '60 days'),
    ('0198a000-0000-7000-8000-000000000002', 'Marina Souza', '11144477735', 'marina.souza@example.com', '11999990002', TRUE, now() - interval '45 days', now() - interval '45 days')
ON CONFLICT (id) DO NOTHING;

INSERT INTO vehicle (id, customer_id, plate, brand, model, model_year, active, created_at, updated_at) VALUES
    ('0198a000-0000-7000-8000-000000000101', '0198a000-0000-7000-8000-000000000001', 'ABC1D23', 'Fiat', 'Argo', 2022, TRUE, now() - interval '60 days', now() - interval '60 days'),
    ('0198a000-0000-7000-8000-000000000102', '0198a000-0000-7000-8000-000000000001', 'XYZ9A87', 'Volkswagen', 'Polo', 2021, TRUE, now() - interval '55 days', now() - interval '55 days'),
    ('0198a000-0000-7000-8000-000000000103', '0198a000-0000-7000-8000-000000000002', 'QWE4R56', 'Chevrolet', 'Onix', 2020, TRUE, now() - interval '45 days', now() - interval '45 days')
ON CONFLICT (id) DO NOTHING;
