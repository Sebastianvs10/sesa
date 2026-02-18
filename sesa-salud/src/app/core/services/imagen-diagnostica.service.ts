import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ImagenDiagnosticaDto {
  id: number;
  atencionId: number;
  tipo?: string;
  resultado?: string;
  urlArchivo?: string;
  createdAt?: string;
}

export interface ImagenDiagnosticaRequestDto {
  atencionId: number;
  tipo?: string;
  resultado?: string;
  urlArchivo?: string;
}

@Injectable({ providedIn: 'root' })
export class ImagenDiagnosticaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/imagenes-diagnosticas`;

  listByAtencion(atencionId: number): Observable<ImagenDiagnosticaDto[]> {
    const params = new HttpParams().set('atencionId', atencionId).set('page', '0').set('size', '50');
    return this.http.get<ImagenDiagnosticaDto[]>(this.apiUrl, { params });
  }

  create(request: ImagenDiagnosticaRequestDto): Observable<ImagenDiagnosticaDto> {
    return this.http.post<ImagenDiagnosticaDto>(this.apiUrl, request);
  }
}
