/**
 * Modelos de dominio para el módulo Odontología.
 * Autor: Ing. J Sebastian Vargas S
 */

export type HigieneOral = 'BUENA' | 'REGULAR' | 'MALA';
export type RiesgoCaries = 'BAJO' | 'MEDIO' | 'ALTO';
export type CondicionPeriodontal = 'SANA' | 'LEVE' | 'MODERADA' | 'SEVERA';
export type TipoPago = 'EPS' | 'PARTICULAR' | 'MIXTO';
export type EstadoPlan = 'PENDIENTE' | 'EN_TRATAMIENTO' | 'FINALIZADO' | 'CANCELADO';
export type EstadoConsulta = 'EN_ATENCION' | 'FINALIZADO' | 'ATENDIDA' | 'CANCELADO' | 'CANCELADA';

/** Tipo de imagen clínica */
export type TipoImagen =
  | 'RADIOGRAFIA_PERIAPICAL'
  | 'RADIOGRAFIA_PANORAMICA'
  | 'FOTO_CLINICA'
  | 'MODELO'
  | 'OTRO';

/** Superficie de la pieza dental (sistema FDI de 5 superficies) */
export type Superficie = 'MESIAL' | 'DISTAL' | 'VESTIBULAR' | 'LINGUAL' | 'OCLUSAL' | 'GENERAL';

/** Estado clínico de una superficie dental */
export type EstadoDental =
  | 'SANO'
  | 'CARIES'
  | 'OBTURACION'
  | 'ENDODONCIA'
  | 'CORONA'
  | 'AUSENTE'
  | 'PROTESIS'
  | 'FRACTURA'
  | 'SELLANTE'
  | 'EXTRACCION_INDICADA'
  | 'IMPLANTE';

export const ESTADO_COLOR: Record<EstadoDental, string> = {
  SANO:                 'transparent',
  CARIES:               '#ef4444',
  OBTURACION:           '#3b82f6',
  ENDODONCIA:           '#eab308',
  CORONA:               '#6b7280',
  AUSENTE:              '#1f2937',
  PROTESIS:             '#8b5cf6',
  FRACTURA:             '#f97316',
  SELLANTE:             '#22c55e',
  EXTRACCION_INDICADA:  '#dc2626',
  IMPLANTE:             '#0ea5e9',
};

export const ESTADO_LABEL: Record<EstadoDental, string> = {
  SANO:                 'Sano',
  CARIES:               'Caries',
  OBTURACION:           'Obturación',
  ENDODONCIA:           'Endodoncia',
  CORONA:               'Corona',
  AUSENTE:              'Ausente',
  PROTESIS:             'Prótesis',
  FRACTURA:             'Fractura',
  SELLANTE:             'Sellante',
  EXTRACCION_INDICADA:  'Extrac. Indicada',
  IMPLANTE:             'Implante',
};

/** Representa el estado de una superficie individual de un diente */
export interface SuperficieEstado {
  superficie: Superficie;
  estado: EstadoDental;
  observacion?: string;
}

/** Pieza dental con sus 5 superficies y estado general */
export interface PiezaDental {
  fdi: number;
  esPediatrica: boolean;
  superficies: Record<Superficie, EstadoDental>;
  ausente: boolean;
  observacion?: string;
}

export interface OdontogramaEstadoDto {
  id?: number;
  pacienteId: number;
  profesionalId: number;
  profesionalNombre?: string;
  consultaId?: number;
  piezaFdi: number;
  superficie: Superficie;
  estado: EstadoDental;
  observacion?: string;
  createdAt?: string;
}

export type OdontogramaCambioKey = `${number}-${Superficie}`;

export type TipoConsultaOdonto = 'PRIMERA_VEZ' | 'CONTROL' | 'URGENCIA_ODONTOLOGICA' | 'INTERCONSULTA';

