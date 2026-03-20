// Autor: Ing. J Sebastian Vargas S
// Dashboard SESA — acceso por módulo según rol del usuario

import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import '../../core/constants/app_colors.dart';
import '../../core/providers/auth_provider.dart';
import '../../shared/widgets/module_card.dart';
import '../../shared/widgets/stat_card.dart';
import '../auth/login_screen.dart';

class DashboardScreen extends StatefulWidget {
  const DashboardScreen({super.key});
  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<AuthProvider>().loadResumen();
    });
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthProvider>();
    final user = auth.user!;

    return Scaffold(
      backgroundColor: AppColors.darkBg,
      body: CustomScrollView(
        slivers: [
          _SesaAppBar(user: user, onLogout: _confirmLogout),
          SliverToBoxAdapter(
            child: _StatsSection(resumen: auth.resumen, role: user.role),
          ),
          SliverPadding(
            padding: const EdgeInsets.fromLTRB(20, 0, 20, 32),
            sliver: _ModulesGrid(modules: auth.modules),
          ),
        ],
      ),
    );
  }

  void _confirmLogout() {
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        backgroundColor: AppColors.darkSurface,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: Text(
          'Cerrar sesión',
          style: GoogleFonts.inter(
            fontWeight: FontWeight.w700, color: Colors.white,
          ),
        ),
        content: Text(
          '¿Estás seguro que deseas salir?',
          style: GoogleFonts.inter(color: AppColors.darkTextSecondary),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text('Cancelar',
                style: GoogleFonts.inter(color: AppColors.darkTextSecondary)),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.error,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(10),
              ),
            ),
            onPressed: () async {
              Navigator.pop(context);
              await context.read<AuthProvider>().logout();
              if (mounted) {
                Navigator.of(context).pushReplacement(
                  PageRouteBuilder(
                    pageBuilder: (_, __, ___) => const LoginScreen(),
                    transitionsBuilder: (_, anim, __, child) =>
                        FadeTransition(opacity: anim, child: child),
                    transitionDuration: const Duration(milliseconds: 400),
                  ),
                );
              }
            },
            child: Text('Salir',
                style: GoogleFonts.inter(
                    color: Colors.white, fontWeight: FontWeight.w600)),
          ),
        ],
      ),
    );
  }
}

// ══════════════════════════════════════════════════════════════════
// AppBar personalizado con gradiente
// ══════════════════════════════════════════════════════════════════
class _SesaAppBar extends StatelessWidget {
  const _SesaAppBar({required this.user, required this.onLogout});
  final dynamic user;
  final VoidCallback onLogout;

  @override
  Widget build(BuildContext context) {
    return SliverAppBar(
      expandedHeight: 190,
      floating: false,
      pinned: true,
      backgroundColor: AppColors.darkBg,
      elevation: 0,
      automaticallyImplyLeading: false,
      flexibleSpace: FlexibleSpaceBar(
        background: Container(
          decoration: const BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [Color(0xFF0A2E4F), Color(0xFF1F6AE1), Color(0xFF2BB0A6)],
            ),
          ),
          child: SafeArea(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(24, 12, 24, 20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      // Logo + nombre empresa
                      Row(
                        children: [
                          Container(
                            width: 40,
                            height: 40,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              color: Colors.white.withValues(alpha: 0.15),
                              border: Border.all(
                                color: Colors.white.withValues(alpha: 0.25),
                              ),
                            ),
                            child: Center(
                              child: Text(
                                'S',
                                style: GoogleFonts.inter(
                                  fontSize: 20,
                                  fontWeight: FontWeight.w900,
                                  color: Colors.white,
                                ),
                              ),
                            ),
                          ),
                          const SizedBox(width: 10),
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'SESA',
                                style: GoogleFonts.inter(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w800,
                                  color: Colors.white,
                                  letterSpacing: 2,
                                ),
                              ),
                              Text(
                                user.empresaNombre,
                                style: GoogleFonts.inter(
                                  fontSize: 11,
                                  color: Colors.white.withValues(alpha: 0.75),
                                  fontWeight: FontWeight.w400,
                                ),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ],
                          ),
                        ],
                      ),
                      // Botón logout
                      IconButton(
                        onPressed: onLogout,
                        icon: Container(
                          padding: const EdgeInsets.all(8),
                          decoration: BoxDecoration(
                            color: Colors.white.withValues(alpha: 0.12),
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: const Icon(
                            Icons.logout_rounded,
                            color: Colors.white,
                            size: 18,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 20),
                  // Saludo + rol
                  Text(
                    _greeting(),
                    style: GoogleFonts.inter(
                      fontSize: 13,
                      color: Colors.white.withValues(alpha: 0.75),
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    user.nombreCompleto,
                    style: GoogleFonts.inter(
                      fontSize: 22,
                      fontWeight: FontWeight.w800,
                      color: Colors.white,
                    ),
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: 8),
                  _RoleBadge(role: user.role),
                ],
              ),
            ),
          ),
        ),
      ),
      title: Text(
        'SESA',
        style: GoogleFonts.inter(
          fontWeight: FontWeight.w800,
          color: Colors.white,
          letterSpacing: 2,
        ),
      ),
    );
  }

  String _greeting() {
    final h = DateTime.now().hour;
    if (h < 12) return 'Buenos días,';
    if (h < 18) return 'Buenas tardes,';
    return 'Buenas noches,';
  }
}

