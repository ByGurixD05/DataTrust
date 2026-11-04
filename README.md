# DataTrust

API REST para catalogar datasets CSV, conservar versiones inmutables de sus esquemas y validar archivos CSV frente a esos esquemas. Incluye registro de usuarios, autenticación JWT y acceso a datasets limitado por propietario o rol `ADMIN`.

> **Estado:** proyecto en desarrollo. La API cubre registro e inicio de sesión, catálogo versionado, consulta paginada y validación de CSV. No incluye todavía edición/creación de nuevas versiones mediante API, reglas de calidad configurables, informes persistidos ni puntuación de calidad.

## Funcionalidades

- Registro público como `DATA_STEWARD` e inicio de sesión con JWT Bearer.
- Alta de datasets CSV con esquema inicial versionado e inmutable.
- Consulta individual y listado paginado; los `DATA_STEWARD` solo ven sus datasets y `ADMIN` puede consultar todos.
- Validación de CSV en streaming respecto a las columnas requeridas y sus tipos (`STRING`, `INTEGER`, `DECIMAL`, `BOOLEAN`, `DATE`).
- Migraciones de PostgreSQL con Flyway y errores REST en formato `application/problem+json`.

## Tecnologías

Java 17, Spring Boot 4.1.1, Spring Security, Spring Data JPA, PostgreSQL 16, Flyway, Maven Wrapper, Docker Compose y Testcontainers.

## Requisitos

- Git
- Docker Desktop o Docker Engine con Docker Compose
- JDK 17 para ejecutar Maven localmente (opcional si solo se usa Docker)

Las pruebas de integración requieren Docker porque usan PostgreSQL mediante Testcontainers.

## Inicio rápido

Clona el repositorio y prepara la configuración local:

```bash
git clone https://github.com/ByGurixD095/DataTrust.git
cd DataTrust
cp .env.example .env
```

Edita `.env`: establece una contraseña local para PostgreSQL y genera un `JWT_SECRET` Base64 de al menos 32 bytes. En PowerShell:

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

Pega el resultado como valor de `JWT_SECRET`. No compartas ni subas `.env`; el archivo está excluido de Git. Después inicia los servicios:

```bash
docker compose up --build -d
docker compose ps
docker compose logs -f app
```

La API estará disponible en `http://localhost:8080`. Compose publica PostgreSQL solo en `127.0.0.1:5432`. Para detener los servicios conservando los datos ejecuta `docker compose down`; para borrar también los datos locales ejecuta `docker compose down -v`.

## Configuración

| Variable | Obligatoria | Valor de ejemplo / predeterminado | Uso |
| --- | --- | --- | --- |
| `POSTGRES_DB` | Sí en Compose | `datatrust` | Base de datos PostgreSQL |
| `POSTGRES_USER` | Sí en Compose | `datatrust` | Usuario de PostgreSQL |
| `POSTGRES_PASSWORD` | Sí en Compose | Definir localmente | Contraseña de PostgreSQL |
| `JWT_SECRET` | Sí | Sin valor predeterminado | Clave Base64 que debe decodificar al menos 32 bytes |
| `DATATRUST_MAX_UPLOAD_SIZE` | No | `10MB` | Máximo para el archivo y la petición multipart |

Al iniciar, Flyway aplica las migraciones y Hibernate valida el esquema existente. La aplicación no crea ni modifica el esquema automáticamente.

## API

Base URL: `http://localhost:8080/api/v1`. Las rutas de datasets requieren `Authorization: Bearer <accessToken>`.

### Autenticación

`POST /auth/register` crea una cuenta `DATA_STEWARD` y devuelve un JWT. El cliente no puede asignarse un rol. Ejemplo:

```json
{
  "username": "ana",
  "password": "una-clave-local-segura",
  "displayName": "Ana García",
  "email": "ana@example.com"
}
```

`POST /auth/login` recibe `username` y `password`. Ambos endpoints devuelven `accessToken`, `tokenType`, `expiresAt` y `userId`. El token expira en 15 minutos; no hay refresh token ni revocación.

### Datasets

- `POST /datasets`: registra un dataset y su esquema inicial; responde `201 Created` con `Location`.
- `GET /datasets/{id}`: consulta un dataset accesible para el usuario.
- `GET /datasets?page=0&size=20`: lista datasets accesibles. `page` empieza en cero y `size` admite de 1 a 100.
- `POST /datasets/{id}/validations` (`multipart/form-data`, campo `file`): valida un CSV frente al esquema vigente.

Ejemplo de alta completa: [`docs/ejemplos/crear-dataset.json`](docs/ejemplos/crear-dataset.json). La propiedad `ownerId` se deriva de la identidad autenticada y no se acepta en el cuerpo. Las versiones existentes no se pueden modificar; la API aún no crea versiones posteriores.

La validación devuelve el total de filas, errores por columna, errores de cabecera y hasta 100 ejemplos de celdas inválidas. Los contadores incluyen todas las incidencias. El tamaño máximo predeterminado es 10 MB; se puede cambiar con `DATATRUST_MAX_UPLOAD_SIZE`.

Los errores usan `application/problem+json`. Los estados más habituales son `400` (petición o CSV inválido), `401` (sin autenticación), `403` (sin permiso), `404` (dataset inexistente), `409` (duplicado o conflicto de concurrencia) y `413` (archivo demasiado grande). Los errores inesperados incluyen un `correlationId` sin exponer detalles internos.

## Desarrollo

Para ejecutar la verificación Maven (incluye pruebas que requieren Docker):

```powershell
./mvnw.cmd verify
```

En Linux/macOS:

```bash
./mvnw verify
```

Para iniciar desde Maven, configura las variables de PostgreSQL y `JWT_SECRET` en el entorno y ejecuta `./mvnw spring-boot:run` (en Windows, `./mvnw.cmd spring-boot:run`).

Consulta [CONTRIBUTING.md](CONTRIBUTING.md) para preparar cambios. Las decisiones de diseño y las limitaciones conocidas están en [`docs/decisiones.md`](docs/decisiones.md).

## Seguridad

No incluyas secretos, credenciales reales ni datos sensibles en commits o incidencias públicas. Para reportar una vulnerabilidad, consulta [SECURITY.md](SECURITY.md).

## Licencia

Este proyecto se distribuye bajo la licencia [MIT](LICENSE).
