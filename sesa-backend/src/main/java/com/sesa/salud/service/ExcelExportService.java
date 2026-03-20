/**
 * Servicio global de exportación a Excel (.xlsx)
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.FacturaDto;
import com.sesa.salud.dto.PacienteDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelExportService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DTTIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /* ── Pacientes ──────────────────────────────────────────────────── */

    public byte[] exportarPacientes(List<PacienteDto> pacientes, String empresaNombre) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Pacientes");
            ExcelStyles styles = new ExcelStyles(wb);

            int rowNum = 0;

            // Título
            rowNum = writeTitulo(sheet, wb, styles, empresaNombre,
                    "Listado de Pacientes", rowNum, 9);

            // Encabezados
            String[] headers = {"ID", "Tipo Doc.", "Documento", "Nombres", "Apellidos",
                    "Fecha Nac.", "Sexo", "Teléfono", "Email", "Dirección", "EPS", "Activo"};
            // Ajustamos el ancho de columnas
            int[] colWidths = {8, 12, 18, 25, 25, 14, 10, 16, 28, 30, 20, 8};
            writeHeaders(sheet, styles, rowNum++, headers, colWidths);

            // Datos
            CellStyle dataStyle   = styles.data();
            CellStyle numberStyle = styles.number();

            for (PacienteDto p : pacientes) {
                Row row = sheet.createRow(rowNum++);
                createCell(row, 0, p.getId() != null ? p.getId().doubleValue() : 0, numberStyle);
                createCell(row, 1, p.getTipoDocumento(), dataStyle);
                createCell(row, 2, p.getDocumento(), dataStyle);
                createCell(row, 3, p.getNombres(), dataStyle);
                createCell(row, 4, p.getApellidos(), dataStyle);
                createCell(row, 5, p.getFechaNacimiento() != null
                        ? DT_FMT.format(p.getFechaNacimiento()) : "", dataStyle);
                createCell(row, 6, p.getSexo(), dataStyle);
                createCell(row, 7, p.getTelefono(), dataStyle);
                createCell(row, 8, p.getEmail(), dataStyle);
                createCell(row, 9, p.getDireccion(), dataStyle);
                createCell(row, 10, p.getEpsNombre(), dataStyle);
                createCell(row, 11, Boolean.TRUE.equals(p.getActivo()) ? "Sí" : "No", dataStyle);
            }

            writeTotal(sheet, styles, rowNum, 9, pacientes.size(), "pacientes");
            return toBytes(wb);
        } catch (Exception e) {
            throw new RuntimeException("Error exportando pacientes: " + e.getMessage(), e);
        }
    }

    /* ── Facturas ───────────────────────────────────────────────────── */

    public byte[] exportarFacturas(List<FacturaDto> facturas, String empresaNombre) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Facturas");
            ExcelStyles styles = new ExcelStyles(wb);

            int rowNum = 0;
            rowNum = writeTitulo(sheet, wb, styles, empresaNombre,
                    "Listado de Facturas", rowNum, 8);

            String[] headers = {"ID", "N° Factura", "Paciente", "Documento", "EPS",
                    "Valor Total", "Estado", "Descripción", "Fecha"};
            int[] colWidths = {8, 18, 28, 16, 20, 16, 14, 35, 14};
            writeHeaders(sheet, styles, rowNum++, headers, colWidths);

            CellStyle dataStyle   = styles.data();
            CellStyle numberStyle = styles.number();
            CellStyle moneyStyle  = styles.money();

            for (FacturaDto f : facturas) {
                Row row = sheet.createRow(rowNum++);
                createCell(row, 0, f.getId() != null ? f.getId().doubleValue() : 0, numberStyle);
                createCell(row, 1, f.getNumeroFactura(), dataStyle);
                createCell(row, 2, f.getPacienteNombre(), dataStyle);
                createCell(row, 3, (f.getPacienteTipoDocumento() != null ? f.getPacienteTipoDocumento() + " " : "")
                        + (f.getPacienteDocumento() != null ? f.getPacienteDocumento() : ""), dataStyle);
                createCell(row, 4, f.getEpsNombre(), dataStyle);
                createCell(row, 5, f.getValorTotal() != null ? f.getValorTotal().doubleValue() : 0, moneyStyle);
                createCell(row, 6, f.getEstado(), dataStyle);
                createCell(row, 7, f.getDescripcion(), dataStyle);
                createCell(row, 8, f.getFechaFactura() != null
                        ? DTTIME_FMT.format(f.getFechaFactura().atZone(ZoneOffset.UTC)) : "", dataStyle);
            }

            writeTotal(sheet, styles, rowNum, 8, facturas.size(), "facturas");
            return toBytes(wb);
        } catch (Exception e) {
            throw new RuntimeException("Error exportando facturas: " + e.getMessage(), e);
        }
    }

    /* ── Helpers de escritura ────────────────────────────────────────── */

    private int writeTitulo(Sheet sheet, Workbook wb, ExcelStyles styles,
                             String empresa, String titulo, int rowNum, int lastCol) {
        // Fila empresa
        Row rEmp = sheet.createRow(rowNum++);
        rEmp.setHeight((short) 500);
        Cell cEmp = rEmp.createCell(0);
        cEmp.setCellValue(empresa);
        cEmp.setCellStyle(styles.empresa());
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, lastCol));

        // Fila título
        Row rTit = sheet.createRow(rowNum++);
        rTit.setHeight((short) 600);
        Cell cTit = rTit.createCell(0);
        cTit.setCellValue(titulo);
        cTit.setCellStyle(styles.titulo());
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, lastCol));

        // Fila separadora
        sheet.createRow(rowNum++);
        return rowNum;
    }

    private void writeHeaders(Sheet sheet, ExcelStyles styles, int rowNum,
                               String[] headers, int[] colWidths) {
        Row row = sheet.createRow(rowNum);
        row.setHeight((short) 500);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.header());
            if (i < colWidths.length) {
                sheet.setColumnWidth(i, colWidths[i] * 256);
            }
        }
    }

    private void writeTotal(Sheet sheet, ExcelStyles styles, int rowNum,
                              int lastCol, int count, String entidad) {
        sheet.createRow(rowNum); // espacio
        Row rTotal = sheet.createRow(rowNum + 1);
        Cell cTotal = rTotal.createCell(0);
        cTotal.setCellValue("Total: " + count + " " + entidad);
        cTotal.setCellStyle(styles.total());
        sheet.addMergedRegion(new CellRangeAddress(rowNum + 1, rowNum + 1, 0, lastCol));
    }

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void createCell(Row row, int col, double value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private byte[] toBytes(XSSFWorkbook wb) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            wb.write(out);
            return out.toByteArray();
        }
    }

    /* ── Estilos reutilizables ──────────────────────────────────────── */

    private static class ExcelStyles {
        private final Workbook wb;
        private final Font boldWhite;
        private final Font boldBrand;
        private final Font regular;
        private final short BRAND_COLOR = IndexedColors.DARK_BLUE.getIndex();

        ExcelStyles(Workbook wb) {
            this.wb = wb;

            boldWhite = wb.createFont();
            boldWhite.setBold(true);
            boldWhite.setColor(IndexedColors.WHITE.getIndex());
            boldWhite.setFontHeightInPoints((short) 11);

            boldBrand = wb.createFont();
            boldBrand.setBold(true);
            boldBrand.setColor(BRAND_COLOR);
            boldBrand.setFontHeightInPoints((short) 14);

            regular = wb.createFont();
            regular.setFontHeightInPoints((short) 10);
        }

        CellStyle empresa() {
            CellStyle s = wb.createCellStyle();
            Font f = wb.createFont();
            f.setBold(true);
            f.setFontHeightInPoints((short) 10);
            f.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            s.setFont(f);
            s.setAlignment(HorizontalAlignment.LEFT);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            return s;
        }

        CellStyle titulo() {
            CellStyle s = wb.createCellStyle();
            s.setFont(boldBrand);
            s.setAlignment(HorizontalAlignment.LEFT);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            s.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            return s;
        }

        CellStyle header() {
            CellStyle s = wb.createCellStyle();
            s.setFont(boldWhite);
            s.setFillForegroundColor(BRAND_COLOR);
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            s.setAlignment(HorizontalAlignment.CENTER);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            s.setBorderBottom(BorderStyle.THIN);
            s.setBorderTop(BorderStyle.THIN);
            s.setBorderLeft(BorderStyle.THIN);
            s.setBorderRight(BorderStyle.THIN);
            s.setBottomBorderColor(IndexedColors.WHITE.getIndex());
            s.setWrapText(true);
            return s;
        }

        CellStyle data() {
            CellStyle s = wb.createCellStyle();
            s.setFont(regular);
            s.setBorderBottom(BorderStyle.THIN);
            s.setBorderLeft(BorderStyle.THIN);
            s.setBorderRight(BorderStyle.THIN);
            s.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            s.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            s.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            s.setWrapText(false);
            return s;
        }

        CellStyle number() {
            CellStyle s = data();
            s.setAlignment(HorizontalAlignment.CENTER);
            return s;
        }

        CellStyle money() {
            CellStyle s = data();
            DataFormat fmt = wb.createDataFormat();
            s.setDataFormat(fmt.getFormat("#,##0.00"));
            s.setAlignment(HorizontalAlignment.RIGHT);
            return s;
        }

        CellStyle total() {
            CellStyle s = wb.createCellStyle();
            Font f = wb.createFont();
            f.setBold(true);
            f.setFontHeightInPoints((short) 10);
            f.setColor(BRAND_COLOR);
            s.setFont(f);
            s.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            s.setAlignment(HorizontalAlignment.RIGHT);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            return s;
        }
    }
}
