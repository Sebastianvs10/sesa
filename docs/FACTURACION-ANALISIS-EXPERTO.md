# Análisis experto del módulo de Facturación — IPS / Hospitales / Colombia

**Enfoque:** Facturador de servicios hospitalarios con más de 20 años de experiencia.  
**Objetivo:** Revisar lo implementado, lo faltante y proponer mejoras competitivas e innovadoras.  
**Autor:** Análisis para SESA (Ing. J Sebastian Vargas S).

---

## 1. Lo que ya está implementado (fortalezas)

### 1.1 Modelo de datos y flujo base
- **Entidad Factura** con paciente, orden clínica opcional, valor total, estado, fecha, CUPS, responsable de pago, cuota moderadora, número de autorización EPS.
- **Trazabilidad orden → factura**: una factura puede asociarse a una orden (medicamento, laboratorio, procedimiento).
- **Órdenes pendientes de facturar**: listado de órdenes sin factura y acción "Generar factura" con formulario prellenado.
- **Estados de factura**: PENDIENTE, EN_PROCESO, PAGADA, RECHAZADA, ANULADA.
- **Resumen/KPIs**: facturado mes, pendiente de cobro, pagadas, rechazadas/anuladas, vencidas radicación (22 días hábiles).
- **Días hábiles para radicación**: constante 22 (alineado con normativa).

### 1.2 Normativa colombiana (RIPS y FEV)
- **CUPS**: catálogo y búsqueda en el formulario de nueva factura.
- **Tipo de servicio** alineado con RIPS: consulta externa, urgencias, hospitalización, procedimiento, laboratorio, medicamento, etc.
- **Responsable del pago**: EPS, ARL, SOAT, paciente particular, ente territorial, ADRES/víctimas.
- **Cuota moderadora** y **número de autorización EPS**.
- **Exportación RIPS**:
  - CSV genérico.
  - RIPS estructurado: archivos **CT** (usuarios/pacientes), **US** (servicios), **AP** (procedimientos), **AC** (consultas).
- **Facturación electrónica DIAN**: estructura (FEV, CUFE, QR, estado); implementación real pendiente (stub).

### 1.3 Glosas y recuperación de cartera
- **Entidad Glosa** vinculada a factura: motivo rechazo, estado, fechas, adjuntos.
- **Servicio de glosas**: crear, actualizar, listar por factura/estado/fechas, adjuntos.
- **Reporte recuperación de cartera**: total glosas, pendientes, enviadas, aceptadas, total recuperado.
- **Vista Glosas** en frontend con filtros y enlace desde Facturación.

### 1.4 UX del módulo
- Formulario de nueva factura en **pasos** (Identificación → Servicio RIPS → Financiación → Valor y estado).
- **Contexto “Desde orden clínica”** cuando se genera factura desde una orden (paciente y datos prellenados).
- Referencias normativas (Res. 2275/2023, Ley 100, etc.) en etiquetas y ayudas.
- Filtros por estado, fechas y paciente; paginación; acciones por factura (Emitir FEV, Pagar, Glosa, Anular).

---

## 2. Lo que falta por implementar (brechas críticas)

### 2.1 Factura multiclínea (cuenta médica con varios ítems)
- **Hoy:** Una factura = un valor total y un solo CUPS/tipo de servicio.
- **Realidad IPS:** Una cuenta médica suele tener **varios ítems** (varios CUPS, varios procedimientos/medicamentos en la misma factura).
- **Recomendación:** Introducir **FacturaItem** (o detalle de factura): factura_id, codigo_cups, descripcion_cups, cantidad, valor_unitario, valor_total, tipo_servicio. La factura tendría `valor_total` como suma de ítems. Creación/edición de factura con líneas dinámicas.

### 2.2 Actualización completa de la factura
- **Hoy:** `FacturaServiceImpl.update()` solo actualiza: numeroFactura, valorTotal, estado, descripcion, fechaFactura. **No** actualiza codigoCups, descripcionCups, tipoServicio, responsablePago, cuotaModeradora, numeroAutorizacionEps.
- **Recomendación:** Incluir en `update()` todos los campos editables del DTO (CUPS, responsable, autorización, cuota moderadora, etc.) para poder corregir datos antes de radicar.

