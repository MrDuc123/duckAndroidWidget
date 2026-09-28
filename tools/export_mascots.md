# Updating the mascot pictures in the Android app

`assets/mascots/*.svg` are the source of truth. The Android widget can't draw these SVGs directly, so the app uses PNG copies in `app/src/main/res/drawable-nodpi/`:

| PNG | from SVG |
|---|---|
| `duck_happy.png` | `duck-happy.svg` |
| `duck_calm.png` | `duck-calm.svg` |
| `duck_sleepy.png` | `duck-sleepy.svg` |
| `cat_box_happy.png` | `cat-box-peeker-happy.svg` |
| `cat_box_calm.png` | `cat-box-peeker.svg` |
| `cat_curl_sleepy.png` | `cat-cozy-curl.svg` |

When you change an SVG, export it again as a 384 × 384 PNG with a transparent background and replace the matching file. Any SVG tool works (Figma, Inkscape, or Android Studio: right-click `res` > New > Vector Asset imports an SVG directly).
