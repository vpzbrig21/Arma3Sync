# Arma3Sync 2026.4.25 Beta

## Added

- Added `-discord "Repository Name"` for sending the latest locally stored build changelog to the repository's configured Discord webhook.
- Builds without content changes now send an empty changelog for the newest build instead of re-sending an older change list.
- Added the `DISCORD` command to the interactive console and documented current repository creation and CLI workflows.
- Discord CLI sends now return a non-zero exit code when configuration or delivery fails.
- Console launchers can run CLI commands synchronously so automation receives their output and exit status.
