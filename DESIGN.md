---
name: LearningProgram
description: A Chinese-first learning workspace that combines study, practice, progress, and accountability.
colors:
  primary: "#409eff"
  primary-light-3: "#79bbff"
  primary-light-5: "#a0cfff"
  primary-light-7: "#c6e2ff"
  primary-light-9: "#ecf5ff"
  primary-dark-2: "#337ecc"
  neutral-bg-page: "#f2f3f5"
  neutral-bg: "#ffffff"
  neutral-fill: "#f0f2f5"
  neutral-fill-light: "#f5f7fa"
  neutral-fill-lighter: "#fafafa"
  text-primary: "#303133"
  text-regular: "#606266"
  text-secondary: "#606266"
  text-placeholder: "#a8abb2"
  border: "#dcdfe6"
  border-light: "#e4e7ed"
  border-lighter: "#ebeef5"
  success: "#67c23a"
  warning: "#e6a23c"
  danger: "#f56c6c"
  info: "#909399"
  white: "#ffffff"
  deck-ink: "#071624"
  deck-ink-soft: "#0b2036"
  deck-ink-deep: "#061323"
  deck-text: "#f7fbff"
  deck-accent: "#8ec5ff"
typography:
  hero:
    fontFamily: "Inter, PingFang SC, Microsoft YaHei, sans-serif"
    fontSize: "clamp(34px, 4.5vw, 56px)"
    fontWeight: 700
    lineHeight: 1.08
    letterSpacing: "-0.035em"
  display:
    fontFamily: "Inter, PingFang SC, Microsoft YaHei, sans-serif"
    fontSize: "26px"
    fontWeight: 700
    lineHeight: 1.3
  headline:
    fontFamily: "Inter, PingFang SC, Microsoft YaHei, sans-serif"
    fontSize: "24px"
    fontWeight: 700
    lineHeight: 1.3
  title:
    fontFamily: "Inter, PingFang SC, Microsoft YaHei, sans-serif"
    fontSize: "20px"
    fontWeight: 600
    lineHeight: 1.4
  card-title:
    fontFamily: "Inter, PingFang SC, Microsoft YaHei, sans-serif"
    fontSize: "17px"
    fontWeight: 600
    lineHeight: 1.4
  body:
    fontFamily: "Inter, PingFang SC, Microsoft YaHei, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: 1.5
  meta:
    fontFamily: "Inter, PingFang SC, Microsoft YaHei, sans-serif"
    fontSize: "12px"
    fontWeight: 400
    lineHeight: 1.4
  label:
    fontFamily: "Inter, PingFang SC, Microsoft YaHei, sans-serif"
    fontSize: "11px"
    fontWeight: 700
    lineHeight: 1.3
    letterSpacing: "0.13em"
rounded:
  xs: "7px"
  sm: "8px"
  md: "10px"
  lg: "14px"
  xl: "18px"
  xxl: "24px"
  pill: "999px"
spacing:
  xs: "6px"
  sm: "10px"
  md: "14px"
  lg: "18px"
  xl: "24px"
  section: "42px"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.white}"
    rounded: "{rounded.sm}"
    padding: "9px 18px"
  button-ghost:
    backgroundColor: "{colors.neutral-bg}"
    textColor: "{colors.text-regular}"
    rounded: "{rounded.sm}"
    padding: "8px 16px"
  button-quiet:
    backgroundColor: "transparent"
    textColor: "{colors.text-secondary}"
    rounded: "{rounded.sm}"
    padding: "9px 14px"
  nav-item:
    backgroundColor: "transparent"
    textColor: "{colors.text-secondary}"
    rounded: "{rounded.md}"
    padding: "12px 14px"
    height: "45px"
  card:
    backgroundColor: "{colors.neutral-bg}"
    textColor: "{colors.text-primary}"
    rounded: "{rounded.lg}"
    padding: "24px"
  input:
    backgroundColor: "{colors.neutral-bg}"
    textColor: "{colors.text-primary}"
    rounded: "{rounded.sm}"
    height: "32px"
  chip:
    backgroundColor: "{colors.neutral-fill-light}"
    textColor: "{colors.text-secondary}"
    rounded: "{rounded.pill}"
    padding: "7px 11px"
  status-chip:
    backgroundColor: "{colors.primary-light-9}"
    textColor: "{colors.primary}"
    rounded: "{rounded.pill}"
    padding: "2px 8px"
  progress-track:
    backgroundColor: "{colors.neutral-fill}"
    height: "6px"
    rounded: "{rounded.pill}"
