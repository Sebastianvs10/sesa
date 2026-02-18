/**
 * Dashboard clínico dinámico por rol - Premium Edition.
 * Carga datos reales según permisos del usuario.
 * Autor: Ing. J Sebastian Vargas S
 */

import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faUsers,
  faCalendarCheck,
  faFileMedical,
  faFlaskVial,
  faExclamationTriangle,
  faHospital,
  faPills,
  faFileInvoiceDollar,
  faChartLine,
  faChartBar,
} from '@fortawesome/free-solid-svg-icons';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { SesaCalendarComponent } from '../../shared/components/sesa-calendar/sesa-calendar.component';
import { AuthService } from '../../core/services/auth.service';
import { DashboardService, DashboardData } from '../../core/services/dashboard.service';
import { CitaDto, CitaService } from '../../core/services/cita.service';
import { PermissionsService } from '../../core/services/permissions.service';

interface StatCard {
  titulo: string;
  valor: number | string;
  chip?: string;
  chipClase?: string;
  icon: any;
  link?: string;
  gradient: string;
}

@Component({
  standalone: true,
  selector: 'sesa-dashboard-page',
  imports: [CommonModule, RouterLink, FontAwesomeModule, SesaCardComponent, SesaCalendarComponent],
  templateUrl: './dashboard.page.html',
  styleUrl: './dashboard.page.scss',
})
export class DashboardPageComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly dashboardService = inject(DashboardService);
  private readonly citaService = inject(CitaService);
  readonly permissions = inject(PermissionsService);

  data: DashboardData | null = null;
  loading = true;
  highlightedDates = new Set<string>();
  selectedDate: string | null = null;
  citasParaFecha: CitaDto[] = [];
  loadingCitasParaFecha = false;

  faChartLine = faChartLine;
  faChartBar = faChartBar;

  ngOnInit(): void {
    this.load();
  }

  get isAdmin(): boolean {
    const role = this.auth.currentUser()?.role ?? '';
    return role === 'ADMIN' || role === 'SUPERADMINISTRADOR';
  }

  load(): void {
    const role = this.auth.currentUser()?.role ?? '';
    this.loading = true;
    this.dashboardService.load(role).subscribe({
      next: (d) => {
        this.data = d;
        this.loading = false;
        this.highlightedDates = this.dashboardService.getCitasDatesForMonth(d.citasHoy);
        this.citasParaFecha = d.citasHoy ?? [];
        this.selectedDate = null;
      },
      error: () => {
        this.loading = false;
        this.data = { role, resumen: {}, citasHoy: [], fechaHoy: '', loading: false, error: 'Error al cargar' };
      },
    });
  }

  get roleLabel(): string {
    const role = this.auth.currentUser()?.role ?? '';
    const map: Record<string, string> = {
      SUPERADMINISTRADOR: 'Super Usuario',
      ADMIN: 'Administrador',
      MEDICO: 'Médico',
      ODONTOLOGO: 'Odontólogo',
      BACTERIOLOGO: 'Bacteriólogo',
      ENFERMERO: 'Enfermero',
      JEFE_ENFERMERIA: 'Jefe de Enfermería',
      AUXILIAR_ENFERMERIA: 'Auxiliar de Enfermería',
      PSICOLOGO: 'Psicólogo',
      REGENTE_FARMACIA: 'Regente de Farmacia',
      RECEPCIONISTA: 'Recepcionista',
    };
    return map[role] ?? role;
  }

  /** Tarjetas de resumen visibles según rol, ordenadas por prioridad */
  get statCards(): StatCard[] {
    const r = this.data?.resumen ?? {};
    const citasHoy = (this.data?.citasHoy ?? []).length;
    const cards: StatCard[] = [];

    if (this.permissions.canAccess('CITAS')) {
      cards.push({
        titulo: 'Citas hoy',
        valor: citasHoy,
        chip: citasHoy > 0 ? 'Programadas' : 'Sin citas',
        chipClase: citasHoy > 0 ? 'sesa-chip-success' : 'sesa-chip-secondary',
        icon: faCalendarCheck,
        link: '/citas',
        gradient: 'linear-gradient(135deg, #1f6ae1, #38bdf8)',
      });
    }
    if (this.permissions.canAccess('PACIENTES') && r.totalPacientes !== undefined) {
      cards.push({
        titulo: 'Total pacientes',
        valor: r.totalPacientes,
        chip: 'Registrados',
        chipClase: 'sesa-chip-info',
        icon: faUsers,
        link: '/pacientes',
        gradient: 'linear-gradient(135deg, #0ea5e9, #06b6d4)',
      });
    }
    if (this.permissions.canAccess('HISTORIA_CLINICA') && r.totalConsultas !== undefined) {
      cards.push({
        titulo: 'Consultas',
        valor: r.totalConsultas,
        chip: 'Historial',
        chipClase: 'sesa-chip-primary',
        icon: faFileMedical,
        link: '/historia-clinica',
        gradient: 'linear-gradient(135deg, #6366f1, #8b5cf6)',
      });
    }
    if (this.permissions.canAccess('LABORATORIOS') && r.totalOrdenes !== undefined) {
      cards.push({
        titulo: 'Órdenes laboratorio',
        valor: r.totalOrdenes,
        chip: 'Activas',
        chipClase: 'sesa-chip-warning',
        icon: faFlaskVial,
        link: '/laboratorios',
        gradient: 'linear-gradient(135deg, #f59e0b, #f97316)',
      });
    }
    if (this.permissions.canAccess('URGENCIAS')) {
      cards.push({
        titulo: 'Urgencias',
        valor: r.totalCitas ?? 0,
        chip: 'Monitoreo',
        chipClase: 'sesa-chip-error',
        icon: faExclamationTriangle,
        link: '/urgencias',
        gradient: 'linear-gradient(135deg, #ef4444, #f97316)',
      });
    }
    if (this.permissions.canAccess('HOSPITALIZACION')) {
      cards.push({
        titulo: 'Hospitalización',
        valor: '-',
        chip: 'Camas',
        chipClase: 'sesa-chip-primary',
        icon: faHospital,
        link: '/hospitalizacion',
        gradient: 'linear-gradient(135deg, #8b5cf6, #a78bfa)',
      });
    }
    if (this.permissions.canAccess('FARMACIA')) {
      cards.push({
        titulo: 'Farmacia',
        valor: '-',
        chip: 'Dispensación',
        chipClase: 'sesa-chip-info',
        icon: faPills,
        link: '/farmacia',
        gradient: 'linear-gradient(135deg, #14b8a6, #06b6d4)',
      });
    }
    if (this.permissions.canAccess('FACTURACION') && r.totalFacturas !== undefined) {
      cards.push({
        titulo: 'Facturación',
        valor: r.totalFacturas,
        chip: r.totalFacturado != null ? `$${Number(r.totalFacturado).toLocaleString('es-CO')}` : 'Facturas',
        chipClase: 'sesa-chip-success',
        icon: faFileInvoiceDollar,
        link: '/facturacion',
        gradient: 'linear-gradient(135deg, #22c55e, #16a34a)',
      });
    }

    return cards;
  }

  formatHora(fechaHora?: string): string {
    if (!fechaHora) return '--:--';
    const d = new Date(fechaHora);
    return d.toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit', hour12: false });
  }

  get proximasCitas(): CitaDto[] {
    return this.citasParaFecha.slice(0, 10);
  }

  onDateSelected(fecha: string): void {
    this.selectedDate = fecha;
    this.loadingCitasParaFecha = true;
    this.citaService.list(fecha).subscribe({
      next: (citas) => { this.citasParaFecha = citas ?? []; this.loadingCitasParaFecha = false; },
      error: () => { this.citasParaFecha = []; this.loadingCitasParaFecha = false; },
    });
  }

  get fechaSeleccionadaFormateada(): string {
    if (!this.selectedDate) {
      return new Date().toLocaleDateString('es-CO', { weekday: 'long', day: 'numeric', month: 'long' });
    }
    return new Date(this.selectedDate + 'T12:00:00').toLocaleDateString('es-CO', { weekday: 'long', day: 'numeric', month: 'long' });
  }

  get fechaHoyFormateada(): string {
    return new Date().toLocaleDateString('es-CO', { weekday: 'long', day: 'numeric', month: 'long' });
  }
}
