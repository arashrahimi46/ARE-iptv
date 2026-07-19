---
name: are-iptv-design
description: Use this skill to generate well-branded interfaces and assets for ARE iptv — a free, feature-rich IPTV player for Android TV — either for production or throwaway prototypes/mocks. Contains essential design guidelines, colors, type, fonts, assets, and a full TV-focused UI kit for prototyping.
user-invocable: true
---

Read the `readme.md` file within this skill, and explore the other available files (tokens, components, ui_kits, guidelines).

ARE iptv is a **dark-first, D-pad-driven 10-foot TV interface** with an electric-blue accent. The single most important interaction is the **focus state** (scale + accent glow ring). Copy is calm, sentence-case, and functional; no emoji in UI. Both light and dark themes are supported via `data-theme` on the root.

If creating visual artifacts (slides, mocks, throwaway prototypes, etc), copy assets out and create static HTML files for the user to view — load `styles.css`, the Lucide CDN script, and (for components) `_ds_bundle.js`, then read components from `window.AREIptvDesignSystem_632b75`. If working on production code, copy assets and read the rules here to become an expert in designing with this brand.

If the user invokes this skill without any other guidance, ask them what they want to build or design, ask some questions, and act as an expert designer who outputs HTML artifacts _or_ production code, depending on the need.

Key files:
- `styles.css` — global entry point (import this one file).
- `tokens/` — colors (dark + light), typography, spacing, radius, shadows, motion.
- `components/` — Button, IconButton, Chip, Badge, Icon, PosterTile, ChannelTile, Rail, Hero, ContinueCard, SidebarNav, Tabs, GuideCell, StreamHealth, TextField, Switch, StepIndicator, PlayerControls, Dialog.
- `ui_kits/are-tv/` — full interactive Android TV app recreation.
- `guidelines/` — foundation specimen cards.
