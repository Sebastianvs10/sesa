import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { superAdminGuard } from './core/guards/super-admin.guard';
import { medicoGuard } from './core/guards/medico.guard';
import { roleGuard, rolesManagementGuard } from './core/guards/role.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'login',
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login.page').then(
        (m) => m.LoginPageComponent,
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
    path: 'notificaciones',
    canActivate: [authGuard, roleGuard('NOTIFICACIONES')],
    loadComponent: () =>
      import('./features/notificaciones/notificaciones.page').then(
        (m) => m.NotificacionesPageComponent,
      ),
  },
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];

