<p align="center">
  <img src="sesa-salud/public/icon2.png" alt="SESA Logo" width="80" />
</p>

<h1 align="center">SESA — Sistema Electrónico de Salud</h1>

<p align="center">
  Plataforma SaaS de administración clínica para organizaciones de salud.<br/>
  Gestión integral de pacientes, citas, historias clínicas, facturación y más.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Angular-18-dd0031?logo=angular&logoColor=white" alt="Angular 18" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.2-6db33f?logo=springboot&logoColor=white" alt="Spring Boot 3.2" />
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169e1?logo=postgresql&logoColor=white" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/Java-17-f89820?logo=openjdk&logoColor=white" alt="Java 17" />
  <img src="https://img.shields.io/badge/License-Proprietary-blue" alt="License" />
</p>

---

## Descripción

**SESA** es una plataforma SaaS multi-tenant diseñada para la administración clínica de organizaciones de salud en Colombia. Permite gestionar de forma centralizada:

- **Pacientes** — Registro, búsqueda y ficha completa
- **Citas médicas** — Agendamiento, calendario interactivo
- **Historia clínica** — Consultas, diagnósticos CIE-10, mapa de dolor corporal
- **Laboratorios** — Solicitudes y resultados
- **Imágenes diagnósticas** — Gestión de órdenes
- **Urgencias** — Triage y atención
- **Hospitalización** — Control de camas
- **Farmacia** — Dispensación de medicamentos
- **Facturación** — Facturas, totales y reportes
- **Usuarios y roles** — RBAC granular por módulo
- **Empresas** — Multi-tenancy con esquemas PostgreSQL separados
- **Reportes** — Gráficas de indicadores (solo administrador)

## Arquitectura

```
SESA/
├── sesa-backend/          # API REST — Spring Boot 3.2 + Java 17
│   ├── src/main/java/     # Código fuente (controllers, services, entities, DTOs)
│   ├── src/main/resources/ # Configuración (application.yml, perfiles)
│   └── pom.xml            # Dependencias Maven
│
├── sesa-salud/            # Frontend SPA — Angular 18
│   ├── src/app/           # Componentes, servicios, guards, rutas
│   ├── src/environments/  # Configuración por entorno
│   └── package.json       # Dependencias npm
│
├── setup-dev.sh           # Script de setup para Linux/macOS
├── setup-dev.ps1          # Script de setup para Windows
└── README.md
```

## Tech Stack

| Capa | Tecnología | Versión |
|------|-----------|---------|
| **Frontend** | Angular (standalone components) | 18.2 |
| **UI** | SCSS + CSS Variables (tema claro/oscuro) | — |
| **Iconos** | Font Awesome (Angular) | 7.x |
| **Gráficas** | Chart.js | 4.x |
| **Backend** | Spring Boot | 3.2.5 |
| **Seguridad** | Spring Security + JWT (OAuth2 Resource Server) | — |
| **ORM** | Hibernate / JPA | — |
| **Base de datos** | PostgreSQL | 16+ |
| **Multi-tenancy** | Esquemas PostgreSQL por empresa | — |
| **Build** | Maven (backend) · Angular CLI (frontend) | — |

## Requisitos previos

- **Java** 17+
- **Maven** 3.8+
- **Node.js** 18+ y **npm** 9+
- **PostgreSQL** 14+ (o Docker)
- **Angular CLI** 18+ (`npm i -g @angular/cli`)

## Inicio rápido

### 1. Clonar el repositorio

```bash
git clone https://github.com/<tu-usuario>/SESA.git
cd SESA
```

### 2. Base de datos

```bash
# Con Docker (recomendado)
docker run -d --name sesa-postgres \
  -e POSTGRES_USER=sesa \
  -e POSTGRES_PASSWORD=sesa_secret \
  -e POSTGRES_DB=sesa_db \
  -p 5432:5432 \
  postgres:16-alpine
```