---

## Overview

**Creative North Star: "The Practical Learning Space"**

LearningProgram is a calm, utility-first workspace built on Element Plus primitives. A cool neutral page canvas, white work surfaces, a single blue action color, compact labels, and generous content gutters keep a broad learning product understandable. The interface should feel dependable and encouraging rather than competitive or promotional.

The system relies on tonal separation more than heavy decoration: the page background distinguishes the shell from white cards, light borders define structure, and blue is reserved for navigation, primary actions, progress emphasis, and links. One surface is allowed to break that restraint: the dark navy mission deck that opens the home page. It is the product's signature, and everything beneath it stays quiet so it reads as the single loud moment.

**Key Characteristics:**
- Chinese-first system typography; no webfont is loaded.
- Cool neutral page canvas with white work surfaces, in both light and dark themes.
- Blue accent for action, active navigation, and learning emphasis; a dark ink deck as the single expressive surface.
- Compact rounded geometry with light borders and restrained shadows.
- Dense data typography with tabular numerals for points, ranks, counts, and dates.
- Responsive application shell: expandable desktop rail and off-canvas mobile navigation.

## Themes

Two themes ship: light and dark. The user picks light, dark, or "follow system" in Settings; the choice is stored under `learning-theme-mode` and applied as a `.dark` class on `<html>`. Dark values come from Element Plus's dark CSS variables (`element-plus/theme-chalk/dark/css-vars.css`), which are imported at startup.

Every component should consume semantic `var(--el-*)` variables (text, fill, border, color steps) so both themes stay coherent. Hard-coded hex values are acceptable only for surfaces that are intentionally identical in both themes: the mission deck's ink palette and its light text.

## Colors

Primary action uses Element Plus blue (`#409eff`), with its light steps for active navigation backgrounds, soft emphasis, and tinted cards. Semantic green, amber, red, and gray are reserved for status:

- **Primary:** actions, active navigation, links, progress fill, the signed-in user's own leaderboard row.
- **Danger:** overdue tasks, destructive actions, the unread-notification dot.
- **Warning:** tasks due today, points, second rank, late completion.
- **Success:** completed task items, check-in streaks, resource-type items.
- **Info:** member role badges, default avatars.
- **Placeholder gray:** no-due-date tasks, metadata, quiet labels, third rank borders.

The status vocabulary is consistent across the product: a small colored dot or marker plus a soft `-light-9` tinted chip carrying the semantic color as text.

The mission deck uses a fixed ink palette layered with the primary color: `#071624`, `#0b2036`, and `#061323` in a 128deg gradient, text `#f7fbff`, muted text at 68% white-blue, accent `#8ec5ff` for the highlighted word in the hero title.

## Typography

The global stack is `Inter, "PingFang SC", "Microsoft YaHei", sans-serif`. No webfont is bundled or linked, so Chinese copy renders in the user's system CJK font and Latin text uses Inter only when it is installed locally. Design for that: do not rely on Inter-specific metrics.

The working scale is denser than a typical marketing page:

- **Hero (home deck):** `clamp(34px, 4.5vw, 56px)`, 700, line height 1.08, letter spacing -0.035em.
- **Page title:** 24px, 700 (26px in the admin area, 22px to 30px for home section titles).
- **Card title:** 16px to 17px, 600; panel titles 20px.
- **Body:** 14px, 1.5 line height; descriptive paragraphs 13px at 1.65 to 1.8.
- **Metadata:** 11px to 12px in placeholder gray.
- **Control label:** 13px, weight 600 on buttons.
- **Eyebrow:** 11px, 700, 0.13em to 0.16em tracking, placeholder gray or primary blue.