class _RoleBadge extends StatelessWidget {
  const _RoleBadge({required this.role});
  final String role;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: Colors.white.withValues(alpha: 0.25)),
      ),
      child: Text(
        _roleLabel(role),
        style: GoogleFonts.inter(
          fontSize: 12,
          fontWeight: FontWeight.w600,
          color: Colors.white,
        ),
      ),
    );
  }

  String _roleLabel(String code) {
    const labels = {
      'SUPERADMINISTRADOR' : 'Super Usuario',
      'ADMIN'              : 'Administrador',
      'MEDICO'             : 'Médico',
      'ODONTOLOGO'         : 'Odontólogo/a',
      'BACTERIOLOGO'       : 'Bacteriólogo',
      'ENFERMERO'          : 'Enfermero/a',
      'JEFE_ENFERMERIA'    : 'Jefe de Enfermería',
      'AUXILIAR_ENFERMERIA': 'Auxiliar de Enfermería',
      'PSICOLOGO'          : 'Psicólogo/a',
      'REGENTE_FARMACIA'   : 'Regente de Farmacia',
      'RECEPCIONISTA'      : 'Recepcionista',
    };
    return labels[code] ?? code;
  }
}

// ══════════════════════════════════════════════════════════════════
// Sección de estadísticas
// ══════════════════════════════════════════════════════════════════
class _StatsSection extends StatelessWidget {
  const _StatsSection({required this.resumen, required this.role});
  final Map<String, dynamic> resumen;
  final String role;

  @override
  Widget build(BuildContext context) {
    final stats = _buildStats();
    if (stats.isEmpty) return const SizedBox(height: 16);
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 24, 0, 24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.only(right: 20, bottom: 14),
            child: Text(
              'Resumen',
              style: GoogleFonts.inter(
                fontSize: 18,
                fontWeight: FontWeight.w700,
                color: Colors.white,
              ),
            ),
          ),
          SizedBox(
            height: 110,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              itemCount: stats.length,
              separatorBuilder: (_, __) => const SizedBox(width: 12),
              itemBuilder: (ctx, i) => StatCard(
                label: stats[i].$1,
                value: stats[i].$2,
                icon: stats[i].$3,
                gradient: stats[i].$4,
                index: i,
              ),
            ),
          ),
        ],
      ),
    );
  }

  List<(String, String, IconData, List<Color>)> _buildStats() {
    final out = <(String, String, IconData, List<Color>)>[];
    final showAll = {'SUPERADMINISTRADOR', 'ADMIN'}.contains(role);
    final showCitas = showAll ||
        {'MEDICO','ODONTOLOGO','ENFERMERO','JEFE_ENFERMERIA','AUXILIAR_ENFERMERIA',
         'RECEPCIONISTA','PSICOLOGO'}.contains(role);
    final showPacientes = showCitas || role == 'REGENTE_FARMACIA' || role == 'BACTERIOLOGO';

    if (showPacientes && resumen.containsKey('totalPacientes')) {
      out.add(('Pacientes', '${resumen['totalPacientes'] ?? 0}',
          Icons.people_rounded,
          const [Color(0xFF0EA5E9), Color(0xFF06B6D4)]));
    }
    if (showCitas && resumen.containsKey('totalCitas')) {
      out.add(('Citas', '${resumen['totalCitas'] ?? 0}',
          Icons.calendar_month_rounded,
          const [Color(0xFF1F6AE1), Color(0xFF38BDF8)]));
    }
    if (showAll && resumen.containsKey('totalConsultas')) {
      out.add(('Consultas', '${resumen['totalConsultas'] ?? 0}',
          Icons.article_rounded,
          const [Color(0xFF6366F1), Color(0xFF8B5CF6)]));
    }
    if (showAll && resumen.containsKey('totalFacturado')) {
      final val = resumen['totalFacturado'];
      final label = val == null ? '—' : '\$${_formatNum(val)}';
      out.add(('Facturado', label,
          Icons.payments_rounded,
          const [Color(0xFF22C55E), Color(0xFF16A34A)]));
    }
    return out;
  }

  String _formatNum(dynamic v) {
    final n = (v as num).toInt();
    if (n >= 1000000) return '${(n / 1000000).toStringAsFixed(1)}M';
    if (n >= 1000) return '${(n / 1000).toStringAsFixed(0)}k';
    return '$n';
  }
}

// ══════════════════════════════════════════════════════════════════
// Grid de módulos
// ══════════════════════════════════════════════════════════════════
class _ModulesGrid extends StatelessWidget {
  const _ModulesGrid({required this.modules});
  final List<String> modules;

  @override
  Widget build(BuildContext context) {
    final visible = modules
        .where((m) => m != 'DASHBOARD')
        .toList();

    return SliverGrid(
      delegate: SliverChildBuilderDelegate(
        (ctx, i) => ModuleCard(code: visible[i], index: i),
        childCount: visible.length,
      ),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        mainAxisSpacing: 14,
        crossAxisSpacing: 14,
        childAspectRatio: 1.18,
      ),
    );
  }
}
