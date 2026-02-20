// Autor: Ing. J Sebastian Vargas S
// Provider de autenticación — estado global de la sesión

import 'package:flutter/foundation.dart';
import '../models/login_response.dart';
import '../services/auth_service.dart';
import '../services/storage_service.dart';

enum AuthStatus { unknown, authenticated, unauthenticated }

class AuthProvider extends ChangeNotifier {
  final AuthService    _authService;
  final StorageService _storage;

  AuthProvider({
    AuthService?    authService,
    StorageService? storage,
  })  : _authService = authService ?? AuthService(),
        _storage     = storage     ?? StorageService();

  AuthStatus    _status      = AuthStatus.unknown;
  LoginResponse? _user;
  List<String>  _modules     = [];
  String?       _errorMessage;
  bool          _loading      = false;

  AuthStatus    get status       => _status;
  LoginResponse? get user        => _user;
  List<String>  get modules      => _modules;
  String?       get errorMessage => _errorMessage;
  bool          get isLoading    => _loading;

  /// Restaurar sesión al arrancar la app
  Future<void> tryRestoreSession() async {
    final saved = await _storage.getUser();
    if (saved != null) {
      _user    = saved;
      _status  = AuthStatus.authenticated;
      _modules = await _authService.getUserModules(saved.accessToken);
    } else {
      _status = AuthStatus.unauthenticated;
    }
    notifyListeners();
  }

  Future<bool> login(String identifier, String password) async {
    _loading      = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final response = await _authService.login(
        identifier: identifier,
        password: password,
      );
      await _storage.saveSession(response);
      _user    = response;
      _modules = await _authService.getUserModules(response.accessToken);
      _status  = AuthStatus.authenticated;
      _loading = false;
      notifyListeners();
      return true;
    } on AuthException catch (e) {
      _errorMessage = e.message;
      _loading      = false;
      _status       = AuthStatus.unauthenticated;
      notifyListeners();
      return false;
    }
  }

  Future<void> logout() async {
    await _storage.clearSession();
    _user     = null;
    _modules  = [];
    _status   = AuthStatus.unauthenticated;
    notifyListeners();
  }

  Map<String, dynamic> _resumen = {};
  Map<String, dynamic> get resumen => _resumen;

  Future<void> loadResumen() async {
    if (_user == null) return;
    _resumen = await _authService.getResumen(_user!.accessToken);
    notifyListeners();
  }
}
