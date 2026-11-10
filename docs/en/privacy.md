# Privacy & Security

Agora is a BYOK client. It does not operate a relay for chat completions: requests go from your device to the provider, model server, or tool endpoint you configure.

## Data stored on the device

Conversations, message trees, tasks, loops, memories, prompts, attachments, and tool media are stored in Agora-managed databases/files. Ordinary settings use Android preferences. Secret settings normally use an AES-256-GCM envelope backed by the Android Keystore; legacy plaintext remains readable and encryption failure deliberately stores plaintext rather than losing the value.

Deleting app data or uninstalling the app removes app-managed data unless Android backup or an exported archive preserves it.

## When data leaves the device

App network requests are limited to the model provider(s) and MCP servers you configure:

- chat, title, transcription, image-generation, embedding, and provider-native search requests go to their selected model provider or configured endpoint
- MCP calls go only to enabled MCP servers

Agora does not capture crash reports, check for updates, submit ratings, run standalone web search, fetch remote preview assets, or contact Agora-owned analytics/services. Provider and MCP operators have their own privacy and retention policies.

## Backups and secrets

A `.agora` export is a ZIP archive. If you explicitly include API keys/secrets, those values are unencrypted inside the archive. Keep that file private and delete copies you no longer need.

## Proxy and transport

The configured network proxy covers provider and MCP HTTP traffic, not on-device inference or sandbox process networking. TLS and endpoint trust still matter.

Review the endpoint URLs, server certificates, provider terms, and tool permissions before sending sensitive content.
