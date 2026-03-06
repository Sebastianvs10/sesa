# EBS – Análisis y complemento por dependencia

Documento de análisis del estado actual del módulo EBS frente a las funcionalidades requeridas por dependencia (Coordinador, Enfermería, Médico, Psicología, Odontología, Salud Pública, Territorial, RIPS, Offline, Dashboard gerencial). Incluye preguntas para priorizar y plan por fases.

**Autor:** Ing. J Sebastian Vargas S

---

## 0. Decisiones tomadas (respuestas test)

| # | Pregunta | Respuesta | Implicación |
|---|----------|-----------|-------------|
| 1 | ¿Quién actúa como Coordinador EBS? | **A** | Solo el rol `COORDINADOR_TERRITORIAL`. |
| 2 | ¿Cómo acceden Enfermería/Médico/Psicología/etc. al EBS? | **C** | Menú EBS visible para todos; pantallas/acciones distintas según rol. |
| 3 | ¿Cómo asignar equipo humano al territorio? | **C** | Varias personas por territorio **y** por brigada (cada brigada tiene su equipo). |
| 4 | ¿Qué son las brigadas? | **A** | Eventos con fecha inicio/fin, territorio y equipo asignado (se programan). |
| 5 | ¿Registro de visita por dependencia? | **C** | Formulario base común + al final "agregar intervención" (Enfermería/Médico/Psicología). |
| 6 | ¿Dónde van CIE10 y plan de cuidado? | **C** | Opcionales en visita EBS; si hay consulta FHIR, se copian ahí. |
| 7 | ¿RIPS desde visitas EBS? | **B** | Export/proceso aparte "RIPS EBS / extramural". |
| 8 | ¿Formato de reportes (cobertura, PDM, etc.)? | **B** | Descarga en PDF y/o Excel. |
| 9 | ¿Indicadores Res. 3280 y metas PDM definidos? | **B** | No aún; pantallas genéricas que después se ajusten. |
| 10 | ¿Cómo funcionan las alertas? | **C** | Carga manual + integración futura (vigilancia). |
| 11 | ¿Paciente → vereda por ubicación? | **C** | Por dirección/vereda escrita, sin geometría (PostGIS). |

---

## 1. Estado actual en SESA (lo que ya existe)

### 1.1 Modelo de datos (backend)

| Entidad / Tabla | Descripción | Campos relevantes |
|-----------------|-------------|-------------------|
| `ebs_territories` | Microterritorios | code, name, type, geometry, assigned_team_id, igac_* (dep/mun/vereda) |
| `ebs_households` | Hogares | territory_id, address_text, latitude, longitude, rural, stratum, state, risk_level |
| `ebs_family_groups` | Grupos familiares | household_id, main_contact_patient_id, socioeconomic_level, risk_notes |
| `ebs_home_visits` | Visitas domiciliarias | household_id, professional_id, visit_date, visit_type, motivo, notes, status, offline_uuid, sync_status |
| `ebs_risk_assessments` | Valoración de riesgo | patient_id, home_visit_id, category, score, risk_level, fhir_observation_id |

### 1.2 Catálogo IGAC (public)

- `igac_departamentos`, `igac_municipios`, `igac_veredas` (Huila completo: 37 municipios + veredas/corregimientos).
- Asignación de límites oficiales por territorio EBS (departamento → municipio → vereda).

### 1.3 API y servicios EBS

- `GET/PUT` territorios (listar, actualizar IGAC).
- `GET` hogares por territorio (filtros riesgo/estado visita).
- `POST` visita domiciliaria (crear).
- `GET` visitas (filtros territorio, profesional, fechas).
- `GET` dashboard supervisor (KPIs: territorios, hogares, cobertura, alto riesgo, visitas en período).

### 1.4 Frontend EBS (sesa-salud)

