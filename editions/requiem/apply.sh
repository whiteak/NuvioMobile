#!/bin/bash
# Turns this checkout into the Requiem edition (run in CI before building it).
set -euo pipefail
cd "$(dirname "$0")/../.."
cp -R editions/requiem/overlay/. .
grep -rlZ 'name="app_name"' composeApp/src/androidMain androidApp/src --include='*.xml' | xargs -0 sed -i 's#<string name="app_name">[^<]*</string>#<string name="app_name">Requiem</string>#'
sed -i 's#[A-Za-z ]* · built on Nuvio#Requiem · built on Nuvio#' composeApp/src/commonMain/composeResources/values/strings.xml
sed -i 's#<string>Ethereal</string>#<string>Requiem</string>#' iosApp/iosApp/Info.plist
sed -i 's#<color name="app_icon_background">[^<]*</color>#<color name="app_icon_background">\#F2F2F4</color>#' composeApp/src/androidMain/res/values/colors.xml
sed -i 's#<color name="edition_splash_background">[^<]*</color>#<color name="edition_splash_background">\#0B0B0C</color>#' composeApp/src/androidMain/res/values/colors.xml
sed -i 's#"com.nuviodebug.com"#"com.requiem.app"#' androidApp/build.gradle.kts
sed -i "s#val EDITION_DEFAULT_THEME = AppTheme.VIOLET#val EDITION_DEFAULT_THEME = AppTheme.WHITE#" composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/AppTheme.kt
sed -i 's#RELEASE_ASSET_PREFIX = "Ethereal-"#RELEASE_ASSET_PREFIX = "Requiem-"#' composeApp/src/commonMain/kotlin/com/nuvio/app/features/updater/AppUpdaterRepository.kt
grep -q 'com.requiem.app' androidApp/build.gradle.kts
grep -q "EDITION_DEFAULT_THEME = AppTheme.WHITE" composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/AppTheme.kt
grep -q '"Requiem-"' composeApp/src/commonMain/kotlin/com/nuvio/app/features/updater/AppUpdaterRepository.kt
echo "Requiem edition applied."
