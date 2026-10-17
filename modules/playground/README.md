# Mars Playground: history viewer and adjustable workspace

This document records the visual and interaction decisions for Mars Playground and the adjustable
mockup used to explore them. Broader product direction lives in
[PLAYGROUND.md](../../docs/PLAYGROUND.md). The app loads replay recordings and supports live solo play
through the Play tab. Sandbox editing and physical card/map input remain design intent.

## Solo play

**PLAY** opens the solo configuration dialog. **GO** starts a live engine session with named,
shuffled decks. The optional integer seed reproduces the shuffle and appears in Config. The
dialog requires SoloMode and a single seated player. Search for Life and Asteroid Deflection
System are excluded because fresh-card reveals are unsupported. Decks never reshuffle: exhausting
a deck, including finding too few matches in a search, ends that session. Searches consume every
examined card, including nonmatches. Startup consumes the first four project cards to place the
neutral cities and greeneries automatically, using the solo-placement tool's standard calculation
(not its compatibility mode). These tiles are already on the map when play opens. All seven maps
are available: Tharsis, Hellas, Elysium, Utopia, Cimmeria, Amazonis, and Vastitas.

New games default to Green on Tharsis with Corporate Era, Prelude, Venus Next, and `promos:cards`.
Milestone and award entries are disabled for a single player. Colony tile entry is disabled until
ColoniesExpansion is included.

The **Play** tab lists only the player's pending tasks, then the engine's next narrowing choices or
card-name choices. At **Choose a task**, each live task is selected hypothetically and rolled back.
Selectable rows show the evaluated instruction, including current `PER` amounts; unavailable rows
remain visible in darker gray and cannot be clicked. Selecting a row uses its original task ID.
These previews do not deal cards or invoke application completion callbacks. The task log and
history display retain their original task text. Players execute their tasks manually; Admin retains
EAGER autoexecution and receives missing card names through the dealer. After undo or truncation,
executable Admin work appears as a continuation row in Play; selecting it resumes automatic work
through the same dealer. Unsupported Admin choices appear as a waiting status when they block play. Hand and
Selecting cards appear above played cards. Card-playing choices are filtered to known cards at the
source (Hand or Selecting), with the required card back, before pagination. Other choices can still
fail: the options API checks current prerequisites without simulating effects or future work.
Read the error, reset provisional choices, or return to an earlier stop and **TRUNCATE**. Earlier
successful commands remain committed when a later command fails.

History browsing is read-only. **RESUME PLAY** returns to the retained tail; **TRUNCATE** discards
its future, restores deck cursors and named holdings, and stays at that position until the next
input. Undoing an automatic deal stops before that deal; another undo can then undo the preceding
player command. **TO START**
returns to completed setup startup, with initialization events still in the log. Cmd-Z (Ctrl-Z)
cancels a naming attempt or resets draft choices first; otherwise it steps back and truncates.
The shortcut leaves text-field editing alone. Live sessions are not saved across page reloads.
Development builds do not refresh an open page automatically; refresh manually when ready to use a
new build. The new-game dialog caches candidate validation for the current draft and checks choices
between browser turns; changing the draft or player invalidates those results. The chosen
configuration is fully validated before GO is enabled.

## What we know about the app itself

Mars Playground is a browser app for constructing Terraforming Mars situations and trying moves
with Solarnet's rules. It uses Compose Multiplatform. Its browser artifact is a static collection
of files; a separate server may mount it at a chosen relative URI. The target is desktop and tablet
browsers. The whole workspace scales uniformly to the browser width: making the browser narrower
must not reflow the layout or squeeze individual regions.

The title badge shows **PLAY** at a live game's latest position and **HISTORY** when browsing.
**LOAD GAME** opens a large scrollable chooser of replay recordings, showing each name, its full
comma-separated original GameConfig (including players), and final scores for games that reached
VictoryPhase. Loading opens the saved history and previews it over roughly five seconds, with the
workspace visible and interaction disabled. It finishes at the last recorded position and names the recorded winner, or says that the recording ended when no
victory was recorded. Clicking a generation or navigating history clears that message. Strict engine
reconstruction remains in the replay export tests. The viewer retains the original event order and
notes. **TO START** seeks to the first completed position. Sandbox editing
remains a future mode.

