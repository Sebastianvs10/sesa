// Autor: Ing. J Sebastian Vargas S
// EBS Hogares de un territorio — listado y acción Nueva visita

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../core/constants/app_colors.dart';
import '../../core/providers/auth_provider.dart';
import '../../core/services/ebs_service.dart';
import '../../core/models/ebs_models.dart';
import 'ebs_visita_nueva_screen.dart';

class EbsHouseholdsScreen extends StatefulWidget {
  const EbsHouseholdsScreen({
    super.key,
    required this.ebsService,
    required this.territory,
  });
  final EbsService ebsService;
  final EbsTerritorySummary territory;

  @override
  State<EbsHouseholdsScreen> createState() => _EbsHouseholdsScreenState();
}

class _EbsHouseholdsScreenState extends State<EbsHouseholdsScreen> {
  List<EbsHouseholdSummary> _list = [];
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
      final list = await widget.ebsService.listHouseholds(
        token,
        widget.territory.id,
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
    return Scaffold(
      backgroundColor: AppColors.darkBg,
      appBar: AppBar(
        backgroundColor: AppColors.darkBg,
        elevation: 0,
        title: Text(
          widget.territory.name,
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
                    'Sin hogares en este territorio.',
                    style: GoogleFonts.inter(
                      color: AppColors.darkTextSecondary,
                    ),
                  ),
                )
              : RefreshIndicator(
                  onRefresh: _load,
                  color: AppColors.secondary,
                  child: ListView.builder(
                    padding: const EdgeInsets.fromLTRB(20, 16, 20, 100),
                    itemCount: _list.length,
                    itemBuilder: (_, i) {
                      final h = _list[i];
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
                            h.addressText,
                            style: GoogleFonts.inter(
                              fontWeight: FontWeight.w600,
                              color: AppColors.darkTextPrimary,
                            ),
                          ),
                          subtitle: Text(
                            '${h.state}${h.riskLevel != null ? " · ${h.riskLevel}" : ""}',
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
                              builder: (_) => EbsVisitaNuevaScreen(
                                ebsService: widget.ebsService,
                                preselectedTerritoryId: widget.territory.id,
                                preselectedHouseholdId: h.id,
                              ),
                            ),
                          ),
                        ),
                      );
                    },
                  ),
                ),
      floatingActionButton: _list.isEmpty
          ? null
          : FloatingActionButton.extended(
              onPressed: () => Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => EbsVisitaNuevaScreen(
                    ebsService: widget.ebsService,
                    preselectedTerritoryId: widget.territory.id,
                  ),
                ),
              ),
              backgroundColor: AppColors.secondary,
              icon: const Icon(Icons.add_rounded),
              label: Text(
                'Nueva visita',
                style: GoogleFonts.inter(fontWeight: FontWeight.w600),
              ),
            ),
    );
  }
}
