# NuvioMobile fork (Ethereal / Requiem) — CLAUDE.md

whiteak's fork of NuvioMedia/NuvioMobile: Kotlin Multiplatform + Compose Multiplatform (Android, iOS, Windows desktop).
It is the **Ethereal** app (with Ethereal's changes built in) and the **Requiem** edition. It's also the base that
whiteak/Prism and whiteak/Supernatural build on, so changes here reach more than one app.

**Jason builds only through GitHub Actions. Don't run Gradle or set up a local build.**

## Branches: check which one you're on first

| Branch                         | Used by                                                                     |
|--------------------------------|-----------------------------------------------------------------------------|
| `cmp-rewrite` (default)        | Ethereal + Requiem releases; Supernatural phone build                       |
| `claude/project-thread-lph2qt` | Windows app (`desktop.yml`); **Prism phone + PC**; Requiem phone/iPhone on Prism; Supernatural PC |
| upstream NuvioMedia/NuvioMobile `cmp-rewrite` | merged into `cmp-rewrite` weekly by `release.yml`            |

- `lph2qt` split from `cmp-rewrite` on 25 Oct 2026. On 2 Nov it had 8 commits of its own (desktop target, libVLC
  player) and was 11 behind. **Nothing merges `cmp-rewrite` into it automatically.** Ask Jason before merging either way.
- Other `claude/project-thread-*` and `personal/baseline` branches: leftovers from old sessions. Ignore them: don't
  build from them, merge them or delete them.

## Layout (only the parts you're likely to touch)

```
composeApp/src/commonMain/kotlin/com/nuvio/app/   shared app code (features/<area>/...)
composeApp/src/androidMain/, androidFull/          Android-only code
composeApp/src/iosMain/                            iPhone code
composeApp/src/desktopMain/                        Windows app (lph2qt only)
composeApp/src/commonMain/composeResources/        strings.xml, drawables
androidApp/                                        Android entry point, manifest, app ID (build.gradle.kts)
iosApp/Configuration/Version.xcconfig              MARKETING_VERSION / CURRENT_PROJECT_VERSION (app version)
editions/requiem/   apply.sh                 Requiem on Ethereal (used by release.yml)
                    android-on-prism.sh      Requiem phone on top of Prism
                    ios-on-prism.sh, ios-old-sdk.sh   Requiem iPhone on top of Prism
                    mkoverlay.sh, masters/, overlay/ (GENERATED images)
editions/ethereal/  masters/, avatars/
MPVKit/ (submodule), vendor/, libass-android/      third-party; don't edit
```

Ethereal's own code is marked by `Edition*` names and `EDITION_*` constants (e.g. `EditionLoadingArt.kt`,
`EditionAvatars.kt`, `EDITION_DEFAULT_THEME`). Put new fork-specific code in `Edition*` files so upstream merges stay clean.

## Build & release (workflows on `cmp-rewrite`)

- `release.yml`: runs weekly (Sun 18:00 UTC), on dispatch, or **on any push to `cmp-rewrite`**.
  1. Merges upstream, keeping this fork's workflows and branding images. On other conflicts it opens an issue
     "Ethereal: upstream sync needs attention" and stops.
  2. Builds Ethereal, then runs `editions/requiem/apply.sh` and builds Requiem.
  3. Publishes `v<nuvio version>.<n>` with `Ethereal-Android-*.apk` + `Requiem-Android-*.apk`. It skips the release
     if that commit was already released.
- `requiem-android.yml` (manual, `prism_ref` input) builds Requiem phone **on Prism**: it runs Prism's
  `mobile/apply.sh`, then `android-on-prism.sh`. It rebuilds Ethereal from this branch as-is (no sync) and puts both
  on the same release, so the Ethereal app isn't offered Requiem's APK.
- `requiem-iphone.yml` (manual) does the same with Prism `mobile/` + `ios/` and `ios-on-prism.sh`. It attaches an
  unsigned Requiem IPA to the newest release. Prism's `ios/` is on Prism branch `claude/project-thread-rs7nwb`.
- `desktop.yml` (on `lph2qt`): compiles on pushes to `claude/**` and `desktop/**`. On dispatch it builds the installer.
- **A push to `cmp-rewrite` publishes a release.** Add `[skip ci]` to any commit that shouldn't (docs, this file).
- Signing alias `ethereal`. Secrets: `SIGNING_KEYSTORE`, `SIGNING_KEY_PASSWORD`, Trakt keys.
- App IDs: releases ship the full **debug** variant, so Ethereal installs as `com.nuviodebug.com`. The editions
  sed-replace that exact string (Requiem `com.requiem.app`, Prism `com.prism.app`, Supernatural `com.supernatural.app`).

## Rules

- **Downstream patch scripts anchor on exact text in this repo.** whiteak/Prism (`mobile/apply.sh`,
  `desktop/apply.sh`) and whiteak/Supernatural edit `App.kt`, `AppGate.kt`, `MainActivity.kt`, `HomeScreen.kt`,
  `AppUpdaterRepository.kt`, `TraktAuthRepository.kt`, `CatalogScreen.kt`, `PlayerScreen.kt`, `main.kt` and more.
  Before reformatting or restructuring those files, grep both repos' `apply.sh` for the lines you're changing and
  tell Jason which anchors need updating. Keep `"com.nuviodebug.com"` and `RELEASE_ASSET_PREFIX = "Ethereal-"`
  exactly as they are.
- `android-on-prism.sh` / `ios-on-prism.sh` depend on Prism's hooks (`PrismAndroid.install(this)`,
  `PrismIos.install();`). If Prism changes those, update these scripts too.
- Never hand-edit `editions/*/overlay/` images. Regenerate them with `editions/requiem/mkoverlay.sh <repo>`.
- Keep behaviour consistent with the TV fork (whiteak/NuvioTV). Say so when a phone feature can't match on TV.
- Don't touch upstream CI/bots. The fork's `.github/workflows` are kept as they are on every sync.
- LF line endings only (downstream patches match on them). GPLv3, keep the "built on Nuvio" credit.

## Working efficiently

- Don't explore `MPVKit/`, `vendor/`, `libass-android/`, `iosApp/` or the `assets/` images unless the task needs them.
- Find a feature with `ls composeApp/src/commonMain/kotlin/com/nuvio/app/features/` and go straight to its folder.
- To check that an edition script still applies, run it on a fresh `git clone --depth 1` (no Gradle needed).
