# Decisiones de diseño: DataTrust

Oct 21, 2026 · @Gonzalo

Registro de decisiones de las entregas 1 y 2. Las decisiones de la entrega 1 que la entrega 2 cambió están marcadas como **superadas** y conservan el motivo del cambio.

## Stack y entorno

| Tema | Decisión | Motivo |
| --- | --- | --- |
| Lenguaje | Java 17 (`java.version` 17 en `pom.xml`) | Versión del proyecto. El Dockerfile usa imágenes 17 para coincidir con el entorno local. |
| Framework | Spring Boot 4.1.1 | Versión generada por el inicializador. Algunos paquetes y starters son propios de Spring Boot 4. |
| Base de datos | PostgreSQL 16 (`postgres:16-alpine`) | Mismo motor en Compose, Testcontainers y producción. H2 descartado: no coincide en restricciones e índices. |
| Migraciones | Flyway: V1 (catálogo), V2 (usuarios), V3 (endurecimiento del versionado) | El esquema se versiona en SQL, no con `ddl-auto`. |
| Validación de esquema | `spring.jpa.hibernate.ddl-auto=validate` | Hibernate comprueba que las entidades coinciden con las tablas al arrancar. |
| Contenedores | Dockerfile multietapa: Maven compila, JRE ejecuta, usuario sin privilegios | Imagen de ejecución pequeña y sin herramientas de compilación. |
| Tests de integración | Testcontainers con PostgreSQL real | Las restricciones, los índices y los triggers se comprueban contra el motor real. |
| Tests fuera de Docker | Se ejecutan con `mvn verify` desde la máquina de desarrollo | Testcontainers necesita el Docker del host, que no existe durante el `docker build`. |

## Modelo de datos

**Identificadores UUID generados en el dominio.** `Dataset.create` y `DatasetVersion.create` generan el id con `UUID.randomUUID()`. Las entidades JPA no usan `@GeneratedValue`.

**Nombres únicos sin distinguir mayúsculas.** Nombre de dataset único en toda la base (`ux_datasets_name_lower`). Usuario y email únicos sin distinguir mayúsculas (`ux_users_username_lower`, `ux_users_email_lower`).

**Versionado (entrega 2).** Un dataset apunta a su versión vigente con `datasets.current_version_id`. Las versiones viven en `dataset_versions`, con `version_number` en texto `x.y.z` y el esquema en `schema_snapshot` (JSONB).

- El número de versión se comprueba con un `CHECK` de formato `x.y.z` sin ceros a la izquierda.
- No se ordena por `version_number`. La versión vigente se obtiene siempre por `current_version_id`.
- La FK de `current_version_id` es compuesta, `(id, current_version_id) → dataset_versions(dataset_id, id)`, diferida hasta el commit. Impide apuntar a una versión de otro dataset.
- Las versiones son inmutables en la base de datos: un trigger rechaza `UPDATE` y `DELETE`. El borrado en cascada al eliminar el dataset padre sí se permite, porque en ese momento la fila de `datasets` ya no existe.
- `datasets.created_at` guarda la fecha de creación. Las filas anteriores a V3 la tomaron de su primera versión.

**Columnas dentro del snapshot (entrega 2, superada la tabla `dataset_columns` de la entrega 1).** Las columnas viven en el JSON del snapshot, con sus `id`. Renombrar campos de esos records rompe la lectura de versiones guardadas.

**Tipos de columna: `DECIMAL` en lugar de `FLOAT` (entrega 2).** `ColumnType` tiene `STRING`, `INTEGER`, `DECIMAL`, `BOOLEAN` y `DATE`. `DECIMAL` se comprueba con `BigDecimal`, para que la comparación con el valor del CSV no pierda precisión por redondeo. Cada tipo sabe validar un texto (`accepts`).

**Formato de dataset: solo `CSV`.** Un `CHECK` y no una tabla de búsqueda, porque hay un único formato.

**Marcas de tiempo con zona horaria.** `TIMESTAMPTZ`, mapeado a `Instant`.

**Bloqueo optimista con `lock_version`.** Mapeado con `@Version`. El conflicto de concurrencia responde 409.

**Superadas de la entrega 1:**

- **Versión codificada en un entero** (`major * 1000000 + minor * 1000 + patch`). Superada por el texto `x.y.z` con `CHECK`. El entero limitaba el major a 2146 sin razón de negocio, y no permitía ordenar versiones correctamente sin lógica adicional.
- **`V1` como única migración.** Superada por V2 y V3. V1 y V2 no se modifican una vez aplicadas, porque cambiar su contenido cambia el checksum de Flyway.
- **Borrado en cascada de columnas** (`ON DELETE CASCADE` sobre `dataset_columns`). Ya no existe esa tabla: las columnas viven en el snapshot de cada versión.

## Arquitectura

**Dominio sin anotaciones de persistencia.** `Dataset`, `DatasetVersion`, `DatasetColumn` y `ColumnType` están en `domain` sin JPA ni Spring. Las entidades JPA viven en `infrastructure/persistence` y un adaptador traduce entre ambos. Lo verifica `ArchitectureTest`.

