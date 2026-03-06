# Diseño del Módulo de Farmacia (Sistema Hospitalario / ERP)

**Autor:** Ing. J Sebastian Vargas S  
**Integración:** Historia Clínica, Órdenes clínicas (tipo MEDICAMENTO), Facturación, Auditoría.

---

## 1. Resumen del ciclo de vida

- **Compra / recepción** → Entrada de medicamentos (proveedor, lote, vencimiento, cantidad).
- **Almacenamiento** → Inventario por medicamento, lote, ubicación.
- **Control de inventario** → Stock, alertas de mínimo y vencimiento, ajustes.
- **Dispensación al paciente** → Desde órdenes médicas (Historia Clínica), total o parcial.
- **Devoluciones** → Entrada al inventario con motivo y trazabilidad.
- **Reportes y control regulatorio** → Kardex, medicamentos controlados, auditoría.

---

## 2. Submódulos y entidades

### 2.1 Maestro de Medicamentos (`medicamentos_farmacia`)

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | BIGSERIAL | PK |
| codigo | VARCHAR(50) | Código interno |
| nombre_generico | VARCHAR(200) | Nombre genérico |
| nombre_comercial | VARCHAR(200) | Nombre comercial |
| presentacion | VARCHAR(100) | Tabletas, jarabe, ampolla, etc. |
| concentracion | VARCHAR(80) | Ej. 500 mg |
| forma_farmaceutica | VARCHAR(80) | Comprimido, solución, etc. |
| unidad_medida | VARCHAR(30) | Unidad, ml, tabletas |
| atc | VARCHAR(30) | Clasificación ATC (opcional) |
| requiere_formula | BOOLEAN | Sí/No |
| controlado | BOOLEAN | Medicamento controlado |
| cadena_frio | BOOLEAN | Requiere cadena de frío |
| stock_minimo | INT | Alerta de stock bajo |
| stock_maximo | INT | Opcional |
| activo | BOOLEAN | Estado Activo/Inactivo |
| created_at | TIMESTAMP | |

**Funciones:** Crear, editar, inactivar, buscar medicamentos.

### 2.2 Proveedores (`proveedores_farmacia`)

| Campo | Tipo |
|-------|------|
| id | BIGSERIAL |
| nit | VARCHAR(20) |
| nombre | VARCHAR(200) |
| direccion | VARCHAR(255) |
| telefono | VARCHAR(50) |
| email | VARCHAR(255) |
| registro_invima | VARCHAR(100) |
| activo | BOOLEAN |
| created_at | TIMESTAMP |

### 2.3 Inventario por lote (`inventario_farmacia`)

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | BIGSERIAL | PK |
| medicamento_id | BIGINT | FK medicamentos_farmacia |
| lote | VARCHAR(80) | Número de lote |
| fecha_vencimiento | DATE | |
| cantidad | INT | Disponible |
| ubicacion | VARCHAR(100) | Estantería/bodega |
| created_at, updated_at | TIMESTAMP | |

**Funciones:** Consultar inventario, por lote, por medicamento, alertas mínimo/vencimiento, ajustes.

### 2.4 Recepción de medicamentos (`recepciones_farmacia`, `recepcion_farmacia_detalle`)

**Cabecera:** numero_recepcion, proveedor_id, fecha_recepcion, usuario_id.  
**Detalle:** medicamento_id, lote, fecha_vencimiento, cantidad, precio_unitario, ubicacion.

Genera movimiento de entrada en inventario y registro en kardex.

### 2.5 Kardex (`kardex_farmacia`)

| Campo | Tipo |
|-------|------|
| id | BIGSERIAL |
| fecha | TIMESTAMP |
| tipo_movimiento | VARCHAR(20) | ENTRADA, SALIDA, AJUSTE, DEVOLUCION, TRANSFERENCIA |
| medicamento_id | BIGINT |
| lote | VARCHAR(80) |
| cantidad | INT | (+ o -) |
| usuario_id | BIGINT |
| documento_ref | VARCHAR(100) | ID recepción/dispensación/devolución |
| created_at | TIMESTAMP |

### 2.6 Dispensación (vinculada a Orden Clínica)

**Tabla `farmacia_dispensaciones` (ampliada):**

