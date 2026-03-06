// Autor: Ing. J Sebastian Vargas S
// EBS Territorios — listado de microterritorios y hogares

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../core/constants/app_colors.dart';
import '../../core/providers/auth_provider.dart';
import '../../core/services/ebs_service.dart';
import '../../core/models/ebs_models.dart';
import 'ebs_households_screen.dart';

class EbsTerritoriosScreen extends StatefulWidget {
  const EbsTerritoriosScreen({
    super.key,
    required this.ebsService,
    this.initialTerritoryId,
  });
  final EbsService ebsService;
  final int? initialTerritoryId;

  @override
  State<EbsTerritoriosScreen> createState() => _EbsTerritoriosScreenState();
}

class _EbsTerritoriosScreenState extends State<EbsTerritoriosScreen> {
  List<EbsTerritorySummary> _list = [];
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
      final list = await widget.ebsService.listTerritories(token);
      if (mounted) {
        setState(() {
          _list = list;
          _loading = false;
        });
        if (widget.initialTerritoryId != null) {
          EbsTerritorySummary? selected;
          for (final e in list) {
            if (e.id == widget.initialTerritoryId) {
              selected = e;
              break;
            }
          }
          if (selected != null) {
            Navigator.push(
              context,
              MaterialPageRoute(
                builder: (_) => EbsHouseholdsScreen(
                  ebsService: widget.ebsService,
                  territory: selected!,
                ),
              ),
            );
          }
        }
      }
    } catch (_) {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_list.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Text(
            'No hay territorios configurados.',
            style: GoogleFonts.inter(
              color: AppColors.darkTextSecondary,
              fontSize: 15,
            ),
            textAlign: TextAlign.center,
          ),
        ),
      );
    }
    return RefreshIndicator(
      onRefresh: _load,
      color: AppColors.secondary,
      child: ListView.builder(
        padding: const EdgeInsets.fromLTRB(20, 16, 20, 32),
        itemCount: _list.length,
        itemBuilder: (_, i) {
          final t = _list[i];
          return Card(
            margin: const EdgeInsets.only(bottom: 12),
            color: AppColors.darkSurface,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(14),
            ),
            child: ListTile(
              contentPadding:
                  const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              title: Text(
                t.name,
                style: GoogleFonts.inter(
                  fontWeight: FontWeight.w700,
                  color: AppColors.darkTextPrimary,
                ),
              ),
              subtitle: Text(
                '${t.code} · ${t.visitedHouseholdsCount}/${t.householdsCount} visitados'
                '${t.highRiskHouseholdsCount > 0 ? " · ${t.highRiskHouseholdsCount} alto riesgo" : ""}',
                style: GoogleFonts.inter(
                  fontSize: 12,
                  color: AppColors.darkTextSecondary,
                ),
              ),
              trailing: const Icon(
                Icons.chevron_right_rounded,
                color: AppColors.darkTextSecondary,
              ),
              onTap: () => Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => EbsHouseholdsScreen(
                    ebsService: widget.ebsService,
                    territory: t,
                  ),
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}