**Fábricas `create` y `restore`.** `create` valida las invariantes. `restore` reconstruye desde la base sin validar, porque los datos ya se validaron al guardarse. El constructor es privado.

**Puertos en `application/ports`.** `DatasetRepository`, `UserRepository`, `PasswordHasher`, `AccessTokenIssuer` y `CurrentUserProvider` definen los contratos. La capa de aplicación no conoce JPA ni JWT.

**Un único servicio de catálogo.** `DatasetService` agrupa `register`, `get` y `list`.

**Transacciones en el servicio.** `@Transactional` está en `DatasetService` y `AuthService`. Las lecturas son `readOnly = true`. `open-in-view` está desactivado. El servicio de validación **no** es transaccional: el fichero puede tardar y no debe retener una conexión a la base de datos.

**Duplicados detectados por la base de datos.** El servicio no hace un `SELECT` previo. El adaptador captura la violación del índice y la traduce a `DuplicateDatasetNameException` o `DuplicateUserException`.

**Errores de dominio con código estable.** `DatasetValidationException` lleva un `DatasetErrorCode`. El cliente trata el error por el código, no por el texto.

**Validación de contenido separada del formato (entrega 2).** Las reglas no dependen de ningún formato:

- `RowReader` es la interfaz de lectura en streaming. Cada formato la implementa en infraestructura.
- `TabularRow` es la fila genérica: número y valores por nombre de columna normalizado.
- `RowReaderFactory` es la fábrica creacional: crea el lector adecuado para un `DatasetFormat`. Añadir un formato es implementar `RowReader` y registrarlo en `FormatRowReaderFactory`.
- `ContentValidator` aplica `required` y `expectedType` a cualquier `RowReader`.
- `CsvRowReader` es el único código que conoce Commons CSV, y vive en `infrastructure`.

**Limitación conocida.** La fábrica recibe un `InputStream`, que sirve para ficheros pero no para una base de datos relacional. Ese caso requerirá un puerto de fuente más general.

**Excepciones en la capa que las detecta.** Las reglas de negocio están en `domain`. Un recurso inexistente en `application`. Un contenido mal formado en `application` (`MalformedContentException`). Solo `api` conoce HTTP.

## API REST

**Ruta base `/api/v1/datasets`.** Versionada desde el principio.

**Creación con `201` y cabecera `Location`.** `POST /api/v1/datasets`.

**El propietario sale del token, nunca del cuerpo.** Se obtiene de `CurrentUserProvider` (`sub` del JWT validado).

**Listado paginado con límite y orden estable.** `GET /api/v1/datasets` acepta `page` (mínimo 0) y `size` (entre 1 y 100, por defecto 20). El orden es por fecha de creación y después por id, así que las páginas no se solapan.

**Validación de un fichero (entrega 2).** `POST /api/v1/datasets/{id}/validations`, `multipart/form-data`, campo `file`. Responde `200` con el informe:

```json
{
  "rows": 2,
  "valid": false,
  "headerErrors": [],
  "errorsByColumn": { "id": 1 },
  "samples": [
    { "row": 1, "column": "id", "value": "abc", "reason": "no es un valor válido de tipo INTEGER" }
  ]
}
```

El informe no se guarda: es una respuesta, no un recurso. Solo se devuelven las primeras 100 incidencias; los contadores cuentan todas.

**Límite de tamaño (entrega 2).** `spring.servlet.multipart.max-file-size` y `max-request-size` valen `${DATATRUST_MAX_UPLOAD_SIZE:10MB}`. Por encima del límite responde `413`, comprobado contra el servidor real.

**Tabla de errores.**

| Caso | Estado | Propiedad adicional |
| --- | --- | --- |
| `DatasetValidationException` | 400 | `code`, `field` |
| Bean Validation y validación de parámetros | 400 | `errors` |
| JSON mal formado, UUID inválido, fichero ausente | 400 | — |
| `MalformedContentException` (CSV mal formado) | 400 | — |
| Token ausente o inválido | 401 | — |
| `AuthenticationException` en un controlador | 401 | — |
| `AccessDeniedException` en un controlador | 403 | — |
| `DatasetNotFoundException` | 404 | `datasetId` |
| `DuplicateDatasetNameException` y `DuplicateUserException` | 409 | `conflictingName` / `field` |
| `ObjectOptimisticLockingFailureException` | 409 | — |
| Fichero mayor que el límite | 413 | Generado por Spring (en inglés) |
| Cualquier otro error | 500 | `correlationId` |

**El 500 no revela detalles internos.** La excepción se registra en el log con el `correlationId`.

**Sin actualización ni borrado.** Solo crear y consultar, como pedían RF-01 y RF-02.

## Seguridad

**JWT stateless con HS256.** Spring Security valida los bearer tokens. El emisor incluye el UUID del usuario como `sub`, junto con username y rol. El secreto Base64 se exige mediante `JWT_SECRET` y debe representar al menos 32 bytes.

**Registro público limitado a `DATA_STEWARD`.** El cliente nunca escoge su rol.

