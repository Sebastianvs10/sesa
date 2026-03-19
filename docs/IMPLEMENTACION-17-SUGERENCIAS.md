# Plan de implementación — 17 sugerencias innovadoras SESA

**Objetivo:** Implementar las 17 sugerencias del documento SESA-SUGERENCIAS-INNOVADORAS-SALUD-DIGITAL.md en backend (Java/Spring) y frontend (Angular) de forma limpia y profesional, con guía de pruebas para cada una.

**Convenciones:**
- Todas las entidades JPA usan schema por tenant (multi-tenant).
- DTOs en `dto/`, controladores en `controller/`, servicios en `service/` e `impl/`.
- Frontend: servicios en `core/services/`, componentes en `features/` o `shared/`.
- Autor en código: Ing. J Sebastian Vargas S (según reglas del proyecto).

---

## S1. Score de riesgo en cabecera de la HC

### Descripción
Indicador visual de riesgo por paciente calculado con: alergias no reconciliadas, polifarmacia, resultados críticos pendientes de revisión, controles vencidos, visitas recientes a urgencias.

### Backend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| DTO | `dto/PacienteRiesgoDto.java` | `nivelRiesgo` (BAJO/MEDIO/ALTO), `puntos` (int), `factores` (List&lt;String&gt;), `recomendaciones` (List&lt;String&gt;) |
| Service | `ReporteService` + `ReporteServiceImpl` | Método `getRiesgoPaciente(Long pacienteId)`: consultar HC (alergias), órdenes con resultado crítico no leído, consultas recientes (urgencias), medicamentos activos; reglas de puntuación; devolver DTO |
| Controller | `ReporteController` o `HistoriaClinicaController` | `GET /reportes/paciente/{pacienteId}/riesgo` o `GET /historia-clinica/paciente/{pacienteId}/riesgo` → `PacienteRiesgoDto` |
| Schema | No nueva tabla | Usa tablas existentes (historias_clinicas, ordenes_clinicas, consultas, etc.) |

### Frontend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Service | `core/services/reporte.service.ts` o `historia-clinica` | Método `getRiesgoPaciente(pacienteId): Observable<PacienteRiesgoDto>` |
| Interface | `core/services/reporte.service.ts` | `PacienteRiesgoDto` con nivelRiesgo, puntos, factores, recomendaciones |
| Componente | Historia clínica (banner) | En `hc-patient-banner`, llamar al endpoint al tener `selectedPatient()`; mostrar badge o tarjeta "Riesgo: BAJO/MEDIO/ALTO" con tooltip con factores y recomendaciones |
| Estilos | `historia-clinica.page.scss` | Clases `.hc-riesgo-badge`, `.hc-riesgo--bajo`, `.hc-riesgo--medio`, `.hc-riesgo--alto` |

### Cómo probar S1
1. **Datos de prueba:** Paciente con alergias en HC, al menos una orden de laboratorio con resultado y (opcional) una orden marcada como resultado crítico no leído.
2. **Flujo:** Abrir Historia Clínica → buscar y seleccionar paciente.
3. **Verificar:** En el banner del paciente (derecha o debajo del nombre) aparece el indicador de riesgo (ej. "Riesgo MEDIO" con icono). Al pasar el mouse, se ven factores y recomendaciones.
4. **API directa:** `GET /api/reportes/paciente/{id}/riesgo` con token de sesión → respuesta 200 con `nivelRiesgo`, `puntos`, `factores`.

---

## S2. Alertas de resultados críticos con trazabilidad de lectura

### Descripción
Al registrar resultado crítico (rangos configurables): marcar orden, notificar, registrar quién y cuándo leyó el resultado.

### Backend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Tabla | `tenant_schema.sql` + migración | `resultado_critico_lectura` (id, orden_clinica_id, personal_id, leido_at, ip_opcional) |
| Entidad | `entity/ResultadoCriticoLectura.java` | JPA, relación a OrdenClinica y Personal |
| Tabla config | `rango_critico_prueba` (opcional) o config en properties | tipo_prueba (ej. GLICEMIA), valor_min, valor_max; si resultado fuera de rango → crítico |
| Repository | `ResultadoCriticoLecturaRepository` | findByOrdenClinicaId, existsByOrdenClinicaIdAndPersonalId |
| Service | `OrdenClinicaService` / `ResultadoCriticoService` | Al guardar resultado de orden: si es laboratorio y valor fuera de rango configurado, setear `resultado_critico=true` en orden y crear notificación. Endpoint `POST /ordenes-clinica/{id}/marcar-resultado-leido` (personalId desde token) |
| Controller | `OrdenClinicaController` | `PUT /ordenes-clinica/{id}/marcar-resultado-leido` → registra lectura |
| Notificación | Reutilizar `Notificacion` | Tipo `RESULTADO_CRITICO`, destinatario médico de la consulta o config |

### Frontend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Service | `orden-clinica.service.ts` | `marcarResultadoLeido(ordenId): Observable<void>` |
| HC / Órdenes | Lista de órdenes y drawer detalle | Si orden tiene `resultadoCritico === true` y no leído por el usuario actual, mostrar badge "Crítico - No leído"; botón "Marcar como leído" que llama al endpoint |
| Notificaciones | Bandeja de notificaciones | Mostrar notificaciones tipo RESULTADO_CRITICO con enlace a la orden |