- orden_clinica_id (BIGINT, FK ordenes_clinicas) — orden médica de la HC.
- paciente_id (BIGINT).
- fecha_dispensacion (TIMESTAMP).
- usuario_id / entregado_por (regente).
- estado: PENDIENTE | PARCIAL | COMPLETADA | CANCELADA.

**Tabla `dispensacion_farmacia_detalle`:**

- dispensacion_id, medicamento_id, lote, cantidad_prescrita, cantidad_entregada.

Flujo: búsqueda de orden (por número orden, documento o nombre paciente) → visualización de medicamentos prescritos (detalle de la orden) → selección de lote y cantidad por ítem → validación stock y vencimiento → registro de entrega (salida en inventario + kardex). Órdenes parciales: entregar parte y dejar orden en PARCIAL para reclamar el resto.

### 2.7 Devoluciones (`devoluciones_farmacia`)

medicamento_id, lote, cantidad, motivo, usuario_id, paciente_id (opcional), dispensacion_id (opcional). Genera entrada en inventario y línea en kardex.

### 2.8 Control de medicamentos controlados

Para medicamentos con `controlado = true`: validar número de fórmula, médico prescriptor, y registrar para reportes regulatorios (mismo flujo de dispensación con validaciones adicionales).

---

## 3. Integración con Historia Clínica y Órdenes

- Las **órdenes de medicamento** se crean en Historia Clínica (OrdenClinica con `tipo = 'MEDICAMENTO'`). El campo `detalle` contiene la prescripción (medicamento, dosis, cantidad, etc.).
- **Farmacia** consume:
  - `GET /ordenes-clinicas/paciente/{id}` filtrado por tipo MEDICAMENTO, o
  - `GET /farmacia/ordenes-pendientes` (órdenes MEDICAMENTO no dispensadas o parcialmente dispensadas).
- Al **dispensar**, se envía `ordenId` + ítems (medicamento, lote, cantidad entregada). El backend actualiza estado de la orden (y opcionalmente resultado) y genera salida de inventario y kardex.
- **Facturación:** Los ítems dispensados pueden enviarse a facturación (cuenta paciente/EPS) en una fase posterior.

---

## 4. Roles

| Rol | Permisos |
|-----|-----------|
| REGENTE_FARMACIA | Ver órdenes, dispensar, consultar inventario, registrar devoluciones. No crear medicamentos ni ajustes críticos. |
| ADMIN (o ADMIN_FARMACIA) | Crear/editar medicamentos, proveedores, recepciones, ajustar inventario, reportes. |
| Auditor / Administración | Ver reportes, kardex, trazabilidad. |

En el sistema actual: módulo **FARMACIA** con acciones VER, DISPENSAR, CREAR, EDITAR para REGENTE_FARMACIA; ADMIN y SUPERADMINISTRADOR con acceso completo.

---

## 5. Pantallas recomendadas

- **Dashboard farmacia:** Órdenes pendientes, medicamentos por vencer, stock bajo, últimas dispensaciones.
- **Medicamentos:** CRUD maestro de medicamentos (listado, filtros, búsqueda).
- **Proveedores:** CRUD proveedores.
- **Recepción:** Alta de recepciones con detalle (medicamento, lote, vencimiento, cantidad, precio, ubicación).
- **Inventario:** Consulta por medicamento/lote, alertas, ajustes.
- **Dispensación / Órdenes médicas:** Búsqueda por orden, documento o paciente; vista de orden con ítems; selección de lote y cantidad; registro de entrega (total/parcial).
- **Kardex:** Historial de movimientos por medicamento o global.
- **Devoluciones:** Registro de devolución (medicamento, lote, cantidad, motivo).
- **Reportes:** Inventario actual, próximos a vencer, kardex por medicamento, más dispensados, consumo por médico (auditoría).

---

## 6. Alertas

- **Vencimiento:** Por ejemplo alertas a 90 y 30 días.
- **Stock mínimo:** Cuando cantidad &lt; stock_minimo del maestro.

---

## 7. Seguridad y auditoría

Registrar en operaciones críticas: usuario, fecha, acción, documento relacionado (y opcionalmente IP). Kardex y tablas de dispensación/devolución proveen trazabilidad.
