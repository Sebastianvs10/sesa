// Autor: Ing. J Sebastian Vargas S
// EBS Crear territorio — formulario básico

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../core/constants/app_colors.dart';
import '../../core/providers/auth_provider.dart';
import '../../core/services/ebs_service.dart';

class EbsTerritorioCrearScreen extends StatefulWidget {
  const EbsTerritorioCrearScreen({super.key, required this.ebsService});
  final EbsService ebsService;

  @override
  State<EbsTerritorioCrearScreen> createState() =>
      _EbsTerritorioCrearScreenState();
}

class _EbsTerritorioCrearScreenState extends State<EbsTerritorioCrearScreen> {
  final _codeController = TextEditingController();
  final _nameController = TextEditingController();
  String _type = 'MICROTERRITORIO';
  bool _saving = false;

  @override
  void dispose() {
    _codeController.dispose();
    _nameController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final code = _codeController.text.trim();
    final name = _nameController.text.trim();
    if (code.isEmpty || name.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Código y nombre son requeridos'),
          backgroundColor: AppColors.warning,
        ),
      );
      return;
    }
    final token = context.read<AuthProvider>().user?.accessToken;
    if (token == null) return;
    setState(() => _saving = true);
    try {
      final ok = await widget.ebsService.createTerritory(
        token,
        {'code': code, 'name': name, 'type': _type},
      );
      if (mounted) {
        setState(() => _saving = false);
        if (ok) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('Territorio creado'),
              backgroundColor: AppColors.success,
            ),
          );
          Navigator.pop(context);
        } else {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('Error al crear'),
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
          'Nuevo territorio',
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
      body: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(20, 16, 20, 32),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            TextField(
              controller: _codeController,
              decoration: _decoration('Código *'),
              style: GoogleFonts.inter(color: AppColors.darkTextPrimary),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _nameController,
              decoration: _decoration('Nombre *'),
              style: GoogleFonts.inter(color: AppColors.darkTextPrimary),
            ),
            const SizedBox(height: 16),
            DropdownButtonFormField<String>(
              value: _type,
              decoration: _decoration('Tipo'),
              dropdownColor: AppColors.darkSurface,
              items: const [
                DropdownMenuItem(
                    value: 'MICROTERRITORIO',
                    child: Text('Microterritorio')),
                DropdownMenuItem(value: 'VEREDA', child: Text('Vereda')),
                DropdownMenuItem(
                    value: 'CORREGIMIENTO',
                    child: Text('Corregimiento')),
              ],
              onChanged: (v) => setState(() => _type = v ?? _type),
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
                  _saving ? 'Creando…' : 'Crear territorio',
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

  InputDecoration _decoration(String label) {
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
