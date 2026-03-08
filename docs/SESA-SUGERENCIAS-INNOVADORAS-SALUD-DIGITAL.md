# SESA — Sugerencias innovadoras para una app única en el sector salud (Colombia)

**Objetivo:** Ideas modernas e innovadoras que diferencien a SESA en el sector, cumpliendo normativa vigente (Res. 1995/1999, 5596/2015, 3374/2000, 1888/2025, 0256/2016, Ley 1581/2012, Decreto 1011/2006) e incorporando metodologías y tendencias actuales en salud digital.

**Contexto:** SESA ya cuenta con RDA/IHCE (Res. 1888/2025), multi-tenant, historia clínica unificada, urgencias con triage, EBS, farmacia, laboratorio, facturación RIPS, portal del paciente y videoconsulta. Este documento propone el siguiente nivel de innovación.

---

## 1. Interoperabilidad y datos del paciente

### 1.1 Historial de salud portátil (PHR) y “Mi datos de salud”

- **Idea:** Que el paciente pueda **descargar o compartir** un paquete estándar de su información (antecedentes, alergias, últimas atenciones, medicamentos actuales, resultados recientes) en formato **FHIR R4** o **PDF estructurado**, con validez legal y usable por otra IPS o por el mismo paciente (Ley 1581/2012 — derecho de acceso y portabilidad).
- **Normativa:** Res. 1995/1999 (conservación), Res. 1888/2025 (FHIR), Ley 1581/2012.
- **Diferenciador:** Pocas IPS en Colombia ofrecen al paciente un “bundle” FHIR o un informe único descargable para llevar a otra institución; esto mejora continuidad y posiciona a SESA como habilitador de **interoperabilidad centrada en el paciente**.

### 1.2 Recepción de RDA de otras IPS (IHCE bidireccional)

- **Idea:** Además de **enviar** RDA al Ministerio, permitir **recibir y mostrar** en la HC del paciente los RDA de otras instituciones (consulta al IHCE o a un repositorio federado), para ver atenciones previas en otras IPS sin depender de papel o correo.
- **Normativa:** Res. 1888/2025, futuras guías de consulta IHCE.
- **Diferenciador:** Historia clínica “agregada” que acerca el concepto de **historia única nacional** sin cambiar el flujo interno de SESA.

### 1.3 API abierta para integradores (laboratorio, PACS, dispositivos)

- **Idea:** **API REST documentada** (OpenAPI) con autenticación por token o API Key, para que laboratorios externos, PACS o dispositivos (glucómetros, tensiómetros) **envíen resultados o signos vitales** directamente a una atención o orden, reduciendo digitación y errores.
- **Normativa:** Res. 1995/1999 (registro en HC), buenas prácticas de seguridad (Ley 1581/2012).
- **Diferenciador:** Convierte a SESA en **plataforma integrable**, no solo en sistema cerrado; facilita contratos con redes de laboratorio o telemonitoreo.

---

## 2. Seguridad del paciente y apoyo a la decisión clínica

### 2.1 Score de riesgo por paciente en la cabecera de la HC

- **Idea:** En la cabecera de la historia clínica (o en el banner del paciente), un **indicador visual** de “riesgo” calculado a partir de: alergias no reconciliadas, polifarmacia, resultados críticos pendientes de revisión, controles vencidos (diabéticos, hipertensos), últimas visitas a urgencias. No sustituye el criterio médico; **apoya** priorización y seguridad.
- **Normativa:** Decreto 1011/2006 (seguridad del paciente), Res. 1995/1999.
- **Diferenciador:** Dashboard de riesgo por paciente en el primer pantallazo, alineado con **seguridad del paciente** y **atención proactiva**.

### 2.2 Alertas de resultados críticos con trazabilidad de lectura

- **Idea:** Cuando laboratorio (o imagen) registra un resultado **crítico** (según rangos configurables por prueba), el sistema: (1) marca la orden como “resultado crítico”, (2) **notifica** al profesional asignado o al equipo (in-app y opcionalmente correo/SMS), (3) registra **cuándo y quién** abrió/leyó el resultado (trazabilidad ante auditoría o eventos adversos).
- **Normativa:** Decreto 1011/2006, Res. 5521/2013 (auditoría), buenas prácticas de laboratorio clínico.
- **Diferenciador:** Cierre del ciclo **orden → resultado crítico → lectura**, escaso en muchas IPS.

