---
name: Shop Manual
description: A factory service manual as a staff console — numbered procedure, printed severity, no floating panels.
colors:
  stock: "#eaeae5"
  stock-warm: "#efece3"
  stock-cool: "#e4e8e6"
  plate: "#f1f0ea"
  plate-sunk: "#e2e2db"
  ink: "#161a1d"
  ink-2: "#454c51"
  ink-3: "#586065"
  ink-inv: "#f1f0ea"
  rule: "#c7c6be"
  rule-strong: "#9a9890"
  warn: "#c2321b"
  warn-field: "#f6e3de"
  caution: "#a86e0d"
  caution-ink: "#7a4f08"
  caution-field: "#f7ead1"
  ref: "#1f4e79"
  ref-field: "#dfe7ef"
  ok: "#2f6350"
  ok-field: "#dde8e2"
  sec-work-orders: "#1f4e79"
  sec-schedule: "#2f6350"
  sec-inventory: "#8a5a1e"
  sec-customers: "#6b3a5b"
  sec-vehicles: "#3f4e63"
  sec-workers: "#5c5344"
  highlighter: "#f3d98a"
typography:
  display:
    fontFamily: "Barlow Semi Condensed, Barlow, ui-sans-serif, sans-serif"
    fontSize: "2.25rem"
    fontWeight: 700
    lineHeight: 1
    letterSpacing: "-0.02em"
  headline:
    fontFamily: "Barlow Semi Condensed, Barlow, ui-sans-serif, sans-serif"
    fontSize: "1.625rem"
    fontWeight: 600
    lineHeight: 1.1
    letterSpacing: "-0.01em"
  title:
    fontFamily: "Barlow Semi Condensed, Barlow, ui-sans-serif, sans-serif"
    fontSize: "1.25rem"
    fontWeight: 600
    lineHeight: 1.1
    letterSpacing: "-0.01em"
  plate-title:
    fontFamily: "Barlow Semi Condensed, Barlow, ui-sans-serif, sans-serif"
    fontSize: "1rem"
    fontWeight: 600
    lineHeight: 1.1
    letterSpacing: "0.01em"
  body:
    fontFamily: "Barlow, ui-sans-serif, system-ui, sans-serif"
    fontSize: "0.875rem"
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: "normal"
  label:
    fontFamily: "Barlow Semi Condensed, Barlow, ui-sans-serif, sans-serif"
    fontSize: "0.75rem"
    fontWeight: 600
    lineHeight: 1.5
    letterSpacing: "0.1em"
  identifier:
    fontFamily: "Azeret Mono, ui-monospace, Cascadia Mono, monospace"
    fontSize: "0.8125em"
    fontWeight: 500
    lineHeight: 1.5
    letterSpacing: "-0.02em"
rounded:
  sharp: "0"
spacing:
  hair: "0.25rem"
  tight: "0.5rem"
  cell: "0.6rem"
  snug: "0.75rem"
  pad: "1rem"
  gutter: "1.25rem"
  wide: "1.5rem"
  break: "2.5rem"
  rail: "13.5rem"
