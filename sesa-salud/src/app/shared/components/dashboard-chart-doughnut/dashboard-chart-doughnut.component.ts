/**
 * Gráfica tipo doughnut para dashboard - estados o distribución.
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

const CHART_PALETTE = [
  '#1f6ae1',  // secondary
  '#2bb0a6',  // accent
  '#f59e0b',  // warning
  '#22c55e',  // success
  '#ef4444',  // error
  '#0ea5e9',  // info
  '#8b5cf6',  // purple
  '#ec4899',  // pink
];

@Component({
  standalone: true,
  selector: 'sesa-dashboard-chart-doughnut',
  imports: [CommonModule],
  template: '<div class="sesa-chart-doughnut-wrap"><canvas #canvas></canvas></div>',
  styleUrl: './dashboard-chart-doughnut.component.scss',
})
export class DashboardChartDoughnutComponent implements AfterViewInit, OnChanges, OnDestroy {
  @ViewChild('canvas') canvasRef!: ElementRef<HTMLCanvasElement>;
  @Input() title = '';
  @Input() labels: string[] = [];
  @Input() values: number[] = [];

  private chart: Chart<'doughnut'> | null = null;
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
      this.chart.data.datasets[0].backgroundColor = this.getColors(0.85);
      this.chart.update();
    } else {
      this.rebuildChart();
    }
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
    this.chart = null;
  }

  private getColors(alpha: number): string[] {
    return this.labels.map((_, i) => {
      const hex = CHART_PALETTE[i % CHART_PALETTE.length];
      return this.hexToRgba(hex, alpha);
    });
  }

  private rebuildChart(): void {
    if (!this.canvasRef?.nativeElement) return;
    if (this.labels.length === 0) return;
    this.chart?.destroy();

    const surface = css('--sesa-surface', '#ffffff');
    const text = css('--sesa-text-secondary', '#64748b');
    const textPrimary = css('--sesa-text-primary', '#1e293b');
    const grid = css('--sesa-border', '#e2e8f0');

    const config: ChartConfiguration<'doughnut'> = {
      type: 'doughnut',
      data: {
        labels: this.labels,
        datasets: [
          {
            data: this.values,
            backgroundColor: this.getColors(0.85),
            hoverBackgroundColor: this.getColors(1),
            borderColor: surface,
            borderWidth: 3,
            hoverOffset: 8,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '62%',
        animation: { duration: 600, easing: 'easeOutQuart' },
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              color: text,
              padding: 16,
              font: { size: 12, weight: 'normal' },
              usePointStyle: true,
              pointStyleWidth: 10,
            },
          },
          tooltip: {
            backgroundColor: surface,
            titleColor: textPrimary,
            bodyColor: text,
            borderColor: grid,
            borderWidth: 1,
            padding: 12,
            cornerRadius: 8,
            callbacks: {
              label: (ctx) => {
                const total = (ctx.dataset.data as number[]).reduce((a, b) => a + b, 0);
                const pct = total > 0 ? ((ctx.parsed / total) * 100).toFixed(1) : '0';
                return ` ${ctx.parsed} (${pct}%)`;
              },
            },
          },
        },
      },
    };
    this.chart = new Chart(this.canvasRef.nativeElement, config);
  }

  private hexToRgba(hex: string, alpha: number): string {
    hex = hex.replace('#', '');
    const r = parseInt(hex.substring(0, 2), 16);
    const g = parseInt(hex.substring(2, 4), 16);
    const b = parseInt(hex.substring(4, 6), 16);
    return `rgba(${r}, ${g}, ${b}, ${alpha})`;
  }
}
