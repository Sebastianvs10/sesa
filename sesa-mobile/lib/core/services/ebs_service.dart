// Autor: Ing. J Sebastian Vargas S
// Servicio EBS — territorios, hogares, visitas, brigadas, alertas, reportes

import 'dart:convert';
import 'package:http/http.dart' as http;
import '../constants/api_constants.dart';
import '../models/ebs_models.dart';

class EbsService {
  final http.Client _client = http.Client();

  Map<String, String> _headers(String token) => {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $token',
      };

  Future<List<EbsTerritorySummary>> listTerritories(String token) async {
    final r = await _client.get(
      Uri.parse(ApiConstants.ebsTerritories),
      headers: _headers(token),
    ).timeout(const Duration(seconds: 15));
    if (r.statusCode != 200) return [];
    final list = jsonDecode(r.body) as List<dynamic>? ?? [];
    return list
        .map((e) => EbsTerritorySummary.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<List<EbsHouseholdSummary>> listHouseholds(
    String token,
    int territoryId, {
    String? riskLevel,
    String? visitStatus,
  }) async {
    var uri = Uri.parse(ApiConstants.ebsHouseholds).replace(
      queryParameters: {'territoryId': '$territoryId'},
    );
    if (riskLevel != null && riskLevel != 'TODOS') {
      uri = uri.replace(queryParameters: {
        ...uri.queryParameters,
        'riskLevel': riskLevel,
      });
    }
    if (visitStatus != null && visitStatus != 'TODOS') {
      uri = uri.replace(queryParameters: {
        ...uri.queryParameters,
        'visitStatus': visitStatus,
      });
    }
    final r = await _client.get(uri, headers: _headers(token))
        .timeout(const Duration(seconds: 15));
    if (r.statusCode != 200) return [];
    final list = jsonDecode(r.body) as List<dynamic>? ?? [];
    return list
        .map((e) => EbsHouseholdSummary.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<List<EbsHomeVisitSummary>> listHomeVisits(
    String token, {
    int? territoryId,
    String? dateFrom,
    String? dateTo,
  }) async {
    final params = <String, String>{};
    if (territoryId != null) params['territoryId'] = '$territoryId';
    if (dateFrom != null) params['dateFrom'] = dateFrom;
    if (dateTo != null) params['dateTo'] = dateTo;
    final uri = Uri.parse(ApiConstants.ebsHomeVisits).replace(
      queryParameters: params.isEmpty ? null : params,
    );
    final r = await _client.get(uri, headers: _headers(token))
        .timeout(const Duration(seconds: 15));
    if (r.statusCode != 200) return [];
    final list = jsonDecode(r.body) as List<dynamic>? ?? [];
    return list
        .map((e) => EbsHomeVisitSummary.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<bool> createHomeVisit(String token, Map<String, dynamic> payload) async {
    final r = await _client.post(
      Uri.parse(ApiConstants.ebsHomeVisits),
      headers: _headers(token),
      body: jsonEncode(payload),
    ).timeout(const Duration(seconds: 15));
    return r.statusCode == 200 || r.statusCode == 201;
  }

  Future<EbsDashboardData?> getDashboard(String token, {int diasPeriodo = 30}) async {
    final uri = Uri.parse(ApiConstants.ebsDashboard).replace(
      queryParameters: {'diasPeriodo': '$diasPeriodo'},
    );
    final r = await _client.get(uri, headers: _headers(token))
        .timeout(const Duration(seconds: 15));
    if (r.statusCode != 200) return null;
    return EbsDashboardData.fromJson(
        jsonDecode(r.body) as Map<String, dynamic>);
  }

  Future<List<EbsBrigadeDto>> listBrigades(String token, {int? territoryId}) async {
    final uri = territoryId == null
        ? Uri.parse(ApiConstants.ebsBrigades)
        : Uri.parse(ApiConstants.ebsBrigades).replace(
            queryParameters: {'territoryId': '$territoryId'},
          );
    final r = await _client.get(uri, headers: _headers(token))
        .timeout(const Duration(seconds: 15));
    if (r.statusCode != 200) return [];
    final list = jsonDecode(r.body) as List<dynamic>? ?? [];
    return list
        .map((e) => EbsBrigadeDto.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<List<EbsAlertDto>> listAlerts(String token, {String? status}) async {
    final uri = status == null || status.isEmpty
        ? Uri.parse(ApiConstants.ebsAlerts)
        : Uri.parse(ApiConstants.ebsAlerts).replace(
            queryParameters: {'status': status},
          );
    final r = await _client.get(uri, headers: _headers(token))
        .timeout(const Duration(seconds: 15));
    if (r.statusCode != 200) return [];
    final list = jsonDecode(r.body) as List<dynamic>? ?? [];
    return list
        .map((e) => EbsAlertDto.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<bool> createAlert(String token, Map<String, dynamic> dto) async {
    final r = await _client.post(
      Uri.parse(ApiConstants.ebsAlerts),
      headers: _headers(token),
      body: jsonEncode(dto),
    ).timeout(const Duration(seconds: 15));
    return r.statusCode == 200 || r.statusCode == 201;
  }

  Future<EbsReportDataDto?> getReportData(String token,
      {String reportType = 'COBERTURA'}) async {
    final uri = Uri.parse(ApiConstants.ebsReportsData).replace(
      queryParameters: {'reportType': reportType},
    );
    final r = await _client.get(uri, headers: _headers(token))
        .timeout(const Duration(seconds: 15));
    if (r.statusCode != 200) return null;
    return EbsReportDataDto.fromJson(
        jsonDecode(r.body) as Map<String, dynamic>);
  }

  Future<bool> createTerritory(
      String token, Map<String, dynamic> dto) async {
    final r = await _client.post(
      Uri.parse(ApiConstants.ebsTerritories),
      headers: _headers(token),
      body: jsonEncode(dto),
    ).timeout(const Duration(seconds: 15));
    return r.statusCode == 200 || r.statusCode == 201;
  }

  Future<bool> createBrigade(String token, Map<String, dynamic> dto) async {
    final r = await _client.post(
      Uri.parse(ApiConstants.ebsBrigades),
      headers: _headers(token),
      body: jsonEncode(dto),
    ).timeout(const Duration(seconds: 15));
    return r.statusCode == 200 || r.statusCode == 201;
  }

  void dispose() => _client.close();
}
