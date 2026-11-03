# Organiser for Pixel

A Material You app that sorts every app on your Pixel into tidy categories.

## What it does
- Finds every launchable app and sorts it into 13 categories (Social, Messages & Calls, Productivity, Watch & Listen, Games, Photos & Camera, Shopping & Money, Maps & Travel, News & Reading, Health & Fitness, Tools, Google & System, Other).
- Sorting uses, in order: a built-in list of ~100 popular app package names, the category the developer declared to Android, then keyword rules on the app name.
- Long-press any app to move it to another category. Your choice is saved.
- Search bar and filter chips across the top. Tap an app to open it.
- Colours follow your wallpaper (Material You dynamic colour, Android 12+), with light and dark mode.
- Re-sorts automatically when you install or uninstall an app.

## Run it
1. Install Android Studio (Ladybug 2024.2 or newer).
2. File > Open, choose this `PixelOrganiser` folder, let Gradle sync.
3. On your Pixel: Settings > About phone > tap Build number 7 times, then Settings > System > Developer options > USB debugging on.
4. Plug the phone in and press Run.

To use it as your home screen, uncomment the HOME intent filter in `app/src/main/AndroidManifest.xml`.

## Where things live
- `data/Categorizer.kt` – the sorting rules. Add package names or keywords here.
- `data/Category.kt` – category names, icons and accent colours.
- `ui/OrganiserScreen.kt` – the whole UI.
- `ui/theme/Theme.kt` – Material You theme.
