/**
 * Servicio de base de datos offline usando IndexedDB.
 * Almacena operaciones pendientes y cache de datos para funcionalidad offline-first.
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable } from '@angular/core';
import { openDB, IDBPDatabase, DBSchema } from 'idb';

/* ========== Modelo de datos ========== */

export type OfflineHttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';

export type OperationStatus = 'pending' | 'syncing' | 'error' | 'done';

/** Operación HTTP encolada mientras el usuario estaba sin conexión */
export interface PendingOperation {
  id?: number;
  /** UUID generado en el cliente para deduplicación en el server */
  clientId: string;
  /** Método HTTP original */
  method: OfflineHttpMethod;
  /** URL relativa (sin host), ej: /pacientes */
  url: string;
  /** Body serializado (solo POST/PUT/PATCH) */
  body?: unknown;
  /** Timestamp ISO de creación */
  createdAt: string;
  /** Estado actual */
  status: OperationStatus;
  /** Número de reintentos ejecutados */
  retries: number;
  /** Último error (si hubo) */
  lastError?: string;
  /** Entidad afectada (pacientes, consultas, etc.) para agrupar */
  entity?: string;
  /** Schema (tenant) al momento de crear la operación */
  schema?: string;
}

/** Registro en el cache de respuestas GET para lectura offline */
export interface CachedResponse {
  /** Clave: method+url */
  key: string;
  url: string;
  body: unknown;
  /** Timestamp ISO del último fetch exitoso */
  cachedAt: string;
  /** TTL en ms (por defecto 5 min) */
  ttl: number;
}

/* ========== Schema de IndexedDB ========== */

interface SesaOfflineDB extends DBSchema {
  pendingOps: {
    key: number;
    value: PendingOperation;
    indexes: {
      'by-status': OperationStatus;
      'by-clientId': string;
      'by-createdAt': string;
    };
  };
  responseCache: {
    key: string;
    value: CachedResponse;
    indexes: {
      'by-cachedAt': string;
    };
  };
}

const DB_NAME = 'sesa-offline';
const DB_VERSION = 1;

@Injectable({ providedIn: 'root' })
export class OfflineDbService {
  private dbPromise: Promise<IDBPDatabase<SesaOfflineDB>>;

  constructor() {
    this.dbPromise = this.initDb();
  }

  private initDb(): Promise<IDBPDatabase<SesaOfflineDB>> {
    return openDB<SesaOfflineDB>(DB_NAME, DB_VERSION, {
      upgrade(db) {
        // Store de operaciones pendientes
        if (!db.objectStoreNames.contains('pendingOps')) {
          const ops = db.createObjectStore('pendingOps', {
            keyPath: 'id',
            autoIncrement: true,
          });
          ops.createIndex('by-status', 'status');
          ops.createIndex('by-clientId', 'clientId', { unique: true });
          ops.createIndex('by-createdAt', 'createdAt');
        }
        // Store de cache de respuestas
        if (!db.objectStoreNames.contains('responseCache')) {
          const cache = db.createObjectStore('responseCache', { keyPath: 'key' });
          cache.createIndex('by-cachedAt', 'cachedAt');
        }
      },
    });
  }

  /* ========================
   * Operaciones pendientes
   * ======================== */

  /** Encola una nueva operación pendiente */
  async enqueue(op: Omit<PendingOperation, 'id'>): Promise<number> {
    const db = await this.dbPromise;
    return db.add('pendingOps', op as PendingOperation);
  }

  /** Obtiene todas las operaciones con status dado */
  async getByStatus(status: OperationStatus): Promise<PendingOperation[]> {
    const db = await this.dbPromise;
    return db.getAllFromIndex('pendingOps', 'by-status', status);
  }

  /** Obtiene todas las operaciones pendientes ordenadas por fecha */
  async getPending(): Promise<PendingOperation[]> {
    const db = await this.dbPromise;
    const all = await db.getAllFromIndex('pendingOps', 'by-status', 'pending');
    return all.sort((a, b) => a.createdAt.localeCompare(b.createdAt));
  }

  /** Total de operaciones pendientes (para badge en la UI) */
  async countPending(): Promise<number> {
    const db = await this.dbPromise;
    return db.countFromIndex('pendingOps', 'by-status', 'pending');
  }

  /** Actualiza una operación existente */
  async updateOp(op: PendingOperation): Promise<void> {
    const db = await this.dbPromise;
    await db.put('pendingOps', op);
  }

  /** Marca operación como sincronizada y la elimina */
  async markDone(id: number): Promise<void> {
    const db = await this.dbPromise;
    await db.delete('pendingOps', id);
  }

  /** Marca operación con error */
  async markError(id: number, error: string): Promise<void> {
    const db = await this.dbPromise;
    const op = await db.get('pendingOps', id);
    if (op) {
      op.status = 'error';
      op.lastError = error;
      op.retries = (op.retries ?? 0) + 1;
      await db.put('pendingOps', op);
    }
  }

  /** Resetea errores a pending para reintento */
  async retryErrors(): Promise<number> {
    const db = await this.dbPromise;
    const errors = await db.getAllFromIndex('pendingOps', 'by-status', 'error');
    let count = 0;
    const tx = db.transaction('pendingOps', 'readwrite');
    for (const op of errors) {
      if (op.retries < 5) {
        op.status = 'pending';
        await tx.store.put(op);
        count++;
      }
    }
    await tx.done;
    return count;
  }

  /** Limpia operaciones completadas (seguridad) */
  async clearDone(): Promise<void> {
    const db = await this.dbPromise;
    const done = await db.getAllFromIndex('pendingOps', 'by-status', 'done');
    const tx = db.transaction('pendingOps', 'readwrite');
    for (const op of done) {
      if (op.id != null) await tx.store.delete(op.id);
    }
    await tx.done;
  }

  /* ========================
   * Cache de respuestas
   * ======================== */

  /** Guarda (o actualiza) respuesta en cache */
  async cacheResponse(url: string, body: unknown, ttlMs = 300_000): Promise<void> {
    const db = await this.dbPromise;
    const entry: CachedResponse = {
      key: `GET:${url}`,
      url,
      body,
      cachedAt: new Date().toISOString(),
      ttl: ttlMs,
    };
    await db.put('responseCache', entry);
  }

  /** Obtiene respuesta cacheada si aún es válida (dentro del TTL) */
  async getCachedResponse<T = unknown>(url: string): Promise<T | null> {
    const db = await this.dbPromise;
    const entry = await db.get('responseCache', `GET:${url}`);
    if (!entry) return null;
    const age = Date.now() - new Date(entry.cachedAt).getTime();
    if (age > entry.ttl) {
      await db.delete('responseCache', entry.key);
      return null;
    }
    return entry.body as T;
  }

  /** Elimina todo el cache de respuestas */
  async clearCache(): Promise<void> {
    const db = await this.dbPromise;
    await db.clear('responseCache');
  }

  /** Elimina entradas expiradas del cache */
  async purgeExpiredCache(): Promise<number> {
    const db = await this.dbPromise;
    const all = await db.getAll('responseCache');
    const now = Date.now();
    let purged = 0;
    const tx = db.transaction('responseCache', 'readwrite');
    for (const entry of all) {
      const age = now - new Date(entry.cachedAt).getTime();
      if (age > entry.ttl) {
        await tx.store.delete(entry.key);
        purged++;
      }
    }
    await tx.done;
    return purged;
  }
}
