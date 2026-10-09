# The iOS player-app stack

> **Evidence, 2026-10-09.** Item 4 for the V1 grill ([INDEX](../INDEX.md)). A read-only research subagent (Opus) wrote §1–9. The coordinator added the **measured Core ML export** (§3a), ran on the owner's Mac in a throwaway worktree, and checked the library claims the hub research contradicted (§4).
> Nothing was built or installed, and nothing ran on an iPhone. **Xcode is not installed on this Mac** (only the Command Line Tools), so no iOS code could be compiled here.

## The short version
- **iPhone-only SwiftUI app, minimum iOS 18**:
  - `@Observable` models and value-based `NavigationStack`;
  - logic in a local Swift package, so it is testable without a simulator.
- **Camera:** AVFoundation plus Vision running the YOLO model exported to Core ML. VisionKit's scanner cannot run a custom model.
- **The export works on this Mac today:** 21.4 MB FP16 with embedded NMS, ~6 ms per frame on the M1. On 40 synthetic boards it read the same labels as PyTorch on 36.
- **Realtime:** REST for every command, plus one push stream per client carrying a full **per-viewer snapshot**. No maintained Swift STOMP client exists. WebSocket and SSE are both viable; the hub research prefers SSE (§4).
- **Distribution needs the paid Apple Developer Program (99 USD/year) and TestFlight internal testing.** A free Apple ID gives 3 devices and 7-day apps, which doesn't work for the group.
- **CI is free:** GitHub Actions macOS runners cost nothing for this public repo.

## Sources
All accessed 2026-10-09. "3rd-party" marks a secondary source with lower confidence.

