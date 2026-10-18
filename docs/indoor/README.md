# Indoor viewer and developer marker editor

This feature is implemented in shared Compose UI on Android and iOS. Sierra Hall
has two **fictional demonstration layouts**, A and B. They assert no actual floor
count, real room numbers, building geometry, accessibility, or evacuation paths.
No verified real floor plans are bundled. Gateway and Broome have no indoor catalog
entries yet. Outdoor geographic coordinates are not used for indoor markers.

## Student viewer (Stage 1)

Open Directions, select Sierra Hall, scroll its detail card if needed, and tap
**View Floor Plans**. The full-screen viewer has Back to map, layout selection,
pinch zoom, drag-to-pan, Zoom +/−, Reset View, and a text-accessible Markers list.
Tap a marker or its list item to see its name and type. The demonstration banner
stays visible, including while zoomed. A/B are not real floor numbers.

The viewer is a modal above the retained outdoor map composition. Back preserves
the outdoor camera, search, details, starting point, and destination. Opening the
viewer cancels a pending one-shot GPS request. Existing device snapshots still
expire normally after 30 seconds; floor viewing does not extend their lifetime,
start location tracking, or provide indoor positioning.

Stage 1 was compiled successfully for Android and both iOS targets before Stage 2
was added. Existing login, five-tab navigation, map style, and coastal theme remain
in place. No indoor route calculation, network service, paid SDK, storage permission,
or location permission was added.

## Enable the editor (Stage 2)

Normal builds are read-only. The editor is **off by default**, even for debug.
There is no student settings switch or hidden gesture. Android release forces
both `DEBUG` and `INDOOR_EDITOR_REQUESTED` to false, regardless of the property.
The shared action layer also blocks mutation/export without developer access.

### Android

Build and install an explicitly enabled debug APK:

```sh
./gradlew :androidApp:assembleDebug -PenableIndoorEditor=true
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

Or use `./gradlew :androidApp:installDebug -PenableIndoorEditor=true`, then open
the installed app. A later Android Studio Run without the property rebuilds the
normal, read-only variant.

Alternatively, launch Android Studio with that Gradle project property configured
for your local development session. Avoid checking a default-true property into
`gradle.properties`. Rebuild with `-PenableIndoorEditor=false` (or without the
property) to return to the normal configuration. Do not distribute enabled debug
builds to students.

### iOS

For a local **Debug** build only, add `INDOOR_EDITOR` to Active Compilation
Conditions in Xcode; preserve the existing `DEBUG` condition. Leave Release
conditions unchanged. The Swift entry point passes both conditions into the
shared `IndoorEditorPolicy`, which requires both to enable developer tools.

A simulator build from the command line can use:

```sh
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath /private/tmp/cipod-indoor-developer \
  SWIFT_ACTIVE_COMPILATION_CONDITIONS='DEBUG INDOOR_EDITOR' \
  CODE_SIGNING_ALLOWED=NO build
```

Remove `INDOOR_EDITOR` to disable it. A release build with only `INDOOR_EDITOR`
and no `DEBUG` remains read-only. Keep enabled builds restricted to development.

## Edit markers

1. Open Sierra's viewer in the enabled build. **DEVELOPER TOOLS** should be visible;
   editing still starts in read-only mode.
2. Tap **Edit markers**. Choose Add Classroom, Restroom, Entrance, Stair, or Elevator
   (the type row scrolls horizontally).
3. Tap within the actual plan rectangle to place one marker. Letterboxed margins
   do not place markers. Placement disarms after that tap.
4. Drag any marker with one finger to move it. Drag blank space to pan; two fingers
   zoom/pan instead of moving a marker. Zoom is bounded from fit view to 6×.
5. Select a marker by tapping it or using the Markers list. Change its name/type,
   then tap **Apply name and type**. Names must be nonblank and at most 80 characters.
   The developer control area scrolls vertically to reveal the whole form.
6. Tap **Delete marker** to remove the selected marker. Read-only preview disables
   placement, drag editing, name/type changes, and deletion while retaining drafts.
7. Drafts survive switching layouts, Back, tab changes, and configuration changes
   through an in-memory ViewModel. They are never serialized or autosaved. Closing/restarting the app or
   rebuilding loses them. Export anything you need to retain before doing so.

The marker anchor is its center in full-image coordinates: x=0 at left, x=1 at
right, y=0 at top, y=1 at bottom. The same transform draws plans and markers and
inverts pointer input. Zoom, pan, aspect-ratio fitting, letterboxing, and viewport
resizing are included. Dragging is clamped to [0,1]. Marker icons stay readable in
screen pixels while their anchors stay attached to the plan.

## Export and save JSON

1. In an enabled developer build, tap **Export JSON**. The dialog shows the complete
   document and the expected filename, for example `sierra-demo-a.json`.
2. Tap **Copy JSON**. This writes only marker JSON to the native clipboard. It does
   not write a project file or automatically save the draft.
3. With emulator clipboard sharing enabled, paste the JSON into your IDE. On iOS,
   use your simulator/host clipboard workflow. The read-only JSON field also allows
   selection/copying.
4. Save it to:

   `shared/src/commonMain/composeResources/files/indoor/<floorPlanId>.json`

5. Review the diff, then rebuild/relaunch. The loader validates schema version,
   building ID, plan ID/revision, demonstration status, unique marker IDs, names,
   types, and finite normalized coordinates. A failure displays a load-error state;
   it does not substitute fake data or silently discard an existing draft.

If emulator clipboard sharing is unavailable, the visible JSON can be extracted
from Android's UI hierarchy while the Export dialog is open:

```sh
adb shell uiautomator dump /sdcard/indoor-export.xml
adb pull /sdcard/indoor-export.xml /private/tmp/indoor-export.xml
python3 - <<'PY' > /private/tmp/indoor-markers.json
import json
import xml.etree.ElementTree as ET
for node in ET.parse('/private/tmp/indoor-export.xml').iter('node'):
    value = node.get('text', '')
    if value.startswith('{') and 'schemaVersion' in value:
        print(json.dumps(json.loads(value), indent=2))
        break