### Cómo probar S2
1. **Configurar rango crítico:** Ej. Glicemia &lt; 70 o &gt; 400 (en config o en BD).
2. **Registrar resultado:** En Laboratorio, registrar resultado de laboratorio para una orden con valor fuera de rango (ej. Glicemia 450). Guardar.
3. **Verificar:** La orden aparece con badge "Resultado crítico"; en la bandeja del médico llega notificación.
4. **Marcar leído:** En Historia Clínica → Órdenes → abrir detalle de esa orden → "Marcar como leído". Recargar: el badge desaparece para ese usuario y en BD existe registro en `resultado_critico_lectura`.
5. **API:** `PUT /api/ordenes-clinica/{id}/marcar-resultado-leido` con token → 200.

---

## S3. Recordatorios y confirmación de cita por enlace

### Descripción
Recordatorio automático con enlace para que el paciente confirme o cancele/reagende la cita.

### Backend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Tabla | `citas` (existente) | Añadir `token_confirmacion` (VARCHAR 64, único), `confirmado_at`, `cancelado_desde_enlace_at` |
| Entidad | `Cita.java` | Campos tokenConfirmacion, confirmadoAt, canceladoDesdeEnlaceAt |
| Service | `CitaService` | `generarTokenConfirmacion(citaId)`, `confirmarCitaPorToken(token)`, `cancelarCitaPorToken(token, motivo)`. En recordatorio incluir URL: `{portalUrl}/cita/confirmar?t={token}` y `/cita/cancelar?t={token}` |
| Controller público | `CitaController` o `CitaPublicoController` | `GET /cita/confirmar?t={token}` (sin auth) → actualiza cita a CONFIRMADA, devuelve HTML/JSON; `GET/POST /cita/cancelar?t={token}` → estado CANCELADA, opcional motivo. Validar token y vigencia (ej. 7 días) |
| Recordatorio | `RecordatorioCitaServiceImpl` | En el contenido del recordatorio (notificación in-app y si hay email) incluir enlaces con token. Al crear recordatorio, generar y guardar token en cita si no existe |

### Frontend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Rutas públicas | `app.routes.ts` | Ruta pública `cita/confirmar`, `cita/cancelar` (sin guard de auth) |
| Componente | `features/cita/cita-confirmar.component.ts` | Lee queryParam `t`, llama `GET /api/cita/confirmar?t=...` (o POST con token). Muestra mensaje "Cita confirmada" o error (token inválido/caducado) |
| Componente | `features/cita/cita-cancelar.component.ts` | Formulario opcional motivo, envía cancelación. Mensaje "Cita cancelada" |
| Portal paciente | Opcional | En "Mis citas", botón "Confirmar asistencia" que usa el mismo token o sesión |

### Cómo probar S3
1. Crear cita para un paciente con usuario vinculado (para que reciba recordatorio).
2. Ejecutar job de recordatorios (o simular: generar token para esa cita y enviar notificación con enlace).
3. Abrir en navegador (incógnito): `http://localhost:4200/cita/confirmar?t=TOKEN_GENERADO`. Verificar que la cita pase a CONFIRMADA y se muestre mensaje de éxito.
4. Repetir con otra cita y `.../cita/cancelar?t=TOKEN`; opcional enviar motivo. Verificar estado CANCELADA.
5. Probar token inválido o ya usado → mensaje de error.

---

## S4. Panel de cumplimiento normativo

### Descripción
Dashboard: % RDA enviado, % urgencias dentro de tiempo por triage, % HC con CIE-10 y evolución &lt; 24 h, indicadores 0256, resultados críticos no leídos. Filtros por período y profesional; exportar.

### Backend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| DTO | `dto/CumplimientoNormativoDto.java` | Por ejemplo: porcentajeRdaEnviado, porcentajeUrgenciasEnTiempo, porcentajeHcConCie10YEvolucion24h, totalResultadosCriticosNoLeidos, listaIndicadores0256, periodoInicio, periodoFin |
| Service | `ReporteService` | Métodos que consulten: atenciones con RDA enviado vs total; urgencias por triage y tiempo; consultas con CIE-10 y fecha evolución; contar órdenes resultado_critico=true sin lectura. Agregar `getCumplimientoNormativo(LocalDate desde, LocalDate hasta, Long profesionalId)` |
| Controller | `ReporteController` | `GET /reportes/cumplimiento-normativo?desde=&hasta=&profesionalId=` → CumplimientoNormativoDto |
| Export | Mismo controller o PdfController | `GET /reportes/cumplimiento-normativo/export?formato=csv|pdf&desde=&hasta=` |

### Frontend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Service | `reporte.service.ts` | `getCumplimientoNormativo(params)`, `exportCumplimientoNormativo(params, formato)` |
| Página | `features/reportes/` o nueva `cumplimiento-normativo` | Filtros: fecha desde/hasta, profesional (opcional). Tarjetas con porcentajes y tabla indicadores. Botón "Exportar CSV/PDF" |
| Ruta | `app.routes.ts` | Ruta protegida (Admin, Coordinador) `/reportes/cumplimiento-normativo` |

### Cómo probar S4
1. Con usuario Admin o Coordinador, ir a Reportes → Cumplimiento normativo (o ruta equivalente).
2. Seleccionar período (ej. último mes). Verificar que se muestren porcentajes y números coherentes con los datos (ej. si hay 10 atenciones y 8 con RDA enviado, 80%).
3. Filtrar por profesional: comprobar que los datos se acotan.
4. Exportar CSV y PDF: comprobar que se descargan y el contenido es correcto.
5. API: `GET /api/reportes/cumplimiento-normativo?desde=2025-01-01&hasta=2025-12-31` → 200 con DTO.

