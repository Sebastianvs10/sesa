import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface EpsDto {
  id: number;
  codigo: string;
  nombre: string;
}

@Injectable({ providedIn: 'root' })
export class EpsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/eps`;

  list(): Observable<EpsDto[]> {
    return this.http.get<EpsDto[]>(this.apiUrl);
  }
}