**Apple documentation** (versions as Apple's page metadata states them)
| Topic | URL | Version / date |
|---|---|---|
| Core AI framework | https://developer.apple.com/documentation/coreai | iOS 27.0+ |
| Vision (Swift-only API) | https://developer.apple.com/documentation/vision | "Starting in iOS 18.0 … new Swift-only API" |
| `CoreMLRequest` | https://developer.apple.com/documentation/vision/coremlrequest | iOS 18.0+ |
| `VNCoreMLRequest` | https://developer.apple.com/documentation/vision/vncoremlrequest | iOS 11.0+, not deprecated |
| `RecognizedObjectObservation` | https://developer.apple.com/documentation/vision/recognizedobjectobservation | iOS 18.0+ |
| Recognizing Objects in Live Capture (sample) | https://developer.apple.com/documentation/vision/recognizing-objects-in-live-capture | iOS 12+ |
| `DataScannerViewController`, `RecognizedDataType` | https://developer.apple.com/documentation/visionkit/datascannerviewcontroller | iOS 16.0+ |
| AVCam sample | https://developer.apple.com/documentation/avfoundation/avcam-building-a-camera-app | iOS 27.0 / Xcode 27.0 |
| Observation; `Observations` | https://developer.apple.com/documentation/observation | iOS 17.0+; `Observations` iOS 26.0+ |
| `NavigationStack` | https://developer.apple.com/documentation/swiftui/navigationstack | iOS 16.0+ |
| `URLSessionWebSocketTask`; `URLSession.bytes(for:)` | developer.apple.com/documentation/foundation/urlsessionwebsockettask (and siblings) | iOS 13 / 15 |
| `NSAllowsLocalNetworking` | https://developer.apple.com/documentation/bundleresources/information-property-list/nsapptransportsecurity/nsallowslocalnetworking | iOS 10+ |
| Preventing Insecure Network Connections (ATS) | https://developer.apple.com/documentation/security/preventing-insecure-network-connections | undated |
| TN3179 Local network privacy | https://developer.apple.com/documentation/technotes/tn3179-understanding-local-network-privacy | revised 2026-10-06 |
| Restricting keychain item accessibility | https://developer.apple.com/documentation/security/restricting-keychain-item-accessibility | undated |
| Downloading and compiling a model on device | https://developer.apple.com/documentation/coreml/downloading-and-compiling-a-model-on-the-user-s-device | undated |
| Migrating from XCTest (Swift Testing) | https://developer.apple.com/documentation/testing/migratingfromxctest | toolchain 6.4 |
| Xcode system requirements | https://developer.apple.com/xcode/system-requirements/ | Xcode 27: macOS 26.6+, Swift 6.4, deploys iOS 15–27, debugs iOS 17+ |
| Xcode (Mac App Store) | https://apps.apple.com/us/app/xcode/id497799835 | 27.0, 3.1 GB download, macOS 26.6+, M1 or later |
| App Store usage | https://developer.apple.com/support/app-store/ | measured 2026-06-07 |
| Memberships | https://developer.apple.com/support/compare-memberships/ ; https://developer.apple.com/programs/whats-included/ | 99 USD/year |
| TestFlight (overview, internal, external) | https://developer.apple.com/help/app-store-connect/test-a-beta-version/testflight-overview | undated |
| Devices (Ad Hoc limits) | https://developer.apple.com/help/account/devices/devices-overview | undated |
| Xcode Cloud | https://developer.apple.com/xcode-cloud/ | undated |

**Swift, Ultralytics, Spring, GitHub**
- SE-0466, default actor isolation (implemented in Swift 6.2): https://github.com/swiftlang/swift-evolution/blob/main/proposals/0466-control-default-actor-isolation.md
- Ultralytics Core ML docs (via context7 `/websites/ultralytics`): https://docs.ultralytics.com/integrations/coreml/ ; YOLOv8 specs: https://docs.ultralytics.com/models/yolov8/
- Ultralytics 8.4.174 source, read locally: `ultralytics/engine/exporter.py`, `ultralytics/utils/export/coreml.py`
- Ultralytics iOS SDK `ultralytics/yolo-ios-app` v8.9.16 (2026-10-09, **AGPL-3.0**): `BasePredictor.swift`, `ObjectDetector.swift`
- Spring Framework reference (via context7): WebSocket server, STOMP token auth, `SseEmitter`
- GitHub Actions billing: https://docs.github.com/en/billing/concepts/product-billing/github-actions ; runners: https://docs.github.com/en/actions/reference/runners/github-hosted-runners ; images: `actions/runner-images` (`xcode-27-arm64`, image 20260928)

**Third-party (lower confidence):**
- iOS 27 device list (iPhone 11 / SE 2 and later), from iclarified.com and cultofmac;
- WWDC26 camera "deferred start", and On-Demand Resources deprecated in the iOS 27 notes, from blakecrosley.com;
- Let's Encrypt moving to 64-day (2027) and 45-day (2028) certificates, from isrg.org and linuxiac;
- FRITZ!Box DNS-rebind protection, from en.fritz.com.

**Swift packages** (GitHub API, 2026-10-09; the coordinator re-checked the three that drive the realtime choice)
| Package | Latest release | Last push | Manifest / licence | Verdict |
|---|---|---|---|---|
| Romixery/SwiftStomp | 1.2.1, 2024-05-20 | 2024-05-21 | swift-tools 5.3; MIT | stale; no Swift 6 |
| kuraydev/StompClientLib | 1.4.1, 2021-03-07 | 2024-05 | no Package.swift | stale |
| fpseverino/stomp-nio | none | 2026-09-29 | SwiftNIO | too young |
| **launchdarkly/swift-eventsource** | **3.4.0, 2026-09-29** | 2026-09-29 | swift-tools 5.0, iOS 15+; 289 stars; licence not machine-readable (GitHub: NOASSERTION), check before use | **maintained SSE client** |
| apple/swift-openapi-generator | 1.14.0, 2026-10-06 | 2026-10-08 | Apple | maintained |
| square/Valet | 5.1.1, 2026-09-09 | 2026-09-09 | swift-tools 6.0; Apache-2.0 | maintained |
| kishikawakatsumi/KeychainAccess | 4.2.2, 2021-03-01 | 2024-05-31 | swift-tools 5.0 | stale |
| pointfreeco/swift-snapshot-testing | 1.19.6, 2026-09-21 | 2026-09-21 | swift-tools 6.0; MIT | maintained |
| ultralytics/yolo-ios-app | v8.9.16, 2026-10-09 | 2026-10-09 | swift-tools 5.10; **AGPL-3.0** | reference only |

**Repo paths:**
- [PRODUCT.md](../../../PRODUCT.md), [CONTEXT.md](../../../CONTEXT.md), [frontends](../../status-quo/frontends.md), [salvage](../../status-quo/salvage.md), [cv](../../status-quo/cv.md), [ADR 0005](../../adr/0005-rebuild-dont-repair.md);
- `spadeboot/src/main/java/com/spadeboot/config/{WebSocketConfig,WebSocketSecurityConfig}.java`, `spadeboot/src/main/resources/application.yml`, `cv/utils.py`, `scripts/gate.sh`;
- issues #4, #7, #8.

## Recommendation
| Concern | Choice | Why |
|---|---|---|
| UI framework | SwiftUI on a thin Xcode app target; all logic in a local SwiftPM package (`SpadeKit`) | Logic tests run with `swift test`; fewer `project.pbxproj` merge conflicts (synchronized folders) |
| State | `@Observable` models, `@MainActor`, injected via `@Environment` | Observation needs iOS 17 |
| Navigation | `NavigationStack(path:)` with a `Route` enum; login/lobby switch at the root; the scanner as a full-screen cover | Value-based, so a QR "join this table" link can deep-link |
| Concurrency | Swift 6 language mode; default isolation = MainActor (SE-0466); camera and Vision inside an `actor` | Strict checking without annotation noise; Apple's AVCam pattern |
| Minimum iOS | **18.0** | Gives Observation, Swift Vision and `NavigationStack`; every iPhone that runs 26/27 runs 18, so a friend who hasn't updated can still install |
| Camera | AVFoundation (`AVCaptureSession` + `AVCaptureVideoDataOutput`), ~5–10 analysed frames/s, stopped once a stable pair is read | VisionKit's scanner reads only text and barcodes |
| Detection | Export with `nms=True`, FP16 (21.4 MB, §3a), bundled in the app; `VNCoreMLRequest` with `.scaleFit` and `.cpuAndNeuralEngine`; an own dedupe-and-decide step | The documented path for detection results; Ultralytics' own SDK uses the same settings |
| Realtime | **REST for every command, plus one push stream per client carrying versioned, per-viewer table snapshots** | Resync after the phone sleeps is just "reconnect, receive a snapshot"; private cards are filtered by one server function |
| Token storage | Keychain via the Security framework, `kSecAttrAccessibleWhenUnlockedThisDeviceOnly`; no biometrics | ~60 lines, no dependency |
| Token lifetime | A refresh token, or a long-lived revocable device session | The backend's JWT dies after 24 h (`app.jwt.expirationMs: 86400000`), so everyone would log in every poker night |
| Network and TLS | A real domain with a public certificate (hosted, or LAN + DNS-01); otherwise a self-signed cert **pinned via a QR code** on the hub | No ATS exceptions in the first case; the QR also gives "join this table" in one scan |
| Tests | Swift Testing for logic and model-on-stills; XCUITest for 3–4 flows | Swift Testing is the toolchain-6.4 default; UI automation is still XCTest |
| CI | GitHub Actions `xcode-27` runner for the gate's iOS block; Xcode Cloud or a local Archive for TestFlight uploads | $0 for this public repo; Xcode Cloud's 25 h/month come with the membership |
| Distribution | Apple Developer Program + **TestFlight internal testing** | No review, up to 100 testers |

**Alternatives for the big choices**
- **Realtime transport:**
  - **SSE (the hub research's pick).** Same snapshot model; one authenticated GET behind the REST filter chain. The client would be LDSwiftEventSource 3.4.0 (maintained, iOS 15+; licence to check) or a ~50-line parser over `URLSession.bytes`. The server must send heartbeats so dead clients are noticed.
  - **Keep STOMP.** Least backend change and the best web client, but the app would need a hand-written STOMP codec, and the simple broker needs SUBSCRIBE authorisation (issue #2).
- **Detection:**
  - The **Ultralytics iOS SDK** (`YOLOCamera` in ~5 lines): AGPL-3.0, heavy.
  - **Swift Vision `CoreMLRequest`** (iOS 18 async API): Apple's docs don't say it returns detection observations for an NMS pipeline model.
  - **Core AI** (`.aimodel`): needs an iOS 27 minimum; V2.
- **Camera:** a shutter tap with `AVCapturePhotoOutput`. Simpler and lighter on battery, but no live feedback while aiming.
- **Distribution:** TestFlight external testing (public link, but Beta App Review must be able to use the app) or Ad Hoc (100 device IDs per year, manual installs).
- **Minimum iOS 26:** `Observations`, `NetworkConnection` and Liquid Glass without availability checks, but anyone who hasn't updated is locked out.

## 1. SwiftUI architecture
**Platform facts**
- Observation is iOS 17+ (`Observations` async sequence iOS 26+).
- `NavigationStack` with `navigationDestination(for:)` and a path binding is iOS 16+. Apple recommends the path "to support deep links, state restoration, or other kinds of programmatic navigation".
- Xcode 27 (Swift 6.4) deploys to iOS 15–27 but debugs only on iOS 17+.
- SE-0466 makes unannotated code `@MainActor` (`.defaultIsolation(MainActor.self)` in SwiftPM).

**Minimum iOS**
- iOS 26 ran on 79% of all iPhones and 86% of iPhones from the last four years (Apple, 2026-06-07).
- iOS 27 shipped 2026-09-14 and supports iPhone 11 / SE 2 and later (3rd-party list).
- **iOS 18 costs almost nothing** for this app, and building with the iOS 27 SDK still gives the current look on 26+ devices.

**Proposed structure**
```
ios/
  SpadePlayer.xcodeproj        app target only; synchronized folders; Info.plist; Assets; CardDetector.mlpackage
  SpadePlayer/                 @main SpadeApp, AppModel wiring
  Packages/SpadeKit/           local SwiftPM package, platforms [.iOS(.v18), .macOS(.v15)]
    SpadeDomain   Card, Seat, TableSnapshot, LegalAction, Chips (Sendable values)
    SpadeAPI      APIClient (async URLSession), TableStream, DTOs, TokenStore (Keychain), ServerEndpoint
    SpadeScan     CameraSource protocol, CaptureService (actor), StillImageSource, CardDetector (Vision), ScanDecider (pure)
    SpadeUI       screens + design tokens
    Tests/        Swift Testing
  SpadePlayerUITests/          XCUITest
```

**State ownership**
- `AppModel` owns `session` (`.loggedOut` or `.loggedIn(User)`) and `router.path: [Route]`; the root view switches on the session.
- `LobbyModel` lists tables and handles create/join with buy-in. The server does the bankroll check; the UI shows it first.
- `TableModel` (one per seated table):
  - owns the stream task, `snapshot`, `connection` (`.live`, `.reconnecting`, `.offline`);
  - has intents `act(_ LegalAction)` and `submitHoleCards([Card], source:)`;
  - applies only snapshots with a `version` newer than the current one.
- `ScanModel`: `.aiming → .proposed([Card], confidence) → .confirmed`, with `.correcting` reachable from any state.
- Services are protocols placed in `@Environment`, so previews and UI tests inject fakes.

**Screens**
- **Login.**
- **Lobby:** table list, create (configurable blinds, which the legacy app lacked), join with buy-in.
- **Seated:**
  - stack, pot, to-call, whose turn, the board;
  - buttons come from `snapshot.legalActions` (`fold`, `check`, `call(40)`, `raise(min:max:)`, `allIn(amount)`), so **the server computes the legal actions and the app never re-implements poker rules**;
  - raise is a slider plus presets.
- **Scan:** full-screen camera; a "AS 10H — Confirm / Fix" card; Fix opens a 4×13 grid picker. After confirming, the cards stay hidden until pressed.
- **Board and showdown:** from the snapshot; revealed hands, winners and side pots as the server sends them.
- **Privacy:** hide the hole cards whenever `scenePhase != .active`, because iOS takes an app-switcher snapshot.

**Concurrency.** The app module runs Swift 6 with MainActor as default. `SpadeAPI` and `SpadeScan` stay nonisolated with `Sendable` DTOs; camera and Vision work runs inside `actor CaptureService`, as in AVCam. Don't copy the Ultralytics SDK's `@unchecked Sendable` shortcut.

## 2. Camera
| Option | Custom Core ML model? | Fits "point at two cards, read, confirm"? |
|---|---|---|
| AVFoundation `AVCaptureSession` + `AVCaptureVideoDataOutput` → Vision | yes | **Yes.** Full control of resolution, frame rate, torch, stopping |
| VisionKit `DataScannerViewController` (iOS 16) | **no**: `RecognizedDataType` is only `text(…)` and `barcode(…)`, and there is no model parameter | No for cards; **yes for the join QR code** |
| `AVCapturePhotoOutput` (shutter tap) | yes, on a still | Workable fallback; no live feedback |
| Ultralytics `YOLOCamera` | yes | Works; AGPL and a large dependency |

**Setup:**
- Back wide camera; the session preset nearest above the model's 640 px input.
- `alwaysDiscardsLateVideoFrames = true`; at most one Vision request in flight.
- A preview layer in a `UIViewRepresentable`; the right EXIF orientation via a rotation coordinator.
- `NSCameraUsageDescription`; a torch toggle for dark tables.
- Stop the session as soon as a result is accepted.
- **Frames are never stored; only the two labels leave the phone** (issue #4).

## 3. On-device hole-card detection
**Export options** (ultralytics 8.4.174, from its source and the current docs via context7):
- `format="coreml"` produces an `.mlpackage` (ML Program); `imgsz` defaults to 640.
- `quantize` **replaces the deprecated `half`/`int8` flags**:
  - `16` is FP16;
  - `8` is 8-bit k-means palettised weights (weight-only);
  - `32` is FP32.
- `nms=True` appends Apple's NMS pipeline stage (per class unless `agnostic_nms`). Its `iou` and `conf` defaults become inputs that can be overridden per call.
- `dynamic` cannot be combined with `nms=True`.
- The exporter requires `coremltools>=9.0` and **`numpy<=2.3.5`** (numpy 2.4 breaks coremltools export, per its own comment). `cv/` locks numpy 2.5.3, so **the export needs its own environment**.

### 3a. Measured on this Mac (coordinator, 2026-10-09)
**Setup:**
- Apple M1, macOS 27.0.1.
- A throwaway venv: ultralytics 8.4.174, torch 2.14.1, coremltools 9.0, numpy 2.3.5, scikit-learn 1.9.1.
- Model `cv/models/best_60_23.pt` (22.6 MB, YOLOv8s, 52 classes).
- Throwaway worktree `.worktrees/spike-v1-throwaway/spike/coreml/`, not merged.

```bash
uv venv --python 3.12 .venv && VIRTUAL_ENV=.venv uv pip install "ultralytics==8.4.174" "torch==2.14.1" torchvision "coremltools>=9.0" "numpy<=2.3.5" scikit-learn
.venv/bin/yolo export model=best_60_23.pt format=coreml nms=True imgsz=640              # FP16 + NMS
.venv/bin/yolo export model=best_60_23.pt format=coreml nms=True quantize=8 imgsz=640   # INT8 palettised + NMS
.venv/bin/yolo export model=best_60_23.pt format=coreml imgsz=640                       # raw, no NMS
```

| Variant | Export time | `.mlpackage` size | Inputs | Outputs | Loads in coremltools 9.0? |
|---|---|---|---|---|---|
| FP16 + NMS | 3.9 s | **21.4 MB** (20.7 MB zipped) | `image` (640×640 image), `iouThreshold`, `confidenceThreshold` | `confidence` [N×52], `coordinates` [N×4] | yes, 1.4 s |
| INT8 palettised + NMS | 59.1 s | **10.8 MB** | same | same | yes, 1.3 s |
| FP32, raw | 4.1 s | 42.7 MB | `image` | `var_911` [1, 56, 8400] (needs your own decoding and NMS) | yes, 0.4 s |

**Parity check:**
- 40 synthetic boards: 3–5 card PNGs from the legacy deck on a noisy green background, half the cards turned 180°, squashed to 640×640, seed 7.
- Each run through both Core ML NMS variants (`CPU_AND_NE`, conf 0.25, IoU 0.7) and through PyTorch.

| | Same label set as PyTorch | Card labels hit / missed / extra (157 cards) | `predict` median (p95) on the M1 |
|---|---|---|---|
| PyTorch (reference) | — | 132 / 25 / 91 | — |
| Core ML FP16 + NMS | 36 / 40 boards | 133 / 24 / 94 | 6.4 ms (9.4 ms) |
| Core ML INT8 + NMS | 36 / 40 boards | 132 / 25 / 91 | 6.0 ms (6.7 ms) |

**Read:**
- The export works and both NMS variants behave like the PyTorch model; the INT8 totals even match exactly. The 4 disagreeing boards are borderline detections.
- **The hit and extra counts describe this synthetic set, not the model's real accuracy:** the training deck differs from the PNG deck. See the [proxy spike](cv-proxy-spike.md) for the measurement on synthetic boards, and #3 for the real one.
- Warnings: coremltools 9.0 says torch 2.14.1 "has not been tested" (most recent tested: 2.7.0), and its scikit-learn converter is disabled for sklearn 1.9.1 (irrelevant for this model).

### Running it in the app
| Step | Choice | Source |
|---|---|---|
| Load | `MLModelConfiguration().computeUnits = .cpuAndNeuralEngine`; `VNCoreMLModel(for:)` | Ultralytics SDK default (avoids GPU contention with the camera) |
| Request | `VNCoreMLRequest`, `imageCropAndScaleOption = .scaleFit` | aspect-fit matches Ultralytics' letterboxed training (`BasePredictor.swift`) |
| Results | `[VNRecognizedObjectObservation]`: labels sorted by confidence, normalised boxes | Apple live-capture sample |
| Swift Vision API | `CoreMLRequest.perform(on:orientation:)` (iOS 18) | docs list only classification, pixel-buffer and feature-value observations; **detection support unconfirmed** |

### Post-processing (`ScanDecider`, a pure function)
The legacy `get_n_cards` keeps unique labels in result order. Replace that with:
1. Drop labels outside the 52-card set and anything below a confidence floor (start at 0.5; tune in #3).
2. Group by label and score each card by the **max** confidence over its boxes; a card is often detected once per printed corner.
3. Take the top 2. If a third label is within a margin of the second, return `.ambiguous` and open Fix with the candidates preselected.
4. **Temporal stability:** propose a pair only when it appears in ≥3 of the last 5 analysed frames.
5. Optionally compare an `agnostic_nms=True` export in #3; it removes two different labels on one corner.

### Latency
- **Measured:** ~6 ms on the M1 Mac (above).
- **Vendor claim:** YOLO26n INT8 at 640, a smaller model, runs 3.2 ms on the Neural Engine of an iPhone 17 Pro.
- YOLOv8s is 28.6 GFLOPs and 11.2M parameters.
- **Estimate, not measured:** tens of milliseconds on older supported iPhones. Even 50 ms allows 5–10 analysed frames/s, plenty for aim-and-confirm. Measure on the oldest friend's phone.

### Shipping the model
- Bundle the FP16 package; Xcode compiles it to `.mlmodelc`. Use INT8 only if #3 shows equal accuracy.
- **A retrained model:**
  1. re-export in the pinned export environment;
  2. commit the `.mlpackage` (Git LFS) with a manifest (source `.pt` SHA-256, versions, export args);
  3. the gate checks the manifest;
  4. ship a new TestFlight build.
- Alternative: download at runtime and `MLModel.compileModel(at:)` (Apple article).
- **Trusting phone-reported cards** (#4): accept them, but the server rejects or flags a card already on the board or claimed by another seat. That uniqueness check is also the best misread detector.

## 4. Realtime client
**Facts**
- No maintained Swift STOMP client exists (package table).
- `URLSessionWebSocketTask` (iOS 13) has async send/receive, `sendPing`, close codes, and accepts a `URLRequest`, so the app can send an `Authorization` header.
- Spring's raw WebSocket API is `TextWebSocketHandler` + `ConcurrentWebSocketSessionDecorator`, needed because concurrent sends aren't allowed.
- `SseEmitter` must "send data periodically", because the Servlet API gives no notice when a client goes away.
- Browsers can't set headers on `WebSocket` or `EventSource`, so the hub needs a cookie or a one-time ticket either way.

| | STOMP (today) | WebSocket + JSON snapshots | SSE + REST |
|---|---|---|---|
| iOS client | hand-written STOMP codec | built in | LDSwiftEventSource 3.4.0 or a ~50-line parser |
| Backend change | SUBSCRIBE authorisation, `/user/queue` for private data, origins | one `TextWebSocketHandler`, handshake auth, session registry | `SseEmitter` endpoint + heartbeat; same filter chain as REST |
| Auth on connect | token in the CONNECT frame | `Authorization` on the upgrade (app); ticket or cookie (hub) | same as WebSocket |
| Liveness | STOMP heartbeats | client `sendPing` | server pings + client watchdog |
| Private data | user destinations: one wrong SUBSCRIBE rule leaks | each connection gets a snapshot projected for its viewer | same |

**The research agents disagree on the transport, not the model.**
- The iOS agent recommended WebSocket, believing Swift had no SSE client.
- The coordinator checked: **LDSwiftEventSource 3.4.0 was released 2026-09-29 and is maintained**, so that argument falls away.
- The hub research recommends SSE, because the realtime channel then sits behind the same Spring Security filters as REST.
- Both agree on everything else:
  - REST for commands;
  - full per-viewer snapshots on connect and on every change;
  - a monotonic `version`;
  - reconnect = resync.
- **The grill picks the transport;** the model makes it swappable.

**Protocol sketch**
- Commands:
  - `POST /api/v1/tables/{id}/actions {handId, expectedVersion, clientActionId, action, amount}` → the new snapshot, or 409 if stale;
  - `PUT /api/v1/hands/{id}/hole-cards {cards, source: "scan"|"manual"}`.
- The stream carries only `{"type":"snapshot","version":N,"table":{…}}`. A table is at most 10 seats, so a full snapshot every time removes event replay entirely.
- **Reconnect** on `scenePhase == .active`, a network path change, a receive error or a missed heartbeat. Back off 0.5 → 8 s and refresh the token if it is near expiry. The first snapshot is the resync.

**What it requires of the backend:**
- one projection function, `TableView forViewer(viewer)`, the only place hole cards are filtered;
- a property test that no viewer ever gets another seat's hole cards before showdown;
- a monotonic `version` per table;
- `GET /tables/{id}/state` returning the same projection;
- idempotent actions;
- a hub viewer role.

## 5. Auth token storage
- **Keychain:** `SecItemAdd`/`CopyMatching`/`Update`/`Delete` on `kSecClassGenericPassword`, in a ~60-line `TokenStore`, with `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` (not restored to another device's backup, so a new phone logs in once).
- **No biometrics:** the phone is already unlocked in the player's hand.
- **Wrapper, if wanted:** Valet 5.1.1. Avoid KeychainAccess (last release 2021).
- **Refresh (backend work):** the 24 h JWT without refresh contradicts PRODUCT.md's "install once". Options:
  - a short access JWT plus a rotating, revocable refresh token;
  - a long-lived per-device session token stored hashed on the server.
  On a 401 the app refreshes once, retries, then shows login. Tokens are never logged.

## 6. Local network: ATS, local network privacy, and #8
**ATS facts**
- ATS requires TLS 1.2+ with forward secrecy and a trusted, name-matching certificate.
- With ATS on, you can only *tighten* trust evaluation (pinning), never loosen it.
- Since iOS 17, ATS blocks IP-address connections by default. `NSAllowsLocalNetworking = YES` re-allows `.local` names and IPs.

**Local network privacy (TN3179)**
- *Any* TCP connection to a local address triggers the one-time Local Network alert, even when a public DNS name resolves to `192.168.x.y`.
- It needs `NSLocalNetworkUsageDescription`.
- The first attempt may fail while the alert is up; there is no API to query the state.
- The Simulator doesn't show the alert.

| Where Spade runs (#8) | iOS friction | Notes |
|---|---|---|
| **Hosted backend, real domain, public cert** | none | Needs internet at the night; the table-camera box connects outward; works on cellular if the Wi-Fi is flaky |
| **Laptop on the LAN, real domain → LAN IP, Let's Encrypt via DNS-01** | Local Network alert once | Needs a DNS provider API and a DHCP reservation. **FRITZ!Box rebind protection blocks public names resolving to private IPs** until an exception is added. Certificates shrink to 64 days (2027) and 45 days (2028), so renewal must be automated |
| **Laptop on the LAN, self-signed certificate** | `NSAllowsLocalNetworking` + alert + pinning | Pin the SPKI hash, delivered in the hub's join QR (`spade://join?host=…&table=…&pin=sha256/…`); never accept arbitrary certificates |
| Plain `http://` on the LAN | `NSAllowsLocalNetworking` | Tokens and hole cards in clear: no |

**Whatever #8 decides:**
- a `ServerEndpoint` holding host + optional pin;
- ship `NSLocalNetworkUsageDescription` and `NSAllowsLocalNetworking` from day one (harmless when hosted);
- make **a QR code on the hub** the setup path, giving server, trust and table in one scan.

## 7. Testing
| Layer | Tool | What |
|---|---|---|
| Domain and logic | Swift Testing in `SpadeKit` | `ScanDecider` (corner duplicates, ambiguity, stability), the snapshot reducer (stale versions ignored), label parsing, **DTO decoding against JSON fixtures shared with the backend's contract snapshot** |
| Model on stills | Swift Testing in the simulator with `VNImageRequestHandler(cgImage:)` on the spike's fixtures | Assert labels, not exact confidences (the simulator has no Neural Engine) |
| Camera without a camera | a `CameraSource` protocol with a `StillImageSource`, chosen by launch argument | The Simulator has no camera |
| Flows | XCUITest with launch arguments selecting fake services or the backend's `testworld` | login → lobby → join → act; scan → confirm; scan → fix |
| Views | previews fed by the same fakes; optionally swift-snapshot-testing 1.19.6 | pinned to one simulator |

## 8. CI and the gate
- **GitHub Actions:**
  - "Free … for public repositories that use standard GitHub-hosted runners"; this repo is public. A private repo would pay $0.062/min on macOS, ~10× Linux.
  - The standard arm64 macOS runner has 3 M1 cores, 7 GB RAM and 14 GB SSD.
  - The `xcode-27` image (public preview) carries Xcode 27.0/27.1 and iOS 27 simulators.
  - Watch runner-images #14837 (keychain unlock on Xcode 27); it matters only if CI signs builds.
- **Xcode Cloud:** 25 compute hours/month included with the membership; extra 100 h is $49.99/month. Best used for archive → TestFlight, so signing stays with Apple.
- **`scripts/gate.sh --ios`:**
```bash
xcodebuild -version >/dev/null 2>&1 || echo "! no Xcode: iOS block skipped (not proven)"   # CLT-only Macs
swift format lint --strict --recursive ios/
(cd ios/Packages/SpadeKit && swift test)
xcodebuild test -project ios/SpadePlayer.xcodeproj -scheme SpadePlayer -testPlan Gate \
  -destination 'platform=iOS Simulator,name=iPhone 17' -quiet
python3 scripts/check_model_manifest.py
```
- **New "not proven" lines:** no real iPhone, camera, Neural Engine timing or Local Network alert; no TestFlight install.

## 9. Distribution
| Route | Paid program? | Fits 6–10 friends? |
|---|---|---|
| Free Apple Account (Personal Team) | no | **No:** 3 devices, apps expire after 7 days, each install needs the phone on the owner's Mac. Fine for the owner's own phone during development |
| **TestFlight internal** | yes (99 USD/year) | **Yes:** up to 100 testers, who must be App Store Connect users of the team (a role such as Marketing); no review step documented; builds last 90 days |
| TestFlight external | yes | Possible: public link, but the first build goes through Beta App Review, which a LAN-only backend may make impossible (inference) |
| Ad Hoc | yes | 100 iPhones per year by device ID; manual installs |
| Unlisted App Store | yes | Overkill: full App Review |

The 90-day expiry means uploading at least one build a quarter.

## Owner actions and costs
1. **Install Xcode 27.** A 3.1 GB download (installed size much larger, not measured; ~50 GB free on this disk). Then the iOS 27 simulator runtime, `sudo xcode-select -s /Applications/Xcode.app`, and the licence. Until then nothing iOS can be built, previewed or tested. The Core ML export already works without it.
2. **Apple Developer Program, 99 USD/year.** Needed for TestFlight and Xcode Cloud. It can wait until the first install on a friend's phone; the free Personal Team covers the owner's own iPhone.
3. **Owner's iPhone:** Developer Mode on.
4. **Each friend:** an Apple Account email for the invite, the TestFlight app, and their iPhone model and iOS version (the oldest sets the latency test).
5. **#8:** a domain and certificate setup (plus a router rebind exception if on the LAN), or a small hosted server.
6. **CI:** $0.

## Open questions for the V1 grill (recommended default first)
1. **Minimum iOS:** **18.0**; 26 if everyone is already on 26+.
2. **Scan style:** **a live view proposing a stable pair**, with a shutter fallback.
3. **Hole-card peek:** **press-and-hold**, auto-hidden (the legacy app used tap-to-toggle).
4. **Keep the screen awake while seated:** **yes**. Lock-screen "your turn" alerts need push notifications (APNs); deferred.
5. **Model precision:** **FP16**; INT8 only if #3 shows equal accuracy.
6. **Joining:** **a QR code on the hub** (server, certificate pin, table); the lobby list stays as a fallback.
7. **Look:** **the webapp's tokens on standard SwiftUI controls**, not a fully custom control set.
8. **Sign-in:** **username/password + refresh**; Sign in with Apple later.
9. **Distribution:** **TestFlight internal**.
10. **Trusting phone-reported cards:** **trust, plus the server's uniqueness check**.

## Not proven
- **Nothing iOS was built or run;** Xcode is not installed here.
- **iPhone latency is unmeasured.** The only number is ~6 ms on the M1 through coremltools. The iPhone 17 Pro figure is a vendor claim for a smaller model, and "tens of ms on older phones" is an estimate.
- **Accuracy is unmeasured:**
  - whether on-device FP16/INT8 matches the server path on real cards, under real light, at hand distance is #3's job;
  - the parity above is on synthetic boards only.
- **Not confirmed from Apple's docs:**
  - whether Swift Vision's `CoreMLRequest` returns detection observations for an NMS pipeline model;
  - whether `MLModel.compileModel(at:)` accepts an `.mlpackage`.
- **Not tried on a device or router:** the Local Network alert, ATS with a pinned self-signed certificate, FRITZ!Box rebind handling.
- **Secondary sources only:** the iOS 27 device list, the On-Demand Resources deprecation, WWDC26 camera details, the Let's Encrypt timeline.
- **LDSwiftEventSource's licence** is not machine-readable on GitHub; read it before adopting.
- **The export ran on a torch version coremltools hasn't tested** (2.14.1 vs 2.7.0); a retrain should pin the export environment.