O manualmente:

```sql
CREATE USER sesa WITH PASSWORD 'sesa_secret';
CREATE DATABASE sesa_db OWNER sesa;
```

### 3. Backend

```bash
cd sesa-backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

La API queda disponible en `http://localhost:8000/api`.

### 4. Frontend

```bash
cd sesa-salud
npm install
ng serve
```

La aplicación queda en `http://localhost:4200`.

### 5. Credenciales iniciales

Al iniciar por primera vez, el backend crea un usuario administrador:

| Campo | Valor |
|-------|-------|
| Email | `admin@sesa.local` |
| Contraseña | `Admin123!` |
| Rol | ADMIN |

## Scripts de setup

Para configurar todo el entorno de desarrollo automáticamente:

```bash
# Linux / macOS
./setup-dev.sh

# Windows (PowerShell)
.\setup-dev.ps1
```

## Estructura del backend

```
com.sesa.salud
├── config/            # Security, CORS, DataInitializer, TenantFilter
├── controller/        # REST controllers
├── controller/advice/ # GlobalExceptionHandler
├── dto/               # Data Transfer Objects
├── entity/            # Entidades JPA
├── repository/        # Spring Data JPA repositories
├── security/          # JWT filter, token provider, UserDetailsService
├── service/           # Interfaces de servicio
└── service/impl/      # Implementaciones
```

## Estructura del frontend

```
src/app/
├── core/
│   ├── guards/        # Auth, role, medico, super-admin guards
│   ├── interceptors/  # JWT interceptor
│   ├── offline/       # Service worker, IndexedDB sync
│   └── services/      # Auth, CRUD, permisos, tema
├── features/
│   ├── auth/          # Login, reset password
│   ├── dashboard/     # Dashboard dinámico por rol
│   ├── reportes/      # Gráficas (solo ADMIN)
│   ├── pacientes/     # CRUD pacientes
│   ├── citas/         # Gestión de citas
│   ├── historia-clinica/ # HC, consultas, mapa de dolor
│   ├── laboratorios/  # Solicitudes de laboratorio
│   ├── facturacion/   # Facturación
│   └── ...            # Urgencias, farmacia, hospitalización, etc.
└── shared/
    └── components/    # Card, calendar, charts, form-field, breadcrumb
```

## Variables de entorno

### Backend (`application.yml`)

| Variable | Descripción | Default |
|----------|-------------|---------|
| `spring.datasource.url` | JDBC URL | `jdbc:postgresql://localhost:5432/sesa_db` |
| `spring.datasource.username` | Usuario DB | `postgres` |
| `spring.datasource.password` | Contraseña DB | — |
| `sesa.jwt.secret` | Clave JWT (min. 256 bits) | Solo dev |
| `sesa.jwt.expiration-ms` | Validez token (ms) | `86400000` (24h) |
| `sesa.cors.allowed-origins` | Orígenes CORS | `http://localhost:4200` |

### Frontend (`environments/`)

| Variable | Descripción | Default |
|----------|-------------|---------|
| `apiUrl` | Base URL del API | `http://localhost:8000/api` |

## Características destacadas

- **Tema claro / oscuro** con toggle persistente
- **RBAC granular** — 11 roles predefinidos, acceso por módulo
- **Multi-tenancy** — Esquema PostgreSQL separado por empresa
- **Dashboard dinámico** — Tarjetas y datos según el rol del usuario
- **Reportes con gráficas** — Chart.js (barras, líneas, doughnut)
- **Calendario interactivo** — Selección de fecha para ver citas
- **Mapa de dolor corporal** — SVG interactivo en historia clínica
- **PWA-ready** — Service worker, offline sync con IndexedDB
- **Glassmorphism UI** — Login premium con partículas animadas

## Licencia

Software propietario. Todos los derechos reservados.

---

<p align="center">
  Desarrollado por <strong>Ing. J Sebastian Vargas S</strong>
</p>
