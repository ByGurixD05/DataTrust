# Contribuir a DataTrust

Gracias por tu interés en mejorar DataTrust. Las contribuciones deben explicar el problema que resuelven y mantener las reglas de negocio, migraciones y documentación alineadas.

## Preparar el entorno

1. Instala JDK 17 y Docker con Compose.
2. Clona el repositorio y crea `.env` a partir de `.env.example`, con credenciales locales propias y una clave JWT aleatoria.
3. Crea una rama para tu cambio.

## Cambios

- Mantén el dominio independiente de Spring y JPA.
- Añade una migración Flyway nueva cuando cambie el esquema; no reescribas migraciones aplicadas.
- Añade o actualiza pruebas para las reglas y rutas afectadas.
- Actualiza README o documentación de API si cambia el comportamiento visible.
- No incluyas secretos, datos reales, `.env`, `target/` ni artefactos generados.

## Verificación

Antes de abrir un pull request, ejecuta `./mvnw verify` (Windows: `./mvnw.cmd verify`). Las pruebas de integración requieren Docker disponible. Describe en el pull request lo que cambió, cómo lo verificaste y cualquier limitación conocida.

## Pull requests

Mantén cada pull request centrado en un cambio comprensible. Incluye contexto suficiente para revisar el problema, la solución y cualquier impacto en compatibilidad o esquema de base de datos.
