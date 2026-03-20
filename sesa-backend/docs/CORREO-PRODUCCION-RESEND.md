# Correo en producción con Resend (SESA)

El backend **ya está listo**: con perfil `prod` lee la API key y el remitente desde **variables de entorno**. No hace falta cambiar código ni subir secretos al repositorio.

---

## Checklist previo (una sola vez)

1. **Dominio verificado en Resend** (`appsesa.online` o el que uses): DNS con DKIM, MX `send` y SPF `send` como indica el panel de Resend.
2. Estado del dominio en Resend: **Verified**.
3. **API Key** en Resend → *API Keys* → crear clave de producción (`re_...`).

---

## Paso a paso en Render (Web Service del backend)

1. Entra en [Render Dashboard](https://dashboard.render.com) → tu servicio **Web Service** del API SESA.
2. Menú **Environment** (Variables de entorno).
3. Añade o edita estas variables (nombres **exactos**):

| Variable | Valor | Obligatorio |
|----------|--------|-------------|
| `SPRING_PROFILES_ACTIVE` | `prod` | Sí (si no está ya) |
| `RESEND_API_KEY` | `re_tu_clave_secreta` | Sí, para enviar correo |
| `SESA_EMAIL_ENABLED` | `true` | Sí, para activar envío |
| `SESA_EMAIL_FROM` | `SESA <noreply@appsesa.online>` | Sí; debe ser dominio **verificado** en Resend |
| `SESA_FRONTEND_URL` | `https://appsesa.online` | Recomendado (enlaces en plantillas) |
| `SESA_EMAIL_LOGO_URL` | `https://appsesa.online/logo.png` | Opcional (si el logo no carga, define URL absoluta HTTPS) |

4. Pulsa **Save Changes**. Render **redesplegará** el servicio solo.
5. Espera a que el deploy termine en verde.
6. Prueba **recuperación de contraseña** desde el front con un usuario real: debería llegar el correo. Si falla, revisa **Logs** del servicio en Render y **Logs** en Resend.

---

## Otros entornos (Docker, VM, Kubernetes)

Mismas variables de entorno al arrancar el contenedor o el proceso Java:

```bash
export SPRING_PROFILES_ACTIVE=prod
export RESEND_API_KEY=re_...
export SESA_EMAIL_ENABLED=true
export SESA_EMAIL_FROM="SESA <noreply@appsesa.online>"
export SESA_FRONTEND_URL=https://appsesa.online
```

En Docker:

```bash
docker run -e SPRING_PROFILES_ACTIVE=prod \
  -e RESEND_API_KEY=re_... \
  -e SESA_EMAIL_ENABLED=true \
  -e 'SESA_EMAIL_FROM=SESA <noreply@appsesa.online>' \
  ...
```

---

## Local vs producción

| Dónde | Cómo configurar |
|--------|------------------|
| **Local** | Archivo `application-local.yml` en la carpeta `sesa-backend` (ver `application-local.example.yml`) **o** variables `RESEND_API_KEY` + `SESA_EMAIL_ENABLED` en el IDE. |
| **Producción** | **Solo** variables de entorno en Render (u otro host). **No** subas `application-local.yml` ni la API key al Git. |

---

## Si no llegan correos

1. **Render → Logs**: busca `Resend error` o `Correo omitido`.
2. **Resend → Logs**: rechazos por dominio no verificado o `from` inválido.
3. Comprueba que `SESA_EMAIL_ENABLED=true` y que `RESEND_API_KEY` no tenga espacios ni comillas de más.
4. El `from` debe coincidir con un remitente permitido para el dominio verificado.

---

## Archivo de referencia

En la raíz del módulo `sesa-backend` existe `env.produccion.ejemplo.txt` con la lista de variables para copiar al panel del hosting (sin valores secretos).
