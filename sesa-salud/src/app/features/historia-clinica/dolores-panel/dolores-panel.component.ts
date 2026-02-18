import { Component, Input, OnInit, OnChanges, SimpleChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DolorService, DolorDto, DolorRequestDto } from '../../../core/services/dolor.service';

interface BodyZone {
    id: string;
    label: string;
    /** Dot on body – percentage coordinates over the PNG */
    dotX: number;
    dotY: number;
    /** Label anchor – percentage position */
    labelX: number;
    labelY: number;
    side: 'left' | 'right';
    view: 'front' | 'back';
}

@Component({
    standalone: true,
    selector: 'sesa-dolores-panel',
    imports: [CommonModule, FormsModule],
    templateUrl: './dolores-panel.component.html',
    styleUrl: './dolores-panel.component.scss',
})
export class DoloresPanelComponent implements OnInit, OnChanges {
    @Input() pacienteId!: number;
    @Input() historiaClinicaId?: number;

    private readonly dolorService = inject(DolorService);

    dolores: DolorDto[] = [];
    filteredDolores: DolorDto[] = [];
    cargando = false;
    error: string | null = null;
    exito: string | null = null;

    bodyView: 'front' | 'back' = 'front';
    filterStatus = 'all';
    hoveredZone: string | null = null;
    /** Currently selected zone (stays highlighted while filling form) */
    selectedZoneId: string | null = null;

    showForm = false;
    showDetail = false;
    editingDolor: DolorDto | null = null;
    detailDolor: DolorDto | null = null;
    formData: DolorRequestDto = this.emptyForm();

    tiposDolor = ['Agudo', 'Crónico', 'Punzante', 'Sordo', 'Urente', 'Opresivo', 'Cólico', 'Pulsátil', 'Lancinante', 'Referido'];
    severidades = ['leve', 'moderada', 'grave'];

    get totalDolores() { return this.dolores.length; }
    get activeDolores() { return this.dolores.filter(d => d.estado === 'activo').length; }
    get tratamientoDolores() { return this.dolores.filter(d => d.estado === 'tratamiento').length; }
    get resueltoDolores() { return this.dolores.filter(d => d.estado === 'resuelto').length; }


