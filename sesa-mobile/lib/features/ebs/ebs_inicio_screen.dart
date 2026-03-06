// Autor: Ing. J Sebastian Vargas S
// EBS Inicio — resumen, KPIs y accesos rápidos

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../core/constants/app_colors.dart';
import '../../core/providers/auth_provider.dart';
import '../../core/services/ebs_service.dart';
import '../../core/models/ebs_models.dart';
import 'ebs_territorios_screen.dart';
import 'ebs_visitas_screen.dart';
import 'ebs_visita_nueva_screen.dart';
import 'ebs_asignacion_screen.dart';

class EbsInicioScreen extends StatefulWidget {
  const EbsInicioScreen({super.key, required this.ebsService});
  final EbsService ebsService;

  @override
  State<EbsInicioScreen> createState() => _EbsInicioScreenState();
}

class _EbsInicioScreenState extends State<EbsInicioScreen> {
  List<EbsTerritorySummary> _territories = [];
  EbsDashboardData? _dashboard;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final token = context.read<AuthProvider>().user?.accessToken;
    if (token == null) return;
    setState(() => _loading = true);
    try {
      final territories =
          await widget.ebsService.listTerritories(token);
      final dashboard = await widget.ebsService.getDashboard(token);
      if (mounted) {
        setState(() {
          _territories = territories;
          _dashboard = dashboard;
          _loading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return CustomScrollView(
      slivers: [
        SliverToBoxAdapter(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(20, 24, 20, 16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Equipos Básicos de Salud',
                  style: GoogleFonts.inter(
                    fontSize: 22,
                    fontWeight: FontWeight.w800,
                    color: AppColors.darkTextPrimary,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  'Atención Primaria en Salud: gestión territorial y visitas domiciliarias.',
                  style: GoogleFonts.inter(
                    fontSize: 13,
                    color: AppColors.darkTextSecondary,
                  ),
                ),
              ],
            ),
          ),
        ),
        if (_loading)
          const SliverFillRemaining(
            child: Center(child: CircularProgressIndicator()),
          )
        else ...[
          if (_dashboard != null) _buildKpis(_dashboard!),
          if (_territories.isNotEmpty) _buildTerritoriesSection(),
          _buildQuickActions(),
        ],
      ],
    );
  }

  Widget _buildKpis(EbsDashboardData d) {
    return SliverToBoxAdapter(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Resumen',
              style: GoogleFonts.inter(
                fontSize: 16,
                fontWeight: FontWeight.w700,
                color: AppColors.darkTextPrimary,
              ),
            ),
            const SizedBox(height: 12),
            SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                children: [
                  _KpiChip(
                    value: '${d.totalTerritorios}',
                    label: 'Territorios',
                  ),
                  const SizedBox(width: 10),
                  _KpiChip(
                    value: '${d.totalHogares}',
                    label: 'Hogares',
                  ),
                  const SizedBox(width: 10),
                  _KpiChip(
                    value: '${d.porcentajeCobertura.toStringAsFixed(1)}%',
                    label: 'Cobertura',
                    highlight: true,
                  ),
                  const SizedBox(width: 10),
                  _KpiChip(
                    value: '${d.visitasEnPeriodo}',
                    label: 'Visitas (30d)',
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),
          ],
        ),
      ),
    );
  }

  Widget _buildTerritoriesSection() {
    return SliverToBoxAdapter(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Mis microterritorios',
              style: GoogleFonts.inter(
                fontSize: 16,
                fontWeight: FontWeight.w700,
                color: AppColors.darkTextPrimary,
              ),
            ),
            const SizedBox(height: 10),
            ..._territories.take(5).map(
                  (t) => _TerritoryTile(
                    territory: t,
                    onTap: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (_) => EbsTerritoriosScreen(
                            ebsService: widget.ebsService,
                            initialTerritoryId: t.id,
                          ),
                        ),
                      );
                    },
                  ),
                ),
            const SizedBox(height: 20),
          ],
        ),
      ),
    );
  }

  Widget _buildQuickActions() {
    return SliverToBoxAdapter(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 0, 20, 32),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Accesos rápidos',
              style: GoogleFonts.inter(
                fontSize: 16,
                fontWeight: FontWeight.w700,
                color: AppColors.darkTextPrimary,
              ),
            ),
            const SizedBox(height: 12),
            _ActionTile(
              icon: Icons.map_rounded,
              label: 'Territorios y hogares',
              onTap: () => Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => EbsTerritoriosScreen(
                    ebsService: widget.ebsService,
                  ),
                ),
              ),
            ),
            _ActionTile(
              icon: Icons.assignment_rounded,
              label: 'Historial de visitas',
              onTap: () => Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => EbsVisitasScreen(
                    ebsService: widget.ebsService,
                  ),
                ),
              ),
            ),
            _ActionTile(
              icon: Icons.add_circle_rounded,
              label: 'Nueva visita domiciliaria',
              primary: true,
              onTap: () => Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => EbsVisitaNuevaScreen(
                    ebsService: widget.ebsService,
                  ),
                ),
              ),
            ),
            _ActionTile(
              icon: Icons.people_alt_rounded,
              label: 'Asignación territorial',
              onTap: () => Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => EbsAsignacionScreen(
                    ebsService: widget.ebsService,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _KpiChip extends StatelessWidget {
  const _KpiChip({
    required this.value,
    required this.label,
    this.highlight = false,
  });
  final String value;
  final String label;
  final bool highlight;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: highlight
            ? AppColors.secondary.withValues(alpha: 0.15)
            : AppColors.darkSurface,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(
          color: highlight
              ? AppColors.secondary.withValues(alpha: 0.4)
              : AppColors.darkBorder,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            value,
            style: GoogleFonts.inter(
              fontSize: 20,
              fontWeight: FontWeight.w800,
              color: highlight ? AppColors.secondary : AppColors.darkTextPrimary,
            ),
          ),
          const SizedBox(height: 2),
          Text(
            label,
            style: GoogleFonts.inter(
              fontSize: 11,
              color: AppColors.darkTextSecondary,
            ),
          ),
        ],
      ),
    );
  }
}