---

## S5. Reconciliación de medicamentos y alergias por atención

### Descripción
Al abrir una atención, bloque "Reconciliación": medicamentos referidos vs HC, alergias referidas vs registradas; el profesional confirma o actualiza; queda registro de reconciliado por profesional y fecha.

### Backend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Tabla | `tenant_schema.sql` | `reconciliacion_atencion` (id, atencion_id, profesional_id, medicamentos_referidos TEXT, medicamentos_hc TEXT, alergias_referidas TEXT, alergias_hc TEXT, reconciliado_at, observaciones TEXT) |
| Entidad | `entity/ReconciliacionAtencion.java` | JPA, relaciones a Consulta (atención) y Personal |
| Repository | `ReconciliacionAtencionRepository` | findByAtencionId (consulta_id) |
| DTO | `ReconciliacionAtencionDto.java`, `ReconciliacionAtencionRequestDto.java` | Request: medicamentosReferidos (List), alergiasReferidas (List), observaciones. Response: incluye lo guardado + medicamentosHc, alergiasHc (desde HC) |
| Service | `ReconciliacionService` | Al obtener/crear: cargar HC del paciente (medicamentos actuales, alergias). Guardar reconciliación vinculada a consulta_id. Opcional: actualizar HC si el profesional agrega alergia/medicamento desde reconciliación |
| Controller | `ConsultaController` o `ReconciliacionController` | `GET /consultas/{consultaId}/reconciliacion` → DTO o 404. `POST /consultas/{consultaId}/reconciliacion` → body Request, guardar y devolver DTO |

### Frontend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Service | `consulta.service.ts` o `reconciliacion.service.ts` | `getReconciliacion(consultaId)`, `guardarReconciliacion(consultaId, body)` |
| Componente / bloque | Historia clínica — flujo SOAP | Al abrir pestaña Nota SOAP (o al cargar atención del día), mostrar bloque "Reconciliación de medicamentos y alergias" antes del formulario SOAP: dos columnas (Referidos por paciente / En HC), campos editables para referidos, botón "Confirmar reconciliación". Si ya existe reconciliación, mostrar "Reconciliado el {fecha} por {nombre}" y opción "Editar" si política lo permite |
| Validación | Opcional | Recordatorio o bloqueo suave antes de guardar nota SOAP si no hay reconciliación guardada (configurable) |

### Cómo probar S5
1. Abrir Historia Clínica de un paciente con HC que tenga alergias y/o antecedentes farmacológicos.
2. Iniciar o abrir una atención (consulta del día). En la vista SOAP debe aparecer el bloque de Reconciliación con columnas "Referido" y "En HC" prellenadas desde la HC.
3. Editar "Medicamentos referidos" o "Alergias referidas" y pulsar "Confirmar reconciliación". Verificar que se guarda y se muestra "Reconciliado el ... por ...".
4. Crear otra atención (otro día): el bloque debe mostrarse de nuevo; la anterior sigue guardada para la consulta anterior.
5. API: `GET /api/consultas/{id}/reconciliacion` y `POST /api/consultas/{id}/reconciliacion` con body → 200.

---

## S6. Alta / referencia con checklist y PDF para el paciente

### Descripción
Al dar alta (urgencias) o referencia (consulta): checklist (diagnóstico, tratamiento, recomendaciones, próxima cita); generación de PDF en lenguaje claro para el paciente; envío por correo o descarga.

### Backend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| DTO | `AltaReferenciaDto.java`, `AltaReferenciaRequestDto.java` | Diagnóstico, tratamiento, recomendaciones, proximaCita, motivoReferencia, nivelReferencia, etc. |
| Service | `PdfService` o `AltaReferenciaService` | Método que genere PDF de resumen de alta/referencia (usar plantilla o existente en sesa-jspdf). Inyectar en servicio de urgencias y consulta |
| Controller | `UrgenciaRegistroController` | `POST /urgencias/registro/{id}/alta` con body (checklist + texto). Actualiza estado a ALTA, genera PDF, opcional envía correo. `GET /urgencias/registro/{id}/alta/pdf` para descarga |
| Controller | `ConsultaController` o nuevo | `POST /consultas/{id}/referencia` con body. Genera PDF referencia. `GET /consultas/{id}/referencia/pdf` |

### Frontend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Modal / pantalla | Urgencias | Al cambiar estado a "Alta", abrir modal con checklist (diagnóstico, tratamiento, recomendaciones, próxima cita) y botón "Generar PDF para paciente" y "Cerrar alta" |
| Modal | Historia clínica o consulta | Botón "Generar referencia" que abra formulario (motivo, nivel, datos) y al guardar genere PDF y ofrezca descarga/enviar por correo |
| Servicios | `urgencia.service.ts`, `consulta.service.ts` | Métodos para enviar alta/referencia y para obtener PDF |

### Cómo probar S6
1. Urgencias: ingresar paciente, luego cambiar estado a "Alta". Completar modal de alta con diagnóstico y recomendaciones; generar PDF. Verificar descarga y contenido del PDF.
2. Consulta: desde una atención, usar "Generar referencia", llenar datos, guardar. Descargar PDF de referencia y verificar que incluya motivo y nivel.
3. API: `POST /api/urgencias/registro/{id}/alta` y `GET .../alta/pdf` → 200 y PDF.

---

## S7. Historial portátil (PHR) descargable FHIR/PDF

### Descripción
El paciente (o el profesional con consentimiento) puede descargar un paquete de su información en FHIR R4 o PDF estructurado (antecedentes, alergias, últimas atenciones, medicamentos, resultados recientes).

