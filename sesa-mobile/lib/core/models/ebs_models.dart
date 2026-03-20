// Autor: Ing. J Sebastian Vargas S
// Modelos EBS — territorios, hogares, visitas, brigadas, alertas, reportes

class EbsTerritorySummary {
  final int id;
  final String code;
  final String name;
  final String type;
  final int householdsCount;
  final int visitedHouseholdsCount;
  final int highRiskHouseholdsCount;
  final String? igacDepartamentoNombre;
  final String? igacMunicipioNombre;
  final String? igacVeredaNombre;

  const EbsTerritorySummary({
    required this.id,
    required this.code,
    required this.name,
    required this.type,
    required this.householdsCount,
    required this.visitedHouseholdsCount,
    required this.highRiskHouseholdsCount,
    this.igacDepartamentoNombre,
    this.igacMunicipioNombre,
    this.igacVeredaNombre,
  });

  factory EbsTerritorySummary.fromJson(Map<String, dynamic> json) =>
      EbsTerritorySummary(
        id: (json['id'] as num).toInt(),
        code: json['code'] as String? ?? '',
        name: json['name'] as String? ?? '',
        type: json['type'] as String? ?? 'MICROTERRITORIO',
        householdsCount: (json['householdsCount'] as num?)?.toInt() ?? 0,
        visitedHouseholdsCount:
            (json['visitedHouseholdsCount'] as num?)?.toInt() ?? 0,
        highRiskHouseholdsCount:
            (json['highRiskHouseholdsCount'] as num?)?.toInt() ?? 0,
        igacDepartamentoNombre: json['igacDepartamentoNombre'] as String?,
        igacMunicipioNombre: json['igacMunicipioNombre'] as String?,
        igacVeredaNombre: json['igacVeredaNombre'] as String?,
      );
}

class EbsHouseholdSummary {
  final int id;
  final int territoryId;
  final String addressText;
  final double? latitude;
  final double? longitude;
  final String? lastVisitDate;
  final String? riskLevel;
  final String state;

  const EbsHouseholdSummary({
    required this.id,
    required this.territoryId,
    required this.addressText,
    this.latitude,
    this.longitude,
    this.lastVisitDate,
    this.riskLevel,
    required this.state,
  });

  factory EbsHouseholdSummary.fromJson(Map<String, dynamic> json) =>
      EbsHouseholdSummary(
        id: (json['id'] as num).toInt(),
        territoryId: (json['territoryId'] as num).toInt(),
        addressText: json['addressText'] as String? ?? 'Hogar #${json['id']}',
        latitude: (json['latitude'] as num?)?.toDouble(),
        longitude: (json['longitude'] as num?)?.toDouble(),
        lastVisitDate: json['lastVisitDate'] as String?,
        riskLevel: json['riskLevel'] as String?,
        state: json['state'] as String? ?? 'PENDIENTE_VISITA',
      );
}

class EbsHomeVisitSummary {
  final int id;
  final int householdId;
  final String? householdAddress;
  final int territoryId;
  final String? territoryName;
  final String visitDate;
  final String? visitType;
  final String? motivo;
  final String? notes;
  final String? professionalName;

  const EbsHomeVisitSummary({
    required this.id,
    required this.householdId,
    this.householdAddress,
    required this.territoryId,
    this.territoryName,
    required this.visitDate,
    this.visitType,
    this.motivo,
    this.notes,
    this.professionalName,
  });

  factory EbsHomeVisitSummary.fromJson(Map<String, dynamic> json) =>
      EbsHomeVisitSummary(
        id: (json['id'] as num).toInt(),
        householdId: (json['householdId'] as num).toInt(),
        householdAddress: json['householdAddress'] as String?,
        territoryId: (json['territoryId'] as num).toInt(),
        territoryName: json['territoryName'] as String?,
        visitDate: json['visitDate'] as String? ?? '',
        visitType: json['visitType'] as String?,
        motivo: json['motivo'] as String?,
        notes: json['notes'] as String?,
        professionalName: json['professionalName'] as String?,
      );
}

class EbsDashboardData {
  final int totalTerritorios;
  final int totalHogares;
  final int hogaresVisitados;
  final double porcentajeCobertura;
  final int hogaresAltoRiesgo;
  final int visitasEnPeriodo;

