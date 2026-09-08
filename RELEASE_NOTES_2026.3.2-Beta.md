# Arma3Sync 2026.3.2-Beta

Follow-up beta release for secure repository uploads. This build is intended
for testing before a final release.

## New

- Added `FTPS` as a repository upload protocol. It uses explicit TLS on the
  standard port `21`, negotiates `AUTH TLS` before login and protects data
  channels with `PROT P`. This supports FileZilla Server configurations that
  reject plain FTP authentication with `503 Use AUTH first`.
- Added `SFTP` as a repository upload protocol using Apache MINA SSHD and the
  standard SSH port `22`.
- SFTP uploads use password authentication, safe remote-path validation and a
  persistent SSH/SFTP session for the complete repository upload.

## Compatibility

- Existing FTP, HTTP, HTTPS and WebDAV repositories and the legacy `a3s.xml`
  format remain unchanged.
- The new protocols are selectable in the repository and event upload
  settings; existing download protocol choices are not changed.
- FTPS certificate validation is enabled by default. Disabling TLS certificate
  validation remains restricted to the existing local-test policy.

## Validation

- `gradle clean test :updater:test` passed.
- The Fat-JAR was built successfully and contains the SFTP transport and its
  Apache MINA SSHD dependencies.
- A live FTPS/SFTP server test is still recommended before final publication.

## Build artifacts

- `Arma3Sync-2026.3.2-setup.exe` – Standard installer with bundled Java runtime
- `Arma3Sync-2026.3.2.zip` – Standard package
  - SHA-256: `38968a526e3b7093a5566c45caf1ed2d341947e60d98abc1e0d657ac175e31d5`
- `Arma3Sync-2026.3.2-compact-setup.exe` – Compact installer
- `Arma3Sync-2026.3.2-compact.zip` – Compact package without bundled runtime
  - SHA-256: `3c94685e7a621e59ea97e087dfe34e323baaa371469af5b73a2d1398540fba82`
