# Guide: WallHouse Launcher on your TV, from your phone

With **WallHouse TV** you replace your Android TV or Google TV home screen with **WallHouse Launcher**: no ads, no
network, seven looks. No computer: everything happens over Wi-Fi from an Android phone, and it can all be undone.

Screenshots from a Vestel Cosmos (Android TV 11) and a Xiaomi phone. Menu names change from TV to TV; the steps
don't.

**You need:** an Android phone, the TV and the phone **on the same Wi-Fi**, and about five minutes.

This is the TV before: the home screen it came with.

<img src="01-tv-stock.jpg" width="560" alt="The TV's own Android TV home screen">

## 1. Get the TV ready

1. **Unlock developer options:** Settings → Device Preferences → About → go down to **Android TV OS build** and
   press OK **seven times**, until it says you are a developer.

   <img src="02-tv-about-build.jpg" width="560" alt="About, with Android TV OS build selected">

2. **Turn on debugging:** Settings → Device Preferences → **Developer options** → **USB debugging**. Turn on
   **Wireless debugging** or **Network debugging** too if your TV lists them; this one doesn't need them.

   <img src="03-tv-usb-debugging.jpg" width="560" alt="Developer options with USB debugging on">

## 2. Install WallHouse TV and find the TV

Download the APK from the [latest release](https://github.com/TheWallHouse/wallhouse-tv/releases/latest) and open it
on the phone.

<img src="04-splash.jpg" width="300" alt="WallHouse TV starting"> <img src="05-welcome.jpg" width="300" alt="Welcome to WallHouse TV">

The first screen sums up what you are about to do: tap **Continue**. Pick your kind of TV; WallHouse TV checks that
developer options and debugging are on and **looks for TVs on your Wi-Fi**. Tap **Connect** on yours.

<img src="06-find-your-tv.jpg" width="300" alt="Add your TV: the TVs found, each with Connect">

> None listed? **Scan again**, or add it by its IP address (on the TV: Settings → Network). A VPN on the phone can
> hide the TV: pause it for a moment.

## 3. Allow the phone on the TV

The phone shows its key and waits. A box pops up on the TV with the same key.

<img src="07-allow-on-your-tv.jpg" width="300" alt="Allow on your TV: the phone's key, waiting">

On the TV, tick **Always allow from this computer** and select **Allow**. (A TV with Wireless debugging may show a
pairing code instead: type it on the phone.)

<img src="08-tv-allow.jpg" width="560" alt="The TV asks: Allow USB debugging?">

You're in: the phone shows your TV.

<img src="09-overview.jpg" width="300" alt="The TV's overview in WallHouse TV">

## 4. Install WallHouse Launcher

Go to **Tweaks** → **Home screen** → **Get another launcher**.

<img src="10-tweaks.jpg" width="300" alt="Tweaks: Home screen and Get another launcher">

**WallHouse Launcher** comes first. Before anything downloads, the picker warns you if your TV is one where it won't
work or needs one more step (too old, a Fire TV, a TV made for China). Choose it and tap **Download and install**.

<img src="11-choose-launcher.jpg" width="300" alt="Choose a home screen: WallHouse Launcher selected">

WallHouse TV shows **where it comes from**: this project's GitHub release. Tap **Download and install** again.

<img src="12-where-it-comes-from.jpg" width="300" alt="Where it comes from: github.com/TheWallHouse/wallhouse-launcher">

It downloads it, **checks it is signed by its developer** (if not, it installs nothing) and installs it on the TV.
Keep WallHouse TV open until it's done.

<img src="13-installing.jpg" width="300" alt="Downloaded, signed by its developer, installing on the TV">

## 5. Make it the home screen

Tap **Yes, switch home screen**. The text says exactly what changes on your TV and how to undo it.

<img src="14-make-it-home.jpg" width="300" alt="Make it the home screen?">

Then it asks about its **accessibility service**: it keeps the remote's **Home** button opening WallHouse Launcher.
Tap **Turn it on**.

<img src="15-accessibility.jpg" width="300" alt="Turn on WallHouse Launcher's accessibility service?">

Done: the phone says it is the home screen…

<img src="16-done.jpg" width="300" alt="Made WallHouse Launcher Home">

…and the TV shows it.

<img src="17-tv-wallhouse.jpg" width="560" alt="The TV with WallHouse Launcher, Orbit look">

## 6. Make it yours

In **Customize**, pick one of the seven looks and its tone, a custom message after the greeting, the apps per row,
the dock and more. The TV changes at once. (On the TV itself: a long press on Settings opens Launcher settings.)

<img src="18-customize.jpg" width="300" alt="Customize: look, tone and custom message">

**When you're done**, turn debugging off on the TV (Developer options) if you won't use it again.

## Going back

In **Tweaks** → **Home screen**, tap **Make Home** on your TV's own launcher: Home opens it again at once. You can
also **Undo** any step from **History**, or **Uninstall** WallHouse Launcher from the same place.

## If something's off

- **On a Google TV, Google TV shows at power on:** that's normal. WallHouse Launcher appears as soon as you press
  Home.
- **It doesn't find the TV:** check the phone and the TV are on the same Wi-Fi, and pause any VPN on the phone.
- **The remote's volume doesn't work:** not the launcher's doing. Set the remote to control the TV by infrared
  (Settings → Remotes & Accessories) and turn on CEC on the TV.