The visual theme is clean science fiction with a sense of physical objects: cards, tiles, resource
piles, and places where they normally live. Choosing a card should happen at its card, not in a
separate card-choice dialog. Distinct screen regions can be arranged independently, but should not
look like a stack of uniformly headed cards. The task table is a deliberate exception to the
physical metaphor: it exposes raw Pets instructions and direct controls for narrowing them.

The hex overlay fits the loaded map’s actual coordinate extent to the Mars image, scaling tiles
and placement bonuses with it. Player cubes on Mars are 75% of the normal cube size.

Mars stays in the upper-left panel, at 80% of its former width and height. The tabbed panel
sits below Mars and contains **Players** (including player comparison), **Goals** (including
milestones and awards), **Colonies**, **Turmoil**, **Effects**, **Explorer**, and **Config**.
Colonies and Turmoil appear only when their expansion Class components are present; Goals appears
when milestone or award Class components are present. Effects groups current subscriptions by
source owner and exact trigger, showing a single unwrapped `trigger: consequence, consequence`
line with horizontal scrolling and 4 dp between effect rows. Source components are omitted, and copy counts scale their
consequences. Effects shares the event log's imaginary, icon, and tray settings. Icons and trays use the same
text renderer; excluding imaginary effects keeps subscriptions whose watched root class or any
changed RHS root class is Iconic. Include Admin does not yet filter Effects; global effects remain
visible while that rule is unresolved.
Placement, UseAction, Billing, End (scoring), and self triggers are temporarily hidden.
Effects also hides exact specialized trigger/instruction pairs present immediately before
BootstrapPhase ended, regardless of their copy counts. Later card effects remain eligible.
Every `!` is omitted from Effects display text.
There is no pinning; Mars always remains visible. The tab strip scrolls horizontally if needed and brings the selected tab into view.

The right-hand side holds the event log above the task queue, sharing a movable, resizable region.
The event log initially receives three-quarters of its height. Drag the horizontal divider
up or down to give either log more room. Player state sits below the global workspace, with a message bar between them. In the intended game UI, the
message bar should always say what the UI is waiting for the user to do next. Out-of-game functions
occupy a separate area: start over/reset and load scenario, which replaces the current situation.
Saving scenarios and generating restore URLs remain deferred.

A very slim, vertical global dashboard sits to the left of Mars and always shows **Gen**,
phase, player on turn, global parameters such as temperature, oxygen, oceans, and related status.
Player identity appears as a colored square rather than a written color name.

The **EVENT LOG (PAST)** sits above the **TASK QUEUE (PENDING)** on the right.
They present raw information in aligned columns, without decorative event or task treatments.
The task queue is global and shows pending tasks except LATER and FINALLY work, including their
assignee information. Each task includes its ID, raw full-form Pets instruction, **GIVEN BY**
context, **WHY** event ordinal, and a **WHO** square for its assignee. Admin uses a very pale neutral grey. Clicking a task
selects it and resolves it against current game state. The UI should then show why it remains
abstract and allow narrowing at the relevant segment of the instruction: click an abstract root
class to choose a concrete subclass first;
after that, click an abstract dependency to narrow it; click a `?` scalar to choose a value and
remove the `?`; click an OR instruction to choose its branch. A choice widget is attached to the
relevant Pets text. Prefer sending the narrowing directly to the engine if its API permits it.
Options should avoid choices that dead-end the turn on a reasonable best-effort basis. A task
can be executed directly from its table row once concrete, or from the physical location it
concerns. There is no separate “execute selected” button. The task-table controls are exempt from
the physical-object metaphor.

