# Arma3Sync 2026.3.1-Beta

Performance preview for repository operations. This beta is not a final
release; the central application version for this test build is `2026.3.1`.

## Improvements

- Large repository content checks can now verify files through a bounded pool of
  up to four protocol connections.
- SHA-1 calculation uses a bounded worker pool of up to eight workers while
  preserving the existing cache format.
- Progress notifications are reduced to actual percentage changes, making the
  interface more responsive during large operations.
- Repository size calculation uses a streaming file walk and avoids retaining
  the complete path list in memory.
- Local sync comparisons use hash-based name lookup for better scaling.
- Automatic checks are capped at four repositories in parallel to protect both
  client and server resources.
- Repository content-check parallelism has its own setting (default `4`) and no
  longer reuses the configured download connection count. Its safety limit is
  four concurrent connections.
- FTP repository uploads now reuse one authenticated upload session across
  remote existence checks, file transfers, repository metadata and cleanup
  operations. The FTP base directory is restored before each operation so
  nested paths remain correct.
- Repository upload configuration now offers FTPS and SFTP in addition to
  plain FTP and WebDAV. FTPS uses explicit TLS negotiation before login and
  private protection for data channels, fixing servers that require `AUTH`
  before `USER` (including current FileZilla Server configurations).
- SFTP uses Apache MINA SSHD, the standard SSH port default `22`, password
  authentication, safe remote-path handling, and one persistent SSH/SFTP
  session for a complete repository upload.

## Compatibility and safety

- Existing repository formats, `a3s.xml`, HTTP/HTTPS/FTP/FTPS/SFTP/WebDAV handling and
  serialized cache data remain unchanged.
- Fresh sync metadata is still loaded during explicit repository checks, so
  removed and renamed remote files are not hidden by stale local metadata.
- Missing files and connection failures retain their existing meanings. A real
  connection failure cancels the other parallel check workers.
- `.zsync` generation was intentionally not parallelized yet and remains a
  separate validation task.
- Parallel FTP uploads remain disabled until independent sessions, directory
  creation, cancellation and progress aggregation have dedicated validation.

## Validation status

- Main application and updater test suites pass on the performance branch.
- Protocol-specific large-repository, cancellation and failure-injection tests
  remain required before declaring the beta release-ready.
