# Spind - AI agent context

Spind is a Kotlin Multiplatform (KMP) zero-knowledge password manager that synchronizes AES-GCM-encrypted vaults with a self-hosted server. The clients run on Android and Desktop (JVM); the server is a Ktor backend that only ever stores a derived secret and the encrypted vault blob — it never sees the master password or plaintext data.

## Build Tool

Spind uses **Amper / Kotlin CLI** via the `./kotlin` wrapper (Amper 0.11.0) at the repository root (`kotlin` on Unix, `kotlin.bat` on Windows). There is **no `gradlew`** anymore. The project manifest is `project.yaml`; each module has its own `module.yaml`. Target JDK: 25 (Temurin).

List available tasks with:

```shell
./kotlin show tasks                    # list every task in the project
./kotlin show tasks | grep :server:    # filter to a single module
```

## Project Structure

Modules (defined in `project.yaml`); `app` and `server` both depend on `protocol`:

- `protocol` — shared Kotlin Multiplatform library (`kmp/lib`, JVM + Android). Contains the data models / DTOs exchanged between client and server (version, vault responses, error types), and the protocol version constant.
- `app` — shared Compose Multiplatform client library (`kmp/lib`, JVM + Android). Contains the UI, the Spind API client, and all client-side cryptography (secret derivation, BCrypt auth credential, AES-GCM encryption). Depends on `protocol`.
- `jvm-app` — JVM desktop application wrapper (`jvm/app`). Provides the Compose Desktop entry point and bundles `compose.desktop.currentOs`. Depends on `app`.
- `android-app` — Android application wrapper (`android/app`). Provides the Android entry point and platform wiring. Depends on `app`.
- `server` — Ktor/Netty backend (`jvm/app`). Stores the derived secret and the encrypted vault blob, verifies the BCrypt auth credential, and serves the four API endpoints. Depends on `protocol`.

## Tech Stack

- Languages: Kotlin (Multiplatform)
- UI Framework: Compose Multiplatform (Material 3)
- Networking: Ktor Client (in `app`) and Ktor Server / Netty (in `server`)
- Serialization: Kotlinx Serialization (JSON)
- Dependency Management: Amper `module.yaml` + `libs.versions.toml` (referenced as `$libs.*` and `$compose.*`/`$ktor.*` in module manifests)
- Cryptography: `cryptography-kotlin` (SHA3-256, AES-GCM, PBKDF2) and `at.favre.lib:bcrypt` (BCrypt for the auth credential)
- Storage:
  - Client: platform-specific storage behind the `Storage` interface
  - Server: file-based storage (see `server/.../storage`)
- Concurrency: Kotlin Coroutines

## Security Model

Spind implements a zero-knowledge architecture: the server only ever stores a derived secret and the encrypted vault blob. It can never see the user's master password or the decrypted vault.

### Client-side key derivation & encryption

1. **Master password** — entered by the user, never leaves the client.
2. **Secret (`secretHex`)** — `secret = SHA3-256(vaultPassword)` using **cryptography-kotlin**. This is the value the server stores to identify the vault.
3. **Auth credential** — the Basic-Auth password sent to the server is `hex(BCrypt.hash(secretHex))` using **at.favre.lib:bcrypt**. The server never receives `secretHex` in plaintext; it stores only the BCrypt hash and verifies it with `BCrypt.verifyer()`.
4. **Encryption key** — derived from the vault password via **PBKDF2** (cryptography-kotlin) and used to AES-GCM-encrypt the vault payload (passwords). The server never receives this key.
5. **First PUT (registration)** — the first `PUT /v1/vault?new-secret=<secretHex>` registers the vault's secret on the server. Subsequent PUTs omit `new-secret` and just update the encrypted blob.

### Encryption

- Algorithm: AES-GCM (cryptography-kotlin), key derived via PBKDF2 from the vault password.
- Hashing: SHA3-256 (cryptography-kotlin) for secret derivation; BCrypt (`at.favre.lib:bcrypt`) for the auth credential.
- Vault format: opaque encrypted blob from the server's perspective — the client serializes, encrypts, and uploads it; the server treats it as bytes.

> **Removed features (no longer exist):** security questions, vault recovery, backup passwords, and the old SHA3-256 double-hash derivation chain. There are no security-question, recovery, or backup endpoints.

## API Endpoints (v1)

The server exposes exactly four endpoints:

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| `GET` | `/v1/version` | none | Returns `{"version": <PROTOCOL_VERSION>}`. |
| `GET` | `/v1/vault/modification-time?vault=<name>` | none | Returns the last-modification time of the vault. |
| `GET` | `/v1/vault` | HTTP Basic (username = vault name, password = hex BCrypt hash) | Returns the opaque encrypted vault blob. |
| `PUT` | `/v1/vault?new-secret=<secretHex>` | HTTP Basic (username = vault name, password = hex BCrypt hash) | Stores/updates the encrypted vault blob. `new-secret` is sent only on the first PUT to register the vault. Body is the opaque encrypted blob. |

## Build and Run

All commands run from the repository root using the Amper `./kotlin` wrapper (`.\kotlin.bat` on Windows). Amper has no single root "build" task — compile each module individually. Run `./kotlin show tasks` to see the full list.

### Server (Ktor backend)

```shell
./kotlin task :server:compileJvm          # compile
./kotlin task :server:runJvm               # run (default port 8080)
./kotlin task :server:executableJarJvm     # build a runnable JAR
./kotlin task :server:runExecutableJarJvm  # run the runnable JAR
./kotlin task :server:testJvm              # run tests
```

Override the port by setting the `SPIND_PORT` system property (default `8080`).

### Desktop (JVM) client

```shell
./kotlin task :jvm-app:compileJvm          # compile
./kotlin task :jvm-app:runJvm              # run the desktop client
./kotlin task :jvm-app:executableJarJvm    # build a runnable desktop JAR
./kotlin task :jvm-app:runExecutableJarJvm # run the runnable desktop JAR
./kotlin task :jvm-app:testJvm             # run tests
```

### Android client

```shell
./kotlin task :android-app:compileAndroidDebug   # compile debug
./kotlin task :android-app:buildAndroidDebug      # build debug
./kotlin task :android-app:jarAndroidDebug       # package debug
./kotlin task :android-app:bundleAndroid          # bundle release
./kotlin task :android-app:runAndroidDebug        # build + launch on an emulator
./kotlin task :android-app:testAndroidDebug       # run tests
```

### Shared libraries

```shell
./kotlin task :app:compileJvm           # client library (JVM target)
./kotlin task :app:compileAndroidDebug  # client library (Android target)
./kotlin task :protocol:compileJvm      # protocol library (JVM target)
./kotlin task :protocol:compileAndroidDebug
```

## Development Guidelines

- **Shared models** go in `protocol` when used by both client and server; client-only shared code goes in `app`.
- **Reusable Compose components** live under `app/.../component`.
- **Platform-specifics** use the `expect`/`actual` pattern or interfaces with platform-specific implementations (e.g., the `Storage` interface).
- **Style:** follow standard Kotlin coding conventions and keep Material 3 usage consistent with the existing codebase.