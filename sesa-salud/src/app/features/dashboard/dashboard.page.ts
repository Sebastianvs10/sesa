/**
 * Dashboard clínico Premium — layout corporativo SaaS de alto nivel.
 * KPIs animados · gráficas por módulo · actividad en tiempo real · reloj vivo.
 * Autor: Ing. J Sebastian Vargas S
 */

import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faUsers, faCalendarCheck, faFileMedical, faFlaskVial,
  faExclamationTriangle, faHospital, faPills, faFileInvoiceDollar,
  faChartLine, faChartBar, faMapLocationDot, faArrowTrendUp,
  faArrowTrendDown, faCircleInfo,
} from '@fortawesome/free-solid-svg-icons';
import { forkJoin } from 'rxjs';
import { SesaCalendarComponent } from '../../shared/components/sesa-calendar/sesa-calendar.component';
import { SesaSkeletonComponent } from '../../shared/components/sesa-skeleton/sesa-skeleton.component';
import { SesaEmptyStateComponent } from '../../shared/components/sesa-empty-state/sesa-empty-state.component';
import { DashboardChartBarComponent } from '../../shared/components/dashboard-chart-bar/dashboard-chart-bar.component';
import { DashboardChartDoughnutComponent } from '../../shared/components/dashboard-chart-doughnut/dashboard-chart-doughnut.component';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';
import { AuthService } from '../../core/services/auth.service';
import { DashboardService, DashboardData } from '../../core/services/dashboard.service';
import { CitaDto, CitaService } from '../../core/services/cita.service';
import { PermissionsService } from '../../core/services/permissions.service';
import { ReporteService, DashboardStatsDto } from '../../core/services/reporte.service';

/* ── Interfaces ─────────────────────────────────────── */
interface StatCard {
  titulo: string; valor: number | string;
  chip?: string; chipClase?: string;
  icon: any; link?: string; gradient: string;
}

interface ModuleActivityItem {
  label: string; displayValue: string;
  pct: number; gradient: string;
  link: string; icon: any;
}

const MESES_SHORT = ['Ene','Feb','Mar','Abr','May','Jun','Jul','Ago','Sep','Oct','Nov','Dic'];
const AVATAR_COLORS = ['#1f6ae1','#0ea5e9','#6366f1','#14b8a6','#f59e0b','#ef4444','#22c55e','#8b5cf6','#ec4899','#06b6d4'];

@Component({
  standalone: true,
  selector: 'sesa-dashboard-page',
  imports: [
    CommonModule, RouterLink, FontAwesomeModule,
    SesaCalendarComponent, SesaSkeletonComponent, SesaEmptyStateComponent,
    DashboardChartBarComponent, DashboardChartDoughnutComponent,
  ],
  templateUrl: './dashboard.page.html',
  styleUrl:    './dashboard.page.scss',
})
export class DashboardPageComponent implements OnInit, OnDestroy {
  private readonly auth       = inject(AuthService);
  private readonly dashSvc    = inject(DashboardService);
  private readonly citaSvc    = inject(CitaService);
  private readonly reporteSvc = inject(ReporteService);
  private readonly toast      = inject(SesaToastService);
  readonly permissions        = inject(PermissionsService);

  /* ── Iconos ── */
  faChartLine = faChartLine; faChartBar = faChartBar;
  faTrendUp   = faArrowTrendUp; faTrendDown = faArrowTrendDown;
  faInfo      = faCircleInfo;

  /* ── Estado KPI ── */
  data: DashboardData | null = null;
  loading     = true;
  loadError   = false;
  animatedValues = signal<Record<string, number>>({});
  readonly skeletonStatCount = [1,2,3,4,5,6];

  /* ── Citas ── */
  highlightedDates      = new Set<string>();
  selectedDate: string | null = null;
  citasParaFecha: CitaDto[] = [];
  loadingCitasParaFecha = false;

  /* ── Gráficas ── */
  chartStats   = signal<DashboardStatsDto | null>(null);
  chartLoading = signal(true);

  /* ── Reloj ── */
  clockTime = signal(''); clockDate = signal(''); clockDay = signal('');
  private _clockInterval: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void { this.load(); this._startClock(); }
  ngOnDestroy(): void { if (this._clockInterval) clearInterval(this._clockInterval); }

