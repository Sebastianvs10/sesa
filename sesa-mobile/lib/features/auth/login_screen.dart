// Autor: Ing. J Sebastian Vargas S
// Pantalla de Login SESA — diseño premium glassmorphism

import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import '../../core/constants/app_colors.dart';
import '../../core/providers/auth_provider.dart';
import '../dashboard/dashboard_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen>
    with TickerProviderStateMixin {
  // ── Controllers ───────────────────────────────────────────────
  final _formKey        = GlobalKey<FormState>();
  final _identifierCtrl = TextEditingController();
  final _passwordCtrl   = TextEditingController();

  bool _useDocument   = false;
  bool _obscurePass   = true;
  bool _identifierFocused = false;
  bool _passwordFocused   = false;

  late final AnimationController _orbCtrl;
  late final AnimationController _pulseCtrl;

  @override
  void initState() {
    super.initState();
    _orbCtrl = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 8),
    )..repeat(reverse: true);
    _pulseCtrl = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 3),
    )..repeat(reverse: true);
  }

  @override
  void dispose() {
    _identifierCtrl.dispose();
    _passwordCtrl.dispose();
    _orbCtrl.dispose();
    _pulseCtrl.dispose();
    super.dispose();
  }

  // ── Submit ────────────────────────────────────────────────────
  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    final auth = context.read<AuthProvider>();
    final ok   = await auth.login(
      _identifierCtrl.text.trim(),
      _passwordCtrl.text,
    );
    if (ok && mounted) {
      Navigator.of(context).pushReplacement(
        PageRouteBuilder(
          pageBuilder: (_, __, ___) => const DashboardScreen(),
          transitionsBuilder: (_, anim, __, child) =>
              FadeTransition(opacity: anim, child: child),
          transitionDuration: const Duration(milliseconds: 500),
        ),
      );
    }
  }

  // ── Build ─────────────────────────────────────────────────────
  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.of(context).size;
    return Scaffold(
      body: Stack(
        children: [
          _BackgroundLayer(orbCtrl: _orbCtrl, size: size),
          SafeArea(
            child: Center(
              child: SingleChildScrollView(
                padding: const EdgeInsets.symmetric(
                  horizontal: 24, vertical: 32),
                child: _LoginCard(
                  formKey        : _formKey,
                  identifierCtrl : _identifierCtrl,
                  passwordCtrl   : _passwordCtrl,
                  useDocument    : _useDocument,
                  obscurePass    : _obscurePass,
                  identifierFocused: _identifierFocused,
                  passwordFocused  : _passwordFocused,
                  onToggleMode   : (v) => setState(() => _useDocument = v),
                  onTogglePass   : ()  => setState(() => _obscurePass = !_obscurePass),
                  onIdentFocus   : (v) => setState(() => _identifierFocused = v),
                  onPassFocus    : (v) => setState(() => _passwordFocused = v),
                  onSubmit       : _submit,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

// ══════════════════════════════════════════════════════════════════
// Capa de fondo animada
// ══════════════════════════════════════════════════════════════════
class _BackgroundLayer extends StatelessWidget {
  const _BackgroundLayer({required this.orbCtrl, required this.size});
  final AnimationController orbCtrl;
  final Size size;

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: orbCtrl,
      builder: (_, __) {
        final t = orbCtrl.value;
        return Container(
          width: size.width,
          height: size.height,
          decoration: const BoxDecoration(
            gradient: AppColors.loginBgGradient,
          ),
          child: Stack(
            children: [
              // Orbe azul — arriba izquierda
              Positioned(
                top: -60 + 30 * math.sin(t * math.pi),
                left: -80 + 20 * math.cos(t * math.pi),
                child: _Orb(
                  size: size.width * 0.75,
                  color: AppColors.secondary.withValues(alpha: 0.18),
                  blur: 80,
                ),
              ),
              // Orbe verde — abajo derecha
              Positioned(
                bottom: -40 + 25 * math.cos(t * math.pi),
                right: -60 + 15 * math.sin(t * math.pi),
                child: _Orb(
                  size: size.width * 0.65,
                  color: AppColors.accent.withValues(alpha: 0.14),
                  blur: 70,
                ),
              ),
              // Orbe pequeño — centro
              Positioned(
                top: size.height * 0.35 + 20 * math.sin(t * 2 * math.pi),
                left: size.width * 0.1,
                child: _Orb(
                  size: size.width * 0.35,
                  color: AppColors.secondary.withValues(alpha: 0.08),
                  blur: 50,
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}

class _Orb extends StatelessWidget {
  const _Orb({required this.size, required this.color, required this.blur});
  final double size;
  final Color  color;
  final double blur;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: color,
        boxShadow: [BoxShadow(color: color, blurRadius: blur, spreadRadius: blur / 3)],
      ),
    );
  }
}

// ══════════════════════════════════════════════════════════════════
// Tarjeta de login glassmorphism
// ══════════════════════════════════════════════════════════════════
class _LoginCard extends StatelessWidget {
  const _LoginCard({
    required this.formKey,
    required this.identifierCtrl,
    required this.passwordCtrl,
    required this.useDocument,
    required this.obscurePass,
    required this.identifierFocused,
    required this.passwordFocused,
    required this.onToggleMode,
    required this.onTogglePass,
    required this.onIdentFocus,
    required this.onPassFocus,
    required this.onSubmit,
  });

  final GlobalKey<FormState> formKey;
  final TextEditingController identifierCtrl;
  final TextEditingController passwordCtrl;
  final bool useDocument;
  final bool obscurePass;
  final bool identifierFocused;
  final bool passwordFocused;
  final ValueChanged<bool> onToggleMode;
  final VoidCallback onTogglePass;
  final ValueChanged<bool> onIdentFocus;
  final ValueChanged<bool> onPassFocus;
  final VoidCallback onSubmit;

  @override
  Widget build(BuildContext context) {
    return ConstrainedBox(
      constraints: const BoxConstraints(maxWidth: 420),
      child: Container(
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(28),
          color: const Color(0xFF0F172A).withValues(alpha: 0.55),
          border: Border.all(
            color: AppColors.accent.withValues(alpha: 0.15),
            width: 1,
          ),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.45),
              blurRadius: 60,
              offset: const Offset(0, 20),
            ),
          ],
        ),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(28),
          child: Padding(
            padding: const EdgeInsets.all(36),
            child: Form(
              key: formKey,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  _Logo(),
                  const SizedBox(height: 32),
                  _ModeToggle(
                    useDocument: useDocument,
                    onToggle: onToggleMode,
                  ),
                  const SizedBox(height: 24),
                  _IdentifierField(
                    controller: identifierCtrl,
                    useDocument: useDocument,
                    isFocused: identifierFocused,
                    onFocusChange: onIdentFocus,
                  ),
                  const SizedBox(height: 16),
                  _PasswordField(
                    controller: passwordCtrl,
                    obscure: obscurePass,
                    isFocused: passwordFocused,
                    onToggle: onTogglePass,
                    onFocusChange: onPassFocus,
                  ),
                  const SizedBox(height: 8),
                  _ForgotPassword(),
                  const SizedBox(height: 28),
                  _SubmitButton(onSubmit: onSubmit),
                  const SizedBox(height: 20),
                  _ErrorBanner(),
                ],
              ),
            ),
          ),
        ),
      )
          .animate()
          .fadeIn(duration: 600.ms, curve: Curves.easeOut)
          .slideY(begin: 0.08, end: 0, duration: 600.ms, curve: Curves.easeOut),
    );
  }
}

// ── Logo ─────────────────────────────────────────────────────────
class _Logo extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Container(
          width: 72,
          height: 72,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            gradient: AppColors.primaryGradient,
            boxShadow: [
              BoxShadow(
                color: AppColors.secondary.withValues(alpha: 0.45),
                blurRadius: 24,
                spreadRadius: 4,
              ),
            ],
          ),
          child: Center(
            child: Text(
              'S',
              style: GoogleFonts.inter(
                fontSize: 36,
                fontWeight: FontWeight.w900,
                color: Colors.white,
              ),
            ),
          ),
        )
            .animate(onPlay: (c) => c.repeat(reverse: true))
            .shimmer(duration: 3000.ms, color: Colors.white.withValues(alpha: 0.15)),
        const SizedBox(height: 16),
        Text(
          'SESA',
          style: GoogleFonts.inter(
            fontSize: 28,
            fontWeight: FontWeight.w800,
            color: Colors.white,
            letterSpacing: 3,
          ),
        ),
        const SizedBox(height: 6),
        Text(
          'Sistema Electrónico de Salud',
          style: GoogleFonts.inter(
            fontSize: 13,
            fontWeight: FontWeight.w400,
            color: AppColors.darkTextSecondary,
            letterSpacing: 0.5,
          ),
        ),
      ],
    );
  }
}

