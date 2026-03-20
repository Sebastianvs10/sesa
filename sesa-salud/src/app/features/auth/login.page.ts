/**
 * Login — autenticación, toast y estados de carga; recuperación en modal dedicado.
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, OnDestroy, OnInit, inject, signal, viewChild } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faEye, faEyeSlash, faSun, faMoon, faEnvelope, faIdCard } from '@fortawesome/free-solid-svg-icons';
import { SesaFormFieldComponent } from '../../shared/components/sesa-form-field/sesa-form-field.component';
import { AuthService } from '../../core/services/auth.service';
import { EmpresaCurrentService } from '../../core/services/empresa-current.service';
import { PermissionsService } from '../../core/services/permissions.service';
import { ThemeService } from '../../core/services/theme.service';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';
import { SesaLoginRolePickerComponent } from '../../shared/components/sesa-login-role-picker/sesa-login-role-picker.component';
import { SesaPasswordRecoveryModalComponent } from '../../shared/components/sesa-password-recovery-modal/sesa-password-recovery-modal.component';

@Component({
  standalone: true,
  selector: 'sesa-login-page',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FontAwesomeModule,
    SesaFormFieldComponent,
    SesaLoginRolePickerComponent,
    SesaPasswordRecoveryModalComponent,
  ],
  templateUrl: './login.page.html',
  styleUrl: './login.page.scss',
})
export class LoginPageComponent implements OnInit, OnDestroy {
  loginForm: FormGroup;
  loading = signal(false);
  /** Tras login multi-rol: modal de elección de perfil */
  showRolePicker = signal(false);
  rolePickerRoles = signal<string[]>([]);
  rolePickerUserName = signal('');
  rolePickerEmpresa = signal<string | undefined>(undefined);
  rolePickerSuggested = signal<string | undefined>(undefined);
  errorMessage: string | null = null;
  /** Modal premium de recuperación de contraseña */
  recoveryModalOpen = signal(false);
  passwordVisible = false;
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

  /** Referencia al enlace que abre el modal — se re-enfoca al cerrar (ciclo de foco accesible). */
  private readonly forgotPwdLinkRef = viewChild<ElementRef<HTMLElement>>('forgotPwdLink');

  private readonly formSubs = new Subscription();

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService,
    private empresaCurrent: EmpresaCurrentService,
    private permissionsService: PermissionsService
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
      remember: [false],
    });
  }

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
    }
    this.formSubs.add(
      this.loginForm.valueChanges.subscribe(() => {
        if (this.errorMessage) {
          this.errorMessage = null;
        }
      }),
    );
  }

  ngOnDestroy(): void {
    this.formSubs.unsubscribe();
  }

  openRecoveryModal(): void {
    this.errorMessage = null;
    this.recoveryModalOpen.set(true);
  }

  closeRecoveryModal(): void {
    this.recoveryModalOpen.set(false);
    setTimeout(() => this.forgotPwdLinkRef()?.nativeElement?.focus(), 0);
  }

  onSubmit(): void {
    this.errorMessage = null;
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      this.errorMessage = 'Completa correo o identificación y contraseña correctamente.';
      return;
    }
    const { email, password } = this.loginForm.getRawValue();
    this.loading.set(true);
    this.authService.login({ email, password }).subscribe({
      next: () => {
        this.loading.set(false);
        this.errorMessage = null;
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
      error: (err: unknown) => {
        this.loading.set(false);
        const httpErr = err instanceof HttpErrorResponse ? err : null;
        this.errorMessage = httpErr ? this.resolveLoginError(httpErr) : 'No se pudo iniciar sesión. Vuelve a intentarlo.';
        this.toast.error(this.errorMessage, 'Error de acceso');
      },
    });
  }

  /** Mensaje claro según respuesta HTTP o fallo de red. */
  private resolveLoginError(err: HttpErrorResponse): string {
    if (err.status === 401) {
      const api401 = err.error?.message ?? err.error?.error;
      if (typeof api401 === 'string' && api401.trim().length > 0) {
        return api401.trim();
      }
      return 'Correo o contraseña incorrectos. Verifica tus datos o usa «¿Olvidaste tu contraseña?».';
    }
    if (err.status === 403) {
      return 'Acceso denegado. Tu cuenta podría estar deshabilitada; contacta al administrador.';
    }
    if (err.status === 429) {
      return 'Demasiados intentos. Espera un momento e inténtalo de nuevo.';
    }
    if (err.status === 502 || err.status === 503) {
      return 'El servicio no está disponible temporalmente. Intenta de nuevo en unos minutos.';
    }
    if (err.status === 0) {
      return 'No hay conexión con el servidor. Comprueba tu red y que la API esté en ejecución.';
    }
    const apiMsg = err.error?.message ?? err.error?.error;
    if (typeof apiMsg === 'string' && apiMsg.trim().length > 0) {
      return apiMsg.trim();
    }
    if (err.status >= 500) {
      return 'Error en el servidor. Intenta de nuevo más tarde.';
    }
    return 'No se pudo iniciar sesión. Vuelve a intentarlo.';
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

  getLoginFieldError(): string {
    const ctrl = this.loginForm.get('email');
    if (!ctrl?.invalid || !ctrl.touched) return '';
    if (ctrl.hasError('required'))
      return this.loginMode === 'email' ? 'Introduce tu correo' : 'Introduce tu número de identificación';
    if (ctrl.hasError('email')) return 'Introduce un correo válido';
    return '';
  }

  setLoginMode(mode: 'email' | 'identificacion'): void {
    this.errorMessage = null;
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
}
