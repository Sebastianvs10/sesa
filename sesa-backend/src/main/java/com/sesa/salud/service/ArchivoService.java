/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * Servicio central de almacenamiento de archivos en base de datos.
 * Persiste el contenido como base64 TEXT en {@code public.archivo_almacenamiento}
 * y retorna un UUID que se usa para construir URLs estables.
 */
public interface ArchivoService {

    /**
     * Guarda un archivo recibido como {@link MultipartFile}.
     *
     * @param file          Archivo a guardar.
     * @param entidad       Tipo de entidad que referencia el archivo (p. ej. "EMPRESA_LOGO").
     * @param entidadId     ID de la entidad (como String).
     * @param schemaTenant  Schema del tenant propietario. Usar "public" para recursos maestros.
     * @param accesoPublico {@code true} si el archivo se sirve sin autenticación.
     * @return UUID asignado al archivo (usado en la URL {@code /api/archivos/{uuid}}).
     */
    String guardar(MultipartFile file, String entidad, String entidadId,
                   String schemaTenant, boolean accesoPublico);

    /**
     * Guarda un archivo ya codificado en base64.
     *
     * @param base64        Contenido del archivo codificado en base64 (sin prefijo data:URI).
     * @param contentType   MIME type del archivo.
     * @param nombreOriginal Nombre original del archivo.
     * @param entidad       Tipo de entidad.
     * @param entidadId     ID de la entidad.
     * @param schemaTenant  Schema del tenant.
     * @param accesoPublico {@code true} si se sirve sin autenticación.
     * @return UUID asignado al archivo.
     */
    String guardarBase64(String base64, String contentType, String nombreOriginal,
                         String entidad, String entidadId,
                         String schemaTenant, boolean accesoPublico);

    /**
     * Recupera el contenido de un archivo por su UUID.
     *
     * @param uuid UUID del archivo.
     * @return Resultado con los datos decodificados (bytes) y metadata, o vacío si no existe.
     */
    Optional<ArchivoRecurso> obtener(String uuid);

    /**
     * Elimina un archivo por su UUID.
     */
    void eliminar(String uuid);

    /** DTO de retorno con los bytes decodificados y su tipo de contenido. */
    record ArchivoRecurso(
            byte[] datos,
            String contentType,
            String nombreOriginal,
            boolean accesoPublico,
            String schemaTenant
    ) {}
}
