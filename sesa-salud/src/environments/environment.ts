/**
 * Entorno de producción (ng build por defecto).
 * API pública: api.appsesa.online (ver deploy-api-url.generated.ts / SESA_API_URL).
 * Autor: Ing. J Sebastian Vargas S
 */
import { DEPLOY_API_URL } from './deploy-api-url.generated';

export const environment = {
  production: true,
  apiUrl: DEPLOY_API_URL,
};
