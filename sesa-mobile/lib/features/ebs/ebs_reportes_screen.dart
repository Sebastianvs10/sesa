// Autor: Ing. J Sebastian Vargas S
// EBS Reportes — cobertura

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../core/constants/app_colors.dart';
import '../../core/providers/auth_provider.dart';
import '../../core/services/ebs_service.dart';
import '../../core/models/ebs_models.dart';

class EbsReportesScreen extends StatefulWidget {
  const EbsReportesScreen({super.key, required this.ebsService});
  final EbsService ebsService;

  @override
  State<EbsReportesScreen> createState() => _EbsReportesScreenState();
}

class _EbsReportesScreenState extends State<EbsReportesScreen> {
  EbsReportDataDto? _data;
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
      final data = await widget.ebsService.getReportData(token);
      if (mounted) {
        setState(() {
          _data = data;
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
          'Reportes EBS',
          style: GoogleFonts.inter(
            fontWeight: FontWeight.w700,
            color: AppColors.darkTextPrimary,
          ),
        ),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded, color: AppColors.darkTextPrimary),
          onPressed: () => Navigator.pop(context),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh_rounded, color: AppColors.darkTextPrimary),
            onPressed: _load,
          ),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _data == null
              ? Center(
                  child: Text(
                    'Pulsa Actualizar para cargar.',
                    style: GoogleFonts.inter(color: AppColors.darkTextSecondary),
                  ),
                )
              : SingleChildScrollView(
                  padding: const EdgeInsets.fromLTRB(20, 16, 20, 32),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: _Kpi('${_data!.totalHouseholds ?? 0}', 'Hogares'),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: _Kpi('${_data!.visitedHouseholds ?? 0}', 'Visitados'),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: _Kpi(
                              '${_data!.coveragePercent?.toStringAsFixed(1) ?? 0}%',
                              'Cobertura',
                              highlight: true,
                            ),
                          ),
                        ],
                      ),
                      if (_data!.rows != null && _data!.rows!.isNotEmpty) ...[
                        const SizedBox(height: 24),
                        Text(
                          'Por territorio',
                          style: GoogleFonts.inter(
                            fontWeight: FontWeight.w700,
                            color: AppColors.darkTextPrimary,
                          ),
                        ),
                        const SizedBox(height: 12),
                        ..._data!.rows!.map(
                          (r) => Card(
                            margin: const EdgeInsets.only(bottom: 8),
                            color: AppColors.darkSurface,
                            child: ListTile(
                              title: Text(
                                r.territoryName,
                                style: GoogleFonts.inter(
                                  fontWeight: FontWeight.w600,
                                  color: AppColors.darkTextPrimary,
                                ),
                              ),
                              subtitle: Text(
                                '${r.visited}/${r.households} · ${r.percent}%',
                                style: GoogleFonts.inter(
                                  fontSize: 12,
                                  color: AppColors.darkTextSecondary,
                                ),
                              ),
                            ),
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
    );
  }
}

class _Kpi extends StatelessWidget {
  const _Kpi(this.value, this.label, {this.highlight = false});
  final String value;
  final String label;
  final bool highlight;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 14),
      decoration: BoxDecoration(
        color: highlight
            ? AppColors.secondary.withValues(alpha: 0.15)
            : AppColors.darkSurface,
        borderRadius: BorderRadius.circular(14),
      ),
      child: Column(
        children: [
          Text(
            value,
            style: GoogleFonts.inter(
              fontSize: 18,
              fontWeight: FontWeight.w800,
              color: highlight ? AppColors.secondary : AppColors.darkTextPrimary,
            ),
          ),
          const SizedBox(height: 4),
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
