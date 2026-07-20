# Spind - AI agent context

Spind is a Kotlin Multiplatform (KMP) password manager that synchronizes encrypted vaults with a self-hosted server. It supports Android, Desktop (JVM), and a Ktor-based backend. The client and server share a zero-knowledge protocol: the server only ever stores a derived secret and the encrypted vault blob, never the plaintext password or the decrypted data.

## Project Structure
The project is built with **Amper / Kotlin CLI** (Amper 0.11.0) via the `./kotlin` wrapper at the repository root (`kotlin` on Unix, `kotlin.bat` on Windows). There is no Gradle wrapper anymore. The project manifest is `project.yaml`; each module has a `module.yaml`. The modules declared in `project.yaml` are:

- `app`: The Compose Multiplatform **client library** (`product: kmp/lib`, platforms: `jvm`, `android`). Contains the shared UI and client logic (vault handling, cryptography, Ktor client calls) consumed by the platform wrappers. Depends on `protocol`.
- `jvm-app`: The **JVM desktop application wrapper** (`product: jvm/app`). Thin entry point that depends on `app` and `compose.desktop.currentOs` to launch the Compose desktop UI.
- `android-app`: The **Android application wrapper** (`product: android/app`). Depends on `app` and packages the client for Android with the Compose Android runtime.
- `server`: The **Ktor backend** (`product: jvm/app`, main class `de.fabiexe.spind.server.MainKt`). Stores and serves encrypted vault blobs. Uses file-based storage and HTTP Basic authentication. Depends on `protocol`.
- `protocol`: **Shared models and DTOs** (`product: kmp/lib`, platforms: `jvm`, `android`). Contains data models, the protocol version constant, common error types (`ServerError`), and the request/response classes exchanged between `app` and `server`. Both `app` and `server` depend on it.

## Tech Stack
- Languages: Kotlin (Multiplatform)
- UI Framework: Compose Multiplatform
- Networking: Ktor Client (in `app`) & Ktor Server / Netty (in `server`)
- Serialization: Kotlinx Serialization (JSON)
- Build tool: Amper / Kotlin CLI via the `./kotlin` wrapper (Amper 0.11.0); project manifest `project.yaml`, per-module `module.yaml`. Dependency versions are declared with version catalog references (`libs.versions.toml`) inside the `module.yaml` files.
- Storage:
  - Client: Platform-specific storage implementations behind a common `Storage` interface.
  - Server: File-based storage.
- Concurrency: Kotlin Coroutines

## Security Model
Spind implements a zero-knowledge architecture where the server never sees the user's vault password or the decrypted vault data. All cryptographic work happens client-side via **cryptography-kotlin** (SHA3-256, AES-GCM, PBKDF2) and **at.favre.lib:bcrypt** (BCrypt).

### Key Derivation, Authentication & Encryption
1. Vault password: Entered by the user.
2. Secret: `secret = SHA3-256(vaultPassword)` computed with cryptography-kotlin. Its hex form is `secretHex`.
3. Auth credential (Basic-Auth password): `hex(BCrypt.hash(secretHex))` computed with at.favre.lib:bcrypt. The Basic-Auth username is the vault name.
4. Encryption key: A key derived from the vault password with PBKDF2 (cryptography-kotlin), used for AES-GCM encryption of the vault payload.
5. First PUT registration: The first `PUT /v1/vault?new-secret=<secretHex>` stores `secretHex` server-side so the server can verify the BCrypt credential on subsequent requests.

### Encryption
- Algorithm: AES-GCM (cryptography-kotlin), key derived from the vault password via PBKDF2.
- Hashing: SHA3-256 (cryptography-kotlin) for secret derivation; BCrypt (at.favre.lib:bcrypt) for the auth credential.
- Vault payload: An opaque binary blob encrypted on the client. The server stores only the derived secret (`secretHex`) and the encrypted blob — it can never decrypt the contents.