// ── Toggle email / documento ──────────────────────────────────────
class _ModeToggle extends StatelessWidget {
  const _ModeToggle({required this.useDocument, required this.onToggle});
  final bool useDocument;
  final ValueChanged<bool> onToggle;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 48,
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.07),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: AppColors.darkBorder.withValues(alpha: 0.5)),
      ),
      child: Row(
        children: [
          _Tab(
            label: 'Correo',
            icon: Icons.email_rounded,
            active: !useDocument,
            onTap: () => onToggle(false),
          ),
          _Tab(
            label: 'Documento',
            icon: Icons.badge_rounded,
            active: useDocument,
            onTap: () => onToggle(true),
          ),
        ],
      ),
    );
  }
}

class _Tab extends StatelessWidget {
  const _Tab({
    required this.label,
    required this.icon,
    required this.active,
    required this.onTap,
  });
  final String   label;
  final IconData icon;
  final bool     active;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: GestureDetector(
        onTap: onTap,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 250),
          curve: Curves.easeInOut,
          height: double.infinity,
          decoration: BoxDecoration(
            gradient: active ? AppColors.primaryGradient : null,
            borderRadius: BorderRadius.circular(10),
            boxShadow: active
                ? [BoxShadow(
                    color: AppColors.secondary.withValues(alpha: 0.4),
                    blurRadius: 12,
                    spreadRadius: 0,
                  )]
                : null,
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(icon,
                  size: 16,
                  color: active ? Colors.white : AppColors.darkTextSecondary),
              const SizedBox(width: 6),
              Text(
                label,
                style: GoogleFonts.inter(
                  fontSize: 13,
                  fontWeight: active ? FontWeight.w600 : FontWeight.w400,
                  color: active ? Colors.white : AppColors.darkTextSecondary,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// ── Campo identificador ───────────────────────────────────────────
class _IdentifierField extends StatelessWidget {
  const _IdentifierField({
    required this.controller,
    required this.useDocument,
    required this.isFocused,
    required this.onFocusChange,
  });
  final TextEditingController controller;
  final bool useDocument;
  final bool isFocused;
  final ValueChanged<bool> onFocusChange;

  @override
  Widget build(BuildContext context) {
    return Focus(
      onFocusChange: onFocusChange,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(16),
          boxShadow: isFocused
              ? [BoxShadow(
                  color: AppColors.secondary.withValues(alpha: 0.35),
                  blurRadius: 16,
                  spreadRadius: 0,
                )]
              : [],
        ),
        child: TextFormField(
          controller: controller,
          keyboardType: useDocument
              ? TextInputType.number
              : TextInputType.emailAddress,
          style: GoogleFonts.inter(color: Colors.white, fontSize: 15),
          decoration: InputDecoration(
            hintText: useDocument
                ? 'Número de documento'
                : 'Correo electrónico',
            hintStyle: GoogleFonts.inter(
              color: AppColors.darkTextMuted,
              fontSize: 14,
            ),
            prefixIcon: Icon(
              useDocument ? Icons.badge_rounded : Icons.email_rounded,
              color: isFocused ? AppColors.accent : AppColors.darkTextMuted,
              size: 20,
            ),
            filled: true,
            fillColor: Colors.white.withValues(alpha: 0.06),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(16),
              borderSide: BorderSide(
                color: AppColors.darkBorder.withValues(alpha: 0.5),
              ),
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(16),
              borderSide: BorderSide(
                color: AppColors.darkBorder.withValues(alpha: 0.5),
              ),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(16),
              borderSide: const BorderSide(
                color: AppColors.accent, width: 2,
              ),
            ),
          ),
          validator: (v) {
            if (v == null || v.trim().isEmpty) {
              return useDocument
                  ? 'Ingresa tu documento'
                  : 'Ingresa tu correo';
            }
            if (!useDocument &&
                !RegExp(r'^[\w.+\-]+@[a-zA-Z\d\-]+(\.[a-zA-Z\d\-]+)+$')
                    .hasMatch(v.trim())) {
              return 'Correo inválido';
            }
            return null;
          },
        ),
      ),
    );
  }
}

// ── Campo contraseña ──────────────────────────────────────────────
class _PasswordField extends StatelessWidget {
  const _PasswordField({
    required this.controller,
    required this.obscure,
    required this.isFocused,
    required this.onToggle,
    required this.onFocusChange,
  });
  final TextEditingController controller;
  final bool obscure;
  final bool isFocused;
  final VoidCallback onToggle;
  final ValueChanged<bool> onFocusChange;