### 2.3 Sugerencia de CIE-10 y códigos RIPS por contexto

- **Idea:** A partir del **motivo de consulta** y del **texto del análisis/diagnóstico**, sugerir **códigos CIE-10** (y si aplica procedimientos RIPS) mediante búsqueda semántica o un motor de sugerencias (reglas o modelo ligero). El profesional elige o corrige; el sistema aprende de las correcciones por institución (opcional).
- **Normativa:** Res. 3374/2000 (RIPS), Res. 1995/1999.
- **Diferenciador:** Menos rechazos por CIE-10 incorrecto y menos tiempo de digitación; alineado con **valor por resultado** (menos glosas).

### 2.4 Reconciliación de medicamentos y alergias en cada atención

- **Idea:** Al abrir una atención, pantalla o bloque **“Reconciliación”**: lista de medicamentos que el paciente refiere vs. lo que está en la HC; lista de alergias referidas vs. registradas. El profesional confirma o actualiza; queda registro de “reconciliado en fecha X por profesional Y”. Obligatorio antes de prescribir (o con recordatorio fuerte).
- **Normativa:** Decreto 1011/2006, Res. 1995/1999 (contenido mínimo).
- **Diferenciador:** Refuerza **seguridad del paciente** y es exigible en auditorías de calidad y acreditación.

---

## 3. Experiencia del paciente y compromiso (engagement)

### 3.1 Recordatorios y confirmación de cita desde el portal o enlace

- **Idea:** Recordatorio automático (SMS/WhatsApp/email) X horas o días antes de la cita, con **enlace para confirmar o cancelar/reagendar**. Si el paciente confirma, se actualiza el estado de la cita y se reduce no-asistencia; si cancela, el slot puede reasignarse. Opción de “recordatorio de control” según plan de la última atención.
- **Normativa:** Res. 2953/2014 (citas), Ley 1581/2012 (datos y consentimiento de contacto).
- **Diferenciador:** Menos inasistencia y mejor uso de la capacidad instalada; experiencia **centrada en el paciente**.

### 3.2 Cuestionarios pre-consulta y preparación (ePRO)

- **Idea:** Antes de la cita (o al agendarla), el paciente recibe un **cuestionario corto** por correo o portal: motivo de consulta en sus palabras, escalas (dolor EVA, ansiedad, calidad de sueño), medicamentos actuales, alergias. Las respuestas se incorporan a la HC como “datos aportados por el paciente” y prellenan motivo/Subjetivo en la nota SOAP.
- **Normativa:** Res. 1995/1999 (registro en HC), Ley 1581/2012.
- **Diferenciador:** Menos tiempo en consulta para captura básica; alineado con **patient-reported outcomes (PRO)** y telemedicina.

### 3.3 Acceso del paciente a resultados y resúmenes en lenguaje sencillo

- **Idea:** En el portal del paciente: **resultados de laboratorio e imágenes** con breve **interpretación en lenguaje sencillo** (plantillas por tipo de prueba: “Dentro de lo esperado”, “Requiere seguimiento”, “Consulte a su médico”) y enlace a la HC o a la próxima cita. Opción de descarga en PDF para llevar a otro médico.
- **Normativa:** Res. 1995/1999, Ley 1581/2012 (derecho de acceso), Decreto 1011/2006 (información al paciente).
- **Diferenciador:** Empodera al paciente y reduce consultas de “solo quiero el resultado”; coherente con **salud digital centrada en la persona**.

---

## 4. Operación y valor clínico

### 4.1 Panel de cumplimiento normativo en tiempo (casi) real

- **Idea:** Dashboard para coordinación/calidad con: **% atenciones con RDA enviado** (Res. 1888/2025), **% urgencias dentro del tiempo por triage** (Res. 5596/2015), **% HC con CIE-10 y evolución en &lt; 24 h**, **indicadores Res. 0256/2016** (calidad), **alertas de resultados críticos no leídos**. Todo con filtro por período, profesional y servicio; exportable para reuniones de calidad.
- **Normativa:** Res. 1888/2025, 5596/2015, 1995/1999, 0256/2016, 5521/2013.
- **Diferenciador:** **Gobernanza de la calidad** en un solo lugar; preparación para acreditación y auditoría.