### 2.3 Archivo RIPS AM (medicamentos)
- **Hoy:** Se exportan CT, US, AP, AC. **No** existe archivo **AM** (medicamentos), obligatorio cuando hay servicios de farmacia.
- **Recomendación:** Generar archivo AM según Res. 2275/2023 para facturas con tipo servicio MEDICAMENTO (y órdenes de medicamentos vinculadas), con estructura oficial (código medicamento, cantidad, valor, etc.).

### 2.4 RIPS Res. 2275/2023 (formato y CUV)
- **Hoy:** RIPS en texto plano (formato histórico Res. 3374). La Res. 2275 exige **formato de transmisión** (p. ej. JSON) y **Código Único de Validación (CUV)** para soporte de la FEV.
- **Recomendación:** Plan de migración: generación de RIPS en formato vigente (según anexos 2275) y obtención de CUV (integración con Ministerio/SISPRO según normativa actual).

### 2.5 Radicación ante EPS (registro y seguimiento)
- **Hoy:** No hay entidad ni flujo de “radicación” (envío formal a EPS con fecha, CUV, número de radicado, estado).
- **Recomendación:** Entidad **Radicacion** (o similar): factura_id, eps_id o contrato_id, fecha_radicacion, numero_radicado, CUV, estado (radicada, aceptada, rechazada, pagada). Pantalla “Radicaciones” con filtros y alertas de vencimiento (22 d hábiles).

### 2.6 Contratos / convenios y tarifarios
- **Hoy:** No hay entidad de contrato IPS–EPS ni tarifario por procedimiento/CUPS por contrato.
- **Realidad:** Las IPS facturan según convenios y tarifas acordadas (nivel I, II, III, etc.).
- **Recomendación (medio plazo):** Entidad **Contrato** (IPS–EPS, vigencia, tipo) y **TarifarioContrato** (contrato_id, codigo_cups, valor, vigencia). Al crear factura para una EPS, sugerir valor según tarifario y validar CUPS contra contrato.

### 2.7 Numeración consecutiva de facturas
- **Hoy:** Consecutivo con `facturaRepository.count() + 1`, con riesgo de duplicados en concurrencia.
- **Recomendación:** Secuencia o tabla de consecutivos por año (o por punto de emisión) con bloqueo optimista o SELECT FOR UPDATE para garantizar unicidad y cumplimiento DIAN.

### 2.8 Integración DIAN real (FEV)
- **Hoy:** Stub que solo marca estado "PENDIENTE_DIAN" y mensaje de simulación.
- **Recomendación:** Implementar o integrar proveedor de facturación electrónica: generación XML UBL 2.1, firma, envío a DIAN, actualización de CUFE, QR, almacenamiento de XML/PDF.

### 2.9 Vinculación glosa ↔ factura en la UI
- **Hoy:** Glosas se listan por filtros; no hay creación de glosa desde la factura ni vista “detalle de factura” con sus glosas.
- **Recomendación:** En listado/detalle de facturas: botón “Registrar glosa” y listado de glosas de esa factura con estados y montos recuperados.

### 2.10 Datos paciente para RIPS (CT)
- **Hoy:** Se usa Paciente con municipioResidencia, zonaResidencia, regimenAfiliacion, tipoUsuario. Verificar que estén siempre completos y alineados con códigos oficiales (DANE, etc.) para CT y para validaciones EPS.

---

## 3. Sugerencias innovadoras para ser competitivos

### 3.1 Dashboard de facturación predictivo y alertas
- **Indicadores:** Proyección de cartera por vencer (próximos 7/15 días), concentración por EPS, edad promedio de facturas pendientes.
- **Alertas:** “X facturas sin radicar próximas a 22 días hábiles”, “Top 3 EPS con más glosas pendientes”.
- **Gráficas:** Tendencia mensual facturado vs. cobrado, comparativo mes actual vs. anterior.

### 3.2 Flujo “facturación por lote” desde órdenes
- **Selección múltiple** de órdenes pendientes (por paciente, por fecha, por tipo) y generación de **una factura por paciente** con ítems = órdenes seleccionadas de ese paciente (cuando exista factura multiclínea).
- **Plantillas:** “Facturar todo lo pendiente del día” o “Pendientes de laboratorio del mes” con un clic.

