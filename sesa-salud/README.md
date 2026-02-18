# SESA Frontend — Angular 18

Frontend SPA del **Sistema Electrónico de Salud (SESA)**. Construido con Angular 18, standalone components, SCSS con tema claro/oscuro y arquitectura modular.

## Requisitos

- Node.js 18+
- npm 9+
- Angular CLI 18+ (`npm i -g @angular/cli`)

## Instalación

```bash
npm install
```

## Desarrollo

```bash
ng serve
```

Navegar a `http://localhost:4200/`. La app se recarga automáticamente al cambiar archivos fuente.

## Build

```bash
ng build                          # producción
ng build --configuration development  # desarrollo
```

Los artefactos quedan en `dist/sesa-salud/`.

## Configuración

Archivos de entorno en `src/environments/`:

| Variable | Descripción |
|----------|-------------|
| `apiUrl` | URL base del API backend |

## Estructura

```
src/app/
├── core/              # Guards, interceptors, services, offline
├── features/          # Páginas por módulo (login, dashboard, citas, etc.)
└── shared/            # Componentes reutilizables (card, calendar, charts)
```

## Temas

El sistema soporta tema claro y oscuro con `data-theme` en el root. Variables CSS definidas en `src/styles.scss`.