### 4.2 Flujo “alta / referencia” con checklist y PDF para el paciente

- **Idea:** Al dar **alta** en urgencias o **referencia** desde consulta: checklist (diagnóstico, tratamiento, recomendaciones, próxima cita o control); generación automática de **PDF** con resumen en lenguaje claro para el paciente y, en referencia, datos del nivel al que se refiere y motivo. Envío por correo o descarga desde portal.
- **Normativa:** Res. 1995/1999, Res. 5596/2015 (urgencias), continuidad del cuidado.
- **Diferenciador:** Cierre explícito del episodio y **continuidad** entre niveles; menos reconsultas innecesarias.

### 4.3 Gestión de glosas y recuperación de cartera

- **Idea:** Módulo o flujo de **glosas**: cuando una factura viene rechazada (RIPS/ASE), registrar motivo del rechazo, adjuntar documentos (evidencias clínicas, autorizaciones) y enviar respuesta. Seguimiento por estado (pendiente, enviado, aceptado, rechazado) y reporte de **recuperación de cartera** por período y contrato.
- **Normativa:** Res. 3374/2000, Res. 2275/2023 y 1884/2024 (facturación electrónica).
- **Diferenciador:** Va más allá de “facturar y exportar RIPS”; convierte a SESA en herramienta de **gestión financiera** de la IPS.

### 4.4 EBS: modo offline robusto y sincronización con conflictos

- **Idea:** App o vista **offline-first** para visitas domiciliarias EBS: formularios y listas de tareas descargables; registro local cuando no hay red; **sincronización** al recuperar conexión, con detección de conflictos (mismo paciente editado en dos dispositivos) y resolución guiada (mantener servidor / mantener local / fusionar). Cumplimiento de metas en terreno sin depender 100% de cobertura.
- **Normativa:** Res. 3280 (APS), PDM, Res. 1995/1999 (registro en HC).
- **Diferenciador:** Muy valorado en **atención primaria rural** y por equipos territoriales; pocas soluciones lo hacen bien.

---

## 5. Metodologías y tendencias que potencian a SESA

### 5.1 Atención basada en valor (Value-Based Health Care)

- **Idea:** Reportes que crucen **resultados clínicos** (control de HTA, DM, cumplimiento de metas) con **uso de recursos** (consultas, urgencias, medicamentos) por paciente o por cohorte. No sustituye el modelo de fee-for-service actual, pero permite a la IPS medir **valor** (resultado / costo) y negociar con aseguradores o con el mismo Ministerio.
- **Normativa:** Compatible con RIPS, Res. 0256/2016, contratos por capitación o por resultados.
- **Diferenciador:** Posiciona a SESA como sistema que **mide valor**, no solo actividad.

### 5.2 Guías de práctica clínica (GPC) integradas en el flujo

- **Idea:** En la sección de análisis/plan, **sugerencias** basadas en guías (ej. Ministerio de Salud): para un diagnóstico CIE-10 dado, sugerir criterios de control, medicamentos de primera línea, estudios de seguimiento. El profesional acepta, modifica o ignora; el sistema registra que la sugerencia fue mostrada (auditoría de adherencia a GPC).
- **Normativa:** Res. 1995/1999, guías Minsalud, Decreto 1011/2006.
- **Diferenciador:** **Clinical decision support** sin reemplazar al médico; útil para acreditación y calidad.

### 5.3 Auditoría de calidad de HC automatizada

- **Idea:** Proceso periódico (no solo manual) que evalúa **contenido mínimo** de la HC: motivo, subjetivo, hallazgos objetivos, impresión diagnóstica con CIE-10, plan, firma y fecha. Indicadores por profesional y por servicio: % de atenciones “completas” según reglas configurables. Alertas cuando un profesional o servicio cae por debajo de un umbral.
- **Normativa:** Res. 1995/1999, Res. 5521/2013.
- **Diferenciador:** **Mejora continua** basada en datos; reduce carga de auditoría manual.

### 5.4 Preparación para RDA de urgencias y hospitalización