  const EbsDashboardData({
    required this.totalTerritorios,
    required this.totalHogares,
    required this.hogaresVisitados,
    required this.porcentajeCobertura,
    required this.hogaresAltoRiesgo,
    required this.visitasEnPeriodo,
  });

  factory EbsDashboardData.fromJson(Map<String, dynamic> json) =>
      EbsDashboardData(
        totalTerritorios: (json['totalTerritorios'] as num?)?.toInt() ?? 0,
        totalHogares: (json['totalHogares'] as num?)?.toInt() ?? 0,
        hogaresVisitados:
            (json['hogaresVisitados'] as num?)?.toInt() ?? 0,
        porcentajeCobertura:
            (json['porcentajeCobertura'] as num?)?.toDouble() ?? 0,
        hogaresAltoRiesgo:
            (json['hogaresAltoRiesgo'] as num?)?.toInt() ?? 0,
        visitasEnPeriodo:
            (json['visitasEnPeriodo'] as num?)?.toInt() ?? 0,
      );
}

class EbsBrigadeDto {
  final int? id;
  final String name;
  final int? territoryId;
  final String? territoryName;
  final String dateStart;
  final String dateEnd;
  final String? status;
  final List<String>? teamMemberNames;

  const EbsBrigadeDto({
    this.id,
    required this.name,
    this.territoryId,
    this.territoryName,
    required this.dateStart,
    required this.dateEnd,
    this.status,
    this.teamMemberNames,
  });

  factory EbsBrigadeDto.fromJson(Map<String, dynamic> json) => EbsBrigadeDto(
        id: (json['id'] as num?)?.toInt(),
        name: json['name'] as String? ?? '',
        territoryId: (json['territoryId'] as num?)?.toInt(),
        territoryName: json['territoryName'] as String?,
        dateStart: json['dateStart'] as String? ?? '',
        dateEnd: json['dateEnd'] as String? ?? '',
        status: json['status'] as String?,
        teamMemberNames: (json['teamMemberNames'] as List<dynamic>?)
            ?.map((e) => e.toString())
            .toList(),
      );
}

class EbsAlertDto {
  final int? id;
  final String type;
  final String title;
  final String? description;
  final String alertDate;
  final String? status;

  const EbsAlertDto({
    this.id,
    required this.type,
    required this.title,
    this.description,
    required this.alertDate,
    this.status,
  });

  factory EbsAlertDto.fromJson(Map<String, dynamic> json) => EbsAlertDto(
        id: (json['id'] as num?)?.toInt(),
        type: json['type'] as String? ?? 'EPIDEMIOLOGICA',
        title: json['title'] as String? ?? '',
        description: json['description'] as String?,
        alertDate: json['alertDate'] as String? ?? '',
        status: json['status'] as String?,
      );
}

class EbsReportDataDto {
  final String reportType;
  final int? totalHouseholds;
  final int? visitedHouseholds;
  final double? coveragePercent;
  final List<EbsReportRow>? rows;

  const EbsReportDataDto({
    required this.reportType,
    this.totalHouseholds,
    this.visitedHouseholds,
    this.coveragePercent,
    this.rows,
  });

  factory EbsReportDataDto.fromJson(Map<String, dynamic> json) {
    final rowsList = json['rows'] as List<dynamic>?;
    return EbsReportDataDto(
      reportType: json['reportType'] as String? ?? 'COBERTURA',
      totalHouseholds: (json['totalHouseholds'] as num?)?.toInt(),
      visitedHouseholds: (json['visitedHouseholds'] as num?)?.toInt(),
      coveragePercent: (json['coveragePercent'] as num?)?.toDouble(),
      rows: rowsList
          ?.map((e) => EbsReportRow.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}

class EbsReportRow {
  final String territoryName;
  final String? veredaName;
  final int households;
  final int visited;
  final num percent;
  final int highRisk;

  const EbsReportRow({
    required this.territoryName,
    this.veredaName,
    required this.households,
    required this.visited,
    required this.percent,
    required this.highRisk,
  });

  factory EbsReportRow.fromJson(Map<String, dynamic> json) => EbsReportRow(
        territoryName: json['territoryName'] as String? ?? '',
        veredaName: json['veredaName'] as String?,
        households: (json['households'] as num?)?.toInt() ?? 0,
        visited: (json['visited'] as num?)?.toInt() ?? 0,
        percent: (json['percent'] as num?) ?? 0,
        highRisk: (json['highRisk'] as num?)?.toInt() ?? 0,
      );
}
