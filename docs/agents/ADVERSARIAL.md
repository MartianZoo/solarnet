# Adversarial play around Solarnet

This is a discussion proposal, not an implementation plan for adding a game server to Solarnet.
**Solarnet remains just a game engine: a glorified calculator.** Given a game state, it answers
what a Player can legally do and what follows from a supplied decision. It has no interest in
stopping the person running it from exploring, changing, or replacing that state.

“Forward model” is an appropriate term for that role. For example, the
[Tabletop Games research framework](https://tabletopgames.ai/assets/pdf/gaina2020tag.pdf)
uses it for the game logic that determines available actions and computes their consequences.
This describes Solarnet's purpose, not a claim that its current API can already enumerate every
possible choice cleanly.

An adversarial game adds an agreement about which decisions actually count. A calculated
`Victory` or `VictoryPoint` says what follows from the inputs. Recognizing that result as an
achievement says something more: somebody made those decisions under agreed conditions. This
applies to solo play as much as multiplayer play.

**That agreement belongs to the people and software using Solarnet.** Repository hosting, player
authentication, acceptance of moves, custody of hidden information, and recognition of results
are outside the engine. The proposal below describes such an arrangement without assigning those
jobs to Solarnet or selecting new engine APIs.

**Every player's local engine is theirs to use freely.** A player can explore alternative moves,
make hypothetical decisions for opponents, run Admin work, try assumed hands, change resources,
and rewind repeatedly. All of that is fair local calculation. Exploring a possible opponent hand
does not entitle the player to learn the actual hand.

The engine still checks game rules: an ordinary action that requires 10 M€ must have its cost
handled correctly. That mathematical check does not establish who is operating the computer or
whether the other participants have accepted the action. Existing corrections and unrestricted
local experimentation do not become misconduct merely because a competitive game is happening
elsewhere. A proposed history can simply fail to qualify as a continuation of that game.

For Actor attribution, a request to select, narrow, or perform FooPlayer's task through FooPlayer's
Agent is sufficient. Solarnet checks task assignment and the modeled rules, without asking who
operates that Agent or demanding evidence of FooPlayer's consent. Any program may call every
Actor's Agent; acceptance of its submitted decisions belongs to the surrounding arrangement.

**The shared record identifies the game being played.** A readable textual record of the game
in progress is committed to Git. “Type 3 export” refers to the combined recording in the project's
export design: a premise recipe, an Actor decision stream, and an exact event stream. The decisions
explain the result; the events record what happened. The historical export note described these as
the third of three views, and the
[current roadmap](../../SOLARNET_ROADMAP.md#game-records-and-reconstruction) retains that direction.
The combined format remains unspecified and unimplemented. A state digest alone would not replace
the exact events needed for its independent passive playback.

Each participant has the same agreed rules and engine implementation available locally. The game
records enough setup and version information to reproduce its meaning. Upgrading the rules during
a game would itself need agreement; two calculations from different rules are not a fair test of
whether somebody's submitted move is correct.

The *play-tip* is the commit participants currently recognize as the live game. A player starts
there, makes decisions locally, appends them to the export, and pushes a candidate commit. A
personal branch or repository can serve as the submission area. A pull-request-like review and
acceptance can promote that candidate into the shared history.

Pushing a candidate and having it accepted are distinct events. A branch name or Git author field
does not, by itself, establish the participants' consent. The hosting and submission arrangement
must connect decisions to their accepted decision-makers. Exactly how acceptance is recorded is
still a design choice outside Solarnet.

Before continuing from a candidate, participants or their software check that:

- It continues the agreed play-tip and preserves the existing export as an unchanged prefix.
- Its new decisions belong to the appropriate players, or have their agreed authorization.
- Replaying those decisions with the agreed rules produces the claimed consequences and pending
  work. An extra reward cannot be justified merely by appending a resource adjustment.
- The separate card records validate every identity-dependent claim and required discard report.
- Any simultaneous group is complete before it becomes accepted progress.

Git ancestry alone does not establish the unchanged-prefix property: a later commit can modify
earlier lines. Similarly, an event log that can be played back is not proof that its events follow
from legal decisions. The combined export's decision and event streams must reconstruct the game
independently and agree, rather than use asserted consequences to repair missing decisions.

An accepted prefix must also have a settled meaning. A later append cannot retrospectively change
which choice an earlier line represented or who was waiting to act. This is stronger than keeping
the earlier text unchanged. It does not require verbose task identifiers, but the importer must
establish the accepted position, including pending work, without relying on future decisions.
If players later agree to change the rules revision, that agreement must say how the old position
is carried forward; replaying all old decisions under the new rules would answer a different question.

A rejected candidate does not advance the play-tip. Players keep their copies of the last accepted
position and can explain the rejection. Play may remain blocked until a satisfactory proposal or
an agreed change appears. This arrangement deliberately permits refusal; it does not guarantee
that somebody who is losing will cooperate. Refusing a valid continuation and identifying an
invalid continuation are different facts, even if both stop the game.

**A commit may stop in the middle of an operation.** Committing does not mean declaring a turn,
action, or operation complete. Pending tasks are part of the position that the next participant
must reconstruct. Players can ordinarily batch local decisions until somebody else's choice or
newly available information is needed. They may submit smaller valid prefixes too.
Acceptance checks the legality of that prefix without pretending that outstanding payments or
mandatory tasks have been completed.

Icy Impactors is a concrete example already represented in
[the card definition](../../src/common/dev/martianzoo/tfm/canon/PromoCardPack/cards.json5) and
[its scenario test](../../test/common/dev/martianzoo/tfm/tests/cards/IcyImpactorsTest.kt):
the Start Token holder chooses the ocean location, while placement is attributed to the card owner.
The owner must be able to commit the work leading to that choice, receive the other player's
decision, and continue the same operation.

There is an unresolved difficulty here: an accepted partial operation might later have no legal
completion. Local transaction rollback cannot retract another player's accepted decision or make
somebody forget a revealed card. The current
[sequencing discussion](SEQUENCING.md#the-missing-rule-when-an-operation-is-over)
already distinguishes an individual transaction from completion across delegated work. The
surrounding agreement must address this case without assuming those are the same thing.

Finding a possible successful continuation would not by itself settle the problem. That
continuation might depend on another player's choice or an unknown draw. Requiring every proposal
to prove future completion would also be a substantial new verification requirement. No such
search requirement, automatic takeback, or forfeiture rule is selected here.

`Admin` is an Actor in the model, not necessarily a separate person, machine, branch, or committer.
Any player's submission may include appropriate Admin work performed by their local engine. Public
rule consequences remain independently checkable. If Admin work includes an actual discretionary
choice, the players must agree who supplies it; calling it Admin does not give the submitting
player permission to decide it unilaterally. Whoever keeps the deck secret has a separate job from
the engine's Admin Actor.

A determined result is not necessarily work that may be performed immediately: choosing when to
execute it can affect other available choices. For acceptance as shared play, batching another
Actor's work needs that Actor's authorization or an agreed rule that preserves those choices;
this adds no permission check to local Agent calls. Likewise,
an Admin input representing chance needs an externally established outcome. Public random results
need this provenance just as private draws do. The evidence can accompany a player's submission;
this does not require a separate dealer-authored commit for every outcome.

**Drawing commits the decision before revealing the information.** Undisclosed card identities and
deck order stay outside every player's inspectable environment. Identity tracking remains separate
from the engine export. The engine knows anonymous card counts and relevant locations. A deck
custodian can validate identity claims and bless a proposed continuation without publishing other
players' hands.

For example, a draw followed by a choice would proceed as follows:

1. The player submits the decisions that cause the draw, with any required costs and pending work.
2. The candidate is checked and accepted as the new play-tip, with its matching external card
   allocation fixed. No card identity has yet been returned to the player.
3. The custodian releases the drawn identity to that player. Retrying this request returns the
   same allocation; it does not consume or sample another card.
4. The player can now calculate and submit choices that depend on the new information.

This is the intended meaning of “committed before retrieval.” A local commit, an unaccepted push,
or a plausible candidate on a discarded branch must not buy a look at the deck. Otherwise a player
could preview many branches and submit only the useful result. The acceptance evidence and private
allocation must refer to the same game, accepted history, and draw occurrence; the exact storage
and recovery procedure is still undecided.

The corresponding rule for discards runs in the other direction. Before a commit containing
discards is accepted, the player must report their identities to the external tracker. It checks
that the player held those cards in the relevant location and that the removals match the export.
Whether those names are public or privately reported depends on the agreed visibility rules.
Playing a named card likewise requires evidence that this is the card the player actually held.

Look-and-choose offers and externally checked rewards need the same discipline. An offer becomes
binding before its faces are disclosed; the subsequent keep, purchase, or discard choices follow
that disclosure. The custodian verifies claims that depend on hidden faces, including a claimed
reward for a qualifying revealed card. Anonymous counts alone cannot prove those claims.

The custodian's job can consequently require rules calculations as well as card bookkeeping.
For example, checking a claim that an offered card cannot be played requires knowledge of the
offer and the relevant game position. A failed attempt at a particular play does not establish
that every available way of playing it fails. The scope of these external checks needs to be made
explicit before treating a blessing as evidence that all relevant claims were verified.

Each player can therefore check the public rules with their own engine, but cannot independently
check every hidden fact during play without further evidence. Trusting a custodian is the smallest
starting hypothesis. Removing that trust would be a different, substantially larger protocol
project. Neither choice requires making the engine responsible for secret custody.

**Simultaneous choices become accepted together.** The proposed Git representation is a merge
commit joining all participating submissions, with no intermediate submission recognized as the
next live position. Their combined export must preserve the previously accepted prefix and contain
the complete group. Proposal parents in the Git graph need not themselves be accepted positions.

A merge commit records combined histories; it does not prove that the choices were independent
or combine game decisions correctly. Git even allows the resulting content to be edited before
the merge is committed. See the [Git merge documentation](https://git-scm.com/docs/git-merge).

Proposals calculated separately from the same play-tip also need verification together. A textual
merge cannot establish that their combined consequences are correct. Replaying the submitted
decisions can produce a verified combined record that is still stored as a Git merge commit.
If resolution order affects the outcome, that order must come from the agreed game rules; incidental
seat order or a merge tool must not silently decide it.

If Alice's choice must be fixed before she learns Bob's, merely displaying only the final merge
is insufficient when Bob's proposal was readable earlier. Private submissions to a trusted
collector, or a commitment followed by later disclosure, are possible approaches. The requirement
to hide choices until all are fixed is separate from accepting their effects together. We have
not yet established which currently supported situations need this treatment; drafting is a
possible future example, not a selected implementation task. Setup selections and research
purchases are also candidates to examine, without assuming that every part of those procedures
must resolve simultaneously or that their current engine representation settles the question.

**Undo is an agreement about which history to continue.** Players may choose any earlier position,
alternative branch, or mutually agreed correction as their new starting point. They may preserve
the deck order or arrange a reshuffle. Solarnet need not approve that choice. Each client simply
follows the history that its participants recognize. A participant who does not agree can refuse
to continue, leaving the shared game blocked.

The external card records must follow the same agreement: changing a Git ref alone does not
restore a deck cursor or somebody's former hand. More fundamentally, returning to an earlier
position does not erase information already learned. A reshuffle also does not make somebody
forget an opponent's revealed card. Those facts can be accepted as part of the agreed continuation;
they should not be mistaken for an untouched first attempt.

For a solo score or a result shown to outsiders, this exposes a useful distinction. The final
history can demonstrate a valid played sequence without demonstrating how many abandoned attempts,
revealed futures, or agreed takebacks preceded it. What counts as an achievement depends on what
the participants or audience expect the result to establish. Git gives them a history to discuss;
it does not supply those expectations.

**Encrypted identity records are an option, not an engine representation.** The separate card
record could include encrypted faces alongside public identities. It could live beside the export
in the repository while remaining logically separate. A fresh encrypted entry for a later event
would preserve the earlier entry; hiding links must not require rewriting the accepted prefix.

Use established authenticated encryption with correctly generated fresh nonces, rather than
treating a changing plaintext prefix as a complete encryption design. Nonces distinguish separate
encryptions and must not repeat under the same key. Symmetric authentication establishes integrity
among key holders; it cannot prove which holder authored a statement. See the
[libsodium documentation](https://doc.libsodium.org/secret-key_cryptography/secretbox).

Several consequences follow for this proposal:

- A player can immediately decrypt anything encrypted under a key they already possess. Future
  draws cannot be published under that key before acceptance. They must remain unavailable or
  protected by unreleased key material.
- Fresh ciphertext can avoid a direct equality comparison, but public card handles, hand slots,
  lengths, or ordering can still expose a draw-to-discard relationship. The complete visible
  record needs examination. Some deductions are inherent: a player holding a single card who
  discards it cannot conceal that relationship.
- Private tracking still needs to connect the actual card across events, even when other players
  must not see that connection. The custodian can maintain and attest to it.
- Revealing keys later makes retained ciphertext readable. It does not by itself prove that the
  reported hands were valid, the initial deck was fairly chosen, or information was withheld until
  the right moment. Auditing those claims requires the corresponding earlier evidence.

**The current code provides ingredients, not this complete arrangement.**
[Card-tracked replay support](../../test/common/dev/martianzoo/tfm/tests/replays/CardTrackingFullGameTest.kt)
already names hand movements outside the World. It is test support, not a production dealer, and
does not establish complete tracking of every offer or deck movement.
[GameRecording](../../src/common/dev/martianzoo/state/GameRecording.kt) and
[its JSON encoding](../../src/common/dev/martianzoo/state/GameRecordingJson.kt) support passive event
playback. That does not establish an appendable decision format, independent move verification, or
resumption of arbitrary unfinished operations. The
[existing export direction](../../SOLARNET_ROADMAP.md#game-records-and-reconstruction)
already distinguishes decision reconstruction from exact event playback and records unfinished work.

The next useful design work is to settle the acceptance and information-release agreement, then
work through a draw, a named discard, an Icy Impactors handoff, and an agreed undo on paper. Any
resulting need for better decision export or reconstruction should be evaluated as an ordinary
calculator capability. Repository control, secret dealing, player consent, and certification of
achievement remain responsibilities of the surrounding arrangement.

**Five questions, ranked by how much their answers could change the design:**

1. **Who must be trusted with the hidden information?** Is a trusted deck custodian sufficient,
   provided every player can independently verify public consequences? Or should players eventually
   be able to detect the custodian choosing favorable draws, substituting cards, or otherwise
   manipulating the game? Those expectations lead to substantially different systems.

2. **Whose acceptance makes a draw binding?** Suppose the custodian validates Alice's commit,
   advances the shared branch, and reveals her card—but Bob subsequently rejects that commit.
   Has Alice already made a binding move? This determines whether “blessed by the custodian” and
   “accepted by the players” must happen together, or represent different kinds of agreement.
   What if accepted work later cannot be completed, after a card was revealed or another player
   made a choice inside that operation?

3. **Can somebody veto a victory by refusing to accept the final move?** Blocking further play is
   straightforward, but a winning move may require no further play. Should the other participants'
   approval establish the result, or should previously agreed verification rules establish it
   despite their refusal? This separates consent to participate from consent to lose.

4. **What should a result tell somebody who wasn't there?** Your unrestricted, mutually agreed
   undo remains intact. But should a solo victory distinguish “succeeded on the first attempt”
   from “succeeded after seeing future draws and returning to an earlier position”? Both can be
   valid achievements; the final surviving branch alone may not communicate which achievement
   occurred.

5. **What exactly must be simultaneous: acceptance, or commitment before seeing others' choices?**
   If Bob's proposal is readable before Alice submits hers, accepting both through a merge still
   lets Alice respond to Bob. Must submissions therefore remain secret until everybody's choice
   is fixed? And if somebody then refuses to finish, do the remaining players get to learn the
   submitted choices?
