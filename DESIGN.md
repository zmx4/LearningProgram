---
name: LearningProgram
description: A Chinese-first learning workspace that combines study, practice, progress, and accountability.
colors:
  primary: "#409eff"
  primary-light-3: "#79bbff"
  primary-light-5: "#a0cfff"
  primary-light-9: "#ecf5ff"
  primary-dark-2: "#337ecc"
  neutral-bg-page: "#f2f3f5"
  neutral-bg: "#ffffff"
  neutral-fill: "#f5f7fa"
  neutral-fill-lighter: "#fafafa"
  text-primary: "#303133"
  text-secondary: "#606266"
  text-placeholder: "#a8abb2"
  border: "#dcdfe6"
  border-light: "#e4e7ed"
  success: "#67c23a"
  warning: "#e6a23c"
  danger: "#f56c6c"
  info: "#909399"
  white: "#ffffff"
typography:
  display:
    fontFamily: "Inter, PingFang SC, Microsoft YaHei, sans-serif"
    fontSize: "28px"
    fontWeight: 700
    lineHeight: 1.25
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
  body:
    fontFamily: "Inter, PingFang SC, Microsoft YaHei, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: 1.5
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
  pill: "20px"
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
    padding: "10px 16px"
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
    backgroundColor: "{colors.neutral-fill}"
    textColor: "{colors.text-secondary}"
    rounded: "{rounded.pill}"
    padding: "7px 11px"
---

## Overview

**Creative North Star: "The Practical Learning Space"**

LearningProgram's incumbent visual language is a calm, utility-first workspace built on Element Plus primitives. It uses a cool neutral canvas, white work surfaces, a single blue action color, compact labels, and generous content gutters to keep a broad learning product understandable. The interface should feel dependable and encouraging rather than competitive or promotional.

The visual system relies on tonal separation more than heavy decoration: a page background distinguishes the application shell from white cards, light borders define structure, and blue is reserved for navigation, primary actions, progress emphasis, and links. Custom screens should extend these relationships instead of introducing a second palette or a competing visual metaphor.

**Key Characteristics:**
- Chinese-first system typography with an Inter fallback for Latin text.
- Cool neutral page canvas with white elevated surfaces.
- Blue accent for action, active navigation, and learning emphasis.
- Compact rounded geometry with light borders and restrained shadows.
- Responsive application shell: expandable desktop rail and off-canvas mobile navigation.

## Colors

Primary action uses Element Plus blue (`#409eff`), with its light steps for active navigation backgrounds and soft emphasis. Semantic green, amber, red, and gray are reserved for success, warning, danger, and informational states. Neutral backgrounds and borders do most of the layout work.

Dark mode is provided by Element Plus's dark CSS variables. New components should consume semantic `var(--el-*)` variables rather than hard-coding light-mode values so both themes remain coherent.

## Typography

The global stack is `Inter, "PingFang SC", "Microsoft YaHei", sans-serif`. Body text is compact and readable at 14px with a 1.5 line height. Page titles use 24px, section titles use 20px, and small uppercase eyebrow or category labels use 11px with wide tracking. Chinese copy should remain natural and should not be forced into uppercase styling.

## Layout

The primary application shell is a two-region layout: a 72px collapsed or 248px expanded sidebar beside a flexible content column. The top bar is approximately 100px tall with 5% horizontal padding. Main content uses a max width of 1180px, centered with 42px top and 60px bottom padding.

Common content grids use 18px gaps. Section transitions use roughly 42px vertical spacing, while cards and panels use 18px to 24px internal spacing. At widths below 760px, the sidebar becomes an off-canvas drawer and the welcome hero image is removed so the task surface owns the viewport. At widths below 700px, admin panels compress to 16px padding and multi-column content stacks.

## Elevation & Depth

The system is primarily tonal and border-led. Use `var(--el-box-shadow-lighter)` for standard cards and `var(--el-box-shadow)` for a signature hero or clearly raised surface. Avoid stacking multiple shadows on adjacent cards. The hero card may use a blue primary-to-light gradient; ordinary cards should remain flat white surfaces with a light border.

## Shapes

The shape language is compact and approachable: 7px to 10px radii for controls and navigation, 14px to 16px for content panels, 18px for the primary hero, and 20px for pill-like chips. Borders are 1px and use Element Plus light or regular border variables. Circular geometry is reserved for avatars, progress donuts, and decorative rings.

## Components

### Buttons
- **Shape:** Compact rounded controls (7px to 8px).
- **Primary:** Element Plus primary blue with white text, commonly 10px 16px padding.
- **Hover / Focus:** Shift to the appropriate Element Plus light/dark semantic variable; preserve a visible keyboard focus state.
- **Secondary / Ghost / Tertiary:** Transparent or white background with a 1px neutral border; quiet text becomes primary blue on hover.

### Chips
- **Style:** Soft fill from `var(--el-fill-color-light)` or `var(--el-color-primary-light-9)`, secondary text or primary blue, usually pill-shaped.
- **State:** Use the primary light fill for active or actionable chips; keep passive metadata neutral.

### Cards / Containers
- **Corner Style:** 14px for panels and 18px for the main hero.
- **Background:** White `var(--el-bg-color)` on the page background `var(--el-bg-color-page)`.
- **Shadow Strategy:** Light Element Plus shadow for standard cards; no shadow when a border already provides sufficient separation.
- **Border:** 1px `var(--el-border-color-light)` for panels and check-in/stat surfaces.
- **Internal Padding:** 18px to 26px, with 24px as the common default.

### Inputs / Fields
- **Style:** Element Plus inputs with white or theme surface backgrounds, neutral borders, and 8px control radius.
- **Focus:** Use Element Plus primary border and focus treatment; never remove the focus indicator.
- **Error / Disabled:** Use semantic danger and disabled variables from Element Plus rather than custom colors.

### Navigation
- **Style:** Icon-led 45px rail items with 10px radius, 7px gaps, and secondary text by default.
- **Default / Hover / Active:** Transparent default; primary blue text with `var(--el-color-primary-light-9)` background on hover and active; active labels use weight 600.
- **Mobile:** The rail becomes a modal off-canvas drawer with a dimming overlay and closes after navigation.

### Daily Word
- **Style:** A compact learning cue inside the expanded sidebar, using a light fill, 1px border, 13px radius, small metadata label, and a 19px word.
- **Behavior:** It collapses with the sidebar and supports loading and unavailable states without changing the shell layout.

## Do's and Don'ts

### Do:
- **Do** use Element Plus semantic variables for colors so light and dark themes stay synchronized.
- **Do** preserve the blue primary accent as the single dominant action color.
- **Do** use white surfaces, light borders, and tonal layering to establish hierarchy.
- **Do** keep page content within the established 1180px maximum and 5% responsive gutters.
- **Do** provide clear loading, empty, error, disabled, and authenticated states.
- **Do** keep interactive controls keyboard accessible with visible focus treatment.

### Don't:
- **Don't** introduce a second brand accent or a saturated decorative palette without an explicit product decision.
- **Don't** replace the application shell with a marketing-style full-bleed layout on task screens.
- **Don't** use large shadows, glassmorphism, or heavy gradients on ordinary cards.
- **Don't** hard-code light-mode colors where an Element Plus semantic variable exists.
- **Don't** hide important navigation labels or status information on mobile.
- **Don't** invent product claims, metrics, testimonials, pricing, or certifications in UI copy.
