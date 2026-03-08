/**
 * Servicio de generación de PDF con jsPDF — Historias clínicas, órdenes (lab, medicamento, procedimiento).
 * Tema SESA: profesional, completo, legible.
 * Autor: Ing. J Sebastian Vargas S
 */
import { Injectable } from '@angular/core';
import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';
import type {
  PacienteDto,
} from './paciente.service';
import type { HistoriaClinicaDto } from './historia-clinica.service';
import type { ConsultaDto } from './consulta.service';
import type { OrdenClinicaDto } from './orden-clinica.service';
import { parseResultadoToItems } from '../utils/resultado-display.util';

const MARGIN = 18;
const PAGE_W = 210;
const PAGE_H = 297;
/** Zona reservada para el pie: no dibujar contenido por debajo de este Y. */
const FOOTER_TOP = 282;
/** Altura de la cabecera: contenido debe empezar por debajo de este Y (evitar solapamiento). */
const HEADER_H = 50;
const LINE_HEIGHT = 5;
const SECTION_GAP = 8;
const LABEL_WIDTH = 38;
const BRAND_PRIMARY: [number, number, number] = [12, 35, 64];   // #0c2340
const BRAND_ACCENT: [number, number, number] = [5, 150, 105];    // #059669
const TEXT_PRIMARY: [number, number, number] = [15, 23, 42];      // #0f172a
const TEXT_SECONDARY: [number, number, number] = [71, 85, 105];   // #475569
const BORDER: [number, number, number] = [226, 232, 240];        // #e2e8f0
const BG_SOFT: [number, number, number] = [240, 253, 244];     // #f0fdf4

/** Paleta blanco y negro — PDF hospitalario profesional (Colombia). Documento serio, impresión, archivado. */
const BW_BLACK: [number, number, number] = [0, 0, 0];
const BW_DARK: [number, number, number] = [40, 40, 40];
const BW_MID: [number, number, number] = [90, 90, 90];
const BW_LIGHT: [number, number, number] = [220, 220, 220];
const BW_BG: [number, number, number] = [248, 248, 248];
const BW_BG_ALT: [number, number, number] = [252, 252, 252];

export interface SesaPdfPaciente {
  id: number;
  nombres: string;
  apellidos?: string;
  documento?: string;
  tipoDocumento?: string;
  fechaNacimiento?: string;
  sexo?: string;
  telefono?: string;
  email?: string;
  direccion?: string;
  grupoSanguineo?: string;
  epsNombre?: string;
  municipioResidencia?: string;
  departamentoResidencia?: string;
  zonaResidencia?: string;
  regimenAfiliacion?: string;
  tipoUsuario?: string;
  contactoEmergenciaNombre?: string;
  contactoEmergenciaTelefono?: string;
  estadoCivil?: string;
  escolaridad?: string;
  ocupacion?: string;
  pertenenciaEtnica?: string;
}

export interface SesaPdfHistoriaClinica {
  id: number;
  pacienteNombre?: string;
  pacienteDocumento?: string;
  fechaApertura?: string;
  estado?: string;
  grupoSanguineo?: string;
  alergiasGenerales?: string;
  antecedentesPersonales?: string;
  antecedentesFamiliares?: string;
  antecedentesQuirurgicos?: string;
  antecedentesFarmacologicos?: string;
  antecedentesTraumaticos?: string;
  antecedentesGinecoobstetricos?: string;
  habitosTabaco?: boolean;
  habitosAlcohol?: boolean;
  habitosSustancias?: boolean;
  habitosDetalles?: string;
}

/** Branding para cabecera y pie de los PDF (logo + datos de la empresa). */
export interface SesaPdfBranding {
  empresaNombre?: string;
  empresaIdentificacion?: string;
  /** Dirección de la empresa (cabecera y pie). */
  empresaDireccion?: string;
  logoDataUrl?: string;
  /** Usuario que imprime (pie: "Impreso por ..."). */
  printedBy?: string;
  /** Profesional que atiende (bloque final). */
  profesionalNombre?: string;
  profesionalRol?: string;
  profesionalTarjeta?: string;
}

@Injectable({ providedIn: 'root' })
export class SesaJspdfService {

  private getY(doc: jsPDF): number {
    return (doc as jsPDF & { lastAutoTable?: { finalY?: number } }).lastAutoTable?.finalY ?? MARGIN;
  }

  private addHeader(doc: jsPDF, title: string, subtitle?: string, branding?: SesaPdfBranding): number {
    const headerH = 32;
    doc.setFillColor(...BRAND_PRIMARY);
    doc.rect(0, 0, PAGE_W, headerH, 'F');
    let textX = MARGIN;
    const logoW = 26;
    const logoH = 20;
    const logoTop = (headerH - logoH) / 2;
    if (branding?.logoDataUrl) {
      try {
        const fmt = branding.logoDataUrl.includes('image/jpeg') || branding.logoDataUrl.includes('image/jpg') ? 'JPEG' : 'PNG';
        doc.addImage(branding.logoDataUrl, fmt, MARGIN, logoTop, logoW, logoH);
      } catch {
        // Si falla (formato no soportado, etc.) se omite el logo
      }
      textX = MARGIN + logoW + 6;
    }
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(branding?.logoDataUrl ? 14 : 16);
    doc.setFont('helvetica', 'bold');
    doc.text(branding?.empresaNombre?.trim() || 'SESA Salud', textX, 12);
    doc.setFontSize(11);
    doc.text(title, textX, 20);
    if (subtitle) {
      doc.setFontSize(8);
      doc.setTextColor(147, 197, 253);
      doc.text(subtitle, textX, 26);
    }
    doc.setTextColor(...TEXT_PRIMARY);
    doc.setDrawColor(...BRAND_ACCENT);
    doc.setLineWidth(0.8);
    doc.line(0, headerH, PAGE_W, headerH);
    return headerH + 6;
  }

  private addPacienteBlock(doc: jsPDF, paciente: SesaPdfPaciente, startY: number): number {
    const nombre = [paciente.nombres, paciente.apellidos].filter(Boolean).join(' ');
    const docLine = [paciente.tipoDocumento, paciente.documento].filter(Boolean).join(' ');
    doc.setFontSize(10);
    doc.setFont('helvetica', 'bold');
    doc.text('Paciente', MARGIN, startY);
    doc.setFont('helvetica', 'normal');
    doc.text(nombre || '—', MARGIN, startY + 5);
    if (docLine) doc.text(`Documento: ${docLine}`, MARGIN, startY + 10);
    let y = startY + 14;
    const line = (label: string, value: string | undefined) => {
      if (!value) return;
      doc.setFontSize(9);
      doc.setTextColor(...TEXT_SECONDARY);
      doc.text(`${label}: ${value}`, MARGIN, y);
      y += 5;
    };
    doc.setTextColor(...TEXT_PRIMARY);
    line('Fecha nac.', paciente.fechaNacimiento);
    line('Sexo', paciente.sexo);
    line('Teléfono', paciente.telefono);
    line('Email', paciente.email);
    line('Dirección', paciente.direccion);
    line('Grupo sanguíneo', paciente.grupoSanguineo);
    doc.setTextColor(...TEXT_PRIMARY);
    return y + 6;
  }

