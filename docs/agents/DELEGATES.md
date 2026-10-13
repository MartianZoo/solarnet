# Turmoil delegates and party leadership

> **Agent information:** Exploratory design note, not an accepted implementation plan.
>
> **Read when:** changing Turmoil delegate placement, removal, leadership, or their task choices.

## Current model and the pressure to change it

In [Turmoil's Pets declarations](../../src/common/dev/martianzoo/tfm/canon/bundles/turmoil/classes.pets),
`Delegate` contains `Chairman` and `PartyDelegate<Party>`. A separate, owner-held `PartyLeader<Party>`
marker says whose physical delegate occupies the leader seat. Placing a `PartyDelegate` can change
that marker; a player's instruction remains `PartyDelegate<Party>`. On placement, delegate count,
the incumbent, and clockwise distance from the acting player decide who gains the marker. Dominance is
recalculated separately after delegate gains and losses.

The marker represents a property of an existing delegate as another component. Ordinary delegate
removal does not always repair leadership when the leader's owner falls behind; see
[GAME_HACKS.md, item 21](GAME_HACKS.md#21-delegate-removal-relies-on-banned-delegate-to-repair-leadership).
`BannedDelegateRemoval` has its own repair in
[Turmoil cards](../../src/common/dev/martianzoo/tfm/canon/bundles/turmoil/cards.json5).

## Proposed shape for ordinary player operations

Explore `Person = Chairman | Delegate<Party>` and `Delegate<Party> = Leader | Follower`. A Leader
*is* a delegate, so a party has at most a single Leader and no separate leader marker. The user's
sketch gives each owner at most seven Persons. Current Pets instead caps each player's Delegates
at seven and Neutral's at fourteen; decide how those reserves map to `Person` before adopting the
shape. Placements and removals below are atomized: each delegate is handled individually.
Compare the counts that would result from the requested operation; in a valid settled state, the
incumbent cannot already trail another owner.

For a player placing a delegate in a party:

- If the player already holds its Leader, place a Follower.
- Otherwise, place a Follower if the incumbent owner's total will still tie or exceed that
  player's total after placement. An incumbent survives a tie.
- If the placement would put that player ahead, place a Leader. In the same protected operation
  (`::`), turn the previous Leader into a Follower. The first delegate of an empty party is a
  Leader.

For ordinary player-directed removal, inspect the **owner of the delegate being removed**, which
need not be the Actor:

- A nonleader owner removes a Follower.
- A leader owner removes a Follower if their remaining total will still tie or exceed every rival
  owner's total.
- Otherwise, remove that owner's Leader and promote a remaining Follower of the new leading
  owner. Removing a party's last Leader leaves the party empty.

For example, Kim has a Leader and a Follower while Stan has a Follower. Kim can remove her
Follower and retain leadership on a 1–1 tie. If Kim then removes her Leader, Stan's Follower
becomes Leader. From the 1–1 state, Stan could instead add a second delegate: Stan must place a
Leader and Kim's Leader becomes a Follower. The proposed task may still say `Delegate<Party>`;
the concrete operation log would show `Leader` when leadership changes. Whether the task engine
can select that subtype without another user choice remains open.

The user's proposed `BY Player` filter would keep these ordinary removal reactions out of solar
cleanup, which acts as Admin. A phase condition might also work, but it must refer to the actual
`TurmoilSolarPhase`, not a generic `SolarPhase`. The proposal does not yet specify how to update
Dominance or restore leadership after other changes to the party. Recruitment is outside this
experiment's scope.

## What disposable Pets experiments established

Eight focused JVM tests in two temporary fixtures passed; their worktrees were removed. These
were small Pets models, not edits or integration tests against the full Turmoil bundle. The
experiments showed:

- A party-specific `RANK` refinement over Followers can allow a safe Follower removal and reject
  a removal that would leave the incumbent behind. Delegates in another party did not affect it.
- Gaining a Leader can transmute the former Leader into a Follower while respecting a party's
  `MAX 1 Leader`. The old owner needed explicit binding with `EACH` for the transmutation.
- A player removal of the Leader can promote the sole highest rival Follower in the two-player
  fixture. Admin removal did not promote. The promotion reaction needed `IF MAX 0 Leader` to avoid
  repeated reactions during demotion; `::` by itself did not prevent them.
- A bare abstract `Delegate<Party>` gain or removal still offered both concrete subtypes in the
  fixture. The experiment did **not** establish the desired automatic narrowing. The
  [type-system specification](../type-system-spec.md) describes structural, state-independent
  narrowing; live metrics may instead affect legal task choices. Check the actual task machinery
  before promising a choice-free `Delegate<Party>` instruction.

The fixtures did not cover three or more owners, clockwise selection among tied challengers,
Neutral, the seven-Person reserve, Dominance, scoring, or complete solar cleanup. In particular,
promoting a rival after Leader removal needs an explicit rule for a tied highest count; the
two-player fixture's unique winner does not supply it. The current Turmoil behavior and tests,
including `TurmoilRulesTest` and `BannedDelegateTest` under
[`test/common/dev/martianzoo/tfm/tests`](../../test/common/dev/martianzoo/tfm/tests), remain the
source of truth until a replacement is selected and tested end to end.
