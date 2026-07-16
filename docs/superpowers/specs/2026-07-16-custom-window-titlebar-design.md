# Hikalist Custom Window Title Bar Design

## Goal

Replace the white native Windows title bar with a dark Hikalist title bar that visually belongs to the application while preserving expected desktop window behavior.

## Window behavior

- The Compose `Window` is undecorated and remains opaque.
- A dedicated top bar is rendered above `AppLayout` using the active Hikalist theme.
- Dragging an empty part of the title bar moves a restored window.
- Double-clicking an empty part toggles maximize and restore.
- Minimize sends the window to the Windows taskbar.
- Maximize toggles between the maximized and restored bounds.
- Close uses Compose's existing `exitApplication` path so playback, Discord RPC, Supabase, and other resources are disposed normally.
- The title bar buttons use clear hover states; close uses a destructive red hover state.
- The window remains resizable from its edges and corners and retains a minimum size of 900 x 560.

## Structure

- `Main.kt` owns the window state, close action, and placement of the title bar above the application content.
- `HikalistWindowTitleBar.kt` owns title-bar rendering and delegates window operations to a small controller.
- `DesktopWindowController.kt` owns AWT window movement, resize hit testing, minimize, maximize, restore, and cursor updates. Geometry calculations are isolated in pure functions for unit testing.

## Visual layout

- Height: 38 dp.
- Left: compact Hikalist flower mark and the text `Hikalist`.
- Center: empty draggable area.
- Right: minimize, maximize/restore, and close buttons using simple line icons.
- No white separator or native title strip is shown.

## Safety and compatibility

- Resizing is limited to an 8 px edge hit area.
- Dragging is ignored when the window is maximized; dragging down first restores the window before movement.
- Maximize uses the active screen's usable bounds so the Windows taskbar is not covered.
- No Android code, database schema, account data, or application version is changed.

## Verification

- Unit tests cover resize-edge detection and maximize/restore state decisions.
- The complete desktop test suite must pass.
- Runtime smoke testing must confirm open, drag, minimize, maximize/restore, resize, and clean close.
- Only after runtime verification may `packageExe` be run again.
