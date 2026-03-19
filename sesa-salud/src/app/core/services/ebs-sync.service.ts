/**
 * S13: Servicio de sincronización EBS (offline-first). Cola de visitas y sincronización al recuperar red.
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable, inject, signal, computed } from '@angular/core';
import { EbsService, VisitaEbsSyncDto, VisitaEbsSyncResponseDto } from './ebs.service';

const STORAGE_KEY = 'sesa_ebs_pending_visits';

@Injectable({ providedIn: 'root' })
export class EbsSyncService {
  private readonly ebs = inject(EbsService);

  /** Estado de conexión (según navigator.onLine y eventos online/offline). */
  readonly isOnline = signal(typeof navigator !== 'undefined' ? navigator.onLine : true);

  /** Cola de visitas pendientes de enviar al servidor. */
  private readonly queue = signal<VisitaEbsSyncDto[]>(this.loadQueueFromStorage());

  /** Cantidad de visitas pendientes de sincronizar. */
  readonly pendingCount = computed(() => this.queue().length);

  /** Sincronización en curso. */
  readonly syncing = signal(false);

  /** Último mensaje (éxito o conflicto). */
  readonly lastMessage = signal<string | null>(null);

  constructor() {
    if (typeof window !== 'undefined') {
      window.addEventListener('online', () => this.isOnline.set(true));
      window.addEventListener('offline', () => this.isOnline.set(false));
    }
  }

  private loadQueueFromStorage(): VisitaEbsSyncDto[] {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) {
        const parsed = JSON.parse(raw) as VisitaEbsSyncDto[];
        return Array.isArray(parsed) ? parsed : [];
      }
    } catch {
      // ignore
    }
    return [];
  }

  private saveQueueToStorage(list: VisitaEbsSyncDto[]): void {
    try {
      if (list.length === 0) {
        localStorage.removeItem(STORAGE_KEY);
      } else {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(list));
      }
    } catch {
      // ignore
    }
  }

  /** Añade una visita a la cola (p. ej. guardada en modo offline). */
  addToQueue(visita: VisitaEbsSyncDto): void {
    const list = [...this.queue(), visita];
    this.queue.set(list);
    this.saveQueueToStorage(list);
  }

  /** Sincroniza la cola con el servidor. Al terminar limpia la cola y muestra mensaje si hay conflictos. */
  syncNow(): void {
    const list = this.queue();
    if (list.length === 0) {
      this.lastMessage.set('No hay visitas pendientes de sincronizar.');
      return;
    }
    if (!this.isOnline()) {
      this.lastMessage.set('Sin conexión. Conéctese y vuelva a intentar.');
      return;
    }
    this.syncing.set(true);
    this.lastMessage.set(null);
    this.ebs.sincronizarVisitas(list).subscribe({
      next: (res: VisitaEbsSyncResponseDto) => {
        this.syncing.set(false);
        const saved = res.savedIds?.length ?? 0;
        const conflicts = res.conflicts?.length ?? 0;
        if (conflicts > 0) {
          this.lastMessage.set(
            `Sincronizadas ${saved} visita(s). ${conflicts} conflicto(s): el servidor tiene una versión más reciente.`
          );
          const conflictOfflineIds = new Set((res.conflicts ?? []).map((c) => c.offlineUuid).filter(Boolean));
          const stillPending = this.queue().filter((v) => !conflictOfflineIds.has(v.offlineUuid));
          this.queue.set(stillPending);
          this.saveQueueToStorage(stillPending);
        } else {
          this.queue.set([]);
          this.saveQueueToStorage([]);
          this.lastMessage.set(saved > 0 ? `Sincronizadas ${saved} visita(s) correctamente.` : 'Sincronización completada.');
        }
      },
      error: () => {
        this.syncing.set(false);
        this.lastMessage.set('Error al sincronizar. Vuelva a intentar.');
      },
    });
  }

  clearMessage(): void {
    this.lastMessage.set(null);
  }
}