else:
    raise SystemExit('Open the Export JSON dialog first')
PY
```

Inspect that file before copying it into the matching project resource. No device
GPS coordinates are part of this JSON. The schema looks like:

```json
{
  "schemaVersion": 1,
  "buildingId": "sierra-hall",
  "floorPlanId": "sierra-demo-a",
  "planRevision": "demo-1",
  "demonstration": true,
  "markers": [
    {"id":"example","name":"Demo classroom A","type":"classroom","x":0.2,"y":0.3}
  ]
}
```

The JSON is marker data only; changing its demonstration flag cannot verify a plan.
Verification and the underlying image come from the reviewed catalog entry.

## Add an approved real floor-plan image

1. Obtain an image from an authoritative source with approval to bundle it. Verify
   the building, actual floor label, orientation, and revision. Do not derive a
   real plan or real floor count from the demonstrations.
2. Add a PNG/JPEG to `shared/src/commonMain/composeResources/drawable/`, for example
   `sierra_approved_plan.png`. Preserve the image's complete bounds and orientation.
3. Add a `FloorPlan` entry to `FloorPlanCatalog.kt` using:
   - a stable new plan ID and the existing building ID;
   - the floor label exactly as verified, a revision ID, and width/height aspect ratio;
   - `FloorPlanContent.ApprovedImage(Res.drawable.sierra_approved_plan)`;
   - the authoritative `sourceUrl` and actual `reviewedOn` date.
4. Add the corresponding `<id>.json` under `files/indoor/`, with matching metadata,
   `demonstration: false`, and initially `markers: []`. Add marker names/positions
   only after verifying them against that image. Keep demo and real plan IDs separate.
5. Rebuild, inspect orientation/alignment at multiple zoom levels, then edit/export
   verified marker information in the developer build.
6. If the image is cropped, rotated, or its bounds change, use a new revision and
   review all marker anchors. JSON for the old revision is deliberately rejected.

The same catalog approach supports `gateway-hall` and `broome-library`. Adding an
entry enables the View Floor Plans action for that building; the action does not
require outdoor coordinates. No fabricated floor choices are added in advance.

## Files and architecture

- `indoor/FloorPlanModels.kt`: demo/approved-image content, plan metadata, normalized points.
- `FloorPlanCatalog.kt`: available plans by building, currently fictional Sierra A/B only.
- `PlanTransform.kt`: projection, inverse projection, bounded zoom/pan, drag, resize.
- `RoomMarker.kt`: marker types/data and explicit debug/editor-request policy.
- `FloorPlanJson.kt`: encoding/decoding and document/marker validation.
- `FloorPlanRepository.kt`: bundled JSON loading through Compose resources.
- `FloorPlanEditorState.kt`: memory-only drafts, editing actions, and capability checks.
- `FloorPlanEditorViewModel.kt`: retains only floor drafts across configuration changes;
  no SavedStateHandle or disk storage, and no GPS data.
- `FloorPlanExport.kt` plus Android/iOS adapters: native clipboard export.
- `ui/indoor/IndoorFloorViewer.kt`: modal viewer, selection, loading/errors, dialogs.
- `FloorPlanCanvas.kt`: plan/marker drawing and combined pan/pinch/tap/drag input.
- `FloorPlanEditorControls.kt`: developer-only placement and property controls.
- `ui/map/BuildingDetailsCard.kt` / `CampusMapScreen.kt`: action and retained-map presentation.
- `App.kt`, `navigationBar/NavigationBar.kt`, platform entry points, Android build:
  default-off access injection and draft ownership across tabs.
- `shared/build.gradle.kts` / version catalog: direct JSON runtime dependency pinned
  to the already cached/transitively used 1.11.0; no serialization compiler plugin.
- Three common test files: transform, editing/gating, and JSON validation.

## Verification

```sh
./gradlew :androidApp:assembleDebug :shared:testAndroidHostTest \
  :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64 --offline
./gradlew :shared:iosSimulatorArm64Test --offline
./gradlew :androidApp:assembleRelease -PenableIndoorEditor=true
```

Stage 1 built before the editor was added. Android host tests: 67 passed; iOS
simulator tests: 61 passed, including all existing location/building tests. Android
release was checked with the editor requested and generated both debug/editor
flags as false. The first offline release attempt needed a missing standard lint
dependency; the online release build subsequently passed with vital lint enabled.
Full Xcode simulator app linking passed for both the default configuration and
the explicitly enabled developer configuration.

Manual Android checks cover normal viewer entry/Back, fictional labels, bundled
marker loading, layout selection, zoom/reset, and absence of editor/export controls.
The enabled build passed tap placement, dragging at zoom 1.8225, name/type editing,
deletion, native clipboard export, and normalized JSON round-trip checks. Unit
tests also verify retaining drafts when plans are revisited. Real-device touch behavior and approved-image
alignment should still be reviewed on Android and iOS before student distribution.