### Backend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Service | Reutilizar/ampliar `RdaGeneratorService` o nuevo `PhrService` | Generar Bundle FHIR "document" tipo RDA-Paciente ampliado (Patient, Conditions, Medications, Observations desde últimas atenciones y órdenes con resultado). Alternativa: PDF con mismo contenido estructurado |
| Controller | `PortalController` o `HistoriaClinicaController` | `GET /portal/paciente/{pacienteId}/phr?formato=fhir|pdf` (autenticado como paciente o como profesional con permiso). Validar que el usuario tenga derecho a ese paciente |
| Seguridad | Solo paciente dueño o profesional con rol que permita ver HC | PreAuthorize o validación en servicio |

### Frontend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Portal paciente | Perfil o sección "Mis datos de salud" | Botón "Descargar mi historial (PDF)" y "Descargar mi historial (FHIR)" que llamen al endpoint con formato elegido |
| Historia clínica (profesional) | Banner o pestaña Documentos | Botón "Descargar historial portátil para el paciente" (PDF/FHIR) para llevar a otra IPS |

### Cómo probar S7
1. Como paciente en el portal: ir a "Mis datos de salud" o perfil, descargar PDF y FHIR. Verificar que el PDF contenga antecedentes, alergias, resumen de atenciones recientes.
2. Como médico en HC: abrir paciente, usar botón de descarga de historial portátil; verificar que el archivo corresponde al paciente.
3. API: `GET /api/portal/paciente/{id}/phr?formato=pdf` con token → 200 y application/pdf.

---

## S8. Sugerencia de CIE-10 y códigos RIPS por contexto

### Descripción
A partir del motivo de consulta y del texto del análisis/diagnóstico, sugerir códigos CIE-10 (y si aplica procedimientos RIPS) por búsqueda semántica o reglas.

### Backend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Catálogo | Tabla o archivo estático | CIE-10 Colombia (código, descripción). Si ya existe en proyecto, reutilizar |
| Service | `Cie10SugerenciaService` | Método `sugerir(motivoConsulta, textoAnalisis)`: búsqueda por palabras clave en descripción CIE-10; devolver lista de DTO (código, descripción, relevancia). Opcional: cache |
| Controller | `ConsultaController` o `Cie10Controller` | `GET /cie10/sugerir?motivo=&texto=` → List&lt;Cie10SugerenciaDto&gt; |
| DTO | `Cie10SugerenciaDto` | codigo, descripcion, relevancia (score o orden) |

### Frontend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Service | `cie10.service.ts` o en consulta | `sugerirCie10(motivo, texto): Observable<Cie10SugerenciaDto[]>` |
| Componente | Formulario SOAP / Análisis | Junto al campo de diagnóstico o CIE-10: al escribir motivo o análisis, llamar al endpoint (debounced) y mostrar dropdown de sugerencias; al elegir una, rellenar código y descripción |

### Cómo probar S8
1. En Nota SOAP, escribir en "Motivo de consulta" por ejemplo "dolor lumbar" o "control diabetes". En "Diagnóstico" escribir "lumbalgia". Verificar que aparezcan sugerencias de CIE-10 (ej. M54.5). Seleccionar una y comprobar que se rellena el código en el formulario.
2. API: `GET /api/cie10/sugerir?motivo=dolor+lumbar&texto=lumbalgia` → 200 con lista de sugerencias.

---

## S9. Gestión de glosas y recuperación de cartera

### Descripción
Módulo para registrar rechazos de factura (glosa), adjuntar documentos, enviar respuesta y dar seguimiento; reporte de recuperación de cartera por período y contrato.

### Backend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Tablas | `tenant_schema.sql` | `glosas` (id, factura_id, motivo_rechazo, estado PENDIENTE/ENVIADO/ACEPTADO/RECHAZADO, fecha_registro, fecha_respuesta, observaciones, creado_por_id). `glosa_adjuntos` (id, glosa_id, nombre_archivo, tipo, url_o_blob) |
| Entidad | `Glosa.java`, `GlosaAdjunto.java` | JPA |
| Repository | `GlosaRepository` | findByFacturaId, findByEstado, findByPeriodo |
| Service | `GlosaService` | CRUD glosa, adjuntar archivo, cambiar estado, listar por factura y por filtros |
| Controller | `GlosaController` | REST: list por facturaId o por filtros (estado, desde, hasta); create; update; upload adjunto; reporte recuperación (agregados por período/contrato) |
| Reporte | `ReporteService` | Método recuperacionCartera(desde, hasta, contratoId opcional) → DTO con totalGlosas, totalRecuperado, porEstado |

### Frontend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Módulo / ruta | `features/facturacion/` o `features/glosas/` | Ruta `/facturacion/glosas` o `/glosas` |
| Lista | Componente glosas | Tabla de glosas con filtros (estado, fecha, factura). Desde factura: botón "Registrar glosa" si factura está rechazada |
| Formulario | Alta/edición glosa | Motivo rechazo, estado, subida de adjuntos, observaciones. Botón "Enviar respuesta" |
| Reporte | Pestaña o página | "Recuperación de cartera": período, contrato; tabla y totales; exportar CSV/PDF |
| Service | `glosa.service.ts` | CRUD, upload adjunto, reporte |

