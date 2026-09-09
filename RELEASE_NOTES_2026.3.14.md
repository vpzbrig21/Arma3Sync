# Arma3Sync 2026.3.14

This release consolidates the repository, transfer and packaging changes made
since 2026.2.6.

## Highlights

### Repository upload performance

- FTP and SFTP repository uploads can use between 1 and 10 parallel file
  transfers. The default is 4 and the setting is stored per repository.
- Upload parallelism is independent from repository-check and download
  connections.
- FTP upload sessions reuse their connections, directory listings and remote
  file checks while an operation is running.
- Repository directories are prepared before file transfers. Synchronization
  metadata and deletion of obsolete files happen only after all file uploads
  complete.
- Aggregate upload progress, speed and remaining time are reported across all
  active upload sessions.

### SFTP and transfer reliability

- SFTP repository connections support custom SSH ports, password
  authentication and remote paths.
- The SFTP connection workflow includes explicit connection, authentication,
  channel and cancellation handling.
- SFTP transfers use bounded network buffering for compatibility with servers
  using smaller SSH channel windows.
- FTP/FTPS, HTTP/WEBDAV and HTTPS/WEBDAV repository compatibility is retained.

### Diagnostics and cancellation

- The optional `-debug` start parameter writes a rotating diagnostic log for
  repository checks, uploads, connection setup, progress and errors.
- Passwords are never written to the diagnostic log.
- Stop requests now cancel active transfer sessions and pending SFTP
  connection attempts without leaving the user interface blocked.
- Successful completion is no longer reported as a cancellation.

### Runtime and packaging

- The Standard Windows package includes a reduced Java 25 runtime and does not
  require Java to be installed separately.
- The Compact package remains available for users who already provide Java 25
  or newer.
- Fixed the Standard NSIS installer so the bundled runtime is installed under
  `runtime\\`. Previously, its contents were placed directly in the
  installation root, while the launcher correctly searched in
  `runtime\\bin\\java.exe`.
- The installer and ZIP package now use the same launcher, root application
  JAR and runtime layout.

### Compatibility

- Existing repository configurations and the legacy `a3s.xml` format remain
  compatible.
- Older repository configurations automatically use the default of 4 parallel
  upload connections.
- Existing profiles, repositories and user configuration are preserved during
  updates and uninstall operations.

## Validation

- `gradle clean test` successful.
- Standard and Compact packages built successfully.
- Standard ZIP verified with `runtime\\bin\\java.exe` and
  `runtime\\bin\\javaw.exe`.
- Standard installer tested in an isolated installation and verified to place
  the runtime below `runtime\\`.
- FTP upload, SFTP upload, repository update and client download tested.

## Release files

- `Arma3Sync-2026.3.14.zip`
- `Arma3Sync-2026.3.14-setup.exe`
- Optional Compact artifacts use the `-compact` suffix.
