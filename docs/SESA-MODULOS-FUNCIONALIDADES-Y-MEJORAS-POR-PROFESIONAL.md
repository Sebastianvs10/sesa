# Análisis de Módulos SESA por Profesional de Salud (Colombia)
## Funcionalidades, problemáticas y sugerencias de mejora

**Objetivo:** Actuar como los profesionales en salud en Colombia de cada dependencia para verificar las funcionalidades de cada componente de SESA, listarlas y, desde esa perspectiva, identificar problemáticas en la atención de pacientes o en la gestión de procesos, y proponer mejoras e ideas innovadoras.

**Normativa de referencia:** Res. 1995/1999 (historia clínica), Res. 5596/2015 (triage/urgencias), Res. 3374/2000 (RIPS), Res. 2953/2014 (citas), Res. 3380/1981 (consentimiento informado), Ley 1581/2012 (datos personales), Res. 5521/2013 (auditoría), Decreto 1011/2006 (seguridad del paciente).

---

## Parte 1. Módulos SESA y funcionalidades por profesional

### 1. Atención clínica

| Módulo | Profesional(es) típico | Funcionalidades verificadas |
|--------|------------------------|-----------------------------|
| **Dashboard** | Coordinador médico, Admin, Recepcionista | Ver indicadores dinámicos por rol (pacientes, citas, consultas, urgencias, hospitalización, farmacia, facturación); calendario con citas del día; acceso rápido a módulos según permisos. |
| **Pacientes** | Recepcionista, Médico, Admin | Listar pacientes con paginación y búsqueda; filtrar por estado activo/inactivo; crear paciente (nuevo); editar paciente; eliminar (solo Admin); permisos por rol (crear/eliminar). |
| **Historia clínica** | Médico, Odontólogo, Psicólogo (según rol) | Buscar paciente; crear historia/nueva atención; vista unificada: antecedentes, línea de tiempo de atenciones, evoluciones SOAP, órdenes (lab, imágenes, farmacia), documentos, dolores, consentimientos; generar PDF de evolución; flujo clínico integrado (citas, consultas, órdenes, facturas). |
| **Odontología** | Odontólogo | Dashboard con citas del día; lista de citas por estado (agendada, en espera, en atención, atendida, no asistió, cancelada); ficha por paciente: historia, odontograma interactivo (piezas, superficies, tratamientos), procedimientos, plan de tratamiento, imágenes clínicas, evolución; tipos de consulta (primera vez, control, urgencia odontológica, interconsulta). |
| **Evolución de Enfermería** | Enfermero, Jefe de Enfermería, Auxiliar | Módulo definido en roles y sidebar; **ruta no implementada** en `app.routes.ts` — enlace lleva al dashboard. |
| **Urgencias** | Médico, Enfermero, Auxiliar | Listar ingresos por triage (I–V) y estado (en espera, en atención, en observación, alta, hospitalizado, referido); nuevo ingreso con triage, signos vitales, motivo, tipo de llegada; tiempo transcurrido y tiempo restante según Res. 5596/2015; panel médico: evoluciones SOAP, CIE-10; panel enfermería: notas de enfermería; abrir historia clínica; re-triage; checklist (identificación, alergias); resumen/instrucciones de alta; dashboard con contadores; alertas por tiempo; generación PDF. |
| **Hospitalización** | Médico, Enfermero | Listar hospitalizaciones por estado; crear hospitalización (paciente, servicio, cama, estado, evolución diaria, órdenes médicas, epicrisis); formulario básico sin flujo de camas ni planificación. |
| **EBS (Equipos Básicos de Salud)** | Profesional EBS, Coordinador Territorial, Supervisor APS | **Inicio:** territorios asignados (profesional) o dashboard de indicadores (supervisor). **Territorios:** listado, mapa, crear territorio (IGAC), indicadores. **Visitas:** listado, nueva visita domiciliaria. **Asignación:** microterritorios a equipos. **Brigadas, reportes, alertas, dashboard supervisor.** |

### 2. Diagnóstico y tratamiento

