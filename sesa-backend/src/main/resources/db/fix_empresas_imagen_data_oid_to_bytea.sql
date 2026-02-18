-- Convierte public.empresas.imagen_data de oid (Large Object) a bytea.
-- Necesario cuando la columna se creó con @Lob (tipo oid) y ahora se mapea como bytea.
--
-- CÓMO EJECUTAR (una sola vez, con la aplicación parada):
--   psql -U <usuario> -d <basedatos> -f src/main/resources/db/fix_empresas_imagen_data_oid_to_bytea.sql
-- O desde psql: \i ruta/al/fix_empresas_imagen_data_oid_to_bytea.sql
--
-- Autor: Ing. J Sebastian Vargas S

DO $$
DECLARE
  col_type text;
BEGIN
  SELECT data_type INTO col_type
  FROM information_schema.columns
  WHERE table_schema = 'public' AND table_name = 'empresas' AND column_name = 'imagen_data';

  IF col_type = 'oid' THEN
    EXECUTE 'ALTER TABLE public.empresas ALTER COLUMN imagen_data TYPE bytea USING (CASE WHEN imagen_data IS NOT NULL THEN lo_get(imagen_data) ELSE NULL::bytea END)';
    RAISE NOTICE 'Columna imagen_data convertida de oid a bytea.';
  ELSIF col_type IS NULL THEN
    RAISE NOTICE 'Columna imagen_data no existe en public.empresas.';
  ELSIF col_type = 'bytea' THEN
    RAISE NOTICE 'Columna imagen_data ya es bytea. Nada que hacer.';
  ELSE
    BEGIN
      EXECUTE 'ALTER TABLE public.empresas ALTER COLUMN imagen_data TYPE bytea USING imagen_data::bytea';
      RAISE NOTICE 'Columna imagen_data convertida a bytea.';
    EXCEPTION WHEN OTHERS THEN
      RAISE NOTICE 'No se pudo convertir (tipo actual: %). Ejecute manualmente el ALTER con USING apropiado.', col_type;
    END;
  END IF;
END $$;
