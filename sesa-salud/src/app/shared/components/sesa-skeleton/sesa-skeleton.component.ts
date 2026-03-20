/**
 * Skeleton Loader — animación shimmer premium. Presets: card, table-row, list-item, stat-card, form.
 * Autor: Ing. J Sebastian Vargas S
 */

import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type SkeletonVariant = 'text' | 'circle' | 'square' | 'rounded';
export type SkeletonPreset = 'none' | 'card' | 'table-row' | 'list-item' | 'stat-card' | 'form';

@Component({
  standalone: true,
  selector: 'sesa-skeleton',
  imports: [CommonModule],
  template: `
    @switch (preset) {
      @case ('card') {
        <div class="sesa-sk-preset sesa-sk-card">
          <div class="sesa-sk-card-header">
            <div class="sesa-skeleton sesa-sk-bar" style="width:55%;height:1rem"></div>
            <div class="sesa-skeleton sesa-sk-bar" style="width:35%;height:0.75rem;margin-top:0.4rem"></div>
          </div>
          <div class="sesa-sk-card-body">
            <div class="sesa-skeleton sesa-sk-bar" style="width:100%;height:0.8rem"></div>
            <div class="sesa-skeleton sesa-sk-bar" style="width:85%;height:0.8rem"></div>
            <div class="sesa-skeleton sesa-sk-bar" style="width:60%;height:0.8rem"></div>
          </div>
        </div>
      }
      @case ('stat-card') {
        <div class="sesa-sk-preset sesa-sk-stat">
          <div class="sesa-sk-stat-top">
            <div class="sesa-skeleton sesa-sk-bar" style="width:55%;height:0.8rem"></div>
            <div class="sesa-skeleton sesa-sk-circle" style="width:36px;height:36px"></div>
          </div>
          <div class="sesa-skeleton sesa-sk-bar" style="width:45%;height:1.6rem;margin-top:0.5rem"></div>
          <div class="sesa-skeleton sesa-sk-bar" style="width:65%;height:0.7rem;margin-top:0.6rem"></div>
        </div>
      }
      @case ('list-item') {
        <div class="sesa-sk-preset sesa-sk-list-item">
          <div class="sesa-skeleton sesa-sk-circle" style="width:38px;height:38px;flex-shrink:0"></div>
          <div class="sesa-sk-list-text">
            <div class="sesa-skeleton sesa-sk-bar" style="width:60%;height:0.85rem"></div>
            <div class="sesa-skeleton sesa-sk-bar" style="width:40%;height:0.7rem"></div>
          </div>
        </div>
      }
      @case ('table-row') {
        <div class="sesa-sk-preset sesa-sk-table-row">
          <div class="sesa-skeleton sesa-sk-bar" style="flex:1;height:0.8rem"></div>
          <div class="sesa-skeleton sesa-sk-bar" style="flex:1.5;height:0.8rem"></div>
          <div class="sesa-skeleton sesa-sk-bar" style="flex:1;height:0.8rem"></div>
          <div class="sesa-skeleton sesa-sk-bar" style="flex:0.5;height:0.8rem"></div>
        </div>
      }
      @case ('form') {
        <div class="sesa-sk-preset sesa-sk-form">
          @for (f of [1,2,3]; track f) {
            <div class="sesa-sk-form-field">
              <div class="sesa-skeleton sesa-sk-bar" style="width:35%;height:0.75rem"></div>
              <div class="sesa-skeleton sesa-sk-bar" style="width:100%;height:2.5rem;border-radius:8px"></div>
            </div>
          }
        </div>
      }
      @default {
        <!-- Skeleton base individual -->
        <div
          class="sesa-skeleton"
          [class.sesa-skeleton--circle]="variant === 'circle'"
          [class.sesa-skeleton--square]="variant === 'square'"
          [class.sesa-skeleton--rounded]="variant === 'rounded'"
          [style.width]="width"
          [style.height]="height"
          [style.border-radius]="borderRadius || null"
        ></div>
      }
    }
  `,
  styleUrl: './sesa-skeleton.component.scss',
})
export class SesaSkeletonComponent {
  /** Preset de layout complejo */
  @Input() preset: SkeletonPreset = 'none';
  /** Forma del skeleton individual */
  @Input() variant: SkeletonVariant = 'text';
  @Input() width: string = '100%';
  @Input() height: string = '1rem';
  /** Sobreescribe el border-radius */
  @Input() borderRadius: string = '';
}