export interface ConsultaOdontologicaDto {
  id?: number;
  pacienteId: number;
  pacienteNombre?: string;
  pacienteDocumento?: string;
  pacienteEdad?: number;
  pacienteEps?: string;
  profesionalId: number;
  profesionalNombre?: string;
  citaId?: number;
  // Campos normativos
  tipoConsulta?: TipoConsultaOdonto;
  codigoCie10?: string;
  descripcionCie10?: string;
  consentimientoFirmado?: boolean;
  fechaConsentimiento?: string;
  consentimientoObservaciones?: string;
  // SOAP
  motivoConsulta?: string;
  enfermedadActual?: string;
  antecedentesOdontologicos?: string;
  antecedentesSistemicos?: string;
  medicamentosActuales?: string;
  alergias?: string;
  habitosOrales?: string;
  higieneOral?: HigieneOral;
  examenExtraOral?: string;
  examenIntraOral?: string;
  // Índice CPOD (permanente) y ceod (temporal)
  cpodCariados?: number;
  cpodPerdidos?: number;
  cpodObturados?: number;
  ceodCariados?: number;
  ceodExtraidos?: number;
  ceodObturados?: number;
  // Índice IHO-S
  ihosPlaca?: number;
  ihosCalculo?: number;
  condicionPeriodontal?: CondicionPeriodontal;
  riesgoCaries?: RiesgoCaries;
  diagnostico?: string;
  planTratamiento?: string;
  firmaProfesionalUrl?: string;
  firmaCanvasData?: string;
  estado?: EstadoConsulta;
  createdAt?: string;
  updatedAt?: string;
}

export interface ProcedimientoCatalogo {
  id?: number;
  codigo?: string;
  nombre: string;
  descripcion?: string;
  categoria?: string;
  precioBase?: number;
  activo?: boolean;
  origen?: string;
}

export interface PlanTratamientoItemDto {
  id?: number;
  planId?: number;
  procedimientoId: number;
  procedimientoNombre?: string;
  procedimientoCodigo?: string;
  piezaFdi?: number;
  cantidad?: number;
  precioUnitario: number;
  descuento?: number;
  valorTotal: number;
  estado?: string;
  observaciones?: string;
  createdAt?: string;
}

export interface PlanTratamientoDto {
  id?: number;
  pacienteId: number;
  pacienteNombre?: string;
  profesionalId: number;
  profesionalNombre?: string;
  consultaId?: number;
  nombre?: string;
  fase?: number;
  descripcion?: string;
  valorTotal?: number;
  descuento?: number;
  valorFinal?: number;
  valorAbonado?: number;
  saldoPendiente?: number;
  tipoPago?: TipoPago;
  estado?: EstadoPlan;
  fechaInicio?: string;
  fechaFin?: string;
  items?: PlanTratamientoItemDto[];
  createdAt?: string;
  updatedAt?: string;
}

export interface ImagenClinicaDto {
  id?: number;
  pacienteId: number;
  profesionalId: number;
  profesionalNombre?: string;
  consultaId?: number;
  piezaFdi?: number;
  tipo?: TipoImagen;
  nombreArchivo?: string;
  url?: string;
  thumbnailBase64?: string;
  descripcion?: string;
  createdAt?: string;
}

export interface EvolucionOdontologicaDto {
  id?: number;
  pacienteId: number;
  profesionalId: number;
  profesionalNombre?: string;
  consultaId: number;
  planId?: number;
  notaEvolucion: string;
  controlPostTratamiento?: string;
  proximaCitaRecomendada?: string;
  createdAt?: string;
}

/** Piezas permanentes FDI (adulto) */
export const PIEZAS_PERMANENTES_SUPERIOR = [18,17,16,15,14,13,12,11, 21,22,23,24,25,26,27,28];
export const PIEZAS_PERMANENTES_INFERIOR = [48,47,46,45,44,43,42,41, 31,32,33,34,35,36,37,38];

/** Piezas deciduas/temporales FDI (pediátrico) */
export const PIEZAS_DECIDUAS_SUPERIOR = [55,54,53,52,51, 61,62,63,64,65];
export const PIEZAS_DECIDUAS_INFERIOR = [85,84,83,82,81, 71,72,73,74,75];

