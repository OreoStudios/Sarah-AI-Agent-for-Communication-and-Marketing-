# Sarah — the working edition

## Philosophy
A calm publishing desk for turning intent into considered work. Editorial hierarchy distinguishes conversation, creative output and operational records. Whitespace has a job: generous around prose, compact around status and metadata. No decorative charts or simulated activity.

## Identity
The Sarah mark joins two offset editorial brackets with a connecting line and a decision point. It represents a thought becoming an action. Use a single color, preserve its open geometry, and never animate it continuously. The mark appears in navigation, welcome, replies, empty states and loading states.

## Palette
Light: paper `#f5f3ed`, surface `#fffefa`, inset `#eae8df`, ink `#252e2c`, secondary `#626b65`, rule `#d5d9cf`, petrol `#245c54`. Dark: paper `#191f1e`, surface `#222a28`, inset `#2b3531`, ink `#eceee5`, secondary `#b0bbb2`, rule `#414e47`, accent `#9bc9b7`. Success, attention and danger have separately tuned light/dark text colors. Color indicates actions and meaning, not decoration.

## Typography
Display: Georgia, with Times New Roman/serif fallbacks. UI: Segoe UI with system sans fallbacks. Metadata: Consolas with platform monospace fallbacks. These are platform-installed fonts used through CSS; no proprietary font files are distributed or downloaded. The product works without external font services. Display headings use normal weight, tight tracking and short measures; body text uses comfortable 1.65 leading, UI 1.45. Metadata is small but readable, with tabular numerals. Sizes, measures and font families are centralized in tokens.

## Space and geometry
Base spacing units: 4, 8, 12, 16, 24, 32, 48, 64px. Maximum prose width 68ch; page width 1280px. Buttons and fields use 4px radii, composer 8px. Section rules replace nested cards. Campaign index rows, publication date groups and audit timelines have distinct structures. White space is reduced on narrow viewports.

## Interaction
Buttons have a minimum 44px target, visible focus and explicit disabled states. Primary actions use petrol; secondary actions use quiet outlines/text. Status is textual with a small semantic dot, never color alone. Form labels remain visible. Errors use alerts and preserve entered content. Dialogs use native modal behavior, accessible names, Escape and focus restoration. Async actions disable duplicate submission. Theme preference is persisted and applied before rendering.

## Motion
120–180ms color/opacity transitions only. Message entry can briefly fade in. No ambient animation, simulated progress or spring effects. Reduced-motion disables transitions and animations.

## Responsive composition
Desktop: narrow editorial rail, fluid work area; conversation gets an optional context margin. Tablet: tighter rail and single-column detail. Mobile: compact brand header with an expandable navigation menu, single-column conversation, stacked campaign rows and audience records. Publication actions wrap beneath content. Tables scroll only inside their named region when a true table is necessary. Main page never intentionally overflows.

## Capabilities
Studio is a reading/review surface because draft writes and version history are absent from the API. Analytics is operational intelligence with real campaign counts; no fabricated engagement. Agent timeline shows only returned action outcomes. Publication retry/cancel and autonomy pause/resume preserve their existing backend behavior.
