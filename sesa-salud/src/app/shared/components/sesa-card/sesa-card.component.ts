import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  standalone: true,
  selector: 'sesa-card',
  imports: [CommonModule],
  templateUrl: './sesa-card.component.html',
  styleUrl: './sesa-card.component.scss',
})
export class SesaCardComponent {
  @Input() title = '';
  @Input() subtitle = '';
  @Input() alignHeader: 'start' | 'between' = 'between';
}

