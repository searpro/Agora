# Privacy Policy

**Last updated: August 10, 2026**

Agora is a BYOK Android client. It does not operate a relay for chat completions: model requests go from your device to the provider or endpoint you configure.

## Local data

Conversations, message trees, tasks, loops, memories, prompts, attachments, tool media, settings, and imported models are stored in app-managed databases/files on the device. Secret settings normally use an AES-256-GCM envelope backed by the Android Keystore. Legacy values are accepted as plaintext, and encryption failure deliberately falls back to plaintext rather than losing the value, so device storage protection remains important.

Clearing app data or uninstalling removes app-managed data unless Android backup or a user-created export preserves it.

## Network destinations

Network requests made by the app are limited to the configured model provider(s) and enabled MCP servers:

- chat, title, transcription, image-generation, embedding, and provider-native search requests go to their selected model provider or configured endpoint;
- MCP calls go only to enabled MCP servers.

Agora does not capture crash reports, check for updates, submit ratings, run standalone web search, fetch remote preview assets, or contact Agora-owned analytics/services. Provider and MCP operators have their own privacy and retention policies.

## Backups and exports

A `.agora` export is a ZIP archive. If you explicitly include API keys or other secrets, those values are unencrypted inside the archive. Protect and delete exported copies as appropriate.

## Transport and proxy

The configured proxy applies to provider and MCP HTTP traffic, not local inference or processes inside the Alpine sandbox.

## Permissions

- **Internet**: configured model-provider and MCP connections.
- **Notifications / foreground service**: ongoing generation, automation, and user-visible completion behavior.
- **Files and media**: only when you select/import attachments, models, backups, fonts, or shared sandbox storage.
- **Exact alarms**: optional automation scheduling where supported and explicitly enabled.

## Children, changes, and contact

Agora is not directed to children under 13. This policy may be updated with the repository/application. Questions can be opened at [github.com/newo-ether/Agora](https://github.com/newo-ether/Agora).
