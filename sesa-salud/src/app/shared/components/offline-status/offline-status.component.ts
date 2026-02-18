/**
 * Componente visual de estado de conexión y sincronización offline.
 * Muestra un banner cuando está offline y una badge con operaciones pendientes.
 * Autor: Ing. J Sebastian Vargas S
 */

import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faBolt, faArrowsRotate, faTriangleExclamation } from '@fortawesome/free-solid-svg-icons';
import { ConnectionService } from '../../../core/offline/connection.service';
import { SyncService } from '../../../core/offline/sync.service';

@Component({
  standalone: true,
  selector: 'sesa-offline-status',
  imports: [CommonModule, FontAwesomeModule],
  templateUrl: './offline-status.component.html',
  styleUrl: './offline-status.component.scss',
})
export class OfflineStatusComponent {
  readonly connection = inject(ConnectionService);
  readonly sync = inject(SyncService);

  /** true mientras se ejecuta un chequeo manual (botón Reintentar) */
  readonly checkingConnection = signal(false);

  faBolt = faBolt;
  faArrowsRotate = faArrowsRotate;
  faTriangleExclamation = faTriangleExclamation;

  forceSync(): void {
    this.sync.forceSync();
  }

  async retryConnection(): Promise<void> {
    if (this.checkingConnection()) return;
    this.checkingConnection.set(true);
    try {
      await this.connection.checkNow();
    } finally {
      this.checkingConnection.set(false);
    }
  }
}