  private addSectionTitle(doc: jsPDF, title: string, y: number): number {
    doc.setFillColor(...BRAND_ACCENT);
    doc.rect(0, y - 5, PAGE_W, 8, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(10);
    doc.setFont('helvetica', 'bold');
    doc.text(title, MARGIN, y + 1);
    doc.setTextColor(...TEXT_PRIMARY);
    return y + 12;
  }

  private addFooter(doc: jsPDF, pageNum?: number, branding?: SesaPdfBranding): void {
    const total = pageNum ?? doc.getNumberOfPages();
    const nombre = branding?.empresaNombre?.trim() || 'SESA Salud';
    doc.setFontSize(8);
    doc.setTextColor(...TEXT_SECONDARY);
    doc.text(
      `Documento generado por ${nombre} · Página ${total}`,
      PAGE_W / 2,
      PAGE_H - 10,
      { align: 'center' }
    );
  }

  /** Cabecera estilo NOTA DE EVOLUCIÓN: logo a la izquierda del nombre de la institución, NIT, dirección, título documento, Página X/Y. */
  private addHeaderDocumento(doc: jsPDF, tituloDoc: string, branding: SesaPdfBranding | undefined, pageNum: number, totalPages: number): number {
    const headerH = 46;
    doc.setFillColor(255, 255, 255);
    doc.rect(0, 0, PAGE_W, headerH, 'F');

    // Logo siempre a la izquierda; nombre de la empresa a la derecha del logo (misma línea visual).
    const logoBoxSize = 22;
    const logoLeft = MARGIN;
    const logoTop = (headerH - logoBoxSize) / 2;
    if (branding?.logoDataUrl) {
      try {
        const fmt = branding.logoDataUrl.includes('image/jpeg') || branding.logoDataUrl.includes('image/jpg') ? 'JPEG' : 'PNG';
        doc.addImage(branding.logoDataUrl, fmt, logoLeft, logoTop, logoBoxSize, logoBoxSize);
      } catch {
        // omitir logo si falla (formato no soportado, etc.)
      }
    }

    const textStartX = branding?.logoDataUrl ? logoLeft + logoBoxSize + 6 : MARGIN;
    const textMaxW = PAGE_W - textStartX - MARGIN - 28;
    const firstLineY = 18;

    const nombreEmp = (branding?.empresaNombre?.trim() || 'SESA Salud').toUpperCase();
    doc.setFontSize(11);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(...TEXT_PRIMARY);
    doc.text(nombreEmp, textStartX, firstLineY);

    let lineY = firstLineY + 6;
    if (branding?.empresaIdentificacion?.trim()) {
      doc.setFontSize(8);
      doc.setFont('helvetica', 'normal');
      doc.setTextColor(...TEXT_SECONDARY);
      doc.text(`NIT: ${branding.empresaIdentificacion.trim()}`, textStartX, lineY);
      lineY += 5;
      doc.setTextColor(...TEXT_PRIMARY);
    }
    if (branding?.empresaDireccion?.trim()) {
      doc.setFontSize(8);
      doc.setFont('helvetica', 'normal');
      doc.setTextColor(...TEXT_SECONDARY);
      const lineasDir = doc.splitTextToSize(branding.empresaDireccion.trim(), textMaxW);
      lineasDir.forEach((line: string) => {
        doc.text(line, textStartX, lineY);
        lineY += 4;
      });
      doc.setTextColor(...TEXT_PRIMARY);
    }

    doc.setFontSize(12);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(...TEXT_PRIMARY);
    doc.text(tituloDoc.toUpperCase(), PAGE_W / 2, 38, { align: 'center' });

    doc.setFontSize(8);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(...TEXT_SECONDARY);
    doc.text(`Página ${pageNum}/${totalPages}`, PAGE_W - MARGIN, firstLineY, { align: 'right' });
    doc.setTextColor(...TEXT_PRIMARY);

    doc.setDrawColor(...BORDER);
    doc.setLineWidth(0.3);
    doc.line(0, headerH - 2, PAGE_W, headerH - 2);
    return headerH + 4;
  }

  /** Título de sección estilo hospitalario: barra lateral, mayúsculas, negrita, subrayado. */
  private addSeccionSubrayada(doc: jsPDF, titulo: string, y: number): number {
    doc.setFillColor(...BRAND_ACCENT);
    doc.rect(MARGIN, y - 3.5, 2, 5, 'F');
    doc.setFontSize(10);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(...TEXT_PRIMARY);
    doc.text(titulo.toUpperCase(), MARGIN + 4, y);
    const tw = doc.getTextWidth(titulo.toUpperCase());
    doc.setLineWidth(0.3);
    doc.setDrawColor(...TEXT_PRIMARY);
    doc.line(MARGIN + 4, y + 1.5, MARGIN + 4 + Math.min(tw, PAGE_W - 2 * MARGIN - 4), y + 1.5);
    return y + 8;
  }

  /** Línea horizontal separadora. */
  private addLineaSeparadora(doc: jsPDF, y: number): number {
    doc.setDrawColor(...BORDER);
    doc.setLineWidth(0.2);
    doc.line(MARGIN, y, PAGE_W - MARGIN, y);
    return y + 5;
  }

  /** Sección estilo hospitalario B&W: barra lateral oscura, título en mayúsculas, subrayado sutil. SaaS pro, serio. */
  private addSeccionHospitalaria(doc: jsPDF, titulo: string, y: number): number {
    doc.setFillColor(...BW_DARK);
    doc.rect(MARGIN, y - 3.2, 2.2, 5.5, 'F');
    doc.setFontSize(9.5);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(...BW_BLACK);
    doc.text(titulo.toUpperCase(), MARGIN + 3.5, y + 0.5);
    const tw = doc.getTextWidth(titulo.toUpperCase());
    doc.setDrawColor(...BW_LIGHT);
    doc.setLineWidth(0.25);
    doc.line(MARGIN + 3.5, y + 2.2, MARGIN + 3.5 + Math.min(tw, PAGE_W - 2 * MARGIN - 4), y + 2.2);
    return y + 7.5;
  }

  /** Línea separadora B&W (gris claro). */
  private addLineaSeparadoraByn(doc: jsPDF, y: number): number {
    doc.setDrawColor(...BW_LIGHT);
    doc.setLineWidth(0.2);
    doc.line(MARGIN, y, PAGE_W - MARGIN, y);
    return y + 5;
  }

  /** Pie estilo NOTA DE EVOLUCIÓN: línea separadora, impreso/fecha, institución, página. Siempre en zona fija. */
  private addFooterNotaEvolucion(doc: jsPDF, pageNum: number, totalPages: number, branding?: SesaPdfBranding): void {
    const yLine = FOOTER_TOP - 4;
    const yLine1 = FOOTER_TOP + 4;
    const yLine2 = FOOTER_TOP + 11;

    doc.setDrawColor(...BORDER);
    doc.setLineWidth(0.3);
    doc.line(MARGIN, yLine, PAGE_W - MARGIN, yLine);

    doc.setFontSize(8);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(...TEXT_SECONDARY);
    const now = new Date();
    const fechaStr = now.toLocaleDateString('es-CO', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit' });
    doc.text(`Impreso el ${fechaStr}${branding?.printedBy ? ' por ' + branding.printedBy : ''}`, MARGIN, yLine1);
    const nombre = branding?.empresaNombre?.trim() || 'SESA Salud';
    const nit = branding?.empresaIdentificacion?.trim() ? ` NIT: ${branding.empresaIdentificacion.trim()}` : '';
    const dir = branding?.empresaDireccion?.trim() ? ` · ${branding.empresaDireccion.trim()}` : '';
    const centerText = `${nombre}${nit}${dir}`;
    doc.setTextColor(...TEXT_PRIMARY);
    doc.setFont('helvetica', 'bold');
    const centerLines = doc.splitTextToSize(centerText, PAGE_W - 2 * MARGIN - 10);
    const maxFooterLines = 2;
    centerLines.slice(0, maxFooterLines).forEach((line: string, idx: number) => {
      doc.text(line, PAGE_W / 2, yLine2 + idx * 4, { align: 'center' });
    });
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(...TEXT_SECONDARY);
    doc.text(`Página ${pageNum}/${totalPages}`, PAGE_W - MARGIN, yLine1, { align: 'right' });
    doc.setTextColor(...TEXT_PRIMARY);
  }

  private checkPageBreak(doc: jsPDF, needed: number): void {
    if (doc.getNumberOfPages() > 0 && (doc as jsPDF & { lastAutoTable?: { finalY?: number } }).lastAutoTable?.finalY != null) {
      const y = (doc as jsPDF & { lastAutoTable?: { finalY?: number } }).lastAutoTable!.finalY!;
      if (y + needed > FOOTER_TOP - 15) {
        doc.addPage();
        (doc as jsPDF & { lastAutoTable?: { finalY?: number } }).lastAutoTable = { finalY: MARGIN };
      }
    }
  }

  /**
   * Formato de edad para PDF: "X Años XX Meses XX Días (dd/mm/yyyy)".
   * Si no hay fecha de nacimiento válida, devuelve "—".
   */
  private formatEdadConFecha(fechaNacimiento: string | undefined): string {
    if (!fechaNacimiento?.trim()) return '—';
    const birth = new Date(fechaNacimiento.trim());
    if (isNaN(birth.getTime())) return '—';
    const today = new Date();
    let years = today.getFullYear() - birth.getFullYear();
    let months = today.getMonth() - birth.getMonth();
    let days = today.getDate() - birth.getDate();
    if (days < 0) {
      months--;
      days += new Date(today.getFullYear(), today.getMonth(), 0).getDate();
    }
    if (months < 0) {
      years--;
      months += 12;
    }
    const pad = (n: number) => String(n).padStart(2, '0');
    const dd = pad(birth.getDate());
    const mm = pad(birth.getMonth() + 1);
    const yyyy = birth.getFullYear();
    return `${years} Años ${pad(months)} Meses ${pad(days)} Días (${dd}/${mm}/${yyyy})`;
  }

  /** Genera PDF de Historia Clínica — Estilo hospitalario profesional (Colombia). Blanco y negro, serio, SaaS pro. Cumple Res. 1995/1999, CIE-10, estructura SOAP. */
  generarHistoriaClinicaPdf(
    paciente: SesaPdfPaciente,
    historia: SesaPdfHistoriaClinica | null,
    ultimaConsulta: ConsultaDto | null,
    ordenes: OrdenClinicaDto[],
    branding?: SesaPdfBranding
  ): Blob {
    const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
    let y = HEADER_H;
    const contentW = PAGE_W - 2 * MARGIN;
    const midX = MARGIN + contentW / 2;
    const colW = contentW / 3;

    const fechaHistoria = ultimaConsulta?.fechaConsulta || historia?.fechaApertura || '—';
    const identif = paciente.documento || '—';
    const nombres = paciente.nombres || '—';
    const apellidos = paciente.apellidos ?? '—';
    const folio = historia?.id ? String(historia.id) : '1';

    doc.setTextColor(...BW_BLACK);

    // ——— Resumen del encuentro (grid) ———
    doc.setFontSize(8);
    doc.setFont('helvetica', 'bold');
    doc.text('Fecha historia:', MARGIN, y);
    doc.setFont('helvetica', 'normal');
    doc.text(doc.splitTextToSize(fechaHistoria, colW - LABEL_WIDTH)[0] || '—', MARGIN + LABEL_WIDTH, y);
    doc.setFont('helvetica', 'bold');
    doc.text('Identificación:', MARGIN + colW, y);
    doc.setFont('helvetica', 'normal');
    doc.text(doc.splitTextToSize(identif, colW - LABEL_WIDTH)[0] || '—', MARGIN + colW + LABEL_WIDTH, y);
    doc.setFont('helvetica', 'bold');
    doc.text('Nº folio:', MARGIN + 2 * colW, y);
    doc.setFont('helvetica', 'normal');
    doc.text(folio, MARGIN + 2 * colW + 22, y);
    y += LINE_HEIGHT + 2;
    doc.setFont('helvetica', 'bold');
    doc.text('Nombres:', MARGIN, y);
    doc.setFont('helvetica', 'normal');
    doc.text(doc.splitTextToSize(nombres, colW - 20)[0] || '—', MARGIN + 22, y);
    doc.setFont('helvetica', 'bold');
    doc.text('Apellidos:', MARGIN + colW, y);
    doc.setFont('helvetica', 'normal');
    doc.text(doc.splitTextToSize(apellidos, colW - 22)[0] || '—', MARGIN + colW + 24, y);
    y += SECTION_GAP;
    y = this.addLineaSeparadoraByn(doc, y);

    // ——— IDENTIFICACIÓN (toda la información personal registrada al crear el paciente) ———
    y = this.addSeccionHospitalaria(doc, 'Identificación', y);
    const leftW = contentW / 2 - 6;
    const rightW = contentW / 2 - 6;
    const labelW = 42;
    let yL = y;
    let yR = y;
    const lineLeft = (label: string, value: string | undefined) => {
      doc.setFontSize(8);
      doc.setFont('helvetica', 'bold');
      doc.setTextColor(...BW_DARK);
      doc.text(label, MARGIN, yL);
      doc.setFont('helvetica', 'normal');
      doc.setTextColor(...BW_BLACK);
      const lines = doc.splitTextToSize(String(value ?? '—'), leftW - labelW);
      lines.forEach((line: string) => { doc.text(line, MARGIN + labelW, yL); yL += LINE_HEIGHT; });
      yL += 2;
    };
    const lineRight = (label: string, value: string | undefined) => {
      doc.setFontSize(8);
      doc.setFont('helvetica', 'bold');
      doc.setTextColor(...BW_DARK);
      doc.text(label, midX, yR);
      doc.setFont('helvetica', 'normal');
      doc.setTextColor(...BW_BLACK);
      const lines = doc.splitTextToSize(String(value ?? '—'), rightW - 28);
      lines.forEach((line: string) => { doc.text(line, midX + 28, yR); yR += LINE_HEIGHT; });
      yR += 2;
    };
    lineLeft('Nombres:', nombres);
    lineLeft('Apellidos:', apellidos);
    lineLeft('Tipo documento:', paciente.tipoDocumento);
    lineLeft('Número documento:', paciente.documento);
    lineLeft('Fecha nacimiento:', paciente.fechaNacimiento);
    lineLeft('Edad:', this.formatEdadConFecha(paciente.fechaNacimiento));
    lineLeft('Sexo:', paciente.sexo);
    lineLeft('Grupo sanguíneo:', paciente.grupoSanguineo);
    lineLeft('Dirección:', paciente.direccion);
    lineLeft('Teléfono:', paciente.telefono);
    lineLeft('Correo electrónico:', paciente.email);
    lineLeft('EPS:', paciente.epsNombre);
    lineRight('Municipio residencia:', paciente.municipioResidencia);
    lineRight('Departamento residencia:', paciente.departamentoResidencia);
    lineRight('Zona residencia:', paciente.zonaResidencia);
    lineRight('Régimen afiliación:', paciente.regimenAfiliacion);
    lineRight('Tipo usuario:', paciente.tipoUsuario);
    lineRight('Contacto emergencia:', paciente.contactoEmergenciaNombre);
    lineRight('Tel. emergencia:', paciente.contactoEmergenciaTelefono);
    lineRight('Estado civil:', paciente.estadoCivil);
    lineRight('Escolaridad:', paciente.escolaridad);
    lineRight('Ocupación:', paciente.ocupacion);
    lineRight('Pertenencia étnica:', paciente.pertenenciaEtnica);
    y = Math.max(yL, yR) + SECTION_GAP;
    y = this.addLineaSeparadoraByn(doc, y);

    if (y > FOOTER_TOP - 55) { doc.addPage(); y = HEADER_H; }
    // ——— ANAMNESIS ———
    y = this.addSeccionHospitalaria(doc, 'Anamnesis', y);
    doc.setFontSize(9);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(...BW_DARK);
    doc.text('Motivo de consulta:', MARGIN, y);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(...BW_BLACK);
    y += LINE_HEIGHT;
    const linesMotivo = doc.splitTextToSize(ultimaConsulta?.motivoConsulta || '—', contentW);
    linesMotivo.forEach((line: string) => { doc.text(line, MARGIN, y); y += LINE_HEIGHT; });
    y += 4;
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(...BW_DARK);
    doc.text('Enfermedad actual:', MARGIN, y);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(...BW_BLACK);
    y += LINE_HEIGHT;
    const linesEnf = doc.splitTextToSize(ultimaConsulta?.enfermedadActual || '—', contentW);
    linesEnf.forEach((line: string) => { doc.text(line, MARGIN, y); y += LINE_HEIGHT; });
    y += SECTION_GAP;
    y = this.addLineaSeparadoraByn(doc, y);

    // ——— IMPRESIÓN DIAGNÓSTICA (CIE-10) ———
    y = this.addSeccionHospitalaria(doc, 'Impresión diagnóstica', y);
    const diag = ultimaConsulta?.diagnostico?.trim();
    const codigoCie = ultimaConsulta?.codigoCie10;
    if (diag || codigoCie) {
      autoTable(doc, {
        startY: y,
        head: [['CIE-10', 'Diagnóstico', 'Observaciones', 'Principal']],
        body: [[codigoCie || '—', diag || '—', '—', 'X']],
        theme: 'plain',
        headStyles: { fillColor: BW_BG, textColor: BW_BLACK, fontStyle: 'bold', fontSize: 8 },
        bodyStyles: { fontSize: 8, textColor: BW_BLACK },
        margin: { top: HEADER_H, left: MARGIN, right: MARGIN, bottom: PAGE_H - FOOTER_TOP + 5 },
        columnStyles: { 0: { cellWidth: 22 }, 1: { cellWidth: 68 }, 2: { cellWidth: 35 }, 3: { cellWidth: 15 } },
      });
      y = this.getY(doc) + SECTION_GAP;
    } else {
      doc.setFontSize(9);
      doc.setTextColor(...BW_MID);
      doc.text('Sin registro.', MARGIN, y);
      doc.setTextColor(...BW_BLACK);
      y += LINE_HEIGHT + 4;
    }
    y = this.addLineaSeparadoraByn(doc, y);

    if (y > FOOTER_TOP - 55) { doc.addPage(); y = HEADER_H; }
    // ——— ANÁLISIS (hallazgos y recomendaciones) ———
    y = this.addSeccionHospitalaria(doc, 'Análisis', y);
    if (ultimaConsulta?.motivoConsulta?.trim()) {
      doc.setFontSize(9);
      doc.setFont('helvetica', 'bold');
      doc.setTextColor(...BW_DARK);
      doc.text('MC:', MARGIN, y);
      doc.setFont('helvetica', 'normal');
      doc.setTextColor(...BW_BLACK);
      y += LINE_HEIGHT;
      const linesMc = doc.splitTextToSize(ultimaConsulta.motivoConsulta, contentW - 10);
      linesMc.forEach((line: string) => { doc.text(line, MARGIN + 10, y); y += LINE_HEIGHT; });
      y += 4;
    }
    const analisis = [ultimaConsulta?.hallazgosExamen, ultimaConsulta?.recomendaciones].filter(Boolean).join(' ');
    const linesAnal = doc.splitTextToSize(analisis || '—', contentW);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(...BW_BLACK);
    linesAnal.forEach((line: string) => { doc.text(line, MARGIN, y); y += LINE_HEIGHT; });
    y += SECTION_GAP;
    y = this.addLineaSeparadoraByn(doc, y);

    if (y > FOOTER_TOP - 70) { doc.addPage(); y = HEADER_H; }
    // ——— ANTECEDENTES ———
    y = this.addSeccionHospitalaria(doc, 'Antecedentes', y);
    const h = historia;
    const antPers = h?.antecedentesPersonales?.trim() ? h.antecedentesPersonales : 'NIEGA';
    const antAlerg = h?.alergiasGenerales?.trim() ? h.alergiasGenerales : 'NIEGA';
    const antQx = h?.antecedentesQuirurgicos?.trim() ? h.antecedentesQuirurgicos : 'NIEGA';
    doc.setFontSize(9);
    doc.setTextColor(...BW_BLACK);
    doc.text(`Antecedentes: ${antPers}`, MARGIN, y);
    y += LINE_HEIGHT;
    doc.text(`Alérgico: ${antAlerg}`, MARGIN, y);
    y += LINE_HEIGHT;
    doc.text(`Quirúrgico: ${antQx}`, MARGIN, y);
    y += LINE_HEIGHT + 2;
    if (h?.antecedentesFamiliares?.trim() || h?.antecedentesFarmacologicos?.trim() || h?.antecedentesTraumaticos?.trim() || h?.antecedentesGinecoobstetricos?.trim()) {
      if (h.antecedentesFamiliares) { doc.text(`Familiar: ${h.antecedentesFamiliares}`, MARGIN, y); y += LINE_HEIGHT; }
      if (h.antecedentesFarmacologicos) { doc.text(`Farmacológico: ${h.antecedentesFarmacologicos}`, MARGIN, y); y += LINE_HEIGHT; }
      if (h.antecedentesTraumaticos) { doc.text(`Traumático: ${h.antecedentesTraumaticos}`, MARGIN, y); y += LINE_HEIGHT; }
      if (h.antecedentesGinecoobstetricos) { doc.text(`Ginecoobstétrico: ${h.antecedentesGinecoobstetricos}`, MARGIN, y); y += LINE_HEIGHT; }
      y += 2;
    }
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(...BW_DARK);
    doc.text('Hábitos:', MARGIN, y);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(...BW_MID);
    doc.text(`Tabaco: ${h?.habitosTabaco ? 'Sí' : 'No'}  ·  Alcohol: ${h?.habitosAlcohol ? 'Sí' : 'No'}  ·  Sustancias: ${h?.habitosSustancias ? 'Sí' : 'No'}`, MARGIN + 20, y);
    doc.setTextColor(...BW_BLACK);
    y += LINE_HEIGHT;
    if (h?.habitosDetalles?.trim()) {
      const linesHab = doc.splitTextToSize(h.habitosDetalles, contentW - 20);
      linesHab.forEach((line: string) => { doc.text(line, MARGIN + 20, y); y += LINE_HEIGHT; });
      y += 2;
    }
    y += SECTION_GAP;
    y = this.addLineaSeparadoraByn(doc, y);

    if (y > FOOTER_TOP - 55) { doc.addPage(); y = HEADER_H; }
    // ——— PLAN ———
    y = this.addSeccionHospitalaria(doc, 'Plan', y);
    const plan = ultimaConsulta?.planTratamiento || ultimaConsulta?.tratamientoFarmacologico || ultimaConsulta?.recomendaciones || '—';
    const linesPlan = doc.splitTextToSize(plan, contentW);
    doc.setFontSize(9);
    doc.setTextColor(...BW_BLACK);
    linesPlan.forEach((line: string) => { doc.text(line, MARGIN, y); y += LINE_HEIGHT; });
    y += SECTION_GAP;
    y = this.addLineaSeparadoraByn(doc, y);

    if (y > FOOTER_TOP - 40) { doc.addPage(); y = HEADER_H; }
    // ——— ÓRDENES CLÍNICAS ———
    y = this.addSeccionHospitalaria(doc, 'Órdenes clínicas emitidas', y);
    if (ordenes.length > 0) {
      autoTable(doc, {
        startY: y,
        head: [['Nº', 'Servicio / Detalle', 'Cant.', 'Estado', 'Resultado']],
        body: ordenes.map((o) => {
          const detalle = this.resumenOrden(o);
          const resultado = (o.resultado && o.resultado.length > 50) ? o.resultado.substring(0, 50) + '…' : (o.resultado ?? '—');
          return ['#' + o.id, detalle || o.detalle || '—', (o.cantidadPrescrita != null ? String(o.cantidadPrescrita) : '1'), o.estado ?? 'PENDIENTE', resultado];
        }),
        theme: 'plain',
        headStyles: { fillColor: BW_BG, textColor: BW_BLACK, fontStyle: 'bold', fontSize: 8 },
        bodyStyles: { fontSize: 8, textColor: BW_BLACK },
        margin: { top: HEADER_H, left: MARGIN, right: MARGIN, bottom: PAGE_H - FOOTER_TOP + 5 },
        columnStyles: { 0: { cellWidth: 14 }, 1: { cellWidth: 72 }, 2: { cellWidth: 14 }, 3: { cellWidth: 22 }, 4: { cellWidth: 38 } },
      });
      y = this.getY(doc) + SECTION_GAP;
    } else {
      doc.setFontSize(9);
      doc.setTextColor(...BW_MID);
      doc.text('Sin órdenes registradas.', MARGIN, y);
      doc.setTextColor(...BW_BLACK);
      y += LINE_HEIGHT + 4;
    }

    // ——— Bloque profesional y verificación paciente ———
    if (y + 45 > FOOTER_TOP - 15) {
      doc.addPage();
      y = HEADER_H;
      (doc as jsPDF & { lastAutoTable?: { finalY?: number } }).lastAutoTable = { finalY: y };
    }
    y = this.addLineaSeparadoraByn(doc, y);
    doc.setFontSize(9);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(...BW_DARK);
    doc.text('Profesional:', MARGIN, y);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(...BW_BLACK);
    doc.text(branding?.profesionalNombre?.trim() || ultimaConsulta?.profesionalNombre || '—', MARGIN + LABEL_WIDTH, y);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(...BW_DARK);
    doc.text('Especialidad:', MARGIN, y + LINE_HEIGHT);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(...BW_BLACK);
    doc.text(branding?.profesionalRol?.trim() || '—', MARGIN + LABEL_WIDTH, y + LINE_HEIGHT);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(...BW_DARK);
    doc.text('Tarjeta Prof.:', MARGIN, y + 2 * LINE_HEIGHT);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(...BW_BLACK);
    doc.text(branding?.profesionalTarjeta?.trim() || '—', MARGIN + LABEL_WIDTH, y + 2 * LINE_HEIGHT);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(...BW_DARK);
    doc.text('Identificación:', midX, y);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(...BW_BLACK);
    doc.text(identif, midX + 26, y);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(...BW_DARK);
    doc.text('Nombre:', midX, y + LINE_HEIGHT);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(...BW_BLACK);
    doc.text(nombres, midX + 26, y + LINE_HEIGHT);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(...BW_DARK);
    doc.text('Apellido:', midX, y + 2 * LINE_HEIGHT);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(...BW_BLACK);
    doc.text(apellidos, midX + 26, y + 2 * LINE_HEIGHT);

    const totalPages = doc.getNumberOfPages();
    for (let i = 1; i <= totalPages; i++) {
      doc.setPage(i);
      this.addHeaderDocumento(doc, 'Historia clínica', branding, i, totalPages);
      this.addFooterNotaEvolucion(doc, i, totalPages, branding);
    }
    return doc.output('blob');
  }

  /**
   * Genera PDF de Evolución Clínica: documento centrado en las notas de evolución (consultas),
   * con título "Evolución Clínica". Incluye por cada encuentro: fecha, tipo consulta, motivo,
   * enfermedad actual, signos vitales, hallazgos, diagnóstico CIE-10, plan y profesional.
   * Estilo B&amp;N hospitalario, coherente con la HC.
   */
  generarEvolucionClinicaPdf(
    paciente: SesaPdfPaciente,
    consultas: ConsultaDto[],
    branding?: SesaPdfBranding
  ): Blob {
    const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
    const contentW = PAGE_W - 2 * MARGIN;
    let y = HEADER_H;

    const nombres = [paciente.nombres, paciente.apellidos].filter(Boolean).join(' ');
    const docLine = [paciente.tipoDocumento, paciente.documento].filter(Boolean).join(' ');
    const edadStr = this.formatEdadConFecha(paciente.fechaNacimiento);

    doc.setTextColor(...BW_BLACK);
    doc.setFontSize(9);
    doc.setFont('helvetica', 'bold');
    doc.text('Paciente:', MARGIN, y);
    doc.setFont('helvetica', 'normal');
    doc.text(nombres || '—', MARGIN + 22, y);
    doc.setFont('helvetica', 'bold');
    doc.text('Documento:', MARGIN + contentW / 2, y);
    doc.setFont('helvetica', 'normal');
    doc.text(docLine || '—', MARGIN + contentW / 2 + 26, y);
    y += LINE_HEIGHT;
    doc.setFont('helvetica', 'bold');
    doc.text('Edad:', MARGIN, y);
    doc.setFont('helvetica', 'normal');
    doc.text(edadStr, MARGIN + 22, y);
    y += LINE_HEIGHT + SECTION_GAP;
    y = this.addLineaSeparadoraByn(doc, y);

    if (consultas.length === 0) {
      doc.setFontSize(9);
      doc.setTextColor(...BW_MID);
      doc.text('Sin consultas registradas en la evolución clínica.', MARGIN, y);
      doc.setTextColor(...BW_BLACK);
      y += LINE_HEIGHT + SECTION_GAP;
    } else {
      const formatFechaLarga = (f?: string) => {
        if (!f) return '—';
        const d = new Date(f);
        return isNaN(d.getTime()) ? '—' : d.toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' });
      };
      const svTexto = (c: ConsultaDto) => {
        const p: string[] = [];
        if (c.presionArterial) p.push(`TA: ${c.presionArterial}`);
        if (c.frecuenciaCardiaca) p.push(`FC: ${c.frecuenciaCardiaca}`);
        if (c.frecuenciaRespiratoria) p.push(`FR: ${c.frecuenciaRespiratoria}`);
        if (c.temperatura) p.push(`Temp: ${c.temperatura} °C`);
        if (c.saturacionO2) p.push(`SpO₂: ${c.saturacionO2}%`);
        if (c.peso) p.push(`Peso: ${c.peso} kg`);
        if (c.talla) p.push(`Talla: ${c.talla} cm`);
        if (c.imc) p.push(`IMC: ${c.imc}`);
        if (c.dolorEva) p.push(`Dolor EVA: ${c.dolorEva}/10`);
        return p.length ? p.join(' · ') : null;
      };

      consultas.forEach((c, idx) => {
        if (y > FOOTER_TOP - 80) {
          doc.addPage();
          y = HEADER_H;
        }
        const tituloEncuentro = `Encuentro ${idx + 1} — ${formatFechaLarga(c.fechaConsulta)}`;
        y = this.addSeccionHospitalaria(doc, tituloEncuentro, y);

        const lineLabel = (label: string, value: string | undefined) => {
          if (!value?.trim()) return;
          doc.setFontSize(8);
          doc.setFont('helvetica', 'bold');
          doc.setTextColor(...BW_DARK);
          doc.text(`${label}:`, MARGIN, y);
          doc.setFont('helvetica', 'normal');
          doc.setTextColor(...BW_BLACK);
          const lines = doc.splitTextToSize(value.trim(), contentW - LABEL_WIDTH - 4);
          lines.forEach((line: string) => { doc.text(line, MARGIN + LABEL_WIDTH, y); y += LINE_HEIGHT; });
          y += 2;
        };

        lineLabel('Tipo de consulta', c.tipoConsulta);
        lineLabel('Motivo de consulta', c.motivoConsulta);
        lineLabel('Enfermedad actual', c.enfermedadActual);
        const sv = svTexto(c);
        if (sv) lineLabel('Signos vitales', sv);
        lineLabel('Hallazgos examen físico', c.hallazgosExamen);

        if (c.codigoCie10 || c.diagnostico) {
          doc.setFontSize(8);
          doc.setFont('helvetica', 'bold');
          doc.setTextColor(...BW_DARK);
          doc.text('Impresión diagnóstica:', MARGIN, y);
          doc.setFont('helvetica', 'normal');
          doc.setTextColor(...BW_BLACK);
          y += LINE_HEIGHT;
          doc.text(`CIE-10: ${c.codigoCie10 || '—'}  ·  ${c.diagnostico || '—'}`, MARGIN + 4, y);
          y += LINE_HEIGHT + 2;
        }
        lineLabel('Observaciones clínicas', c.observacionesClincias);

        const planParts = [c.planTratamiento, c.tratamientoFarmacologico, c.recomendaciones].filter(Boolean);
        if (planParts.length) lineLabel('Plan / Tratamiento', planParts.join('\n'));

        if (c.profesionalNombre) {
          doc.setFontSize(8);
          doc.setFont('helvetica', 'bold');
          doc.setTextColor(...BW_DARK);
          doc.text('Profesional:', MARGIN, y);
          doc.setFont('helvetica', 'normal');
          doc.setTextColor(...BW_BLACK);
          doc.text(`Dr(a). ${c.profesionalNombre}${c.profesionalTarjetaProfesional ? ' · TP: ' + c.profesionalTarjetaProfesional : ''}`, MARGIN + LABEL_WIDTH, y);
          y += LINE_HEIGHT + 2;
        }

        y = this.addLineaSeparadoraByn(doc, y);
      });
    }

    const totalPages = doc.getNumberOfPages();
    for (let i = 1; i <= totalPages; i++) {
      doc.setPage(i);
      this.addHeaderDocumento(doc, 'Evolución clínica', branding, i, totalPages);
      this.addFooterNotaEvolucion(doc, i, totalPages, branding);
    }
    return doc.output('blob');
  }

  /** Genera PDF con listado de órdenes del paciente. Mismo estilo que HC: cabecera institucional, secciones subrayadas, pie. */
  generarOrdenesPacientePdf(paciente: SesaPdfPaciente, ordenes: OrdenClinicaDto[], branding?: SesaPdfBranding): Blob {
    const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
    const contentW = PAGE_W - 2 * MARGIN;
    let y = HEADER_H;

    const nombre = [paciente.nombres, paciente.apellidos].filter(Boolean).join(' ');
    const docLine = [paciente.tipoDocumento, paciente.documento].filter(Boolean).join(' ');
    doc.setFontSize(9);
    doc.setFont('helvetica', 'bold');
    doc.text('Paciente:', MARGIN, y);
    doc.setFont('helvetica', 'normal');
    doc.text(nombre || '—', MARGIN + 22, y);
    doc.setFont('helvetica', 'bold');
    doc.text('Documento:', MARGIN + contentW / 2, y);
    doc.setFont('helvetica', 'normal');
    doc.text(docLine || '—', MARGIN + contentW / 2 + 24, y);
    y += LINE_HEIGHT + 4;
    y = this.addLineaSeparadora(doc, y);

    y = this.addSeccionSubrayada(doc, 'Resumen de órdenes', y);
    autoTable(doc, {
      startY: y,
      head: [['Nº Orden', 'Tipo', 'Detalle / Posología', 'Estado', 'Resultado']],
      body: ordenes.map((o) => {
        const detalle = this.resumenOrden(o);
        const resultado = (o.resultado && o.resultado.length > 80) ? o.resultado.substring(0, 80) + '…' : (o.resultado ?? '—');
        return ['#' + o.id, o.tipo, detalle, o.estado ?? 'PENDIENTE', resultado];
      }),
      theme: 'striped',
      headStyles: { fillColor: [248, 250, 252], textColor: TEXT_PRIMARY, fontStyle: 'bold', fontSize: 8 },
      bodyStyles: { fontSize: 8 },
      margin: { top: HEADER_H, left: MARGIN, right: MARGIN, bottom: PAGE_H - FOOTER_TOP + 5 },
      columnStyles: { 0: { cellWidth: 18 }, 1: { cellWidth: 26 }, 2: { cellWidth: 62 }, 3: { cellWidth: 22 }, 4: { cellWidth: 42 } },
    });
    let finalY = this.getY(doc) + SECTION_GAP;

    ordenes.forEach((o) => {
      if (finalY > FOOTER_TOP - 20) {
        doc.addPage();
        finalY = HEADER_H;
        (doc as jsPDF & { lastAutoTable?: { finalY?: number } }).lastAutoTable = { finalY: HEADER_H };
      }
      finalY = this.addSeccionSubrayada(doc, `Orden #${o.id} — ${o.tipo}`, finalY);
      doc.setFontSize(9);
      doc.setFont('helvetica', 'normal');
      const linesDet = doc.splitTextToSize(this.resumenOrden(o), contentW);
      linesDet.forEach((line: string) => { doc.text(line, MARGIN, finalY); finalY += LINE_HEIGHT; });
      finalY += 4;
      if (o.resultado) {
        doc.setFont('helvetica', 'bold');
        doc.text('Resultado:', MARGIN, finalY);
        finalY += LINE_HEIGHT;
        doc.setFont('helvetica', 'normal');
        const items = parseResultadoToItems(o.resultado);
        if (items.length) {
          items.forEach((it) => {
            doc.text(`${it.etiqueta}: ${it.valor}`, MARGIN + 4, finalY);
            finalY += LINE_HEIGHT;
          });
        } else {
          const lines = doc.splitTextToSize(o.resultado, contentW);
          lines.forEach((line: string) => { doc.text(line, MARGIN, finalY); finalY += LINE_HEIGHT; });
        }
        finalY += 2;
      }
      if (o.resultadoRegistradoPorNombre) {
        doc.setFontSize(8);
        doc.setTextColor(...TEXT_SECONDARY);
        doc.text(`Registrado por: ${o.resultadoRegistradoPorNombre}${o.resultadoRegistradoPorRol ? ' (' + o.resultadoRegistradoPorRol + ')' : ''}`, MARGIN, finalY);
        doc.setTextColor(...TEXT_PRIMARY);
        finalY += LINE_HEIGHT;
      }
      finalY += SECTION_GAP;
    });

    const totalPages = doc.getNumberOfPages();
    for (let i = 1; i <= totalPages; i++) {
      doc.setPage(i);
      this.addHeaderDocumento(doc, 'Órdenes y resultados', branding, i, totalPages);
      this.addFooterNotaEvolucion(doc, i, totalPages, branding);
    }
    return doc.output('blob');
  }

  /** Genera PDF de una sola orden (laboratorio con/sin resultados, medicamento o procedimiento). */
  generarOrdenIndividualPdf(paciente: SesaPdfPaciente, orden: OrdenClinicaDto, branding?: SesaPdfBranding): Blob {
    const tipo = (orden.tipo || '').toUpperCase();
    if (tipo === 'LABORATORIO') return this.generarOrdenLaboratorioPdf(paciente, orden, branding);
    if (tipo === 'MEDICAMENTO') return this.generarOrdenMedicamentoPdf(paciente, orden, branding);
    return this.generarOrdenProcedimientoPdf(paciente, orden, branding);
  }

  /** PDF orden de laboratorio (con o sin resultados). Mismo estilo institucional que HC. */
  generarOrdenLaboratorioPdf(paciente: SesaPdfPaciente, orden: OrdenClinicaDto, branding?: SesaPdfBranding): Blob {
    const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
    const contentW = PAGE_W - 2 * MARGIN;
    const midX = MARGIN + contentW / 2;
    let y = HEADER_H;

    doc.setFontSize(9);
    doc.setFont('helvetica', 'bold');
    doc.text('Paciente:', MARGIN, y);
    doc.setFont('helvetica', 'normal');
    doc.text([paciente.nombres, paciente.apellidos].filter(Boolean).join(' ') || '—', MARGIN + 22, y);
    doc.setFont('helvetica', 'bold');
    doc.text('Documento:', midX, y);
    doc.setFont('helvetica', 'normal');
    doc.text([paciente.tipoDocumento, paciente.documento].filter(Boolean).join(' ') || '—', midX + 24, y);
    y += LINE_HEIGHT + 4;
    y = this.addLineaSeparadora(doc, y);

    y = this.addSeccionSubrayada(doc, 'Orden de laboratorio', y);
    doc.setFontSize(9);
    doc.setFont('helvetica', 'bold');
    doc.text(`Nº Orden: #${orden.id}`, MARGIN, y);
    y += LINE_HEIGHT + 2;
    doc.setFont('helvetica', 'normal');
    const linesDet = doc.splitTextToSize(orden.detalle || 'Sin detalle.', contentW);
    linesDet.forEach((line: string) => { doc.text(line, MARGIN, y); y += LINE_HEIGHT; });
    y += SECTION_GAP;

    if (orden.resultado) {
      y = this.addSeccionSubrayada(doc, 'Resultados', y);
      const items = parseResultadoToItems(orden.resultado);
      if (items.length) {
        items.forEach((it) => {
          doc.setFont('helvetica', 'bold');
          doc.text(it.etiqueta + ':', MARGIN, y);
          doc.setFont('helvetica', 'normal');
          const vLines = doc.splitTextToSize(it.valor, contentW - 40);
          vLines.forEach((line: string) => { doc.text(line, MARGIN + 38, y); y += LINE_HEIGHT; });
          if (vLines.length <= 1) y += 2;
        });
        y += 2;
      } else {
        const lines = doc.splitTextToSize(orden.resultado, contentW);
        lines.forEach((line: string) => { doc.text(line, MARGIN, y); y += LINE_HEIGHT; });
        y += 2;
      }
      if (orden.resultadoRegistradoPorNombre) {
        doc.setFontSize(8);
        doc.setTextColor(...TEXT_SECONDARY);
        doc.text(`Registrado por: ${orden.resultadoRegistradoPorNombre}${orden.resultadoRegistradoPorRol ? ' · ' + orden.resultadoRegistradoPorRol : ''}`, MARGIN, y);
        doc.setTextColor(...TEXT_PRIMARY);
        y += LINE_HEIGHT;
      }
    } else {
      doc.setFontSize(9);
      doc.setTextColor(...TEXT_SECONDARY);
      doc.text('Resultado pendiente de registrar.', MARGIN, y);
      doc.setTextColor(...TEXT_PRIMARY);
      y += LINE_HEIGHT + 4;
    }

    y = this.addLineaSeparadora(doc, y);
    doc.setFontSize(9);
    doc.setFont('helvetica', 'bold');
    doc.text('Profesional:', MARGIN, y);
    doc.setFont('helvetica', 'normal');
    doc.text(orden.resultadoRegistradoPorNombre || branding?.profesionalNombre || '—', MARGIN + LABEL_WIDTH, y);
    doc.setFont('helvetica', 'bold');
    doc.text('Tarjeta Prof.:', MARGIN, y + LINE_HEIGHT);
    doc.setFont('helvetica', 'normal');
    doc.text(branding?.profesionalTarjeta || '—', MARGIN + LABEL_WIDTH, y + LINE_HEIGHT);

    const totalPages = doc.getNumberOfPages();
    for (let i = 1; i <= totalPages; i++) {
      doc.setPage(i);
      this.addHeaderDocumento(doc, 'Orden de laboratorio', branding, i, totalPages);
      this.addFooterNotaEvolucion(doc, i, totalPages, branding);
    }
    return doc.output('blob');
  }

  /** PDF orden de medicamento (posología completa). Mismo estilo institucional que HC. */
  generarOrdenMedicamentoPdf(paciente: SesaPdfPaciente, orden: OrdenClinicaDto, branding?: SesaPdfBranding): Blob {
    const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
    const contentW = PAGE_W - 2 * MARGIN;
    const midX = MARGIN + contentW / 2;
    let y = HEADER_H;

    doc.setFontSize(9);
    doc.setFont('helvetica', 'bold');
    doc.text('Paciente:', MARGIN, y);
    doc.setFont('helvetica', 'normal');
    doc.text([paciente.nombres, paciente.apellidos].filter(Boolean).join(' ') || '—', MARGIN + 22, y);
    doc.setFont('helvetica', 'bold');
    doc.text('Documento:', midX, y);
    doc.setFont('helvetica', 'normal');
    doc.text([paciente.tipoDocumento, paciente.documento].filter(Boolean).join(' ') || '—', midX + 24, y);
    y += LINE_HEIGHT + 4;
    y = this.addLineaSeparadora(doc, y);

    y = this.addSeccionSubrayada(doc, 'Orden de medicamento', y);
    doc.setFontSize(9);
    doc.setFont('helvetica', 'bold');
    doc.text(`Nº Orden: #${orden.id}`, MARGIN, y);
    y += LINE_HEIGHT + 4;

    const posologia = [
      ['Cantidad a dispensar', orden.cantidadPrescrita != null && orden.unidadMedida ? `${orden.cantidadPrescrita} ${orden.unidadMedida}` : (orden.cantidadPrescrita != null ? String(orden.cantidadPrescrita) : 'No especificado')],
      ['Frecuencia', orden.frecuencia || 'No especificado'],
      ['Duración', orden.duracionDias != null ? `${orden.duracionDias} días` : 'No especificado'],
    ];
    autoTable(doc, {
      startY: y,
      head: [['Concepto', 'Valor']],
      body: posologia,
      theme: 'plain',
      headStyles: { fillColor: [248, 250, 252], textColor: TEXT_PRIMARY, fontStyle: 'bold', fontSize: 9 },
      bodyStyles: { fontSize: 9 },
      margin: { top: HEADER_H, left: MARGIN, right: MARGIN, bottom: PAGE_H - FOOTER_TOP + 5 },
      columnStyles: { 0: { cellWidth: 50 }, 1: { cellWidth: contentW - 50 } },
    });
    y = this.getY(doc) + SECTION_GAP;

    y = this.addSeccionSubrayada(doc, 'Indicaciones y especificaciones', y);
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9);
    const lines = doc.splitTextToSize(orden.detalle || 'Sin indicaciones adicionales.', contentW);
    lines.forEach((line: string) => { doc.text(line, MARGIN, y); y += LINE_HEIGHT; });
    y += SECTION_GAP;

    y = this.addLineaSeparadora(doc, y);
    doc.setFont('helvetica', 'bold');
    doc.text('Profesional:', MARGIN, y);
    doc.setFont('helvetica', 'normal');
    doc.text(branding?.profesionalNombre || '—', MARGIN + LABEL_WIDTH, y);
    doc.setFont('helvetica', 'bold');
    doc.text('Tarjeta Prof.:', MARGIN, y + LINE_HEIGHT);
    doc.setFont('helvetica', 'normal');
    doc.text(branding?.profesionalTarjeta || '—', MARGIN + LABEL_WIDTH, y + LINE_HEIGHT);

    const totalPages = doc.getNumberOfPages();
    for (let i = 1; i <= totalPages; i++) {
      doc.setPage(i);
      this.addHeaderDocumento(doc, 'Orden de medicamento', branding, i, totalPages);
      this.addFooterNotaEvolucion(doc, i, totalPages, branding);
    }
    return doc.output('blob');
  }

  /** PDF orden de procedimiento o imagen. Mismo estilo institucional que HC. */
  generarOrdenProcedimientoPdf(paciente: SesaPdfPaciente, orden: OrdenClinicaDto, branding?: SesaPdfBranding): Blob {
    const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
    const titulo = orden.tipo === 'IMAGEN' ? 'Orden de imagen diagnóstica' : 'Orden de procedimiento';
    const contentW = PAGE_W - 2 * MARGIN;
    const midX = MARGIN + contentW / 2;
    let y = HEADER_H;

    doc.setFontSize(9);
    doc.setFont('helvetica', 'bold');
    doc.text('Paciente:', MARGIN, y);
    doc.setFont('helvetica', 'normal');
    doc.text([paciente.nombres, paciente.apellidos].filter(Boolean).join(' ') || '—', MARGIN + 22, y);
    doc.setFont('helvetica', 'bold');
    doc.text('Documento:', midX, y);
    doc.setFont('helvetica', 'normal');
    doc.text([paciente.tipoDocumento, paciente.documento].filter(Boolean).join(' ') || '—', midX + 24, y);
    y += LINE_HEIGHT + 4;
    y = this.addLineaSeparadora(doc, y);

    y = this.addSeccionSubrayada(doc, titulo, y);
    doc.setFontSize(9);
    doc.setFont('helvetica', 'bold');
    doc.text(`Nº Orden: #${orden.id}`, MARGIN, y);
    y += LINE_HEIGHT + 2;
    doc.setFont('helvetica', 'normal');
    const lines = doc.splitTextToSize(orden.detalle || 'Sin descripción.', contentW);
    lines.forEach((line: string) => { doc.text(line, MARGIN, y); y += LINE_HEIGHT; });
    y += SECTION_GAP;

    y = this.addLineaSeparadora(doc, y);
    doc.setFont('helvetica', 'bold');
    doc.text('Profesional:', MARGIN, y);
    doc.setFont('helvetica', 'normal');
    doc.text(branding?.profesionalNombre || '—', MARGIN + LABEL_WIDTH, y);
    doc.setFont('helvetica', 'bold');
    doc.text('Tarjeta Prof.:', MARGIN, y + LINE_HEIGHT);
    doc.setFont('helvetica', 'normal');
    doc.text(branding?.profesionalTarjeta || '—', MARGIN + LABEL_WIDTH, y + LINE_HEIGHT);

    const totalPages = doc.getNumberOfPages();
    for (let i = 1; i <= totalPages; i++) {
      doc.setPage(i);
      this.addHeaderDocumento(doc, titulo, branding, i, totalPages);
      this.addFooterNotaEvolucion(doc, i, totalPages, branding);
    }
    return doc.output('blob');
  }

  private resumenOrden(o: OrdenClinicaDto): string {
    const parts: string[] = [];
    if (o.cantidadPrescrita != null) {
      parts.push(o.unidadMedida ? `${o.cantidadPrescrita} ${o.unidadMedida}` : String(o.cantidadPrescrita));
    }
    if (o.frecuencia) parts.push(o.frecuencia);
    if (o.duracionDias != null) parts.push(`${o.duracionDias} días`);
    const resumen = parts.length ? parts.join(' · ') + (o.detalle ? ' — ' : '') : '';
    return (resumen + (o.detalle || '')).trim() || '—';
  }

  /** Resumen de alta de urgencias para el paciente (sugerencia 2.7). */
  generarResumenAltaUrgenciasPdf(data: {
    pacienteNombre: string;
    pacienteDocumento: string;
    fechaAlta: string;
    resumen: string;
    instrucciones: string;
  }): void {
    const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
    const contentW = PAGE_W - 2 * MARGIN;
    let y = MARGIN + 10;
    doc.setFontSize(14);
    doc.setFont('helvetica', 'bold');
    doc.text('Resumen de alta — Urgencias', MARGIN, y);
    y += 10;
    doc.setFontSize(10);
    doc.setFont('helvetica', 'normal');
    doc.setDrawColor(...BW_LIGHT);
    doc.line(MARGIN, y, PAGE_W - MARGIN, y);
    y += 8;
    doc.setFont('helvetica', 'bold');
    doc.text('Paciente:', MARGIN, y);
    doc.setFont('helvetica', 'normal');
    doc.text(data.pacienteNombre, MARGIN + 25, y);
    y += LINE_HEIGHT;
    doc.setFont('helvetica', 'bold');
    doc.text('Documento:', MARGIN, y);
    doc.setFont('helvetica', 'normal');
    doc.text(data.pacienteDocumento, MARGIN + 25, y);
    y += LINE_HEIGHT;
    doc.setFont('helvetica', 'bold');
    doc.text('Fecha de alta:', MARGIN, y);
    doc.setFont('helvetica', 'normal');
    doc.text(data.fechaAlta, MARGIN + 28, y);
    y += 10;
    doc.setFont('helvetica', 'bold');
    doc.text('Resumen de la atención', MARGIN, y);
    y += LINE_HEIGHT + 2;
    doc.setFont('helvetica', 'normal');
    const linesResumen = doc.splitTextToSize(data.resumen, contentW);
    linesResumen.forEach((line: string) => { doc.text(line, MARGIN, y); y += LINE_HEIGHT; });
    y += SECTION_GAP;
    doc.setFont('helvetica', 'bold');
    doc.text('Instrucciones para el paciente', MARGIN, y);
    y += LINE_HEIGHT + 2;
    doc.setFont('helvetica', 'normal');
    const linesInst = doc.splitTextToSize(data.instrucciones, contentW);
    linesInst.forEach((line: string) => { doc.text(line, MARGIN, y); y += LINE_HEIGHT; });
    doc.setFontSize(8);
    doc.setTextColor(...BW_MID);
    doc.text('Documento generado por SESA — Urgencias. Res. 5596/2015.', MARGIN, FOOTER_TOP);
    const blob = doc.output('blob');
    const name = `resumen-alta-urgencias-${(data.pacienteDocumento || 'paciente').replace(/\s/g, '-')}.pdf`;
    this.triggerDownload(blob, name);
  }

  /**
   * Resumen de alta / referencia desde consulta (Res. 1995/1999 — continuidad).
   * Retorna Blob para descarga o impresión.
   */
  generarResumenAltaConsultaPdf(data: {
    pacienteNombre: string;
    pacienteDocumento: string;
    fechaConsulta: string;
    profesionalNombre?: string;
    motivoConsulta?: string;
    diagnostico?: string;
    codigoCie10?: string;
    planTratamiento?: string;
    tratamientoFarmacologico?: string;
    recomendaciones?: string;
  }): Blob {
    const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
    const contentW = PAGE_W - 2 * MARGIN;
    let y = MARGIN + 10;
    doc.setFontSize(14);
    doc.setFont('helvetica', 'bold');
    doc.text('Resumen de alta / Referencia — Consulta', MARGIN, y);
    y += 10;
    doc.setFontSize(10);
    doc.setFont('helvetica', 'normal');
    doc.setDrawColor(...BW_LIGHT);
    doc.line(MARGIN, y, PAGE_W - MARGIN, y);
    y += 8;
    doc.setFont('helvetica', 'bold');
    doc.text('Paciente:', MARGIN, y);
    doc.setFont('helvetica', 'normal');
    doc.text(data.pacienteNombre, MARGIN + 25, y);
    y += LINE_HEIGHT;
    doc.setFont('helvetica', 'bold');
    doc.text('Documento:', MARGIN, y);
    doc.setFont('helvetica', 'normal');
    doc.text(data.pacienteDocumento, MARGIN + 28, y);
    y += LINE_HEIGHT;
    doc.setFont('helvetica', 'bold');
    doc.text('Fecha de atención:', MARGIN, y);
    doc.setFont('helvetica', 'normal');
    doc.text(data.fechaConsulta, MARGIN + 35, y);
    y += LINE_HEIGHT;
    if (data.profesionalNombre) {
      doc.setFont('helvetica', 'bold');
      doc.text('Profesional:', MARGIN, y);
      doc.setFont('helvetica', 'normal');
      doc.text(data.profesionalNombre, MARGIN + 28, y);
      y += LINE_HEIGHT;
    }
    y += SECTION_GAP;
    const sections: [string, string][] = [];
    if (data.motivoConsulta) sections.push(['Motivo de consulta', data.motivoConsulta]);
    if (data.diagnostico) sections.push(['Diagnóstico', data.diagnostico]);
    if (data.codigoCie10) sections.push(['CIE-10', data.codigoCie10]);
    if (data.planTratamiento) sections.push(['Plan de tratamiento', data.planTratamiento]);
    if (data.tratamientoFarmacologico) sections.push(['Tratamiento farmacológico', data.tratamientoFarmacologico]);
    if (data.recomendaciones) sections.push(['Recomendaciones', data.recomendaciones]);
    for (const [label, text] of sections) {
      doc.setFont('helvetica', 'bold');
      doc.text(label + ':', MARGIN, y);
      y += LINE_HEIGHT + 2;
      doc.setFont('helvetica', 'normal');
      const lines = doc.splitTextToSize(text, contentW);
      lines.forEach((line: string) => { doc.text(line, MARGIN, y); y += LINE_HEIGHT; });
      y += SECTION_GAP;
    }
    doc.setFontSize(8);
    doc.setTextColor(...BW_MID);
    doc.text('Documento generado por SESA — Res. 1995/1999. Continuidad del cuidado.', MARGIN, FOOTER_TOP);
    return doc.output('blob');
  }

  /** Descarga un Blob como archivo PDF. */
  triggerDownload(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }

  /** Abre el PDF en nueva ventana y dispara impresión. */
  openForPrint(blob: Blob): void {
    const url = URL.createObjectURL(blob);
    const w = window.open(url, '_blank', 'noopener,noreferrer');
    if (w) {
      w.onload = () => {
        w.print();
        w.onafterprint = () => {
          w.close();
          URL.revokeObjectURL(url);
        };
      };
    } else {
      URL.revokeObjectURL(url);
    }
  }
}