    readonly zones: BodyZone[] = [

        // ── Columna izquierda (Der. del paciente) ──
        { id: 'cabeza', label: 'Cabeza', dotX: 150, dotY: 50, labelX: 56, labelY: 4, side: 'right', view: 'front' },
        { id: 'cuello_f', label: 'Cuello', dotX: 150, dotY: 120, labelX: 46, labelY: 18, side: 'right', view: 'front' },
        { id: 'hombro_d_f', label: 'Hombro Der.', dotX: 102, dotY: 140, labelX: 156, labelY: 16, side: 'left', view: 'front' },
        { id: 'torax_d', label: 'Tórax Der.', dotX: 128, dotY: 165, labelX: 6, labelY: 23, side: 'left', view: 'front' },
        { id: 'brazo_sup_d', label: 'Brazo Der.', dotX: 82, dotY: 185, labelX: 6, labelY: 28, side: 'left', view: 'front' },
        { id: 'codo_d', label: 'Codo Der.', dotX: 68, dotY: 232, labelX: 6, labelY: 34, side: 'left', view: 'front' },
        { id: 'antebrazo_d', label: 'Antebrazo Der.', dotX: 58, dotY: 270, labelX: 6, labelY: 40, side: 'left', view: 'front' },
        { id: 'muneca_d', label: 'Muñeca/Mano Der.', dotX: 46, dotY: 320, labelX: 6, labelY: 46, side: 'left', view: 'front' },
        { id: 'ingle_d', label: 'Ingle Der.', dotX: 133, dotY: 325, labelX: 6, labelY: 52, side: 'left', view: 'front' },
        { id: 'muslo_d', label: 'Muslo Der.', dotX: 128, dotY: 395, labelX: 6, labelY: 59, side: 'left', view: 'front' },
        { id: 'rodilla_d', label: 'Rodilla Der.', dotX: 128, dotY: 450, labelX: 6, labelY: 68, side: 'left', view: 'front' },
        { id: 'pierna_d', label: 'Pierna Der.', dotX: 125, dotY: 510, labelX: 6, labelY: 78, side: 'left', view: 'front' },
        { id: 'tobillo_d', label: 'Tobillo/Pie Der.', dotX: 125, dotY: 600, labelX: 6, labelY: 92, side: 'left', view: 'front' },
        // ── Columna derecha (Izq. del paciente) ──
        { id: 'hombro_i_f', label: 'Hombro Izq.', dotX: 198, dotY: 140, labelX: 94, labelY: 16, side: 'right', view: 'front' },
        { id: 'torax_i', label: 'Tórax Izq.', dotX: 172, dotY: 165, labelX: 94, labelY: 23, side: 'right', view: 'front' },
        { id: 'brazo_sup_i', label: 'Brazo Izq.', dotX: 218, dotY: 185, labelX: 94, labelY: 28, side: 'right', view: 'front' },
        { id: 'abdomen', label: 'Abdomen', dotX: 150, dotY: 240, labelX: 94, labelY: 35, side: 'right', view: 'front' },
        { id: 'codo_i', label: 'Codo Izq.', dotX: 232, dotY: 232, labelX: 94, labelY: 41, side: 'right', view: 'front' },
        { id: 'antebrazo_i', label: 'Antebrazo Izq.', dotX: 242, dotY: 270, labelX: 94, labelY: 47, side: 'right', view: 'front' },
        { id: 'pelvis', label: 'Pelvis', dotX: 150, dotY: 290, labelX: 94, labelY: 53, side: 'right', view: 'front' },
        { id: 'muneca_i', label: 'Muñeca/Mano Izq.', dotX: 254, dotY: 320, labelX: 94, labelY: 59, side: 'right', view: 'front' },
        { id: 'ingle_i', label: 'Ingle Izq.', dotX: 167, dotY: 325, labelX: 94, labelY: 65, side: 'right', view: 'front' },
        { id: 'muslo_i', label: 'Muslo Izq.', dotX: 172, dotY: 395, labelX: 94, labelY: 71, side: 'right', view: 'front' },
        { id: 'rodilla_i', label: 'Rodilla Izq.', dotX: 172, dotY: 450, labelX: 94, labelY: 77, side: 'right', view: 'front' },
        { id: 'pierna_i', label: 'Pierna Izq.', dotX: 175, dotY: 510, labelX: 94, labelY: 83, side: 'right', view: 'front' },
        { id: 'tobillo_i', label: 'Tobillo/Pie Izq.', dotX: 175, dotY: 600, labelX: 94, labelY: 92, side: 'right', view: 'front' },

        /* ═══════════════════════════════════════════════════════════════
           POSTERIOR — Derecha del visor = Derecho del paciente (Der.)
           Izquierda del visor = Izquierdo del paciente (Izq.)
           ═══════════════════════════════════════════════════════════════ */
        // ── Columna derecha (Der. del paciente = visor derecha) ──
        { id: 'cabeza_post', label: 'Cabeza', dotX: 150, dotY: 40, labelX: 94, labelY: 4, side: 'right', view: 'back' },
        { id: 'cuello_post', label: 'Cuello', dotX: 150, dotY: 88, labelX: 94, labelY: 11, side: 'right', view: 'back' },
        { id: 'hombro_d_b', label: 'Hombro Der.', dotX: 198, dotY: 118, labelX: 94, labelY: 16, side: 'right', view: 'back' },
        { id: 'brazo_sup_d_b', label: 'Brazo Der.', dotX: 218, dotY: 185, labelX: 94, labelY: 26, side: 'right', view: 'back' },
        { id: 'codo_d_b', label: 'Codo Der.', dotX: 232, dotY: 232, labelX: 94, labelY: 34, side: 'right', view: 'back' },
        { id: 'cadera_d', label: 'Cadera Der.', dotX: 167, dotY: 290, labelX: 94, labelY: 42, side: 'right', view: 'back' },
        { id: 'gluteo_d', label: 'Glúteo Der.', dotX: 170, dotY: 335, labelX: 94, labelY: 50, side: 'right', view: 'back' },
        { id: 'muslo_d_b', label: 'Muslo Der.', dotX: 172, dotY: 395, labelX: 94, labelY: 58, side: 'right', view: 'back' },
        { id: 'pantorrilla_d', label: 'Pantorrilla Der.', dotX: 175, dotY: 510, labelX: 94, labelY: 74, side: 'right', view: 'back' },
        { id: 'talon_d', label: 'Talón Der.', dotX: 175, dotY: 600, labelX: 94, labelY: 92, side: 'right', view: 'back' },
        // ── Columna izquierda (Izq. del paciente = visor izquierda) ──
        { id: 'hombro_i_b', label: 'Hombro Izq.', dotX: 102, dotY: 118, labelX: 6, labelY: 16, side: 'left', view: 'back' },
        { id: 'espalda_alta', label: 'Espalda Alta', dotX: 150, dotY: 160, labelX: 6, labelY: 23, side: 'left', view: 'back' },
        { id: 'brazo_sup_i_b', label: 'Brazo Izq.', dotX: 82, dotY: 185, labelX: 6, labelY: 28, side: 'left', view: 'back' },
        { id: 'codo_i_b', label: 'Codo Izq.', dotX: 68, dotY: 232, labelX: 6, labelY: 34, side: 'left', view: 'back' },
        { id: 'espalda_baja', label: 'Espalda Baja', dotX: 150, dotY: 250, labelX: 6, labelY: 40, side: 'left', view: 'back' },
        { id: 'cadera_i', label: 'Cadera Izq.', dotX: 133, dotY: 290, labelX: 6, labelY: 46, side: 'left', view: 'back' },
        { id: 'gluteo_i', label: 'Glúteo Izq.', dotX: 130, dotY: 335, labelX: 6, labelY: 52, side: 'left', view: 'back' },
        { id: 'muslo_i_b', label: 'Muslo Izq.', dotX: 128, dotY: 395, labelX: 6, labelY: 59, side: 'left', view: 'back' },
        { id: 'pantorrilla_i', label: 'Pantorrilla Izq.', dotX: 125, dotY: 510, labelX: 6, labelY: 78, side: 'left', view: 'back' },
        { id: 'talon_i', label: 'Talón Izq.', dotX: 125, dotY: 600, labelX: 6, labelY: 92, side: 'left', view: 'back' },
    ];