  @override
  Widget build(BuildContext context) {
    return Focus(
      onFocusChange: onFocusChange,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(16),
          boxShadow: isFocused
              ? [BoxShadow(
                  color: AppColors.secondary.withValues(alpha: 0.35),
                  blurRadius: 16,
                )]
              : [],
        ),
        child: TextFormField(
          controller: controller,
          obscureText: obscure,
          style: GoogleFonts.inter(color: Colors.white, fontSize: 15),
          decoration: InputDecoration(
            hintText: 'Contraseña',
            hintStyle: GoogleFonts.inter(
              color: AppColors.darkTextMuted, fontSize: 14,
            ),
            prefixIcon: Icon(
              Icons.lock_rounded,
              color: isFocused ? AppColors.accent : AppColors.darkTextMuted,
              size: 20,
            ),
            suffixIcon: IconButton(
              icon: Icon(
                obscure ? Icons.visibility_rounded : Icons.visibility_off_rounded,
                color: AppColors.darkTextMuted, size: 20,
              ),
              onPressed: onToggle,
            ),
            filled: true,
            fillColor: Colors.white.withValues(alpha: 0.06),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(16),
              borderSide: BorderSide(
                color: AppColors.darkBorder.withValues(alpha: 0.5),
              ),
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(16),
              borderSide: BorderSide(
                color: AppColors.darkBorder.withValues(alpha: 0.5),
              ),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(16),
              borderSide: const BorderSide(
                color: AppColors.accent, width: 2,
              ),
            ),
          ),
          validator: (v) {
            if (v == null || v.isEmpty) return 'Ingresa tu contraseña';
            if (v.length < 6) return 'Mínimo 6 caracteres';
            return null;
          },
        ),
      ),
    );
  }
}

