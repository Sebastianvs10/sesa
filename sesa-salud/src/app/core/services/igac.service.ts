/**
 * Servicio catálogo IGAC – Límites oficiales (Departamentos, Municipios, Veredas).
 * Datos servidos desde nuestro backend; no depende del servidor público IGAC.
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface IgacDepartamento {
  id: number;
  codigoDane: string;
  nombre: string;
}

/** Normaliza ítem de departamento (backend puede enviar camelCase o snake_case). */
function toDepartamento(raw: Record<string, unknown>): IgacDepartamento {
  const codigo = raw['codigoDane'] ?? raw['codigo_dane'];
  return {
    id: Number(raw['id']) || 0,
    codigoDane: codigo != null ? String(codigo) : '',
    nombre: raw['nombre'] != null ? String(raw['nombre']) : '',
  };
}

export interface IgacMunicipio {
  id: number;
  codigoDane: string;
  departamentoCodigo: string;
  nombre: string;
}

export interface IgacVereda {
  id: number;
  codigo: string;
  municipioCodigo: string;
  nombre: string;
  geometryJson?: string;
}

@Injectable({ providedIn: 'root' })
export class IgacService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  listDepartamentos(): Observable<IgacDepartamento[]> {
    return this.http.get<Record<string, unknown>[]>(`${this.apiUrl}/igac/departamentos`).pipe(
      map((list) => (list ?? []).map(toDepartamento).filter((d) => d.codigoDane && d.nombre)),
      map((list) => [...list].sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'))),
    );
  }

  listMunicipios(departamentoCodigo: string): Observable<IgacMunicipio[]> {
    const params = new HttpParams().set('departamentoCodigo', departamentoCodigo);
    return this.http.get<IgacMunicipio[]>(`${this.apiUrl}/igac/municipios`, { params });
  }

  listVeredas(municipioCodigo: string): Observable<IgacVereda[]> {
    const params = new HttpParams().set('municipioCodigo', municipioCodigo);
    return this.http.get<IgacVereda[]>(`${this.apiUrl}/igac/veredas`, { params });
  }

  getDepartamento(codigoDane: string): Observable<IgacDepartamento | null> {
    return this.http.get<IgacDepartamento>(`${this.apiUrl}/igac/departamentos/${encodeURIComponent(codigoDane)}`);
  }

  getMunicipio(codigoDane: string): Observable<IgacMunicipio | null> {
    return this.http.get<IgacMunicipio>(`${this.apiUrl}/igac/municipios/${encodeURIComponent(codigoDane)}`);
  }

  getVereda(codigo: string): Observable<IgacVereda | null> {
    return this.http.get<IgacVereda>(`${this.apiUrl}/igac/veredas/${encodeURIComponent(codigo)}`);
  }
}