Numbers that the user compares (points, completion percent, rank, counts, dates) use `font-variant-numeric: tabular-nums`. Large headings use slight negative tracking (-0.02em to -0.04em). Chinese copy should remain natural and should not be forced into uppercase styling.

## Layout

The primary application shell is a two-region layout: a 72px collapsed or 248px expanded sidebar beside a flexible content column. The top bar is approximately 100px tall with 5% horizontal padding and drops to 68px on mobile. Main content uses a max width of 1180px, centered, typically with 42px top and 60px bottom padding; the home page uses 36px top and 64px bottom.

The home page is a 40px-gap vertical stack. Below the deck, signed-in users get a two-column dashboard: a fluid main column and a 352px right rail (18px gap) holding a sticky check-in card (stuck 22px from the top). Sections inside the column are separated by 42px; the rail drops below the main column at 1080px.

Common content grids use 18px gaps. Cards and panels use 18px to 24px internal spacing. Breakpoints in use: 1080px (home rail stacks), 980px (deck becomes one column), 860px (group lanes reflow), 760px (sidebar becomes an off-canvas drawer, auth hero removed, top bar compresses), 700px (admin panels compress to 16px padding and multi-column content stacks), 620px, and 430px.

The authentication shell is separate from the app shell: a full-height split with a cover image on the left (`/background.webp`, object-fit cover) and a 400px white card on the right, with the product title overlaid bottom-left in white. Below 760px the image and overlay disappear and the card owns the screen.

## Elevation & Depth

The system is primarily tonal and border-led. Use `var(--el-box-shadow-lighter)` for hover lift on cards and `var(--el-box-shadow-light)` for the mobile drawer and brand mark. Avoid stacking multiple shadows on adjacent cards.

The mission deck is the exception and carries the only heavy shadow in the product (`0 34px 80px -48px` in ink at 90%). Its inner console panel is a 55% dark translucent surface with a hairline border in a blue-white line color.

## Shapes

- **Controls:** 7px to 9px radius (buttons, inputs, toggles, small action buttons).
- **Navigation:** 10px radius, 45px tall items, 7px gaps.
- **Cards and panels:** 12px to 14px (group cards, task cards, chat area, leaderboard board, admin panels).
- **Large surfaces:** 16px to 18px (group lanes, to-do board, home step rail, deck console).
- **Signature surface:** 24px for the mission deck.
- **Pills:** `999px` for chips, status badges, tabs, and progress tracks.

Borders are 1px and use Element Plus light or lighter border variables. Circular geometry is reserved for avatars, rank badges, progress rings, numbered section discs, and decorative rings.

## Motion

Motion is short and functional. Control and navigation transitions run at `.2s ease`. Page changes use a 200ms fade with a 10px rise in and a 6px lift out. The sidebar drawer slides in at `.25s`.

The home deck is the one place with expressive motion: copy and console rise 14px over 600ms with a `cubic-bezier(.2, .8, .2, 1)` curve (console staggered 80ms), the kicker dot pulses on a 2.4s loop, and progress bars grow from the left at 750ms. Skeletons shimmer at 1.4s.

Every animated surface pairs with a `prefers-reduced-motion: reduce` block that turns animations off and transitions near-instant. New motion must ship with the same block.

## Components

### Buttons
- **Shape:** Compact rounded controls (8px to 9px).
- **Primary:** Element Plus primary blue with white text, 13px weight-600 label, usually 9px 18px padding; on the deck it reaches a 42px minimum height and carries a soft colored shadow.
- **Ghost:** White or themed surface with a 1px neutral border and regular text; border and text turn primary blue on hover.
- **Danger:** A tinted `danger-light-9` fill with danger text and border, turning solid danger on hover for destructive confirmations.
- **Text:** Primary blue, no padding beyond 4px vertical, darkening to `primary-dark-2` on hover; used for "view all" links and row actions.
- **Focus:** All custom buttons show a 2px solid primary outline, offset 2px. Never remove it.

