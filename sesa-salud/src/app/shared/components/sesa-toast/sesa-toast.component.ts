/**
 * Sistema de Toast Notifications — Premium Edition.
 * Soporta tipos: success, error, warning, info.
 * Stack de hasta 5 toasts, auto-dismiss, SVG icons, progress bar corregida.
 * Compatible con tema claro/oscuro.
 * Autor: Ing. J Sebastian Vargas S
 */

import { Component, Injectable, Input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

export type ToastType = 'success' | 'error' | 'warning' | 'info';
export type ToastPosition = 'top-right' | 'top-center' | 'bottom-right' | 'bottom-center';

export interface ToastConfig {
  message: string;
  type?: ToastType;
  title?: string;
  duration?: number;
  action?: { label: string; callback: () => void };
}

interface ToastItem extends ToastConfig {
  id: number;
  leaving: boolean;
  progressKey: number;
}

@Injectable({ providedIn: 'root' })
export class SesaToastService {
  private _toasts = signal<ToastItem[]>([]);
  private nextId = 0;

  readonly toasts = this._toasts.asReadonly();

  show(config: ToastConfig): number {
    const id = ++this.nextId;
    const toast: ToastItem = {
      ...config,
      id,
      type: config.type ?? 'info',
      duration: config.duration ?? 4000,
      leaving: false,
      progressKey: Date.now(),
    };

    this._toasts.update((list) => {
      const updated = [...list, toast];
      return updated.length > 5 ? updated.slice(-5) : updated;
    });

    if (toast.duration! > 0) {
      setTimeout(() => this.dismiss(id), toast.duration!);
    }
    return id;
  }

  success(message: string, title?: string, duration?: number): number {
    return this.show({ message, title, type: 'success', duration });
  }

  error(message: string, title?: string, duration?: number): number {
    return this.show({ message, title, type: 'error', duration: duration ?? 6000 });
  }

  warning(message: string, title?: string, duration?: number): number {
    return this.show({ message, title, type: 'warning', duration });
  }

  info(message: string, title?: string, duration?: number): number {
    return this.show({ message, title, type: 'info', duration });
  }

  dismiss(id: number): void {
    this._toasts.update((list) =>
      list.map((t) => (t.id === id ? { ...t, leaving: true } : t))
    );
    setTimeout(() => {
      this._toasts.update((list) => list.filter((t) => t.id !== id));
    }, 320);
  }

  /** Confirm dialog replacement — returns a Promise<boolean> */
  confirm(message: string, title = 'Confirmación'): Promise<boolean> {
    return new Promise((resolve) => {
      const id = this.show({
        message,
        title,
        type: 'warning',
        duration: 0,
        action: {
          label: 'Confirmar',
          callback: () => {
            this.dismiss(id);
            resolve(true);
          },
        },
      });
      const checkInterval = setInterval(() => {
        const exists = this._toasts().find((t) => t.id === id);
        if (!exists) {
          clearInterval(checkInterval);
          resolve(false);
        }
      }, 100);
    });
  }
}

@Component({
  standalone: true,
  selector: 'sesa-toast-container',
  imports: [CommonModule],
  template: `
    <div
      class="sesa-toast-stack"
      [class.sesa-toast-stack--top-center]="position === 'top-center'"
      [class.sesa-toast-stack--bottom-right]="position === 'bottom-right'"
      [class.sesa-toast-stack--bottom-center]="position === 'bottom-center'"
      role="status"
      aria-live="polite"
      aria-atomic="false"
    >
      @for (toast of toastService.toasts(); track toast.id) {
        <div
          class="sesa-toast"
          [class.sesa-toast--success]="toast.type === 'success'"
          [class.sesa-toast--error]="toast.type === 'error'"
          [class.sesa-toast--warning]="toast.type === 'warning'"
          [class.sesa-toast--info]="toast.type === 'info'"
          [class.sesa-toast--leaving]="toast.leaving"
          role="alert"
        >
          <div class="sesa-toast-icon" aria-hidden="true">
            @switch (toast.type) {
              @case ('success') {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                  <polyline points="22 4 12 14.01 9 11.01"/>
                </svg>
              }
              @case ('error') {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10"/>
                  <line x1="15" y1="9" x2="9" y2="15"/>
                  <line x1="9" y1="9" x2="15" y2="15"/>
                </svg>
              }
              @case ('warning') {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
                  <line x1="12" y1="9" x2="12" y2="13"/>
                  <line x1="12" y1="17" x2="12.01" y2="17"/>
                </svg>
              }
              @case ('info') {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10"/>
                  <line x1="12" y1="16" x2="12" y2="12"/>
                  <line x1="12" y1="8" x2="12.01" y2="8"/>
                </svg>
              }
            }
          </div>

          <div class="sesa-toast-body">
            @if (toast.title) {
              <div class="sesa-toast-title">{{ toast.title }}</div>
            }
            <div class="sesa-toast-message">{{ toast.message }}</div>
            @if (toast.action) {
              <div class="sesa-toast-actions">
                <button type="button" class="sesa-toast-action-btn" (click)="toast.action!.callback()">
                  {{ toast.action!.label }}
                </button>
                <button type="button" class="sesa-toast-cancel-btn" (click)="toastService.dismiss(toast.id)">
                  Cancelar
                </button>
              </div>
            }
          </div>

          <button
            type="button"
            class="sesa-toast-close"
            (click)="toastService.dismiss(toast.id)"
            aria-label="Cerrar notificación"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>

          @if (toast.duration! > 0 && !toast.leaving) {
            <div
              class="sesa-toast-progress"
              [style.animation-duration.ms]="toast.duration"
              [attr.key]="toast.progressKey"
            ></div>
          }
        </div>
      }
    </div>
  `,
  styleUrl: './sesa-toast.component.scss',
})
export class SesaToastContainerComponent {
  @Input() position: ToastPosition = 'top-right';
  constructor(public toastService: SesaToastService) {}
}
