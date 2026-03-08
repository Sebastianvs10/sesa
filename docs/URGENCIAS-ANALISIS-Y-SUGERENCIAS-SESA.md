# Análisis del servicio de urgencias y sugerencias de mejora con SESA

**Enfoque:** Profesional en salud (Colombia) + visión de producto y tecnología.  
**Normativa de referencia:** Res. 5596/2015 (triage, tiempos de espera), Res. 1995/1999 (historia clínica), Res. 3374/2000 (RIPS).

---

## 1. Problemáticas habituales en urgencias (Colombia)

### 1.1 Operativas y de flujo

| Problemática | Impacto | Relación con SESA actual |
|-------------|---------|---------------------------|
| **Desconexión ingreso urgencias ↔ Historia Clínica** | El médico no puede registrar evolución hasta que exista “atención” vinculada; el usuario debe ir a otro módulo. | Parcialmente mitigado con “Abrir Historia Clínica”; falta creación automática de atención/HC al ingresar. |
| **Tiempos de espera por triage no visibles en tiempo real** | Incumplimiento de Res. 5596/2015 (límites por nivel), riesgo legal y de calidad. | SESA ya muestra tiempo transcurrido y “restante”; falta alertas audibles/visivas y reportes. |
| **Falta de tablero de mando (dashboard)** | Coordinación y priorización poco objetiva. | No hay vista consolidada de indicadores (tiempos, triage, estado, profesional). |
| **Registro de triage sin trazabilidad** | No se sabe quién clasificó ni cuándo; difícil auditoría. | Campo `profesionalTriageId` existe en DTO pero no se usa en flujo de ingreso en front. |
| **Pacientes en espera sin re-triage** | Deterioro clínico sin reclasificación. | No hay recordatorio ni flujo de re-triage según tiempo en espera. |

### 1.2 Clínicas y de seguridad

| Problemática | Impacto | Relación con SESA actual |
|-------------|---------|---------------------------|
| **Signos vitales solo al ingreso** | No hay serie temporal en urgencias; se pierde evolución durante la espera. | SV se capturan en ingreso; no hay registro de SV repetidos en panel de urgencias. |
| **Alertas por SV anormales** | Valores de riesgo (ej. TA, SpO₂, Glasgow) pasan desapercibidos. | No hay reglas clínicas ni alertas visuales/audibles según rangos. |
| **Glasgow solo para T I/II** | En otros niveles también puede ser útil (TEC, alteración de conciencia). | Glasgow solo se muestra en ingreso para I/II; no como campo opcional para todos. |
| **Sin checklist de seguridad (ej. identificación, alergias)** | Errores de identificación y reacciones adversas. | No hay recordatorio de verificación de identidad ni de alergias en el flujo de urgencias. |

### 1.3 Normativas y reportes

| Problemática | Impacto | Relación con SESA actual |
|-------------|---------|---------------------------|
| **Poca trazabilidad para RIPS / SISPRO** | Dificultad para reportes obligatorios y auditoría. | Tipo de llegada y motivo están; falta integración explícita con RIPS y reportes por tiempo. |
| **Sin indicadores de cumplimiento de tiempos** | No se mide % dentro del tiempo límite por triage. | Cálculo existe en front; no hay agregación ni reporte exportable. |
| **Alta sin resumen ni instrucciones** | Riesgo de reconsultas y mala continuidad. | No hay plantilla de “resumen de alta” ni impreso/PDF para el paciente. |

### 1.4 Experiencia del profesional y UX

| Problemática | Impacto | Relación con SESA actual |
|-------------|---------|---------------------------|
| **Auto-refresh cada 30 s genérico** | Puede distraer o no ser suficiente en picos. | Un solo intervalo; no hay modo “alta carga” ni notificaciones por paciente crítico. |
| **Lista plana sin priorización visual fuerte** | Los T I/II pueden no destacar lo suficiente. | Hay badges de triage; se puede mejorar orden y prominencia visual/sonora. |
| **Sin atajos de teclado** | Más clics y tiempo en pantalla. | No hay shortcuts para “siguiente en espera”, “cambiar estado”, etc. |

