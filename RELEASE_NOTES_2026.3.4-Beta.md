# Arma3Sync 2026.3.4-Beta

Patch beta for the SFTP upload workflow.

## Fixed

- SFTP connections now use explicit connection, authentication,
  channel-opening and idle timeouts. A failed or unreachable SFTP endpoint no
  longer leaves the upload waiting indefinitely.
- Pressing `Stop` now closes the active SFTP session asynchronously so the user
  interface remains responsive and the operation can be canceled during
  connection setup or remote-file checks.
- SFTP cancellation state is safely shared between the upload worker and the
  user interface.
- A connection status is shown before the remote-file check starts. The size,
  uploaded amount, speed and remaining time fields are populated when the
  actual file-transfer phase begins.

## Compatibility

- SFTP remains the default upload protocol and custom ports such as `2022`
  remain supported.
- FTP, HTTP/WEBDAV and HTTPS/WEBDAV remain available for new configurations.
- FTPS remains implemented and existing FTPS configurations remain retained,
  but FTPS is still hidden from new protocol selections.
- Existing repository formats and the standard and Compact package models are
  unchanged.

## Verification

- `gradle clean test :updater:test` passed.
- This is a beta build and should be tested against both reachable and
  unreachable SFTP endpoints, including the Stop action during connection and
  remote-file checking.
