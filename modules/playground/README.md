# Mars Playground: history viewer and adjustable workspace

This document records the visual and interaction decisions for Mars Playground and the adjustable
mockup used to explore them. Broader product direction lives in
[PLAYGROUND.md](../../PLAYGROUND.md). The current app is a passive Solarnet recording viewer. It
loads a real replay, reads its world at recorded positions, and does not run gameplay. The sandbox
and gameplay interactions below remain design intent.

## What we know about the app itself

Mars Playground is a browser app for constructing Terraforming Mars situations and trying moves
with Solarnet's rules. It uses Compose Multiplatform. Its browser artifact is a static collection
of files; a separate server may mount it at a chosen relative URI. The target is desktop and tablet
browsers. The whole workspace scales uniformly to the browser width: making the browser narrower
must not reflow the layout or squeeze individual regions.

The planned workspace serves **SANDBOX**, **GAMEPLAY**, and **HISTORY**. For now only **HISTORY** is
active. The selected mode glows in a
small three-button group beside a prominent “Mars Playground” title; **START OVER** sits to the
right. Switching between sandbox and gameplay midway through a game is desired. The precise game
state consequences of that switch still need a model decision. The view should look largely the
same in every mode.

The visual theme is clean science fiction with a sense of physical objects: cards, tiles, resource
piles, and places where they normally live. Choosing a card should happen at its card, not in a
separate card-choice dialog. Distinct screen regions can be arranged independently, but should not
look like a stack of uniformly headed cards. The task table is a deliberate exception to the
physical metaphor: it exposes raw Pets instructions and direct controls for narrowing them.

The screen places global state above the global task queue and player state below it. A central
message bar should always say what the UI is waiting for the user to do next. That requirement
does not call for the old, separate “waiting for Yellow” row. The event log sits beside the task
queue. Out-of-game functions occupy a small separate area: start over/reset and load scenario,
which replaces the current situation. Saving scenarios and generating restore URLs are deferred.

The upper workspace has two equal, roughly square global panes with elegant visible dividing
lines and genuine looking tabs attached above them. The tab strip contains **Mars**, **Players**
(including player comparison), **Goals** (including milestones and awards), **Colonies**,
**Turmoil**, **Explorer**, **Config**, and **PETS info**. The pinned tab moves to the leftmost position and
looks physically attached to the left pane; the remaining tabs retain their natural order and
control the right pane. Hovering an unpinned tab reveals a thumbtack on its right side. Explicitly
pinning another tab is the only way to replace the left pane. The former pinned tab returns to its
natural position. The right pane never duplicates the pinned tab: clicking that tab or having a
task request it leaves the right pane alone. Otherwise, a task needing a particular global view
automatically selects that tab in the right pane. Each tab needs representative content, even if
it is only placeholder content in the mockup.

A very slim, vertical global dashboard sits to the right of those panes and always shows **Gen**,
phase, player on turn, global parameters such as temperature, oxygen, oceans, and related status.
Player identity appears as a colored square rather than a written color name.

The **TASK QUEUE (PENDING)** and **EVENT LOG (PAST)** sit side by side below the global workspace.
They present raw information in aligned columns, without decorative event or task treatments.
The task queue is global and shows all pending tasks, including their assignee information. Each
task includes its ID, raw full-form Pets instruction, **GIVEN BY** context, **BECAUSE** event
ordinal, and a **WHO** column of three ordered squares. Their tooltips read exactly **“Decides when
the task will happen”**, **“Makes choices for the task”**, and **“Actually carries out the task”**.
Admin uses a very pale neutral grey. Clicking a task selects it and resolves it against current
game state. The UI should then show why it remains abstract and allow narrowing at the relevant
segment of the instruction: click an abstract root class to choose a concrete subclass first;
after that, click an abstract dependency to narrow it; click a `?` scalar to choose a value and
remove the `?`; click an OR instruction to choose its branch. A choice widget is attached to the
relevant Pets text. Prefer sending the narrowing directly to the engine if its API permits it.
Options should avoid choices that dead-end the turn on a reasonable best-effort basis. A task
can be executed directly from its table row once concrete, or from the physical location it
concerns. There is no separate “execute selected” button. The task-table controls are exempt from
the physical-object metaphor.

