# Campus map: Milestones 0 and 1

The Directions tab displays a real MapLibre Compose map centered on CSU Channel
Islands in Camarillo, California. Drag to pan, pinch to zoom, or use the + / −
buttons. MyCI and Settings retain their existing placeholder content.

## Setup

- MapLibre Compose and its Android OpenGL runtime are pinned to **0.20.0** in
  `gradle/libs.versions.toml`. Keep both at the same version.
- The shared UI lives in `shared/src/commonMain/kotlin/com/example/cipod_final/ui/map/`.
- `CampusMapConfig.kt` sets the initial camera to latitude 34.1622, longitude
  -119.0434, zoom 15.5. This is a campus overview, not a routing destination.
- The background style is `https://tiles.openfreemap.org/styles/liberty`.
  No API key, billing account, or paid API is used. An internet connection is
  required to load background maps; this milestone does not provide offline maps.
- Android declares internet permission. No location permissions are declared or
  requested. Network-state permission also comes from a library dependency.
- `iosApp/Configuration/Config.xcconfig` supplies the system linker flags required
  by MapLibre when Xcode links the static Shared framework.

Sources: [MapLibre setup](https://maplibre.org/maplibre-compose/getting-started/),
[OpenFreeMap mobile setup](https://openfreemap.org/quick_start/).

## Attribution

`CampusMapScreen` uses `MapOverlay.Full`. This includes the MapLibre logo,
source attribution, compass, scale, and zoom buttons. Attribution is initially
expanded and may collapse to an information button during map interaction.
Tap that button to reopen the source links. Do not remove or cover these controls.
The map occupies the space above HomePage's bottom navigation.

OpenFreeMap's source credits include OpenFreeMap, OpenMapTiles, and OpenStreetMap.
The source-provided links are displayed by MapLibre rather than replaced with
hardcoded credits. Background-map labels come from the provider; they are not a
verified campus building directory.

## Branding transfer

The eight reviewed branding files were copied from local `feature/app-rebranding`
at `a3d4eadfd084bceb9b5c3aa16bf5a17c127275c1`, including the branding documents
introduced by `1c5e8b4`. The original branch and its commits were not changed.
No merge, cherry-pick commit, new commit, or push was performed.

The shared theme and login screen match that source branch. Email validation,
password masking, and the login callback are preserved. Login remains the existing
demo flow: a supported university email passes without credential authentication.

## Test in Android Studio

1. Stay on `feature/campus-map` and sync the Gradle project. The first sync needs
   internet access to download MapLibre and its native runtime.
2. Select the `androidApp` run configuration and an emulator or connected device
   with internet access. Hardware/automatic graphics rendering is recommended.
3. Run the app. Submit an empty or unsupported email and confirm the existing
   validation error appears.
4. Enter a sample address such as `student@csuci.edu` or
   `student@myci.csuci.edu`, then tap Log In. No real password is needed by the
   existing demo implementation.
5. Confirm Directions shows campus streets and buildings. Initial map data may
   take a few seconds to load. IDE previews show a placeholder, not live tiles.
6. Drag the map, pinch, and use + / −. Open the attribution information button and
   check the source links remain available above the bottom bar.
7. Switch to MyCI and Settings, then back to Directions. The map is recreated when
   returning to Directions and starts at the campus overview.

If the map stays blank, check the device's internet connection and access to the
OpenFreeMap style URL, then reopen Directions. The public background-map service
is an external dependency. Do not add API keys or disable HTTPS to troubleshoot.

## Build verification

Executed successfully for this implementation:

```sh
./gradlew :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 :shared:compileKotlinIosArm64
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' -derivedDataPath /private/tmp/cipod-map-derived CODE_SIGNING_ALLOWED=NO build
```

The full simulator app linked successfully with Xcode using the installed iOS 26.0
SDK. This does not substitute for testing map rendering on an iOS simulator/device.
On the running Android emulator, verified empty-email rejection, sample-email
login, campus tile rendering, zoom-in controls, drag-to-pan, source attribution,
and switching to MyCI, Settings, and back to Directions.
To run iOS, open `iosApp/iosApp.xcodeproj`, choose scheme `iosApp`, select a simulator,
and run. A physical device also needs your Apple signing team.

Non-blocking warnings observed:

- Android packaged some native libraries without stripping debug symbols.
- Kotlin could not infer the Shared framework bundle ID.
- The existing iOS application identifier contains underscores, which Xcode warns
  are invalid. Choose a valid identifier before device distribution; identifiers
  were left unchanged in this map feature.
- A native ICU object targets iOS Simulator 18.5 while the project targets 18.2.
  Test on iOS 18.5 or newer for now; support for 18.2 needs a separate dependency /
  deployment-target review.
- Xcode skipped optional App Intents metadata because this app does not use it.

## Scope

This milestone does not add app-specific building markers, building search,
destination selection, routing, walking-time estimates, floor plans, or current
location. Those remain later milestones. The map library's transitive location
support is not enabled or called by the app.
