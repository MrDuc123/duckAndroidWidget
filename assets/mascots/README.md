## Mascots

The app's mascots live in `assets/mascots/` as standalone SVG files. They are the single source of truth: load them from there, never paste their SVG drawing into components or copy it into other files.

| File | Character | Mood |
|---|---|---|
| `duck-happy.svg` | Duck | Happy |
| `duck-calm.svg` | Duck | Calm |
| `duck-sleepy.svg` | Duck | Sleepy |
| `cat-box-peeker-happy.svg` | Cat peeking out of a box | Happy |
| `cat-box-peeker.svg` | Cat peeking out of a box | Calm |
| `cat-box-peeker-sleepy.svg` | Cat peeking out of a box | Sleepy (used as the yawn in the bedtime animation) |
| `cat-cozy-curl.svg` | Cat curled up asleep | Sleepy (the box cat's sleeping pose) |
| `cat-cozy-curl-happy.svg` | Cat curled up | Happy (spare) |
| `cat-cozy-curl-calm.svg` | Cat curled up | Calm (spare) |

### Style rules for new poses or moods

- Kawaii blob style: round, soft shapes, flat solid fills. No outlines, gradients, drop shadows, or 3D shading.
- Canvas: `viewBox="0 0 120 120"`, transparent background, character centred with a soft ground shadow (ellipse, `#3b3350` at 12% opacity).
- Where parts of the same colour overlap (head over body, paws, tail), separate them with a thin 1.6px edge in a slightly darker shade of the body colour (cat: `#a79dc2`), plus a soft shadow on the body under resting parts (cat: `#b9b1d0`). No dark outlines and no white gaps.
- Faces: small and low on the head. Pink cheeks, tiny pink triangle nose, "ω"-shaped mouth, thin whiskers on the cat.
- Eyes by mood: happy = upward arcs (^ ^) with an open smile; calm = round dark eyes with a white highlight; sleepy = downward closed arcs, plus drawn "z" shapes (paths, not text) and optionally a moon or sleep bubble.
- Palette:
  - Duck: body `#ffd84d`, tuft/wings `#ffc933`, bill/feet `#ff9f43`
  - Cat: body `#c9c2dc`, tail/stripes `#aea5c6`–`#b7aecf`, inner ears `#ffc2d1`, mouth lines `#5a4a7a`, whiskers `#8a7fa8`
  - Shared: eyes `#1a1a1a`/`#141414`, cheeks `#ffb3c1` (calm) / `#ff8fa8` (happy), nose `#ff7f9a`, moon `#ffd35c`
- No text elements inside the SVGs, so they render the same everywhere without fonts.

### Animation parts

Each SVG groups its moving parts with `data-part` labels. The drawings are unchanged; animation code in the app targets these labels, so the SVG files stay the single source of truth.

| Label | Found in | Used for |
|---|---|---|
| `duck` | all duck files | hop, sway, nod (everything except the ground shadow) |
| `wing-l`, `wing-r` | duck | wing flaps |
| `eyes` | duck, box cat, curled cat | blinking |
| `bill` | duck | quack when tapped |
| `bubble` | `duck-sleepy.svg` | breathing, pops on wake-up |
| `cat`, `box`, `paws` | box cat files | peekaboo, bedtime sink, jack-in-the-box, box wobble |
| `ear-l`, `ear-r` | box cat, curled cat | ear twitch |
| `body`, `tail`, `head` | curled cat files | breathing, tail swish, head perk |
| `sparkles`, `zz`, `moon`, `shadow` | where drawn | twinkle, floating z's |

When you draw a new pose or mood, keep the same labels so the existing animations work on it.
