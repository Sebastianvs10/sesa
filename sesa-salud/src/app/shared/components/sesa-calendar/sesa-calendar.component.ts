/**
 * Calendario premium para dashboard - vista mensual interactiva.
 * Autor: Ing. J Sebastian Vargas S
 */

import { CommonModule } from '@angular/common';
import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  standalone: true,
  selector: 'sesa-calendar',
  imports: [CommonModule],
  templateUrl: './sesa-calendar.component.html',
  styleUrl: './sesa-calendar.component.scss',
})
export class SesaCalendarComponent {
  @Input() highlightedDates: Set<string> = new Set();
  @Input() selectedDate: string | null = null;
  @Output() dateSelected = new EventEmitter<string>();

  private today = new Date();
  currentYear = this.today.getFullYear();
  currentMonth = this.today.getMonth();

  weekdays = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];

  get monthLabel(): string {
    const d = new Date(this.currentYear, this.currentMonth);
    return d.toLocaleDateString('es-CO', { month: 'long', year: 'numeric' });
  }

  get grid(): Array<Array<{ day: number; date: string; isCurrentMonth: boolean; isToday: boolean; isHighlighted: boolean; isSelected: boolean }>> {
    const year = this.currentYear;
    const month = this.currentMonth;
    const last = new Date(year, month + 1, 0);
    const first = new Date(year, month, 1);
    const startOffset = (first.getDay() + 6) % 7;
    const totalDays = last.getDate();
    const weeks: Array<Array<{ day: number; date: string; isCurrentMonth: boolean; isToday: boolean; isHighlighted: boolean; isSelected: boolean }>> = [];
    let dayNum = 1 - startOffset;

    for (let i = 0; i < 6; i++) {
      const week: Array<{ day: number; date: string; isCurrentMonth: boolean; isToday: boolean; isHighlighted: boolean; isSelected: boolean }> = [];
      for (let j = 0; j < 7; j++) {
        const d = new Date(year, month, dayNum);
        const dateStr = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
        week.push({
          day: d.getDate(),
          date: dateStr,
          isCurrentMonth: d.getMonth() === month,
          isToday: this.isTodayStr(dateStr),
          isHighlighted: this.highlightedDates.has(dateStr),
          isSelected: this.selectedDate === dateStr,
        });
        dayNum++;
      }
      weeks.push(week);
      if (dayNum > totalDays) break;
    }
    return weeks;
  }

  private isTodayStr(dateStr: string): boolean {
    const d = new Date();
    const todayStr = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
    return dateStr === todayStr;
  }

  selectDate(date: string): void {
    this.dateSelected.emit(date);
  }

  prevMonth(): void {
    if (this.currentMonth === 0) {
      this.currentMonth = 11;
      this.currentYear--;
    } else {
      this.currentMonth--;
    }
  }

  nextMonth(): void {
    if (this.currentMonth === 11) {
      this.currentMonth = 0;
      this.currentYear++;
    } else {
      this.currentMonth++;
    }
  }

  goToday(): void {
    this.currentYear = this.today.getFullYear();
    this.currentMonth = this.today.getMonth();
  }
}