---

## 2. Sugerencias innovadoras para implementar en SESA

### 2.1 Vinculación automática Urgencias ↔ Historia Clínica

- **Idea:** Al registrar un **ingreso a urgencias**, crear automáticamente (backend) una **atención** y, si el paciente no tiene HC, una **Historia Clínica** en estado “activa”, asociada a esa atención.
- **Beneficio:** El médico no ve el mensaje “no tiene atención vinculada”; puede registrar la evolución SOAP desde el primer momento.
- **Implementación sugerida:**  
  - Backend: en `UrgenciaRegistroServiceImpl` (o servicio de atención), al crear el registro de urgencia, crear Atención + HC si no existe, y devolver `atencionId` en el DTO.  
  - Front: seguir mostrando “Abrir Historia Clínica” como respaldo, pero el flujo principal sería ya con atención creada.

### 2.2 Tablero de urgencias (dashboard) en tiempo real

- **Idea:** Vista adicional (o cabecera mejorada) con:  
  - Contadores por estado y por triage.  
  - **Pacientes que superan tiempo límite** (Res. 5596/2015) en rojo con contador.  
  - **Tiempo promedio de espera** por triage (calculado en backend o front).  
  - Filtro rápido “Solo críticos (T I/II)” y “Solo fuera de tiempo”.
- **Beneficio:** Coordinación y priorización basada en evidencia; cumplimiento normativo visible.
- **Implementación:** Nuevo endpoint (ej. `GET /urgencias/dashboard`) con agregados; componente “dashboard” o sección en la misma página con tarjetas/gráficos.

### 2.3 Alertas por tiempo y por signos vitales

- **Tiempo:**  
  - Cuando un paciente en espera se acerca al **75% del tiempo límite** (ya existe `tiempoEsperaClass`), mostrar **alerta visual** (borde/icono) en la tarjeta.  
  - Cuando **supera el tiempo límite**, alerta más intensa (ej. banner o notificación) y opción de “Re-clasificar triage” o “Priorizar”.
- **Signos vitales:**  
  - Reglas simples (ej. TA &gt; 180 o &lt; 90, SpO₂ &lt; 92, Glasgow &lt; 15) para marcar el ingreso como “Alerta SV” y mostrar icono/badge en la lista.
- **Implementación:** Lógica en front con constantes configurables; opcionalmente backend que calcule “alertas” en el listado.

### 2.4 Re-triage y trazabilidad del triage

- **Re-triage:** Botón “Re-clasificar triage” en el detalle del paciente (en espera o en atención), que abra un modal con los mismos niveles y permita actualizar. Guardar fecha/hora y profesional.  
- **Trazabilidad:** En el flujo de **nuevo ingreso**, registrar automáticamente `profesionalTriageId` (usuario logueado) y mostrarlo en el detalle (ej. “Clasificado por: Dr. X – 05/03 14:30”).
- **Beneficio:** Auditoría y cumplimiento de buenas prácticas; re-triage ante deterioro o tiempo prolongado.

### 2.5 Signos vitales seriados en urgencias

- **Idea:** En el panel del paciente (urgencias), sección “Signos vitales” con:  
  - Valores al ingreso (ya existentes).  
  - Botón “Registrar nuevo registro de SV” (fecha/hora + TA, FC, FR, Temp, SpO₂, etc.).  
  - Lista cronológica de registros (tabla o timeline).  
- **Beneficio:** Ver evolución durante la espera; apoyo a re-triage y decisiones clínicas.
- **Implementación:** Backend: entidad o tabla “SignosVitalesUrgencia” (urgenciaRegistroId, fechaHora, valores); endpoint list/create. Front: formulario + lista en el panel derecho.

### 2.6 Checklist de seguridad al asignar atención

- **Idea:** Al pasar paciente a “En atención” (o al abrir evolución), mostrar un mini checklist:  
  - “Identificación del paciente verificada”.  
  - “Alergias revisadas (HC/portal)” con enlace a HC.  
  - “Triage verificado / Re-triage si &gt; X min”.  