- **Idea:** La Res. 1888/2025 define también **RDA de urgencias** y **RDA de hospitalización**. Extender el generador FHIR actual para estos tipos a partir del modelo de datos de urgencias y de hospitalización, de modo que SESA cumpla los cuatro tipos de RDA y no solo consulta externa y paciente.
- **Normativa:** Res. 1888/2025 (plazo hasta abril 2026 para adecuación).
- **Diferenciador:** **Cumplimiento integral** de la interoperabilidad IHCE; ventaja frente a sistemas que solo envían consulta externa.

---

## 6. Priorización sugerida (impacto diferenciador / esfuerzo)

| Prioridad | Sugerencia | Impacto diferenciador | Cumplimiento normativo | Esfuerzo |
|-----------|------------|------------------------|-------------------------|----------|
| 1 | Score de riesgo en cabecera de HC | Alto | Dec. 1011/2006, Res. 1995/1999 | Medio |
| 2 | Alertas resultados críticos + trazabilidad de lectura | Muy alto | Dec. 1011/2006, Res. 5521/2013 | Medio |
| 3 | Recordatorios y confirmación de cita (enlace) | Alto | Res. 2953/2014 | Medio |
| 4 | Panel cumplimiento normativo (RDA, triage, CIE-10, 0256) | Alto | Res. 1888, 5596, 1995, 0256 | Medio |
| 5 | Reconciliación medicamentos/alergias por atención | Muy alto (seguridad) | Dec. 1011/2006, Res. 1995/1999 | Medio |
| 6 | Alta/referencia con checklist y PDF paciente | Alto | Res. 1995/1999, 5596/2015 | Bajo–Medio |
| 7 | Historial portátil (PHR) descargable FHIR/PDF | Alto | Res. 1888, Ley 1581/2012 | Medio |
| 8 | Sugerencia CIE-10/RIPS por contexto | Alto | Res. 3374/2000 | Medio–Alto |
| 9 | Gestión de glosas y recuperación de cartera | Alto (económico) | Res. 3374, 2275, 1884 | Alto |
| 10 | Cuestionarios pre-consulta (ePRO) | Medio–Alto | Res. 1995/1999, Ley 1581 | Medio |
| 11 | RDA urgencias y hospitalización (Res. 1888) | Alto (obligatorio) | Res. 1888/2025 | Alto |
| 12 | API abierta para laboratorio/PACS/dispositivos | Muy alto (ecosistema) | Res. 1995/1999, Ley 1581 | Alto |
| 13 | EBS modo offline y sincronización | Muy alto (APS) | Res. 3280, 1995/1999 | Alto |
| 14 | Resultados con interpretación en lenguaje sencillo (portal) | Medio | Res. 1995/1999, Ley 1581 | Medio |
| 15 | Guías GPC integradas en flujo (sugerencias) | Alto | Res. 1995/1999, Dec. 1011 | Alto |
| 16 | Auditoría de calidad de HC automatizada | Alto | Res. 5521/2013, 1995/1999 | Medio |
| 17 | Recepción RDA de otras IPS (IHCE bidireccional) | Muy alto (continuidad) | Res. 1888/2025 | Muy alto |

---

## 7. Conclusión

SESA puede consolidarse como una solución **única e innovadora** en el sector salud en Colombia si se enfoca en:

1. **Interoperabilidad centrada en el paciente** (PHR portátil, RDA bidireccional, API para integradores).
2. **Seguridad del paciente y apoyo a la decisión** (riesgo en cabecera, resultados críticos con trazabilidad, reconciliación, sugerencia CIE-10).
3. **Compromiso del paciente** (recordatorios con confirmación, pre-consulta, resultados en lenguaje sencillo).
4. **Cumplimiento normativo visible** (panel RDA/triage/CIE-10/0256, RDA urgencias y hospitalización, gestión de glosas).
5. **Metodologías modernas** (valor por resultado, GPC integradas, auditoría automatizada, EBS offline).

Todas las sugerencias son compatibles con la normativa vigente (Res. 1995/1999, 5596/2015, 3374/2000, 1888/2025, 0256/2016, Ley 1581/2012, Decreto 1011/2006) y pueden implementarse de forma incremental para mantener a SESA a la vanguardia en salud digital en Colombia.