### Cómo probar S9
1. Crear una factura en estado RECHAZADA (o simular). Desde detalle de factura, "Registrar glosa": completar motivo, adjuntar PDF, guardar. Cambiar estado a ENVIADO. Verificar listado de glosas y reporte de recuperación.
2. API: `POST /api/glosas`, `GET /api/glosas?facturaId=1`, `GET /api/reportes/recuperacion-cartera?desde=&hasta=`.

---

## S10. Cuestionarios pre-consulta (ePRO)

### Descripción
Antes de la cita el paciente recibe un cuestionario (motivo en sus palabras, escalas EVA, medicamentos, alergias). Las respuestas se guardan como "datos aportados por el paciente" y prellenan motivo/Subjetivo en la nota SOAP.

### Backend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Tabla | `tenant_schema.sql` | `cuestionario_preconsulta` (id, cita_id, paciente_id, motivo_palabras TEXT, dolor_eva INT, ansiedad_eva INT, medicamentos_actuales TEXT, alergias_referidas TEXT, enviado_at, created_at) |
| Entidad | `CuestionarioPreconsulta.java` | JPA |
| Repository | `CuestionarioPreconsultaRepository` | findByCitaId, findByPacienteId |
| Service | `CuestionarioPreconsultaService` | create (desde portal o enlace), getByCitaId. Al crear consulta/atención, si existe cuestionario para la cita, prellenar motivoConsulta y soapS desde cuestionario |
| Controller | `PortalController` o `CitaPublicoController` | `POST /portal/cita/{citaId}/cuestionario-preconsulta` (con token de cita o sesión paciente). `GET /consultas/{consultaId}/cuestionario-preconsulta` para que el médico vea lo enviado |

### Frontend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Portal / enlace | Página pública o portal paciente | Formulario: motivo en sus palabras, escala dolor 0-10, medicamentos actuales, alergias. Envío vinculado a cita_id (por token o sesión). Mensaje "Gracias, sus respuestas serán vistas por su médico" |
| Historia clínica | Al abrir atención con cita | Si la cita tiene cuestionario preconsulta, mostrar bloque "Datos aportados por el paciente" y prellenar motivo y subjetivo con esos datos; opción "Usar en nota SOAP" |

### Cómo probar S10
1. Crear cita; generar enlace o entrar como paciente al cuestionario pre-consulta para esa cita; completar y enviar.
2. En consulta médica, abrir la atención de esa cita. Verificar que aparezcan "Datos aportados por el paciente" y que al guardar nota SOAP el motivo/subjetivo puedan llevar ese contenido.
3. API: `POST /api/portal/cita/{id}/cuestionario-preconsulta` con body → 200.

---

## S11. RDA de urgencias y hospitalización (Res. 1888/2025)

### Descripción
Extender el generador FHIR para emitir RDA de urgencias y RDA de hospitalización además de consulta externa y paciente.

### Backend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Perfiles | Revisar Guía RDA CO | Perfiles StructureDefinition para RDA-Urgencias y RDA-Hospitalizacion |
| Service | `RdaGeneratorService` / `FhirMapperService` | Métodos `generarBundleRdaUrgencias(urgenciaRegistroId)`, `generarBundleRdaHospitalizacion(hospitalizacionId)`. Mapear entidades UrgenciaRegistro y Hospitalizacion a Composition y recursos FHIR según perfil |
| RdaEnvio | TipoRda | Añadir URGENCIAS, HOSPITALIZACION si no existen |
| Controller | `RdaController` | `POST /rda/generar/urgencia/{urgenciaRegistroId}` y `.../hospitalizacion/{hospitalizacionId}`. Mismo flujo de envío al Ministerio que consulta |

### Frontend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Urgencias | Panel de detalle del ingreso | Botón "Generar y enviar RDA (Urgencias)" que llame al nuevo endpoint |
| Hospitalización | Detalle de hospitalización | Botón "Generar y enviar RDA (Hospitalización)" |
| Servicio RDA | `rda.service.ts` | Métodos `generarRdaUrgencia(urgenciaRegistroId)`, `generarRdaHospitalizacion(hospitalizacionId)` |

### Cómo probar S11
1. Tener un ingreso a urgencias con datos mínimos. En detalle del ingreso, pulsar "Generar y enviar RDA". Verificar que se genera el Bundle y (si está configurado) se envía al Ministerio; en BD existe RdaEnvio con tipo URGENCIAS.
2. Igual para una hospitalización: generar RDA tipo HOSPITALIZACION.
3. API: `POST /api/rda/generar/urgencia/1` → 200 con id RDA y estado.

---

## S12. API abierta para integradores (laboratorio, PACS, dispositivos)

### Descripción
API REST documentada (OpenAPI) con autenticación por API Key para que sistemas externos envíen resultados de laboratorio, informes de imagen o signos vitales a una orden o atención.

### Backend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Tabla | `api_keys` (por tenant o global) | id, nombre_integrador, api_key_hash, permisos (LABORATORIO, IMAGEN, SIGNOS_VITALES), activo, created_at |
| Filtro / Interceptor | Spring | Filtro que lea header `X-API-Key` o `Authorization: ApiKey xxx`; valide contra api_keys y establezca contexto (tenant, permisos). Endpoints bajo `/api/integracion/` o `/api/v1/external/` |
| Controller | `IntegracionController` o `ExternalApiController` | `POST /integracion/ordenes/{ordenId}/resultado` (body: resultado texto o JSON estructurado). `POST /integracion/atenciones/{atencionId}/signos-vitales` (body: TA, FC, etc.). Documentar en OpenAPI con seguridad ApiKey |
| Service | Delegar a OrdenClinicaService, ConsultaService | Validar que la orden exista y pertenezca al tenant; guardar resultado. Para signos vitales, crear registro asociado a la atención |