  private _startClock(): void {
    const tick = () => {
      const n = new Date();
      this.clockTime.set(n.toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false }));
      this.clockDate.set(n.toLocaleDateString('es-CO', { day: 'numeric', month: 'short', year: 'numeric' }));
      this.clockDay.set(n.toLocaleDateString('es-CO', { weekday: 'long' }));
    };
    tick();
    this._clockInterval = setInterval(tick, 1000);
  }

  load(): void {
    const role = this.auth.currentUser()?.role ?? '';
    this.loading = true; this.loadError = false; this.chartLoading.set(true);
    forkJoin({ dash: this.dashSvc.load(role), charts: this.reporteSvc.dashboardStats() }).subscribe({
      next: ({ dash, charts }) => {
        this.data = dash; this.loading = false;
        this.highlightedDates = this.dashSvc.getCitasDatesForMonth(dash.citasHoy);
        this.citasParaFecha = dash.citasHoy ?? [];
        this.selectedDate = null;
        this.chartStats.set(charts); this.chartLoading.set(false);
        this._animateCounters();
      },
      error: () => {
        this.loading = false; this.loadError = true; this.chartLoading.set(false);
        this.data = { role, resumen: {}, citasHoy: [], fechaHoy: '', loading: false, error: 'Error al cargar' };
        this.toast.error('No se pudo cargar el dashboard.', 'Error');
      },
    });
  }

  private _animateCounters(): void {
    const animated: Record<string, number> = {};
    this.statCards.forEach((c) => {
      const num = typeof c.valor === 'number' ? c.valor : NaN;
      if (!isNaN(num) && num > 0) {
        animated[c.titulo] = 0;
        const steps = 28, dur = 750;
        let cur = 0;
        const iv = setInterval(() => {
          cur += num / steps;
          animated[c.titulo] = cur >= num ? num : Math.floor(cur);
          if (cur >= num) clearInterval(iv);
          this.animatedValues.set({ ...animated });
        }, dur / steps);
      } else { animated[c.titulo] = 0; }
    });
    this.animatedValues.set(animated);
  }

  getDisplayValue(c: StatCard): string | number {
    const av = this.animatedValues()[c.titulo];
    return (av !== undefined && typeof c.valor === 'number') ? av : c.valor;
  }

  /* ── KPI cards ── */
  get statCards(): StatCard[] {
    const r = this.data?.resumen ?? {}, cy = (this.data?.citasHoy ?? []).length;
    const cards: StatCard[] = [];
    if (this.permissions.canAccess('CITAS')) cards.push({ titulo: 'Citas hoy', valor: cy, chip: cy > 0 ? 'Programadas' : 'Sin citas', chipClase: cy > 0 ? 'db-chip-green' : 'db-chip-muted', icon: faCalendarCheck, link: '/citas', gradient: 'linear-gradient(135deg,#1f6ae1,#38bdf8)' });
    if (this.permissions.canAccess('PACIENTES') && r.totalPacientes !== undefined) cards.push({ titulo: 'Pacientes', valor: r.totalPacientes, chip: 'Total', chipClase: 'db-chip-blue', icon: faUsers, link: '/pacientes', gradient: 'linear-gradient(135deg,#0ea5e9,#06b6d4)' });
    if (this.permissions.canAccess('HISTORIA_CLINICA') && r.totalConsultas !== undefined) cards.push({ titulo: 'Consultas', valor: r.totalConsultas, chip: 'Historial', chipClase: 'db-chip-purple', icon: faFileMedical, link: '/historia-clinica', gradient: 'linear-gradient(135deg,#6366f1,#8b5cf6)' });
    if (this.permissions.canAccess('LABORATORIOS') && r.totalOrdenes !== undefined) cards.push({ titulo: 'Laboratorios', valor: r.totalOrdenes, chip: 'Activas', chipClase: 'db-chip-amber', icon: faFlaskVial, link: '/laboratorios', gradient: 'linear-gradient(135deg,#f59e0b,#f97316)' });
    if (this.permissions.canAccess('URGENCIAS')) cards.push({ titulo: 'Urgencias', valor: r.totalCitas ?? 0, chip: 'Monitoreo', chipClase: 'db-chip-red', icon: faExclamationTriangle, link: '/urgencias', gradient: 'linear-gradient(135deg,#ef4444,#f97316)' });
    if (this.permissions.canAccess('HOSPITALIZACION')) cards.push({ titulo: 'Hospitalización', valor: '—', chip: 'Camas', chipClase: 'db-chip-purple', icon: faHospital, link: '/hospitalizacion', gradient: 'linear-gradient(135deg,#8b5cf6,#a78bfa)' });
    if (this.permissions.canAccess('FARMACIA')) cards.push({ titulo: 'Farmacia', valor: '—', chip: 'Dispensación', chipClase: 'db-chip-teal', icon: faPills, link: '/farmacia', gradient: 'linear-gradient(135deg,#14b8a6,#06b6d4)' });
    if (this.permissions.canAccess('FACTURACION') && r.totalFacturas !== undefined) cards.push({ titulo: 'Facturación', valor: r.totalFacturas, chip: r.totalFacturado != null ? `$${Number(r.totalFacturado).toLocaleString('es-CO')}` : 'Facturas', chipClase: 'db-chip-green', icon: faFileInvoiceDollar, link: '/facturacion', gradient: 'linear-gradient(135deg,#22c55e,#16a34a)' });
    if (this.permissions.canAccess('EBS')) cards.push({ titulo: 'EBS', valor: '—', chip: 'APS', chipClase: 'db-chip-teal', icon: faMapLocationDot, link: '/ebs', gradient: 'linear-gradient(135deg,#059669,#10b981)' });
    return cards;
  }

  /* ── Panel de actividad por módulo (barras de progreso) ── */
  get moduleActivityItems(): ModuleActivityItem[] {
    const r = this.data?.resumen ?? {}, cy = (this.data?.citasHoy ?? []).length;
    const items: ModuleActivityItem[] = [];
    const nums: number[] = [];
    if (this.permissions.canAccess('CITAS')) nums.push(Math.max(cy, 1));
    if (this.permissions.canAccess('PACIENTES') && r.totalPacientes) nums.push(r.totalPacientes);
    if (this.permissions.canAccess('HISTORIA_CLINICA') && r.totalConsultas) nums.push(r.totalConsultas);
    if (this.permissions.canAccess('LABORATORIOS') && r.totalOrdenes) nums.push(r.totalOrdenes);
    if (this.permissions.canAccess('URGENCIAS')) nums.push(r.totalCitas ?? 1);
    if (this.permissions.canAccess('FACTURACION') && r.totalFacturas) nums.push(r.totalFacturas);
    const maxVal = Math.max(...nums, 1);
    const pct = (v: number) => Math.max(Math.round(Math.min((v / maxVal) * 100, 100)), 4);

    if (this.permissions.canAccess('CITAS')) items.push({ label: 'Citas hoy', displayValue: String(cy), pct: pct(cy), gradient: 'linear-gradient(90deg,#1f6ae1,#38bdf8)', link: '/citas', icon: faCalendarCheck });
    if (this.permissions.canAccess('PACIENTES') && r.totalPacientes !== undefined) items.push({ label: 'Pacientes', displayValue: String(r.totalPacientes), pct: pct(r.totalPacientes), gradient: 'linear-gradient(90deg,#0ea5e9,#06b6d4)', link: '/pacientes', icon: faUsers });
    if (this.permissions.canAccess('HISTORIA_CLINICA') && r.totalConsultas !== undefined) items.push({ label: 'Consultas', displayValue: String(r.totalConsultas), pct: pct(r.totalConsultas), gradient: 'linear-gradient(90deg,#6366f1,#8b5cf6)', link: '/historia-clinica', icon: faFileMedical });
    if (this.permissions.canAccess('LABORATORIOS') && r.totalOrdenes !== undefined) items.push({ label: 'Laboratorios', displayValue: String(r.totalOrdenes), pct: pct(r.totalOrdenes), gradient: 'linear-gradient(90deg,#f59e0b,#f97316)', link: '/laboratorios', icon: faFlaskVial });
    if (this.permissions.canAccess('URGENCIAS')) items.push({ label: 'Urgencias', displayValue: String(r.totalCitas ?? 0), pct: pct(r.totalCitas ?? 0), gradient: 'linear-gradient(90deg,#ef4444,#f97316)', link: '/urgencias', icon: faExclamationTriangle });
    if (this.permissions.canAccess('HOSPITALIZACION')) items.push({ label: 'Hospitalización', displayValue: '—', pct: 4, gradient: 'linear-gradient(90deg,#8b5cf6,#a78bfa)', link: '/hospitalizacion', icon: faHospital });
    if (this.permissions.canAccess('FARMACIA')) items.push({ label: 'Farmacia', displayValue: '—', pct: 4, gradient: 'linear-gradient(90deg,#14b8a6,#06b6d4)', link: '/farmacia', icon: faPills });
    if (this.permissions.canAccess('FACTURACION') && r.totalFacturas !== undefined) items.push({ label: 'Facturación', displayValue: `${r.totalFacturas} fact.`, pct: pct(r.totalFacturas), gradient: 'linear-gradient(90deg,#22c55e,#16a34a)', link: '/facturacion', icon: faFileInvoiceDollar });
    if (this.permissions.canAccess('EBS')) items.push({ label: 'EBS', displayValue: '—', pct: 4, gradient: 'linear-gradient(90deg,#059669,#10b981)', link: '/ebs', icon: faMapLocationDot });
    return items;
  }

  /* ── Datos gráficas ── */
  get citasDiaLabels(): string[] { return (this.chartStats()?.citasPorDia ?? []).map(d => new Date(d.fecha + 'T12:00:00').toLocaleDateString('es-CO', { weekday: 'short', day: 'numeric' })); }
  get citasDiaValues(): number[] { return (this.chartStats()?.citasPorDia ?? []).map(d => d.total); }
  get consultasMesLabels(): string[] { return (this.chartStats()?.consultasPorMes ?? []).map(d => MESES_SHORT[d.mes - 1]); }
  get consultasMesValues(): number[] { return (this.chartStats()?.consultasPorMes ?? []).map(d => d.total); }
  get citasEstadoLabels(): string[] {
    const M: Record<string, string> = { PENDIENTE: 'Pendiente', CONFIRMADA: 'Confirmada', ATENDIDO: 'Atendida', CANCELADA: 'Cancelada', NO_ASISTIO: 'No asistió' };
    return (this.chartStats()?.citasPorEstado ?? []).map(d => M[d.estado] ?? d.estado);
  }
  get citasEstadoValues(): number[] { return (this.chartStats()?.citasPorEstado ?? []).map(d => d.total); }
  get facturacionLabels(): string[] { return (this.chartStats()?.facturacionPorMes ?? []).map(d => MESES_SHORT[d.mes - 1]); }
  get facturacionValues(): number[] { return (this.chartStats()?.facturacionPorMes ?? []).map(d => d.cantidad); }

  get totalCitasSemana(): number { return this.citasDiaValues.reduce((a, b) => a + b, 0); }
  get totalConsultasMes(): number { const a = this.consultasMesValues; return a.length ? a[a.length - 1] : 0; }
  get totalCitasEstado(): number { return this.citasEstadoValues.reduce((a, b) => a + b, 0); }

  /* ── Visibilidad de gráficas ── */
  get showChartCitas():     boolean { return this.permissions.canAccess('CITAS'); }
  get showChartConsultas(): boolean { return this.permissions.canAccess('HISTORIA_CLINICA'); }
  get showChartEstados():   boolean { return this.permissions.canAccess('CITAS'); }
  get showChartFact():      boolean { return this.isAdmin && this.permissions.canAccess('FACTURACION') && this.facturacionValues.length > 0; }

  /* ── Métodos de citas ── */
  get proximasCitas(): CitaDto[] { return this.citasParaFecha.slice(0, 8); }

  onDateSelected(fecha: string): void {
    this.selectedDate = fecha; this.loadingCitasParaFecha = true;
    this.citaSvc.list(fecha).subscribe({
      next:  (c) => { this.citasParaFecha = c ?? []; this.loadingCitasParaFecha = false; },
      error: ()  => { this.citasParaFecha = []; this.loadingCitasParaFecha = false; },
    });
  }

  formatHora(fh?: string): string {
    if (!fh) return '--:--';
    return new Date(fh).toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit', hour12: false });
  }

  /* ── Avatares de pacientes ── */
  getPatientInitials(name?: string): string {
    if (!name) return '?';
    const p = name.trim().split(/\s+/).filter(x => x.length > 0);
    return p.slice(0, 2).map(x => x[0]).join('').toUpperCase();
  }

  getInitialBgColor(name?: string): string {
    if (!name) return AVATAR_COLORS[0];
    let h = 0;
    for (let i = 0; i < name.length; i++) h = name.charCodeAt(i) + ((h << 5) - h);
    return AVATAR_COLORS[Math.abs(h) % AVATAR_COLORS.length];
  }

  estadoBadgeClass(estado?: string): string {
    const m: Record<string, string> = { PENDIENTE: 'db-badge-amber', CONFIRMADA: 'db-badge-blue', ATENDIDO: 'db-badge-green', CANCELADA: 'db-badge-red' };
    return m[estado ?? ''] ?? 'db-badge-muted';
  }

  /* ── Utilidades de sesión ── */
  get isAdmin(): boolean {
    const r = this.auth.currentUser()?.role ?? '';
    return r === 'ADMIN' || r === 'SUPERADMINISTRADOR';
  }

  get roleLabel(): string {
    const m: Record<string, string> = {
      SUPERADMINISTRADOR:'Super Admin', ADMIN:'Administrador', MEDICO:'Médico',
      ODONTOLOGO:'Odontólogo', BACTERIOLOGO:'Bacteriólogo', ENFERMERO:'Enfermero',
      JEFE_ENFERMERIA:'Jefe Enfermería', AUXILIAR_ENFERMERIA:'Aux. Enfermería',
      PSICOLOGO:'Psicólogo', REGENTE_FARMACIA:'Regente Farmacia', RECEPCIONISTA:'Recepcionista',
    };
    return m[this.auth.currentUser()?.role ?? ''] ?? (this.auth.currentUser()?.role ?? '');
  }

  get fechaHoyFormateada(): string { return new Date().toLocaleDateString('es-CO', { weekday: 'long', day: 'numeric', month: 'long' }); }

  get fechaSeleccionadaFormateada(): string {
    if (!this.selectedDate) return this.fechaHoyFormateada;
    return new Date(this.selectedDate + 'T12:00:00').toLocaleDateString('es-CO', { weekday: 'long', day: 'numeric', month: 'long' });
  }
}
