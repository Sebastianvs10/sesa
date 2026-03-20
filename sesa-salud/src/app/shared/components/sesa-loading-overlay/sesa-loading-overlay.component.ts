/**
 * Componente Loading Overlay Premium - SESA Design System
 * Un overlay con efecto backdrop-blur para procesos que bloquean la UI.
 * Autor: Ing. J Sebastian Vargas S
 */

import { Component, Input, Injectable, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Injectable({ providedIn: 'root' })
export class SesaLoadingService {
    private _loading = signal(false);
    private _message = signal('Procesando...');

    readonly isLoading = this._loading.asReadonly();
    readonly message = this._message.asReadonly();

    show(message?: string): void {
        if (message) this._message.set(message);
        this._loading.set(true);
    }

    hide(): void {
        this._loading.set(false);
        // Reset message after animation
        setTimeout(() => this._message.set('Procesando...'), 300);
    }
}

@Component({
    standalone: true,
    selector: 'sesa-loading-overlay',
    imports: [CommonModule],
    template: `
    @if (loadingService.isLoading()) {
      <div class="sesa-overlay" [class.sesa-overlay--visible]="loadingService.isLoading()">
        <div class="sesa-loader-card">
          <div class="sesa-premium-spinner">
            <div class="inner one"></div>
            <div class="inner two"></div>
            <div class="inner three"></div>
          </div>
          <p class="sesa-loader-message">{{ loadingService.message() }}</p>
        </div>
      </div>
    }
  `,
    styleUrl: './sesa-loading-overlay.component.scss'
})
export class SesaLoadingOverlayComponent {
    constructor(public loadingService: SesaLoadingService) { }
}
