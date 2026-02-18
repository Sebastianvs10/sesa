# SESA Backend

Backend del **Sistema Electrónico de Salud (SESA)**. API REST con Spring Boot, autenticación OAuth2 (JWT), PostgreSQL y capas completas: entidades, DTOs, repositorios, servicios y controladores.

## Requisitos

- Java 17+
- Maven 3.8+
- PostgreSQL 14+ (o contenedor Docker)

## Base de datos PostgreSQL

Crear base de datos y usuario:

```sql
CREATE USER sesa WITH PASSWORD 'sesa_secret';
CREATE DATABASE sesa_db OWNER sesa;
GRANT ALL PRIVILEGES ON DATABASE sesa_db TO sesa;
```

Con Docker:

```bash
docker run -d --name sesa-postgres -e POSTGRES_USER=sesa -e POSTGRES_PASSWORD=sesa_secret -e POSTGRES_DB=sesa_db -p 5432:5432 postgres:16-alpine
```

## Configuración

Variables en `src/main/resources/application.yml`:

| Variable        | Descripción              | Por defecto |
|----------------|---------------------------|-------------|
| `spring.datasource.url` | JDBC URL PostgreSQL | `jdbc:postgresql://localhost:5432/sesa_db` |
| `spring.datasource.username` | Usuario DB | `sesa` |
| `spring.datasource.password` | Contraseña DB | `sesa_secret` |
| `sesa.jwt.secret` | Clave para firmar JWT (mín. 256 bits para HS256) | valor por defecto en yml |
| `sesa.jwt.expiration-ms` | Validez del token en ms | 86400000 (24 h) |

En producción definir `SESA_JWT_SECRET` en el entorno.

## Ejecución

```bash
mvn spring-boot:run
```

La API queda en **http://localhost:8080/api**.

## Autenticación (OAuth2 / JWT)

- **POST /api/auth/login** (público)  
  Cuerpo: `{ "email": "admin@sesa.local", "password": "Admin123!" }`  
  Respuesta: `{ "accessToken": "...", "tokenType": "Bearer", "userId", "email", "nombreCompleto", "role", ... }`

- El frontend debe enviar en las peticiones protegidas el header:  
  `Authorization: Bearer <accessToken>`

Al arrancar se crea un usuario inicial si no existe:

- **Email:** admin@sesa.local  
- **Contraseña:** Admin123!  
- **Rol:** ADMIN  

## Endpoints principales

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | /api/auth/login | Login (JWT) |
| GET  | /api/pacientes | Listar pacientes (paginado, opcional ?q=) |
| GET  | /api/pacientes/{id} | Obtener paciente |
| POST | /api/pacientes | Crear paciente |
| PUT  | /api/pacientes/{id} | Actualizar paciente |
| DELETE | /api/pacientes/{id} | Eliminar paciente |
| GET  | /api/citas?fecha=YYYY-MM-DD | Citas por fecha |
| GET  | /api/citas/paciente/{id} | Citas de un paciente |
| POST | /api/citas | Crear cita |
| GET  | /api/consultas/paciente/{id} | Historia clínica (consultas) del paciente |
| POST | /api/consultas | Crear consulta |
| GET  | /api/personal | Listar personal |
| POST | /api/personal | Crear personal |
| GET  | /api/laboratorio-solicitudes | Solicitudes de laboratorio |
| POST | /api/laboratorio-solicitudes | Crear solicitud |
| GET  | /api/urgencias | Listar urgencias |
| POST | /api/urgencias | Registrar urgencia |

Los endpoints de escritura y algunos de lectura requieren autenticación (JWT) y en varios casos rol ADMIN.

## Integración con el frontend (Angular)

En el proyecto `sesa-salud` (Angular):

1. Configurar la base URL del API, por ejemplo `http://localhost:8080/api`.
2. En el login, llamar a `POST /api/auth/login` con `email` y `password`, guardar `accessToken` (p. ej. en memoria o almacenamiento seguro).
3. En las peticiones HTTP a los recursos protegidos, añadir el header:  
   `Authorization: Bearer <accessToken>`.

CORS está configurado para permitir origen `http://localhost:4200`.

## Estructura del proyecto

```
com.sesa.salud
├── config          # Security, PasswordEncoder, DataInitializer
├── controller      # REST (Auth, Pacientes, Citas, Consultas, Personal, Laboratorio, Urgencias)
├── controller/advice  # GlobalExceptionHandler
├── dto             # DTOs de request/response
├── entity          # Entidades JPA
├── repository      # JpaRepository
├── security        # JWT (filter, token provider), UserDetailsService
├── service         # Interfaces de servicio
└── service/impl    # Implementaciones
```
