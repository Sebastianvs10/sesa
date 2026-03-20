/**
 * OdontogramaComponent — Odontograma SVG interactivo. Diseño premium clínico.
 * Múltiples vistas (frontal, oclusal, lateral, arcadas), toolbar de tratamientos,
 * menú contextual, modal de historial. FDI adulto y pediátrico.
 * Autor: Ing. J Sebastian Vargas S
 */

import {
  Component, input, output, signal, computed, effect, ChangeDetectionStrategy,
  HostListener, inject,
} from '@angular/core';
import { CommonModule, DOCUMENT } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  PiezaDental, Superficie, EstadoDental, ESTADO_COLOR, ESTADO_LABEL,
  PIEZAS_PERMANENTES_SUPERIOR, PIEZAS_PERMANENTES_INFERIOR,
  PIEZAS_DECIDUAS_SUPERIOR, PIEZAS_DECIDUAS_INFERIOR,
  crearPiezaVacia, esPiezaAnterior,
} from '../odontologia.models';
import { OdontogramaService } from './services/odontograma.service';
import { VistaSelectorComponent } from './components/vista-selector.component';
import { ToolbarAccionesComponent } from './components/toolbar-acciones.component';
import type { TipoVista } from './services/odontograma.service';
import type { Tratamiento } from './models/tratamiento.model';
import { AuthService } from '../../../core/services/auth.service';

export interface OdontogramaCambio {
  fdi: number;
  superficie: Superficie;
  estadoAnterior: EstadoDental;
  estadoNuevo: EstadoDental;
}

/** Formato exportable/importable (compatible con React mockup). */
export interface OdontogramaExportData {
  modo: 'adulto' | 'pediatrico';
  fecha: string;
  piezas: Array<{
    fdi: number;
    esPediatrica: boolean;
    ausente: boolean;
    observacion?: string;
    superficies: Record<string, string>;
  }>;
}

export interface OdontogramaSnapshot {
  id: string;
  fecha: string;
  modo: string;
  data: OdontogramaExportData['piezas'];
}

export interface PiezaUI {
  fdi: number;
  x: number;
  y: number;
  label: string;
  esSuperior: boolean;
  esAnterior: boolean;
  hw: number;
  hh: number;
  rx: number;   // radio de borde según tipo de diente
}

type VisualMode = 'hiperrealista' | 'operativo';

const ESTADOS_LISTA: EstadoDental[] = [
  'SANO','CARIES','OBTURACION','ENDODONCIA','CORONA',
  'AUSENTE','PROTESIS','FRACTURA','SELLANTE','EXTRACCION_INDICADA','IMPLANTE',
];
const SUPERFICIES_VALIDAS: Superficie[] = ['MESIAL', 'DISTAL', 'VESTIBULAR', 'LINGUAL', 'OCLUSAL', 'GENERAL'];

/** Tamaños anatómicos en vista oclusal (mesio-distal × vestibulo-lingual) */
const TOOTH_SIZES: Record<number, { hw: number; hh: number; rx: number }> = {
  1: { hw: 12, hh: 22, rx: 9 },  // incisivo central
  2: { hw: 11, hh: 21, rx: 9 },  // incisivo lateral
  3: { hw: 13, hh: 21, rx: 8 },  // canino
  4: { hw: 17, hh: 18, rx: 6 },  // 1er premolar
  5: { hw: 16, hh: 18, rx: 6 },  // 2do premolar
  6: { hw: 22, hh: 20, rx: 5 },  // 1er molar
  7: { hw: 21, hh: 19, rx: 4 },  // 2do molar
  8: { hw: 19, hh: 18, rx: 4 },  // cordal
};

