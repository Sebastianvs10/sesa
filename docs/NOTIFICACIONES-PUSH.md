# Notificaciones push y recordatorios — SESA Salud

## Resumen

- **Recordatorios automáticos de citas**: job programado cada hora; envía notificaciones in-app (y opcionalmente push) a pacientes con cita en las próximas 24 h y 1 h.
- **Registro de dispositivos**: el backend expone `POST /api/push/register` para registrar el token FCM/Web Push del usuario.
- **Firma de consentimientos**: el paciente puede ver y firmar consentimientos pendientes desde el portal/móvil (canvas de firma).

## Backend

### Recordatorios de cita

- **Job**: `RecordatorioCitaJob` se ejecuta cada hora (`sesa.recordatorios.cron`, por defecto `0 0 * * * ?`).
- **Lógica**: para cada tenant se buscan citas en ventana 24 h y 1 h (estado AGENDADA, sin recordatorio ya enviado). Se crea una notificación in-app (tipo `RECORDATORIO_CITA`) al usuario vinculado al paciente (`paciente.usuarioId`).
- **Requisito**: el paciente debe tener `usuario_id` en la tabla `pacientes` para recibir recordatorios (vínculo con el usuario del portal/móvil).

### Push (registro de token)

- **POST /api/push/register** (autenticado): body `{ "token": "<FCM o Web Push token>", "plataforma": "WEB" | "ANDROID" | "IOS" }`.
- **DELETE /api/push/register?token=...** (autenticado): elimina el token.
- Los tokens se guardan en `dispositivos_push`. Para enviar push real se debe integrar Firebase Cloud Messaging (FCM) o Web Push con VAPID.

### Cómo enviar push con FCM

1. Crear proyecto en Firebase Console y obtener `server key` o configurar Cuenta de servicio.
2. En el backend, al crear una notificación de recordatorio (o al enviar notificación manual), obtener los tokens de `DispositivoPushRepository.findByUsuarioId(usuarioId)` y llamar a la API HTTP de FCM (v1) para enviar el mensaje a cada token.
3. Opcional: exponer un endpoint interno o un job que lea notificaciones no enviadas por push y las envíe vía FCM.

## Frontend / PWA

- **Portal**: el usuario puede registrar su token cuando acepte notificaciones. Ejemplo tras login en portal:

```typescript
// Cuando el usuario acepte notificaciones (ej. en perfil o al instalar PWA)
this.pushService.registerToken(fcmToken, 'WEB').subscribe();
```

- **PWA**: para Web Push sin FCM se necesita Service Worker, suscripción con `pushManager.subscribe()` y clave VAPID. El token que se envía a `/api/push/register` sería el endpoint de la suscripción (o el token que devuelva tu proveedor).

## Mobile (app nativa o Ionic/Capacitor)

- En la app móvil, tras obtener el token FCM (Android/iOS), llamar a `POST /api/push/register` con el JWT del usuario y body `{ "token": "<fcm_token>", "plataforma": "ANDROID" | "IOS" }`.
- Al cerrar sesión, llamar a `DELETE /api/push/register?token=...` para dejar de recibir en ese dispositivo.

## Configuración opcional

En `application.yml`:

```yaml
sesa:
  recordatorios:
    cron: "0 0 * * * ?"   # Cada hora en el minuto 0
```

Para desactivar el job: `sesa.recordatorios.cron: "-"` (cron deshabilitado en Spring).
