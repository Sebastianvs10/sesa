-- Migración V6: Asegurar que foto_data y firma_data en personal son BYTEA (evitar OID)
-- Autor: Ing. J Sebastian Vargas S
-- Ejecutar con search_path = schema_tenant

-- Si las columnas ya son bytea, el USING col::bytea no cambia nada.
-- Si fueron creadas como OID por Hibernate, se convierten con lo_get.
DO $$
DECLARE
  ft text;
  fd text;
  sch text := current_schema();
BEGIN
  SELECT data_type INTO ft FROM information_schema.columns
   WHERE table_schema = sch AND table_name = 'personal' AND column_name = 'firma_data';
  SELECT data_type INTO fd FROM information_schema.columns
   WHERE table_schema = sch AND table_name = 'personal' AND column_name = 'foto_data';

  IF ft = 'oid' THEN
    EXECUTE 'ALTER TABLE ' || quote_ident(sch) || '.personal ALTER COLUMN firma_data TYPE bytea USING (CASE WHEN firma_data IS NOT NULL THEN lo_get(firma_data) ELSE NULL::bytea END)';
  ELSIF ft IS NOT NULL AND ft != 'bytea' THEN
    EXECUTE 'ALTER TABLE ' || quote_ident(sch) || '.personal ALTER COLUMN firma_data TYPE bytea USING firma_data::bytea';
  END IF;

  IF fd = 'oid' THEN
    EXECUTE 'ALTER TABLE ' || quote_ident(sch) || '.personal ALTER COLUMN foto_data TYPE bytea USING (CASE WHEN foto_data IS NOT NULL THEN lo_get(foto_data) ELSE NULL::bytea END)';
  ELSIF fd IS NOT NULL AND fd != 'bytea' THEN
    EXECUTE 'ALTER TABLE ' || quote_ident(sch) || '.personal ALTER COLUMN foto_data TYPE bytea USING foto_data::bytea';
  END IF;
END $$;

-- Si prefieres ejecutar a mano y las columnas ya son bytea (o no existían como OID):
-- ALTER TABLE personal ALTER COLUMN foto_data TYPE bytea USING foto_data::bytea;
-- ALTER TABLE personal ALTER COLUMN firma_data TYPE bytea USING firma_data::bytea;
