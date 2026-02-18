/**
 * Servicio de sincronización offline → online.
 * Detecta la reconexión, agrupa operaciones pendientes en lotes y las envía al backend.
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable, OnDestroy, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { environment } from '../../../environments/environment';
import { OfflineDbService, PendingOperation } from './offline-db.service';
import { ConnectionService } from './connection.service';

/* ========== DTOs de sincronización ========== */

/** Una operación dentro del lote enviado al backend */
export interface SyncBatchItem {
  clientId: string;
  method: string;
  url: string;
  body?: unknown;
  createdAt: string;
  entity?: string;
}

/** Resultado individual devuelto por el backend */
export interface SyncItemResult {
  clientId: string;
  success: boolean;
  status?: number;
  error?: string;
  /** ID del recurso creado/actualizado en el server, si aplica */
  serverId?: number;
}

/** Respuesta completa del endpoint de sync en lote */
export interface SyncBatchResponse {
  processed: number;
  succeeded: number;
  failed: number;
  results: SyncItemResult[];
}

@Injectable({ providedIn: 'root' })
export class SyncService implements OnDestroy {

  private readonly http = inject(HttpClient);
  private readonly offlineDb = inject(OfflineDbService);
  private readonly connection = inject(ConnectionService);

  private readonly syncUrl = `${environment.apiUrl}/sync/batch`;

  /** Estado de la sincronización */
  readonly syncing = signal<boolean>(false);
  readonly pendingCount = signal<number>(0);
  readonly lastSyncResult = signal<SyncBatchResponse | null>(null);
  readonly lastSyncError = signal<string | null>(null);

  /** Computed helpers */
  readonly hasPending = computed(() => this.pendingCount() > 0);

  private sub: Subscription | null = null;
  private refreshInterval: ReturnType<typeof setInterval> | null = null;

  /** Tamaño máximo por lote */
  private readonly BATCH_SIZE = 50;

  constructor() {
    this.init();
  }

  private init(): void {
    this.refreshPendingCount();

    // Auto-sync al reconectar
    this.sub = this.connection.onReconnect$.subscribe(() => {
      this.syncAll();
    });

    // Refrescar el contador cada 10s
    this.refreshInterval = setInterval(() => this.refreshPendingCount(), 10_000);
  }

  /** Actualiza el conteo de operaciones pendientes */
  async refreshPendingCount(): Promise<void> {
    const count = await this.offlineDb.countPending();
    this.pendingCount.set(count);
  }

  /** Sincroniza todas las operaciones pendientes en lotes */
  async syncAll(): Promise<SyncBatchResponse | null> {
    if (this.syncing() || !this.connection.isOnline()) return null;

    this.syncing.set(true);
    this.lastSyncError.set(null);

    try {
      let allPending = await this.offlineDb.getPending();
      if (allPending.length === 0) {
        this.syncing.set(false);
        return null;
      }

      let aggregated: SyncBatchResponse = { processed: 0, succeeded: 0, failed: 0, results: [] };

      while (allPending.length > 0) {
        const batch = allPending.splice(0, this.BATCH_SIZE);

        // Marcar como "syncing"
        for (const op of batch) {
          op.status = 'syncing';
          await this.offlineDb.updateOp(op);
        }

        const items: SyncBatchItem[] = batch.map(op => ({
          clientId: op.clientId,
          method: op.method,
          url: op.url,
          body: op.body,
          createdAt: op.createdAt,
          entity: op.entity,
        }));

        try {
          const response = await this.http
            .post<SyncBatchResponse>(this.syncUrl, { operations: items })
            .toPromise();

          if (response) {
            aggregated.processed += response.processed;
            aggregated.succeeded += response.succeeded;
            aggregated.failed += response.failed;
            aggregated.results.push(...response.results);

            // Procesar resultados individuales
            for (const result of response.results) {
              const op = batch.find(o => o.clientId === result.clientId);
              if (!op || op.id == null) continue;

              if (result.success) {
                await this.offlineDb.markDone(op.id);
              } else {
                await this.offlineDb.markError(op.id, result.error ?? 'Error del servidor');
              }
            }
          }
        } catch (err: any) {
          // Error de red completo: devolver todo el lote a pending
          for (const op of batch) {
            if (op.id != null) {
              op.status = 'pending';
              await this.offlineDb.updateOp(op);
            }
          }
          this.lastSyncError.set(err?.message ?? 'Error de red durante la sincronización');
          break;
        }

        // Recargar pendientes por si se agregaron nuevas mientras sincronizaba
        allPending = await this.offlineDb.getPending();
      }

      this.lastSyncResult.set(aggregated);
      await this.refreshPendingCount();
      return aggregated;

    } finally {
      this.syncing.set(false);
    }
  }

  /** Fuerza sincronización manual */
  async forceSync(): Promise<SyncBatchResponse | null> {
    // Primero reintentar errores
    await this.offlineDb.retryErrors();
    await this.refreshPendingCount();
    return this.syncAll();
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    if (this.refreshInterval) clearInterval(this.refreshInterval);
  }
}
