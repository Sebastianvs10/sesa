// Autor: Ing. J Sebastian Vargas S
// Constantes de la API SESA backend

class ApiConstants {
  ApiConstants._();

  // ══════════════════════════════════════════════════════════════
  // CONFIGURACIÓN DE SERVIDOR
  //
  // ▸ Emulador Android  → usa 10.0.2.2  (apunta a localhost del PC)
  // ▸ Dispositivo físico → usa la IP local de tu PC en la red WiFi/LAN
  //   (actualmente: 192.168.18.132)
  // ▸ Producción        → reemplaza con la URL real del servidor
  // ══════════════════════════════════════════════════════════════

  static const bool _isEmulator = true; // ← true = emulador Android | false = dispositivo físico

  static const String _emulatorHost = '10.0.2.2:8000';
  static const String _deviceHost   = '192.168.18.132:8000';

  static String get _host => _isEmulator ? _emulatorHost : _deviceHost;
  static String get baseUrl => 'http://$_host/api';

  // ── Auth ──────────────────────────────────────────────────────
  static String get login         => '$baseUrl/auth/login';
  static String get requestReset  => '$baseUrl/auth/password/request-reset';
  static String get resetPassword => '$baseUrl/auth/password/reset';

  // ── Roles / módulos ───────────────────────────────────────────
  static String get userModules   => '$baseUrl/roles/usuario-actual';

  // ── Reportes dashboard ────────────────────────────────────────
  static String get reportResumen => '$baseUrl/reportes/resumen';
  static String get citas         => '$baseUrl/citas';

  // ── EBS (Equipos Básicos de Salud) ───────────────────────────
  static String get ebsBase           => '$baseUrl/ebs';
  static String get ebsTerritories    => '$ebsBase/territories';
  static String get ebsHouseholds     => '$ebsBase/households';
  static String get ebsHomeVisits     => '$ebsBase/home-visits';
  static String get ebsDashboard      => '$ebsBase/dashboard';
  static String get ebsBrigades        => '$ebsBase/brigades';
  static String get ebsAlerts         => '$ebsBase/alerts';
  static String ebsTerritoryTeam(int id) => '$ebsBase/territories/$id/team';
  static String ebsBrigadeTeam(int id)   => '$ebsBase/brigades/$id/team';
  static String get ebsReportsData    => '$ebsBase/reports/data';
  static String get ebsIgacDeptos     => '$baseUrl/igac/departamentos';
  static String get ebsIgacMunicipios => '$baseUrl/igac/municipios';
  static String get ebsIgacVeredas   => '$baseUrl/igac/veredas';
}