| Módulo | Profesional(es) típico | Funcionalidades verificadas |
|--------|------------------------|-----------------------------|
| **Laboratorios** | Bacteriólogo | Dos pestañas: **Solicitudes** (crear solicitud por paciente y tipo de prueba; listar por estado PENDIENTE/EN_PROCESO/COMPLETADO; registrar resultado con plantillas por tipo de prueba o texto libre; observaciones); **Órdenes HC** (órdenes de laboratorio desde historia clínica; mismo flujo de resultado). Tipos de prueba típicos Colombia (hemograma, perfil lipídico, glicemia, etc.). Filtros por estado y búsqueda. |
| **Imágenes diagnósticas** | Médico, técnico/radiology | Buscar por ID de atención; listar estudios por atención; crear estudio (atención, tipo, resultado, URL archivo). Flujo centrado en atención, sin listado global ni integración PACS. |
| **Farmacia** | Regente de Farmacia, Auxiliar | **Órdenes:** listar órdenes pendientes desde HC; dispensar por orden (seleccionar medicamento y cantidad por línea). **Inventario:** listar medicamentos (nombre, lote, vencimiento, cantidad, precio, stock mínimo); alta de medicamento. **Dispensar directo:** dispensación sin orden (paciente, medicamento, cantidad, entregado por). Historial de dispensaciones. |

### 3. Programación y agenda

| Módulo | Profesional(es) típico | Funcionalidades verificadas |
|--------|------------------------|-----------------------------|
| **Citas** | Recepcionista | Crear cita: búsqueda de paciente por identificación → especialidad → profesional (filtrado por rol) → calendario día/hora → slots disponibles; tipo de cita (primera vez, control, etc.), número de autorización, duración (Res. 2953/2014); lista de citas por día con filtros; estados (agendada, confirmada, atendido, cancelada, no asistió). |
| **Agenda** | Coordinador Médico, Jefe de Enfermería | Calendario de turnos por mes; por rol: coordinador gestiona médicos, jefe de enfermería gestiona auxiliares; tipos de turno y servicio clínico; programación en estado BORRADOR/EN_REVISION/PUBLICADO; arrastrar y soltar turnos; límites laborales y festivos Colombia; resumen por profesional. |
| **Consulta médica** | Médico, Jefe de Enfermería | Vista de citas del día del profesional (o filtro por profesional si admin); estados (agendada, confirmada, en espera, en atención, atendida, no asistió, cancelada); estadísticas; acciones: abrir historia clínica, facturación, receta electrónica, videoconsulta; integración con flujo de atención. |

### 4. Gestión y administración

| Módulo | Profesional(es) típico | Funcionalidades verificadas |
|--------|------------------------|-----------------------------|
| **Facturación** | Recepcionista, Admin | Resumen (totales por estado); listar facturas con filtros (estado, fechas, paciente); paginación; crear factura (paciente, datos RIPS/cuentas médicas); estados PENDIENTE, EN_PROCESO, PAGADA, RECHAZADA, ANULADA; generación/exportación RIPS (desde–hasta). |
| **Reportes** | Coordinador médico, Admin | Gráficas: citas por día, consultas por mes, facturación por mes, citas por estado; indicadores de calidad; acceso restringido (ADMIN empresa, SUPERADMINISTRADOR). |
| **Usuarios** | Admin | Listar usuarios; crear/editar usuario (rol, permisos por módulos); roles predefinidos (MEDICO, ENFERMERO, RECEPCIONISTA, etc.). |
| **Personal** | Admin, Coordinador | Gestión del personal de la institución (cargo, rol, vinculación); roles específicos (médico, enfermería, EBS, etc.) para asignación a agenda y citas. |
| **Empresas** | Superadministrador | Listar empresas (multi-tenant); crear/editar empresa (wizard); asignación de módulos y submódulos por empresa. |
| **Mi empresa** | Admin empresa | Configuración de la empresa actual del usuario (datos institucionales). |
| **Notificaciones** | Cualquier usuario | Bandeja tipo correo: inbox, importantes, enviados; categorías (general, urgente, informativo); redactar con editor rich text y adjuntos; envío individual o broadcast; marcar leído/importante/archivar. |
| **Roles** | Superadministrador | Listar roles; editar módulos por rol (grupos: atención clínica, diagnóstico, programación, gestión); metadata por rol (icono, color, descripción). |

### 5. Otros

| Módulo | Usuario | Funcionalidades verificadas |
|--------|--------|-----------------------------|
| **Portal del paciente** | Paciente | Inicio con resumen (historias, órdenes, próxima cita); acceso a historia clínica, laboratorios, órdenes, consentimientos, perfil. |
| **Videoconsulta** | Médico, Paciente | Asistente y sala de videoconsulta (WebRTC). |
| **Receta electrónica** | Farmacia / ciudadano | Ruta pública verificar-receta; modal de receta electrónica. |

