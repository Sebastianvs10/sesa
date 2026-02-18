/**
 * Gráfica de barras/linea para dashboard - datos reales, tema claro/oscuro.
 * Autor: Ing. J Sebastian Vargas S
 */

import {
  Component,
  Input,
  OnChanges,
  OnDestroy,
  AfterViewInit,
  ViewChild,
  ElementRef,
  SimpleChanges,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration, registerables } from 'chart.js';

Chart.register(...registerables);

function css(prop: string, fallback: string): string {
  if (typeof document === 'undefined') return fallback;
  return getComputedStyle(document.documentElement).getPropertyValue(prop).trim() || fallback;
}

@Component({
  standalone: true,
  selector: 'sesa-dashboard-chart-bar',
  imports: [CommonModule],
  template: '<div class="sesa-chart-bar-wrap"><canvas #canvas></canvas></div>',
  styleUrl: './dashboard-chart-bar.component.scss',
})
export class DashboardChartBarComponent implements AfterViewInit, OnChanges, OnDestroy {
  @ViewChild('canvas') canvasRef!: ElementRef<HTMLCanvasElement>;
  @Input() title = '';
  @Input() labels: string[] = [];
  @Input() values: number[] = [];
  @Input() type: 'bar' | 'line' = 'bar';
  @Input() valueSuffix = '';

  private chart: Chart | null = null;
  private initialized = false;

  ngAfterViewInit(): void {
    this.initialized = true;
    this.rebuildChart();
  }

  ngOnChanges(_changes: SimpleChanges): void {
    if (!this.initialized) return;
    if (this.chart) {
      this.chart.data.labels = this.labels;
      this.chart.data.datasets[0].data = this.values;
      this.chart.update();
    } else {
      this.rebuildChart();
    }
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
    this.chart = null;
  }

  private rebuildChart(): void {
    if (!this.canvasRef?.nativeElement) return;
    if (this.labels.length === 0) return;
    this.chart?.destroy();

    const primary = css('--sesa-secondary', '#1f6ae1');
    const accent = css('--sesa-accent', '#2bb0a6');
    const grid = css('--sesa-border', '#e2e8f0');
    const text = css('--sesa-text-secondary', '#64748b');
    const surface = css('--sesa-surface', '#ffffff');
    const textPrimary = css('--sesa-text-primary', '#1e293b');

    const isLine = this.type === 'line';

    const config: ChartConfiguration = {
      type: this.type,
      data: {
        labels: this.labels,
        datasets: [
          {
            label: this.title,
            data: this.values,
            backgroundColor: isLine
              ? this.hexToRgba(accent, 0.12)
              : this.createGradientColors(primary, accent, this.labels.length),
            borderColor: isLine ? accent : primary,
            borderWidth: isLine ? 2.5 : 0,
            borderRadius: isLine ? 0 : 6,
            fill: isLine,
            tension: 0.4,
            pointBackgroundColor: accent,
            pointBorderColor: surface,
            pointBorderWidth: 2,
            pointRadius: 5,
            pointHoverRadius: 7,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        animation: { duration: 600, easing: 'easeOutQuart' },
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: surface,
            titleColor: textPrimary,
            bodyColor: text,
            borderColor: grid,
            borderWidth: 1,
            padding: 12,
            cornerRadius: 8,
            titleFont: { weight: 'bold' },
            callbacks: { label: (ctx) => ` ${ctx.parsed.y}${this.valueSuffix}` },
          },
        },
        scales: {
          x: {
            grid: { display: false },
            border: { display: false },
            ticks: { color: text, font: { size: 11, weight: 'bold' } },
          },
          y: {
            beginAtZero: true,
            grid: { color: this.hexToRgba(grid, 0.5) },
            border: { display: false },
            ticks: {
              color: text,
              font: { size: 11 },
              callback: (v) => `${v}${this.valueSuffix}`,
            },
          },
        },
      },
    };
    this.chart = new Chart(this.canvasRef.nativeElement, config);
  }

  private createGradientColors(from: string, to: string, count: number): string[] {
    if (count <= 1) return [this.hexToRgba(from, 0.7)];
    return Array.from({ length: count }, (_, i) => {
      const t = i / (count - 1);
      return this.hexToRgba(this.lerpColor(from, to, t), 0.7);
    });
  }

  private lerpColor(a: string, b: string, t: number): string {
    const ra = this.parseHex(a), rb = this.parseHex(b);
    const r = Math.round(ra[0] + (rb[0] - ra[0]) * t);
    const g = Math.round(ra[1] + (rb[1] - ra[1]) * t);
    const bl = Math.round(ra[2] + (rb[2] - ra[2]) * t);
    return `#${[r, g, bl].map(c => c.toString(16).padStart(2, '0')).join('')}`;
  }

  private parseHex(hex: string): [number, number, number] {
    hex = hex.replace('#', '');
    if (hex.length === 3) hex = hex.split('').map(c => c + c).join('');
    return [parseInt(hex.substring(0, 2), 16), parseInt(hex.substring(2, 4), 16), parseInt(hex.substring(4, 6), 16)];
  }

  private hexToRgba(hex: string, alpha: number): string {
    const [r, g, b] = this.parseHex(hex);
    return `rgba(${r}, ${g}, ${b}, ${alpha})`;
  }
}
