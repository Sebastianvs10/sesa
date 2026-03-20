/**
 * Escribe la URL del API para builds de producción.
 * Si existe la variable de entorno SESAAPIURL (o SESAAPI_URL), se usa; si no, el valor por defecto (dominio producción).
 * Autor: Ing. J Sebastian Vargas S
 */
/* eslint-disable no-console */
const fs = require('fs');
const path = require('path');

const outFile = path.join(__dirname, '../src/environments/deploy-api-url.generated.ts');
const fromEnv =
  process.env.SESA_API_URL || process.env.SESAAPI_URL || process.env.SESAAPIURL;
// Por defecto API desplegada (Render). Override: SESA_API_URL para api.appsesa.online u otro host.
const defaultUrl = 'https://sesa-api.onrender.com/api';
const apiUrl = (fromEnv && String(fromEnv).trim()) || defaultUrl;

const content = `/**
 * Generado por scripts/apply-sesa-api-url.cjs — no editar a mano.
 * Override: SESA_API_URL (o SESAAPI_URL) en el entorno antes de npm run build.
 * Autor: Ing. J Sebastian Vargas S
 */
export const DEPLOY_API_URL = ${JSON.stringify(apiUrl)};
`;

fs.writeFileSync(outFile, content, 'utf8');
console.log('[apply-sesa-api-url] API producción:', apiUrl);
