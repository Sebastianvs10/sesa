/**
 * Login — spinner en autenticación, toast errores/éxito.
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faEye, faEyeSlash, faSun, faMoon, faEnvelope, faIdCard } from '@fortawesome/free-solid-svg-icons';
import { SesaFormFieldComponent } from '../../shared/components/sesa-form-field/sesa-form-field.component';
import { AuthService } from '../../core/services/auth.service';
import { EmpresaCurrentService } from '../../core/services/empresa-current.service';
import { PermissionsService } from '../../core/services/permissions.service';
import { ThemeService } from '../../core/services/theme.service';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';

@Component({
  standalone: true,
  selector: 'sesa-login-page',
  imports: [CommonModule, ReactiveFormsModule, FontAwesomeModule, SesaFormFieldComponent],
  templateUrl: './login.page.html',
  styleUrl: './login.page.scss',
})
export class LoginPageComponent implements OnInit {
  loginForm: FormGroup;
  resetRequestForm: FormGroup;
  resetConfirmForm: FormGroup;
  loading = signal(false);
  errorMessage: string | null = null;
  resetMessage: string | null = null;
  showReset = false;
  passwordVisible = false;
  resetPasswordVisible = false;
  /** Modo: 'email' = correo electrónico, 'identificacion' = número de identificación */
  loginMode: 'email' | 'identificacion' = 'email';

  faEye = faEye;
  faEyeSlash = faEyeSlash;
  faSun = faSun;
  faMoon = faMoon;
  faEnvelope = faEnvelope;
  faIdCard = faIdCard;

  themeService = inject(ThemeService);
  private readonly toast = inject(SesaToastService);

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService,
    private empresaCurrent: EmpresaCurrentService,
    private permissionsService: PermissionsService
  ) {
    this.loginForm = this.fb.group({
      /** Acepta correo electrónico o número de identificación según loginMode */
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
      remember: [false],
    });
    this.resetRequestForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
    });
    this.resetConfirmForm = this.fb.group({
      token: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
    });
  }

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
    }
  }

  onSubmit(): void {
    this.errorMessage = null;
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }
    const { email, password } = this.loginForm.getRawValue();
    this.loading.set(true);
    this.authService.login({ email, password }).subscribe({
      next: () => {
        this.loading.set(false);
        this.empresaCurrent.load();
        // Cargar módulos del rol activo (rol primario al iniciar sesión)
        this.permissionsService.load(this.authService.rolActivo());
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        if (err.status === 401) {
          this.errorMessage = 'Correo o contraseña incorrectos.';
        } else if (err.error?.message) {
          this.errorMessage = err.error.message;
        } else {
          this.errorMessage = 'Error de conexión. Comprueba que el backend esté en ejecución.';
        }
        this.toast.error(this.errorMessage!, 'Error de acceso');
      },
    });
  }

  togglePasswordVisibility(): void {
    this.passwordVisible = !this.passwordVisible;
  }

  toggleResetPasswordVisibility(): void {
    this.resetPasswordVisible = !this.resetPasswordVisible;
  }

  getLoginFieldError(): string {
    const ctrl = this.loginForm.get('email');
    if (!ctrl?.invalid || !ctrl.touched) return '';
    if (ctrl.hasError('required')) return this.loginMode === 'email' ? 'Introduce tu correo' : 'Introduce tu número de identificación';
    if (ctrl.hasError('email')) return 'Introduce un correo válido';
    return '';
  }

  setLoginMode(mode: 'email' | 'identificacion'): void {
    this.loginMode = mode;
    const ctrl = this.loginForm.get('email');
    if (ctrl) {
      ctrl.setValue('');
      ctrl.updateValueAndValidity();
      if (mode === 'email') {
        ctrl.setValidators([Validators.required, Validators.email]);
      } else {
        ctrl.setValidators([Validators.required]);
      }
    }
  }

  toggleReset(): void {
    this.showReset = !this.showReset;
    this.resetMessage = null;
    this.errorMessage = null;
  }

  solicitarReset(): void {
    this.resetMessage = null;
    if (this.resetRequestForm.invalid) {
      this.resetRequestForm.markAllAsTouched();
      return;
    }
    const { email } = this.resetRequestForm.getRawValue();
    this.authService.requestPasswordReset({ email }).subscribe({
      next: (res) => {
        this.resetMessage = `${res.message}${res.token ? ' Token: ' + res.token : ''}`;
        this.toast.success('Solicitud enviada. Revisa tu correo.', 'Recuperación');
      },
      error: (err) => {
        this.errorMessage = err?.error?.error || 'No se pudo procesar la solicitud';
        this.toast.error(this.errorMessage!, 'Error');
      },
    });
  }

  confirmarReset(): void {
    this.resetMessage = null;
    if (this.resetConfirmForm.invalid) {
      this.resetConfirmForm.markAllAsTouched();
      return;
    }
    const { token, newPassword } = this.resetConfirmForm.getRawValue();
    this.authService.resetPassword({ token, newPassword }).subscribe({
      next: (res) => {
        this.resetMessage = res.message;
        this.toast.success('Contraseña restablecida correctamente. Inicia sesión.', 'Contraseña restablecida');
      },
      error: (err) => {
        this.errorMessage = err?.error?.error || 'No se pudo restablecer la contraseña';
        this.toast.error(this.errorMessage!, 'Error');
      },
    });
  }
}