The event log has **WHO** squares, a narrow right-aligned delta column sized for at most two-digit
values, component, **BAL**, **GIVEN BY**, and **WHY** columns. **BAL** shows the exact
component's count immediately after each regular gain or removal, including its owner and other
arguments. For transmutations it shows the gained type's resulting count; signals leave it blank.
Counts include hidden events and remain tied
to each event when browsing earlier history. Loading or updating a game plays the exact event history
through a passive GameWorld and reads each count immediately after applying that event; the UI does
not maintain its own running totals. Leave more room after the delta than
before it. Show recorded events in forward order, with all Pets expressions in monospace.
When an event has no GIVEN BY value, display `EX MACHINA` in that cell.
Gains and removals have signs; transmutations show an unsigned quantity followed by `B FROM A`,
with the gained type first. Literal `->` text remains unchanged. A Signal change shows ✴
and its type once. For example, a recorded `ConvertPlants FROM ConvertPlants` signal displays as
`✴ ConvertPlants`. Events are uniformly spaced 32 logical pixels apart. A separate left-hand gutter holds a
vertical rail with clickable ticks between rows, immediately before the first visible event after
each decision. Decisions sharing that gap share a tick; both clicking and up/down visit only the
earliest decision in the gap. The selected stop has a longer, thicker teal tick.
The **#**, **WHO**, and **WHY** columns center their headings and contents.
The **Use icons** checkbox starts checked; it replaces class names with available
icons in event expressions and GIVEN BY, task instructions and GIVEN BY, and Play tab options.
Event and task icons keep names as hover labels and Almanac links; clicking any part of a
Play option, including its icons, selects that option.
Icons are 1.32 times the text height and retain their original proportions. Player-color cubes
take precedence. Otherwise, a class name uses exactly `images/classes/30px/<ClassName>.png` when
available and remains text when absent; there are no card or standard-project exclusions and no
full-size image fallback. Dashboard icons also fall back to their full class names. The `30px`
asset directory specifies image height, preserving aspect ratio. Class icons in player dashboards
and comparison rows also open their Almanac entries.
The generic CardFront, ActiveCard, AutomatedCard, ActionCard, and EventCard symbols
are hosted at `https://www.martianzoo.dev/images/classes/` with transparent exteriors and trimmed
margins. All game images load from martianzoo.dev; none are bundled locally.
ActiveCard, AutomatedCard, and EventCard use
the darker base colors of the printed-card banners, excluding highlights: teal-blue with a shallow
downward-curved bottom, green with a very slightly upward-curved bottom, and orange with a straight
rectangular banner. ActionCard uses the same card and banner as ActiveCard, with an action arrow;
CardFront uses EventCard's rectangular banner in gray. ActionUsedMarker uses a clear cube over the
action arrow, leaving space before the arrowhead. Shared image loads continue when
preview rows disappear, so advancing through a recording does not cancel phase artwork. Player
markers show a large front face with narrow top and right faces and sharp edges. Phase icons use the
same hosted artwork in inline text and the sidebar, where hovering shows the phase name and
clicking opens its Almanac entry in a new browser tab. The hosted collection includes Disease, Science, Fighter, Graphene, and
Hydroelectric resource artwork.
The log includes component changes from the beginning of the recording, including initialization,
non-Iconic, and System changes.
Hovering an event row with attached notes shows those notes, including over otherwise empty cells.
The **WHY** cell keeps its own cause tooltip and does not show the row note.

**Include Admin** starts checked; **Include imaginary** starts unchecked.
Admin events are visible by default, while non-Iconic changes stay hidden. `SoloOpponent` is already
non-Iconic. Its dependent components also count as imaginary unless they have an Iconic dependency,
directly or through other components: neutral board tiles stay visible because their areas are
Iconic, while the solo resource stock stays hidden. A transfer still appears if either component
passes this filter. A navigation target remains visible even if filtered.
Reaching either end of the log must not pass wheel scrolling to the page. The log does not need forward/back/rollback controls.

The recorded Pets strings remain intact in display data. Pets text always uses monospace,
including instructions, table contexts, and the Explorer. By default, player-name dependency
arguments remain visible: `ProjectCard<Yellow, Hand>!` keeps `Yellow` inside the brackets. **Use trays** starts unchecked. Enabling it omits player dependency arguments from each complete type expression when they
all name the same player, and puts a tray under exactly that shortened expression. For example,
`Animal<Pets<Blue>>` becomes `Animal<Pets>` on a blue tray. Counts, punctuation, keywords, and
neighboring types stay outside the tray. Bare player names stay unchanged and receive no tray; mixed-player types keep their
arguments and receive no tray. This applies to Pets text in events, tasks, effects, and Play options. Effects retain their parsed
expressions when shortening owners, so bound forms such as `BioTag<@CardFront<Black>>` also
receive trays without losing the `@CardFront` binding. With **Use icons** enabled, each player name uses
that player's cube, with its name tooltip and class link. `SoloOpponent` uses an off-white cube.
Instruction text and class links use near-white. Megacredits use the generic `MC` artwork in
inline text and the resource dashboard, rather than the `MC01` amount icon.