## API Endpoints (v1)
The server exposes exactly four endpoints under `/v1`:

- `GET /v1/version` (no auth) -> `{"version": <PROTOCOL_VERSION>}`. Reports the protocol version.
- `GET /v1/vault/modification-time?vault=<name>` (no auth) -> the vault's last modification time. Returns `VAULT_NOT_FOUND` if the vault does not exist and `VAULT_NOT_INITIALIZED` if it exists but has no data yet.
- `GET /v1/vault` (HTTP Basic, username = vault name, password = `hex(BCrypt.hash(secretHex))`) -> the opaque encrypted vault blob as raw bytes.
- `PUT /v1/vault?new-secret=<secretHex>` (HTTP Basic, same credentials as above) with the encrypted blob as the request body. The first PUT (with `new-secret`) registers `secretHex` with the server; subsequent PUTs update the stored blob.

There are no other endpoints: security questions, vault recovery, and backup passwords have been removed.

## Build and Run
Builds use the **Amper / Kotlin CLI** through the `./kotlin` wrapper at the repository root (`kotlin` on Unix, `kotlin.bat` on Windows). There is no `gradlew`. The task graph is listed with `./kotlin show tasks` (run from the repo root, no per-module argument) and an individual task is run with:

```
./kotlin task <task-name>
```

where `<task-name>` is the full task identifier printed by `./kotlin show tasks` (e.g. `:server:runJvm`). Applications can also be launched directly with `./kotlin run -m <module>`.

The exact task names below were confirmed via `./kotlin show tasks`. Re-run that command if tasks change.

### Server
- Run the server (from source): `./kotlin run -m server` (equivalently `./kotlin task :server:runJvm`)
- Build an executable JAR: `./kotlin task :server:executableJarJvm`
- Run the executable JAR: `./kotlin task :server:runExecutableJarJvm`
- Run server tests: `./kotlin task :server:testJvm`
- Port: `SPIND_PORT` is a **JVM system property** (default `8080`), read via `System.getProperty("SPIND_PORT")`. Set it with a JVM arg, e.g.:
  - `./kotlin run -m server --jvm-args="-DSPIND_PORT=9000"`

### Desktop client (JVM)
- Run the desktop client: `./kotlin run -m jvm-app` (equivalently `./kotlin task :jvm-app:runJvm`)
- Build an executable JAR: `./kotlin task :jvm-app:executableJarJvm`
- Run the executable JAR: `./kotlin task :jvm-app:runExecutableJarJvm`
- Run desktop client tests: `./kotlin task :jvm-app:testJvm`

### Android app
- Build a debug Android app: `./kotlin task :android-app:buildAndroidDebug`
- Build a release Android app: `./kotlin task :android-app:buildAndroidRelease`
- Build an Android bundle (release AAB): `./kotlin task :android-app:bundleAndroid`
- Build an Android debug JAR/AAR: `./kotlin task :android-app:jarAndroidDebug`
- Run on an Android device/emulator (debug): `./kotlin task :android-app:runAndroidDebug`
- Run Android tests: `./kotlin task :android-app:testAndroidDebug`

### Whole project
- Build everything: `./kotlin build`
- Run all checks: `./kotlin check`
- Run all tests: `./kotlin test`
- Clean build outputs: `./kotlin clean`

## Development Guidelines
- Shared Logic: Prefer putting logic in `protocol` if it's used by both client and server.
- UI Components: Place reusable Compose components in `app/src/de/fabiexe/spind/app/component` (Amper layout — common sources live under `app/src/`, platform-specific sources under `app/src@jvm/` and `app/src@android/`).
- Data Models: Defined in `app/src/de/fabiexe/spind/app/data` for client-side and `protocol` for common ones.
- Platform-Specifics: Use the `expect`/`actual` pattern or interfaces with platform-specific implementations (e.g., the `Storage` interface).
- Style: Follow standard Kotlin coding conventions. Maintain consistency with the existing codebase (e.g., use of `Material3`).