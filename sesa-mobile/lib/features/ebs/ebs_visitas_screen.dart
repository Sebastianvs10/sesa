// Autor: Ing. J Sebastian Vargas S
// EBS Visitas — historial de visitas domiciliarias

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../core/constants/app_colors.dart';
import '../../core/providers/auth_provider.dart';
import '../../core/services/ebs_service.dart';
import '../../core/models/ebs_models.dart';

class EbsVisitasScreen extends StatefulWidget {
  const EbsVisitasScreen({super.key, required this.ebsService});
  final EbsService ebsService;

  @override
  State<EbsVisitasScreen> createState() => _EbsVisitasScreenState();
}

class _EbsVisitasScreenState extends State<EbsVisitasScreen> {
  List<EbsHomeVisitSummary> _list = [];
  bool _loading = true;
  int _dias = 90;

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
      final to = DateTime.now();
      final from = to.subtract(Duration(days: _dias));
      final list = await widget.ebsService.listHomeVisits(
        token,
        dateFrom: from.toIso8601String(),
        dateTo: to.toIso8601String(),
      );
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
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
          child: Row(
            children: [
              Text(
                'Últimos ',
                style: GoogleFonts.inter(
                  color: AppColors.darkTextSecondary,
                  fontSize: 14,
                ),
              ),
              DropdownButton<int>(
                value: _dias,
                dropdownColor: AppColors.darkSurface,
                underline: const SizedBox(),
                items: const [
                  DropdownMenuItem(value: 30, child: Text('30 días')),
                  DropdownMenuItem(value: 60, child: Text('60 días')),
                  DropdownMenuItem(value: 90, child: Text('90 días')),
                  DropdownMenuItem(value: 180, child: Text('180 días')),
                ],
                onChanged: (v) {
                  if (v != null) setState(() => _dias = v);
                  _load();
                },
              ),
              const Spacer(),
              IconButton(
                icon: const Icon(Icons.refresh_rounded,
                    color: AppColors.darkTextSecondary),
                onPressed: _load,
              ),
            ],
          ),
        ),
        Expanded(
          child: _loading
              ? const Center(child: CircularProgressIndicator())
              : _list.isEmpty
                  ? Center(
                      child: Text(
                        'No hay visitas en el período seleccionado.',
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
                        padding: const EdgeInsets.fromLTRB(20, 0, 20, 32),
                        itemCount: _list.length,
                        itemBuilder: (_, i) {
                          final v = _list[i];
                          final date = DateTime.tryParse(v.visitDate);
                          final dateStr = date != null
                              ? '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year}'
                              : v.visitDate;
                          return Card(
                            margin: const EdgeInsets.only(bottom: 10),
                            color: AppColors.darkSurface,
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(14),
                            ),
                            child: ListTile(
                              contentPadding: const EdgeInsets.symmetric(
                                  horizontal: 16, vertical: 12),
                              title: Text(
                                v.householdAddress ?? 'Hogar #${v.householdId}',
                                style: GoogleFonts.inter(
                                  fontWeight: FontWeight.w600,
                                  color: AppColors.darkTextPrimary,
                                ),
                              ),
                              subtitle: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  const SizedBox(height: 4),
                                  Text(
                                    dateStr +
                                        (v.territoryName != null
                                            ? ' · ${v.territoryName}'
                                            : ''),
                                    style: GoogleFonts.inter(
                                      fontSize: 12,
                                      color: AppColors.darkTextSecondary,
                                    ),
                                  ),
                                  if (v.motivo != null && v.motivo!.isNotEmpty)
                                    Padding(
                                      padding: const EdgeInsets.only(top: 4),
                                      child: Text(
                                        v.motivo!,
                                        style: GoogleFonts.inter(
                                          fontSize: 12,
                                          color: AppColors.darkTextSecondary,
                                        ),
                                        maxLines: 2,
                                        overflow: TextOverflow.ellipsis,
                                      ),
                                    ),
                                ],
                              ),
                            ),
                          );
                        },
                      ),
                    ),
        ),
      ],
    );
  }
}