Below those tables, tall, narrow rectangles marked `<` and `>` flank the entire player area.
Clicking either switches the viewed player with a quick horizontal swipe. The dashboard resembles
the existing viewer dashboard: resource stocks and production, TR, hand count, played-event count,
and tags.
The event symbol reports `PlayedEvent` beside the hand count, separate from the tags; EventTag is
omitted from the tag group. It has no “Pristar” or “action phase” caption.
Played cards fill eight columns without horizontal scrolling. The first three columns are
reserved for corporations, Preludes, and an overlapping automated-card stack, even when empty.
Active cards fill the remaining five columns and wrap to further rows. Played event cards are
hidden. Corporations and Preludes have space between full cards; automated cards overlap while
exposing titles, with a full-card preview on hover. The panel grows vertically, and vertical
scrolling belongs to the whole page. Visible played cards retain first-play order within each group.
A separate horizontally scrolling row shows Selecting cards to the left of the hand. Drag hand
cards horizontally to rearrange them. This display order is retained per player for the open game;
it does not change game state or recorded inputs. New cards are appended to the arranged hand.
Resource counters read the current World and cover the printed numeral using the source artwork's
Prototype font and badge gradient. At zero the original image is left intact. Card art lookup
tries the exact class name, then adds or removes `Fake`; cards still lacking
art show a named rectangle.
The **Published card text** checkbox above the cards starts unchecked (generated text). It switches
only card-front images, including hand and Selecting cards and hover previews, to the published-text
collection. The choice persists when changing the viewed player. Missing images in either collection
keep the named rectangle; they do not silently use the other text version.

Game images are fetched from `https://www.martianzoo.dev/images/`: generated card fronts
and tiles use `classes/`, published-text card fronts use `classes-pubtext/`, icons use `classes/30px/`,
and the Mars background uses `other/mars.png`. Fonts and replay recordings remain bundled.

The **Explorer** tab counts present Iconic components by default. Counts sit to the left of their
types. Each category uses Pets full-form spelling, omitting class-fixed dependencies while keeping
chosen values such as `Player2` visible. Clicking a class name opens its Almanac entry in a new browser tab.
The circle beside a root class or nested dependency type rebuilds that row's children by the chosen class's nearest visible subclasses, preserving the other bounds
as far as the type rules allow. A node such as `OwnedTile<Player2, Area>` can therefore expand by
tile class or by area while retaining `Player2`. Classes identified by `ClassTable.isProbableAspect`,
such as `Atomized`, are omitted as categories. Their concrete descendants remain reachable beneath
their nearest visible superclasses, with component counts unchanged.
Multiple inheritance can put the same component below two sibling categories; each count is the
count for its displayed type, and sibling counts need not add to their parent. **Include non-Iconic
components** starts off. Zero-count branches stay hidden. Sandbox count editing remains deferred.

## How the current history viewer works

The app opens with no game selected. **PLAY** starts a solo game; **LOAD GAME** lists the full game-file JSON exports generated
by the replay tests and opens the chosen recording at its first completed position.
`:tools:buildReplayIndex` extracts original configurations and completed scores into a small
`index.json` during resource packaging. Browsing the chooser reads only this index; the full game
is opened only after selection. The tool can also be run directly to regenerate the index.
**TO START** returns to that first position. The current Game World supplies the map, queue, player
and global numbers, played cards, goals, colonies, Turmoil, and Explorer counts. The full visible event
log comes from the recording. Ticks on the left rail mark positions before player task selections,
including automatic selections that leave an unresolved choice, plus the initial and final positions.
Clicking a tick seeks there, while clicking an event row leaves the timeline cursor alone. Up/down
arrows step between displayed ticks. Left/right arrows use the subset that begins a turn, including
the optional second action, passing, corporation and Prelude choices, and final greenery offers.
Later events remain visible but dimmed to 72% opacity. Every navigation target is centered vertically in the log, including jumps
from generation tabs, action markers, keyboard arrows, and WHY links. Hovering a WHY number
shows only the referenced gain instruction (or removal for a removal-only event); clicking seeks
to its completed position and highlights that event. Padding lets even the start and end positions sit in the middle of the log.
Generation tabs seek to the start of a generation. Small, player-colored right-pointing triangles
represent corporation and Prelude plays as well as chosen actions, including both actions in a turn,
with no surrounding square. Their tooltips describe the choice briefly: “Blue plays Insects,”
“Blue uses Sulphur-Eating Bacteria,” or “Blue sells 3 patents.” Sale quantities follow causal links.
Arrow navigation follows decision ticks rather than these later action events.
Passes and unused action slots have no triangle.
The Mars panel uses the old viewer's planet image and hex layout, including empty-area bonuses,
Noctis and volcano marks, tile images, and centered owner cubes. The same cube marks used actions
on played cards, centered two-thirds of the way up each marked card.
PETS class names and icons open `https://martianzoo.dev/pets/almanac/#ClassName` in a new browser tab,
using the clicked class's name in place of `ClassName`. The Config tab shows the game file's resolved configuration as a
comma-separated line,
then groups the resolved GamePremise's players, included class selections, and setup adjustments.
Class names in the premise list open the corresponding Almanac entries.