The event log has **WHO** squares, a narrow right-aligned delta column sized for at most two-digit
values, component, **GIVEN BY**, and **BECAUSE** columns. Leave more room after the delta than
before it. Show recorded events in forward order, with all Pets expressions in monospace.
Gains and removals have signs; transmutations show an unsigned quantity; a Signal change shows ✴
and its type once. For example, a recorded `ConvertPlants → ConvertPlants` signal displays as
`✴ ConvertPlants`. Its rows are more closely spaced than the task rows. The log includes component
changes after engine initialization, including non-Real and System changes. Checkboxes can exclude
Admin events and non-Real changes. Reaching either end of the log must not pass wheel scrolling to
the page. The log does not need forward/back/rollback controls.

The recorded Pets strings remain intact in display data. Pets text always uses monospace,
including instructions, table contexts, and the Explorer. In the display, if every player name
used as a dependency argument in an expression is the same player, omit those repeated arguments.
For example, `ProjectCard<Yellow, Hand>!` appears as `ProjectCard<Hand>!` with a yellow underline.
Extend the underline a little beyond the text and place it close below the letters, with tiny
upward ticks at both ends to make a tray. Each side of a transmutation arrow has its own tray;
the tray never crosses the arrow. If a segment refers to different players, keep their names
visible. Instruction text uses the same pale neutral grey as Admin, not cyan; the tray carries
the player color.

Below those tables, tall, narrow rectangles marked `<` and `>` flank the entire player area.
Clicking either switches the viewed player with a quick horizontal swipe. The dashboard resembles
the existing viewer dashboard: resource stocks and production, TR, card count, and tags. It has
no “Pristar” or “action phase” caption. The horizontally scrollable played-card row is literally
just card images: no **HAND** section, **IN PLAY** heading, or count. The images are close together
and about 140% the size used in the early mockup. Cards without local art are omitted.

The **Explorer** tab shows present component types and counts as a tree, with abstract types as
non-leaf nodes. A node such as
`OwnedTile<Owner, Area>` can expand by concrete subtype, or the user can click `Owner` or `Area`
to regroup that node by the chosen dependency, such as `OwnedTile<Red, Area>` and
`OwnedTile<Green, Area>`. The parent total stays the same regardless of subdivision. **Include
non-Real components** starts off. Sandbox mode allows count editing only for components where the
engine permits the equivalent of `exMachinaSetTo`, plus a field to type a component and summon a
chosen number of occurrences. Broad expansion of every zero-count type is deferred because large
type families would be unwieldy. The PETS info tab can display raw type and task information.

## How the current history viewer works

The app packages the full game files generated by `:tfm-tests:jvmTest`. At launch it opens
`OtbGame20260912Test` near event 12896 if that recording exists; otherwise it opens the first
available recording at its end. **LOAD SCENARIO** chooses another recording, and **START OVER**
seeks to its first player-request position. The current Game World supplies the map, queue, player and
global numbers, played cards, goals, colonies, Turmoil, and Explorer counts. The full visible event
log comes from the recording. Thin dividers mark positions just before a player selects a task or a
replay correction starts, plus the final position; clicking a divider seeks
there, while clicking an event row leaves the timeline cursor alone. Up/down arrows step between
dividers, and left/right arrows step between player-action squares. Later events remain visible but
dimmed. A BECAUSE number scrolls to and highlights its event without seeking. When its cause is
outside the displayed change log, the number is plain text with a tooltip showing the event.
Generation tabs seek to the start of a generation. The colored squares below them seek to the first
recorded position after each player action slot opens. A first action comes from `NewTurn`, and a
second action comes from `SecondAction`; passes and unused second slots have no square.
The Mars tab uses the old viewer's planet image and hex layout, including empty-area bonuses,
Noctis and volcano marks, tile images, and centered owner cubes. The same cube marks used actions
on played cards, centered two-thirds of the way up each marked card.
PETS class names throughout the workspace open PETS info on the pane that holds that tab. The
inspector shows the loaded class declaration, docstring, type relationships, invariants, and live
counts. The Config tab shows the game file's resolved configuration as a comma-separated line,
then groups the resolved GamePremise's players, included class selections, and setup adjustments.
Class names in the premise list open the same inspector.

