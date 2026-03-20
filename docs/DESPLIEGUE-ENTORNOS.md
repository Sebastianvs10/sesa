# SESA — Entornos (local vs producción)

Dominio de producción: **appsesa.online** (front: `https://appsesa.online` y variantes; API: `https://api.appsesa.online/api`).

## Frontend (`sesa-salud`)

| Entorno | Comando | API base |
|--------|---------|----------|
| **Local** | `npm start` / `ng serve` (config *development*) | `http://localhost:8000/api` |
| **Producción** | `npm run build` (ejecuta `prebuild` + `ng build`) | Por defecto `https://api.appsesa.online/api` |

**Override en CI / Vercel:** variable de entorno `SESA_API_URL` (o `SESAAPI_URL`) con la URL completa del API, incluyendo `/api`, por ejemplo `https://api.appsesa.online/api`.

**Vercel:** enlaza DNS **appsesa.online** (o **app.appsesa.online**) al proyecto. Build Command: `npm run build`. **Output:** `dist/sesa-salud/browser`.

## Backend (`sesa-backend`)

| Entorno | Perfil | Archivo principal |
|--------|--------|-------------------|
| **Local** | (ninguno o el que uses en IDE) | `application.yml` → puerto **8000**, Postgres local |
| **Producción** | `SPRING_PROFILES_ACTIVE=prod` | `application-prod.yml` + variables de entorno |

**Variables habituales en producción:** `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `SESA_JWT_SECRET`, `SESA_CORS_ALLOWED_ORIGINS`, `SESA_FRONTEND_URL`, `PORT` (PaaS), `SESA_UPLOAD_PATH`, `LOGGING_FILE_NAME`.

Por defecto, `SESA_FRONTEND_URL` es **https://appsesa.online** y CORS incluye **https://appsesa.online**, **https://www.appsesa.online** y **https://app.appsesa.online**. Si solo usas un origen en producción, define `SESA_CORS_ALLOWED_ORIGINS` con esa URL exacta. Secretos y credenciales solo en el panel del proveedor, no en Git.

### Esquema vacío en Neon (error `Schema-validation: missing table [...]`)

Con base de datos **nueva**, no existen tablas. En `application-prod` el valor por defecto de **`SESA_JPA_DDL_AUTO`** es **`update`**: Hibernate crea/ajusta tablas del modelo (incl. `public.archivo_almacenamiento` y demás entidades) en el **primer arranque**.

Cuando el esquema ya esté fijo y quieras solo comprobar coincidencia con el modelo, en Render añade **`SESA_JPA_DDL_AUTO=validate`** (y mantén migraciones/SQL alineados con las entidades).

### Render (API con Docker)

1. **New → Web Service** → conecta el repo. **Root Directory** vacío si usas `render.yaml` en la raíz del monorepo; si importas solo `sesa-backend`, apunta ahí.
2. **Runtime: Docker** (recomendado). Dockerfile: `sesa-backend/Dockerfile`, contexto `sesa-backend`.
3. **Health check path:** `/api/actuator/health` (hay `context-path: /api`).
4. **Variables obligatorias** (Environment):

| Variable | Ejemplo / notas |
|----------|------------------|
| `SPRING_PROFILES_ACTIVE` | `prod` (ya va en `render.yaml` si usas Blueprint) |
| `SPRING_DATASOURCE_URL` | JDBC Neon: `jdbc:postgresql://HOST/DB?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | Usuario Postgres |
| `SPRING_DATASOURCE_PASSWORD` | Secreto del proveedor |
| `SESA_JWT_SECRET` | Mín. 32 caracteres aleatorios (p. ej. `openssl rand -base64 48`) |
| `SESA_CORS_ALLOWED_ORIGINS` | Orígenes HTTPS del front, separados por coma |
| `SESA_FRONTEND_URL` | `https://appsesa.online` (o la URL real del SPA) |

Opcionales: `SESA_UPLOAD_PATH`, `LOGGING_FILE_NAME`, `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE`.

5. **Arranque:** si falta `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_PASSWORD` o `SESA_JWT_SECRET`, Spring no levanta. Con BD remota, `ProductionEnvironmentValidator` rechaza JWT débil o contraseña placeholder.

6. **Blueprint:** en la raíz del repo, `render.yaml` define el servicio; tras el primer deploy, completa las variables sensibles en el panel.
