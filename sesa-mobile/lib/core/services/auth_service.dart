// Autor: Ing. J Sebastian Vargas S
// Servicio de autenticación — consume la API SESA backend

import 'dart:convert';
import 'package:http/http.dart' as http;
import '../constants/api_constants.dart';
import '../models/login_response.dart';

class AuthException implements Exception {
  final String message;
  const AuthException(this.message);
  @override
  String toString() => message;
}

class AuthService {
  final http.Client _client;
  AuthService({http.Client? client}) : _client = client ?? http.Client();

  /// Inicio de sesión — acepta email o número de documento
  Future<LoginResponse> login({
    required String identifier,
    required String password,
  }) async {
    late http.Response response;
    try {
      response = await _client
          .post(
            Uri.parse(ApiConstants.login),
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({'email': identifier, 'password': password}),
          )
          .timeout(const Duration(seconds: 20));
    } on Exception catch (e) {
      final msg = e.toString().toLowerCase();
      if (msg.contains('timeout')) {
        throw const AuthException(
          'El servidor tardó demasiado en responder. Verifica que el backend esté activo.',
        );
      }
      throw AuthException(
        'No se pudo conectar al servidor ($e).\n'
        'Verifica que el backend esté corriendo en el puerto 8081 '
        'y que la IP en api_constants.dart sea correcta.',
      );
    }

    if (response.statusCode == 200) {
      final json = jsonDecode(response.body) as Map<String, dynamic>;
      return LoginResponse.fromJson(json);
    } else if (response.statusCode == 401 || response.statusCode == 403) {
      throw const AuthException('Credenciales incorrectas.');
    } else {
      throw AuthException('Error del servidor (${response.statusCode}).');
    }
  }

  /// Módulos accesibles para el usuario actual
  Future<List<String>> getUserModules(String token) async {
    late http.Response response;
    try {
      response = await _client
          .get(
            Uri.parse(ApiConstants.userModules),
            headers: {'Authorization': 'Bearer $token'},
          )
          .timeout(const Duration(seconds: 15));
    } catch (_) {
      return [];
    }
    if (response.statusCode == 200) {
      final json = jsonDecode(response.body) as Map<String, dynamic>;
      final list = json['modulos'] as List<dynamic>? ?? [];
      return list.map((e) => e.toString()).toList();
    }
    return [];
  }

  /// Resumen estadístico para el dashboard
  Future<Map<String, dynamic>> getResumen(String token) async {
    late http.Response response;
    try {
      response = await _client
          .get(
            Uri.parse(ApiConstants.reportResumen),
            headers: {'Authorization': 'Bearer $token'},
          )
          .timeout(const Duration(seconds: 15));
    } catch (_) {
      return {};
    }
    if (response.statusCode == 200) {
      return jsonDecode(response.body) as Map<String, dynamic>;
    }
    return {};
  }
}
