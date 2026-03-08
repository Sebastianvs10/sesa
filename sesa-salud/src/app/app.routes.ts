import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { superAdminGuard } from './core/guards/super-admin.guard';
import { medicoGuard } from './core/guards/medico.guard';
import { roleGuard, rolesManagementGuard } from './core/guards/role.guard';
import { portalGuard } from './core/guards/portal.guard';
import { videoconsultaGuard } from './core/guards/videoconsulta.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'login',
  },

  // ── Portal del Paciente ─────────────────────────────────────────────
  {
    path: 'portal',
    canActivate: [portalGuard],
    loadComponent: () =>
      import('./features/portal/portal-layout.component').then(
        (m) => m.PortalLayoutComponent,
      ),
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'inicio',
      },
      {
        path: 'inicio',
        loadComponent: () =>
          import('./features/portal/portal-dashboard.page').then(
            (m) => m.PortalDashboardPageComponent,
          ),
      },
      {
        path: 'historia-clinica',
        loadComponent: () =>
          import('./features/portal/portal-historia-clinica.page').then(
            (m) => m.PortalHistoriaClinicaPageComponent,
          ),
      },
      {
        path: 'laboratorios',
        loadComponent: () =>
          import('./features/portal/portal-laboratorios.page').then(
            (m) => m.PortalLaboratoriosPageComponent,
          ),
      },
      {
        path: 'ordenes',
        loadComponent: () =>
          import('./features/portal/portal-ordenes.page').then(
            (m) => m.PortalOrdenesPageComponent,
          ),
      },
      {
        path: 'consentimientos',
        loadComponent: () =>
          import('./features/portal/portal-consentimientos.page').then(
            (m) => m.PortalConsentimientosPageComponent,
          ),
      },
      {
        path: 'perfil',
        loadComponent: () =>
          import('./features/portal/portal-perfil.page').then(
            (m) => m.PortalPerfilPageComponent,
          ),
      },
    ],
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login.page').then(
        (m) => m.LoginPageComponent,
      ),
  },
  {
    path: 'verificar-receta',
    loadComponent: () =>
      import('./features/receta-electronica/verificar-receta.page').then(
        (m) => m.VerificarRecetaPageComponent,
      ),
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/dashboard/dashboard.page').then(
        (m) => m.DashboardPageComponent,
      ),
  },
  {
    path: 'pacientes',
    canActivate: [authGuard, roleGuard('PACIENTES')],
    loadComponent: () =>
      import('./features/pacientes/pacientes-list.page').then(
        (m) => m.PacientesListPageComponent,
      ),
  },
  {
    path: 'pacientes/nuevo',
    canActivate: [authGuard, roleGuard('PACIENTES')],
    loadComponent: () =>
      import('./features/pacientes/paciente-form.page').then(
        (m) => m.PacienteFormPageComponent,
      ),
  },
  {
    path: 'pacientes/:id/editar',
    canActivate: [authGuard, roleGuard('PACIENTES')],
    loadComponent: () =>
      import('./features/pacientes/paciente-form.page').then(
        (m) => m.PacienteFormPageComponent,
      ),
  },
  {
    path: 'historia-clinica',
    canActivate: [authGuard, roleGuard('HISTORIA_CLINICA')],
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/historia-clinica/historia-clinica.page').then(
            (m) => m.HistoriaClinicaPageComponent,
          ),
      },
      {
        path: 'nueva',
        canActivate: [medicoGuard],
        loadComponent: () =>
          import('./features/historia-clinica/buscar-paciente.page').then(
            (m) => m.BuscarPacientePageComponent,
          ),
      },
      {
        path: ':pacienteId/nueva',
        canActivate: [medicoGuard],
        loadComponent: () =>
          import('./features/historia-clinica/crear-historia.page').then(
            (m) => m.CrearHistoriaPageComponent,
          ),
      },
    ],
  },
  {
    path: 'laboratorios',
    canActivate: [authGuard, roleGuard('LABORATORIOS')],
    loadComponent: () =>
      import('./features/laboratorios/laboratorios.page').then(
        (m) => m.LaboratoriosPageComponent,
      ),
  },
  {
    path: 'imagenes-diagnosticas',
    canActivate: [authGuard, roleGuard('IMAGENES')],
    loadComponent: () =>
      import('./features/imagenes-diagnosticas/imagenes-diagnosticas.page').then(
        (m) => m.ImagenesDiagnosticasPageComponent,
      ),
  },
  {
    path: 'urgencias',
    canActivate: [authGuard, roleGuard('URGENCIAS')],
    loadComponent: () =>
      import('./features/urgencias/urgencias.page').then(
        (m) => m.UrgenciasPageComponent,
      ),
  },
  {
    path: 'citas',
    canActivate: [authGuard, roleGuard('CITAS')],
    loadComponent: () =>
      import('./features/citas/citas.page').then(
        (m) => m.CitasPageComponent,
      ),
  },
  {
    path: 'roles',
    canActivate: [authGuard, rolesManagementGuard],
    loadComponent: () =>
      import('./features/roles/roles.page').then(
        (m) => m.RolesPageComponent,
      ),
  },
  {
    path: 'usuarios',
    canActivate: [authGuard, roleGuard('USUARIOS')],
    loadComponent: () =>
      import('./features/usuarios/usuarios.page').then(
        (m) => m.UsuariosPageComponent,
      ),
  },
  {
    path: 'hospitalizacion',
    canActivate: [authGuard, roleGuard('HOSPITALIZACION')],
    loadComponent: () =>
      import('./features/hospitalizacion/hospitalizacion.page').then(
        (m) => m.HospitalizacionPageComponent,
      ),
  },
  {
    path: 'farmacia',
    canActivate: [authGuard, roleGuard('FARMACIA')],
    loadComponent: () =>
      import('./features/farmacia/farmacia.page').then(
        (m) => m.FarmaciaPageComponent,
      ),
  },
  {
    path: 'facturacion',
    canActivate: [authGuard, roleGuard('FACTURACION')],
    loadComponent: () =>
      import('./features/facturacion/facturacion.page').then(
        (m) => m.FacturacionPageComponent,
      ),
  },
  {
    path: 'reportes',
    canActivate: [authGuard, roleGuard('REPORTES')],
    loadComponent: () =>
      import('./features/reportes/reportes.page').then(
        (m) => m.ReportesPageComponent,
      ),
  },
  {
    path: 'personal',
    canActivate: [authGuard, roleGuard('PERSONAL')],
    loadComponent: () =>
      import('./features/personal/personal.page').then(
        (m) => m.PersonalPageComponent,
      ),
  },
  {
    path: 'empresas',
    canActivate: [authGuard, superAdminGuard],
    loadComponent: () =>
      import('./features/empresas/empresas-list.page').then(
        (m) => m.EmpresasListPageComponent,
      ),
  },
  {
    path: 'empresas/nueva',
    canActivate: [authGuard, superAdminGuard],
    loadComponent: () =>
      import('./features/empresas/empresa-wizard.page').then(
        (m) => m.EmpresaWizardPageComponent,
      ),
  },
  {
    path: 'empresas/:id/editar',
    canActivate: [authGuard, superAdminGuard],
    loadComponent: () =>
      import('./features/empresas/empresa-wizard.page').then(
        (m) => m.EmpresaWizardPageComponent,
      ),
  },
  {
    path: 'mi-empresa',
    canActivate: [authGuard, roleGuard('EMPRESAS')],
    loadComponent: () =>
      import('./features/mi-empresa/mi-empresa.page').then(
        (m) => m.MiEmpresaPageComponent,
      ),
  },
  {
    path: 'agenda',
    canActivate: [authGuard, roleGuard('AGENDA')],
    loadComponent: () =>
      import('./features/agenda/agenda.page').then(
        (m) => m.AgendaPageComponent,
      ),
  },
  {
    path: 'notificaciones',
    canActivate: [authGuard, roleGuard('NOTIFICACIONES')],
    loadComponent: () =>
      import('./features/notificaciones/notificaciones.page').then(
        (m) => m.NotificacionesPageComponent,
      ),
  },
  {
    path: 'consulta-medica',
    canActivate: [authGuard, roleGuard('CONSULTA_MEDICA')],
    loadComponent: () =>
      import('./features/consulta-medica/consulta-medica.page').then(
        (m) => m.ConsultaMedicaPageComponent,
      ),
  },
  {
    path: 'videoconsulta/asistente',
    canActivate: [videoconsultaGuard],
    loadComponent: () =>
      import('./features/videoconsulta/videoconsulta-asistente.page').then(
        (m) => m.VideoconsultaAsistentePageComponent,
      ),
  },
  {
    path: 'videoconsulta',
    canActivate: [videoconsultaGuard],
    loadComponent: () =>
      import('./features/videoconsulta/videoconsulta-sala.page').then(
        (m) => m.VideoconsultaSalaPageComponent,
      ),
  },
  {
    path: 'odontologia',
    canActivate: [authGuard, roleGuard('ODONTOLOGIA')],
    loadComponent: () =>
      import('./features/odontologia/odontologia.page').then(
        (m) => m.OdontologiaPageComponent,
      ),
  },
  {
    path: 'evolucion-enfermeria',
    canActivate: [authGuard, roleGuard('EVOLUCION_ENFERMERIA')],
    loadComponent: () =>
      import('./features/evolucion-enfermeria/evolucion-enfermeria.page').then(
        (m) => m.EvolucionEnfermeriaPageComponent,
      ),
  },
  {
    path: 'ebs',
    canActivate: [authGuard, roleGuard('EBS')],
    loadComponent: () =>
      import('./features/ebs/ebs-layout.component').then(
        (m) => m.EbsLayoutComponent,
      ),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
      {
        path: 'inicio',
        loadComponent: () =>
          import('./features/ebs/ebs-inicio.page').then(
            (m) => m.EbsInicioPageComponent,
          ),
      },
      {
        path: 'territorios',
        loadComponent: () =>
          import('./features/ebs/ebs-territorios.page').then(
            (m) => m.EbsTerritoriosPageComponent,
          ),
      },
      {
        path: 'visitas',
        loadComponent: () =>
          import('./features/ebs/ebs-visitas.page').then(
            (m) => m.EbsVisitasPageComponent,
          ),
      },
      {
        path: 'visita/nueva',
        loadComponent: () =>
          import('./features/ebs/ebs-visita-nueva.page').then(
            (m) => m.EbsVisitaNuevaPageComponent,
          ),
      },
      {
        path: 'territorios/crear',
        loadComponent: () =>
          import('./features/ebs/ebs-territorio-crear.page').then(
            (m) => m.EbsTerritorioCrearPageComponent,
          ),
      },
      {
        path: 'asignacion',
        loadComponent: () =>
          import('./features/ebs/ebs-asignacion.page').then(
            (m) => m.EbsAsignacionPageComponent,
          ),
      },
      {
        path: 'brigadas',
        loadComponent: () =>
          import('./features/ebs/ebs-brigadas.page').then(
            (m) => m.EbsBrigadasPageComponent,
          ),
      },
      {
        path: 'reportes',
        loadComponent: () =>
          import('./features/ebs/ebs-reportes.page').then(
            (m) => m.EbsReportesPageComponent,
          ),
      },
      {
        path: 'alertas',
        loadComponent: () =>
          import('./features/ebs/ebs-alertas.page').then(
            (m) => m.EbsAlertasPageComponent,
          ),
      },
      {
        path: 'dashboard-supervisor',
        loadComponent: () =>
          import('./features/ebs/ebs-dashboard-supervisor.page').then(
            (m) => m.EbsDashboardSupervisorPageComponent,
          ),
      },
    ],
  },
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];