### Frontend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Admin | Configuración empresa o módulo Integraciones | Pantalla para crear/editar API Keys (nombre, permisos). Mostrar clave solo al crear; no almacenar en claro en front, solo enviar al backend que guarda hash) |
| Documentación | Swagger/OpenAPI | Incluir en el mismo Swagger los endpoints de integración y el esquema de seguridad API Key. Enlace desde la app "Documentación API integradores" |

### Cómo probar S12
1. Como admin, crear una API Key con permiso LABORATORIO. Copiar la clave.
2. Con Postman o curl: `POST /api/integracion/ordenes/1/resultado` con header `X-API-Key: CLAVE` y body `{"resultado":"Hemoglobina: 14 g/dL"}`. Verificar que la orden 1 tenga el resultado guardado y que sin API Key o con clave inválida devuelva 401.
3. Revisar Swagger: los endpoints de integración deben aparecer y permitir probar con API Key.

---

## S13. EBS: modo offline y sincronización con conflictos

### Descripción
App o vista offline-first para visitas EBS: formularios descargables, registro local, sincronización al recuperar red con detección y resolución de conflictos.

### Backend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Endpoints | `EbsController` / `SyncController` | `GET /ebs/visitas/pendientes-sincronizar` (devuelve visitas modificadas después de una fecha). `POST /ebs/visitas/sincronizar` (body: lista de visitas con timestamp local y datos). Servidor aplica "last-write-wins" o devuelve conflictos (misma visita modificada en servidor y en cliente) con detalles para resolución |
| DTO | `VisitaEbsSyncDto` | id local cliente, id servidor, timestamp, datos visita, hash opcional |
| Service | `EbsVisitaService` | Al recibir sincronización: por cada visita, si no existe crear; si existe y el timestamp remoto es mayor, actualizar; si hay conflicto (mismo id, distinto contenido), devolver en lista de conflictos para que el cliente muestre "mantener servidor / mantener local / fusionar" y reenvíe |

### Frontend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Service Worker / estrategia | Angular o PWA | Cachear listado de visitas y formularios; guardar en IndexedDB o localStorage los drafts de visitas cuando no hay red |
| Servicio | `ebs-sync.service.ts` | Al estar online: enviar cola de visitas pendientes de sincronizar; si respuesta tiene conflictos, mostrar modal de resolución y reenviar elección |
| Vista EBS | Formulario de visita | Indicador "Sin conexión - se guardará localmente". Al volver conexión, botón "Sincronizar ahora" |

### Cómo probar S13
1. Simular offline (DevTools → Network → Offline). Crear o editar una visita EBS y guardar. Verificar que se guarde en local.
2. Volver a online y pulsar "Sincronizar". Verificar que la visita aparezca en el servidor (y en otra sesión). Si se simuló el mismo registro editado en dos sitios, verificar flujo de conflicto y resolución.
3. API: `POST /api/ebs/visitas/sincronizar` con array de visitas → 200 y lista de conflictos si aplica.

---

## S14. Resultados con interpretación en lenguaje sencillo (portal)

### Descripción
En el portal del paciente, mostrar resultados de laboratorio e imágenes con interpretación breve en lenguaje sencillo (plantillas por tipo de prueba) y enlace a descarga PDF.

### Backend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Reglas / plantillas | Config o tabla | Por tipo de prueba (ej. GLICEMIA) o por rango: texto "Dentro de lo esperado", "Requiere seguimiento", "Consulte a su médico". Servicio que dado orden_id o resultado, devuelva interpretacionLenguajeSencillo |
| Controller | `PortalController` | `GET /portal/paciente/ordenes-con-resultados` (solo del paciente). Response incluye para cada orden: resultado, interpretacionLenguajeSencillo, enlaceDescargaPdf |
| PdfController | Ya existente o ampliar | Endpoint que genere PDF de un resultado para el paciente (autorizado por paciente o por token) |

### Frontend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Portal paciente | Sección Laboratorios / Resultados | Lista de órdenes con resultado; cada ítem muestra resultado y un párrafo "Interpretación: Dentro de lo esperado" (o el que corresponda). Botón "Descargar PDF" |
| Estilos | Lenguaje claro, sin jerga técnica excesiva | Textos cortos y accesibles |

### Cómo probar S14
1. Como paciente en el portal, ir a Resultados. Verificar que se listen órdenes con resultado y que aparezca la interpretación en lenguaje sencillo. Descargar PDF de un resultado.
2. API: `GET /api/portal/paciente/ordenes-con-resultados` con token paciente → 200 con lista que incluya interpretacionLenguajeSencillo.

---

## S15. Guías de práctica clínica (GPC) integradas en el flujo

### Descripción
Para un diagnóstico CIE-10 dado, sugerir criterios de control, medicamentos de primera línea y estudios de seguimiento según guías; registrar que la sugerencia fue mostrada (auditoría).