---

## Parte 2. Problemáticas por dependencia (visión del profesional)

### 2.1 Médico / Consulta externa

| Problemática | Impacto |
|-------------|---------|
| Historia clínica y consulta en pantallas distintas; muchos clics para abrir HC, atención y evolución. | Menos tiempo con el paciente; registro al final del día, menos fiel. |
| Antecedentes y alergias no siempre visibles en el flujo de prescripción. | Riesgo de reacciones adversas. |
| CIE-10 sin búsqueda asistida; códigos incorrectos o ausentes. | Rechazos RIPS y reportes SISPRO erróneos. |
| Órdenes de lab/imágenes sin resultado visible en la misma atención. | No se cierra el ciclo orden–resultado en la consulta. |
| No hay plantillas SOAP por motivo de consulta; registro desde cero. | Inconsistencia y tiempo perdido. |
| Evolución de Enfermería aparece en menú pero la ruta no existe. | Confusión y enlace roto. |

### 2.2 Enfermería (Urgencias / Hospitalización)

| Problemática | Impacto |
|-------------|---------|
| Ingreso a urgencias no crea automáticamente atención/HC; el médico puede ver “no tiene atención vinculada”. | Retraso para registrar evolución SOAP. |
| Signos vitales solo al ingreso; no hay registro seriado en urgencias. | Se pierde evolución durante la espera; re-triage poco informado. |
| No hay alertas clínicas por SV anormales (TA, SpO₂, Glasgow). | Valores de riesgo pasan desapercibidos. |
| Módulo Evolución de Enfermería sin ruta implementada. | El personal de enfermería no tiene espacio dedicado. |
| Hospitalización: solo formulario básico; no hay gestión de camas ni planificación visual. | Dificulta coordinación de ingresos y altas. |

### 2.3 Bacteriólogo / Laboratorio

| Problemática | Impacto |
|-------------|---------|
| Solicitudes y órdenes HC en dos pestañas separadas; no hay vista unificada por prioridad o fecha. | Puede priorizarse mal en picos de demanda. |
| Resultado se registra con plantilla o texto libre; no hay integración con equipos (interfaz LIS). | Doble digitación y riesgo de error. |
| No hay alertas por resultados críticos (ej. valor fuera de rango que requiera contacto al médico). | Retraso en actuación clínica. |
| No se ve explícitamente la alergia del paciente al registrar resultado (ej. contrastes). | Riesgo en estudios que requieran contraste. |

### 2.4 Regente de Farmacia

| Problemática | Impacto |
|-------------|---------|
| Dispensación por orden sin validación explícita de alergias en el momento de dispensar. | Riesgo si la orden fue prescrita sin cruce. |
| Inventario sin alertas de stock mínimo ni vencimientos próximos en vista principal. | Ruptura de stock o medicamento vencido dispensado. |
| No hay trazabilidad de lote/cantidad dispensada por paciente en reportes fáciles. | Dificulta farmacovigilancia y recall. |
| Dispensación directa (sin orden) no exige motivo ni registro vinculado a atención. | Trazabilidad débil para auditoría. |

### 2.5 Imágenes diagnósticas

| Problemática | Impacto |
|-------------|---------|
| Búsqueda solo por ID de atención; no hay listado global de estudios pendientes o por paciente. | Flujo poco ágil en el día a día. |
| No hay integración PACS; solo URL de archivo. | El radiólogo trabaja en otro sistema; resultado se copia. |
| No hay flujo de “orden de imagen → resultado” vinculado a la atención en HC. | El médico no ve el resultado en la misma atención. |

### 2.6 Recepcionista / Citas y agenda

| Problemática | Impacto |
|-------------|---------|
| Citas y Agenda son dos módulos; la agenda define turnos y la cita asigna slot; no siempre está claro si el slot respeta la agenda publicada. | Sobrecupos o conflictos de horario. |
| No hay recordatorios automáticos al paciente (SMS/email) para confirmar o recordar cita. | Más inasistencia y consultas de confirmación. |
| Tipo de cita y número de autorización (Res. 2953/2014) no se validan contra reglas de la EPS. | Riesgo de glosas. |

### 2.7 Facturación / Cuentas médicas