- **Rutas:** `/ebs` (layout), `/ebs/inicio`, `/ebs/territorios`, `/ebs/visitas`, `/ebs/visita/nueva`, `/ebs/asignacion`, `/ebs/dashboard-supervisor`.
- **Acceso:** `roleGuard('EBS')` → quien tenga permiso módulo EBS (incl. COORDINADOR_TERRITORIAL, SUPERVISOR_APS).
- **Inicio:** Por rol: Supervisor ve dashboard; resto ve territorios asignados y accesos rápidos.
- **Territorios:** Lista de microterritorios, hogares por territorio, filtros riesgo/estado, **mapa** (Leaflet) con marcadores por hogar, panel de acciones por hogar, **asignar límites IGAC** (dep → mun → vereda).
- **Visitas:** Listado histórico con filtros.
- **Visita nueva:** Formulario (territorio, hogar, fecha, tipo, motivo, notas, banderas riesgo).
- **Asignación:** Página básica (listado territorios; texto indicando que la asignación se gestiona en backend).
- **Dashboard supervisor:** KPIs + tabla indicadores por territorio, período configurable.

### 1.5 Offline (PWA)

- Interceptor HTTP: escrituras (POST/PUT/DELETE/PATCH) se encolan en IndexedDB si no hay conexión; GET con cache TTL.
- Visita domiciliaria (POST /ebs/home-visits) ya es “offline-friendly” (offline_uuid, sync_status en entidad).
- Servicio de sincronización y cola de operaciones pendientes existe.

### 1.6 RIPS en SESA (fuera de EBS)

- Módulo de facturación/RIPS: generación, exportación estructurada Res. 3374/2000, CIE10 en FHIR. **No está hoy vinculado explícitamente a visitas EBS** (no hay “RIPS desde visita domiciliaria” en un solo flujo).

---

## 2. Gaps por dependencia (lo que falta)

### 2.1 COORDINADOR EBS

| Requerido | Estado | Acción propuesta |
|-----------|--------|-------------------|
| Crear territorios EBS | Parcial | Solo datos semilla; no hay UI “Crear territorio” ni API POST territorio. |
| Asignar veredas | Hecho | Asignación IGAC (dep → mun → vereda) por territorio. |
| Asignar equipo humano | Parcial | Campo `assigned_team_id` en territorio; no hay UI de asignación ni modelo “equipo” (varios profesionales). |
| Programar brigadas | No | No existe entidad “brigada” ni programación. |
| Validar visitas | Parcial | Campo `status` en visita; no hay flujo “validar” ni rol explícito. |
| Supervisar indicadores | Parcial | Dashboard supervisor con KPIs; falta detalle por vereda y alertas. |
| Aprobar reportes | No | No hay entidad “reporte” ni flujo de aprobación. |
| Gestionar rutas rurales | No | No hay entidad “ruta” ni orden de visitas. |
| Medir cobertura | Parcial | Dashboard con cobertura; falta % población intervenida y métricas por vereda. |
| **Ver: mapa cobertura** | Parcial | Mapa con hogares; falta capa “cobertura” (polígonos/calor). |
| **Ver: % población intervenida** | Parcial | Se puede derivar de visitas/hogares; no expuesto en UI. |
| **Ver: riesgos por vereda** | No | No hay agregación por vereda IGAC. |
| **Ver: alertas epidemiológicas** | No | No hay entidad ni pantalla de alertas. |
| **Reportes:** Cobertura territorial, captación temprana, crónicos, PDM | No | No hay módulo de reportes EBS ni plantillas PDM. |

### 2.2 ENFERMERÍA

| Requerido | Estado | Acción propuesta |
|-----------|--------|-------------------|
| Registro visita domiciliaria | Hecho | Formulario visita + creación. |
| Clasificación de riesgo | Parcial | EbsRiskAssessment + banderas en visita; falta formulario estructurado “clasificación” en visita. |
| Seguimiento gestantes | No | No hay formulario/entidad específica gestantes en EBS. |
| Vacunación (PAI) | No | No hay registro PAI en visita. |
| Tamizajes | No | No hay ítems tamizaje en visita. |
| Control crecimiento y desarrollo | No | No hay ítems Crecimiento y Desarrollo. |
| Registro signos vitales | No | No hay ítems signos vitales en visita (podría reutilizar FHIR/Observation). |
| Alertas de riesgo | Parcial | risk_level en hogar/visita; no hay “alertas” activas. |
| **Campos críticos:** GPS, Vereda, Tipo intervención, CIE10, Plan de cuidado | Parcial | GPS en hogar; vereda vía IGAC en territorio; tipo en visit_type; CIE10 y plan no en visita. |

### 2.3 MÉDICO

