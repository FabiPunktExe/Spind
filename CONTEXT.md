# Spind - AI agent context

Spind is a Kotlin Multiplatform (KMP) password manager that synchronizes encrypted vaults with a self-hosted server. It supports Android, Desktop (JVM), Web (Wasm/JS), and a Ktor-based backend.

## Project Structure
The project is divided into three main modules:
- `composeApp`: The client application built with Compose Multiplatform.
  - `commonMain`: Shared UI and logic across all client platforms.
  - `androidMain`, `jvmMain`, `webMain`, `wasmJsMain`, `jsMain`: Platform-specific implementations (e.g., storage, platform-specific APIs).
- `server`: A Ktor-based backend server for storing and serving encrypted vaults.
  - Uses a simple file-based storage system.
  - Supports basic authentication for vault access.
- `shared`: Code shared between both the `composeApp` and the `server`.
  - Contains data models, common error types (`ApiError`), and utility classes (`Either`).

## Tech Stack
- Languages: Kotlin (Multiplatform)
- UI Framework: Compose Multiplatform
- Networking: Ktor Client & Server
- Serialization: Kotlinx Serialization (JSON & CBOR)
- Dependency Management: Gradle with Version Catalogs (`libs.versions.toml`)
- Database/Storage: 
  - Client: Platform-specific (e.g., `AndroidStorage`, `WebStorage`, `JvmStorage`)
  - Server: File-based storage
- Concurrency: Kotlin Coroutines

## Security Model
Spind implements a zero-knowledge architecture where the server never sees the user's master password or the decrypted vault data.

### Key Derivation & Hashing
1. Master Password: Entered by the user.
2. Password Hash: `SHA3-256(Master Password)`. Used for local key derivation.
3. Secret (API Token): `SHA3-256(Password Hash)`. Used for Basic Auth with the server.
4. Encryption Key: First 32 characters of the `Password Hash`.

### Encryption
- Algorithm: AES-256-CBC
- Hashing: SHA3-256
- Vault Format: Versioned binary blob (currently Version 1).
  - Contains encrypted password data, security questions, and optional backup/recovery data.
- Backup Password: Derived from security question answers.

## API Endpoints (v1)
- `GET /v1/vault`: Retrieve the encrypted vault blob. Requires Basic Auth (Username + Secret).
- `PUT /v1/vault`: Upload/Update the encrypted vault blob. Requires Basic Auth (Username + Secret).

## Build and Run
Use `.\gradlew.bat` on Windows or `./gradlew` on Unix-based systems.
### Android
- `./gradlew :composeApp:assembleDebug`
### Desktop (JVM)
- `./gradlew :composeApp:jvmJar`
- `./gradlew :composeApp:run`
### Web (JS)
- `./gradlew :composeApp:jsJar`
- `./gradlew :composeApp:jsBrowserDevelopmentRun`
### Web (Wasm)
- `./gradlew :composeApp:wasmJsJar`
- `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`
### Server
- `./gradlew :server:jar`
- `./gradlew :server:run`
- Environment variables: `SPIND_PORT` (default: 8080)

## Development Guidelines
- Shared Logic: Prefer putting logic in `shared` if it's used by both client and server.
- UI Components: Place reusable Compose components in `composeApp/src/commonMain/kotlin/de/fabiexe/spind/component`.
- Data Models: Defined in `composeApp/src/commonMain/kotlin/de/fabiexe/spind/data` for client-side and `shared` for common ones.
- Platform-Specifics: Use the `expect`/`actual` pattern or interfaces with platform-specific implementations (e.g., `Storage` interface).
- Style: Follow standard Kotlin coding conventions. Maintain consistency with the existing codebase (e.g., use of `Material3`).