| Problemática | Impacto |
|-------------|---------|
| Datos RIPS no se capturan en el mismo flujo de la consulta; puede haber doble digitación. | Errores y rechazos de cobro. |
| No hay flujo explícito de glosas (respuesta a rechazos). | Dificulta recuperación de cartera. |
| Resumen por estado existe pero no reportes por período/contrato/CIE-10 exportables. | Menos capacidad de análisis gerencial. |

### 2.8 Odontólogo

| Problemática | Impacto |
|-------------|---------|
| Odontograma e historia en la misma ficha pero sin integración fuerte con órdenes (lab/imágenes) desde la misma vista. | El odontólogo debe cambiar de módulo para ver resultados. |
| Plan de tratamiento no genera recordatorios ni cobro por fase. | Seguimiento y facturación más manual. |
| No hay plantillas por procedimiento (ej. extracción, endodoncia) para evolución. | Tiempo y heterogeneidad en el registro. |

### 2.9 EBS / Atención primaria

| Problemática | Impacto |
|-------------|---------|
| Visitas domiciliarias y territorios bien diferenciados; falta flujo offline robusto para zonas sin conectividad. | Registro retrasado o perdido en campo. |
| Alertas y dashboard supervisor pueden no incluir indicadores de cumplimiento de metas (cobertura, tamizajes). | Menor capacidad de gestión por resultados. |

### 2.10 Coordinación / Admin

| Problemática | Impacto |
|-------------|---------|
| Reportes con gráficas útiles pero sin desglose por profesional/servicio/periodo exportable en un solo lugar. | Análisis requiere cruzar pantallas. |
| Módulo Evolución de Enfermería visible en roles pero sin ruta; roles con ese permiso no pueden usarlo. | Configuración inconsistente. |
| Multi-tenant (empresas) y módulos por empresa bien definidos; falta documentación de operación para el administrador de cada IPS. | Soporte y autogestión más difíciles. |

### 2.11 Paciente (Portal)

| Problemática | Impacto |
|-------------|---------|
| Portal con historia, laboratorios, órdenes y consentimientos; no siempre está claro si el paciente puede descargar resultados en formato estándar (PDF). | Autonomía y portabilidad de datos. |
| No hay recordatorio de citas ni notificaciones desde el portal. | Mayor no asistencia. |

---

## Parte 3. Mejoras y sugerencias innovadoras por dependencia

### 3.1 Médico / Historia clínica y consulta

- **Vista única paciente–atención–evolución:** Una pantalla que integre búsqueda de paciente, atención del día y bloque SOAP/órdenes/antecedentes sin cambiar de módulo.
- **Antecedentes y alergias obligatorios y visibles:** Bloque fijo o colapsable en cada atención; en prescripción, alerta o bloqueo si hay alergia al medicamento.
- **Plantillas SOAP por motivo de consulta:** Catálogo (control, agudo, crónico, preanestésico, etc.) con validación de contenido mínimo antes de guardar.
- **CIE-10 con búsqueda asistida:** Autocompletado por código o descripción (CIE-10 Colombia) y uso directo en RIPS.
- **Órdenes con resultado vinculado:** En la misma atención, pestaña “Resultados” con órdenes pendientes y cargadas; recordatorio de “órdenes sin resultado”.
- **Firma y trazabilidad en cada evolución:** Profesional, fecha/hora (y opcionalmente IP) en cada nota; sin edición sin huella.
- **Resumen de alta y referencia:** Plantilla con motivo, hallazgos, diagnósticos CIE-10, estudios, tratamiento e instrucciones; PDF para paciente o siguiente nivel.
- **Interacciones medicamentosas en prescripción:** Alerta al añadir medicamento que interactúe con otros de la fórmula o del paciente.

### 3.2 Enfermería y Urgencias

- **Vinculación automática Urgencias–HC:** Al registrar ingreso, crear atención (y HC si no existe) y exponer `atencionId` para evolución inmediata.
- **Signos vitales seriados en urgencias:** Sección “Signos vitales” en el panel del paciente con registros repetidos (fecha/hora + TA, FC, FR, Temp, SpO₂, Glasgow) y lista cronológica.
- **Alertas por tiempo y por SV:** Alerta visual al 75% y al 100% del tiempo límite de espera (Res. 5596/2015); reglas de SV (TA, SpO₂, Glasgow) para badge “Alerta SV”.
- **Re-triage con trazabilidad:** Botón “Re-clasificar triage” con registro de profesional y fecha/hora; en ingreso, guardar siempre `profesionalTriageId`.
- **Dashboard de urgencias:** Contadores por estado y triage; pacientes que superan tiempo límite; tiempo promedio de espera; filtros “Solo críticos” y “Solo fuera de tiempo”.
- **Checklist de seguridad al asignar atención:** Verificación de identificación, alergias y triage antes o al abrir evolución; registro para auditoría.
- **Implementar ruta y pantalla de Evolución de Enfermería:** Módulo dedicado para notas de enfermería (evolución de enfermería) con permisos ya definidos en roles.