| Requerido | Estado | Acción propuesta |
|-----------|--------|-------------------|
| Consulta primaria, diagnóstico, fórmula, remisión, seguimiento crónicos | Existe en SESA | Módulos consulta médica, historia clínica, receta, etc.; no integrados como “desde EBS/visita”. |
| Cargue RIPS | Existe en SESA | RIPS en facturación; no enlazado a visita domiciliaria. |
| Validación riesgo cardiovascular | Parcial | EbsRiskAssessment category/risk_level; no flujo “validación” médico. |

### 2.4 PSICOLOGÍA

| Requerido | Estado | Acción propuesta |
|-----------|--------|-------------------|
| Evaluación salud mental, riesgo suicida, violencia intrafamiliar, intervención comunitaria, seguimiento adolescente | No en EBS | No hay formularios ni entidades EBS específicas de psicología. |

### 2.5 ODONTOLOGÍA

| Requerido | Estado | Acción propuesta |
|-----------|--------|-------------------|
| Valoración oral, caries, fluorización, sellantes, remisiones | Existe en SESA | Módulo odontología; no integrado desde EBS/visita. |

### 2.6 SALUD PÚBLICA

| Requerido | Estado | Acción propuesta |
|-----------|--------|-------------------|
| Brotes epidemiológicos, mapa de riesgos, determinantes sociales, vigilancia, eventos notificación obligatoria | No | No hay entidades ni pantallas. |

### 2.7 MÓDULO TERRITORIAL

| Requerido | Estado | Acción propuesta |
|-----------|--------|-------------------|
| Cargar veredas oficiales (IGAC) | Hecho | Catálogo Huila + otros; asignación por territorio. |
| Relacionar con municipios (DIVIPOLA) | Hecho | IGAC dep/mun/vereda. |
| Crear microterritorios EBS | Parcial | Falta UI crear territorio. |
| Asignar equipo a polígono | Parcial | assigned_team_id; no “polígono” ni equipos multi-persona. |
| Consulta paciente → vereda automática | No | No hay servicio “paciente → vereda” (por dirección/coordenada). |
| Mapa de calor de intervenciones | No | Mapa actual es marcadores; no heatmap. |
| Alertas geográficas | No | No hay alertas por zona. |
| **PostGIS / intersección territorio–vereda** | No | Sin PostGIS; geometría en texto. |
| Dashboard por microterritorio | Parcial | Dashboard supervisor con indicadores por territorio; no por vereda. |

### 2.8 MÓDULO RIPS READY

| Requerido | Estado | Acción propuesta |
|-----------|--------|-------------------|
| Generar RIPS automáticamente | Existe | En facturación; no desde visitas EBS. |
| Exportar formato oficial, validar CIE10/servicio | Existe | Res. 3374/2000, CIE10 en FHIR. |
| Integración futura SISPRO | No | No implementado. |
| **RIPS desde visita domiciliaria** | No | No hay flujo “esta visita → RIPS” ni archivo específico extramural. |

### 2.9 MODO OFFLINE

| Requerido | Estado | Acción propuesta |
|-----------|--------|-------------------|
| Guardar visitas offline | Hecho | Cola + offline_uuid. |
| Guardar coordenadas | Hecho | Lat/long en hogar y en visita (si se capturan en formulario). |
| Sincronización posterior | Hecho | SyncService + cola. |
| Control de conflictos | Parcial | sync_status/sync_errors; no hay UI de resolución. |

### 2.10 DASHBOARD GERENCIAL (Secretaría de Salud)

| Requerido | Estado | Acción propuesta |
|-----------|--------|-------------------|
| Cobertura por vereda, % población intervenida, riesgo alto, mapa de calor, cumplimiento metas, indicadores Res. 3280 | Parcial | Dashboard supervisor con KPIs; falta por vereda, heatmap, metas PDM y 3280. |

---

## 3. Preguntas para priorizar y acotar

1. **Roles y acceso**  
   - ¿El Coordinador EBS debe ser exactamente el rol `COORDINADOR_TERRITORIAL` o habrá otro (ej. “Coordinador EBS”)?  
   - ¿Enfermería/Médico/Psicología/Odontología/Salud Pública deben entrar al módulo EBS con el mismo menú que el Coordinador o solo desde “su” flujo (ej. consulta médica) con opción “registrar desde visita EBS”?