components:
  btn:
    backgroundColor: "transparent"
    textColor: "{colors.ink}"
    typography: "{typography.label}"
    rounded: "{rounded.sharp}"
    padding: "0.42rem 0.85rem"
  btn-hover:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.ink-inv}"
  btn-disabled:
    backgroundColor: "transparent"
    textColor: "{colors.ink-3}"
  btn-primary:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.ink-inv}"
    rounded: "{rounded.sharp}"
    padding: "0.42rem 0.85rem"
  btn-primary-hover:
    backgroundColor: "{colors.ref}"
    textColor: "{colors.ink-inv}"
  btn-danger:
    backgroundColor: "transparent"
    textColor: "{colors.warn}"
    rounded: "{rounded.sharp}"
    padding: "0.42rem 0.85rem"
  btn-danger-hover:
    backgroundColor: "{colors.warn}"
    textColor: "{colors.ink-inv}"
  btn-sm:
    padding: "0.24rem 0.5rem"
    typography: "{typography.label}"
  btn-quiet:
    backgroundColor: "transparent"
    textColor: "{colors.ink}"
    padding: "0.42rem 0.85rem"
  btn-quiet-hover:
    textColor: "{colors.ref}"
  plate:
    backgroundColor: "{colors.plate}"
    textColor: "{colors.ink}"
    rounded: "{rounded.sharp}"
  plate-head:
    backgroundColor: "{colors.plate-sunk}"
    textColor: "{colors.ink}"
    typography: "{typography.plate-title}"
    padding: "0.75rem 1rem 0.6rem"
  plate-body:
    padding: "{spacing.pad}"
  table-header-cell:
    backgroundColor: "{colors.plate-sunk}"
    textColor: "{colors.ink-2}"
    typography: "{typography.label}"
    padding: "0.5rem 0.6rem"
  table-cell:
    textColor: "{colors.ink}"
    typography: "{typography.body}"
    padding: "0.6rem 0.6rem"
  callout-warning:
    backgroundColor: "{colors.warn-field}"
    textColor: "{colors.warn}"
    rounded: "{rounded.sharp}"
    padding: "0.6rem 0.8rem"
  callout-caution:
    backgroundColor: "{colors.caution-field}"
    textColor: "{colors.caution-ink}"
    rounded: "{rounded.sharp}"
    padding: "0.6rem 0.8rem"
  callout-note:
    backgroundColor: "{colors.ref-field}"
    textColor: "{colors.ref}"
    rounded: "{rounded.sharp}"
    padding: "0.6rem 0.8rem"
  input:
    backgroundColor: "{colors.stock}"
    textColor: "{colors.ink}"
    typography: "{typography.body}"
    rounded: "{rounded.sharp}"
    padding: "0.4rem 0.55rem"
    width: "100%"
  input-placeholder:
    textColor: "{colors.ink-3}"
  status-mark-ink:
    textColor: "{colors.ink-2}"
    typography: "{typography.label}"
    rounded: "{rounded.sharp}"
    padding: "0.1rem 0.4rem"
  status-mark-warn:
    backgroundColor: "{colors.warn-field}"
    textColor: "{colors.warn}"
    padding: "0.1rem 0.4rem"
  status-mark-caution:
    backgroundColor: "{colors.caution-field}"
    textColor: "{colors.caution-ink}"
    padding: "0.1rem 0.4rem"
  status-mark-ref:
    backgroundColor: "{colors.ref-field}"
    textColor: "{colors.ref}"
    padding: "0.1rem 0.4rem"
  status-mark-ok:
    backgroundColor: "{colors.ok-field}"
    textColor: "{colors.ok}"
    padding: "0.1rem 0.4rem"
  step-tick:
    backgroundColor: "{colors.plate}"
    textColor: "{colors.ink-3}"
    typography: "{typography.identifier}"
    rounded: "{rounded.sharp}"
    height: "1.35rem"
    width: "1.35rem"
  step-tick-done:
    backgroundColor: "{colors.ink-2}"
    textColor: "{colors.ink-inv}"
  step-tick-here:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.ink-inv}"
  step-tick-blocked:
    backgroundColor: "{colors.warn}"
    textColor: "{colors.ink-inv}"
  step-tick-compact:
    height: "0.95rem"
    width: "0.95rem"
  tab:
    backgroundColor: "transparent"
    textColor: "{colors.ink-2}"
    rounded: "{rounded.sharp}"
    padding: "0.55rem 0.7rem 0.55rem 1.25rem"
  tab-active:
    backgroundColor: "{colors.plate}"
    textColor: "{colors.ink}"
  empty:
    textColor: "{colors.ink-2}"
    rounded: "{rounded.sharp}"
    padding: "2.5rem 1rem"
  jobcard:
    backgroundColor: "{colors.plate}"
    textColor: "{colors.ink}"
    rounded: "{rounded.sharp}"
  jobcard-band:
    backgroundColor: "{colors.plate-sunk}"
    textColor: "{colors.ink}"
    typography: "{typography.label}"
    padding: "0.55rem 1.25rem"
  blank:
    backgroundColor: "transparent"
    textColor: "{colors.ink}"
    rounded: "{rounded.sharp}"
    padding: "0.3rem 0.1rem"
  blank-focus:
    textColor: "{colors.ink}"
  blank-invalid:
    textColor: "{colors.warn}"
  routing:
    backgroundColor: "{colors.plate-sunk}"
    textColor: "{colors.ink-2}"
    rounded: "{rounded.sharp}"
    padding: "2rem 1.25rem 2.25rem"
  slot:
    backgroundColor: "{colors.stock}"
    textColor: "{colors.ink}"
    typography: "{typography.identifier}"
    rounded: "{rounded.sharp}"
    padding: "0.35rem 0.65rem"
  slot-picked:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.ink-inv}"
  volume:
    backgroundColor: "{colors.plate}"
    textColor: "{colors.ink}"
    rounded: "{rounded.sharp}"
    padding: "1.5rem 1.35rem 1.35rem"
---

# Design System: Shop Manual

## Overview

**Creative North Star: "The Workshop Manual"**

This is a factory service manual that happens to be a live console. The whole
interface is a printed document: uncoated pulp stock underneath, blue-black
press ink on top, hairline rules to divide, and standing furniture — plates,
tables, callouts, thumb tabs, registration marks — that behaves identically in
every section. A table in the storeroom is the same table as in work orders,
because a manual is one system, not six themed screens.