### 3.3 Validaciones previas a radicación
- **Checklist pre-radicación:** Autorización EPS, CUPS válido, valor coherente con tarifario (si existe), paciente con datos RIPS completos (municipio, régimen, etc.). Bloqueo o advertencia antes de marcar “lista para radicar”.

### 3.4 Recordatorios y tareas para el facturador
- **Tareas:** “Radicar facturas FV-2026-00012, FV-2026-00015” (próximas a vencer), “Responder glosa #X”.
- **Notificaciones** o bandeja de “pendientes por facturador” (por rol Facturación).

### 3.5 Exportación y reportes listos para auditoría
- **Reportes estándar:** Libro de facturación (por período), facturas por EPS, por tipo de servicio, por estado.
- **Exportación:** Excel/PDF con formato “cuenta médica” para enviar a EPS o archivo.
- **Trazabilidad:** En detalle de factura, línea de tiempo: creada → emitida FEV → radicada → glosa → pagada.

### 3.6 Integración con portales EPS (cuando existan APIs)
- **Consulta de autorizaciones** vía API (si la EPS lo ofrece) para prellenar número de autorización y vigencia.
- **Envío electrónico de radicación** si la EPS admite integración (reducir envío manual de archivos).

### 3.7 Módulo “Estados” y flujo de aprobaciones
- **Hoy:** Enlace “Estados” en el header de Facturación sin flujo claro.
- **Propuesta:** Vista “Estados de facturación” con definición de flujos (ej.: Borrador → Por aprobar → Emitida → Radicada → Cobrada) y permisos por rol (ej.: Facturación crea, Jefe de facturación aprueba).

### 3.8 Recuperación de cartera con priorización
- **Reporte recuperación:** Ordenar por monto o por antigüedad; filtro por EPS; meta de recuperación vs. realizado.
- **Indicador:** “Tasa de recuperación” (recuperado / glosas respondidas) y comparativo por período.

### 3.9 Soporte multi-sede / multi-punto de emisión
- **Consecutivos** por sede o punto de emisión (requerido en muchos esquemas DIAN/IPS).
- **Filtros y reportes** por sede para gerentes de varias IPS.

### 3.10 Ayuda contextual y normativa
- **Tooltips o enlaces** en campos críticos: “Código CUPS según Res. 2275”, “Plazo radicación 22 d hábiles (Res. 558/2024)”.
- **Plantilla de respuestas a glosas** por motivo frecuente (falta de autorización, CUPS no cubierto, etc.) para agilizar la recuperación.

---

## 4. Resumen ejecutivo

| Área                    | Estado actual        | Prioridad sugerida |
|-------------------------|---------------------|--------------------|
| Factura multiclínea     | No implementado     | Alta               |
| Update completo factura | Parcial (faltan campos) | Alta          |
| RIPS AM (medicamentos)  | No implementado     | Alta               |
| Radicación (entidad y flujo) | No implementado | Alta               |
| RIPS Res. 2275 / CUV    | Parcial (formato antiguo) | Media        |
| Contratos y tarifarios  | No implementado     | Media              |
| Consecutivo robusto     | Mejorable          | Media              |
| Integración DIAN real   | Stub                | Alta (según negocio) |
| Glosas desde factura    | Parcial             | Media              |
| Dashboard predictivo    | No implementado     | Media              |
| Facturación por lote    | No implementado     | Innovación         |
| Validaciones pre-radicación | No implementado | Media              |

---

## 5. Próximos pasos recomendados

1. **Corto plazo:** Completar `update()` de factura con todos los campos normativos; añadir archivo AM en exportación RIPS cuando tipo = MEDICAMENTO.
2. **Medio plazo:** Diseñar e implementar FacturaItem y factura multiclínea; entidad Radicacion y pantalla de radicaciones con alertas de vencimiento.
3. **Largo plazo:** Contratos y tarifarios; RIPS en formato Res. 2275 con CUV; integración FEV DIAN real; dashboard predictivo y facturación por lote.

Este análisis puede usarse como hoja de ruta para priorizar desarrollos y presentar el módulo como una solución alineada con la normativa colombiana y competitiva frente a otros sistemas del mercado.