    ngOnInit() { this.loadDolores(); }

    ngOnChanges(changes: SimpleChanges) {
        if (changes['pacienteId'] && !changes['pacienteId'].firstChange) this.loadDolores();
    }

    loadDolores() {
        if (!this.pacienteId) return;
        this.cargando = true;
        this.dolorService.listByPaciente(this.pacienteId).subscribe({
            next: list => { this.dolores = list ?? []; this.applyFilter(); this.cargando = false; },
            error: () => { this.dolores = []; this.filteredDolores = []; this.cargando = false; },
        });
    }

    getZonesForView(): BodyZone[] { return this.zones.filter(z => z.view === this.bodyView); }
    getLeftZones(): BodyZone[] { return this.getZonesForView().filter(z => z.side === 'left'); }
    getRightZones(): BodyZone[] { return this.getZonesForView().filter(z => z.side === 'right'); }

    /** Columnas de etiquetas — side y labelX ya están correctos por vista */
    getZonesForLeftColumn(): BodyZone[] { return this.getLeftZones(); }
    getZonesForRightColumn(): BodyZone[] { return this.getRightZones(); }

    /* ── Zone interactions ── */
    selectZone(zone: BodyZone) {
        this.selectedZoneId = zone.id;
        if (this.hasDolorInZone(zone.id)) {
            const d = this.dolores.find(x => x.zonaCorporal === zone.id && x.estado !== 'resuelto');
            if (d) this.viewDetail(d);
        } else {
            this.openNewDolorForm(zone);
        }
    }

    isZoneSelected(id: string): boolean { return this.selectedZoneId === id; }
    isZoneHighlighted(id: string): boolean { return this.hoveredZone === id || this.selectedZoneId === id; }

    /* ── Pain helpers ── */
    hasDolorInZone(id: string) { return this.dolores.some(d => d.zonaCorporal === id && d.estado !== 'resuelto'); }
    getZoneDolorCount(id: string) { return this.dolores.filter(d => d.zonaCorporal === id && d.estado !== 'resuelto').length; }

    getSeverityForZone(id: string): string {
        const zd = this.dolores.filter(d => d.zonaCorporal === id && d.estado !== 'resuelto');
        if (!zd.length) return '';
        const o: Record<string, number> = { leve: 1, moderada: 2, grave: 3 };
        return zd.reduce((b, d) => (o[d.severidad] || 0) > (o[b.severidad] || 0) ? d : b, zd[0]).severidad;
    }

    getDotColor(id: string): string {
        const s = this.getSeverityForZone(id);
        return s === 'grave' ? '#ef4444' : s === 'moderada' ? '#f59e0b' : s === 'leve' ? '#22c55e' : '';
    }

    /* ── Filters ── */
    setFilter(s: string) { this.filterStatus = s; this.applyFilter(); }
    private applyFilter() {
        this.filteredDolores = this.filterStatus === 'all' ? [...this.dolores] : this.dolores.filter(d => d.estado === this.filterStatus);
    }

    /* ── Form ── */
    openNewDolorForm(zone?: BodyZone) {
        this.showForm = true; this.showDetail = false; this.editingDolor = null;
        this.formData = this.emptyForm();
        if (zone) {
            this.formData.zonaCorporal = zone.id;
            this.formData.zonaLabel = zone.label;
            this.formData.vista = zone.view;
            this.selectedZoneId = zone.id;
        }
    }