The organising idea is that a work order is a job at a numbered step, not a
ticket in a queue. Eleven statuses are printed as a numbered procedure with
preconditions and permitted roles, and every surface reads from that same
procedure. The console therefore never presents a status as a decorated pill or
a lane in a board; it presents it as *where the job stands in the manual*, with
the next lawful step and the person allowed to take it stated in plain words.

Density is high and unapologetic — this is a workstation for people who read
twenty rows at a glance, not a marketing surface. It refuses the generic admin
arrangement outright: no stat tiles, no white cards floating on grey, no sidebar
of decorative glyphs, no rounded chrome. Nothing lifts off the page, because
paper does not lift.

**The manual ships in two volumes.** The staff console is *Volume 1 — Shop
Manual*, a bound book with thumb tabs. The customer facet is *Volume 2 —
Owner's Manual*, the same book set for the person whose car it is: its own
section index numbered from 1, its own reading of the same eleven-status
procedure, and nothing in it that the API withholds from a CUSTOMER principal.
It is not a permission-filtered view of the staff console, and it never renders
a staff screen with rows removed.

**In front of both volumes is the shop's stationery** — the loose card a shop
clips to a windscreen. The public landing page, the account form and the
sign-in receipt are printed on it: a form-header band across the top, blanks
that are ruled lines to write on rather than boxes to type in, a filing band at
the foot, and on the drop-off order a sunk carbon-copy routing margin printing
the whole procedure the job will travel. Same stock, same ink, same hairlines,
different artifact.

**Key Characteristics:**
- Zero corner radius on every surface, control and mark.
- Pure black is absent; ink is a blue-black `#161a1d`.
- One superfamily (Barlow / Barlow Semi Condensed) carries display and caption
  alike; mono is a functional register for identifiers only.
- Colour appears only as functional severity — four declared spots plus a
  six-ink section index.
- Depth is declared once, as a 1px border. There are no ambient shadows.
- Motion is a single crisp 90ms linear step; nothing floats, nothing bounces.
- Browser surfaces (selection, caret, accent, scrollbar, focus ring) are painted
  from the palette rather than left to the OS.

## Colors

A pulp-and-press palette: a warm-to-cool drifting paper ground, one blue-black
ink at three values, two hairline greys, and colour reserved entirely for
severity and section indexing.

### Primary

