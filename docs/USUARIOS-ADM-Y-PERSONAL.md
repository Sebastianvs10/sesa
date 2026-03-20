# Usuarios administrativos vs personal operativo

## Dónde crear cada tipo de cuenta

| Tipo | Pantalla | Roles |
|------|-----------|--------|
| Administradores y superadministradores del tenant | **Usuarios Adm** (`/usuarios`) | Solo `ADMIN` y/o `SUPERADMINISTRADOR` |
| Personal clínico y operativo (médicos, enfermería, recepción, etc.) | **Personal** (`/personal`) | Resto de roles; no sustituye a Usuarios Adm |

## Permisos (API y UI)

- **ADMIN**: puede crear y gestionar usuarios con rol **ADMIN** únicamente. No puede asignar `SUPERADMINISTRADOR`, ni editar/eliminar cuentas que ya tengan `SUPERADMINISTRADOR`.
- **SUPERADMINISTRADOR**: puede crear y gestionar **ADMIN** y **SUPERADMINISTRADOR**.

La API rechaza payloads con roles distintos de `ADMIN`/`SUPERADMINISTRADOR` en creación y actualización de `/usuarios`.

## Eliminación

- **ADMIN**: no puede eliminar usuarios que tengan rol `SUPERADMINISTRADOR`.
- **SUPERADMINISTRADOR**: puede eliminar cuentas administrativas según política de negocio (la API aplica la misma regla que en edición).