### Chips, Badges, and Tabs
- **Metadata chip:** Soft fill from `var(--el-fill-color-light)`, secondary text, pill-shaped, 12px.
- **Status chip:** Semantic color as text on the matching `-light-9` tint, pill-shaped, 10px to 12px, weight 600. Examples: owner role (primary), member role (info), resource item (success), due today (warning), overdue (danger).
- **Tinted outline chip:** 1px border at 22% of the status color on a 10% tint of the same color; used for to-do state labels.
- **Tabs as pills:** 999px radius, 1px border, surface background, 13px label. Hover gets a `primary-light-5` border and primary text; the active tab is solid primary with white text (leaderboard scope and metric tabs).

### Cards / Containers
- **Corner Style:** 14px for panels, 16px to 18px for home surfaces, 24px for the mission deck.
- **Background:** White `var(--el-bg-color)` on the page background `var(--el-bg-color-page)`.
- **Shadow Strategy:** Flat by default; `var(--el-box-shadow-lighter)` appears on hover together with a `primary-light-5` border and a 1px lift.
- **Border:** 1px `var(--el-border-color-light)` for panels, check-in/stat surfaces, and group lanes.
- **Internal Padding:** 18px to 26px, with 24px as the common default.

### Section Header
Home sections open with a three-part header: an eyebrow label with a 22px numbered disc (primary text, 1px border, circle) on the left and a right-aligned total count in 11px placeholder gray, then a title row pairing a 22px to 30px title with a quiet note or text link. Reuse this rhythm when adding a home section.

### Inputs / Fields
- **Style:** Element Plus inputs with white or themed surface backgrounds, neutral borders, and 8px control radius.
- **Focus:** Use Element Plus primary border and focus treatment; never remove the focus indicator.
- **Error / Disabled:** Use semantic danger and disabled variables from Element Plus rather than custom colors.

### Navigation
- **Style:** Icon-led 45px rail items with 10px radius, 7px gaps, and secondary text by default. The brand mark is a 34px primary-blue rounded square holding the character "学".
- **Default / Hover / Active:** Transparent default; primary blue text with `var(--el-color-primary-light-9)` background on hover and active; active labels use weight 600.
- **Top bar:** Greeting title on the left (24px), then notification bell (with a 6px danger dot when unread), avatar with username, and a quiet logout button. Guests see a single primary login button instead.
- **Mobile:** The rail becomes a 264px modal off-canvas drawer with a dimming overlay, focus trapping, Escape to close, and closes after navigation.

### Daily Word
- **Style:** A compact learning cue inside the expanded sidebar, using `var(--el-fill-color-lighter)`, 1px light border, 13px radius, small metadata label, and a 19px word.
- **Behavior:** It collapses with the sidebar and supports loading and unavailable states without changing the shell layout.

### Mission Deck (home hero)
- **Surface:** 24px radius, 128deg ink gradient with two primary-colored radial washes, a 44px grid texture masked toward the right, and a thin concentric glow ring top-right. It is ink-dark in both themes.
- **Copy column:** Pulsing dot plus date and section kicker (11px, 0.16em), two-line hero title with one phrase in accent blue, a 14px description at 1.8 line height, then a primary and a ghost action. The ghost button is translucent white on ink.
- **Console panel (signed in):** 18px radius translucent panel showing a completion ring (masked ring track, 30px to 42px percentage), a three-row legend for completed, ongoing, and overdue counts (overdue turns danger when above zero), and a two-cell footer for group and to-do totals.
- **Artwork (guest):** Three concentric rings (one dashed) around a primary-tinted square holding "学", with three numbered nodes. Hidden below 620px.
- **Rule:** This is the only surface allowed gradients, glows, and grid texture. Do not reproduce it elsewhere.

### Group Lane
A full-width row card (16px radius, 1px light border) that is itself a button. Grid: 32px primary-tinted number disc, name with optional role badge and metadata line (members, tasks, created date), a progress block (6px track, 10px label, 15px percentage), two metric stacks (18px value over 10px label), and a 50px chevron column with a hairline left edge. Overdue metrics turn danger. At 860px progress and metrics wrap to their own row.

