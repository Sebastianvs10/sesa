/**
 * HTTP Interceptor offline-first.
 * - Peticiones de escritura (POST/PUT/DELETE/PATCH): si está offline, encola en IndexedDB.
 * - Peticiones GET: si está offline, intenta devolver respuesta cacheada.
 * - Peticiones GET exitosas: cachea la respuesta para uso offline futuro.
 * Autor: Ing. J Sebastian Vargas S
 */

import { HttpInterceptorFn, HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable, of, from, throwError } from 'rxjs';
import { tap, catchError, switchMap } from 'rxjs/operators';
import { OfflineDbService, OfflineHttpMethod } from './offline-db.service';
import { ConnectionService } from './connection.service';
import { SyncService } from './sync.service';
import { environment } from '../../../environments/environment';

/** Genera UUID v4 básico sin dependencias */
function uuid(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16);
  });
}

/** URLs excluidas de la lógica offline (auth, heartbeat, sync) */
const EXCLUDED_PATTERNS = ['/auth/', '/actuator/', '/sync/batch'];

function isExcluded(url: string): boolean {
  return EXCLUDED_PATTERNS.some(p => url.includes(p));
}

/** Extrae la parte de path relativa al apiUrl */
function relativeUrl(fullUrl: string): string {
  const base = environment.apiUrl;
  return fullUrl.startsWith(base) ? fullUrl.substring(base.length) : fullUrl;
}

/** Extrae el nombre de la entidad desde la URL (/pacientes/3 → pacientes) */
function extractEntity(url: string): string | undefined {
  const rel = relativeUrl(url);
  const match = rel.match(/^\/([a-z\-]+)/i);
  return match?.[1] ?? undefined;
}

/** Lee el schema del usuario actual desde localStorage */
function currentSchema(): string | undefined {
  try {
    const raw = localStorage.getItem('sesa_user');
    return raw ? (JSON.parse(raw)?.schema ?? undefined) : undefined;
  } catch {
    return undefined;
  }
}

const WRITE_METHODS: OfflineHttpMethod[] = ['POST', 'PUT', 'DELETE', 'PATCH'];

/** TTL de cache GET en ms (5 minutos) */
const GET_CACHE_TTL = 5 * 60 * 1000;

/** Crea una respuesta simulada 202 para operaciones encoladas */
function offlineAcceptedResponse(clientId: string): HttpResponse<unknown> {
  return new HttpResponse({
    status: 202,
    body: {
      offline: true,
      clientId,
      message: 'Operación guardada. Se enviará cuando haya conexión.',
    },
  });
}

export const offlineInterceptor: HttpInterceptorFn = (req, next) => {
  const connection = inject(ConnectionService);
  const offlineDb = inject(OfflineDbService);
  const syncService = inject(SyncService);

  const method = req.method.toUpperCase() as OfflineHttpMethod;
  const url = req.urlWithParams;

  // No interceptar rutas excluidas
  if (isExcluded(url)) {
    return next(req);
  }

  const isWrite = WRITE_METHODS.includes(method);
  const isOnline = connection.isOnline();

  /* ========== OFFLINE + ESCRITURA → Encolar ========== */
  if (!isOnline && isWrite) {
    const clientId = uuid();
    const enqueuePromise = offlineDb.enqueue({
      clientId,
      method,
      url: relativeUrl(url),
      body: req.body,
      createdAt: new Date().toISOString(),
      status: 'pending',
      retries: 0,
      entity: extractEntity(url),
      schema: currentSchema(),
    }).then(() => syncService.refreshPendingCount());

    return from(enqueuePromise).pipe(
      switchMap(() => of(offlineAcceptedResponse(clientId)))
    );
  }

  /* ========== OFFLINE + LECTURA → Cache ========== */
  if (!isOnline && method === 'GET') {
    return from(offlineDb.getCachedResponse(relativeUrl(url))).pipe(
      switchMap((cached) => {
        if (cached !== null) {
          return of(new HttpResponse({ status: 200, body: cached }));
        }
        return throwError(() => new HttpErrorResponse({
          status: 0,
          statusText: 'Sin conexión',
          url,
          error: { message: 'No hay conexión a internet y no hay datos en cache para esta consulta.' },
        }));
      })
    );
  }

  /* ========== ONLINE → Pasar la petición normalmente ========== */
  return next(req).pipe(
    tap((event) => {
      // Cachear respuestas GET exitosas
      if (method === 'GET' && event instanceof HttpResponse && event.ok) {
        offlineDb.cacheResponse(relativeUrl(url), event.body, GET_CACHE_TTL).catch(() => {});
      }
    }),
    catchError((err: HttpErrorResponse) => {
      // Si falla por red (status 0) y es escritura, encolar como offline
      if (err.status === 0 && isWrite) {
        const clientId = uuid();
        return from(offlineDb.enqueue({
          clientId,
          method,
          url: relativeUrl(url),
          body: req.body,
          createdAt: new Date().toISOString(),
          status: 'pending',
          retries: 0,
          entity: extractEntity(url),
          schema: currentSchema(),
        })).pipe(
          tap(() => syncService.refreshPendingCount()),
          switchMap(() => of(offlineAcceptedResponse(clientId)))
        );
      }

      // Si falla por red y es GET, intentar cache como fallback
      if (err.status === 0 && method === 'GET') {
        return from(offlineDb.getCachedResponse(relativeUrl(url))).pipe(
          switchMap((cached) => {
            if (cached !== null) {
              return of(new HttpResponse({ status: 200, body: cached }));
            }
            return throwError(() => err);
          })
        );
      }

      return throwError(() => err);
    })
  );
};
