-- MKT-41: ampliar los estados permitidos por el ENUM de H2.
-- Migracion manual. Requiere respaldo previo.

ALTER TABLE payments
    ALTER COLUMN estado
    ENUM (
        'PENDIENTE',
        'APROBADO',
        'RECHAZADO',
        'RESULTADO_DESCONOCIDO',
        'REEMBOLSADO'
    );