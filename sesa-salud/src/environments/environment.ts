/**
 * Entorno de producción (ng build por defecto).
 * Base del API: deploy-api-url.generated.ts (por defecto Render) o SESA_API_URL en el build.
 * Autor: Ing. J Sebastian Vargas S
 */
import { DEPLOY_API_URL } from './deploy-api-url.generated';

export const environment = {
  production: true,
  apiUrl: DEPLOY_API_URL,
};
