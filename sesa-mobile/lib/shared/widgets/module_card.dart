// Autor: Ing. J Sebastian Vargas S
// Tarjeta de módulo para el dashboard SESA

import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../core/constants/app_colors.dart';
import '../../features/ebs/ebs_layout_screen.dart';

class ModuleCard extends StatefulWidget {
  const ModuleCard({super.key, required this.code, required this.index});
  final String code;
  final int    index;
  @override
  State<ModuleCard> createState() => _ModuleCardState();
}

class _ModuleCardState extends State<ModuleCard> {
  bool _pressed = false;

  @override
  Widget build(BuildContext context) {
    final colors  = _gradient(widget.code);
    final icon    = _icon(widget.code);
    final label   = _label(widget.code);

    return GestureDetector(
      onTapDown  : (_) => setState(() => _pressed = true),
      onTapUp    : (_) => setState(() => _pressed = false),
      onTapCancel: ()  => setState(() => _pressed = false),
      onTap: () {
        if (widget.code == 'EBS') {
          Navigator.of(context).push(
            MaterialPageRoute(
              builder: (_) => const EbsLayoutScreen(),
            ),
          );
          return;
        }
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('$label — próximamente',
                style: GoogleFonts.inter()),
            backgroundColor: AppColors.darkSurface,
            behavior: SnackBarBehavior.floating,
            shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12)),
          ),
        );
      },
      child: AnimatedScale(
        scale: _pressed ? 0.95 : 1.0,
        duration: const Duration(milliseconds: 120),
        child: Container(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: colors,
            ),
            borderRadius: BorderRadius.circular(20),
            boxShadow: [
              BoxShadow(
                color: colors.first.withValues(alpha: 0.35),
                blurRadius: 16,
                offset: const Offset(0, 6),
              ),
            ],
          ),
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Container(
                  width: 44,
                  height: 44,
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.2),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Icon(icon, color: Colors.white, size: 22),
                ),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      label,
                      style: GoogleFonts.inter(
                        fontSize: 14,
                        fontWeight: FontWeight.w700,
                        color: Colors.white,
                      ),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: 2),
                    Row(
                      children: [
                        Text(
                          'Ver módulo',
                          style: GoogleFonts.inter(
                            fontSize: 11,
                            color: Colors.white.withValues(alpha: 0.75),
                          ),
                        ),
                        const SizedBox(width: 4),
                        Icon(Icons.arrow_forward_rounded,
                            size: 12,
                            color: Colors.white.withValues(alpha: 0.75)),
                      ],
                    ),
                  ],
                ),
              ],
            ),
          ),
        )
            .animate(delay: (widget.index * 60).ms)
            .fadeIn(duration: 400.ms)
            .slideY(begin: 0.15, end: 0, duration: 400.ms, curve: Curves.easeOut),
      ),
    );
  }

  List<Color> _gradient(String code) =>
      AppColors.moduleGradients[code] ??
      const [AppColors.secondary, AppColors.accent];

  IconData _icon(String code) {
    const map = {
      'DASHBOARD'       : Icons.dashboard_rounded,
      'PACIENTES'       : Icons.people_alt_rounded,
      'HISTORIA_CLINICA': Icons.article_rounded,
      'LABORATORIOS'    : Icons.science_rounded,
      'IMAGENES'        : Icons.medical_information_rounded,
      'URGENCIAS'       : Icons.emergency_rounded,
      'HOSPITALIZACION' : Icons.local_hospital_rounded,
      'FARMACIA'        : Icons.local_pharmacy_rounded,
      'FACTURACION'     : Icons.receipt_long_rounded,
      'CITAS'           : Icons.calendar_month_rounded,
      'USUARIOS'        : Icons.manage_accounts_rounded,
      'PERSONAL'        : Icons.badge_rounded,
      'EMPRESAS'        : Icons.business_rounded,
      'NOTIFICACIONES'  : Icons.notifications_rounded,
      'ROLES'           : Icons.admin_panel_settings_rounded,
      'REPORTES'        : Icons.bar_chart_rounded,
      'EBS'             : Icons.health_and_safety_rounded,
    };
    return map[code] ?? Icons.apps_rounded;
  }

  String _label(String code) {
    const map = {
      'DASHBOARD'       : 'Dashboard',
      'PACIENTES'       : 'Pacientes',
      'HISTORIA_CLINICA': 'Historia Clínica',
      'LABORATORIOS'    : 'Laboratorios',
      'IMAGENES'        : 'Imágenes Diagnósticas',
      'URGENCIAS'       : 'Urgencias',
      'HOSPITALIZACION' : 'Hospitalización',
      'FARMACIA'        : 'Farmacia',
      'FACTURACION'     : 'Facturación',
      'CITAS'           : 'Citas',
      'USUARIOS'        : 'Usuarios',
      'PERSONAL'        : 'Personal',
      'EMPRESAS'        : 'Empresas',
      'NOTIFICACIONES'  : 'Notificaciones',
      'ROLES'           : 'Roles',
      'REPORTES'        : 'Reportes',
      'EBS'             : 'EBS — APS',
    };
    return map[code] ?? code;
  }
}
