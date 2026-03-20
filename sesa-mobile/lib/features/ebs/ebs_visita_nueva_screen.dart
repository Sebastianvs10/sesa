// Autor: Ing. J Sebastian Vargas S
// EBS Nueva visita domiciliaria — formulario

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../core/constants/app_colors.dart';
import '../../core/providers/auth_provider.dart';
import '../../core/services/ebs_service.dart';
import '../../core/models/ebs_models.dart';

class EbsVisitaNuevaScreen extends StatefulWidget {
  const EbsVisitaNuevaScreen({
    super.key,
    required this.ebsService,
    this.preselectedTerritoryId,
    this.preselectedHouseholdId,
  });
  final EbsService ebsService;
  final int? preselectedTerritoryId;
  final int? preselectedHouseholdId;

  @override
  State<EbsVisitaNuevaScreen> createState() => _EbsVisitaNuevaScreenState();
}

class _EbsVisitaNuevaScreenState extends State<EbsVisitaNuevaScreen> {
  List<EbsTerritorySummary> _territories = [];
  List<EbsHouseholdSummary> _households = [];
  int? _territoryId;
  int? _householdId;
  String _visitType = 'DOMICILIARIA_APS';
  String _motivo = '';
  String _notes = '';
  bool _loadingTerritories = true;
  bool _loadingHouseholds = false;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _territoryId = widget.preselectedTerritoryId;
    _householdId = widget.preselectedHouseholdId;
    _loadTerritories();
  }

  Future<void> _loadTerritories() async {
    final token = context.read<AuthProvider>().user?.accessToken;
    if (token == null) return;
    setState(() => _loadingTerritories = true);
    try {
      final list = await widget.ebsService.listTerritories(token);
      if (mounted) {
        setState(() {
          _territories = list;
          _loadingTerritories = false;
          if (_territoryId == null && list.isNotEmpty) {
            _territoryId = list.first.id;
            _loadHouseholds();
          } else if (_territoryId != null) {
            _loadHouseholds();
          }
        });
      }
    } catch (_) {
      if (mounted) setState(() => _loadingTerritories = false);
    }
  }

  Future<void> _loadHouseholds() async {
    if (_territoryId == null) return;
    final token = context.read<AuthProvider>().user?.accessToken;
    if (token == null) return;
    setState(() => _loadingHouseholds = true);
    try {
      final list = await widget.ebsService.listHouseholds(token, _territoryId!);
      if (mounted) {
        setState(() {
          _households = list;
          _loadingHouseholds = false;
          if (_householdId == null && list.isNotEmpty && widget.preselectedHouseholdId == null) {
            _householdId = list.first.id;
          }
        });
      }
    } catch (_) {
      if (mounted) setState(() => _loadingHouseholds = false);
    }
  }

  Future<void> _submit() async {
    if (_householdId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Selecciona un hogar'),
          backgroundColor: AppColors.warning,
        ),
      );
      return;
    }
    final token = context.read<AuthProvider>().user?.accessToken;
    if (token == null) return;
    setState(() => _saving = true);
    try {
      final payload = {
        'householdId': _householdId,
        'visitDate': DateTime.now().toUtc().toIso8601String(),
        'visitType': _visitType,
        'motivo': _motivo.isEmpty ? null : _motivo,
        'notes': _notes.isEmpty ? null : _notes,
      };
      final ok = await widget.ebsService.createHomeVisit(token, payload);
      if (mounted) {
        setState(() => _saving = false);
        if (ok) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('Visita registrada'),
              backgroundColor: AppColors.success,
            ),
          );
          Navigator.pop(context);
        } else {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('Error al guardar'),
              backgroundColor: AppColors.error,
            ),
          );
        }
      }
    } catch (_) {
      if (mounted) {
        setState(() => _saving = false);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Error de conexión'),
            backgroundColor: AppColors.error,
          ),
        );
      }
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
          'Nueva visita domiciliaria',
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
      body: _loadingTerritories
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: const EdgeInsets.fromLTRB(20, 16, 20, 32),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  DropdownButtonFormField<int>(
                    value: _territoryId,
                    decoration: _inputDecoration('Microterritorio'),
                    dropdownColor: AppColors.darkSurface,
                    items: _territories
                        .map((t) => DropdownMenuItem(
                              value: t.id,
                              child: Text(t.name),
                            ))
                        .toList(),
                    onChanged: (v) {
                      setState(() {
                        _territoryId = v;
                        _householdId = null;
                      });
                      _loadHouseholds();
                    },
                  ),
                  const SizedBox(height: 16),
                  DropdownButtonFormField<int>(
                    value: _householdId,
                    decoration: _inputDecoration('Hogar'),
                    dropdownColor: AppColors.darkSurface,
                    items: _households
                        .map((h) => DropdownMenuItem(
                              value: h.id,
                              child: Text(h.addressText),
                            ))
                        .toList(),
                    onChanged: _loadingHouseholds
                        ? null
                        : (v) => setState(() => _householdId = v),
                  ),
                  const SizedBox(height: 16),
                  DropdownButtonFormField<String>(
                    value: _visitType,
                    decoration: _inputDecoration('Tipo de visita'),
                    dropdownColor: AppColors.darkSurface,
                    items: const [
                      DropdownMenuItem(
                          value: 'DOMICILIARIA_APS',
                          child: Text('Domiciliaria APS')),
                      DropdownMenuItem(
                          value: 'SEGUIMIENTO', child: Text('Seguimiento')),
                      DropdownMenuItem(
                          value: 'ESTRATIFICACION',
                          child: Text('Estratificación de riesgo')),
                      DropdownMenuItem(
                          value: 'PROMOCION',
                          child: Text('Promoción y prevención')),
                    ],
                    onChanged: (v) =>
                        setState(() => _visitType = v ?? _visitType),
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    decoration: _inputDecoration('Motivo de la visita'),
                    style: GoogleFonts.inter(color: AppColors.darkTextPrimary),
                    maxLines: 2,
                    onChanged: (v) => setState(() => _motivo = v),
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    decoration: _inputDecoration('Notas / hallazgos'),
                    style: GoogleFonts.inter(color: AppColors.darkTextPrimary),
                    maxLines: 3,
                    onChanged: (v) => setState(() => _notes = v),
                  ),
                  const SizedBox(height: 32),
                  SizedBox(
                    height: 52,
                    child: ElevatedButton(
                      onPressed: _saving ? null : _submit,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.secondary,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(14),
                        ),
                      ),
                      child: Text(
                        _saving ? 'Guardando…' : 'Registrar visita',
                        style: GoogleFonts.inter(
                          fontWeight: FontWeight.w700,
                          fontSize: 16,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
    );
  }

  InputDecoration _inputDecoration(String label) {
    return InputDecoration(
      labelText: label,
      labelStyle: GoogleFonts.inter(color: AppColors.darkTextSecondary),
      filled: true,
      fillColor: AppColors.darkSurface,
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: const BorderSide(color: AppColors.darkBorder),
      ),
    );
  }
}