### Backend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Tabla / recurso | Catálogo GPC | `guia_gpc` (id, codigo_cie10, titulo, criterios_control TEXT, medicamentos_primera_linea TEXT, estudios_seguimiento TEXT, fuente). Datos estáticos o importación |
| Service | `GuiaGpcService` | `buscarPorCie10(codigo)` → lista de sugerencias. Al mostrar en front se puede registrar en tabla `gpc_sugerencia_mostrada` (atencion_id, codigo_cie10, guia_id, mostrado_at, profesional_id) para auditoría |
| Controller | `GuiaGpcController` o dentro de Consulta | `GET /guia-gpc/sugerir?codigoCie10=M54.5` → List&lt;GuiaGpcSugerenciaDto&gt; |
| DTO | `GuiaGpcSugerenciaDto` | titulo, criteriosControl, medicamentosPrimeraLinea, estudiosSeguimiento, fuente |
| Registro auditoría | `POST /guia-gpc/registrar-visualizacion` | body: atencionId, codigoCie10, guiaId → guarda en gpc_sugerencia_mostrada |

### Frontend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Service | `guia-gpc.service.ts` | `sugerir(codigoCie10)`, `registrarVisualizacion(atencionId, codigoCie10, guiaId)` |
| Componente | Formulario SOAP — sección Análisis/Plan | Cuando el usuario selecciona o escribe un CIE-10, llamar a sugerir y mostrar un panel colapsable "Sugerencias según guías" con criterios, medicamentos y estudios. Botón "Usar en plan" opcional. Al mostrar, llamar a registrarVisualizacion |

### Cómo probar S15
1. En Nota SOAP, en Análisis, ingresar código CIE-10 (ej. M54.5 o E11). Verificar que aparezca panel "Sugerencias según guías" con contenido coherente. Registrar visualización y comprobar en BD que existe registro en gpc_sugerencia_mostrada.
2. API: `GET /api/guia-gpc/sugerir?codigoCie10=E11` → 200 con sugerencias.

---

## S16. Auditoría de calidad de HC automatizada

### Descripción
Proceso que evalúa contenido mínimo de la HC (motivo, subjetivo, hallazgos, CIE-10, plan, firma). Indicadores por profesional y servicio; alertas si se cae bajo umbral.

### Backend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Service | `AuditoriaHcService` | Método `evaluarAtencion(consultaId)`: revisar consulta y evolución (motivo, enfermedadActual, codigoCie10, plan, profesionalId, fecha). Devolver DTO con camposCompletos (boolean), listaCamposFaltantes, puntuacion (0-100). Método `reportePorProfesional(desde, hasta)` y `reportePorServicio(desde, hasta)` que agreguen % atenciones completas por profesional/servicio |
| Tabla opcional | `auditoria_hc_evaluacion` | id, consulta_id, evaluado_at, puntuacion, campos_faltantes TEXT, para historial |
| Controller | `ReporteController` | `GET /reportes/auditoria-hc/atencion/{consultaId}` → EvaluacionHcDto. `GET /reportes/auditoria-hc/por-profesional?desde=&hasta=` y `.../por-servicio` → listas para dashboard |
| Config | Umbral | Si puntuacion &lt; 70 (configurable), considerar "bajo umbral" y aparecer en alertas del panel de cumplimiento (S4) |

### Frontend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Panel cumplimiento / Reportes | Misma página S4 o pestaña | Sección "Auditoría de calidad HC": tabla por profesional con % atenciones completas y puntuación media; tabla por servicio. Filtro por período. Alertas: "Profesionales bajo umbral" |
| Detalle atención | Opcional | En vista de una consulta, indicador "Calidad del registro: 85%" con desglose de campos faltantes |

### Cómo probar S16
1. Crear varias atenciones: unas con todos los campos (motivo, CIE-10, plan, firma) y otras incompletas. Ejecutar reporte por profesional en el período. Verificar que los porcentajes reflejen las completas vs total.
2. API: `GET /api/reportes/auditoria-hc/atencion/1` → 200 con puntuacion y camposFaltantes. `GET /api/reportes/auditoria-hc/por-profesional?desde=2025-01-01&hasta=2025-12-31` → 200 con lista.

---

## S17. Recepción de RDA de otras IPS (IHCE bidireccional)

### Descripción
Consultar al IHCE (o repositorio federado) por paciente y mostrar en la HC los RDA recibidos de otras instituciones.

### Backend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Cliente | Similar a `RdaMinisterioClient` | Cliente que consulte al Ministerio (o al repositorio) por identificador del paciente (tipo doc + número) para obtener listado de RDA recibidos. Depende de que la API del Ministerio exponga consulta por paciente (ver Anexo Res. 1888) |
| Tabla opcional | `rda_recibidos_cache` | id, paciente_id, id_ministerio, tipo_rda, fecha_atencion, institucion_origen, bundle_json (o referencia), fetched_at. Para no consultar en cada apertura de HC |
| Service | `RdaService` | Método `obtenerRdaRecibidosPaciente(pacienteId)`: si hay API de consulta, llamarla; guardar en cache; devolver DTOs con resumen (fecha, tipo, institución). Método `getRdaRecibidoDetalle(id)` para ver Bundle o resumen extendido |
| Controller | `RdaController` | `GET /rda/recibidos/paciente/{pacienteId}` → List&lt;RdaRecibidoResumenDto&gt;. `GET /rda/recibidos/{id}` → detalle |

### Frontend
| Elemento | Ubicación | Detalle |
|----------|-----------|---------|
| Historia clínica | Pestaña Documentos o nueva "RDA de otras IPS" | Sección "Atenciones en otras instituciones (IHCE)": lista de RDA recibidos con fecha, tipo, institución; al hacer clic, modal o drawer con detalle del resumen (no el Bundle completo, sino datos legibles) |
| Service | `rda.service.ts` | `getRdaRecibidosPaciente(pacienteId)`, `getRdaRecibidoDetalle(id)` |