`resources/files/specimen.json` retains the former static design capture for reference; it is not
packaged or read by the app. The event log's leading `#` column shows each event's ordinal, which
is what the **WHY** column refers to. Recorded task rows are read-only; live tasks are chosen in the Play tab. Tasks currently
deferred as LATER or FINALLY are omitted from the queue.

Every rectangular workspace region is a freely movable and resizable mockup window. The default
arrangement is aligned and well spaced, without overlaps. Drag affordances are special to the
mockup: light but plainly visible dashed outlines appear only around the region nearest the
pointer, including when the pointer is inside it or in a narrow strip just outside it. The four
edge directions and corners show matching double-arrow resize cursors; the move strip outside the
edges shows a move cursor. Because that strip is thin where windows are close together, dragging
anywhere inside a window with Alt (Option) held also moves it. These outlines are absent from the
intended game UI. Mars and the tabbed panel still have their own visible borders in the intended UI.

Thin vertical dividers between task and event columns can be dragged horizontally, with a
horizontal double-arrow cursor on hover. Both tables share six column positions and widths; dragging
a divider in either table moves both in lockstep. The task table leaves the BAL slot empty. Both
tables reserve the same left gutter, so every divider aligns. The #, WHO, BAL, and WHY columns start
compact; component/instruction and GIVEN BY receive the remaining space in a 60/40 split.
The event's signed amount stays aligned inside its component cell. Shared widths are recorded
in the same review snapshot as the window geometry. Window and column changes redraw while dragging and are
saved when the drag ends, so
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

Window geometry, table-column widths, the log divider, canvas dimensions, and style values are
written to `_local/playground/layout.json` after an adjustment. This is a write-only snapshot for
review: the app never loads it or watches it for edits. Reloading starts from the layout and style
in `MockupLayout.kt`. To adopt adjustments permanently, inspect the snapshot and update those
code defaults. The entire workspace and its art scale as a single fixed-proportion canvas when browser width changes. Window geometry and table column widths
stay the same in logical workspace coordinates on a narrow browser.

Hovering a task in Play or Task Queue shows English from Pets `text`, using the loaded game's
vocabulary and addressing the assigned player as “you”. Unsupported phrases remain bracketed Pets.

Packaging reuses existing replay exports. Add `-PreplayExports` to regenerate them through the
JVM test suite, including on a checkout with no exports yet.

Run `./gradlew :playground:wasmJsBrowserDevelopmentRun` for the editable preview at
`http://localhost:8086/`. Run
`./gradlew :playground:wasmJsBrowserDistribution` to create the static web distribution in this
module's Gradle build directory. The static distribution starts from the same code defaults;
adjustments last until reload. Only the development preview writes snapshots; the endpoint is in
`webpack.config.d/layout-endpoint.js`. The Kotlin sources are in
`src/wasmJs/dev/martianzoo/tfm/web/playground/`: `Main.kt` assembles the regions, `Workspace.kt`
owns scaling and the mockup window handles, `MockupLayout.kt` holds the layout and style defaults,
`RecordingStore.kt` opens recordings, `liveSpecimen.kt` reads the current world, `Specimen.kt`
holds display values, and `PetsText.kt` renders linked Pets text and optional inline icons.

The static distribution is published at `https://www.martianzoo.dev/playground/` in the
`MartianZoo/site` repository. Copy the contents of `dist/wasmJs/productionExecutable` from this
module's Gradle build directory into that repository's `playground/` directory. The deployed app
uses images from `https://www.martianzoo.dev/images/`, makes no layout-save requests, and includes
a `noindex, nofollow` robots tag. Test the distribution at `/playground/` before publishing.
