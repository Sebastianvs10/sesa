/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.security.JwtPrincipal;
import com.sesa.salud.service.ArchivoService;
import com.sesa.salud.service.ArchivoService.ArchivoRecurso;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Endpoint central para servir y subir archivos almacenados en BD.
 *
 * <ul>
 *   <li>{@code GET /archivos/{uuid}}  — Sirve el archivo con headers de caché.
 *       Acceso público para logos de empresa; requiere JWT para recursos privados.</li>
 *   <li>{@code POST /archivos}        — Sube un archivo y retorna el UUID + URL.</li>
 *   <li>{@code DELETE /archivos/{uuid}} — Elimina un archivo (solo ADMIN/SUPERADMIN).</li>
 * </ul>
 */
@RestController
@RequestMapping("/archivos")
@RequiredArgsConstructor
@Slf4j
public class ArchivoController {

    private final ArchivoService archivoService;

    /**
     * Sirve el archivo identificado por {@code uuid}.
     *
     * <p>Acceso público si {@code accesoPublico = true} (logos de empresa).
     * De lo contrario se requiere JWT con schema coincidente o rol SUPERADMINISTRADOR.</p>
     */
    @GetMapping("/{uuid}")
    public ResponseEntity<byte[]> getArchivo(@PathVariable String uuid,
                                              Authentication auth) {
        ArchivoRecurso recurso = archivoService.obtener(uuid).orElse(null);
        if (recurso == null) {
            return ResponseEntity.notFound().build();
        }

        // Validación de acceso para recursos privados
        if (!recurso.accesoPublico()) {
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            if (auth.getPrincipal() instanceof JwtPrincipal principal) {
                boolean esSuperAdmin = principal.hasRole("SUPERADMINISTRADOR");
                boolean schemaCoincide = recurso.schemaTenant().equals("public")
                        || recurso.schemaTenant().equals(principal.schema());
                if (!esSuperAdmin && !schemaCoincide) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(recurso.contentType()))
                // Cache de 1 día para recursos públicos (logos); sin caché para privados
                .header("Cache-Control", recurso.accesoPublico()
                        ? "public, max-age=86400, immutable"
                        : "no-store, no-cache")
                // ETag basado en el UUID (el contenido no cambia sin nuevo UUID)
                .header("ETag", "\"" + uuid + "\"")
                .body(recurso.datos());
    }

    /**
     * Sube un archivo genérico y retorna el UUID y la URL de acceso.
     * Requiere autenticación.
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> uploadArchivo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "entidad", defaultValue = "GENERICO") String entidad,
            @RequestParam(value = "entidadId", defaultValue = "") String entidadId,
            @RequestParam(value = "accesoPublico", defaultValue = "false") boolean accesoPublico,
            Authentication auth) {

        String schemaTenant = "public";
        if (auth != null && auth.getPrincipal() instanceof JwtPrincipal p) {
            schemaTenant = p.schema() != null ? p.schema() : "public";
        }

        String uuid = archivoService.guardar(file, entidad, entidadId, schemaTenant, accesoPublico);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "uuid", uuid,
                "url", "/archivos/" + uuid
        ));
    }

    /**
     * Elimina un archivo por su UUID. Solo ADMIN o SUPERADMINISTRADOR.
     */
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteArchivo(@PathVariable String uuid) {
        archivoService.eliminar(uuid);
        return ResponseEntity.noContent().build();
    }
}
