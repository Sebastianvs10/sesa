// Autor: Ing. J Sebastian Vargas S
// Sistema de colores SESA — espejo del design system web

import 'package:flutter/material.dart';

class AppColors {
  AppColors._();

  // ── Paleta base ───────────────────────────────────────────────
  static const Color primary   = Color(0xFF0A2E4F);
  static const Color secondary = Color(0xFF1F6AE1);
  static const Color accent    = Color(0xFF2BB0A6);

  // ── Gradiente principal (botón / acentos) ─────────────────────
  static const LinearGradient primaryGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [Color(0xFF1F6AE1), Color(0xFF1558C9), Color(0xFF2BB0A6)],
    stops: [0.0, 0.5, 1.0],
  );

  // ── Gradiente fondo login dark ────────────────────────────────
  static const LinearGradient loginBgGradient = LinearGradient(
    begin: Alignment.topCenter,
    end: Alignment.bottomCenter,
    colors: [Color(0xFF060B16), Color(0xFF0A1020), Color(0xFF070D18)],
    stops: [0.0, 0.4, 1.0],
  );

  // ── Light theme ───────────────────────────────────────────────
  static const Color lightBg           = Color(0xFFF4F7FB);
  static const Color lightSurface      = Color(0xFFFFFFFF);
  static const Color lightTextPrimary  = Color(0xFF1E293B);
  static const Color lightTextSecondary= Color(0xFF64748B);
  static const Color lightTextMuted    = Color(0xFF94A3B8);
  static const Color lightBorder       = Color(0xFFE2E8F0);
  static const Color lightInputBg      = Color(0xFFFFFFFF);

  // ── Dark theme ────────────────────────────────────────────────
  static const Color darkBg            = Color(0xFF0C1222);
  static const Color darkSurface       = Color(0xFF1A2235);
  static const Color darkSurfaceHover  = Color(0xFF212D42);
  static const Color darkTextPrimary   = Color(0xFFF1F5F9);
  static const Color darkTextSecondary = Color(0xFF94A3B8);
  static const Color darkTextMuted     = Color(0xFF64748B);
  static const Color darkBorder        = Color(0xFF2A3A52);
  static const Color darkInputBg       = Color(0xFF1A2235);

  // ── Semánticos ────────────────────────────────────────────────
  static const Color success = Color(0xFF22C55E);
  static const Color warning = Color(0xFFF59E0B);
  static const Color error   = Color(0xFFEF4444);
  static const Color info    = Color(0xFF0EA5E9);

  // ── Gradientes por módulo (dashboard) ─────────────────────────
  static const Map<String, List<Color>> moduleGradients = {
    'DASHBOARD'       : [Color(0xFF1F6AE1), Color(0xFF38BDF8)],
    'PACIENTES'       : [Color(0xFF0EA5E9), Color(0xFF06B6D4)],
    'HISTORIA_CLINICA': [Color(0xFF6366F1), Color(0xFF8B5CF6)],
    'LABORATORIOS'    : [Color(0xFFF59E0B), Color(0xFFF97316)],
    'IMAGENES'        : [Color(0xFF8B5CF6), Color(0xFFA78BFA)],
    'URGENCIAS'       : [Color(0xFFEF4444), Color(0xFFF97316)],
    'HOSPITALIZACION' : [Color(0xFF8B5CF6), Color(0xFFA78BFA)],
    'FARMACIA'        : [Color(0xFF14B8A6), Color(0xFF06B6D4)],
    'FACTURACION'     : [Color(0xFF22C55E), Color(0xFF16A34A)],
    'CITAS'           : [Color(0xFF1F6AE1), Color(0xFF38BDF8)],
    'USUARIOS'        : [Color(0xFF6366F1), Color(0xFF8B5CF6)],
    'PERSONAL'        : [Color(0xFF0EA5E9), Color(0xFF06B6D4)],
    'EMPRESAS'        : [Color(0xFF0A2E4F), Color(0xFF1F6AE1)],
    'NOTIFICACIONES'  : [Color(0xFFF59E0B), Color(0xFFEF4444)],
    'ROLES'           : [Color(0xFF14B8A6), Color(0xFF6366F1)],
    'REPORTES'        : [Color(0xFF22C55E), Color(0xFF0EA5E9)],
  };
}
