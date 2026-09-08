# Performance plan – Repository operations

This document tracks the performance work on the `performance/repository-operations`
branch. The current application version remains `2026.2.6` until the beta scope
and release version are confirmed.

## Goals

- Reduce the time required to check large repositories.
- Reduce repeated local file-system work while building a repository.
- Keep repository formats, legacy `a3s.xml`, profiles and existing protocol
  behavior compatible.
- Keep cancellation, error reporting and verification semantics intact.

## Completed in this branch

- Remote repository content checks use a bounded pool of up to four independent
  protocol connections. The configured client connection count is respected up
  to that safety limit.
- Missing-file reporting remains unchanged; real connection errors cancel the
  other check workers and are reported to the caller.
- SHA-1 work uses a bounded pool of up to eight workers. Results are collected
  concurrently and merged into the existing serialized cache by one writer.
- SHA-1 and remote-check progress notifications are emitted only when the
  visible percentage changes, reducing Swing event-queue pressure.
- Repository total-size calculation no longer materializes the complete file
  walk as a list.
- Large local sync comparisons use hash-based name lookup instead of repeated
  linear list searches.
- Automatic checks are capped at four repositories in parallel to avoid
  uncontrolled connection bursts against servers.

## Deliberately not changed

- No repository metadata format or protocol contract was changed.
- The sync tree is still refreshed during an explicit repository check; this is
  required to avoid stale metadata after removed or renamed files.
- The serialized `FileAttributes` structure was not changed, preserving legacy
  cache compatibility.
- `.zsync` generation remains single-threaded until its writer and metadata
  dependencies have been verified for safe parallel use.

## Remaining validation stages

1. Measure scan, hashing, metadata and remote-check phases on representative
   repositories with 1,000, 10,000 and 50,000 files.
2. Test bounded checks against HTTP, HTTPS, FTP and WebDAV servers with normal
   latency, high latency, missing files and connection failures.
3. Verify cancellation, repeated checks and concurrent repository operations.
4. Compare generated `sync`, `serverinfo`, `changelogs` and `autoconfig` data
   with a baseline build.
5. Decide whether additional `.zsync` parallelism is safe before considering it
   for the beta.