### 3.3 Laboratorio

- **Vista unificada solicitudes + órdenes HC:** Una sola lista ordenable por prioridad/fecha con indicador de origen (solicitud vs orden HC).
- **Alertas de resultados críticos:** Al guardar resultado, si el valor está fuera de rango crítico definido por prueba, marcar y opcionalmente notificar al médico o al paciente.
- **Integración LIS (opcional):** Recepción de resultados desde equipos o LIS para reducir digitación.
- **Alergias visibles al registrar resultado:** Mostrar alergias del paciente en el formulario de resultado (útil para estudios con contraste).

### 3.4 Farmacia

- **Alertas en dispensación:** Al dispensar por orden, mostrar alergias del paciente y alertar si el medicamento está contraindicado.
- **Inventario: alertas de stock y vencimiento:** Vista principal con indicadores de stock bajo y próximos vencimientos; recordatorios configurables.
- **Trazabilidad lote por paciente:** Registro de lote y cantidad por dispensación; reporte o filtro por paciente para farmacovigilancia y recall.
- **Dispensación directa con motivo y vinculación:** Campo motivo y opcionalmente vinculación a atención para auditoría.

### 3.5 Imágenes diagnósticas

- **Listado global de estudios:** Por paciente, por estado (pendiente/en proceso/informado) y por fecha; búsqueda por documento o nombre.
- **Flujo orden → resultado en HC:** Órdenes de imagen desde la atención; registro de resultado vinculado y visible en la misma atención y en la HC.
- **Integración PACS (medio/largo plazo):** Enlace o embedding de visor PACS; resultado/informe asociado a la orden en SESA.

### 3.6 Citas y Agenda

- **Unificación lógica Agenda–Citas:** Los slots de citas se calculan a partir de la agenda publicada del profesional; sin slots fuera de turno.
- **Recordatorios al paciente:** SMS o email automático (configurable) X horas/días antes de la cita; opción de confirmación o cancelación desde enlace.
- **Validación de autorización y tipo de cita:** Reglas configurables por EPS/contrato (número de autorización, tipo primera vez/control) para reducir glosas.

### 3.7 Facturación

- **Captura RIPS en el flujo de consulta:** Campos RIPS en atención/evolución; validación al cerrar atención o al generar RIPS.
- **Gestión de glosas:** Flujo de “rechazo → respuesta con documentos” y seguimiento por factura.
- **Reportes exportables:** Por período, contrato, CIE-10, profesional; Excel/PDF para gerencia y auditoría.

### 3.8 Odontología

- **Integración órdenes/resultados en ficha:** En la misma ficha paciente, ver órdenes de lab/imagen y resultados sin salir del módulo.
- **Plantillas por procedimiento:** Plantillas de evolución por tipo (extracción, endodoncia, etc.) para homogeneizar y ahorrar tiempo.
- **Plan de tratamiento con recordatorios y fases:** Recordatorios por fase y opción de cobro o facturación por fase.

### 3.9 EBS

- **Modo offline para visitas:** Sincronización cuando hay conexión; cola de envío y resolución de conflictos para zonas sin cobertura.
- **Indicadores de cumplimiento en dashboard supervisor:** Cobertura, tamizajes, metas por territorio con alertas de desvío.

### 3.10 Coordinación y producto

- **Reportes consolidados exportables:** Un solo lugar para elegir indicadores, período, desglose (profesional, servicio, sede) y exportar (Excel/PDF).
- **Consistencia módulos–rutas–permisos:** Implementar la ruta de Evolución de Enfermería o quitarla del menú y de la asignación de roles hasta que exista.
- **Documentación para administrador de IPS:** Guía de configuración (empresas, módulos, roles, flujos recomendados) para autogestión y soporte.

### 3.11 Portal del paciente

- **Descarga de resultados en PDF:** En laboratorios y órdenes, opción “Descargar resultado” en PDF estándar.
- **Recordatorios y notificaciones de cita:** Notificación de cita próxima y, si se implementa, confirmación/cancelación desde el portal o enlace.

