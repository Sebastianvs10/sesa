# Problemáticas del facturador en Colombia y soluciones con SESA

**Enfoque:** Facturador profesional y experto en Colombia (IPS, EPS, sector salud).  
**Objetivo:** Identificar las mayores problemáticas del día a día y cómo el módulo de facturación SESA puede solucionarlas.  
**Autor:** Análisis para SESA (Ing. J Sebastian Vargas S).

---

## 1. Las mayores problemáticas (vista del facturador)

### 1.1 Pérdida de plazo de radicación (22 días hábiles)

**Problema:** La Res. 558/2024 y la normativa FEV-RIPS exigen radicar las cuentas médicas ante la EPS en un plazo de **22 días hábiles** desde la fecha de la factura. Si se vence el plazo, la IPS puede **perder el derecho al cobro** o enfrentar glosas masivas. En la práctica, el facturador no sabe qué facturas están por vencer ni cuántos días le quedan hasta que ya es tarde.

**Impacto:** Cartera perdida, conflictos con la EPS, estrés operativo.

---

### 1.2 Glosas sin respuesta a tiempo

**Problema:** Cuando la EPS devuelve una glosa (rechazo total o parcial), la IPS tiene plazos para responder con soportes (autorización, historia clínica, CUPS correcto, etc.). Si no se responde, se pierde el cobro. Hoy muchas IPS llevan glosas en Excel o en carpetas y no tienen una vista clara de: cuántas hay pendientes, por qué monto, por EPS, ni recordatorios.

**Impacto:** Recuperación de cartera baja, montos atrapados, auditorías negativas.

---

### 1.3 Cuenta médica con un solo ítem (realidad multiclínea)

**Problema:** En la realidad, una cuenta médica tiene **varios ítems**: varios CUPS (consulta + laboratorio + medicamento), varios procedimientos o varios medicamentos en la misma factura. Si el sistema solo permite "una factura = un valor y un CUPS", el facturador tiene que emitir **varias facturas por un mismo acto de atención** o meter todo en la descripción, lo que genera rechazos y no cumple con RIPS.

**Impacto:** RIPS incorrectos, glosas por "detalle no concordante", retrabajo.

---

### 1.4 RIPS incompletos o en formato equivocado

**Problema:**
- Falta el archivo **AM (medicamentos)** cuando hay farmacia: la EPS o el Ministerio lo exigen y rechazan el paquete.
- El formato sigue siendo el histórico (Res. 3374) y no el vigente (Res. 2275/2023) con CUV y estructura de transmisión.
- Errores en archivo CT (datos del paciente): municipio, régimen, tipo de usuario mal codificados generan rechazos en lote.

**Impacto:** Cuentas devueltas, retrasos en el pago, no cumplimiento normativo.

---

### 1.5 Sin trazabilidad factura → radicación → glosa → pago

**Problema:** El gerente o el auditor pregunta: "¿esta factura ya se radicó?", "¿tuvo glosa?", "¿en qué estado está?". Si no hay un solo lugar donde ver la **línea de tiempo** (creada → emitida FEV → radicada → glosa → pagada), la respuesta depende de memoria o de hojas sueltas.

**Impacto:** Auditorías lentas, imposibilidad de priorizar, falta de control.

---

### 1.6 Facturar "a ciegas" sin validar antes de radicar

**Problema:** Se radica la factura y la EPS rechaza por: falta número de autorización, CUPS no cubierto en el contrato, paciente sin municipio/régimen en RIPS, valor incoherente. El facturador no tuvo una **validación previa** (checklist) antes de marcar "lista para radicar".

**Impacto:** Devoluciones, retrabajo y sensación de "probemos a ver si pasa".

---

### 1.7 Facturación electrónica (FEV) solo "de nombre"

**Problema:** La DIAN exige facturación electrónica (FEV) con CUFE, QR, XML. Si el sistema solo simula el envío y no integra con un proveedor real (firma, envío a la DIAN, almacenamiento del XML/PDF), la IPS no puede cumplir y se expone a sanciones o a no poder cobrar a particulares/empresas.

**Impacto:** Incumplimiento DIAN, limitación para facturar a quien exija FEV.

---

### 1.8 Tarifas y convenios solo "en la cabeza"

**Problema:** Cada EPS tiene convenios con tarifas por CUPS (nivel I, II, III, etc.). Si el sistema no tiene **contratos ni tarifarios**, el valor de la factura lo pone el usuario "de memoria" o copiando de otra factura. Valores incorrectos generan glosas o devoluciones.

**Impacto:** Glosas por valor no convenido, pérdida de tiempo buscando tarifas.

---

### 1.9 Reportes y auditoría hechos a mano

**Problema:** Para cerrar mes, presentar a la EPS o auditar, se pide: libro de facturación por período, facturas por EPS, por tipo de servicio, por estado. Si no hay **reportes estándar y exportación** (Excel/CSV/PDF), se arma en Excel manual con filtros y recortes.

**Impacto:** Horas perdidas, errores de corte, reportes inconsistentes.

---

### 1.10 Órdenes que "se pierden" para facturar

**Problema:** Las órdenes de medicamentos, laboratorio o procedimientos deben convertirse en cuenta médica. Si no hay un listado claro de **órdenes pendientes de facturar** y un flujo "desde orden → factura", algunas órdenes nunca se facturan o se facturan tarde.

**Impacto:** Ingresos no capturados, diferencias entre lo prestado y lo facturado.

---