// ── Olvidé contraseña ─────────────────────────────────────────────
class _ForgotPassword extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.centerRight,
      child: TextButton(
        style: TextButton.styleFrom(
          padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 4),
        ),
        onPressed: () {
          // TODO: navegar a recuperar contraseña
        },
        child: Text(
          '¿Olvidaste tu contraseña?',
          style: GoogleFonts.inter(
            fontSize: 13,
            color: AppColors.accent,
            fontWeight: FontWeight.w500,
          ),
        ),
      ),
    );
  }
}

// ── Botón de ingreso ──────────────────────────────────────────────
class _SubmitButton extends StatelessWidget {
  const _SubmitButton({required this.onSubmit});
  final VoidCallback onSubmit;

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthProvider>();
    return SizedBox(
      width: double.infinity,
      height: 54,
      child: DecoratedBox(
        decoration: BoxDecoration(
          gradient: auth.isLoading ? null : AppColors.primaryGradient,
          color: auth.isLoading ? AppColors.darkSurfaceHover : null,
          borderRadius: BorderRadius.circular(16),
          boxShadow: auth.isLoading
              ? []
              : [
                  BoxShadow(
                    color: AppColors.secondary.withValues(alpha: 0.5),
                    blurRadius: 20,
                    offset: const Offset(0, 6),
                  ),
                ],
        ),
        child: ElevatedButton(
          style: ElevatedButton.styleFrom(
            backgroundColor: Colors.transparent,
            shadowColor: Colors.transparent,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
            ),
          ),
          onPressed: auth.isLoading ? null : onSubmit,
          child: auth.isLoading
              ? const SizedBox(
                  width: 22,
                  height: 22,
                  child: CircularProgressIndicator(
                    color: Colors.white,
                    strokeWidth: 2.5,
                  ),
                )
              : Text(
                  'Ingresar',
                  style: GoogleFonts.inter(
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                    color: Colors.white,
                    letterSpacing: 0.5,
                  ),
                ),
        ),
      ),
    );
  }
}

// ── Banner de error ───────────────────────────────────────────────
class _ErrorBanner extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final error = context.watch<AuthProvider>().errorMessage;
    if (error == null) return const SizedBox.shrink();
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: AppColors.error.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.error.withValues(alpha: 0.4)),
      ),
      child: Row(
        children: [
          const Icon(Icons.error_outline_rounded,
              color: AppColors.error, size: 18),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              error,
              style: GoogleFonts.inter(
                fontSize: 13,
                color: AppColors.error,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
        ],
      ),
    )
        .animate()
        .fadeIn(duration: 300.ms)
        .slideY(begin: -0.1, end: 0, duration: 300.ms);
  }
}