### Cómo probar S17
1. Configurar URL y auth de la API de consulta IHCE (si existe). Con un paciente que tenga Número de Identificación que exista en el Ministerio, abrir HC y pestaña "RDA de otras IPS". Verificar que se listen los RDA recibidos (o mensaje "No hay datos disponibles" si la API no está o no devuelve nada).
2. API: `GET /api/rda/recibidos/paciente/1` → 200 con lista (puede ser vacía si no hay integración real aún).

---

## Resumen de archivos nuevos / modificados

### Backend (Java)
- **Nuevos:** `PacienteRiesgoDto`, `ResultadoCriticoLectura` (entity + repo), `ReconciliacionAtencion` (entity + repo + service + controller), `Glosa` / `GlosaAdjunto` (entity + repo + service + controller), `CuestionarioPreconsulta` (entity + repo + service), `Cie10SugerenciaService` + controller, `GuiaGpc` (entity/catálogo) + service + controller, `AuditoriaHcService`, `IntegracionController`, `CumplimientoNormativoDto` + métodos en ReporteService, `PhrService` / ampliación RDA, `AltaReferenciaService`, `ApiKey` (entity) + filtro seguridad, extensiones EBS sync, RDA urgencias/hospitalización, RDA recibidos.
- **Modificados:** `tenant_schema.sql` (tablas resultado_critico_lectura, reconciliacion_atencion, glosas, cuestionario_preconsulta, api_keys, guia_gpc, auditoria_hc_evaluacion, rda_recibidos_cache; columnas en citas). `Cita`, `OrdenClinica` (resultado_critico, token_confirmacion, etc.). `RecordatorioCitaServiceImpl`, `ReporteServiceImpl`, `OrdenClinicaService`, `RdaGeneratorService`, `RdaService`, controllers existentes.

### Frontend (Angular)
- **Nuevos:** Servicios: `reconciliacion.service.ts`, `glosa.service.ts`, `guia-gpc.service.ts`, `cie10-sugerencia.service.ts` (o integrado en consulta). Componentes/páginas: `cita-confirmar`, `cita-cancelar`, `cumplimiento-normativo` (o sección en reportes), bloque reconciliación en HC, módulo glosas, cuestionario pre-consulta (portal), resultados con interpretación (portal), sugerencias GPC en SOAP, sugerencias CIE-10 en SOAP, integraciones (API Keys) en admin. Ampliaciones: `reporte.service.ts` (riesgo, cumplimiento), `orden-clinica.service.ts` (marcar resultado leído), `rda.service.ts` (urgencia, hospitalización, recibidos), `portal.service.ts` (phr, cuestionario, resultados).
- **Modificados:** `historia-clinica.page.ts/html` (score riesgo, bloque reconciliación), `app.routes.ts`, estilos varios.

---

## Checklist de pruebas por sugerencia

| # | Sugerencia | Prueba funcional | Prueba API | Prueba integración |
|---|------------|------------------|------------|---------------------|
| S1 | Score riesgo | Abrir HC paciente con datos → ver badge riesgo | GET /reportes/paciente/{id}/riesgo | - |
| S2 | Resultados críticos | Registrar resultado fuera rango → notificación y marcar leído | PUT /ordenes-clinica/{id}/marcar-resultado-leido | Laboratorio guarda resultado |
| S3 | Confirmación cita | Abrir enlace con token → cita confirmada/cancelada | GET /cita/confirmar?t= | Recordatorio incluye enlace |
| S4 | Panel cumplimiento | Ver dashboard con % y exportar | GET /reportes/cumplimiento-normativo | - |
| S5 | Reconciliación | Abrir atención → reconciliar → guardar | GET/POST /consultas/{id}/reconciliacion | - |
| S6 | Alta/referencia PDF | Dar alta urgencia con checklist → descargar PDF | POST /urgencias/registro/{id}/alta, GET .../alta/pdf | - |
| S7 | PHR | Descargar historial PDF/FHIR desde portal y desde HC | GET /portal/paciente/{id}/phr?formato= | - |
| S8 | Sugerencia CIE-10 | Escribir motivo/diagnóstico → ver sugerencias y elegir | GET /cie10/sugerir?motivo=&texto= | - |
| S9 | Glosas | Crear glosa, adjuntar, reporte recuperación | POST/GET /glosas, GET /reportes/recuperacion-cartera | - |
| S10 | ePRO | Enviar cuestionario pre-consulta → ver en atención | POST /portal/cita/{id}/cuestionario-preconsulta | - |
| S11 | RDA urgencias/hosp | Generar y enviar RDA desde urgencia y desde hosp | POST /rda/generar/urgencia/{id} | Ministerio (si config) |
| S12 | API integradores | Llamar con API Key a resultado orden | POST /integracion/ordenes/{id}/resultado | - |
| S13 | EBS offline | Editar visita sin red → sincronizar | POST /ebs/visitas/sincronizar | - |
| S14 | Interpretación portal | Portal paciente → resultados con texto sencillo | GET /portal/paciente/ordenes-con-resultados | - |
| S15 | GPC | Ingresar CIE-10 → ver sugerencias GPC | GET /guia-gpc/sugerir?codigoCie10= | - |
| S16 | Auditoría HC | Ver reporte por profesional/servicio | GET /reportes/auditoria-hc/por-profesional | - |
| S17 | RDA recibidos | HC → pestaña RDA otras IPS | GET /rda/recibidos/paciente/{id} | Ministerio (si API) |

---

*Documento vivo: actualizar conforme se implementen cada ítem. Autor: Ing. J Sebastian Vargas S.*