**Contraseñas con BCrypt (coste 12).** Longitud entre 12 y 72 bytes UTF-8, porque BCrypt ignora lo que pase del byte 72.

**Login sin revelar si el usuario existe (entrega 2).** Si el usuario no existe, se ejecuta igualmente una comparación BCrypt contra un hash de referencia. Todas las credenciales inválidas responden 401 con el mismo mensaje.

**Identidad de datasets desde el token.** `JwtCurrentUserProvider` obtiene el propietario del `sub` validado. El cuerpo de creación no admite propietario.

**Autorización por rol y propietario.** Un `DATA_STEWARD` solo puede consultar y validar datasets cuyo `owner_id` coincide con su identidad del token; su listado también queda limitado a esos datasets. Un `ADMIN` puede consultar, listar y validar datasets de cualquier propietario. En las rutas por ID se comprueba primero la existencia (`404`) y después el permiso (`403`).

## Pruebas

**Unitarias sin Spring ni Docker.** `DatasetTest`, `DatasetColumnTest`, `ColumnTypeTest`, `ContentValidatorTest` (sobre un lector en memoria) y `CsvRowReaderTest`.

**Persistencia con PostgreSQL real.** `DatasetPersistenceAdapterTest`, con `@DataJpaTest` y Testcontainers.

**Migraciones.** `FlywayMigrationTest` sobre base vacía. `DatasetVersioningMigrationTest` comprueba las reglas de V3 directamente en SQL: versión inmutable, borrado bloqueado, versión de otro dataset rechazada, formato `x.y.z`, cascada al borrar un dataset y nueva versión como vigente.

**API con `MockMvc`.** `AuthApiTest`, `DatasetApiTest`, `DatasetListingOrderTest` y `ValidationApiTest`.

**Límite de tamaño, fuera de los tests automáticos.** `MockMvc` no aplica el límite del contenedor, así que el 413 se comprueba a mano contra el servidor real.

**Persistencia tras reinicio.** `DatasetPersistenceAcrossRestartTest`.

**Arquitectura.** `ArchitectureTest` con ArchUnit 1.4.1: el dominio no usa frameworks ni capas externas; la aplicación no depende de `api` ni de `infrastructure`; la API no depende de `infrastructure`.

Si el puerto de Ryuk está bloqueado, `TESTCONTAINERS_RYUK_DISABLED=true` lo desactiva durante la sesión.

## Entrega y ejecución

**Compose con PostgreSQL y aplicación.** La base tiene `healthcheck` con `pg_isready`. El puerto de PostgreSQL se publica solo en `127.0.0.1`.

**Credenciales en `.env`.** No van en el repositorio. `.env` está en `.gitignore` y en `.dockerignore`. `.env.example` solo tiene valores de ejemplo.

**Ejemplos en `docs/ejemplos/`.** En Windows, `curl.exe` envía el JSON con `--data-binary "@fichero.json"`, porque el escapado de comillas en PowerShell 5 no es fiable.

## Pendientes

- [ ] Operación de nueva versión: crear una versión y marcarla como vigente, con política de numeración (la nueva debe ser mayor que la vigente). Sección 7.8 del análisis.
- [x] Autorización por rol y por propietario en lectura y validación.
- [ ] Decidir si el CSV puede pasar por disco. Con multipart, Tomcat escribe en un temporal por encima de `file-size-threshold`. Si el requisito es no escribir nada, pasar el CSV como cuerpo crudo.
- [ ] Verificar la numeración de fila del informe con Commons CSV real y documentarla en la API.
- [ ] Mensaje del 413 en español, con el mismo formato que el resto de errores (opcional).
- [ ] Actualizar `README.md` con el endpoint de validación y el límite de tamaño.

## Qué cambió respecto al plan inicial

| Plan inicial | Lo que se hizo | Motivo |
| --- | --- | --- |
| Java 21 | Java 17 | El entorno local tiene JDK 17. |
| Tres servicios de aplicación | `DatasetService` único | Una operación por servicio no aportaba nada. |
| Versión de dataset como entero codificado | `x.y.z` en texto, con `CHECK` y snapshot JSONB (entrega 2) | El entero no ordenaba bien y limitaba el major sin razón de negocio. |
| `FLOAT` para valores decimales | `DECIMAL` con `BigDecimal` (entrega 2) | `FLOAT` introduce errores de redondeo al comparar. |
| Seguridad desde el principio | Registro de DATA_STEWARD y autenticación JWT | La identidad del propietario proviene del token validado. |
| Validación con `@Validated` en el controlador | Validación de Spring MVC | `@Validated` lanza una excepción que el manejador no cubre. |
| Subida de CSV en el controlador del catálogo | Endpoint de validación separado (entrega 2) | Pertenece a la validación, no al catálogo. |
| Validación del contenido ligada al CSV | Lector por formato, reglas genéricas y fábrica (entrega 2) | Permite añadir JSON o una base de datos sin tocar las reglas. |
| Versiones modificables y borrado en cascada | Versiones inmutables por trigger (entrega 2) | Un historial de calidad no puede reescribirse. |
