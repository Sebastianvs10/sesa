import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DolorDto {
    id: number;
    pacienteId: number;
    pacienteNombre: string;
    historiaClinicaId?: number;
    zonaCorporal: string;
    zonaLabel: string;
    tipoDolor: string;
    intensidad: number;
    severidad: string;
    estado: string;
    fechaInicio: string;
    fechaResolucion?: string;
    descripcion?: string;
    factoresAgravantes?: string;
    factoresAliviantes?: string;
    tratamiento?: string;
    notas?: string;
    vista: string;
    createdAt: string;
}

export interface DolorRequestDto {
    pacienteId: number;
    historiaClinicaId?: number;
    zonaCorporal: string;
    zonaLabel: string;
    tipoDolor?: string;
    intensidad: number;
    severidad?: string;
    estado?: string;
    fechaInicio?: string;
    fechaResolucion?: string;
    descripcion?: string;
    factoresAgravantes?: string;
    factoresAliviantes?: string;
    tratamiento?: string;
    notas?: string;
    vista?: string;
}

@Injectable({ providedIn: 'root' })
export class DolorService {
    private readonly http = inject(HttpClient);
    private readonly apiUrl = `${environment.apiUrl}/dolores`;

    listByPaciente(pacienteId: number): Observable<DolorDto[]> {
        return this.http.get<DolorDto[]>(`${this.apiUrl}/paciente/${pacienteId}`);
    }

    listByHistoriaClinica(historiaClinicaId: number): Observable<DolorDto[]> {
        return this.http.get<DolorDto[]>(`${this.apiUrl}/historia/${historiaClinicaId}`);
    }

    getById(id: number): Observable<DolorDto> {
        return this.http.get<DolorDto>(`${this.apiUrl}/${id}`);
    }

    create(dto: DolorRequestDto): Observable<DolorDto> {
        return this.http.post<DolorDto>(this.apiUrl, dto);
    }

    update(id: number, dto: DolorRequestDto): Observable<DolorDto> {
        return this.http.put<DolorDto>(`${this.apiUrl}/${id}`, dto);
    }

    delete(id: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/${id}`);
    }
}