@Component({
  selector: 'app-odontograma',
  standalone: true,
  imports: [CommonModule, FormsModule, VistaSelectorComponent, ToolbarAccionesComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './odontograma.component.html',
  styleUrl: './odontograma.component.scss',
})
export class OdontogramaComponent {
  private readonly doc = inject(DOCUMENT) as Document;
  readonly odontogramaSvc = inject(OdontogramaService);
  private readonly auth = inject(AuthService);
  private readonly visualModeStorageKey = 'sesa_odontograma_visual_mode';

  readonly piezasInput  = input<Map<number, PiezaDental>>(new Map(), { alias: 'piezas' });
  readonly readonly     = input(false);
  readonly cambio       = output<OdontogramaCambio>();

  readonly modo           = signal<'adulto' | 'pediatrico'>('adulto');
  readonly piezasLocal    = signal<Map<number, PiezaDental>>(new Map());
  readonly visualMode     = signal<VisualMode>('hiperrealista');

  constructor() {
    this.restoreVisualMode();
    effect(() => {
      const input = this.piezasInput();
      const copia = new Map(input);
      this.asegurarTodasPiezas(copia);
      this.piezasLocal.set(copia);
    }, { allowSignalWrites: true });
    effect(() => {
      const svcModo = this.odontogramaSvc.modo();
      if (this.modo() !== svcModo) this.modo.set(svcModo);
    });
  }
  readonly estadosLista   = ESTADOS_LISTA;
  readonly estadoColor    = ESTADO_COLOR;
  readonly estadoLabel    = ESTADO_LABEL;
  readonly mostrarLeyenda = signal(true);
  /** Superficies para la rueda (cuadrantes V, D, L, M). */
  readonly superficiesRueda: Superficie[] = ['VESTIBULAR', 'DISTAL', 'LINGUAL', 'MESIAL'];

  readonly popupVisible    = signal(false);
  readonly popupX          = signal(0);
  readonly popupY          = signal(0);
  readonly popupFdi        = signal<number | null>(null);
  readonly popupSuperficie = signal<Superficie | null>(null);

  /** Menú contextual (click derecho) */
  readonly contextVisible = signal(false);
  readonly contextX      = signal(0);
  readonly contextY      = signal(0);
  readonly contextFdi    = signal<number | null>(null);
  readonly contextSuperficie = signal<Superficie | null>(null);

  /** Modal historial clínico (doble click) */
  readonly historialVisible = signal(false);
  readonly historialFdi    = signal<number | null>(null);
  readonly historialSuperficie = signal<Superficie | null>(null);

  /** Pieza resaltada (hover o selección) para overlay premium tipo imagen 2 */
  readonly piezaResaltadaFdi = signal<number | null>(null);

  /** Vista superficies: true = grid 3×3 (cruz tipo React), false = círculo FDI */
  readonly useGridLayout = signal(false);

  /** Snapshots para evolución del caso (tomar / restaurar) */
  readonly snapshots = signal<OdontogramaSnapshot[]>([]);

  /** Mapeo grid React → FDI: top→OCLUSAL, left→MESIAL, center→LINGUAL, right→DISTAL, bottom→VESTIBULAR */
  readonly gridSurfaces: { key: 'top' | 'left' | 'center' | 'right' | 'bottom'; sup: Superficie }[] = [
    { key: 'top', sup: 'OCLUSAL' },
    { key: 'left', sup: 'MESIAL' },
    { key: 'center', sup: 'LINGUAL' },
    { key: 'right', sup: 'DISTAL' },
    { key: 'bottom', sup: 'VESTIBULAR' },
  ];

  // ── Piezas ────────────────────────────────────────────────────────────

  readonly piezasUI = computed<PiezaUI[]>(() => {
    if (this.modo() === 'adulto') {
      const sup = PIEZAS_PERMANENTES_SUPERIOR.map((fdi, i) => this.calcPos(fdi, i, true));
      const inf = PIEZAS_PERMANENTES_INFERIOR.map((fdi, i) => this.calcPos(fdi, i, false));
      return [...sup, ...inf];
    }
    const sup = PIEZAS_DECIDUAS_SUPERIOR.map((fdi, i) => this.calcPosPed(fdi, i, true));
    const inf = PIEZAS_DECIDUAS_INFERIOR.map((fdi, i) => this.calcPosPed(fdi, i, false));
    return [...sup, ...inf];
  });

  /** Filtra piezas según la vista seleccionada (frontal, oclusal, lateral, arcadas). */
  readonly piezasVisibles = computed<PiezaUI[]>(() => {
    const todas = this.piezasUI();
    const vista = this.odontogramaSvc.vista();
    if (vista === 'frontal' || vista === 'oclusal') return todas;
    if (vista === 'arcada_superior') return todas.filter(p => p.esSuperior);
    if (vista === 'arcada_inferior') return todas.filter(p => !p.esSuperior);
    if (vista === 'lateral_derecha') {
      return todas.filter(p => {
        const f = p.fdi;
        return (f >= 11 && f <= 18) || (f >= 41 && f <= 48) ||
          (f >= 51 && f <= 55) || (f >= 81 && f <= 85);
      });
    }
    if (vista === 'lateral_izquierda') {
      return todas.filter(p => {
        const f = p.fdi;
        return (f >= 21 && f <= 28) || (f >= 31 && f <= 38) ||
          (f >= 61 && f <= 65) || (f >= 71 && f <= 75);
      });
    }
    return todas;
  });

  readonly resumenEstados = computed(() => {
    const res: Partial<Record<EstadoDental, number>> = {};
    for (const pieza of this.piezasLocal().values())
      for (const [, est] of Object.entries(pieza.superficies) as [Superficie, EstadoDental][])
        if (est !== 'SANO') res[est] = (res[est] ?? 0) + 1;
    return res;
  });

  readonly cpodCalculado = computed(() => {
    let c = 0, p = 0, o = 0;
    for (const pieza of this.piezasLocal().values()) {
      const sups = Object.values(pieza.superficies) as EstadoDental[];
      if (pieza.ausente) { p++; continue; }
      if (sups.some(s => s === 'CARIES'))     c++;
      if (sups.some(s => s === 'OBTURACION')) o++;
    }
    return { c, p, o, total: c + p + o };
  });

  /** Resumen para presupuesto: procedimiento, dientes, superficies, cantidad (tipo React). */
  readonly resumenPresupuesto = computed(() => {
    const map = new Map<EstadoDental, { dientes: Set<number>; superficies: number }>();
    for (const [fdi, pieza] of this.piezasLocal().entries()) {
      if (pieza.ausente) {
        const r = map.get('AUSENTE') ?? { dientes: new Set(), superficies: 0 };
        r.dientes.add(fdi);
        map.set('AUSENTE', r);
      }
      for (const [sup, est] of Object.entries(pieza.superficies) as [Superficie, EstadoDental][]) {
        if (est === 'SANO') continue;
        const r = map.get(est) ?? { dientes: new Set(), superficies: 0 };
        r.dientes.add(fdi);
        r.superficies += 1;
        map.set(est, r);
      }
    }
    return Array.from(map.entries()).map(([estado, r]) => ({
      estado,
      label: ESTADO_LABEL[estado],
      dientes: Array.from(r.dientes).sort((a, b) => a - b),
      superficies: r.superficies,
      cantidad: r.superficies > 0 ? r.superficies : r.dientes.size,
    }));
  });

  // ── Posiciones ────────────────────────────────────────────────────────

  private getToothSize(fdi: number) {
    return TOOTH_SIZES[fdi % 10] ?? { hw: 16, hh: 18, rx: 5 };
  }

  private calcPos(fdi: number, idx: number, esSuperior: boolean): PiezaUI {
    const { hw, hh, rx } = this.getToothSize(fdi);
    return {
      fdi, x: 58 + idx * 62, y: esSuperior ? 165 : 318,
      label: String(fdi), esSuperior, esAnterior: esPiezaAnterior(fdi), hw, hh, rx,
    };
  }

  private calcPosPed(fdi: number, idx: number, esSuperior: boolean): PiezaUI {
    const { hw, hh, rx } = this.getToothSize(fdi);
    return {
      fdi, x: 170 + idx * 76, y: esSuperior ? 165 : 318,
      label: String(fdi), esSuperior, esAnterior: esPiezaAnterior(fdi), hw, hh, rx,
    };
  }

  // ── Polígonos de superficie ───────────────────────────────────────────

  private inner(hw: number, hh: number) {
    return { oi: Math.round(hw * 0.44), oj: Math.round(hh * 0.44) };
  }

  getVestibularPoints(p: PiezaUI): string {
    const { hw, hh, esSuperior } = p;
    const { oi, oj } = this.inner(hw, hh);
    return esSuperior
      ? `${-hw},${-hh} ${hw},${-hh} ${oi},${-oj} ${-oi},${-oj}`
      : `${-hw},${hh} ${hw},${hh} ${oi},${oj} ${-oi},${oj}`;
  }

  getLingualPoints(p: PiezaUI): string {
    const { hw, hh, esSuperior } = p;
    const { oi, oj } = this.inner(hw, hh);
    return esSuperior
      ? `${-hw},${hh} ${hw},${hh} ${oi},${oj} ${-oi},${oj}`
      : `${-hw},${-hh} ${hw},${-hh} ${oi},${-oj} ${-oi},${-oj}`;
  }

  getMesialPoints(p: PiezaUI): string {
    const { hw, hh } = p;
    const { oi, oj } = this.inner(hw, hh);
    return `${-hw},${-hh} ${-hw},${hh} ${-oi},${oj} ${-oi},${-oj}`;
  }

  getDistalPoints(p: PiezaUI): string {
    const { hw, hh } = p;
    const { oi, oj } = this.inner(hw, hh);
    return `${hw},${-hh} ${hw},${hh} ${oi},${oj} ${oi},${-oj}`;
  }

  getOclusalPoints(p: PiezaUI): string {
    const { oi, oj } = this.inner(p.hw, p.hh);
    return `${-oi},${-oj} ${oi},${-oj} ${oi},${oj} ${-oi},${oj}`;
  }

  /** Dimensiones plantilla SVG referencia: molar 55×70, anterior 40×70 (Sistema FDI). */
  getToothRectWidth(p: PiezaUI): number {
    return p.esAnterior ? 40 : 55;
  }
  getToothRectHeight(p: PiezaUI): number {
    return 70;
  }
  /** Radio del círculo de superficies dentro del rect (molar 18, anterior 12). */
  getSurfaceCircleR(p: PiezaUI): number {
    return p.esAnterior ? 12 : 18;
  }

  /** Path SVG para un segmento del círculo (ángulos en grados). */
  getSurfaceSegmentPath(r: number, startDeg: number, endDeg: number): string {
    const rad = (d: number) => (d * Math.PI) / 180;
    const x1 = r * Math.cos(rad(startDeg));
    const y1 = r * Math.sin(rad(startDeg));
    const x2 = r * Math.cos(rad(endDeg));
    const y2 = r * Math.sin(rad(endDeg));
    const large = endDeg - startDeg > 180 ? 1 : 0;
    return `M 0 0 L ${x1.toFixed(2)} ${y1.toFixed(2)} A ${r} ${r} 0 ${large} 1 ${x2.toFixed(2)} ${y2.toFixed(2)} Z`;
  }

  getSurfaceCenterPath(r: number): string {
    return `M ${r} 0 A ${r} ${r} 0 1 1 ${r - 0.01} 0`;
  }

  /** Segmento de superficie para plantilla clínica (V=arriba, D=derecha, L=abajo, M=izquierda, O=centro). */
  getSurfaceSegmentFor(p: PiezaUI, sup: Superficie): { path: string; isCenter: boolean } {
    const r = this.getSurfaceCircleR(p);
    switch (sup) {
      case 'VESTIBULAR': return { path: this.getSurfaceSegmentPath(r, -90, 0), isCenter: false };
      case 'DISTAL':     return { path: this.getSurfaceSegmentPath(r, 0, 90), isCenter: false };
      case 'LINGUAL':    return { path: this.getSurfaceSegmentPath(r, 90, 180), isCenter: false };
      case 'MESIAL':     return { path: this.getSurfaceSegmentPath(r, 180, 270), isCenter: false };
      case 'OCLUSAL':    return { path: this.getSurfaceCenterPath(r * 0.38), isCenter: true };
      default:           return { path: this.getSurfaceCenterPath(r * 0.38), isCenter: true };
    }
  }

  /** Coordenadas del rect para cada superficie en layout grid 3×3 (cruz). */
  getGridSurfaceRect(p: PiezaUI, sup: Superficie): { x: number; y: number; width: number; height: number } {
    const w = this.getToothRectWidth(p);
    const h = this.getToothRectHeight(p);
    const x0 = -w / 2, y0 = -h / 2;
    const tw = w / 3, th = h / 3;
    switch (sup) {
      case 'OCLUSAL':  return { x: x0, y: y0, width: w, height: th };
      case 'VESTIBULAR': return { x: x0, y: y0 + th * 2, width: w, height: th };
      case 'MESIAL':   return { x: x0, y: y0 + th, width: tw, height: th };
      case 'LINGUAL':  return { x: x0 + tw, y: y0 + th, width: tw, height: th };
      case 'DISTAL':   return { x: x0 + tw * 2, y: y0 + th, width: tw, height: th };
      default:         return { x: x0 + tw, y: y0 + th, width: tw, height: th };
    }
  }

  tieneCorona(fdi: number): boolean {
    return this.getSup(fdi, 'GENERAL') === 'CORONA' || this.getSup(fdi, 'OCLUSAL') === 'CORONA';
  }

  /** Clase CSS del cuerpo del diente según estado (tooth, crown, implant, missing). */
  getToothBodyClass(p: PiezaUI): string {
    if (this.isAusente(p.fdi)) return 'od-tooth missing';
    if (this.tieneImplante(p.fdi)) return 'od-tooth implant';
    if (this.tieneCorona(p.fdi)) return 'od-tooth crown';
    return 'od-tooth';
  }

  /** Radio de la rueda de superficies (debajo de la silueta del diente) — legacy/compat */
  private readonly WHEEL_R = 10;

  getWheelSegmentPath(r: number, startDeg: number, endDeg: number): string {
    return this.getSurfaceSegmentPath(r, startDeg, endDeg);
  }

  getWheelCenterPath(r: number): string {
    return this.getSurfaceCenterPath(r);
  }

  getWheelSegmentFor(sup: Superficie): { path: string; isCenter: boolean } {
    const r = this.WHEEL_R;
    switch (sup) {
      case 'VESTIBULAR': return { path: this.getSurfaceSegmentPath(r, -90, 0), isCenter: false };
      case 'DISTAL':     return { path: this.getSurfaceSegmentPath(r, 0, 90), isCenter: false };
      case 'LINGUAL':    return { path: this.getSurfaceSegmentPath(r, 90, 180), isCenter: false };
      case 'MESIAL':     return { path: this.getSurfaceSegmentPath(r, 180, 270), isCenter: false };
      case 'OCLUSAL':    return { path: this.getSurfaceCenterPath(r * 0.38), isCenter: true };
      default:           return { path: this.getSurfaceCenterPath(r * 0.38), isCenter: true };
    }
  }

  /** Silueta anatómica del diente (corona + raíz). Superior: raíz hacia -y; inferior: hacia +y. */
  getToothSilhouettePath(p: PiezaUI): string {
    const { hw, hh, esSuperior } = p;
    const rootLen = Math.min(10, hh * 0.45);
    const nw = hw * 0.35;
    if (esSuperior) {
      return `M ${-hw} ${hh} L ${hw} ${hh} L ${hw} ${-hh} L ${nw} ${-hh} L ${nw} ${-hh - rootLen} L ${-nw} ${-hh - rootLen} L ${-nw} ${-hh} Z`;
    }
    return `M ${-hw} ${-hh} L ${hw} ${-hh} L ${hw} ${hh} L ${nw} ${hh} L ${nw} ${hh + rootLen} L ${-nw} ${hh + rootLen} L ${-nw} ${hh} Z`;
  }

  /**
   * Silueta anatómica en coordenadas de pantalla (mismo tamaño que el rect 40/55×70).
   * Corona con bordes ligeramente redondeados + raíz estrecha (incisivos) o más ancha (molares).
   */
  getToothAnatomicPathDisplay(p: PiezaUI): string {
    const w = this.getToothRectWidth(p);
    const h = this.getToothRectHeight(p);
    const hw = w / 2;
    const hh = h / 2;
    const rootLen = p.esAnterior ? 16 : 18;
    const neck = p.esAnterior ? hw * 0.34 : hw * 0.42;
    const rootSpread = p.esAnterior ? hw * 0.2 : hw * 0.36;
    const outer = hw * 0.95;
    const baseY = p.esSuperior ? hh * 0.82 : -hh * 0.82;
    const crownShoulderY = p.esSuperior ? hh * 0.42 : -hh * 0.42;
    const occlusalY = p.esSuperior ? hh * 0.96 : -hh * 0.96;
    const cervicalY = p.esSuperior ? -hh * 0.22 : hh * 0.22;
    const rootMidY = p.esSuperior ? -(hh + rootLen * 0.52) : hh + rootLen * 0.52;
    const rootTipY = p.esSuperior ? -(hh + rootLen) : hh + rootLen;

    return `
      M ${-outer} ${baseY}
      Q ${-hw} ${crownShoulderY} ${-outer * 0.75} ${occlusalY * 0.94}
      Q ${-outer * 0.38} ${occlusalY} 0 ${occlusalY}
      Q ${outer * 0.38} ${occlusalY} ${outer * 0.75} ${occlusalY * 0.94}
      Q ${hw} ${crownShoulderY} ${outer} ${baseY}
      Q ${neck} ${baseY * 0.55} ${neck} ${cervicalY}
      Q ${rootSpread} ${cervicalY * 0.9} ${rootSpread} ${rootMidY}
      Q ${neck * 0.45} ${rootTipY * 0.98} 0 ${rootTipY}
      Q ${-neck * 0.45} ${rootTipY * 0.98} ${-rootSpread} ${rootMidY}
      Q ${-rootSpread} ${cervicalY * 0.9} ${-neck} ${cervicalY}
      Q ${-neck} ${baseY * 0.55} ${-outer} ${baseY}
      Z
    `;
  }

  getFissureMainPath(p: PiezaUI): string {
    const dir = p.esSuperior ? 1 : -1;
    return `M ${-p.hw * 0.34} ${dir * p.hh * 0.14} Q 0 ${dir * p.hh * 0.3} ${p.hw * 0.34} ${dir * p.hh * 0.14}`;
  }

  getFissureSubPathA(p: PiezaUI): string {
    const dir = p.esSuperior ? 1 : -1;
    return `M ${-p.hw * 0.24} ${dir * p.hh * 0.06} Q ${-p.hw * 0.03} ${dir * p.hh * 0.18} ${p.hw * 0.2} ${dir * p.hh * 0.06}`;
  }

  getFissureSubPathB(p: PiezaUI): string {
    const dir = p.esSuperior ? 1 : -1;
    return `M ${-p.hw * 0.2} ${dir * p.hh * 0.24} Q 0 ${dir * p.hh * 0.33} ${p.hw * 0.2} ${dir * p.hh * 0.24}`;
  }

  /** Solo la zona radicular (para relleno endodoncia). */
  getToothRootOnlyPath(p: PiezaUI): string {
    const { hw, hh, esSuperior } = p;
    const rootLen = Math.min(10, hh * 0.45);
    const nw = hw * 0.35;
    if (esSuperior) {
      return `M ${nw} ${-hh} L ${nw} ${-hh - rootLen} L ${-nw} ${-hh - rootLen} L ${-nw} ${-hh} Z`;
    }
    return `M ${nw} ${hh} L ${nw} ${hh + rootLen} L ${-nw} ${hh + rootLen} L ${-nw} ${hh} Z`;
  }

  /** Offset Y del centro de la rueda respecto al centro del grupo. */
  getWheelCenterY(p: PiezaUI): number {
    const rootLen = Math.min(10, p.hh * 0.45);
    return p.esSuperior ? p.hh + rootLen + 18 : -p.hh - rootLen - 18;
  }

  getSupLabel(p: PiezaUI, sup: 'V' | 'L' | 'M' | 'D' | 'O') {
    const { hw, hh, esSuperior } = p;
    const f = 0.68;
    switch (sup) {
      case 'V': return { x: 0,       y: esSuperior ? -hh * f : hh * f };
      case 'L': return { x: 0,       y: esSuperior ?  hh * f : -hh * f };
      case 'M': return { x: -hw * f, y: 0 };
      case 'D': return { x:  hw * f, y: 0 };
      case 'O': return { x: 0,       y: 0 };
    }
  }

  // ── Colores ───────────────────────────────────────────────────────────

  /** true si el documento tiene data-theme="dark" activo */
  private isDarkMode(): boolean {
    return this.doc.documentElement.getAttribute('data-theme') === 'dark';
  }

  /** URL del gradiente de esmalte para el cuerpo del diente (silueta anatómica). */
  getBodyGradientUrl(): string {
    return this.isDarkMode() ? 'url(#od-body-enamel-d)' : 'url(#od-body-enamel-l)';
  }

  /** ID del filtro de sombra por diente según tema. */
  getToothShadowFilterId(): string {
    return this.isDarkMode() ? 'od-f-tooth-d' : 'od-f-tooth-l';
  }

  /** Fill del cuerpo del diente: esmalte (sano), gradiente corona/implante o fondo ausente. */
  getToothBodyFill(p: PiezaUI): string {
    if (this.isAusente(p.fdi)) return this.isDarkMode() ? '#1c1b18' : '#f5f3ee';
    if (this.tieneImplante(p.fdi)) return ESTADO_COLOR['IMPLANTE'];
    if (this.tieneCorona(p.fdi)) return this.isDarkMode() ? 'url(#od-corona-d)' : 'url(#od-corona-l)';
    return this.getBodyGradientUrl();
  }

  /**
   * Gradiente de marfil para SANO según superficie y modo de tema.
   * Usa IDs de gradiente hardcodeados con hex — no CSS variables en SVG.
   */
  getColor(fdi: number, sup: Superficie): string {
    const estado = this.getSup(fdi, sup);
    if (estado !== 'SANO') return ESTADO_COLOR[estado];

    const d  = this.isDarkMode() ? 'd' : 'l';           // dark/light
    const vs = fdi >= 11 && fdi <= 28 ? 's' : 'i';     // superior/inferior

    switch (sup) {
      case 'VESTIBULAR': return `url(#od-v-${vs}-${d})`;
      case 'LINGUAL':    return `url(#od-l-${vs}-${d})`;
      case 'MESIAL':     return `url(#od-m-${d})`;
      case 'DISTAL':     return `url(#od-di-${d})`;
      case 'OCLUSAL':    return `url(#od-o-${d})`;
      default:           return 'transparent';
    }
  }

  // ── API pública ───────────────────────────────────────────────────────

  inicializar(piezas: Map<number, PiezaDental>): void {
    const copia = new Map(piezas);
    this.asegurarTodasPiezas(copia);
    this.piezasLocal.set(copia);
  }

  getPiezasLocal(): Map<number, PiezaDental> { return this.piezasLocal(); }

  private asegurarTodasPiezas(map: Map<number, PiezaDental>): void {
    const todas = this.modo() === 'adulto'
      ? [...PIEZAS_PERMANENTES_SUPERIOR, ...PIEZAS_PERMANENTES_INFERIOR]
      : [...PIEZAS_DECIDUAS_SUPERIOR,    ...PIEZAS_DECIDUAS_INFERIOR];
    for (const fdi of todas)
      if (!map.has(fdi)) map.set(fdi, crearPiezaVacia(fdi, fdi >= 50));
  }

  cambiarModo(modo: 'adulto' | 'pediatrico'): void {
    if (this.hasCambiosLocales()) {
      const ok = this.doc.defaultView?.confirm(
        'Hay cambios sin guardar en el odontograma. Si cambias el modo, podrías perder contexto visual. ¿Deseas continuar?',
      );
      if (!ok) return;
    }
    this.modo.set(modo);
    this.odontogramaSvc.setModo(modo);
    const copia = new Map(this.piezasInput());
    this.asegurarTodasPiezas(copia);
    this.piezasLocal.set(copia);
    this.cerrarPopup();
    this.cerrarContextMenu();
  }

  // ── Estado de piezas ──────────────────────────────────────────────────

  getSup(fdi: number, sup: Superficie): EstadoDental {
    return this.piezasLocal().get(fdi)?.superficies[sup] ?? 'SANO';
  }

  isAusente(fdi: number): boolean {
    return this.piezasLocal().get(fdi)?.ausente ?? false;
  }

  isPopupPieza(fdi: number, sup: Superficie): boolean {
    return this.popupFdi() === fdi && this.popupSuperficie() === sup && this.popupVisible();
  }

  isPiezaResaltada(fdi: number): boolean {
    return this.piezaResaltadaFdi() === fdi;
  }

  setPiezaResaltada(fdi: number | null): void {
    this.piezaResaltadaFdi.set(fdi);
  }

  // ── Interacción ───────────────────────────────────────────────────────

  onSuperficieClick(event: MouseEvent, fdi: number, superficie: Superficie): void {
    if (this.readonly()) return;
    event.stopPropagation();
    this.setPiezaResaltada(fdi);
    if (this.isAusente(fdi)) return;
    const tratamientoSel = this.odontogramaSvc.tratamientoSeleccionado();
    if (tratamientoSel) {
      this.aplicarEstado(tratamientoSel, fdi, superficie);
      return;
    }
    const wrap = (event.target as Element).closest('.odontograma-svg-container') as HTMLElement;
    if (!wrap) return;
    const rect = wrap.getBoundingClientRect();
    let px = event.clientX - rect.left;
    let py = event.clientY - rect.top;
    if (px + 230 > rect.width)  px -= 230;
    if (py + 340 > rect.height) py -= 340;
    this.popupX.set(Math.max(4, px));
    this.popupY.set(Math.max(4, py));
    this.popupFdi.set(fdi);
    this.popupSuperficie.set(superficie);
    this.popupVisible.set(true);
  }

  onSuperficieContextMenu(event: MouseEvent, fdi: number, superficie: Superficie): void {
    if (this.readonly()) return;
    event.preventDefault();
    event.stopPropagation();
    if (this.isAusente(fdi)) return;
    this.contextFdi.set(fdi);
    this.contextSuperficie.set(superficie);
    this.contextX.set(event.clientX);
    this.contextY.set(event.clientY);
    this.contextVisible.set(true);
  }

  onSuperficieDblClick(_event: MouseEvent, fdi: number, superficie: Superficie): void {
    this.historialFdi.set(fdi);
    this.historialSuperficie.set(superficie);
    this.historialVisible.set(true);
  }

  getHistorialParaModal(): Tratamiento[] {
    const fdi = this.historialFdi();
    const sup = this.historialSuperficie();
    if (fdi === null || sup === null) return [];
    return this.odontogramaSvc.getHistorial(fdi, sup);
  }

  cerrarContextMenu(): void {
    this.contextVisible.set(false);
    this.contextFdi.set(null);
    this.contextSuperficie.set(null);
  }

  cerrarHistorial(): void {
    this.historialVisible.set(false);
    this.historialFdi.set(null);
    this.historialSuperficie.set(null);
  }

  aplicarEstado(estado: EstadoDental, fdiOverride?: number, supOverride?: Superficie): void {
    const fdi = fdiOverride ?? this.popupFdi();
    const sup = supOverride ?? this.popupSuperficie();
    if (fdi === null || fdi === undefined || sup === null || sup === undefined) return;
    const mapa  = new Map(this.piezasLocal());
    const pieza = { ...(mapa.get(fdi) ?? crearPiezaVacia(fdi, fdi >= 50)) };
    const anterior = pieza.superficies[sup] ?? 'SANO';
    pieza.superficies = { ...pieza.superficies, [sup]: estado };
    if (estado === 'AUSENTE') {
      pieza.superficies.GENERAL = 'AUSENTE';
      pieza.ausente = true;
    } else if (sup === 'GENERAL') {
      pieza.ausente = false;
      if (pieza.superficies.GENERAL === 'AUSENTE') pieza.superficies.GENERAL = estado;
    }
    mapa.set(fdi, pieza);
    this.piezasLocal.set(mapa);
    this.cambio.emit({ fdi, superficie: sup, estadoAnterior: anterior, estadoNuevo: estado });
    this.odontogramaSvc.agregarAlHistorial(fdi, sup, {
      tipo: estado,
      fecha: new Date().toISOString(),
      profesional: this.auth.currentUser()?.nombreCompleto ?? '',
    });
    this.cerrarPopup();
    this.cerrarContextMenu();
  }

  cerrarPopup(): void {
    this.popupVisible.set(false);
    this.popupFdi.set(null);
    this.popupSuperficie.set(null);
  }

  /** Tiene endodoncia en cualquier superficie (para dibujar relleno radicular). */
  tieneEndodoncia(fdi: number): boolean {
    return this.getSup(fdi, 'OCLUSAL') === 'ENDODONCIA' || this.getSup(fdi, 'GENERAL') === 'ENDODONCIA' ||
      Object.values(this.piezasLocal().get(fdi)?.superficies ?? {}).includes('ENDODONCIA');
  }

  /** Tiene implante (pieza completa). */
  tieneImplante(fdi: number): boolean {
    return this.getSup(fdi, 'GENERAL') === 'IMPLANTE';
  }

  @HostListener('document:click')
  onDocumentClick(): void { this.cerrarPopup(); }

  @HostListener('document:keydown', ['$event'])
  onDocumentKeydown(event: KeyboardEvent): void {
    if (this.readonly()) return;
    if (this.isTypingTarget(event.target)) return;
    const key = event.key.toLowerCase();
    if (key === 'escape') {
      this.cerrarPopup();
      this.cerrarContextMenu();
      this.cerrarHistorial();
      this.odontogramaSvc.setTratamientoSeleccionado(null);
      return;
    }
    const shortcutMap: Partial<Record<string, EstadoDental>> = {
      c: 'CARIES',
      o: 'OBTURACION',
      e: 'ENDODONCIA',
      i: 'IMPLANTE',
      x: 'EXTRACCION_INDICADA',
      s: 'SELLANTE',
      p: 'PROTESIS',
      a: 'AUSENTE',
    };
    const estado = shortcutMap[key];
    if (!estado) return;
    event.preventDefault();
    this.odontogramaSvc.setTratamientoSeleccionado(estado);
  }

  onPiezaKeyboardActivate(event: KeyboardEvent, fdi: number): void {
    if (this.readonly()) return;
    if (event.key !== 'Enter' && event.key !== ' ') return;
    event.preventDefault();
    const foco = this.doc.activeElement as SVGElement | null;
    const rect = foco?.getBoundingClientRect();
    if (!rect) return;
    this.popupX.set(Math.max(8, rect.x));
    this.popupY.set(Math.max(8, rect.y));
    this.popupFdi.set(fdi);
    this.popupSuperficie.set('OCLUSAL');
    this.popupVisible.set(true);
  }

  getEstadoColor(key: unknown): string  { return ESTADO_COLOR[key as EstadoDental] ?? 'transparent'; }
  getEstadoLabel(key: unknown): string  { return ESTADO_LABEL[key as EstadoDental] ?? String(key); }

  // ── Notas y limpiar (tipo React) ────────────────────────────────────────

  getObservacion(fdi: number): string {
    return this.piezasLocal().get(fdi)?.observacion ?? '';
  }

  setObservacion(fdi: number, texto: string): void {
    const mapa = new Map(this.piezasLocal());
    const pieza = mapa.get(fdi);
    if (!pieza) return;
    const p = { ...pieza, observacion: texto };
    mapa.set(fdi, p);
    this.piezasLocal.set(mapa);
  }

  /** Limpia todas las superficies y estados de la pieza (excepto ausente se resetea). */
  limpiarPieza(fdi: number): void {
    const mapa = new Map(this.piezasLocal());
    const pediatrica = fdi >= 50;
    mapa.set(fdi, crearPiezaVacia(fdi, pediatrica));
    this.piezasLocal.set(mapa);
    this.setPiezaResaltada(null);
    this.cerrarPopup();
  }

  // ── Serialización / Snapshots / Export-Import (tipo React) ─────────────

  serializePiezas(): OdontogramaExportData['piezas'] {
    const list: OdontogramaExportData['piezas'] = [];
    for (const [fdi, p] of this.piezasLocal().entries()) {
      const supRecord: Record<string, string> = {};
      for (const [k, v] of Object.entries(p.superficies)) supRecord[k] = v;
      list.push({
        fdi: p.fdi,
        esPediatrica: p.esPediatrica,
        ausente: p.ausente,
        observacion: p.observacion,
        superficies: supRecord,
      });
    }
    return list;
  }

  deserializePiezas(
    data: OdontogramaExportData['piezas'],
    modoObjetivo: 'adulto' | 'pediatrico' = this.modo(),
  ): Map<number, PiezaDental> {
    const mapa = new Map<number, PiezaDental>();
    const todas = modoObjetivo === 'adulto'
      ? [...PIEZAS_PERMANENTES_SUPERIOR, ...PIEZAS_PERMANENTES_INFERIOR]
      : [...PIEZAS_DECIDUAS_SUPERIOR, ...PIEZAS_DECIDUAS_INFERIOR];
    for (const fdi of todas) {
      mapa.set(fdi, crearPiezaVacia(fdi, fdi >= 50));
    }
    for (const item of data) {
      const pieza: PiezaDental = {
        fdi: item.fdi,
        esPediatrica: item.esPediatrica,
        ausente: item.ausente ?? false,
        observacion: item.observacion,
        superficies: {
          MESIAL: 'SANO', DISTAL: 'SANO', VESTIBULAR: 'SANO',
          LINGUAL: 'SANO', OCLUSAL: 'SANO', GENERAL: 'SANO',
        },
      };
      for (const [k, v] of Object.entries(item.superficies ?? {})) {
        if (!this.isSuperficieValida(k) || !this.isEstadoValido(v)) continue;
        pieza.superficies[k] = v;
      }
      if (pieza.superficies.GENERAL === 'AUSENTE') pieza.ausente = true;
      mapa.set(item.fdi, pieza);
    }
    return mapa;
  }

  takeSnapshot(): void {
    const payload: OdontogramaExportData = {
      modo: this.modo(),
      fecha: new Date().toISOString(),
      piezas: this.serializePiezas(),
    };
    const snap: OdontogramaSnapshot = {
      id: typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID() : `snap_${Date.now()}`,
      fecha: payload.fecha,
      modo: payload.modo,
      data: payload.piezas,
    };
    this.snapshots.update(s => [snap, ...s]);
  }

  restoreSnapshot(snap: OdontogramaSnapshot): void {
    if (snap.modo === 'pediatrico' || snap.modo === 'adulto') {
      this.modo.set(snap.modo);
      this.odontogramaSvc.setModo(snap.modo);
    }
    const modoObjetivo = snap.modo === 'pediatrico' || snap.modo === 'adulto' ? snap.modo : this.modo();
    const mapa = this.deserializePiezas(snap.data, modoObjetivo);
    this.piezasLocal.set(mapa);
    this.setPiezaResaltada(null);
    this.cerrarPopup();
  }

  clearSnapshots(): void {
    this.snapshots.set([]);
  }

  exportJson(): void {
    const payload: OdontogramaExportData = {
      modo: this.modo(),
      fecha: new Date().toISOString(),
      piezas: this.serializePiezas(),
    };
    const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `odontograma_${this.modo()}_${payload.fecha.slice(0, 10)}.json`;
    a.click();
    URL.revokeObjectURL(url);
  }

  onFileImport(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      try {
        const parsed = JSON.parse(reader.result as string) as OdontogramaExportData;
        if (parsed?.piezas?.length !== undefined) {
          const modoObjetivo = parsed.modo === 'pediatrico' || parsed.modo === 'adulto'
            ? parsed.modo
            : this.modo();
          this.modo.set(modoObjetivo);
          this.odontogramaSvc.setModo(modoObjetivo);
          const mapa = this.deserializePiezas(parsed.piezas, modoObjetivo);
          this.piezasLocal.set(mapa);
        }
      } catch {
        console.error('Archivo JSON inválido');
      }
      input.value = '';
    };
    reader.readAsText(file);
  }

  downloadSnapshot(snap: OdontogramaSnapshot): void {
    const payload: OdontogramaExportData = {
      modo: snap.modo as 'adulto' | 'pediatrico',
      fecha: snap.fecha,
      piezas: snap.data,
    };
    const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `snapshot_${snap.id}.json`;
    a.click();
    URL.revokeObjectURL(url);
  }

  supNombre(sup: Superficie | null): string {
    const map: Record<string, string> = {
      VESTIBULAR: 'Vestibular', LINGUAL: 'Lingual / Palatino',
      MESIAL: 'Mesial', DISTAL: 'Distal', OCLUSAL: 'Oclusal / Incisal', GENERAL: 'General',
    };
    return sup ? (map[sup] ?? sup) : '';
  }

  setVisualMode(mode: VisualMode): void {
    this.visualMode.set(mode);
    this.persistVisualMode(mode);
  }

  toggleVisualMode(): void {
    this.setVisualMode(this.visualMode() === 'hiperrealista' ? 'operativo' : 'hiperrealista');
  }

  private isEstadoValido(value: unknown): value is EstadoDental {
    return typeof value === 'string' && ESTADOS_LISTA.includes(value as EstadoDental);
  }

  private isSuperficieValida(value: string): value is Superficie {
    return SUPERFICIES_VALIDAS.includes(value as Superficie);
  }

  private isTypingTarget(target: EventTarget | null): boolean {
    if (!(target instanceof HTMLElement)) return false;
    const tag = target.tagName.toLowerCase();
    return tag === 'input' || tag === 'textarea' || tag === 'select' || target.isContentEditable;
  }

  private hasCambiosLocales(): boolean {
    const current = JSON.stringify(this.serializePiezas());
    const fromInput = (() => {
      const list: OdontogramaExportData['piezas'] = [];
      for (const p of this.piezasInput().values()) {
        list.push({
          fdi: p.fdi,
          esPediatrica: p.esPediatrica,
          ausente: p.ausente,
          observacion: p.observacion,
          superficies: { ...p.superficies },
        });
      }
      return JSON.stringify(list);
    })();
    return current !== fromInput;
  }

  private restoreVisualMode(): void {
    const stored = this.doc.defaultView?.localStorage?.getItem(this.visualModeStorageKey);
    if (stored === 'hiperrealista' || stored === 'operativo') {
      this.visualMode.set(stored);
    }
  }

  private persistVisualMode(mode: VisualMode): void {
    this.doc.defaultView?.localStorage?.setItem(this.visualModeStorageKey, mode);
  }
}
