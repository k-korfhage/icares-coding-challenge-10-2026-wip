INSERT INTO provider (name, max_parallel_appointments)
VALUES
    ('IT-Service Müller', 2),
    ('Computer-Service Schneider', 3);

INSERT INTO service (service_code, name)
VALUES
    ('DIA', 'PC-Diagnose'),
    ('LAP', 'Laptop-Service'),
    ('OSI', 'Betriebssysteminstallation'),
    ('VIR', 'Virenentfernung'),
    ('DAT', 'Datenrettung');

INSERT INTO provider_service (provider_id, service_id, duration_minutes)
SELECT p.id, s.id, service_offer.duration_minutes
FROM (VALUES
    ('IT-Service Müller', 'DIA', 30),
    ('IT-Service Müller', 'LAP', 120),
    ('IT-Service Müller', 'OSI', 60),
    ('Computer-Service Schneider', 'DIA', 15),
    ('Computer-Service Schneider', 'VIR', 60),
    ('Computer-Service Schneider', 'DAT', 180)
) AS service_offer(provider_name, service_code, duration_minutes)
JOIN provider p ON p.name = service_offer.provider_name
JOIN service s ON s.service_code = service_offer.service_code;
