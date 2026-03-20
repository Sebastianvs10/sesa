/**
 * Login — autenticación, recuperación de contraseña (dos pasos), toast y estados de carga.
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faEye, faEyeSlash, faSun, faMoon, faEnvelope, faIdCard, faArrowLeft, faKey } from '@fortawesome/free-solid-svg-icons';
import { SesaFormFieldComponent } from '../../shared/components/sesa-form-field/sesa-form-field.component';
import { AuthService } from '../../core/services/auth.service';
import { PasswordRecoveryService, PasswordRecoveryError } from '../../core/services/password-recovery.service';
import { EmpresaCurrentService } from '../../core/services/empresa-current.service';
import { PermissionsService } from '../../core/services/permissions.service';
import { ThemeService } from '../../core/services/theme.service';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';
import { SesaLoginRolePickerComponent } from '../../shared/components/sesa-login-role-picker/sesa-login-role-picker.component';
import { environment } from '../../../environments/environment';

@Component({
  standalone: true,
  selector: 'sesa-login-page',
  imports: [CommonModule, ReactiveFormsModule, FontAwesomeModule, SesaFormFieldComponent, SesaLoginRolePickerComponent],
  templateUrl: './login.page.html',
  styleUrl: './login.page.scss',
})
export class LoginPageComponent implements OnInit {
  loginForm: FormGroup;
  resetRequestForm: FormGroup;
  resetConfirmForm: FormGroup;
  loading = signal(false);
  /** Tras login multi-rol: modal de elección de perfil */
  showRolePicker = signal(false);
  rolePickerRoles = signal<string[]>([]);
  rolePickerUserName = signal('');
  rolePickerEmpresa = signal<string | undefined>(undefined);
  rolePickerSuggested = signal<string | undefined>(undefined);
  errorMessage: string | null = null;
  resetMessage: string | null = null;
  showReset = false;
  passwordVisible = false;
  resetPasswordVisible = false;
  resetConfirmPasswordVisible = false;
  /** Paso 1 = correo, 2 = código + nueva contraseña */
  recoveryStep = signal<1 | 2>(1);
  resetRequestLoading = signal(false);
  resetConfirmLoading = signal(false);
  /** Modo: 'email' = correo electrónico, 'identificacion' = número de identificación */
  loginMode: 'email' | 'identificacion' = 'email';

  /** Misma regla que backend (letra + número, mín. 8). */
  readonly passwordRecoveryPattern = /^(?=.*[A-Za-zÁÉÍÓÚáéíóúÑñ])(?=.*\d).+$/;

  faEye = faEye;
  faEyeSlash = faEyeSlash;
  faSun = faSun;
  faMoon = faMoon;
  faEnvelope = faEnvelope;
  faIdCard = faIdCard;
  faArrowLeft = faArrowLeft;
  faKey = faKey;

  themeService = inject(ThemeService);
  private readonly toast = inject(SesaToastService);

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService,
    private passwordRecovery: PasswordRecoveryService,
    private empresaCurrent: EmpresaCurrentService,
    private permissionsService: PermissionsService
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
      remember: [false],
    });
    this.resetRequestForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
    });
    this.resetConfirmForm = this.fb.group({
      token: ['', [Validators.required, Validators.maxLength(128)]],
      newPassword: [
        '',
        [Validators.required, Validators.minLength(8), Validators.pattern(this.passwordRecoveryPattern)],
      ],
      confirmPassword: ['', Validators.required],
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
        const roles = this.authService.currentRoles();
        if (roles.length > 1) {
          const u = this.authService.currentUser();
          this.rolePickerRoles.set([...roles]);
          this.rolePickerUserName.set(u?.nombreCompleto ?? '');
          this.rolePickerEmpresa.set(u?.empresaNombre);
          this.rolePickerSuggested.set(u?.rolActivo ?? u?.role);
          this.showRolePicker.set(true);
          return;
        }
        this.finishLoginAndNavigate();
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

  finishLoginAndNavigate(): void {
    this.empresaCurrent.load();
    this.permissionsService.load(this.authService.rolActivo());
    this.router.navigate(['/dashboard']);
  }

  onRolePicked(rol: string): void {
    this.authService.switchRole(rol);
    this.showRolePicker.set(false);
    this.finishLoginAndNavigate();
  }

  onRolePickerCancel(): void {
    this.showRolePicker.set(false);
    this.authService.logout();
  }

  togglePasswordVisibility(): void {
    this.passwordVisible = !this.passwordVisible;
  }

  toggleResetPasswordVisibility(): void {
    this.resetPasswordVisible = !this.resetPasswordVisible;
  }

  toggleResetConfirmPasswordVisibility(): void {
    this.resetConfirmPasswordVisible = !this.resetConfirmPasswordVisible;
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
    if (!this.showReset) {
      this.recoveryStep.set(1);
      this.resetRequestForm.reset();
      this.resetConfirmForm.reset();
    }
  }

  backToRecoveryRequest(): void {
    this.recoveryStep.set(1);
    this.resetMessage = null;
    this.errorMessage = null;
  }

  getNewPasswordFieldError(): string {
    const c = this.resetConfirmForm.get('newPassword');
    if (!c?.invalid || !c.touched) return '';
    if (c.hasError('required')) return 'La contraseña es obligatoria';
    if (c.hasError('minlength')) return 'Mínimo 8 caracteres';
    if (c.hasError('pattern')) return 'Incluye al menos una letra y un número';
    return '';
  }

  solicitarReset(): void {
    this.resetMessage = null;
    this.errorMessage = null;
    if (this.resetRequestForm.invalid) {
      this.resetRequestForm.markAllAsTouched();
      return;
    }
    const { email } = this.resetRequestForm.getRawValue();
    this.resetRequestLoading.set(true);
    this.passwordRecovery.requestCode(email).subscribe({
      next: (res) => {
        this.resetRequestLoading.set(false);
        this.resetMessage = res.message;
        this.recoveryStep.set(2);
        if (environment.passwordResetHintDevToken && res.devToken) {
          this.resetConfirmForm.patchValue({ token: res.devToken });
          this.toast.info('Código de prueba aplicado (solo desarrollo).', 'Recuperación');
        }
        this.toast.success('Siguiente paso: introduce el código y tu nueva contraseña.', 'Solicitud registrada');
      },
      error: (err: PasswordRecoveryError) => {
        this.resetRequestLoading.set(false);
        const msg = err?.message ?? 'No se pudo enviar la solicitud.';
        this.errorMessage = msg;
        this.toast.error(msg, 'Recuperación');
      },
    });
  }

  confirmarReset(): void {
    this.resetMessage = null;
    this.errorMessage = null;
    if (this.resetConfirmForm.invalid) {
      this.resetConfirmForm.markAllAsTouched();
      return;
    }
    const { token, newPassword, confirmPassword } = this.resetConfirmForm.getRawValue();
    if (newPassword !== confirmPassword) {
      this.errorMessage = 'Las contraseñas no coinciden.';
      this.toast.error(this.errorMessage, 'Validación');
      return;
    }
    this.resetConfirmLoading.set(true);
    this.passwordRecovery.confirmNewPassword(token, newPassword).subscribe({
      next: (res) => {
        this.resetConfirmLoading.set(false);
        this.resetMessage = res.message;
        this.recoveryStep.set(1);
        this.resetRequestForm.reset();
        this.resetConfirmForm.reset();
        this.showReset = false;
        this.toast.success(res.message, 'Contraseña actualizada');
      },
      error: (err: PasswordRecoveryError) => {
        this.resetConfirmLoading.set(false);
        const msg = err?.message ?? 'No se pudo actualizar la contraseña.';
        this.errorMessage = msg;
        this.toast.error(msg, 'Recuperación');
      },
    });
  }
}
