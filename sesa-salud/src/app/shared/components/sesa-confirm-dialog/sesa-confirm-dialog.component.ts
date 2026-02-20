/**
 * Dialog de confirmación premium — reemplaza window.confirm().
 * Soporta tipos: danger, warning, info. Animación scale-in/out con backdrop blur.
 * Autor: Ing. J Sebastian Vargas S
 */
import { Component, Injectable, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

export type ConfirmDialogType = 'danger' | 'warning' | 'info';

export interface ConfirmDialogOptions {
  title?: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  type?: ConfirmDialogType;
}

interface ActiveDialog extends ConfirmDialogOptions {
  resolve: (value: boolean) => void;
}

@Injectable({ providedIn: 'root' })
export class SesaConfirmDialogService {
  readonly dialog = signal<ActiveDialog | null>(null);
  readonly leaving = signal(false);

  confirm(options: ConfirmDialogOptions): Promise<boolean> {
    return new Promise((resolve) => {
      this.leaving.set(false);
      this.dialog.set({
        title: options.title ?? this._defaultTitle(options.type),
        message: options.message,
        confirmLabel: options.confirmLabel ?? (options.type === 'danger' ? 'Eliminar' : 'Confirmar'),
        cancelLabel: options.cancelLabel ?? 'Cancelar',
        type: options.type ?? 'warning',
        resolve,
      });
    });
  }

  accept(): void {
    this._close(true);
  }

  cancel(): void {
    this._close(false);
  }

  private _close(value: boolean): void {
    const d = this.dialog();
    if (!d) return;
    this.leaving.set(true);
    setTimeout(() => {
      d.resolve(value);
      this.dialog.set(null);
      this.leaving.set(false);
    }, 250);
  }

  private _defaultTitle(type?: ConfirmDialogType): string {
    switch (type) {
      case 'danger':  return 'Confirmar eliminación';
      case 'warning': return 'Confirmar acción';
      default:        return 'Información';
    }
  }
}

@Component({
  standalone: true,
  selector: 'sesa-confirm-dialog-outlet',
  imports: [CommonModule],
  template: `
    @if (svc.dialog(); as dialog) {
      <div
        class="sesa-cdialog-backdrop"
        [class.sesa-cdialog-backdrop--leaving]="svc.leaving()"
        (click)="svc.cancel()"
        role="presentation"
      >
        <div
          class="sesa-cdialog"
          [class.sesa-cdialog--danger]="dialog.type === 'danger'"
          [class.sesa-cdialog--warning]="dialog.type === 'warning'"
          [class.sesa-cdialog--info]="dialog.type === 'info'"
          [class.sesa-cdialog--leaving]="svc.leaving()"
          role="alertdialog"
          aria-modal="true"
          [attr.aria-label]="dialog.title"
          (click)="$event.stopPropagation()"
        >
          <div class="sesa-cdialog-icon-wrap">
            @switch (dialog.type) {
              @case ('danger') {
                <svg class="sesa-cdialog-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <polyline points="3 6 5 6 21 6"/>
                  <path d="M19 6l-1 14H6L5 6"/>
                  <path d="M10 11v6M14 11v6"/>
                  <path d="M9 6V4h6v2"/>
                </svg>
              }
              @case ('warning') {
                <svg class="sesa-cdialog-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
                  <line x1="12" y1="9" x2="12" y2="13"/>
                  <line x1="12" y1="17" x2="12.01" y2="17"/>
                </svg>
              }
              @default {
                <svg class="sesa-cdialog-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <circle cx="12" cy="12" r="10"/>
                  <line x1="12" y1="16" x2="12" y2="12"/>
                  <line x1="12" y1="8" x2="12.01" y2="8"/>
                </svg>
              }
            }
          </div>

          <div class="sesa-cdialog-body">
            <h2 class="sesa-cdialog-title">{{ dialog.title }}</h2>
            <p class="sesa-cdialog-message">{{ dialog.message }}</p>
          </div>

          <div class="sesa-cdialog-actions">
            <button
              type="button"
              class="sesa-btn sesa-btn-outline sesa-cdialog-cancel"
              (click)="svc.cancel()"
            >
              {{ dialog.cancelLabel }}
            </button>
            <button
              type="button"
              class="sesa-btn sesa-cdialog-confirm"
              [class.sesa-btn-danger]="dialog.type === 'danger'"
              [class.sesa-btn-primary]="dialog.type !== 'danger'"
              (click)="svc.accept()"
            >
              {{ dialog.confirmLabel }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styleUrl: './sesa-confirm-dialog.component.scss',
})
export class SesaConfirmDialogOutletComponent {
  constructor(public svc: SesaConfirmDialogService) {}
}