---

## Parte 4. Priorización sugerida (impacto / esfuerzo)

| Prioridad | Mejora | Dependencia | Impacto | Esfuerzo | Estado |
|-----------|--------|-------------|---------|----------|--------|
| 1 | Antecedentes y alergias visibles + alerta en prescripción y dispensación | Médico, Farmacia | Muy alto (seguridad) | Medio | **Implementado:** alerta en HC al prescribir; en Farmacia se muestran alergias del paciente (HC) al dispensar por orden y alertas de stock bajo/vencimiento en inventario. |
| 2 | Vinculación automática Urgencias ↔ HC/atención | Enfermería, Médico | Alto | Medio | Ya estaba implementado en backend (creación automática de HC y atención al ingreso). |
| 3 | Implementar ruta y pantalla Evolución de Enfermería | Enfermería | Alto | Medio | **Implementado:** ruta `/evolucion-enfermeria`, pantalla para consultar y registrar notas por ID de atención. |
| 4 | Firma y trazabilidad en evoluciones | Médico, Auditoría | Alto | Bajo | Ya implementado (profesionalId/profesionalNombre en DTO y vista HC). |
| 5 | CIE-10 con búsqueda asistida y uso en RIPS | Médico, Facturación | Alto | Medio | Parcial (datalist CIE-10 frecuentes en HC y urgencias). |
| 6 | Vista única paciente–atención–evolución (menos clics) | Médico | Muy alto (UX) | Alto | Pendiente. |
| 7 | Órdenes (lab/imágenes) con resultado vinculado en atención | Médico, Lab, Imágenes | Alto | Medio | Parcial (lab e imágenes vinculados a atención; mejorar visibilidad en HC). |
| 8 | Dashboard y alertas por tiempo en urgencias | Enfermería, Coordinación | Alto | Medio | Ya implementado (dashboard backend + front, alertas por tiempo). |
| 9 | Signos vitales seriados en urgencias | Enfermería | Alto | Medio | Pendiente. |
| 10 | Plantillas SOAP por motivo de consulta | Médico | Alto | Medio | Pendiente. |
| 11 | Alertas stock y vencimiento en farmacia | Farmacia | Medio–Alto | Bajo | **Implementado:** alertas en pestaña Inventario (stock ≤ mínimo, vencen en 30 días). |
| 12 | Unificación Agenda–Citas y recordatorios al paciente | Recepcionista, Paciente | Alto | Medio | Pendiente. |
| 13 | Reportes consolidados exportables | Admin, Coordinación | Medio–Alto | Medio | **Implementado:** botón «Exportar CSV» con citas por día, consultas por mes, facturación, citas por estado e indicadores de calidad. |
| 14 | Captura RIPS en flujo de consulta | Facturación, Médico | Alto | Medio | Parcial (campos en atención/evolución). |
| 15 | Listado global y flujo orden–resultado en imágenes | Imágenes, Médico | Medio | Medio | Pendiente. |

---

## Conclusión

SESA cubre los flujos principales de una IPS en Colombia: **atención clínica** (historia clínica, urgencias, hospitalización, odontología, EBS), **diagnóstico y tratamiento** (laboratorio, imágenes, farmacia), **programación** (citas, agenda, consulta médica) y **gestión** (facturación, reportes, usuarios, personal, empresas, notificaciones, roles). Las funcionalidades están alineadas con los roles típicos (médico, enfermero, bacteriólogo, regente de farmacia, recepcionista, etc.).

Las **problemáticas** más recurrentes son: fragmentación de la información (varias pantallas para un mismo flujo), falta de alertas en puntos críticos (alergias, tiempos de espera, stock, resultados críticos), módulos referenciados pero no implementados (Evolución de Enfermería), y debilidad en trazabilidad y reportes exportables.

Las **mejoras propuestas** priorizan seguridad del paciente (alergias, SV, checklist), cumplimiento normativo (RIPS, Res. 5596/2015, Res. 1995/1999) y experiencia del profesional (vista única, plantillas, menos clics). Un **primer paquete** muy efectivo sería: **alergias visibles y alerta en prescripción/dispensación**, **vinculación automática Urgencias–HC**, **implementación de Evolución de Enfermería**, **firma y trazabilidad en evoluciones** y **CIE-10 asistido**. Con esto se gana en seguridad, cumplimiento y base para RIPS y auditoría.
