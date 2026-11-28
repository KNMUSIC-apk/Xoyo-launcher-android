# StarlingEngine

Starling (https://github.com/starling-client/starling, branch `starling`, commit 0e5bb58, MIT) vendored into
XOYO as the Rust Edition's native engine. This is the subset of its workspace that `crates/horizon-android`
needs to build `libhorizon_android.so` (the NativeActivity library), plus the five patched forks in
`third_party/` and the few files outside `crates/` that the build reads.

- Minecraft code and assets are NOT included. The engine downloads or reads the player's own game files at run time.
- Built by CI job `starling-native` (`.github/workflows/xoyo-ci.yml`) with Rust 1.99.1, NDK 28.2.13676358 and cargo-ndk.
- Re-vendor from upstream when updating; do not hand-edit files under `crates/` or `third_party/`.
