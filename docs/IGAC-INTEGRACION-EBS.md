# Integración IGAC – Límites oficiales en el módulo EBS

El módulo EBS utiliza el catálogo **IGAC (Instituto Geográfico Agustín Codazzi)** para asociar territorios a la división oficial colombiana: **Departamentos → Municipios → Veredas**.

## Arquitectura

- **Catálogo en schema `public`**: Las tablas `igac_departamentos`, `igac_municipios` e `igac_veredas` viven en el schema público y se comparten entre todos los tenants. No se depende del servidor público de IGAC; los datos se sirven desde nuestra base.
- **Enlace en EBS**: Cada microterritorio EBS (`ebs_territories`) puede tener opcionalmente `igac_departamento_codigo`, `igac_municipio_codigo` e `igac_vereda_codigo`. La UI muestra la ruta legible (ej. *Antioquia › Medellín › Santa Elena*) y permite asignar o editar desde **EBS → Territorios**.

## Datos semilla

Al arrancar el backend, `IgacPublicSchemaInitializer` crea las tablas en `public` e inserta una **muestra** de datos DANE (varios departamentos, municipios y veredas de ejemplo). Es suficiente para probar la cascada y la asignación.

## Cargar el catálogo completo (recomendación)

Para no depender del servidor público de IGAC, se recomienda **descargar la capa oficial** y cargar los datos en nuestra base:

1. **Fuentes oficiales**
   - [Datos abiertos IGAC](https://www.igac.gov.co/datos-abiertos/)
   - [Colombia en Mapas](https://colombiaenmapas.gov.co) → Límites
   - Divipola DANE (códigos de departamentos y municipios)

2. **Formatos**: Shapefile (SHP), GeoJSON, CSV con códigos DANE.

3. **Carga en BD**
   - **Departamentos**: INSERT en `public.igac_departamentos` (codigo_dane 2 dígitos, nombre).
   - **Municipios**: INSERT en `public.igac_municipios` (codigo_dane 5 dígitos, departamento_codigo, nombre).
   - **Veredas**: INSERT en `public.igac_veredas` (codigo, municipio_codigo, nombre). Opcionalmente, columna `geometry_json` para GeoJSON simplificado si se desea servir geometría desde nuestro API.

4. **Script de ejemplo** (PostgreSQL):

```sql
-- Ejemplo: más departamentos
INSERT INTO public.igac_departamentos (codigo_dane, nombre)
VALUES ('25', 'Cundinamarca'), ('76', 'Valle del Cauca')
ON CONFLICT (codigo_dane) DO NOTHING;

-- Ejemplo: más municipios
INSERT INTO public.igac_municipios (codigo_dane, departamento_codigo, nombre)
VALUES ('25001', '25', 'Agua de Dios')
ON CONFLICT (codigo_dane) DO NOTHING;
```

5. **GeoJSON de veredas**: Si se dispone de geometría (p. ej. desde un Shapefile convertido a GeoJSON), se puede actualizar `geometry_json` en `public.igac_veredas`. El endpoint `GET /igac/veredas/{codigo}/geojson` devuelve ese JSON para uso en mapas.

## API REST

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/igac/departamentos` | Lista todos los departamentos |
| GET | `/igac/municipios?departamentoCodigo=05` | Municipios de un departamento |
| GET | `/igac/veredas?municipioCodigo=05001` | Veredas de un municipio |
| PUT | `/ebs/territories/{id}/igac` | Actualiza códigos IGAC del territorio (body: igacDepartamentoCodigo, igacMunicipioCodigo, igacVeredaCodigo) |
| GET | `/igac/veredas/{codigo}/geojson` | GeoJSON de la vereda (si existe geometry_json) |

Todos los endpoints de IGAC y EBS requieren autenticación y roles con permiso EBS.

## Flujo en la aplicación

1. El usuario entra a **EBS → Territorios** y selecciona un microterritorio.
2. En la tarjeta principal se muestra **Límites oficiales IGAC** (ruta actual o "Sin asignar").
3. Al hacer clic en **Asignar / Editar** se despliegan los selectores en cascada: Departamento → Municipio → Vereda (opcional).
4. Al **Guardar límites IGAC** se llama a `PUT /ebs/territories/{id}/igac` y se recarga la lista; la ruta se muestra en el listado de territorios y en la barra del territorio seleccionado.

Autor: Ing. J Sebastian Vargas S
