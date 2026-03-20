// Autor: Ing. J Sebastian Vargas S
// Modelo de respuesta del endpoint POST /api/auth/login

class LoginResponse {
  final String accessToken;
  final String tokenType;
  final int expiresInMs;
  final int userId;
  final String email;
  final String nombreCompleto;
  final String role;
  final String schema;
  final String empresaNombre;

  const LoginResponse({
    required this.accessToken,
    required this.tokenType,
    required this.expiresInMs,
    required this.userId,
    required this.email,
    required this.nombreCompleto,
    required this.role,
    required this.schema,
    required this.empresaNombre,
  });

  factory LoginResponse.fromJson(Map<String, dynamic> json) => LoginResponse(
    accessToken   : json['accessToken']    as String,
    tokenType     : json['tokenType']      as String? ?? 'Bearer',
    expiresInMs   : json['expiresInMs']    as int?    ?? 86400000,
    userId        : (json['userId'] as num).toInt(),
    email         : json['email']          as String,
    nombreCompleto: json['nombreCompleto'] as String,
    role          : json['role']           as String,
    schema        : json['schema']         as String,
    empresaNombre : json['empresaNombre']  as String? ?? 'SESA Global',
  );

  Map<String, dynamic> toJson() => {
    'accessToken'  : accessToken,
    'tokenType'    : tokenType,
    'expiresInMs'  : expiresInMs,
    'userId'       : userId,
    'email'        : email,
    'nombreCompleto': nombreCompleto,
    'role'         : role,
    'schema'       : schema,
    'empresaNombre': empresaNombre,
  };
}
