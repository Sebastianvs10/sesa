/**
 * Componente QR a partir de texto/URL (anti-falsificación recetas).
 * Autor: Ing. J Sebastian Vargas S
 */

import { Component, Input, OnInit, ChangeDetectionStrategy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'sesa-qr',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  template: `
    @if (dataUrl()) {
      <img [src]="dataUrl()" [alt]="alt" [width]="size" [height]="size" class="sesa-qr__img" />
    } @else if (error()) {
      <div class="sesa-qr__fallback">
        <span class="sesa-qr__fallback-label">Código no disponible</span>
        <a [href]="value" target="_blank" rel="noopener" class="sesa-qr__fallback-link">Abrir enlace</a>
      </div>
    } @else {
      <div class="sesa-qr__loading" [style.width.px]="size" [style.height.px]="size"></div>
    }
  `,
  styles: [`
    .sesa-qr__img { display: block; border-radius: 8px; }
    .sesa-qr__loading { background: var(--sesa-border-light); border-radius: 8px; animation: pulse 1s ease-in-out infinite; }
    .sesa-qr__fallback {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 6px;
      padding: 12px;
      background: var(--sesa-bg);
      border-radius: 8px;
      min-width: 120px;
      min-height: 120px;
    }
    .sesa-qr__fallback-label { font-size: 12px; color: var(--sesa-text-muted); }
    .sesa-qr__fallback-link { font-size: 12px; color: var(--sesa-primary); word-break: break-all; }
    @keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.5; } }
  `],
})
export class SesaQrComponent implements OnInit {
  @Input() value = '';
  @Input() size = 200;
  @Input() alt = 'Código QR';

  readonly dataUrl = signal<string | null>(null);
  readonly error = signal(false);

  ngOnInit(): void {
    if (!this.value) {
      this.error.set(true);
      return;
    }
    import('qrcode').then((QRCode) => {
      QRCode.toDataURL(this.value, { width: this.size, margin: 2 }).then((url: string) => {
        this.dataUrl.set(url);
      }).catch(() => this.error.set(true));
    }).catch(() => this.error.set(true));
  }
}
