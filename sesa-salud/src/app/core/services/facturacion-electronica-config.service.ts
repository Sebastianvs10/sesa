/**
 * Configuración de facturación electrónica DIAN (frontend).
 * Autor: Ing. J Sebastian Vargas S
 */
import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface FacturacionElectronicaConfigDto {
  id?: number;
  facturacionActiva?: boolean;
  nit?: string;
  razonSocial?: string;
  nombreComercial?: string;
  regimen?: string;
  direccion?: string;
  municipio?: string;
  departamento?: string;
  pais?: string;
  emailContacto?: string;
  ambiente?: string;
  numeroResolucion?: string;
  fechaResolucion?: string | null;
  prefijo?: string;
  rangoDesde?: number | null;
  rangoHasta?: number | null;
  claveTecnica?: string;
  softwareId?: string;
  softwarePin?: string;
  plantillaPdf?: string;
}

@Injectable({ providedIn: 'root' })
export class FacturacionElectronicaConfigService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/facturacion/config-electronica`;

  getConfig(): Observable<FacturacionElectronicaConfigDto> {
    return this.http.get<FacturacionElectronicaConfigDto>(this.baseUrl);
  }

  updateConfig(dto: FacturacionElectronicaConfigDto): Observable<FacturacionElectronicaConfigDto> {
    return this.http.put<FacturacionElectronicaConfigDto>(this.baseUrl, dto);
  }
}

