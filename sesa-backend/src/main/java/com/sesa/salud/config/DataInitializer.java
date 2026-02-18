/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.config;

import com.sesa.salud.entity.Usuario;
import com.sesa.salud.entity.master.Modulo;
import com.sesa.salud.entity.master.Role;
import com.sesa.salud.entity.master.Submodulo;
import com.sesa.salud.entity.master.TenantUsuarioLogin;
import com.sesa.salud.repository.UsuarioRepository;
import com.sesa.salud.repository.master.ModuloRepository;
import com.sesa.salud.repository.master.RoleRepository;
import com.sesa.salud.repository.master.SubmoduloRepository;
import com.sesa.salud.repository.master.TenantUsuarioLoginRepository;
import com.sesa.salud.security.RoleConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final TenantUsuarioLoginRepository tenantUsuarioLoginRepository;
    private final ModuloRepository moduloRepository;
    private final SubmoduloRepository submoduloRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initModulos();
        initSubmodulos();
        initRoles();
        initAdminPublic();
    }

    private void initRoles() {
        if (roleRepository.count() > 0) return;
        var roles = new Role[]{
            role(RoleConstants.SUPERADMINISTRADOR, "Super Usuario"),
            role(RoleConstants.ADMIN, "Administrador del Sistema"),
            role(RoleConstants.MEDICO, "Médico"),
            role(RoleConstants.ODONTOLOGO, "Odontólogo/a"),
            role(RoleConstants.BACTERIOLOGO, "Bacteriólogo"),
            role(RoleConstants.ENFERMERO, "Enfermero/a"),
            role(RoleConstants.JEFE_ENFERMERIA, "Jefe de Enfermería"),
            role(RoleConstants.AUXILIAR_ENFERMERIA, "Auxiliar de Enfermería"),
            role(RoleConstants.PSICOLOGO, "Psicólogo"),
            role(RoleConstants.REGENTE_FARMACIA, "Regente de Farmacia"),
            role(RoleConstants.RECEPCIONISTA, "Recepcionista")
        };
        for (Role r : roles) roleRepository.save(r);
        log.info("Roles del sistema inicializados: {}", roles.length);
    }

    private static Role role(String codigo, String nombre) {
        return Role.builder().codigo(codigo).nombre(nombre).build();
    }

    /* ===========================
     *  Módulos (top-level)
     * =========================== */
    private void initModulos() {
        if (moduloRepository.count() > 0) return;
        var modulos = new Modulo[]{
                mod("AGENDAS", "Agendas"),
                mod("ALERTAS", "Alertas"),
                mod("CARACTERIZACION", "Caracterización"),
                mod("ENCUESTAS", "Encuestas"),
                mod("FACTURACION", "Facturación"),
                mod("HISTORIAS_CLINICAS", "Historias Clínicas"),
                mod("LABORATORIOS", "Laboratorios"),
                mod("MODELO_POBLACIONAL", "Modelo Poblacional"),
                mod("PERSONAS", "Personas"),
                mod("REPORTES", "Reportes"),
                mod("RIESGOS", "Riesgos"),
                mod("SEGUIMIENTO", "Seguimiento"),
                mod("SISTEMA", "Sistema"),
                mod("TABLERO_CONTROL", "Tablero de control")
        };
        for (Modulo m : modulos) moduloRepository.save(m);
        log.info("Módulos iniciales creados: {}", modulos.length);
    }

    private static Modulo mod(String codigo, String nombre) {
        return Modulo.builder().codigo(codigo).nombre(nombre).build();
    }

    /* ===========================
     *  Submódulos por módulo
     * =========================== */
    private void initSubmodulos() {
        if (submoduloRepository.count() > 0) return;

        List<Modulo> all = moduloRepository.findAll();

        // AGENDAS
        seedSubs(all, "AGENDAS",
            sub("agendamiento_citas", "Agendamiento de citas"),
            sub("agenda_profesional", "Agenda (profesional)"),
            sub("configuracion_agendas", "Configuración"),
            sub("gestion_preagenda", "Gestión de agenda (pre agenda)"),
            sub("gestion_agenda_admin", "Gestión de agendas (administración)"),
            sub("gestion_agenda_profesional", "Gestión de agendas (profesional)"),
            sub("lista_espera", "Lista de espera"),
            sub("registrar_lista_espera", "Registrar paciente lista de espera"),
            sub("reportes_agenda", "Reportes – Export agenda profesional")
        );

        // ALERTAS
        seedSubs(all, "ALERTAS",
            sub("configuracion_alertas", "Configuración"),
            sub("gestion_alertas", "Gestión de alertas"),
            sub("alertas_masivas_admin", "Gestión de alertas masivas (administración)"),
            sub("todas_alertas", "Gestión de todas las alertas")
        );

        // CARACTERIZACIÓN
        seedSubs(all, "CARACTERIZACION",
            sub("calculo_pe_dt", "Cálculo de PE y DT"),
            sub("calculo_riesgos", "Cálculo de riesgos"),
            sub("caracterizacion_bd", "Caracterización BD"),
            sub("caracterizacion_general", "Caracterización"),
            sub("caracterizacion_movil", "Caracterización móvil"),
            sub("caracterizacion_web", "Caracterización web"),
            sub("carga_manual", "Carga manual de caracterización"),
            sub("configuracion_caract", "Configuración"),
            sub("correccion_datos", "Corrección datos caracterización"),
            sub("gestion_admin_caract", "Gestión de caracterización (administración)"),
            sub("reportes_caract", "Reportes"),
            sub("seguimiento_admin_caract", "Seguimiento a caracterización (admin)")
        );

        // ENCUESTAS
        seedSubs(all, "ENCUESTAS",
            sub("configuracion_encuestas", "Configuración"),
            sub("encuestas_movil", "Encuestas móvil"),
            sub("reportes_encuestas", "Reportes")
        );

        // FACTURACIÓN
        seedSubs(all, "FACTURACION",
            sub("arqueo", "Arqueo facturación"),
            sub("configuracion_fact", "Configuración"),
            sub("facturacion_masiva", "Facturación masiva"),
            sub("facturar", "Facturar"),
            sub("rips", "Generación de RIPS"),
            sub("gestion_facturacion", "Gestión de facturación"),
            sub("facturas_electronicas", "Gestión de facturas electrónicas"),
            sub("tablero_facturacion", "Tablero de control")
        );

        // HISTORIAS CLÍNICAS
        seedSubs(all, "HISTORIAS_CLINICAS",
            sub("carga_archivos", "Carga de archivos"),
            sub("configuracion_hc", "Configuración"),
            sub("gestion_hc", "Gestión de historias clínicas"),
            sub("historial_usuario", "Historial del usuario"),
            sub("pendientes_hc", "Historias clínicas pendientes"),
            sub("nueva_hc", "Nueva historia clínica"),
            sub("ordenes_medicas", "Órdenes médicas"),
            sub("proximo_control", "Próximo control paciente"),
            sub("reportes_hc", "Reporte historia clínica consolidada")
        );

        // LABORATORIOS
        seedSubs(all, "LABORATORIOS",
            sub("resultados_equipos", "Cargar resultados equipos laboratorio"),
            sub("resultados_externos", "Cargar resultados laboratorios externos"),
            sub("generador_ordenes", "Generador de órdenes")
        );

        // MODELO POBLACIONAL
        seedSubs(all, "MODELO_POBLACIONAL",
            sub("modelo_poblacional", "Modelo poblacional")
        );

        // PERSONAS
        seedSubs(all, "PERSONAS",
            sub("gestion_personas", "Gestión de personas"),
            sub("gestion_programas", "Gestión de programas")
        );

        // REPORTES
        seedSubs(all, "REPORTES",
            sub("configuracion_reportes", "Configuración"),
            sub("generador_dinamico", "Generador dinámico")
        );

        // RIESGOS
        seedSubs(all, "RIESGOS",
            sub("gestion_riesgos_admin", "Gestión de riesgos (administración)")
        );

        // SEGUIMIENTO
        seedSubs(all, "SEGUIMIENTO",
            sub("aprobacion_casos", "Aprobación de casos"),
            sub("configuracion_seg", "Configuración"),
            sub("gestion_casos_admin", "Gestión de casos (administración)"),
            sub("registrar_caso", "Registrar caso"),
            sub("reporte_estatico", "Reporte seguimiento estático"),
            sub("reportes_seg", "Reportes"),
            sub("seguimiento_personal", "Seguimiento a casos (personal)"),
            sub("seguimiento_general", "Seguimiento"),
            sub("seguimiento_movil", "Seguimiento móvil")
        );

        // SISTEMA
        seedSubs(all, "SISTEMA",
            sub("auditoria", "Auditoría"),
            sub("configuracion_equipo", "Configuración de equipo"),
            sub("crear_empresa", "Crear empresa"),
            sub("desarrollador_movil", "Desarrollador móvil"),
            sub("editar_empresa", "Editar empresa"),
            sub("gestion_administradora", "Gestión administradora"),
            sub("dispositivos_moviles", "Gestión dispositivos móviles"),
            sub("gestion_empresas", "Gestión empresas"),
            sub("ips", "Institución prestadora de servicio"),
            sub("manuales_usuario", "Manuales de usuario"),
            sub("parametros", "Parámetros del sistema"),
            sub("perfil_usuario", "Perfil de usuario"),
            sub("rh", "RH"),
            sub("sincronizacion", "Sincronización")
        );

        // TABLERO DE CONTROL
        seedSubs(all, "TABLERO_CONTROL",
            sub("configuracion_tablero", "Configuración"),
            sub("dashboard", "Tablero de control")
        );

        log.info("Submódulos iniciales creados");
    }

    private void seedSubs(List<Modulo> allModules, String moduloCodigo, Submodulo... subs) {
        Modulo parent = allModules.stream()
                .filter(m -> m.getCodigo().equals(moduloCodigo))
                .findFirst()
                .orElse(null);
        if (parent == null) {
            log.warn("Módulo {} no encontrado, omitiendo submódulos", moduloCodigo);
            return;
        }
        for (Submodulo s : subs) {
            s.setModulo(parent);
            submoduloRepository.save(s);
        }
    }

    private static Submodulo sub(String codigo, String nombre) {
        return Submodulo.builder().codigo(codigo).nombre(nombre).build();
    }

    /* ===========================
     *  Admin usuario público
     * =========================== */
    private void initAdminPublic() {
        var optAdmin = usuarioRepository.findByEmail("admin@sesa.local");
        if (optAdmin.isEmpty()) {
            Usuario admin = Usuario.builder()
                    .email("admin@sesa.local")
                    .passwordHash(passwordEncoder.encode("Admin123!"))
                    .nombreCompleto("Administrador SESA")
                    .activo(true)
                    .roles(Set.of("SUPERADMINISTRADOR", "ADMIN"))
                    .build();
            usuarioRepository.save(admin);
            log.info("Usuario inicial creado: admin@sesa.local / Admin123!");
        } else {
            Usuario admin = optAdmin.get();
            if (admin.getRoles() == null || admin.getRoles().isEmpty()
                    || !admin.getRoles().contains("SUPERADMINISTRADOR") || !admin.getRoles().contains("ADMIN")) {
                admin.setRoles(Set.of("SUPERADMINISTRADOR", "ADMIN"));
                usuarioRepository.save(admin);
                log.info("Roles SUPERADMINISTRADOR y ADMIN asignados a admin@sesa.local");
            }
        }
        if (tenantUsuarioLoginRepository.findByEmail("admin@sesa.local").isEmpty()) {
            tenantUsuarioLoginRepository.save(TenantUsuarioLogin.builder()
                    .email("admin@sesa.local")
                    .schemaName("public")
                    .build());
            log.info("Login tenant registrado: admin@sesa.local -> public");
        }
    }
}