class _TerritoryTile extends StatelessWidget {
  const _TerritoryTile({
    required this.territory,
    required this.onTap,
  });
  final EbsTerritorySummary territory;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      color: AppColors.darkSurface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
      ),
      child: ListTile(
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        title: Text(
          territory.name,
          style: GoogleFonts.inter(
            fontWeight: FontWeight.w600,
            color: AppColors.darkTextPrimary,
          ),
        ),
        subtitle: Text(
          '${territory.visitedHouseholdsCount}/${territory.householdsCount} visitados'
          '${territory.highRiskHouseholdsCount > 0 ? " · ${territory.highRiskHouseholdsCount} alto riesgo" : ""}',
          style: GoogleFonts.inter(
            fontSize: 12,
            color: AppColors.darkTextSecondary,
          ),
        ),
        trailing: const Icon(
          Icons.chevron_right_rounded,
          color: AppColors.darkTextSecondary,
        ),
        onTap: onTap,
      ),
    );
  }
}

class _ActionTile extends StatelessWidget {
  const _ActionTile({
    required this.icon,
    required this.label,
    required this.onTap,
    this.primary = false,
  });
  final IconData icon;
  final String label;
  final VoidCallback onTap;
  final bool primary;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      color: primary
          ? AppColors.secondary.withValues(alpha: 0.12)
          : AppColors.darkSurface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
      ),
      child: ListTile(
        leading: Icon(
          icon,
          color: primary ? AppColors.secondary : AppColors.darkTextSecondary,
          size: 22,
        ),
        title: Text(
          label,
          style: GoogleFonts.inter(
            fontWeight: FontWeight.w600,
            color: primary ? AppColors.secondary : AppColors.darkTextPrimary,
          ),
        ),
        trailing: Icon(
          Icons.arrow_forward_ios_rounded,
          size: 14,
          color: primary ? AppColors.secondary : AppColors.darkTextSecondary,
        ),
        onTap: onTap,
      ),
    );
  }
}