## 2. Cómo SESA puede solucionarlas

A continuación se mapea cada problemática con lo que **SESA ya hace** y lo que **falta o se puede reforzar**.

| # | Problemática | Solución en SESA | Estado actual |
|---|--------------|------------------|---------------|
| 1 | Plazo 22 días hábiles | Alertas y bandeja: facturas por vencer (≤7 d) y vencidas; días restantes en listado y en detalle. Constante 22 en backend (Res. 558/2024). | **Implementado:** alertas, bandeja facturador, KPIs vencidas radicación, checklist con referencia normativa. |
| 2 | Glosas sin respuesta | Vista Glosas con filtros; enlace desde factura; filtro por facturaId; reporte recuperación de cartera. | **Implementado.** Reforzar: recordatorios en bandeja, plazos por tipo de glosa, plantillas de respuesta. |
| 3 | Cuenta multiclínea | Factura con ítems (FacturaItem): varios CUPS, cantidades, valores por línea; total = suma ítems. Facturación por lote agrupa órdenes por paciente en una factura con ítems. | **Implementado:** FacturaItem, formulario con tabla de ítems, lote por paciente. |
| 4 | RIPS incompletos / formato | Export RIPS estructurado CT, US, AP, AC y **AM (medicamentos)**; checklist y ayuda con Res. 2275. | **Implementado:** AM en export; CT/US/AP/AC. **Pendiente:** formato Res. 2275 (CUV, transmisión oficial). |
| 5 | Trazabilidad factura → radicación → glosa | Timeline por factura: creada → emitida FEV → radicada → glosa(s) → estado (pagada/rechazada/anulada). Panel "Trazabilidad" desde cada factura. | **Implementado:** endpoint timeline, panel Trazabilidad en facturación. |
| 6 | Validar antes de radicar | Checklist pre-radicación: autorización EPS, al menos un CUPS, valor > 0, documento paciente; advertencias (municipio, tipo doc). Bloqueo o advertencia en panel Radicar. | **Implementado:** GET checklist-radicacion, panel Radicación con checklist y botón deshabilitado si hay errores. |
| 7 | FEV real | Integración con proveedor de FEV (XML UBL 2.1, firma, envío DIAN, CUFE, QR). | **Pendiente:** hoy es stub. Requiere decisión de negocio y proveedor (ej. facturador electrónico). |
| 8 | Tarifas por contrato | Contratos IPS–EPS y tarifario por CUPS; al crear factura sugerir valor y validar CUPS. | **Pendiente.** Prioridad media; alto impacto en reducción de glosas por valor. |
| 9 | Reportes y auditoría | Libro de facturación por período (y estado); export CSV; filtros por fecha/estado. | **Implementado:** reporte libro, export CSV desde filtros. Reforzar: más columnas, Excel, PDF "cuenta médica". |
| 10 | Órdenes pendientes de facturar | Listado "Órdenes pendientes de facturar"; botón "Generar factura" por orden; facturación por lote (selección múltiple, una factura por paciente). | **Implementado:** órdenes pendientes, generar desde orden, facturación por lote. |

---

## 3. Resumen: qué priorizar en SESA

### Ya resuelto con SESA (mantener y explotar)

- Plazo 22 días: alertas, bandeja, vencidas en resumen.
- Glosas: listado, filtros, enlace desde factura, recuperación de cartera.
- Cuenta multiclínea (ítems) y facturación por lote.
- RIPS con archivo AM; libro de facturación y export CSV.
- Trazabilidad (timeline) y checklist pre-radicación.
- Órdenes pendientes y flujo orden → factura.

### Pendiente de alto impacto

1. **FEV real:** Integrar con proveedor de facturación electrónica (DIAN) para no depender del stub.
2. **RIPS Res. 2275/2023:** Formato y CUV según normativa vigente (cuando SISPRO/Ministerio definan flujo de transmisión).
3. **Contratos y tarifarios:** Modelo Contrato + Tarifario por CUPS para sugerir valores y reducir glosas por valor.

### Reforzos recomendados (rápidos)

- **Glosas:** Plazos de respuesta por tipo de glosa; plantillas de respuesta por motivo frecuente; recordatorios en bandeja.
- **Reportes:** Export Excel del libro; reporte "facturas por EPS" y "por tipo de servicio"; PDF tipo cuenta médica para envío a EPS.
- **Datos paciente RIPS:** Validación o avisos cuando falte municipio, régimen o tipo de usuario (para archivo CT y checklist).

---

## 4. Mensaje para el facturador

Con SESA hoy puedes:

- Ver **qué facturas están por vencer o vencidas** para radicación (22 días hábiles) y actuar a tiempo.
- Trabajar **cuentas médicas con varios ítems** (varios CUPS) y facturar por lote desde órdenes pendientes.
- **Validar antes de radicar** (autorización, CUPS, paciente) y registrar la radicación con número y CUV.
- Ver la **trazabilidad** de cada factura (creada → FEV → radicada → glosa → pagada) en un solo lugar.
- Gestionar **glosas** desde el módulo y enlazadas a la factura, y bajar el **libro de facturación** en CSV por período.

Lo que sigue en la hoja de ruta es: FEV real con la DIAN, RIPS en formato 2275 con CUV, y contratos/tarifarios para facturar con el valor correcto por EPS y reducir glosas.

Este documento sirve como guía para priorizar desarrollos y para comunicar a facturadores y gerentes cómo SESA aborda las mayores problemáticas del día a día en Colombia.