2. **Territorial y equipos**  
   - ¿“Asignar equipo humano” es 1 persona por territorio (actual `assigned_team_id`) o un “equipo” de N personas por territorio/brigada?  
   - ¿Brigadas son “eventos puntuales” (fecha, territorio, equipo) o solo otra forma de agrupar visitas?  
   - ¿Necesitan consultas espaciales PostGIS (ej. “paciente en esta vereda”) en esta fase o basta con vereda asignada al territorio?

3. **Visita domiciliaria**  
   - ¿Un solo tipo de “registro de visita” con pestañas/bloques por dependencia (enfermería, médico, psicología, etc.) o formularios separados por rol?  
   - ¿CIE10 y plan de cuidado deben estar siempre en la visita EBS o solo cuando se genera encuentro FHIR/consulta?

4. **RIPS**  
   - ¿Las visitas domiciliarias EBS deben generar RIPS de consulta extramural (UX, AC, etc.) en el mismo flujo de RIPS actual o un export “RIPS EBS” aparte?

5. **Reportes y metas**  
   - ¿Tienen ya definidos indicadores Res. 3280 y metas PDM por vereda/municipio para replicar en pantalla?  
   - ¿Los “reportes” del Coordinador (cobertura, captación, crónicos, PDM) son PDF/Excel fijos o solo dashboards en pantalla?

6. **Alertas**  
   - ¿Alertas epidemiológicas/geográficas son solo listado de “eventos” que alguien carga o integración con vigilancia (Sivigila, etc.)?

---

## 4. Plan de complemento por fases

### Fase 1 – Coordinador EBS (núcleo territorial)

- **Backend:**  
  - API `POST /ebs/territories` (crear territorio).  
  - Modelo “equipo” o ampliar asignación (equipo = lista de personal_id por territorio).  
  - Endpoint “brigadas”: entidad `ebs_brigada` (nombre, territorio_id, fecha_inicio, fecha_fin, estado) y CRUD básico.
- **Frontend:**  
  - Pantalla “Crear territorio” (código, nombre, tipo, límites IGAC).  
  - En “Asignación territorial”: asignar veredas (ya hay IGAC), asignar equipo (lista de profesionales).  
  - Listado/calendario simple de brigadas y vinculación “visitas de esta brigada”.

### Fase 2 – Visita domiciliaria rica (Enfermería + campos críticos)

- **Backend:**  
  - Ampliar `EbsHomeVisit` o tabla relacionada: vereda_codigo, tipo_intervencion, diagnostico_cie10, plan_cuidado (texto o JSON).  
  - Opcional: tabla `ebs_visita_signos_vitales`, `ebs_visita_tamizaje`, etc., o reutilizar Observation FHIR.
- **Frontend:**  
  - Formulario “Visita domiciliaria” con: coordenadas (captura o del hogar), vereda (desde territorio IGAC), tipo de intervención, CIE10 (autocompletado), plan de cuidado.  
  - Bloque “Enfermería”: clasificación de riesgo estructurada, signos vitales, tamizajes (si aplica).

### Fase 3 – Integración Médico / Psicología / Odontología / Salud Pública

- Reutilizar consulta médica, historia clínica, odontología, etc., con “contexto de visita EBS” (paciente/hogar/visita_id).  
- Pantalla o sección “Desde visita EBS”: abrir consulta/receta/interconsulta con visita_id y vereda ya informados.  
- Formularios específicos ligeros para psicología (riesgo suicida, VIF) y salud pública (evento notificación) como ítems o anexos a la visita.

### Fase 4 – Reportes y dashboard gerencial

- Reportes: cobertura territorial, captación temprana, seguimiento crónicos (consultas a visitas + diagnósticos).  
- Dashboard: % población intervenida por vereda/municipio, riesgo alto por zona, indicadores Res. 3280 si se definen.  
- Export PDF/Excel de reportes si se requiere.

### Fase 5 – Mapa de calor y alertas

- Backend: agregación de intervenciones/visitas por zona (vereda o grid).  
- Frontend: capa heatmap en el mapa (Leaflet heat).  
- Entidad “alerta” (tipo, vereda/municipio, fecha, estado) y pantalla de listado/gestión.

### Fase 6 – RIPS desde visita EBS y offline

- Flujo “Generar RIPS desde visitas EBS” (período, territorio) integrado al módulo RIPS existente.  
- Offline: UI de cola de pendientes y resolución de conflictos (ver sync_status, reintentar, descartar).

---

## 4.1 Plan de implementación según decisiones (resumen)

