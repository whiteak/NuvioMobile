# Requiem edition

A black and silver Angelcore edition of Reel Room with its own name, artwork and app ID
(so it installs alongside Reel Room). The release workflow builds it after Reel Room by running
`apply.sh`, then publishes it as `Requiem-*.apk` on the same release.

- `overlay/`: resource files that replace Reel Room's, generated from `masters/` by `mkoverlay.sh <repo>`.
- `apply.sh`: copies the overlay and switches the app name, app ID and updater asset name.

The Home banner (`composeResources/drawable/edition_home_banner.webp`) is overlaid the same way; the main app ships the Ethereal banner at that path.
