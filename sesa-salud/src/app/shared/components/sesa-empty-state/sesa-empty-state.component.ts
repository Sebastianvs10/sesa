/**
 * Empty State premium — ilustración SVG inline, CTA opcional, animación fade-in.
 * Autor: Ing. J Sebastian Vargas S
 */
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

export type EmptyStateIcon = 'box' | 'search' | 'clipboard' | 'users' | 'calendar' | 'document' | 'custom';

@Component({
  standalone: true,
  selector: 'sesa-empty-state',
  imports: [CommonModule],
  template: `
    <div class="sesa-empty" [class.sesa-empty--compact]="compact">
      <div class="sesa-empty-illustration" aria-hidden="true">
        @switch (icon) {
          @case ('search') {
            <svg viewBox="0 0 120 100" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="48" cy="46" r="28" stroke="currentColor" stroke-width="3" stroke-dasharray="6 4" opacity="0.3"/>
              <circle cx="48" cy="46" r="18" fill="currentColor" opacity="0.07"/>
              <circle cx="48" cy="46" r="10" stroke="currentColor" stroke-width="2.5" opacity="0.5"/>
              <line x1="69" y1="67" x2="88" y2="86" stroke="currentColor" stroke-width="3.5" stroke-linecap="round" opacity="0.5"/>
              <line x1="42" y1="46" x2="54" y2="46" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.6"/>
              <line x1="48" y1="40" x2="48" y2="52" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.6"/>
            </svg>
          }
          @case ('clipboard') {
            <svg viewBox="0 0 120 100" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect x="30" y="20" width="60" height="70" rx="6" stroke="currentColor" stroke-width="2.5" opacity="0.3"/>
              <rect x="44" y="14" width="32" height="14" rx="4" fill="var(--sesa-surface)" stroke="currentColor" stroke-width="2" opacity="0.5"/>
              <line x1="42" y1="45" x2="78" y2="45" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.4"/>
              <line x1="42" y1="57" x2="68" y2="57" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.4"/>
              <line x1="42" y1="69" x2="72" y2="69" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.4"/>
            </svg>
          }
          @case ('users') {
            <svg viewBox="0 0 120 100" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="45" cy="35" r="14" stroke="currentColor" stroke-width="2.5" opacity="0.4"/>
              <path d="M20 75c0-14 11-22 25-22s25 8 25 22" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" opacity="0.4"/>
              <circle cx="80" cy="38" r="10" stroke="currentColor" stroke-width="2" opacity="0.3"/>
              <path d="M65 75c0-10 7-16 15-16" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.3"/>
            </svg>
          }
          @case ('calendar') {
            <svg viewBox="0 0 120 100" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect x="20" y="22" width="80" height="66" rx="8" stroke="currentColor" stroke-width="2.5" opacity="0.3"/>
              <line x1="20" y1="42" x2="100" y2="42" stroke="currentColor" stroke-width="2" opacity="0.3"/>
              <line x1="40" y1="14" x2="40" y2="30" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" opacity="0.5"/>
              <line x1="80" y1="14" x2="80" y2="30" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" opacity="0.5"/>
              <rect x="34" y="52" width="12" height="10" rx="2" fill="currentColor" opacity="0.25"/>
              <rect x="54" y="52" width="12" height="10" rx="2" fill="currentColor" opacity="0.25"/>
              <rect x="74" y="52" width="12" height="10" rx="2" fill="currentColor" opacity="0.15"/>
              <rect x="34" y="68" width="12" height="10" rx="2" fill="currentColor" opacity="0.15"/>
              <rect x="54" y="68" width="12" height="10" rx="2" fill="currentColor" opacity="0.15"/>
            </svg>
          }
          @case ('document') {
            <svg viewBox="0 0 120 100" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M35 15h32l18 18v57a5 5 0 0 1-5 5H35a5 5 0 0 1-5-5V20a5 5 0 0 1 5-5z" stroke="currentColor" stroke-width="2.5" opacity="0.3"/>
              <path d="M67 15v18h18" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" opacity="0.4"/>
              <line x1="42" y1="52" x2="78" y2="52" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.4"/>
              <line x1="42" y1="64" x2="70" y2="64" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.4"/>
            </svg>
          }
          @default {
            <!-- box / default -->
            <svg viewBox="0 0 120 100" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M20 38l40-22 40 22v34l-40 22-40-22V38z" stroke="currentColor" stroke-width="2.5" opacity="0.3"/>
              <path d="M20 38l40 22 40-22" stroke="currentColor" stroke-width="2.5" opacity="0.3"/>
              <line x1="60" y1="60" x2="60" y2="94" stroke="currentColor" stroke-width="2.5" opacity="0.3"/>
              <circle cx="60" cy="60" r="6" fill="currentColor" opacity="0.15"/>
            </svg>
          }
        }
      </div>

      <div class="sesa-empty-content">
        @if (title) {
          <p class="sesa-empty-title">{{ title }}</p>
        }
        @if (message) {
          <p class="sesa-empty-message">{{ message }}</p>
        }
        <ng-content></ng-content>
        @if (actionLabel) {
          <button
            type="button"
            class="sesa-btn sesa-btn-primary sesa-empty-action"
            (click)="action.emit()"
          >
            {{ actionLabel }}
          </button>
        }
      </div>
    </div>
  `,
  styleUrl: './sesa-empty-state.component.scss',
})
export class SesaEmptyStateComponent {
  @Input() icon: EmptyStateIcon = 'box';
  @Input() title = 'Sin resultados';
  @Input() message = '';
  @Input() actionLabel = '';
  @Input() compact = false;
  @Output() action = new EventEmitter<void>();
}
