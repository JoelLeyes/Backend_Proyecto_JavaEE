# Backend_Proyecto_JavaEE

Backend Java EE para el chat empresarial.

Incluye Servlets, capa DAO/Service, integracion con ECR y despliegue automatizable en EC2 mediante GitHub Actions.

## Estado AWS actual

- EC2 publica: `i-0105066ec62ac404a` (`nexolab-prod-app-ec2`) con EIP `44.210.180.79`.
- SG_EC2: HTTP 80, HTTPS 443 y SSH 22 restringido por IP.
- SG_RDS: PostgreSQL 5432 solo desde SG_EC2.
- RDS `rds-postgres`: privada, sin acceso publico.

## Atlas Logs

Atlas se usa solo como auditoria de eventos. No reemplaza a PostgreSQL.

- Base de datos: `java2026_logs`
- Colecciones: `users` y `messages`
- La aplicacion define los campos en codigo; Atlas no necesita un schema manual para funcionar.

### Coleccion `users`

Campos que escribe el backend:

- `createdAt`: fecha del evento
- `source`: origen del evento, por ejemplo `AuthServlet`
- `action`: `LOGIN`, `REGISTER`, `FORGOT_PASSWORD`, `RESEND_WELCOME`
- `success`: `true` o `false`
- `outcome`: `SUCCESS` o `ERROR`
- `userId`: id del usuario cuando ya existe
- `email`: email asociado al intento
- `summary`: descripcion corta del evento
- `errorMessage`: mensaje de error cuando falla
- `endpoint`: ruta HTTP
- `method`: metodo HTTP
- `ipAddress`: IP del cliente
- `userAgent`: user agent del cliente
- `details`: metadata adicional opcional

### Coleccion `messages`

Campos que escribe el backend:

- `createdAt`: fecha del evento
- `source`: origen del evento, por ejemplo `MessageServlet`
- `action`: `MESSAGE_SEND`
- `success`: `true` o `false`
- `outcome`: `SUCCESS` o `ERROR`
- `chatId`: id del chat
- `chatName`: nombre del chat
- `chatType`: tipo de chat
- `messageId`: id del mensaje cuando se persiste
- `senderId`: id del emisor
- `senderEmail`: email del emisor
- `replyToMessageId`: id del mensaje respondido, si aplica
- `contentLength`: largo del contenido enviado
- `hasAttachments`: si hubo adjuntos
- `attachmentCount`: cantidad de adjuntos
- `summary`: descripcion corta del evento
- `errorMessage`: mensaje de error cuando falla
- `endpoint`: ruta HTTP
- `method`: metodo HTTP
- `ipAddress`: IP del cliente
- `userAgent`: user agent del cliente
- `details`: metadata adicional opcional