`resources/files/specimen.json` retains the former static design capture for reference; it is not
packaged or read by the app. The event log's leading `#` column shows each event's ordinal, which
is what the **BECAUSE** column refers to. Task rows can be inspected but not executed. The mode
group is fixed at **HISTORY**; sandbox count editing and gameplay are deferred.

Every rectangular workspace region is a freely movable and resizable mockup window. The default
arrangement is aligned and well spaced, without overlaps. Drag affordances are special to the
mockup: light but plainly visible dashed outlines appear only around the region nearest the
pointer, including when the pointer is inside it or in a narrow strip just outside it. The four
edge directions and corners show matching double-arrow resize cursors; the move strip outside the
edges shows a move cursor. Because that strip is thin where windows are close together, dragging
anywhere inside a window with Alt (Option) held also moves it. These outlines are absent from the
intended game UI. The two global panes and their attached tabs still have their own visible
borders in the intended UI.

Thin vertical dividers between task and event columns can be dragged horizontally, with a
horizontal double-arrow cursor on hover. Their widths live in the same layout JSON as the window
geometry. Window and column changes redraw while dragging and are saved when the drag ends, so
file writes do not interrupt pointer movement. Editing the controls beneath the workspace does not
trigger window dragging or move the page.

A line below the workspace separates it from live look-and-feel controls. Those controls include
colors, border width, corner radius, shadow, spacing, a regular font and point size, and a
monospace font and point size. The fonts are bundled: [IBM Plex](https://github.com/IBM/plex),
[Inter](https://github.com/google/fonts/tree/main/ofl/inter), and
[JetBrains Mono](https://github.com/google/fonts/tree/main/ofl/jetbrainsmono). Browser generic
families are not offered: Compose draws to a canvas and cannot use them. License texts are in
`fonts/`. Pets text, task/event tables, and the Explorer use the selected monospace face. The
default colors are dark blue, cool teal, and muted Mars orange; the type and fine lines should
remain clear rather than decorative.

Every window position and size, table-column width, canvas dimension, and tweakable style value is
written to `_local/playground/layout.json` after a change. Editing that JSON while the development
page is open refreshes the view without restarting the server; fields missing from the file take
their defaults, so `{}` restores them all. The file may be copied or hand-edited to try
arrangements; the UI needs no separate preset manager. The entire workspace and its art scale as a
single fixed-proportion canvas when browser width changes. Window geometry and table column widths
stay the same in logical workspace coordinates on a narrow browser.

Run `./gradlew :playground:wasmJsBrowserDevelopmentRun` for the editable preview at
`http://localhost:8086/`. Run
`./gradlew :playground:wasmJsBrowserDistribution` to create the static web distribution in this
module's Gradle build directory. The static distribution has no layout endpoint, so it shows the
default arrangement and rearranging windows uses the development preview; the endpoint is in
`webpack.config.d/layout-endpoint.js`. The Kotlin sources are in
`src/wasmJs/dev/martianzoo/tfm/web/playground/`: `Main.kt` assembles the regions, `Workspace.kt`
owns scaling and the mockup window handles, `MockupLayout.kt` holds the layout and style defaults,
`RecordingStore.kt` opens recordings, `liveSpecimen.kt` reads the current world, `Specimen.kt`
holds display values, and `PetsText.kt` shortens player arguments and draws trays.
