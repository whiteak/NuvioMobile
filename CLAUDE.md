# NuvioMobile fork (Ethereal) — CLAUDE.md

whiteak's fork of NuvioMedia/NuvioMobile: Kotlin Multiplatform + Compose Multiplatform (Android, iOS, Windows desktop).
The default branch is the **Ethereal** app. Other apps (Prism, Requiem, Supernatural) build on their **own fixed
base branches** in this repo, not on Ethereal's branch.

**Jason's rule: every app is its own.** Change only the app he names, on that app's branch. Never carry a change
into another app's branch or base unless he asks for that.
**Jason builds only through GitHub Actions. Don't run Gradle or set up a local build.**

## Branches: check which one you're on first

| Branch                         | App                                                                       |
|--------------------------------|---------------------------------------------------------------------------|
| `cmp-rewrite` (default)        | **Ethereal**                                                              |
| `base/prism`                   | Prism phone + PC (built by whiteak/Prism)                                 |
| `base/requiem`                 | Requiem phone + iPhone (built by whiteak/Prism "Build Requiem")           |
| `base/supernatural-phone`, `base/supernatural-pc` | Supernatural phone, PC (built by whiteak/Supernatural) |
| `claude/project-thread-lph2qt` | Windows-target work branch (`desktop.yml`). No app builds from it now     |
| upstream NuvioMedia/NuvioMobile `cmp-rewrite` | merged into `cmp-rewrite` only when Jason runs the Ethereal workflow |

- The `base/*` branches were pinned on 2 Nov 2026 to what each app was last built from. They move only when Jason
  asks, for that app only: merge or cherry-pick into that one base, then check the app's scripts still apply.
- Other `claude/project-thread-*` and `personal/baseline` branches are leftovers from old sessions. Ignore them:
  don't build from them, merge them or delete them.

## Layout (only the parts you're likely to touch)

```
composeApp/src/commonMain/kotlin/com/nuvio/app/   shared app code (features/<area>/...)
composeApp/src/androidMain/, androidFull/          Android-only code
composeApp/src/iosMain/                            iPhone code
composeApp/src/desktopMain/                        Windows app (lph2qt and the bases made from it)
composeApp/src/commonMain/composeResources/        strings.xml, drawables
androidApp/                                        Android entry point, manifest, app ID (build.gradle.kts)
iosApp/Configuration/Version.xcconfig              MARKETING_VERSION / CURRENT_PROJECT_VERSION (app version)
editions/ethereal/  masters/, avatars/
MPVKit/ (submodule), vendor/, libass-android/      third-party; don't edit
```

Ethereal's own code is marked by `Edition*` names and `EDITION_*` constants (e.g. `EditionLoadingArt.kt`,
`EditionAvatars.kt`, `EDITION_DEFAULT_THEME`). Put new Ethereal code in `Edition*` files so upstream merges stay clean.
Requiem's files are no longer here. They live on whiteak/Prism's `requiem` branch.

## Build & release (Ethereal)

- `release.yml` ("Sync and release Ethereal") is **manual only**. There's no schedule and no push trigger.
  1. If `sync` is ticked, it merges upstream Nuvio, keeping this fork's workflows and Ethereal's branding images. On
     other conflicts it opens an issue "Ethereal: upstream sync needs attention" and stops.
  2. It builds Ethereal and publishes `v<nuvio version>.<n>` with `Ethereal-Android-*.apk`. It skips the release if
     that commit was already released.
- `desktop.yml` (on `lph2qt`): compiles on pushes to `claude/**` and `desktop/**`. On dispatch it builds the installer.
- Pushes no longer publish anything, but still add `[skip ci]` to doc-only commits.
- Signing alias `ethereal`. Secrets: `SIGNING_KEYSTORE`, `SIGNING_KEY_PASSWORD`, Trakt keys.
- App IDs: releases ship the full **debug** variant, so Ethereal installs as `com.nuviodebug.com`. The other apps
  sed-replace that exact string (Prism `com.prism.app`, Requiem `com.requiem.app`, Supernatural `com.supernatural.app`).
- Releases tagged `requiem-moved-*` are the one-time update that moved Requiem phones to whiteak/Requiem. Their tags
  are deliberately not version numbers, so Ethereal ignores them. Leave them.

## Rules

- Work on the branch of the app Jason names. Ethereal changes go on `cmp-rewrite` and reach no other app.
  A Prism/Requiem/Supernatural change that needs fork code goes on that app's `base/*` branch only.
- The other apps' patch scripts (Prism `mobile/` + `desktop/apply.sh`, Requiem's `requiem/mobile/*-on-prism.sh`,
  Supernatural `mobile/` + `desktop/apply.sh`) anchor on exact text in their base. When you move a base forward
  at Jason's request, re-run that app's scripts on a fresh clone of the base and fix the anchors in that app's repo.
- Keep `"com.nuviodebug.com"` and `RELEASE_ASSET_PREFIX = "Ethereal-"` exactly as they are.
- Keep Ethereal's phone behaviour consistent with Ethereal TV (whiteak/NuvioTV `dev`). Say so when it can't match.
- Don't touch upstream CI/bots. The fork's `.github/workflows` are kept as they are on every sync.
- LF line endings only. GPLv3, keep the "built on Nuvio" credit.

## Working efficiently

- Don't explore `MPVKit/`, `vendor/`, `libass-android/`, `iosApp/` or the `assets/` images unless the task needs them.
- Find a feature with `ls composeApp/src/commonMain/kotlin/com/nuvio/app/features/` and go straight to its folder.
