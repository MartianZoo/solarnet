# Solarnet glossary

Runtime, task, and history terms. Language, type, and Canon terms are in the
[Pets glossary](../pets/docs/glossary.md).

- **abstract task:** A task whose instruction still requires an externally supplied choice. Selecting it resolves game-world-dependent parts such as metrics, but the task remains abstract until narrowing has supplied every required choice. Antonym: concrete task.
- **action:** An action granted to a Player on a turn: starting it adds work assigned to that Player, and it lasts until the relevant work finishes. Authored actions are defined in the [Pets glossary](../pets/docs/glossary.md).
- **ActorAccess:** The passive, permissions-aware conduit through which an agent invokes engine mutations for its Actor. Callers do not bypass the Actor's unique agent to use it.
- **agent:** The unique mutation issuer for one Actor in one configured game world. Client requests pass through it. It is not a task queue or core-engine concept.
- **assignee:** The Actor with exclusive authority to advance a task now. Initially the controller selects it; selection may reassign it to the task's selection assignee, who narrows any remaining choices. When instruction-side `BY` becomes concrete, it may reassign the task again. The final assignee's agent executes it.
- **bootstrap:** The shortest special prefix needed to construct a game world and establish Admin as an ordinary Actor. Once Admin can carry out assigned tasks through the normal lifecycle, bootstrap is over; later game setup remains ordinary Admin work.
- **cause:** Attribution attached to non-manual tasks and copied to the resulting change events. It pairs the type of the context Component whose effect fired with the ordinal of the triggering change event. Those pointers explain the causal chain but do not uniquely identify which of several matching effects fired, so a cause is attribution rather than a complete derivation proof.
- **change event:** A game event recording one state change together with its ordinal, performer, and optional cause. Task lifecycle changes are recorded separately as task events.
- **component graph:** The logical directed graph whose vertices are Components and whose edges are dependencies. Because a type includes the exact types of its dependency targets, the game world stores the vertices as a multiset of types rather than as separately identified objects and edges.
- **concrete task:** Antonym: abstract task.
- **controller:** The Actor that ordered a task and receives its independent siblings and continuations. A task keeps this identity while selection and concrete `BY` handoffs change its assignee.
- **dependent removal:** Automatic removal of Components that depend on a Component being removed, performed first and cascading as necessary.
- **drain:** To become empty. Once no ordinary task remains, the earliest remaining deferred work (`LATER`, then `FINALLY`) becomes ordinary.
- **engine:** The passive mechanism that validates Actor-attributed mutations and calculates their state consequences. It is not an Actor or Component.
- **event log:** The ordered history of state changes and task lifecycle events in a game world.
- **follow mode:** The mode in which Solarnet calculates the state transitions for a game played elsewhere and trusts client-supplied draws, reveals, discards, and plays.
- **game world:** The complete live engine state of a game: its component graph, global task queue, event log, timeline, and class table, together with the catalog and immutable premise behind them. ActorAccess and agents are configured above that state.
- **live effect:** A component effect paired with its existing context Component, so that it can respond to change events. It counts according to the multiplicity of that type.
- **manual:** Initiated by a Solarnet client rather than caused by an effect. Selecting or narrowing an already pending task is not a new manual action. Once a game is started, its own rules offer every turn and begin every phase, so a game can contain no manual operation beyond its start.
- **task narrowing:** Task narrowing may fill one sub-specification at a time; each partial choice is recorded as task state rather than a state change, and resolution may reduce the choices offered for the remaining parts. An abstract task becomes executable only after it has narrowed to a concrete task.
- **origin indicator:**
- **pending task:** An unselected task in the global task pool, visible through its current assignee's filtered view.
- **performer:** The Actor whose agent actually executes a state change and is therefore recorded on its event. A task stores no separate performer: its final assignee performs it.
- **queue position:**
- **REgo PLastics:** Solarnet's command-line interface for driving the engine.
- **resolution:** The engine's interpretation of an instruction against the current game world. It evaluates gates and metrics, checks execution prerequisites, bounds AMAP and optional quantities, and translates concrete custom instructions. Resolution follows selection and repeats after each narrowing.
- **selected task:** The task chosen to finish next. Selection sets `Task.selected`, may change its assignee, and excludes competing task commands because resolution has read the current game world. The task may remain abstract and accept partial narrowing from its new assignee.
- **selection assignee:** The Actor recorded on a task as its recipient upon selection. It may equal the controller. After the transfer it is simply the current assignee, unless concrete instruction-side `BY` transfers the task again.
- **selection:** The client activity that chooses a pending task to finish next, resolves it, and assigns it to its recorded selection assignee. Selection determines which task the client will finish next.
- **state change:** An exact Component gain, removal, or transmutation that occurred in a game world.
- **System:** Admin-only machinery. A normalized queued task containing an elementary gain of a System Component, alone or under a top-level instruction-side `BY`, begins assigned to Admin before any Player could select it. A later explicit `BY` remains authoritative, but the gain still cannot be performed by any Actor except Admin. An unowned System occurrence does not implicitly restrict effect matching by its Actor. Presentation visibility is classified separately.
- **task:** A unit of pending work containing an instruction; controller, selection-assignee, and current-assignee Actors; optional cause; and lifecycle metadata. It represents both what must eventually happen and the choices still permitted.
- **task event:**
- **task id:**
- **task queue:** The one stored, unordered set of pending tasks in a game world. Every task records its assignee; Actor-specific queues are filtered views of that set. Player clients choose the order of their available tasks.
- **task result:** The change events and newly spawned task ids returned by a successful operation.
- **trigger scaling:**
- **triggered instruction:** The instruction produced when a live effect matches one change event.