export const SUPERFICIES_POSTERIORES: Superficie[] = ['VESTIBULAR','MESIAL','OCLUSAL','DISTAL','LINGUAL'];
export const SUPERFICIES_ANTERIORES: Superficie[] = ['VESTIBULAR','MESIAL','INCISAL' as Superficie,'DISTAL','LINGUAL'];

export function esPiezaAnterior(fdi: number): boolean {
  const n = fdi % 10;
  return n >= 1 && n <= 3;
}

export function crearPiezaVacia(fdi: number, pediatrica = false): PiezaDental {
  return {
    fdi,
    esPediatrica: pediatrica,
    ausente: false,
    superficies: {
      MESIAL: 'SANO', DISTAL: 'SANO', VESTIBULAR: 'SANO',
      LINGUAL: 'SANO', OCLUSAL: 'SANO', GENERAL: 'SANO',
    },
  };
}

/** Convierte lista de OdontogramaEstadoDto a mapa fdi->PiezaDental */
export function dtoListToPiezas(dtos: OdontogramaEstadoDto[]): Map<number, PiezaDental> {
  const ordenados = [...dtos].sort((a, b) => {
    const ta = a.createdAt ? Date.parse(a.createdAt) : 0;
    const tb = b.createdAt ? Date.parse(b.createdAt) : 0;
    return ta - tb;
  });
  const map = new Map<number, PiezaDental>();
  for (const dto of ordenados) {
    if (!map.has(dto.piezaFdi)) {
      const pediatrica = dto.piezaFdi >= 50;
      map.set(dto.piezaFdi, crearPiezaVacia(dto.piezaFdi, pediatrica));
    }
    const pieza = map.get(dto.piezaFdi)!;
    if (dto.superficie === 'GENERAL') {
      pieza.ausente = dto.estado === 'AUSENTE';
    }
    pieza.superficies[dto.superficie] = dto.estado;
  }
  return map;
}

/** Convierte el mapa de piezas modificadas a lista de DTOs para batch */
export function piezasToDtosSnapshot(
  piezas: Map<number, PiezaDental>,
  pacienteId: number,
  profesionalId: number,
  consultaId?: number,
): OdontogramaEstadoDto[] {
  const dtos: OdontogramaEstadoDto[] = [];
  for (const pieza of piezas.values()) {
    for (const sup of Object.keys(pieza.superficies) as Superficie[]) {
      const estado = pieza.superficies[sup];
      dtos.push({ pacienteId, profesionalId, consultaId, piezaFdi: pieza.fdi, superficie: sup, estado });
    }
  }
  return dtos;
}

/** Convierte solo piezas-superficies cambiadas (modo diff) a DTOs para batch */
export function piezasToDtosDiff(
  piezas: Map<number, PiezaDental>,
  pacienteId: number,
  profesionalId: number,
  cambios: Iterable<OdontogramaCambioKey>,
  consultaId?: number,
): OdontogramaEstadoDto[] {
  const dtos: OdontogramaEstadoDto[] = [];
  for (const key of cambios) {
    const [fdiRaw, superficieRaw] = key.split('-');
    const fdi = Number(fdiRaw);
    if (!Number.isFinite(fdi)) continue;
    const superficie = superficieRaw as Superficie;
    const pieza = piezas.get(fdi);
    if (!pieza) continue;
    const estado = pieza.superficies[superficie];
    if (!estado) continue;
    dtos.push({ pacienteId, profesionalId, consultaId, piezaFdi: fdi, superficie, estado });
  }
  return dtos;
}

/** Compatibilidad con llamadas existentes (mantiene snapshot completo). */
export function piezasChangedToDtos(
  piezas: Map<number, PiezaDental>,
  pacienteId: number,
  profesionalId: number,
  consultaId?: number,
): OdontogramaEstadoDto[] {
  return piezasToDtosSnapshot(piezas, pacienteId, profesionalId, consultaId);
}