- **Press Ink** (`ink`, #161a1d): The single voice of the system. Body copy,
  rules that terminate a table head, filled primary buttons, the current step in
  the rail, and the 2px rule under the masthead. It is a blue-black, never
  `#000` — pure black appears nowhere in the build.
- **Secondary Ink** (`ink-2`, #454c51): Secondary prose, table-head labels,
  captions, and — importantly — *completed* steps in the rail, which fill in a
  lighter value of the same ink rather than in a second colour.
- **Tertiary Ink** (`ink-3`, #586065): Placeholders, row indices, sheet numbers,
  spent and inactive rows. Still legible at body size (5.3:1 on stock).
- **Reversed Ink** (`ink-inv`, #f1f0ea): Type set on a solid ink field.

### Secondary — the four functional spots

Severity, never decoration. Each has a saturated ink for the rule and word, and
a pale field for the ground it sits on.

- **Warning Red** (`warn`, #c2321b, field `warn-field`): What blocks, destroys,
  or cannot be undone. Blocked rows, terminal refusal, destructive buttons, the
  role stamp in the masthead, the focus ring, and the caret.
- **Caution Amber** (`caution`, #a86e0d, field `caution-field`): Attention that
  is reversible — waiting on a customer, guest bookings, the synthetic-data
  band. It is a rules-and-marks ink only (3.6:1); body-weight text uses
  **Caution Ink** (`caution-ink`, #7a4f08) instead, which clears 5.9:1.
- **Reference Blue** (`ref`, #1f4e79, field `ref-field`): Reference and active
  informational state. Links, `accent-color`, the note callout, row hover
  tints, the primary button's hover, and open history entries.
- **Satisfied Green** (`ok`, #2f6350, field `ok-field`): A precondition met, a
  service line done, an active facet. Completion of a *record*, never
  completion of a *step*.

### Tertiary — the section index

Six muted inks in the same value range, so the thumb index reads as an index and
not a rainbow. Each section's ink drives its bleed tab, its tab number, and the
3px rule across the top of every plate in that section, exposed to the sheet as
`--section-ink`.

- **Work Orders** (`sec-work-orders`, #1f4e79) · **Schedule**
  (`sec-schedule`, #2f6350) · **Inventory** (`sec-inventory`, #8a5a1e) ·
  **Customers** (`sec-customers`, #6b3a5b) · **Vehicles**
  (`sec-vehicles`, #3f4e63) · **Workers** (`sec-workers`, #5c5344).

### Neutral

- **Pulp Stock** (`stock`, #eaeae5): The page. Never a flat fill — two very
  wide, very low-contrast radial washes drift it warm (`stock-warm`, #efece3) at
  the top-left and cool (`stock-cool`, #e4e8e6) at the bottom-right, fixed to
  the viewport, so the ground reads as printed rather than rendered. It is also
  the ground of every input field.
- **Plate** (`plate`, #f1f0ea): The working surface a table or form is set on;
  one step lighter than the stock it sits against.
- **Sunk Plate** (`plate-sunk`, #e2e2db): Recessed bands — plate headers, sticky
  table heads, inert strips.
- **Hairline** (`rule`, #c7c6be): The ordinary dividing rule, at a genuine 1px.
- **Strong Hairline** (`rule-strong`, #9a9890): The heavier of the two rules —
  field borders, the last row of a table, the rail's spine, scrollbar thumb.
- **Highlighter** (`highlighter`, #f3d98a): `::selection` only. A marker drawn
  across the page instead of the OS blue.

### Named Rules

**The Functional Spot Rule.** Colour is severity or section index, never
decoration. If a hue is on the screen it is answering "how bad is this?" or
"which section am I in?". There is no brand accent, no chart palette, no
tint-for-interest anywhere in the build.

**The Filled-in-Ink Rule.** A completed step fills in press ink (`ink-2`), not
in chroma. Done is not a severity, so it does not get a spot colour;
filled-versus-hollow carries the state and the ink's value carries the rank.

**The Two Rules Rule.** There are exactly two rule weights, `rule` and
`rule-strong`, both at a true 1px, plus two structural exceptions: the 2px ink
rule under the masthead and the work-order head, and the 3px section-ink rule
across the top of a plate.

**The Printed Word Rule.** No tier is carried by colour alone. Callouts print
WARNING / CAUTION / NOTE; status marks print the step's title; blocked rows
print their shortfall. Tone is a second channel, never the only one.

## Typography

**Display Font:** Barlow Semi Condensed (with Barlow, `ui-sans-serif`)
**Body Font:** Barlow (with `ui-sans-serif`, `system-ui`)
**Label/Mono Font:** Azeret Mono (with `ui-monospace`, Cascadia Mono)

**Character:** One superfamily does everything a manual needs — the condensed
cut sets headings, labels, buttons, tabs and stamps; the regular cut sets prose
and table data. Barlow's slightly squared terminals read as drafting lettering
rather than as a product sans, and the condensed cut lets an all-caps label hold
0.1em tracking without eating a column. Azeret Mono is a functional register,
not a costume: it appears only where a string is an *identifier*.

Numerals are tabular globally (`font-variant-numeric: tabular-nums` on `body`)
so money and quantity columns rule up.

### Hierarchy

- **Display** (700, 2.25rem, line-height 1, tracking −0.02em): The section title
  at the top of a sheet — "Work Orders", "Schedule", "Inventory". One per page.
- **Headline** (600, 1.625rem, line-height 1.1): The masthead shop name and
  second-level headings.
- **Title** (600, 1.25rem, line-height 1.1): The open section's name in the
  masthead, the next-step title on a work order, third-level headings.
- **Plate Title** (600, 1rem, tracking 0.01em): The numbered plate heading —
  "5.1 — Booked appointments". Plates are numbered to the manual's own index.
- **Body** (400, 0.875rem, line-height 1.5): Prose and table data. Prose blocks
  are held to a 68–82ch measure depending on the block.
- **Label** (600, 0.75rem, uppercase, tracking 0.1em, `ink-2`): The small-caps
  register — field labels, table heads (0.09em), callout tiers (0.14em, 700),
  buttons (0.045em), masthead descenders (0.16em). Tracking widens with
  authority; the callout tier word is the most widely tracked thing on screen.
- **Identifier** (500, 0.8125em, tracking −0.02em, ligatures off): Azeret Mono,
  strictly for order codes, licence plates, SKUs, part numbers, timestamps,
  slot times, sheet numbers, revision stamps, endpoint paths and step numerals.

### Named Rules

**The Identifier-Only Mono Rule.** Azeret Mono marks a string you would read
back to someone over the phone — a code, a plate, a SKU, a timestamp, an
endpoint. It never sets prose, never sets a label, and never appears for
texture.

**The One Superfamily Rule.** Display and caption are the same family. There is
no second display face, no serif, and no system-UI font stack standing in for a
designed one.

## Layout

The console is a document with a thumb index. A fixed rail of 13.5rem holds the
bleed tabs; the sheet takes the remaining column (`minmax(0, 1fr)`) and is
separated from the rail by a single 1px ink border — the tab that is active runs
*into* the sheet by −1px so the two read as one tabbed page.

Above the shell sit two full-bleed bands: the synthetic-data caution band, and
the masthead — a three-column grid (shop identity / open section / operator
authority) closed by a 2px ink rule. The tab rail is sticky at the top of the
viewport and reserves `100dvh − 8.5rem` so the index is always in reach.

The rail's lower margin is print furniture rather than dead space: registration
marks drawn as geometry, the sheet number ("SHEET 4/9"), and the revision stamp
pinned to the bottom.

Inside a sheet, content is a stack of plates. Content pages open with a head
row (display title plus a right-aligned filter cluster), then plates at 1.25rem
apart. The work-order detail splits into a main column and a fixed 22rem side
column, and its procedure block into two equal columns so the step rail's
precondition panel occupies the space a lone pinned button would waste.

**Spacing rhythm.** A coarse, printer's rhythm rather than a fine scale: 0.25 /
0.5 / 0.6 / 0.75 / 1 / 1.25 / 1.5 / 2.5rem. Table cells are 0.6rem, plate bodies
1rem, page gutters 1.25rem, head-row gaps 2rem, and a section break 2.5rem.

### The stationery (public surfaces)

A loose card, not a bound page. `.jobcard` is an 82rem sheet with a 2px ink
rule across its top, a sunk form-header band (`FORM 1 — VEHICLE DROP-OFF ·
ORDER NO. ____ · date`), and a two-column body: the face
(`minmax(0, 1fr)`) and a 21rem routing margin on `plate-sunk`, divided by a
strong hairline. The account card is the same sheet at `--narrow` (54rem) with
no routing margin, because nothing is being routed yet.

Its form is a six-column grid whose blanks span to fill a row exactly — a
ragged row is an accident, not a paper texture. A blank is a label in the small-
caps register over a 1px bottom rule; focus thickens that rule to 2px ink rather
than boxing the field. A textarea is the one exception and keeps a full border,
because a multi-line answer needs a container.

### Volume 2 — the owner's manual

The console's shell rebuilt for a reader who is not at a desk: the same masthead
block and the same bleed thumb tabs, five sections numbered 0–4, and the sheet
carrying its section ink. Its stamp is reference blue, not warning red — a
customer is not an authority to be careful with.

**Responsive behaviour.** Desktop-only is a product constraint of *Volume 1*,
not of the build: the staff console is built for a minimum of 1280px and
targeted at 1440–1920, with exactly one breakpoint, `max-width: 1180px`, which
collapses the work-order detail's two-column grids to a single column. Wide
tables are not reflowed; they scroll inside their own `overflow-x` wrapper with
a sticky table head, and the board table holds a 72rem floor so the step rail
stays legible across rows.

The stationery and Volume 2 *do* adapt, because their readers are standing next
to a car with a phone. Two breakpoints, both structural:

- `max-width: 62rem` — the routing margin folds under the card's face rather
  than narrowing (a stamp column narrower than its own type is illegible, and
  the procedure is the proof); the tab rail becomes a horizontally scrolling
  thumb strip with each tab's bleed moving from its left edge to its top edge;
  the job detail's step rail moves *above* the budget, because where the car
  stands is the first question.
- `max-width: 46rem` — form rows collapse to one column and every blank spans
  full width; page gutters tighten to 0.9rem.

Nothing is hidden behind a hamburger. Five sections fit across a phone, and a
menu that hides five items exists to look tidy.

### Named Rules

**The Pinned Action Rule.** When a table overflows, the column that must never
be the casualty is the next lawful step. The action column is sticky to the
right edge with its own ground and left rule, and the middle of the table
scrolls beneath it.

**The Print Furniture Rule.** Empty margin gets furniture, not filler.
Registration marks, sheet numbers and revision stamps are geometry that says
where you are in the document; they are never illustration.

**The Ruled Blank Rule.** On the shop's stationery a field is a line to write
on, not a box to type in: transparent ground, one 1px bottom rule, thickening to
2px ink on focus. Inside the bound volumes the console's boxed `.input` still
rules, because a register entry is set into a table and needs its bounds. The
two never mix on one surface.

**The Two Volumes Rule.** A screen belongs to exactly one volume and says which.
The customer surface is never the staff surface with rows removed, and the staff
surface never renders a customer's own decision — the API has no endpoint for a
worker to approve a budget, and neither does this build.

## Elevation & Depth

**There are no shadows in this system.** Elevation is declared once, as a
border: a plate is `1px solid` hairline with a `plate` ground, and that is the
entire depth vocabulary. Nothing floats, nothing hovers, no panel is lifted off
the page, and there is no ambient or resting shadow anywhere in the build.

Hierarchy is carried instead by three flat devices: ground value (`stock` →
`plate` → `plate-sunk`), rule weight (1px hairline, 2px ink for a structural
close, 3px section ink across a plate top), and ink value (`ink` → `ink-2` →
`ink-3`).

### Shadow Vocabulary

- **Scrolled overlap cue** (`box-shadow: -6px 0 7px -5px rgb(22 26 29 / 28%)`):
  The single shadow in the system, on the pinned action column of the work-order
  board, and only while `is-scrolled` is true. It is an affordance, not depth —
  it says columns are passing beneath this one. At 1440px, where nothing is
  hidden, it is absent, because a permanent shadow there would be a lie.

### Named Rules

**The Border-Is-The-Elevation Rule.** If a surface needs to separate from what
is under it, it gets a 1px rule and a ground value. It does not get a shadow.

**The Earned Shadow Rule.** The only shadow permitted is one that reports a
fact the reader cannot otherwise see, and it disappears the moment that fact
stops being true.

## Shapes

Zero radius, everywhere, without exception — `--radius: 0` is declared as a
token and the build carries no `border-radius` rule at all. A printed page has
no rounded corners, so neither do buttons, inputs, plates, callouts, status
marks, step ticks, tabs or selects.

The recurring silhouette is **the ruled rectangle**: a 1px box drawn in
`currentColor` around something that needs to be read as a discrete unit. It
appears at every scale — the callout, the status mark, the role badge, the
licence plate, the row-type flag, the step tick, the masthead's role stamp. The
mark inherits its own colour into its border, so one shape carries all five
tones without a new rule.

Dashed rules mark *absence or inertness* rather than emphasis: the empty state's
1px dashed border, the inert (unauthorised) action pill, and the collapsed
history snapshot divider.

**Icons** are authored in-repo rather than pulled from a library, so every mark
shares one geometry: a 24px box, 1.75 stroke, `stroke-linecap: square`,
`stroke-linejoin: miter`, `fill: none`, drawn in `currentColor` and rendered at
16px by default. That is the weight of a technical drawing, not a UI kit.

### Named Rules

**The Zero Radius Rule.** No corner in this system is rounded. Not a button, not
a badge, not a select, not a focus ring.

**The Mitred Icon Rule.** Icons are square-capped and mitre-joined at 1.75
stroke in a 24px box. No rounded caps, no filled glyphs, no two-tone icons, and
no icon font.

## Components

### Buttons

Filled, bounded, and unmistakably actionable. Condensed uppercase at 0.045em, a
1px ink border, zero radius.

- **Shape:** Square (0 radius), 1px border, padding 0.42rem 0.85rem.
- **Default:** Transparent ground, ink border and ink text. On hover it inverts
  to a solid ink field — the button fills in, the way a stamp lands.
- **Primary:** Solid ink field with reversed type at rest; hovers to reference
  blue (ground and border together). Used for the next lawful step, one per row
  and one per plate.
- **Danger:** Warning-red border and text; hovers to a solid warning field.
- **Small** (`btn--sm`): 0.75rem type, padding 0.24rem 0.5rem — the in-row
  action size on the board.
- **Quiet:** Transparent border, underlined text; hovers to reference blue with
  no ground change. For tertiary escapes only.
- **Disabled:** Strong-hairline border, tertiary ink, `not-allowed` cursor. No
  opacity fade.
- **Transition:** `background-color 90ms linear, color 90ms linear`.

### Inputs / Fields

- **Style:** Stock ground (deliberately darker than the plate it sits on, so a
  field reads as a cut-out), 1px strong-hairline border, zero radius, padding
  0.4rem 0.55rem, full width.
- **Label:** The small-caps label register, block, 0.25rem above the control.
- **Focus:** The border darkens to full ink, and the global focus ring (2px
  warning red at 2px offset) draws a hand-ruled box around the field being
  filled.
- **Placeholder:** Tertiary ink.

### Plates (cards / containers)

The plate is the only container in the system; there is no card.

- **Corner Style:** Square (0 radius).
- **Background:** `plate`, on the drifting stock.
- **Border:** 1px hairline all round, plus a 3px top rule in the open section's
  ink (`--section-ink`, falling back to ink).
- **Head:** A sunk band (`plate-sunk`) closed by a 1px ink rule, carrying the
  numbered plate title on the left and a label-register meta count on the right,
  baseline-aligned.
- **Shadow Strategy:** None. See Elevation & Depth.
- **Internal Padding:** 1rem body; 0.75rem 1rem 0.6rem head.

### Manual Table

The recurring data surface, identical in every section.

- **Head:** Sticky at the top of the scroll container, sunk ground, small-caps
  labels in secondary ink, closed by a 1px ink rule.
- **Rows:** 0.6rem cells, top-aligned, divided by 1px hairlines; the last row
  closes on a strong hairline. **No zebra striping** — a manual rules its rows,
  it does not tint them.
- **Hover:** A 5% reference-blue wash over the row.
- **Numerics:** A right-aligned tabular class; a shrink class holds identifier
  columns to their content.
- **Overflow:** Wrapped in a horizontal scroller, never reflowed.

### Callouts

Three tiers and only three, each a full 1px box in its own ink over its pale
field — never a fat coloured left border.

- **WARNING** (warn on warn-field): blocks, destroys, or cannot be undone.
- **CAUTION** (caution-ink on caution-field): needs attention, reversible.
- **NOTE** (ref on ref-field): reference.
- **Layout:** A two-column grid — the tier word (700, 0.14em tracking,
  uppercase, condensed) beside the body, which stays in full ink for legibility
  while the border and tier word carry the tone.

### Status Marks

A work order's status set as a printed mark, not a rounded pill: condensed
uppercase, 0.07em tracking, 1px `currentColor` box, padding 0.1rem 0.4rem, zero
radius. Five tones map the eleven statuses — **ink** for at-rest bookends
(received, delivered), **caution** for waiting on someone, **ref** for work in
progress, **ok** for satisfied, **warn** for terminal refusal. The word is
always printed alongside the tone.

The same ruled-rectangle mark is reused, retoned, for worker roles, record
state, appointment status, inventory movement type and row-type flags.

### Navigation

**Bleed tabs.** A vertical thumb index down the left edge, numbered to the
manual's own section index (4–9). Each tab carries a solid block of its
section's ink bleeding off the left edge — 0.5rem at 40% opacity at rest,
0.75 on hover, 0.7rem at full opacity when active. There are **no category
glyphs**; the number carries real information the icon did not.

- **Default:** Translucent plate ground, secondary ink, hairline top and bottom
  rules only.
- **Hover:** Solid plate ground, full ink, tab number takes the section ink.
- **Active:** Plate ground, ink border, and a −1px right margin so it runs into
  the sheet with no rule between them.
- **Content:** A mono section number, the section title (condensed 600), and a
  one-line subtitle in tertiary ink.
- **Gating:** Tabs are printed only for roles the API would actually serve; the
  section list and the route guards read the same table.

**Sub-tabs** (inventory) are ruled folder tabs on a shared 1px ink baseline —
bordered on three sides, `-1px` bottom margin so the active one breaks the
baseline. Never pills.

### The Step Rail — signature component

The console's signature: a work order's whole life as a numbered procedure, the
way a service manual prints one. Ten main-line steps plus one terminal branch,
transcribed from the backend's own status enum, `@PreAuthorize` roles and
documented preconditions.

- **Ticks:** 1.35rem squares, 1px strong-hairline border on a plate ground with
  a mono numeral, joined by a 0.55rem hairline spine.
- **Done:** Filled in `ink-2` with reversed type — press ink, not chroma.
- **Here:** Filled in full ink, plus a 1px outline at 2px offset — the
  inspector's stamp, a ruled box drawn round the current step.
- **Blocked:** The current tick and its outline restate in warning red.
- **Refused:** A branch tick marked "R" in warning red, with its connecting
  spine in warning red; the main line truncates at step 5.
- **Full mode:** A precondition panel below the rail, ruled off by a hairline
  and held to 68ch. Hovering or focusing any tick swaps the panel to that step's
  title, its precondition, the roles permitted to perform it, and the endpoint
  in mono. It is `role="status"`, so the swap is announced.
- **Compact mode:** 0.95rem ticks on a 0.22rem spine, no panel, and numerals
  hidden on every tick but the current one — at eighteen rows the numerals are
  180 glyphs nobody reads, so the eye gets filled-bar length instead. Every tick
  keeps its full `aria-label`.

### Empty and Inert States

- **Empty:** A 1px *dashed* strong-hairline box, 2.5rem 1rem, centred, condensed
  title in full ink over secondary-ink body — plus, where relevant, a single
  action to clear the filters that emptied it.
- **Inert action:** Where a role may not perform a step, the button is replaced
  by a dashed-border pill in tertiary ink at the same size — the action's place
  in the layout is held, so the row does not reflow between operators.

### Browser Surfaces

The parts nobody draws still carry the design.

- **Selection:** Highlighter yellow ground with ink text — a marker drawn across
  the page, not the OS blue.
- **Caret:** Warning red. **Accent-color:** Reference blue.
- **Scrollbars:** Thin, strong-hairline thumb on a transparent track, with a
  3px stock border insetting the thumb; darkens to tertiary ink on hover.
- **Focus ring:** 2px solid warning red at 2px offset, globally, on
  `:focus-visible`.
- **Links:** Reference blue with a 1px underline at 0.18em offset, thickening to
  2px on hover — never a colour change alone.

### Stationery components

- **Job card** (`.jobcard`): the public sheet. `plate` ground, 1px strong
  hairline all round, 2px ink rule across the top. `--narrow` at 54rem for a
  card with no routing margin.
- **Form-header band** (`.jobcard__band`): `plate-sunk`, label register, holding
  the form's name, a real blank rule for the number a clerk fills in, and the
  date as an identifier.
- **Blank** (`.blank`): label over a 1px rule. Hover darkens the rule to
  tertiary ink; focus replaces it with 2px ink and compensates the padding so
  nothing shifts; invalid rules in warning red. Help and error text sit under
  the rule at label size, never floating.
- **Routing margin** (`.routing`): the carbon-copy strip. `plate-sunk`, a
  numbered stamp per step, the two steps that wait on the customer marked in
  caution, and the step the current card *is* stamped through in solid ink.
- **Slot** (`.slot`): a bookable time, as a bounded identifier on stock;
  picked inverts to solid ink. Never a pill.
- **Volume** (`.volume`): the facet picker's card — a coloured spine in a
  section ink beside a printed face. The two are given identical weight; there
  is no recommended answer, because the shop does not know which hat someone is
  wearing when they sit down.

### Motion

Paper-native. Every transition in the build is `90ms linear` on
`background-color`, `border-color`, `color`, or a `transform` that is a rotation
(the history disclosure mark turning 90°). There are no easing curves, no
translate-on-hover, no scale, no fade-in, no keyframe animation anywhere.
`prefers-reduced-motion: reduce` is honoured globally, collapsing every
transition and animation to 0.01ms.

**The Stamp Rule.** State changes are a crisp mechanical step, like a stamp
landing. Nothing eases, nothing floats, nothing bounces.

## Do's and Don'ts

### Do:

- **Do** set every new surface on a plate: `plate` ground, 1px hairline, 3px
  section-ink top rule, sunk head band closed by an ink rule, numbered title.
- **Do** number things to the manual's own index — sections 4–9, plates 5.1,
  6.2, steps 1–10 — so a reference is citable.
- **Do** carry state on two channels: the printed word plus the tone. A status
  prints its title; a callout prints WARNING / CAUTION / NOTE.
- **Do** use `ink-2` for anything completed or spent, and reserve the four spot
  colours for live severity.
- **Do** keep the mono face for identifiers only — codes, plates, SKUs,
  timestamps, endpoints, part numbers.
- **Do** hold a 1px rule (`rule`) as the default divider and `rule-strong` for
  a closing or bounding rule.
- **Do** pin the action column when a table overflows, and show the overlap cue
  only while the table is genuinely scrolled.
- **Do** print a control only when the signed-in role could actually perform it;
  where it cannot, hold the slot with a dashed inert marker so the layout does
  not shift between operators.
- **Do** keep transitions at 90ms linear on colour properties.
- **Do** theme the browser's own surfaces — selection, caret, accent, scrollbar,
  focus ring — from these tokens.

### Don't:

- **Don't** round a corner. Anywhere. `--radius` is 0 and there is no
  `border-radius` in the build.
- **Don't** add a shadow. Elevation is the border. The one exception is the
  scrolled-overlap cue on a pinned column, and it must vanish when nothing is
  hidden.
- **Don't** use `#000` or a neutral grey ramp; ink is `#161a1d` and its two
  lighter values.
- **Don't** introduce a colour for decoration, brand warmth, or variety. If a
  hue is not severity or a section index, it does not belong.
- **Don't** fill a completed step in green. Completion is press ink; green is
  reserved for a satisfied precondition or an active record.
- **Don't** put category glyphs in the section navigation. The tab number
  carries the information.
- **Don't** set body copy, labels or headings in Azeret Mono, and don't add a
  second display face.
- **Don't** zebra-stripe a table or tint alternating rows; rule them.
- **Don't** use a fat coloured left border for a callout — the tier gets a full
  1px box and a printed tier word.
- **Don't** animate with easing, translation, scale or fade. No floating panels,
  no lift-on-hover, no bounce.
- **Don't** design a mobile layout for this console. Desktop-only (min 1280,
  target 1440–1920) is a product constraint; the single 1180px breakpoint exists
  to collapse two-column grids, not to begin a responsive ladder.
- **Don't** use body-weight text in `caution` (#a86e0d) — it clears only 3.6:1.
  Use `caution-ink` (#7a4f08) for anything below large-text size.