### To-do Board (home)
An 18px radius white container holding stacked buckets: overdue (danger), due today (warning), and no due date (placeholder gray). Each bucket has a 10.5px letter-spaced header with a 6px colored dot and a count pill. Rows sit on a vertical 1px timeline: time column, 8px node dot with a 4px surface ring, title with group name and task name, a tinted state chip, and a chevron. Row hover uses `var(--el-fill-color-lighter)`.

### Step Rail and Guest Cards
Guest sections use a three-column step rail (18px radius, 24px padding, columns split by lighter borders) with large outlined step numerals (38px to 56px, transparent fill, 1px `primary-light-5` stroke). Course cards are 16px radius with a faint decorative ring in the corner. The login guide card is an 18px radius `primary-light-9` panel with a `primary-light-8` radial highlight.

### Leaderboard
Pill tabs for scope (global or group) and metric, a native select for the group, and a "my rank" card: 14px radius, `primary-light-9` fill, `primary-light-7` border, circular icon in surface white. The board is a 14px radius card with a header row on `var(--el-fill-color-lighter)` and 1px lighter dividers. Rows are an 80px rank column, a flexible user column, and a right-aligned 160px value column. The signed-in user's row takes the `primary-light-9` fill. Ranks 1 to 3 use 26px circular badges: warning-dark-2 on warning-light-9, regular text on `fill-color-dark-1`, danger-dark-2 on danger-light-9.

### Task Card and Progress Matrix (group)
Task posts in group chat are 12px radius cards on a `warning-light-9` tint with a 4px warning left edge. Inside: a type badge, 15px title, 13px description, due chip (overdue in danger, late in warning), then item rows on 9px tinted tiles with a type pill, completion state, and a pill-shaped mini action. Progress tracks are 6px pills. The member progress matrix is a modal with a 14px panel, legend chips, and per-member percent rows.

### Check-in Card
14px radius card with a 20px title, a 7-day calendar grid (34px cells), and two tinted metric tiles: points in warning, streak in success, each a 72% blend of the `-light-9` tint into the page surface.

### Empty States and Loading
- **Empty panel:** 14px to 16px radius, 1px dashed border, `var(--el-fill-color-lighter)` fill, centered 40px placeholder-gray icon, 16px heading, 13px description, optional action. Not every empty state uses the icon; table and list panels skip it.
- **Skeleton:** Shapes filled with a 200% wide gradient of `fill-color-light` to `fill-color-lighter`, shimmering horizontally at 1.4s. Match the layout of the content they replace.

### Admin Panels
Admin pages share a skeleton: 1180px container, 26px page title, a primary-blue eyebrow at 0.14em tracking, and 24px panels with 14px radius. Panel titles carry a primary-colored icon beside a 17px heading.

## Do's and Don'ts

### Do:
- **Do** use Element Plus semantic variables for colors so light and dark themes stay synchronized.
- **Do** preserve the blue primary accent as the single dominant action color and the semantic palette strictly for status.
- **Do** use white surfaces, light borders, and tonal layering to establish hierarchy.
- **Do** keep page content within the established 1180px maximum and 5% responsive gutters.
- **Do** use tabular numerals for any number the user compares.
- **Do** provide clear loading, empty, error, disabled, and authenticated states, and pair every animation with a reduced-motion rule.
- **Do** keep interactive controls keyboard accessible with the 2px primary focus outline.

### Don't:
- **Don't** introduce a second brand accent or a saturated decorative palette without an explicit product decision.
- **Don't** replicate the mission deck's gradients, grid texture, or glow on any other surface.
- **Don't** use glassmorphism or heavy shadows on ordinary cards.
- **Don't** hard-code light-mode colors where an Element Plus semantic variable exists.
- **Don't** hide important navigation labels or status information on mobile.
- **Don't** invent product claims, metrics, testimonials, pricing, or certifications in UI copy.
