// Autor: Ing. J Sebastian Vargas S
// Layout EBS — navegación inferior (Inicio, Territorios, Visitas, Más)

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../core/constants/app_colors.dart';
import '../../core/providers/auth_provider.dart';
import '../../core/services/ebs_service.dart';
import 'ebs_inicio_screen.dart';
import 'ebs_territorios_screen.dart';
import 'ebs_visitas_screen.dart';
import 'ebs_asignacion_screen.dart';
import 'ebs_brigadas_screen.dart';
import 'ebs_reportes_screen.dart';
import 'ebs_alertas_screen.dart';
import 'ebs_territorio_crear_screen.dart';
import '../dashboard/dashboard_screen.dart';

class EbsLayoutScreen extends StatefulWidget {
  const EbsLayoutScreen({super.key});

  @override
  State<EbsLayoutScreen> createState() => _EbsLayoutScreenState();
}

class _EbsLayoutScreenState extends State<EbsLayoutScreen> {
  int _index = 0;
  late final EbsService _ebsService;

  @override
  void initState() {
    super.initState();
    _ebsService = EbsService();
  }

  @override
  void dispose() {
    _ebsService.dispose();
    super.dispose();
  }

  void _showMoreSheet() {
    showModalBottomSheet(
      context: context,
      backgroundColor: AppColors.darkSurface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 16),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                'Más opciones EBS',
                style: GoogleFonts.inter(
                  fontSize: 16,
                  fontWeight: FontWeight.w700,
                  color: AppColors.darkTextPrimary,
                ),
              ),
              const SizedBox(height: 16),
              _MoreTile(
                icon: Icons.people_alt_rounded,
                label: 'Asignación territorial',
                onTap: () {
                  Navigator.pop(ctx);
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) => EbsAsignacionScreen(ebsService: _ebsService),
                    ),
                  );
                },
              ),
              _MoreTile(
                icon: Icons.calendar_today_rounded,
                label: 'Brigadas',
                onTap: () {
                  Navigator.pop(ctx);
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) => EbsBrigadasScreen(ebsService: _ebsService),
                    ),
                  );
                },
              ),
              _MoreTile(
                icon: Icons.bar_chart_rounded,
                label: 'Reportes',
                onTap: () {
                  Navigator.pop(ctx);
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) => EbsReportesScreen(ebsService: _ebsService),
                    ),
                  );
                },
              ),
              _MoreTile(
                icon: Icons.warning_amber_rounded,
                label: 'Alertas',
                onTap: () {
                  Navigator.pop(ctx);
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) => EbsAlertasScreen(ebsService: _ebsService),
                    ),
                  );
                },
              ),
              _MoreTile(
                icon: Icons.add_location_alt_rounded,
                label: 'Crear territorio',
                onTap: () {
                  Navigator.pop(ctx);
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) =>
                          EbsTerritorioCrearScreen(ebsService: _ebsService),
                    ),
                  );
                },
              ),
            ],
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final token = context.watch<AuthProvider>().user?.accessToken;
    if (token == null) {
      return const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      );
    }

    return Scaffold(
      backgroundColor: AppColors.darkBg,
      body: IndexedStack(
        index: _index,
        children: [
          EbsInicioScreen(ebsService: _ebsService),
          EbsTerritoriosScreen(ebsService: _ebsService),
          EbsVisitasScreen(ebsService: _ebsService),
        ],
      ),
      bottomNavigationBar: Container(
        decoration: BoxDecoration(
          color: AppColors.darkSurface,
          border: Border(
            top: BorderSide(color: AppColors.darkBorder),
          ),
        ),
        child: SafeArea(
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 8),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _NavItem(
                  icon: Icons.home_rounded,
                  label: 'Inicio',
                  selected: _index == 0,
                  onTap: () => setState(() => _index = 0),
                ),
                _NavItem(
                  icon: Icons.map_rounded,
                  label: 'Territorios',
                  selected: _index == 1,
                  onTap: () => setState(() => _index = 1),
                ),
                _NavItem(
                  icon: Icons.assignment_rounded,
                  label: 'Visitas',
                  selected: _index == 2,
                  onTap: () => setState(() => _index = 2),
                ),
                _NavItem(
                  icon: Icons.more_horiz_rounded,
                  label: 'Más',
                  selected: false,
                  onTap: _showMoreSheet,
                ),
              ],
            ),
          ),
        ),
      ),
      appBar: _index == 0
          ? null
          : AppBar(
              backgroundColor: AppColors.darkBg,
              elevation: 0,
              leading: IconButton(
                icon: const Icon(Icons.arrow_back_rounded,
                    color: AppColors.darkTextPrimary),
                onPressed: () => Navigator.of(context).pushReplacement(
                  MaterialPageRoute(
                    builder: (_) => const DashboardScreen(),
                  ),
                ),
              ),
              title: Text(
                _title(),
                style: GoogleFonts.inter(
                  fontWeight: FontWeight.w700,
                  color: AppColors.darkTextPrimary,
                ),
              ),
            ),
    );
  }

  String _title() {
    switch (_index) {
      case 1:
        return 'Territorios';
      case 2:
        return 'Visitas';
      default:
        return 'EBS';
    }
  }
}

class _NavItem extends StatelessWidget {
  const _NavItem({
    required this.icon,
    required this.label,
    required this.selected,
    required this.onTap,
  });
  final IconData icon;
  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              icon,
              size: 24,
              color: selected
                  ? AppColors.secondary
                  : AppColors.darkTextSecondary,
            ),
            const SizedBox(height: 4),
            Text(
              label,
              style: GoogleFonts.inter(
                fontSize: 11,
                fontWeight: selected ? FontWeight.w600 : FontWeight.w500,
                color: selected
                    ? AppColors.secondary
                    : AppColors.darkTextSecondary,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _MoreTile extends StatelessWidget {
  const _MoreTile({
    required this.icon,
    required this.label,
    required this.onTap,
  });
  final IconData icon;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Icon(icon, color: AppColors.secondary, size: 22),
      title: Text(
        label,
        style: GoogleFonts.inter(
          color: AppColors.darkTextPrimary,
          fontWeight: FontWeight.w500,
        ),
      ),
      onTap: onTap,
    );
  }
}
