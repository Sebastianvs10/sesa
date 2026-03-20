/**
 * Agenda Export Service — Exportación premium a Excel (3 hojas) y PDF (header corporativo, KPIs, tablas).
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable } from '@angular/core';
import type { jsPDF as jsPDFType } from 'jspdf';
import {
  Turno, Profesional, ResumenProfesional,
  TURNO_CONFIG, SERVICIO_CONFIG,
  EstadoProgramacion, TipoTurno, ServicioClinico,
} from './agenda.models';

export interface AgendaExportPayload {
  turnos:          Turno[];
  profesionales:   Profesional[];
  resumenes:       ResumenProfesional[];
  dashStats: {
    totalProfesionales: number;
    totalTurnos:        number;
    totalHoras:         number;
    conflictos:         number;
    advertencias:       number;
    horasNocturnas:     number;
    horasFestivos:      number;
  };
  filtroServicio:  string;
  filtroTipo:      string;
  estadoMes:       EstadoProgramacion;
  mesLabel:        string;
  generadoPor:     string;
}

@Injectable({ providedIn: 'root' })
export class AgendaExportService {

  /* ═══════════════════════════════════════════════════
     EXCEL — 3 hojas: Turnos · Resumen · KPIs y filtros
     ═══════════════════════════════════════════════════ */

  async exportarExcel(data: AgendaExportPayload): Promise<void> {
    const XLSX = await import('xlsx');
    const wb   = XLSX.utils.book_new();

    /* ── Hoja 1: Turnos del Mes ─────────────────────────────────── */
    const hdrs1 = [
      'N°', 'Profesional', 'Tipo Personal', 'Registro',
      'Servicio Clínico', 'Tipo Turno', 'Duración (h)',
      'Fecha', 'Hora Inicio', 'Hora Fin',
      'Estado', 'Festivo', 'Alerta', 'Notas',
    ];

    const rows1 = data.turnos.map((t, i) => {
      const p   = data.profesionales.find(x => x.id === t.profesionalId);
      const cfg = TURNO_CONFIG[t.tipo];
      const svc = SERVICIO_CONFIG[t.servicio];
      const fi  = new Date(t.fechaInicio);
      const ff  = new Date(t.fechaFin);
      return [
        i + 1,
        p ? `${p.nombre} ${p.apellido}` : 'N/D',
        p ? this.labelTipo(p.tipo) : '',
        p?.registro ?? '',
        svc.label,
        cfg.label,
        t.duracionHoras,
        this.fmtFecha(fi),
        this.fmtHora(fi),
        this.fmtHora(ff),
        this.labelEstado(t.estado),
        t.esFestivo ? 'Sí' : 'No',
        t.alerta,
        t.notas ?? '',
      ];
    });

    const ws1 = XLSX.utils.aoa_to_sheet([hdrs1, ...rows1]);
    ws1['!cols'] = [
      { wch: 5 }, { wch: 30 }, { wch: 22 }, { wch: 14 },
      { wch: 22 }, { wch: 18 }, { wch: 13 },
      { wch: 14 }, { wch: 12 }, { wch: 12 },
      { wch: 12 }, { wch: 10 }, { wch: 14 }, { wch: 32 },
    ];
    XLSX.utils.book_append_sheet(wb, ws1, 'Turnos del Mes');

    /* ── Hoja 2: Resumen por Profesional ────────────────────────── */
    const hdrs2 = [
      'Profesional', 'Tipo Personal', 'Registro',
      'N° Turnos', 'Horas Totales', 'Horas Nocturnas', 'Horas Festivos',
      '% Carga (máx. 192 h)', 'Estado Alerta',
    ];

    const rows2 = data.resumenes.map(r => [
      `${r.profesional.nombre} ${r.profesional.apellido}`,
      this.labelTipo(r.profesional.tipo),
      r.profesional.registro,
      r.turnosCount,
      r.horasTotales,
      r.horasNocturnas,
      r.horasFestivos,
      `${Math.round((r.horasTotales / 192) * 100)}%`,
      r.alerta,
    ]);

    const ws2 = XLSX.utils.aoa_to_sheet([hdrs2, ...rows2]);
    ws2['!cols'] = [
      { wch: 30 }, { wch: 22 }, { wch: 14 },
      { wch: 12 }, { wch: 14 }, { wch: 16 }, { wch: 16 },
      { wch: 22 }, { wch: 16 },
    ];
    XLSX.utils.book_append_sheet(wb, ws2, 'Resumen Profesionales');

    /* ── Hoja 3: KPIs y Filtros ─────────────────────────────────── */
    const s     = data.dashStats;
    const rows3 = [
      ['INDICADOR', 'VALOR'],
      ['Profesionales activos',   s.totalProfesionales],
      ['Turnos asignados',        s.totalTurnos],
      ['Horas totales',           s.totalHoras],
      ['Horas nocturnas',         s.horasNocturnas],
      ['Horas en festivos',       s.horasFestivos],
      ['Conflictos detectados',   s.conflictos],
      ['Advertencias',            s.advertencias],
      ['', ''],
      ['FILTROS APLICADOS',       ''],
      ['Servicio', data.filtroServicio === 'TODOS'
        ? 'Todos'
        : (SERVICIO_CONFIG[data.filtroServicio as ServicioClinico]?.label ?? data.filtroServicio)],
      ['Tipo turno', data.filtroTipo === 'TODOS'
        ? 'Todos'
        : (TURNO_CONFIG[data.filtroTipo as TipoTurno]?.label ?? data.filtroTipo)],
      ['Estado programación',     this.labelEstadoProg(data.estadoMes)],
      ['', ''],
      ['Generado el',             new Date().toLocaleString('es-CO')],
      ['Generado por',            data.generadoPor],
    ];

    const ws3 = XLSX.utils.aoa_to_sheet(rows3);
    ws3['!cols'] = [{ wch: 26 }, { wch: 30 }];
    XLSX.utils.book_append_sheet(wb, ws3, 'KPIs y Filtros');

    XLSX.writeFile(wb, `SESA_Agenda_${data.mesLabel.replace(' ', '_')}_${Date.now()}.xlsx`);
  }

  /* ═══════════════════════════════════════════════════
     PDF — Header SESA · KPIs · Tabla turnos · Resumen
     ═══════════════════════════════════════════════════ */

  async exportarPDF(data: AgendaExportPayload): Promise<void> {
    const { jsPDF }              = await import('jspdf');
    const autoTableMod           = await import('jspdf-autotable');
    const autoTable: (doc: jsPDFType, options: Record<string, unknown>) => void
                                 = autoTableMod.default ?? (autoTableMod as unknown as { default: unknown }).default ?? autoTableMod;

    const doc = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });
    const PW  = doc.internal.pageSize.getWidth();
    const PH  = doc.internal.pageSize.getHeight();

    /* ── Funciones reutilizables por página ──────────────────────── */

    const drawHeader = (): void => {
      // Franja oscura superior
      doc.setFillColor(15, 23, 42);
      doc.rect(0, 0, PW, 16, 'F');
      // Acento teal
      doc.setFillColor(43, 176, 166);
      doc.rect(0, 14.5, PW, 2, 'F');
      // Nombre
      doc.setFontSize(12);
      doc.setFont('helvetica', 'bold');
      doc.setTextColor(255, 255, 255);
      doc.text('SESA', 10, 9.5);
      doc.setFontSize(7);
      doc.setFont('helvetica', 'normal');
      doc.setTextColor(148, 163, 184);
      doc.text('Sistema Electrónico de Salud', 10, 13.5);
      // Título derecha
      doc.setFontSize(9);
      doc.setFont('helvetica', 'bold');
      doc.setTextColor(255, 255, 255);
      doc.text('PROGRAMACIÓN DE TURNOS — IPS NIVEL II', PW - 10, 8.5, { align: 'right' });
      doc.setFontSize(7.5);
      doc.setFont('helvetica', 'normal');
      doc.setTextColor(148, 163, 184);
      doc.text(data.mesLabel.toUpperCase(), PW - 10, 13.5, { align: 'right' });
    };

    const drawFooter = (pageNum: number): void => {
      doc.setDrawColor(226, 232, 240);
      doc.line(8, PH - 9, PW - 8, PH - 9);
      doc.setFontSize(6.5);
      doc.setFont('helvetica', 'normal');
      doc.setTextColor(148, 163, 184);
      const fecha = new Date().toLocaleString('es-CO', { dateStyle: 'short', timeStyle: 'short' });
      doc.text(
        `SESA — Sistema Electrónico de Salud · ${data.mesLabel} · ${fecha} · Generado por: ${data.generadoPor}`,
        10, PH - 5
      );
      doc.text(`Pág. ${pageNum}`, PW - 10, PH - 5, { align: 'right' });
    };

    /* ── Página 1: metadata + KPIs + tabla de turnos ──────────────── */
    drawHeader();
    let y = 20;

    // Banda de metadata/filtros
    doc.setFillColor(248, 250, 252);
    doc.roundedRect(8, y, PW - 16, 13, 1.5, 1.5, 'F');
    doc.setDrawColor(226, 232, 240);
    doc.roundedRect(8, y, PW - 16, 13, 1.5, 1.5, 'S');

    const metaItems: [string, string][] = [
      ['Periodo',   data.mesLabel],
      ['Estado',    this.labelEstadoProg(data.estadoMes)],
      ['Servicio',  data.filtroServicio === 'TODOS' ? 'Todos'
                    : (SERVICIO_CONFIG[data.filtroServicio as ServicioClinico]?.label ?? data.filtroServicio)],
      ['Tipo turno', data.filtroTipo === 'TODOS' ? 'Todos'
                    : (TURNO_CONFIG[data.filtroTipo as TipoTurno]?.label ?? data.filtroTipo)],
      ['Registros', `${data.turnos.length} turnos`],
      ['Por',       data.generadoPor],
    ];
    const metaColW = (PW - 28) / metaItems.length;
    metaItems.forEach(([lbl, val], i) => {
      const x = 12 + i * metaColW;
      doc.setFontSize(6);
      doc.setFont('helvetica', 'bold');
      doc.setTextColor(100, 116, 139);
      doc.text(lbl.toUpperCase(), x, y + 4.5);
      doc.setFontSize(7.5);
      doc.setFont('helvetica', 'bold');
      doc.setTextColor(15, 23, 42);
      doc.text(String(val), x, y + 10);
    });
    y += 17;

    // KPIs
    this.drawKpis(doc as unknown as jsPDFType, data.dashStats, y, PW);
    y += 29;

    // Título sección
    doc.setFontSize(8.5);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(15, 23, 42);
    doc.text('Detalle de turnos del mes', 10, y);
    doc.setFontSize(7);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(100, 116, 139);
    doc.text(`${data.turnos.length} registros`, PW - 10, y, { align: 'right' });
    y += 5;

    // Filas tabla turnos
    const turnRows = data.turnos.map((t, i) => {
      const p   = data.profesionales.find(x => x.id === t.profesionalId);
      const fi  = new Date(t.fechaInicio);
      const ff  = new Date(t.fechaFin);
      return [
        String(i + 1),
        p ? `${p.nombre.split(' ')[0]} ${p.apellido.split(' ')[0]}` : 'N/D',
        SERVICIO_CONFIG[t.servicio].label,
        TURNO_CONFIG[t.tipo].label,
        `${t.duracionHoras}h`,
        this.fmtFecha(fi),
        `${this.fmtHora(fi)} – ${this.fmtHora(ff)}`,
        this.labelEstado(t.estado),
        t.esFestivo ? 'SÍ' : '',
        t.alerta !== 'OK' ? t.alerta : '✓',
      ];
    });

    let tablePageOffset = 0;
    autoTable(doc as unknown as jsPDFType, {
      startY: y,
      head: [['#', 'Profesional', 'Servicio', 'Tipo Turno', 'Dur.', 'Fecha', 'Horario', 'Estado', 'Festivo', 'Alerta']],
      body: turnRows,
      margin: { left: 8, right: 8 },
      styles: {
        fontSize: 7.5,
        cellPadding: { top: 2.5, right: 3, bottom: 2.5, left: 3 },
        font: 'helvetica',
        lineColor: [226, 232, 240],
        lineWidth: 0.2,
      },
      headStyles: {
        fillColor: [15, 23, 42],
        textColor: [255, 255, 255],
        fontStyle: 'bold',
        fontSize: 7.5,
      },
      alternateRowStyles: { fillColor: [248, 250, 252] },
      columnStyles: {
        0: { halign: 'center', cellWidth: 8  },
        1: { cellWidth: 34 },
        2: { cellWidth: 26 },
        3: { cellWidth: 24 },
        4: { halign: 'center', cellWidth: 12 },
        5: { halign: 'center', cellWidth: 22 },
        6: { halign: 'center', cellWidth: 26 },
        7: { halign: 'center', cellWidth: 20 },
        8: { halign: 'center', cellWidth: 14 },
        9: { halign: 'center', cellWidth: 15 },
      },
      didParseCell: (h: Record<string, unknown>) => {
        const section = h['section'];
        const col     = (h['column'] as { index: number }).index;
        const cell    = h['cell'] as { text: string[]; styles: Record<string, unknown> };
        if (section !== 'body') return;
        if (col === 7) {
          const v = cell.text[0];
          if (v === 'Aprobado')    { cell.styles['textColor'] = [34, 197, 94];  cell.styles['fontStyle'] = 'bold'; }
          else if (v === 'Cerrado') { cell.styles['textColor'] = [239, 68, 68]; cell.styles['fontStyle'] = 'bold'; }
          else                      { cell.styles['textColor'] = [100, 116, 139]; }
        }
        if (col === 9) {
          const v = cell.text[0];
          if (v === 'CONFLICTO')   { cell.styles['textColor'] = [239, 68, 68];  cell.styles['fontStyle'] = 'bold'; }
          else if (v === 'ADVERTENCIA') { cell.styles['textColor'] = [245, 158, 11]; cell.styles['fontStyle'] = 'bold'; }
          else                      { cell.styles['textColor'] = [34, 197, 94]; }
        }
      },
      didDrawPage: (h: Record<string, unknown>) => {
        const pn = (h['pageNumber'] as number);
        if (pn > 1) drawHeader();
        drawFooter(pn);
        tablePageOffset = pn;
      },
    } as unknown as Record<string, unknown>);

    /* ── Página: resumen por profesional ─────────────────────────── */
    doc.addPage();
    drawHeader();
    drawFooter(tablePageOffset + 1);
    y = 20;

    doc.setFontSize(8.5);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(15, 23, 42);
    doc.text('Resumen por profesional', 10, y);
    y += 5;

    const resRows = data.resumenes.map(r => [
      `${r.profesional.nombre} ${r.profesional.apellido}`,
      this.labelTipo(r.profesional.tipo),
      r.profesional.registro,
      String(r.turnosCount),
      `${r.horasTotales}`,
      `${r.horasNocturnas}`,
      `${r.horasFestivos}`,
      `${Math.round((r.horasTotales / 192) * 100)}%`,
      r.alerta,
    ]);

    autoTable(doc as unknown as jsPDFType, {
      startY: y,
      head: [['Profesional', 'Tipo', 'Registro', 'Turnos', 'H. Tot.', 'H. Noc.', 'H. Fest.', '% Carga', 'Alerta']],
      body: resRows,
      margin: { left: 8, right: 8 },
      styles: {
        fontSize: 8,
        cellPadding: { top: 3, right: 4, bottom: 3, left: 4 },
        font: 'helvetica',
        lineColor: [226, 232, 240],
        lineWidth: 0.2,
      },
      headStyles: {
        fillColor: [15, 23, 42],
        textColor: [255, 255, 255],
        fontStyle: 'bold',
        fontSize: 8,
      },
      alternateRowStyles: { fillColor: [248, 250, 252] },
      didParseCell: (h: Record<string, unknown>) => {
        const section = h['section'];
        const col     = (h['column'] as { index: number }).index;
        const cell    = h['cell'] as { text: string[]; styles: Record<string, unknown> };
        if (section !== 'body') return;
        if (col === 8) {
          const v = cell.text[0];
          if (v === 'CONFLICTO')       { cell.styles['textColor'] = [239, 68, 68];  cell.styles['fontStyle'] = 'bold'; }
          else if (v === 'ADVERTENCIA') { cell.styles['textColor'] = [245, 158, 11]; cell.styles['fontStyle'] = 'bold'; }
          else                          { cell.styles['textColor'] = [34, 197, 94];  cell.styles['fontStyle'] = 'bold'; }
        }
        if (col === 7) {
          const pct = parseInt(cell.text[0], 10);
          cell.styles['fontStyle'] = 'bold';
          if      (pct >= 95) cell.styles['textColor'] = [239, 68, 68];
          else if (pct >= 80) cell.styles['textColor'] = [245, 158, 11];
          else                cell.styles['textColor'] = [34, 197, 94];
        }
      },
      didDrawPage: (h: Record<string, unknown>) => {
        const pn = (h['pageNumber'] as number);
        if (pn > 1) { drawHeader(); drawFooter(tablePageOffset + pn); }
      },
    } as unknown as Record<string, unknown>);

    doc.save(`SESA_Agenda_${data.mesLabel.replace(' ', '_')}_${Date.now()}.pdf`);
  }

  /* ── KPI cards dibujadas manualmente ─────────────────────────── */
  private drawKpis(doc: jsPDFType, stats: AgendaExportPayload['dashStats'], startY: number, pageWidth: number): void {
    const kpis: { label: string; val: string; r: number; g: number; b: number }[] = [
      { label: 'Profesionales', val: String(stats.totalProfesionales), r: 59,  g: 130, b: 246 },
      { label: 'Turnos',        val: String(stats.totalTurnos),        r: 34,  g: 197, b: 94  },
      { label: 'Horas Totales', val: `${stats.totalHoras}h`,           r: 202, g: 138, b: 4   },
      { label: 'H. Nocturnas',  val: `${stats.horasNocturnas}h`,       r: 99,  g: 102, b: 241 },
      { label: 'H. Festivos',   val: `${stats.horasFestivos}h`,        r: 249, g: 115, b: 22  },
      { label: 'Conflictos',    val: String(stats.conflictos),          r: 239, g: 68,  b: 68  },
      { label: 'Advertencias',  val: String(stats.advertencias),        r: 234, g: 179, b: 8   },
    ];
    const cardW = (pageWidth - 16 - 6 * 3) / 7;
    const cardH = 22;

    kpis.forEach((k, i) => {
      const x = 8 + i * (cardW + 3);
      // Fondo
      doc.setFillColor(248, 250, 252);
      doc.roundedRect(x, startY, cardW, cardH, 2, 2, 'F');
      doc.setDrawColor(226, 232, 240);
      doc.roundedRect(x, startY, cardW, cardH, 2, 2, 'S');
      // Borde superior coloreado
      doc.setFillColor(k.r, k.g, k.b);
      doc.rect(x, startY, cardW, 2.5, 'F');
      // Valor
      doc.setFontSize(13);
      doc.setFont('helvetica', 'bold');
      doc.setTextColor(k.r, k.g, k.b);
      doc.text(k.val, x + cardW / 2, startY + 13.5, { align: 'center' });
      // Label
      doc.setFontSize(5.5);
      doc.setFont('helvetica', 'normal');
      doc.setTextColor(100, 116, 139);
      doc.text(k.label.toUpperCase(), x + cardW / 2, startY + 19, { align: 'center' });
    });
  }

  /* ── Helpers de formato ──────────────────────────────────────── */

  private fmtFecha(d: Date): string {
    return d.toLocaleDateString('es-CO', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  private fmtHora(d: Date): string {
    return d.toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit', hour12: false });
  }

  private labelTipo(tipo: string): string {
    const MAP: Record<string, string> = {
      MEDICO: 'Médico', ENFERMERO: 'Enfermero/a',
      AUXILIAR_ENFERMERIA: 'Aux. Enfermería', ODONTOLOGO: 'Odontólogo',
      RECEPCIONISTA: 'Recepcionista', OTRO: 'Otro',
    };
    return MAP[tipo] ?? tipo;
  }

  private labelEstado(e: string): string {
    return ({ BORRADOR: 'Borrador', APROBADO: 'Aprobado', CERRADO: 'Cerrado' } as Record<string, string>)[e] ?? e;
  }

  private labelEstadoProg(e: string): string {
    return ({
      BORRADOR: 'Borrador', EN_REVISION: 'En revisión', APROBADO: 'Aprobado', CERRADO: 'Cerrado',
    } as Record<string, string>)[e] ?? e;
  }
}
