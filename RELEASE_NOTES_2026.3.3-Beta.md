# Arma3Sync 2026.3.3-Beta

Beta release for testing the secure upload workflow and SFTP configuration
handling.

## Changes

- SFTP is now the default protocol for new repository and event upload
  configurations.
- New upload configurations offer SFTP, FTP, HTTP/WEBDAV and HTTPS/WEBDAV.
- FTPS has not been removed. Its implementation and compatibility with existing
  saved configurations remain available, but it is temporarily hidden from the
  selection list while TLS data-channel compatibility with FileZilla Server is
  being finalized.
- Corrected SFTP host/path normalization. Addresses such as
  `sftp:///enterprise.vpzbrig21.de/arma3sync/workshop` are normalized to the
  host `enterprise.vpzbrig21.de` with remote path
  `/arma3sync/workshop`.
- Custom SFTP ports such as `2022` continue to work unchanged.

## Compatibility

- Existing FTP, HTTP, HTTPS, WebDAV, SFTP and legacy `a3s.xml` configurations
  remain compatible.
- Existing FTPS configurations are retained and are not migrated or deleted.
- The standard package continues to include its Java runtime; the Compact
  package still requires a separately installed Java 25+ runtime.

## Verification

- `gradle test :updater:test` passed.
- SFTP normalization tests cover both the malformed legacy scheme and custom
  port-independent host/path parsing.

This is a beta build and should be tested before publication as a stable
release.
