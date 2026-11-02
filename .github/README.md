# WallHouse TV

Set up your Android TV or Google TV from your phone: install a new home screen with one tap, change it, and undo it,
over Wi-Fi, with no computer and no root.

WallHouse TV is a fork of [Tellyscope](https://github.com/man15h/tellyscope) by man15h. Everything Tellyscope does is
still here (turn off the bloat, install apps, send files, the remote); this fork adds a launcher catalogue and
installs launchers that aren't on the Play Store, starting with
[CleanYourTV · Nothing edition](https://github.com/TheWallHouse/cleanyourtv).

## What's new

- **Launcher catalogue** ([`catalog/launchers.json`](../app/src/main/resources/catalog/launchers.json)): each launcher
  with its GitHub repo and signing certificate. The app reads it from this repo, so a new launcher is a pull request,
  not an app update. Only this list may pin a download.
- **Download and install** for launchers that aren't on the Play Store: the latest release's APK, checked against its
  package name and certificate before anything reaches the TV.
- **Google TV with its setup wizard** (Google TV Streamer, Chromecast with Google TV): Make Home no longer turns
  Google TV off, which left an empty screen there. The launcher's accessibility service holds Home and Google TV,
  with Cast, stays on. Going back turns the service off.
- **Make Home waits for the TV** after turning the old launcher off, and the TV list leaves out the phone itself.

## Install

Download the APK from the [latest release](https://github.com/TheWallHouse/wallhouse-tv/releases/latest) on your
Android phone and open it. It installs beside Tellyscope.

Step by step, with screenshots (Spanish): **[docs/guia](../docs/guia/README.md)**.

## Building

As Tellyscope: JDK 25 and the Android SDK with platform 37, then `./gradlew :app:assembleDebug`.

## Licence

GPL-3.0, like Tellyscope: see [LICENSE](../LICENSE) and [NOTICE](../NOTICE). Not affiliated with Tellyscope, Google or
Nothing.