Ajustado a las respuestas del test:

| Decisión | Cómo se implementa |
|----------|---------------------|
| **Coordinador = COORDINADOR_TERRITORIAL** | Sin rol nuevo; permisos y vistas ya alineados a este rol. |
| **Menú EBS para todos, acciones por rol** | Mantener una sola entrada EBS; en cada pantalla mostrar/ocultar acciones y secciones según rol (Coordinador, Enfermería, Médico, etc.). |
| **Equipo por territorio y por brigada** | Modelo: equipo en territorio (N profesionales) + brigada con su propio equipo y fechas. Backend: tabla `ebs_brigada` y relación brigada–equipo (o lista de personal_id). |
| **Brigadas = eventos programables** | CRUD brigadas (nombre, territorio, fecha_inicio, fecha_fin, estado, equipo). Visitas pueden asociarse a `brigada_id`. |
| **Formulario base + "agregar intervención"** | Visita base (hogar, fecha, tipo, motivo, notas). Al guardar o en pantalla: botón "Agregar intervención" → elegir Enfermería/Médico/Psicología/Odontología/Salud Pública y abrir formulario o bloque correspondiente. |
| **CIE10 y plan opcionales; copiar a FHIR si hay consulta** | Campos opcionales en visita EBS (diagnostico_cie10, plan_cuidado). Si se genera encuentro/consulta FHIR desde la visita, mapear esos campos al recurso FHIR. |
| **RIPS EBS = export aparte** | Nuevo flujo "RIPS EBS / extramural": pantalla o endpoint que filtre por visitas domiciliarias y genere archivos RIPS en formato oficial (sin mezclar con el RIPS de consultorio actual). |
| **Reportes en PDF/Excel** | Reportes del Coordinador (cobertura, captación, crónicos, PDM): descarga en PDF y/o Excel, no solo vista en pantalla. |
| **Indicadores 3280/PDM genéricos** | Dashboard y reportes con estructura genérica (por territorio/vereda); cuando definan indicadores concretos, se mapean a esos campos. |
| **Alertas: manual + integración futura** | Entidad "alerta" (tipo, vereda/municipio, fecha, estado) y pantalla de carga manual; diseño que permita luego conectar con Sivigila u otro. |
| **Paciente → vereda por dirección/vereda escrita** | Sin PostGIS: campo vereda (texto o código IGAC) en hogar/paciente; consulta "paciente → vereda" por dirección o vereda asignada al territorio, no por geometría. |

**Orden sugerido de desarrollo**

1. **Fase 1 – Territorial y brigadas:** Crear territorio (POST), equipos por territorio y por brigada, CRUD brigadas, asignar equipo en UI.  
2. **Fase 2 – Visita rica:** Campos opcionales CIE10 y plan de cuidado en visita; formulario base + "Agregar intervención" (bloques por dependencia).  
3. **Fase 3 – Reportes PDF/Excel:** Reportes Coordinador (cobertura, captación, crónicos, PDM) con descarga.  
4. **Fase 4 – RIPS EBS:** Export aparte "RIPS EBS / extramural" desde visitas domiciliarias.  
5. **Fase 5 – Alertas:** Entidad alerta + carga manual; preparar para integración futura.  
6. **Fase 6 – Dashboard genérico:** Indicadores por territorio/vereda con estructura que luego se complete con 3280/PDM.

---

## 5. Resumen ejecutivo

- **Ya cubierto:** Territorios y hogares, visitas básicas, riesgo básico, IGAC (Huila completo), mapa de hogares, dashboard supervisor, offline de visitas, asignación de límites por territorio.  
- **Prioritario para “completar” EBS por dependencia:**  
  - Coordinador: crear territorios, asignar equipo, brigadas, validar visitas, reportes y vistas (mapa cobertura, % intervenida, riesgos por vereda).  
  - Enfermería: campos críticos en visita (vereda, CIE10, plan de cuidado, signos vitales, clasificación de riesgo).  
  - Territorial: creación de territorios desde UI, “paciente → vereda” si se usa PostGIS/dirección.  
  - RIPS: enlace “visita domiciliaria → RIPS”.  
  - Gerencial: indicadores por vereda y Res. 3280 cuando estén definidos.

Las decisiones tomadas (sección 0) y el plan según decisiones (sección 4.1) definen el alcance y el orden de implementación.
