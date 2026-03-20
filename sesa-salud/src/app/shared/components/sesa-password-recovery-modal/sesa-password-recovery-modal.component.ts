/**
 * Modal premium de recuperación de contraseña (dos pasos, accesible).
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule, DOCUMENT } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  OnDestroy,
  OnInit,
  afterNextRender,
  inject,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faArrowLeft,
  faEye,
  faEyeSlash,
  faKey,
  faXmark,
} from '@fortawesome/free-solid-svg-icons';
import { environment } from '../../../../environments/environment';
import { PasswordRecoveryService, PasswordRecoveryError } from '../../../core/services/password-recovery.service';
import { SesaFormFieldComponent } from '../sesa-form-field/sesa-form-field.component';
import { SesaToastService } from '../sesa-toast/sesa-toast.component';

const BODY_LOCK_CLASS = 'sesa-modal-body-lock';

@Component({
  selector: 'sesa-password-recovery-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FontAwesomeModule, SesaFormFieldComponent],
  templateUrl: './sesa-password-recovery-modal.component.html',
  styleUrl: './sesa-password-recovery-modal.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'sesa-password-recovery-host',
  },
})
export class SesaPasswordRecoveryModalComponent implements OnInit, OnDestroy {
  readonly closed = output<void>();

  private readonly doc = inject(DOCUMENT);
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly fb = inject(FormBuilder);
  private readonly passwordRecovery = inject(PasswordRecoveryService);
  private readonly toast = inject(SesaToastService);

  private readonly panelRef = viewChild<ElementRef<HTMLElement>>('panel');

  /** Posición vertical guardada al bloquear scroll (restaurar al destruir). */
  private bodyScrollY = 0;

  resetRequestForm: FormGroup;
  resetConfirmForm: FormGroup;
  recoveryStep = signal<1 | 2>(1);
  resetRequestLoading = signal(false);
  resetConfirmLoading = signal(false);
  resetMessage = signal<string | null>(null);
  recoveryError = signal<string | null>(null);
  resetPasswordVisible = signal(false);
  resetConfirmPasswordVisible = signal(false);

  readonly passwordRecoveryPattern = /^(?=.*[A-Za-zÁÉÍÓÚáéíóúÑñ])(?=.*\d).+$/;

  faEye = faEye;
  faEyeSlash = faEyeSlash;
  faKey = faKey;
  faArrowLeft = faArrowLeft;
  faXmark = faXmark;

  constructor() {
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

    afterNextRender(() => {
      this.focusInitialField();
    });
  }

  ngOnInit(): void {
    this.lockBodyScroll();
  }

  ngOnDestroy(): void {
    this.unlockBodyScroll();
  }

  /**
   * Elementos enfocables dentro del diálogo (orden DOM), para trampa de foco.
   */
  private getTabbable(panel: HTMLElement): HTMLElement[] {
    const win = this.doc.defaultView;
    const candidates = Array.from(
      panel.querySelectorAll<HTMLElement>(
        'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]',
      ),
    );
    const out: HTMLElement[] = [];
    for (const el of candidates) {
      if (el.matches('input[type="hidden"]')) continue;
      if (el.tabIndex < 0) continue;
      if (el.getAttribute('aria-hidden') === 'true') continue;
      if (el.closest('[aria-hidden="true"]')) continue;
      if (win) {
        const st = win.getComputedStyle(el);
        if (st.visibility === 'hidden' || st.display === 'none') continue;
      }
      out.push(el);
    }
    return out;
  }

  private focusInitialField(): void {
    const root = this.panelRef()?.nativeElement;
    if (!root) return;
    if (this.recoveryStep() === 1) {
      root.querySelector<HTMLElement>('input[type="email"]')?.focus();
    } else {
      root.querySelector<HTMLElement>('input[autocomplete="one-time-code"]')?.focus();
    }
  }

  private lockBodyScroll(): void {
    const win = this.doc.defaultView;
    if (!win) return;
    this.bodyScrollY = win.scrollY || this.doc.documentElement.scrollTop || 0;
    this.doc.body.classList.add(BODY_LOCK_CLASS);
    this.doc.body.style.top = `-${this.bodyScrollY}px`;
  }

  private unlockBodyScroll(): void {
    this.doc.body.classList.remove(BODY_LOCK_CLASS);
    this.doc.body.style.top = '';
    const win = this.doc.defaultView;
    if (win) {
      win.scrollTo(0, this.bodyScrollY);
    }
  }

  @HostListener('document:keydown', ['$event'])
  onDocumentKeydown(e: KeyboardEvent): void {
    if (!this.host.nativeElement.isConnected) return;

    if (e.key === 'Escape') {
      e.preventDefault();
      this.dismiss();
      return;
    }

    if (e.key !== 'Tab') return;

    const panel = this.panelRef()?.nativeElement;
    if (!panel) return;

    const list = this.getTabbable(panel);
    if (list.length === 0) return;

    const active = this.doc.activeElement as HTMLElement | null;

    if (!active || !panel.contains(active)) {
      e.preventDefault();
      (e.shiftKey ? list[list.length - 1] : list[0])?.focus();
      return;
    }

    const ix = list.indexOf(active);
    if (ix < 0) return;

    if (e.shiftKey) {
      if (ix === 0) {
        e.preventDefault();
        list[list.length - 1]?.focus();
      }
    } else if (ix === list.length - 1) {
      e.preventDefault();
      list[0]?.focus();
    }
  }

  /**
   * Si el foco entra al host pero fuera del panel (no debería), o escapa del panel, devolverlo dentro.
   */
  @HostListener('document:focusin', ['$event'])
  onDocumentFocusIn(e: FocusEvent): void {
    const panel = this.panelRef()?.nativeElement;
    if (!panel) return;
    const host = this.host.nativeElement;
    const target = e.target as Node | null;
    if (!target || !host.contains(target)) return;
    if (panel.contains(target)) return;
    const first = this.getTabbable(panel)[0];
    first?.focus();
  }

  dismiss(): void {
    this.resetState();
    this.closed.emit();
  }

  private resetState(): void {
    this.recoveryStep.set(1);
    this.resetMessage.set(null);
    this.recoveryError.set(null);
    this.resetRequestForm.reset();
    this.resetConfirmForm.reset();
    this.resetPasswordVisible.set(false);
    this.resetConfirmPasswordVisible.set(false);
  }

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('sesa-prm-backdrop')) {
      this.dismiss();
    }
  }

  toggleResetPasswordVisibility(): void {
    this.resetPasswordVisible.update((v) => !v);
  }

  toggleResetConfirmPasswordVisibility(): void {
    this.resetConfirmPasswordVisible.update((v) => !v);
  }

  backToRecoveryRequest(): void {
    this.recoveryStep.set(1);
    this.resetMessage.set(null);
    this.recoveryError.set(null);
    queueMicrotask(() => this.focusInitialField());
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
    this.resetMessage.set(null);
    this.recoveryError.set(null);
    if (this.resetRequestForm.invalid) {
      this.resetRequestForm.markAllAsTouched();
      return;
    }
    const { email } = this.resetRequestForm.getRawValue();
    this.resetRequestLoading.set(true);
    this.passwordRecovery.requestCode(email).subscribe({
      next: (res) => {
        this.resetRequestLoading.set(false);
        this.resetMessage.set(res.message);
        this.recoveryStep.set(2);
        if (environment.passwordResetHintDevToken && res.devToken) {
          this.resetConfirmForm.patchValue({ token: res.devToken });
          this.toast.info('Código de prueba aplicado (solo desarrollo).', 'Recuperación');
        }
        this.toast.success('Siguiente paso: introduce el código y tu nueva contraseña.', 'Solicitud registrada');
        queueMicrotask(() => this.focusInitialField());
      },
      error: (err: PasswordRecoveryError) => {
        this.resetRequestLoading.set(false);
        const msg = err?.message ?? 'No se pudo enviar la solicitud.';
        this.recoveryError.set(msg);
        this.toast.error(msg, 'Recuperación');
      },
    });
  }

  confirmarReset(): void {
    this.recoveryError.set(null);
    if (this.resetConfirmForm.invalid) {
      this.resetConfirmForm.markAllAsTouched();
      return;
    }
    const { token, newPassword, confirmPassword } = this.resetConfirmForm.getRawValue();
    if (newPassword !== confirmPassword) {
      const msg = 'Las contraseñas no coinciden.';
      this.recoveryError.set(msg);
      this.toast.error(msg, 'Validación');
      return;
    }
    this.resetConfirmLoading.set(true);
    this.passwordRecovery.confirmNewPassword(token, newPassword).subscribe({
      next: (res) => {
        this.resetConfirmLoading.set(false);
        this.toast.success(res.message, 'Contraseña actualizada');
        this.resetState();
        this.closed.emit();
      },
      error: (err: PasswordRecoveryError) => {
        this.resetConfirmLoading.set(false);
        const msg = err?.message ?? 'No se pudo actualizar la contraseña.';
        this.recoveryError.set(msg);
        this.toast.error(msg, 'Recuperación');
      },
    });
  }
}