    openEditDolorForm(d: DolorDto) {
        this.showForm = true; this.showDetail = false; this.editingDolor = d;
        this.selectedZoneId = d.zonaCorporal;
        this.formData = {
            pacienteId: d.pacienteId, historiaClinicaId: d.historiaClinicaId,
            zonaCorporal: d.zonaCorporal, zonaLabel: d.zonaLabel, tipoDolor: d.tipoDolor,
            intensidad: d.intensidad, severidad: d.severidad, estado: d.estado,
            fechaInicio: d.fechaInicio?.substring(0, 10) ?? '', fechaResolucion: d.fechaResolucion?.substring(0, 10),
            descripcion: d.descripcion, factoresAgravantes: d.factoresAgravantes,
            factoresAliviantes: d.factoresAliviantes, tratamiento: d.tratamiento,
            notas: d.notas, vista: d.vista,
        };
    }

    cancelForm() {
        this.showForm = false; this.editingDolor = null;
        this.selectedZoneId = null;
        this.formData = this.emptyForm();
    }

    saveDolor() {
        this.formData.pacienteId = this.pacienteId;
        if (this.historiaClinicaId) this.formData.historiaClinicaId = this.historiaClinicaId;
        this.error = null; this.exito = null;
        const obs = this.editingDolor ? this.dolorService.update(this.editingDolor.id, this.formData) : this.dolorService.create(this.formData);
        obs.subscribe({
            next: () => {
                this.exito = this.editingDolor ? 'Dolor actualizado' : 'Dolor registrado';
                this.showForm = false; this.editingDolor = null; this.selectedZoneId = null;
                this.formData = this.emptyForm();
                this.loadDolores(); setTimeout(() => this.exito = null, 4000);
            },
            error: err => { this.error = err?.error?.error || 'No se pudo guardar'; },
        });
    }

    /* ── Detail ── */
    viewDetail(d: DolorDto) {
        this.showDetail = true; this.showForm = false;
        this.detailDolor = d;
        this.selectedZoneId = d.zonaCorporal;
    }
    closeDetail() { this.showDetail = false; this.detailDolor = null; this.selectedZoneId = null; }

    deleteDolor(d: DolorDto) {
        if (!confirm('¿Eliminar este registro de dolor?')) return;
        this.dolorService.delete(d.id).subscribe({
            next: () => {
                this.exito = 'Dolor eliminado'; this.showDetail = false;
                this.detailDolor = null; this.selectedZoneId = null;
                this.loadDolores(); setTimeout(() => this.exito = null, 4000);
            },
            error: err => { this.error = err?.error?.error || 'No se pudo eliminar'; },
        });
    }

    /* ── Misc ── */
    getSeverityColor(s: string) { return s === 'leve' ? '#22c55e' : s === 'moderada' ? '#f59e0b' : s === 'grave' ? '#ef4444' : '#94a3b8'; }
    getStatusColor(s: string) { return s === 'activo' ? '#ef4444' : s === 'tratamiento' ? '#f59e0b' : s === 'resuelto' ? '#22c55e' : '#94a3b8'; }
    getStatusIcon(s: string) { return s === 'activo' ? 'bi-exclamation-circle-fill' : s === 'tratamiento' ? 'bi-arrow-repeat' : s === 'resuelto' ? 'bi-check-circle-fill' : 'bi-question-circle'; }
    getStatusLabel(s: string) { return s === 'activo' ? 'Activo' : s === 'tratamiento' ? 'En tratamiento' : s === 'resuelto' ? 'Resuelto' : s; }
    getIntensityLabel(v: number) { return v <= 3 ? 'Leve' : v <= 6 ? 'Moderado' : 'Severo'; }
    getIntensityColor(v: number) { return v <= 3 ? '#22c55e' : v <= 6 ? '#f59e0b' : '#ef4444'; }
    getDaysSince(d: string) { return Math.floor((Date.now() - new Date(d).getTime()) / 864e5); }
    formatDate(d?: string) { if (!d) return '—'; const x = new Date(d); return isNaN(x.getTime()) ? '—' : x.toLocaleDateString(); }

    onZoneSelected(e: Event) {
        const z = this.zones.find(x => x.id === (e.target as HTMLSelectElement).value);
        if (z) {
            this.formData.zonaLabel = z.label;
            this.formData.vista = z.view;
            this.bodyView = z.view;
            this.selectedZoneId = z.id;
        }
    }

    onIntensityChange() {
        const v = this.formData.intensidad;
        this.formData.severidad = v <= 3 ? 'leve' : v <= 6 ? 'moderada' : 'grave';
    }

    private emptyForm(): DolorRequestDto {
        return {
            pacienteId: this.pacienteId, historiaClinicaId: this.historiaClinicaId,
            zonaCorporal: '', zonaLabel: '', tipoDolor: '', intensidad: 5,
            severidad: 'moderada', estado: 'activo', fechaInicio: new Date().toISOString().substring(0, 10),
            descripcion: '', factoresAgravantes: '', factoresAliviantes: '', tratamiento: '', notas: '', vista: 'front',
        };
    }
}
