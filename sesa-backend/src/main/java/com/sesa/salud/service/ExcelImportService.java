/**
 * Servicio global de importación desde Excel (.xlsx / .xls)
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.ExcelImportResultDto;
import com.sesa.salud.dto.FacturaRequestDto;
import com.sesa.salud.entity.Eps;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.repository.EpsRepository;
import com.sesa.salud.repository.PacienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private final PacienteRepository pacienteRepository;
    private final EpsRepository epsRepository;
    private final FacturaService facturaService;

    /**
     * Importa pacientes desde un Excel con las columnas:
     * tipo_doc | documento | nombres | apellidos | fecha_nac | sexo | telefono | email | direccion | eps_codigo
     */
    @Transactional
    public ExcelImportResultDto importarPacientes(MultipartFile file) {
        List<String> errores = new ArrayList<>();
        int importados = 0;
        int omitidos = 0;

        try (InputStream is = file.getInputStream();
             Workbook wb = new XSSFWorkbook(is)) {

            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) throw new RuntimeException("El archivo no tiene hojas");

            // Buscar fila de encabezado (primera fila con "documento")
            int dataStartRow = findDataStartRow(sheet);

            for (int i = dataStartRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                try {
                    String documento = getString(row, 1);
                    if (documento == null || documento.isBlank()) {
                        omitidos++;
                        continue;
                    }

                    // Si ya existe, omitir
                    if (pacienteRepository.existsByDocumento(documento)) {
                        errores.add("Fila " + (i + 1) + ": Documento " + documento + " ya existe (omitido)");
                        omitidos++;
                        continue;
                    }

                    String tipoDocumento = getString(row, 0);
                    String nombres       = getString(row, 2);
                    String apellidos     = getString(row, 3);
                    LocalDate fechaNac   = getLocalDate(row, 4);
                    String sexo          = getString(row, 5);
                    String telefono      = getString(row, 6);
                    String email         = getString(row, 7);
                    String direccion     = getString(row, 8);
                    String epsCodigo     = getString(row, 9);

                    if (nombres == null || nombres.isBlank()) {
                        errores.add("Fila " + (i + 1) + ": Campo 'nombres' es obligatorio");
                        omitidos++;
                        continue;
                    }

                    Eps eps = null;
                    if (epsCodigo != null && !epsCodigo.isBlank()) {
                        eps = epsRepository.findByCodigo(epsCodigo).orElse(null);
                        if (eps == null) {
                            errores.add("Fila " + (i + 1) + ": EPS con código '" + epsCodigo + "' no encontrada");
                        }
                    }

                    Paciente paciente = Paciente.builder()
                            .tipoDocumento(tipoDocumento)
                            .documento(documento)
                            .nombres(nombres)
                            .apellidos(apellidos)
                            .fechaNacimiento(fechaNac)
                            .sexo(sexo)
                            .telefono(telefono)
                            .email(email)
                            .direccion(direccion)
                            .eps(eps)
                            .activo(true)
                            .build();

                    pacienteRepository.save(paciente);
                    importados++;

                } catch (Exception e) {
                    errores.add("Fila " + (i + 1) + ": " + e.getMessage());
                    omitidos++;
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Error procesando archivo: " + e.getMessage(), e);
        }

        return ExcelImportResultDto.builder()
                .importados(importados)
                .omitidos(omitidos)
                .errores(errores)
                .mensaje(importados + " paciente(s) importado(s) correctamente" +
                        (omitidos > 0 ? ", " + omitidos + " omitido(s)" : ""))
                .build();
    }

    /**
     * Importa facturas desde Excel con las columnas:
     * numero_factura | paciente_doc | valor_total | estado | descripcion
     */
    @Transactional
    public ExcelImportResultDto importarFacturas(MultipartFile file) {
        List<String> errores = new ArrayList<>();
        int importados = 0;
        int omitidos = 0;

        try (InputStream is = file.getInputStream();
             Workbook wb = new XSSFWorkbook(is)) {

            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) throw new RuntimeException("El archivo no tiene hojas");

            int dataStartRow = findDataStartRow(sheet);

            for (int i = dataStartRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                try {
                    String pacienteDoc = getString(row, 1);
                    if (pacienteDoc == null || pacienteDoc.isBlank()) {
                        omitidos++;
                        continue;
                    }

                    Paciente paciente = pacienteRepository.findByDocumento(pacienteDoc).orElse(null);
                    if (paciente == null) {
                        errores.add("Fila " + (i + 1) + ": Paciente con documento '" + pacienteDoc + "' no encontrado");
                        omitidos++;
                        continue;
                    }

                    String numeroFactura = getString(row, 0);
                    double valorRaw = getNumeric(row, 2);
                    String estado = getString(row, 3);
                    String descripcion = getString(row, 4);

                    FacturaRequestDto dto = new FacturaRequestDto();
                    dto.setNumeroFactura(numeroFactura);
                    dto.setPacienteId(paciente.getId());
                    dto.setValorTotal(BigDecimal.valueOf(valorRaw));
                    dto.setEstado(estado != null && !estado.isBlank() ? estado : "PENDIENTE");
                    dto.setDescripcion(descripcion);

                    facturaService.create(dto);
                    importados++;

                } catch (Exception e) {
                    errores.add("Fila " + (i + 1) + ": " + e.getMessage());
                    omitidos++;
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Error procesando archivo: " + e.getMessage(), e);
        }

        return ExcelImportResultDto.builder()
                .importados(importados)
                .omitidos(omitidos)
                .errores(errores)
                .mensaje(importados + " factura(s) importada(s) correctamente" +
                        (omitidos > 0 ? ", " + omitidos + " omitida(s)" : ""))
                .build();
    }

    /* ── Helpers ──────────────────────────────────────────────────── */

    private int findDataStartRow(Sheet sheet) {
        for (int i = 0; i <= Math.min(sheet.getLastRowNum(), 10); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            for (Cell cell : row) {
                String val = getCellStringValue(cell);
                if (val != null && (val.equalsIgnoreCase("documento")
                        || val.equalsIgnoreCase("numero_factura")
                        || val.equalsIgnoreCase("n° factura"))) {
                    return i + 1;
                }
            }
        }
        return 1; // por defecto, fila 1 (0-based) es la primera de datos
    }

    private String getString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        String val = getCellStringValue(cell);
        return val != null && !val.isBlank() ? val.trim() : null;
    }

    private double getNumeric(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return 0;
        if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
        String s = getCellStringValue(cell);
        if (s == null || s.isBlank()) return 0;
        try { return Double.parseDouble(s.replace(",", "").replace("$", "").trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private LocalDate getLocalDate(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String s = getCellStringValue(cell);
        if (s == null || s.isBlank()) return null;
        try {
            // Intentar dd/MM/yyyy primero
            return LocalDate.parse(s.trim(), java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            try {
                return LocalDate.parse(s.trim());
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCachedFormulaResultType() == CellType.NUMERIC
                    ? String.valueOf((long) cell.getNumericCellValue())
                    : cell.getRichStringCellValue().getString();
            default      -> null;
        };
    }

    private boolean isRowEmpty(Row row) {
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getCellStringValue(cell);
                if (val != null && !val.isBlank()) return false;
            }
        }
        return true;
    }
}
