-- Inserta el rol FACTURACION en public.roles si no existe.
-- Ejecutar en el schema public (ej.: psql -d tu_bd -f migration_rol_facturacion.sql).
-- Tras ejecutar, reiniciar el backend para que PermissionServiceImpl asigne los módulos por defecto.

INSERT INTO public.roles (codigo, nombre)
SELECT 'FACTURACION', 'Facturación'
WHERE NOT EXISTS (SELECT 1 FROM public.roles WHERE codigo = 'FACTURACION');
