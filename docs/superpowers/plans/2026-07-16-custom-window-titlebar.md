# Hikalist Custom Window Title Bar Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the native white Windows title bar with a themed Hikalist title bar while retaining standard move, resize, minimize, maximize/restore, and close behavior.

**Architecture:** A pure geometry module calculates resize hit areas and bounds. An AWT controller applies those calculations to the undecorated JFrame, while a focused Compose component renders the title bar and invokes the controller. `Main.kt` only owns lifecycle and layout wiring.

**Tech Stack:** Kotlin/JVM 21, Compose Desktop, Swing/AWT, JUnit 4, Gradle Compose packaging.

## Global Constraints

- Desktop code only; Android code and database schema remain untouched.
- Keep the application version at `1.0.0`.
- Preserve the existing `exitApplication` disposal path.
- Do not commit or push without separate authorization.
- Build the EXE only after tests and runtime smoke verification pass.

---

### Task 1: Window resize geometry

**Files:**
- Create: `desktop/app/src/main/kotlin/com/metrolist/desktop/window/WindowResizeGeometry.kt`
- Test: `desktop/app/src/test/kotlin/com/metrolist/desktop/window/WindowResizeGeometryTest.kt`

**Interfaces:**
- Produces: `ResizeEdge`, `resizeEdgeAt(x, y, width, height, border)`, and `resizedBounds(start, edge, dx, dy, minimumWidth, minimumHeight)`.

- [ ] Write tests for all edges, corners, center, and minimum-size clamping.
- [ ] Run `./gradlew :desktop:app:test --tests "com.metrolist.desktop.window.WindowResizeGeometryTest"` and confirm unresolved symbols fail compilation.
- [ ] Implement pure geometry with no UI dependencies.
- [ ] Rerun the targeted test and confirm it passes.

### Task 2: AWT desktop window controller

**Files:**
- Create: `desktop/app/src/main/kotlin/com/metrolist/desktop/window/DesktopWindowController.kt`

**Interfaces:**
- Consumes: Task 1 geometry functions.
- Produces: `install()`, `moveBy(dx, dy)`, `minimize()`, `toggleMaximize()`, `restoreForDrag(anchorFraction)`, `isMaximized`, and `close()`.

- [ ] Install a scoped AWT mouse listener that reacts only to events inside the Hikalist frame.
- [ ] Map the 8 px resize edge to standard Windows resize cursors.
- [ ] Apply resize bounds with the 900 x 560 minimum size.
- [ ] Preserve restored bounds and use the active screen's usable area for maximization through the frame state.
- [ ] Remove the global listener in `close()`.

### Task 3: Compose title bar and window integration

**Files:**
- Create: `desktop/app/src/main/kotlin/com/metrolist/desktop/window/HikalistWindowTitleBar.kt`
- Modify: `desktop/app/src/main/kotlin/com/metrolist/desktop/Main.kt`

**Interfaces:**
- Consumes: `DesktopWindowController` from Task 2 and existing `HikalistLogo`.

- [ ] Render a 38 dp title bar with logo/name and three controls.
- [ ] Add themed hover states and a red close hover state.
- [ ] Add drag movement and double-click maximize/restore to the non-button title area.
- [ ] Set the Compose window to `undecorated = true` and place `AppLayout` below the custom bar.
- [ ] Install and dispose the AWT controller with `DisposableEffect`.
- [ ] Keep `window.minimumSize = Dimension(900, 560)` and route close to `exitApplication`.

### Task 4: Verification and installer

**Files:**
- Verify: `desktop/app/src/main/resources/icon.ico`
- Output: `desktop/app/build/compose/binaries/main/exe/Hikalist-1.0.0.exe`

- [ ] Run `./gradlew :desktop:app:test --rerun-tasks` and require exit code 0.
- [ ] Run the app, confirm the `Hikalist` window opens without a native white title bar, and close it through the custom close button.
- [ ] Confirm the process exits after closing.
- [ ] Run `./gradlew :desktop:app:packageExe --rerun-tasks` and require exit code 0.
- [ ] Inspect the generated installer path, size, SHA-256 hash, and bundled application image for `ffmpeg.exe`.
