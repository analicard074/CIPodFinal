# Campus map: Milestones 0–2

The Directions tab displays a real MapLibre Compose map centered on CSU Channel
Islands in Camarillo, California. Drag to pan, pinch to zoom, or use the + / −
buttons. MyCI and Settings retain their existing placeholder content.

## Setup

- MapLibre Compose and its Android OpenGL runtime are pinned to **0.20.0** in
  `gradle/libs.versions.toml`. Keep both at the same version.
- The shared UI lives in `shared/src/commonMain/kotlin/com/example/cipod_final/ui/map/`.
- `CampusMapConfig.kt` sets the initial camera to latitude 34.1622, longitude
  -119.0434, zoom 15.5. This is a campus overview, not a routing destination.
- The background style is `https://tiles.openfreemap.org/styles/positron`.
  No API key, billing account, or paid API is used. An internet connection is
  required to load background maps; this milestone does not provide offline maps.
- Android declares internet and foreground location permissions and requests
  location access once after initial login. iOS requests When In Use location access
  after initial login.
  Denying access leaves the app usable; current-location display is not implemented.
  Network-state permission also comes from a library dependency.
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

1. Stay on `feature/building-markers` and sync the Gradle project. The first sync needs
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

## Milestone 2: local building directory

All feature code is shared Kotlin in `commonMain`; no platform-specific map,
search, or selection APIs were introduced. Positron and the coastal palette remain
in place. The app loads its directory locally and does not call university or
geocoding services at runtime. Background map tiles still require internet.

- `buildings/CampusBuilding.kt`: building model and nullable verified coordinates.
- `buildings/CampusBuildings.kt`: local directory and trimmed, case-insensitive
  name substring search. Empty searches return all buildings.
- `buildings/CampusNavigationState.kt`: immutable browsing and destination logic.
- `ui/map/BuildingMarkers.kt`: MapLibre GeoJSON circle markers and clickable labels.
  Blue identifies buildings, seafoam the preview, and orange the destination.
- `ui/map/BuildingSearchBar.kt`: coastal search field and scrollable result list.
- `ui/map/BuildingDetailsCard.kt`: building information and destination action.
- `ui/map/CampusMapScreen.kt`: screen composition and camera centering on selection.
- `HomePage.kt`: saves query, preview, and destination IDs across tab changes and
  supported saved-state restoration. A fresh app session starts without a destination.
- `commonTest/.../CampusBuildingsTest.kt`: search and destination behavior tests.

Selecting a result or marker previews its details and centers on its verified
building point at zoom 17. Previewing another building does not replace the
chosen destination. Set/Replace destination explicitly changes that choice;
Clear destination removes it while preserving the details being viewed. The
persistent destination bar can reopen and recenter that building. Search filters
markers as well as results; filtering does not clear a destination. Closing details
also preserves the destination. Results provide a text alternative to map taps.

### Coordinate evidence (reviewed 2026-10-16)

Coordinates below are copied from the **university's published map points**, not
estimated from pixels or taken from the initial campus overview. They represent
buildings, not surveyed entrances, accessible approaches, or routing endpoints.

