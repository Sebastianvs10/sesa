// Autor: Ing. J Sebastian Vargas S
// EBS Asignación territorial — listado de territorios (coord.)

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../core/constants/app_colors.dart';
import '../../core/providers/auth_provider.dart';
import '../../core/services/ebs_service.dart';
import '../../core/models/ebs_models.dart';

class EbsAsignacionScreen extends StatefulWidget {
  const EbsAsignacionScreen({super.key, required this.ebsService});
  final EbsService ebsService;

  @override
  State<EbsAsignacionScreen> createState() => _EbsAsignacionScreenState();
}

class _EbsAsignacionScreenState extends State<EbsAsignacionScreen> {
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
      }
    } catch (_) {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.darkBg,
      appBar: AppBar(
        backgroundColor: AppColors.darkBg,
        elevation: 0,
        title: Text(
          'Asignación territorial',
          style: GoogleFonts.inter(
            fontWeight: FontWeight.w700,
            color: AppColors.darkTextPrimary,
          ),
        ),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded,
              color: AppColors.darkTextPrimary),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _list.isEmpty
              ? Center(
                  child: Text(
                    'No hay territorios. Crea uno desde Más → Crear territorio.',
                    style: GoogleFonts.inter(
                      color: AppColors.darkTextSecondary,
                    ),
                    textAlign: TextAlign.center,
                  ),
                )
              : RefreshIndicator(
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
                        child: Padding(
                          padding: const EdgeInsets.all(16),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  Expanded(
                                    child: Text(
                                      t.name,
                                      style: GoogleFonts.inter(
                                        fontWeight: FontWeight.w700,
                                        color: AppColors.darkTextPrimary,
                                      ),
                                    ),
                                  ),
                                  Container(
                                    padding: const EdgeInsets.symmetric(
                                        horizontal: 10, vertical: 4),
                                    decoration: BoxDecoration(
                                      color: AppColors.secondary
                                          .withValues(alpha: 0.2),
                                      borderRadius: BorderRadius.circular(20),
                                    ),
                                    child: Text(
                                      '${t.visitedHouseholdsCount}/${t.householdsCount}',
                                      style: GoogleFonts.inter(
                                        fontSize: 12,
                                        fontWeight: FontWeight.w600,
                                        color: AppColors.secondary,
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 6),
                              Text(
                                '${t.code}${t.highRiskHouseholdsCount > 0 ? " · ${t.highRiskHouseholdsCount} alto riesgo" : ""}',
                                style: GoogleFonts.inter(
                                  fontSize: 12,
                                  color: AppColors.darkTextSecondary,
                                ),
                              ),
                              const SizedBox(height: 8),
                              Text(
                                'Asignación de equipo: configurar en web o backend.',
                                style: GoogleFonts.inter(
                                  fontSize: 11,
                                  color: AppColors.darkTextMuted,
                                ),
                              ),
                            ],
                          ),
                        ),
                      );
                    },
                  ),
                ),
    );
  }
}
