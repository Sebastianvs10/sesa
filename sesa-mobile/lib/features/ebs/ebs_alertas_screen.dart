// Autor: Ing. J Sebastian Vargas S
// EBS Alertas — listado

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../core/constants/app_colors.dart';
import '../../core/providers/auth_provider.dart';
import '../../core/services/ebs_service.dart';
import '../../core/models/ebs_models.dart';

class EbsAlertasScreen extends StatefulWidget {
  const EbsAlertasScreen({super.key, required this.ebsService});
  final EbsService ebsService;

  @override
  State<EbsAlertasScreen> createState() => _EbsAlertasScreenState();
}

class _EbsAlertasScreenState extends State<EbsAlertasScreen> {
  List<EbsAlertDto> _list = [];
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
      final list = await widget.ebsService.listAlerts(token);
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
          'Alertas',
          style: GoogleFonts.inter(
            fontWeight: FontWeight.w700,
            color: AppColors.darkTextPrimary,
          ),
        ),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded, color: AppColors.darkTextPrimary),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _list.isEmpty
              ? Center(
                  child: Text(
                    'No hay alertas.',
                    style: GoogleFonts.inter(color: AppColors.darkTextSecondary),
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
                      final a = _list[i];
                      return Card(
                        margin: const EdgeInsets.only(bottom: 12),
                        color: AppColors.darkSurface,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(14),
                        ),
                        child: ListTile(
                          contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                          title: Text(
                            a.title,
                            style: GoogleFonts.inter(
                              fontWeight: FontWeight.w700,
                              color: AppColors.darkTextPrimary,
                            ),
                          ),
                          subtitle: Text(
                            '${a.alertDate} · ${a.status ?? "—"}',
                            style: GoogleFonts.inter(
                              fontSize: 12,
                              color: AppColors.darkTextSecondary,
                            ),
                          ),
                        ),
                      );
                    },
                  ),
                ),
    );
  }
}
