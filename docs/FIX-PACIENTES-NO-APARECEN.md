# Pacientes en BD pero no en el front

## Causa

El listado de pacientes en el backend **solo devuelve registros con `activo = true`** (`PacienteServiceImpl.findAll()` usa `findByActivoTrue()`). Si el registro en la tabla `pacientes` tiene `activo = false` o `activo` NULL, no aparecerá en el front.

## Solución 1: Ajustar el registro en la BD

Ejecuta en tu base de datos (por ejemplo en pgAdmin o psql):

```sql
UPDATE pacientes SET activo = true WHERE id = 1;
```

Para todos los que quieras que se listen:

```sql
UPDATE pacientes SET activo = true WHERE activo IS NULL OR activo = false;
```

Después recarga la página de Pacientes en el front.

## Solución 2: Listar todos los pacientes en el backend

Si quieres que en la lista aparezcan **todos** los pacientes (activos e inactivos), se puede cambiar el servicio para usar `findAll()` en lugar de `findByActivoTrue()`. El front ya muestra el estado si lo necesitas.