- Opcional: no bloquear, pero registrar “checklist cumplido” para auditoría.
- **Beneficio:** Reducción de errores de identificación y de eventos adversos.

### 2.7 Resumen de alta e impreso para el paciente

- **Idea:** Al cambiar estado a **“Alta”**, abrir modal o pantalla con:  
  - Resumen de la atención (motivo, hallazgos, diagnóstico, plan).  
  - Instrucciones de alta (medicamentos, cuidados, signos de alarma, control).  
  - Opción “Generar PDF para el paciente” (similar a evolución clínica) con lenguaje sencillo.
- **Beneficio:** Continuidad del cuidado y menor reconsulta innecesaria; cumplimiento de buenas prácticas de alta.

### 2.8 Notificaciones y sonido para críticos

- **Idea:**  
  - Si hay **nuevo ingreso T I o T II** (o paciente que supera tiempo límite), notificación en pantalla y, si el usuario lo permite, sonido corto.  
  - Preferencia en configuración: “Alertas sonoras en urgencias” (on/off).
- **Implementación:** Comparar lista anterior vs actual en cada refresh; si aparece nuevo T I/II o “fuera de tiempo”, disparar notificación (ej. toast o servicio de notificaciones) y opcionalmente Audio.

### 2.9 Atajos de teclado

- **Idea:** En la vista de urgencias:  
  - `1–5`: filtrar por triage I–V.  
  - `E`: En espera, `A`: En atención, etc.  
  - `Enter` o `Espacio` en paciente seleccionado: abrir evolución o foco en motivo.  
  - `Ctrl+N`: Nuevo ingreso.
- **Beneficio:** Menos clics y mayor velocidad en picos de demanda.

### 2.10 Reporte de cumplimiento de tiempos (Res. 5596/2015)

- **Idea:** Reporte (pantalla + exportación Excel/PDF):  
  - Por periodo (día/semana/mes).  
  - Por nivel de triage: número de pacientes, % atendidos dentro del tiempo límite, tiempo promedio de espera.  
  - Listado de casos que superaron el tiempo (para análisis y mejora).
- **Implementación:** Backend: endpoint con filtros de fecha y agregaciones; front: vista “Reportes” en urgencias o en módulo de reportes global.

---

## 3. Priorización sugerida (orden de impacto / esfuerzo)

| Prioridad | Sugerencia | Impacto | Esfuerzo |
|-----------|------------|---------|----------|
| 1 | Vinculación automática Urgencias ↔ HC/atención | Alto | Medio (backend + flujo) |
| 2 | Alertas por tiempo (75% y superado) + destacar en lista | Alto | Bajo |
| 3 | Trazabilidad de profesional en triage (ingreso) | Medio | Bajo |
| 4 | Tablero dashboard (contadores, fuera de tiempo) | Alto | Medio |
| 5 | Re-triage con registro de fecha/profesional | Medio | Medio |
| 6 | Signos vitales seriados en panel urgencias | Alto | Alto |
| 7 | Alertas por SV anormales (TA, SpO₂, Glasgow) | Medio | Medio |
| 8 | Resumen de alta + PDF para paciente | Medio | Medio |
| 9 | Notificaciones/sonido para T I/II y fuera de tiempo | Medio | Bajo |
| 10 | Atajos de teclado | Medio | Bajo |
| 11 | Checklist de seguridad (identificación, alergias) | Medio | Medio |
| 12 | Reporte de cumplimiento de tiempos (exportable) | Alto (gestión) | Medio |

---

## 4. Conclusión

Las principales **problemáticas** que SESA puede atacar de forma directa son: (1) desconexión entre ingreso a urgencias y Historia Clínica, (2) poca visibilidad del cumplimiento de tiempos de espera y (3) falta de alertas y trazabilidad para triage y signos vitales. Las **sugerencias** anteriores son realizables de forma incremental dentro del ecosistema actual (Res. 5596/2015, Res. 1995/1999, RIPS) y mejoran tanto la seguridad del paciente como la operación y la capacidad de reporte y auditoría.

Si se prioriza, un primer paquete muy efectivo sería: **vinculación automática HC + alertas por tiempo + trazabilidad de triage + dashboard básico**.