The [official CSUCI map](https://maps.csuci.edu/) embeds
[Concept3D map 502](https://map.concept3d.com/?id=502). Its public search records
supply `lat` and `lng`; record IDs below make the evidence reproducible. Cross-checks
used the original OpenStreetMap named building geometries via each way's `/full`
API endpoint. Broome’s point lies inside its named OSM polygon. Sierra’s point is within
the named building’s overall bounds and approximately 1.8 meters outside its
outline, consistent with a building-level map point rather than an entrance
survey. Both correspond to the independently identified building locations. OSM is independent of
the university; Mapcarta mirrors OSM and is not
counted as another independent dataset.

| Building       | Published university point (latitude, longitude)    | University location / marker IDs | Independent geographic reference                                                                  | App status                            |
|----------------|-----------------------------------------------------|----------------------------------|---------------------------------------------------------------------------------------------------|---------------------------------------|
| Sierra Hall    | 34.162197, -119.044662                              | 189849 / 133412                  | [OSM way 713268925](https://www.openstreetmap.org/way/713268925), named Sierra Hall               | Verified marker and destination       |
| Broome Library | 34.162807, -119.041100                              | 189826 / 133389                  | [OSM way 461618288](https://www.openstreetmap.org/way/461618288), named John Spoor Broome Library | Verified marker and destination       |
| Gateway Hall   | Withheld from app pending independent corroboration | 1067463 / 803263                 | No named Gateway Hall geographic feature found in the checked OSM area or independent searches    | Search/details only; coordinates null |

University building identity and descriptions are also supported by the
[CSUCI self-guided tour map](https://www.csuci.edu/visit-campus/tours/documents/2019-self-guided-tour-map.pdf)
for Sierra and Broome, and the
[Gateway Hall opening announcement](https://www.csuci.edu/news/releases/moving-to-gateway-hall-20250812.htm).
The 2019 guide predates Gateway's opening and is not evidence for its current location.

Gateway's official map does publish a point. It is deliberately withheld from the
app because the requested independent verification could not be completed.
Gateway displays “Location awaiting verification” and cannot become a destination.
Do not substitute the campus center or another building. To enable it later, add
an independently corroborated geographic source and verified coordinates to its
local record, update this table, and update the unverified-building tests.

### Milestone 2 emulator checks

1. Sync Gradle on `feature/building-markers`; run `androidApp` on an emulator with
   internet and automatic/hardware graphics. Log in with `student@csuci.edu` using
   the existing demo login (no real credentials required).
2. In Directions, verify the coastal search bar, Positron tiles, Sierra/Broome
   markers and labels, and visible attribution/zoom controls.
3. Search `  SiErRa  `: only Sierra remains in results and app markers. Search an
   unknown name to see the empty state, then use Clear to restore all results.
4. Tap Sierra's result. Confirm its details and centered map, then Set destination.
   Close details and confirm the destination bar remains.
5. Clear search and tap Broome's marker or label. Confirm its details and camera
   centering while Sierra remains the destination. Tap Replace destination and
   confirm Broome becomes orange and appears in the destination bar.
6. Tap Clear destination. Confirm the bar disappears and Broome details remain.
   Tap the same building again after panning away to confirm recentering.
7. Search Gateway: details explain the unverified location, Set destination is
   disabled, and no Gateway marker appears. An existing destination is preserved.
8. Choose a verified destination, switch to MyCI or Settings, then return to
   Directions. Confirm query/destination survive. Try screen rotation and reopen
   details if desired. Check keyboard dismissal when selecting search results.
9. On initial install, the system location prompt appears after login. Allow or deny
   access and confirm that the app remains usable. On Android, restart after denial
   and confirm the prompt does not repeat. Pan, zoom, and open attribution.
   Walking routes, walking times, and floor plans are not implemented.

### Milestone 2 build verification

```sh
./gradlew :androidApp:assembleDebug :shared:testAndroidHostTest \
  :shared:compileKotlinIosSimulatorArm64 :shared:compileKotlinIosArm64 \
  :shared:compileTestKotlinIosSimulatorArm64 --offline
```

The Android host suite includes 11 building search/selection tests. Native test
sources are also compiled for the iOS simulator. All commands above passed after the final marker
changes (11/11 new tests,
zero failures). The full iOS simulator app also compiled and linked successfully:

```sh
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath /private/tmp/cipod-building-derived CODE_SIGNING_ALLOWED=NO build
```

On the running Android emulator, verified case-insensitive search filtering,
Positron rendering, building markers and
labels, result selection, camera centering, marker taps opening details, setting a
destination, preserving it when closing details or previewing another building,
replacing it, and clearing it while preserving building details. The label font explicitly uses
Positron's supported Noto Sans
Regular stack. iOS runtime interaction has not been exercised; native source
compilation and the simulator app link passed. Existing Xcode warnings described
above still apply.

## Scope

Milestone 2 adds building discovery and destination selection. Walking routes,
walking-time estimates, indoor floor plans, and current location remain outside
this implementation. No commit or push is needed to test these local changes.
