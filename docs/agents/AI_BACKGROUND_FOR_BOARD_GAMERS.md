# Teaching Computers to Play Big Hidden-Information Games

> **Read when:** a concise explanation of the AI-player opportunity, limitations, or recommended
> first goal is enough.
>
> **Skip when:** making source-backed research claims or concrete architecture/evaluation choices;
> use [AI_BACKGROUND.md](AI_BACKGROUND.md).
>
> **Status:** research overview current through 2026-08-08.

This is the board-gamer's version of the more detailed
[research survey](AI_BACKGROUND.md). It assumes familiarity with games, not with how computer
players are built.

## The short answer

Computers can now play several extremely difficult hidden-information games at expert or better
levels. They have done it in poker, Stratego, Mahjong, DouDizhu, Diplomacy, restricted Hearthstone,
and competitive Pokémon.

That does **not** mean a computer can be shown any new Terraforming Mars card, read it as a person
would, and immediately use it at tournament strength. No published system has demonstrated that.

The evidence supports a narrower and more useful claim:

> If a new card is made from game ideas the computer has practiced before, a well-designed player
> may use it sensibly without practicing that exact card. If the card creates a genuinely new rule,
> assume the computer will need new instruction and more practice.

For example, suppose a computer already understands costs, tags, plants, production, and raising
oxygen. It has a fair chance of valuing a new card that combines those familiar ingredients in new
numbers. A card that changes who takes the next turn, hides a new kind of information, or creates a
new kind of triggered action is a different problem. Similar-looking words do not guarantee that it
will understand the new timing or strategic consequences.

## What teaching a computer player looks like

It helps to picture a very fast board-game club.

First, the club needs a flawless referee. The referee sets up games, shuffles, shows each player only
what that player is allowed to see, lists the legal choices, carries out card effects, and scores the
result. For Solarnet, the game engine should be this referee. The computer player should not have to
guess whether a play is legal or what a card physically does.

Next, the club plays an enormous number of practice games. After each game, it works backward: which
positions and choices tended to lead to winning, and which led to losing? At first it may copy a
reasonable house player or a simple look-ahead player. Later, different copies of the club's player
practice against one another.

The club must keep old opponents around. If it practices only against its latest style, it can enter
a strange local fashion: everyone at the club learns to punish one popular opening and forgets how
to handle older or unusual plans. A good practice league includes old champions, aggressive and
defensive players, simple house players, and players trained specifically to find the champion's
weaknesses.

Finally, the player may look ahead during a real game. It can ask the referee to try several legal
continuations and compare them. Hidden information makes this delicate. A player cannot privately
imagine three possible opponent hands and then choose a different move for each one; at the real
table, it must choose one move without knowing which hand is true. Looking ahead must preserve that
uncertainty.

## Why Terraforming Mars is an unusually hard combination

Many famous game-playing results solve only some of the difficulties that occur together in
Terraforming Mars:

* Opponents' hands and the future deck are hidden.
* An early card can change the value of choices many generations later.
* Hundreds of cards do meaningfully different things, rather than being copies of a few pieces.
* Cards form engines: the value of one card depends heavily on tags, discounts, production, stored
  resources, and future draws.
* A choice may include a card, a payment method, a target, and a board space.
* Players compete over milestones, awards, map space, and when the game ends.
* In games with three or more players, hurting one opponent can help another. This is not the simple
  duel found in chess or heads-up poker.

A huge number of possible games is not the whole story. Poker has an enormous number of possible
deals and betting histories, but every card still belongs to one standard 52-card deck. Terraforming
Mars has fewer ways to shuffle cards but far more kinds of card text and long-term interactions.

## What the strongest examples really prove

### Fixed games can be mastered

DeepNash reached strong human play in Stratego, where the opponent's piece identities are hidden and
games can last hundreds of moves. A newer player called Ataraxos went much further in 2025: in a
20-game match against the most decorated Stratego player, it recorded 15 wins, four draws, and one
loss. It practiced from scratch, learned to estimate the possible identities of hidden pieces, and
looked ahead through many plausible arrangements before moving. Strong computer players have also
beaten professionals in six-player poker, ranked above nearly every player on a major online Mahjong
ladder, and mastered the complicated card combinations of three-player DouDizhu.

These are important results. They show that a computer can learn bluffing, concealment, risk, and
opponent uncertainty through practice. They do not show that it can learn an unfamiliar rule while
the game is underway. Each player practiced one fixed game whose pieces, cards, and rules were known
in advance.

### A large known card library can be learned

A 2023 Hearthstone player practiced with more than 350 implemented cards. It learned both deck
construction and play and beat one strong human in a small set of matches. The practice effort was
enormous: many millions of games' worth of choices on a large bank of computers. Every card belonged
to the known practice pool, and the study did not test newly introduced cards.

Competitive Pokémon is the closest current comparison to an encyclopedia-sized card game. A player
must cope with more than a thousand creatures plus many moves, abilities, items, hidden team members,
and chance effects. Recent specialist players trained on millions of recorded or practice battles
can reach roughly the strongest tenth of the human ladder in some formats. A 2025 competition made
them better, but the report still finds a gap from elite humans. These players learn the particular
Pokémon world; they have not proved that they can understand a new kind of move or ability on sight.

### New cards made from familiar parts are much easier

Legends of Code and Magic provides the cleanest success. Before every game it generates a fresh pool
of 120 cards, so a player cannot memorize every card by name. Winning players learned to judge the
cards from familiar properties such as cost, attack, defense, keywords, and a small selection of
effects. They became strong at cards they had literally never seen.

There is an important catch: the game is deliberately much smaller and more regular than
Terraforming Mars. Its new cards are like new recipes made from a short pantry list. The generator
does not suddenly invent a new phase, a new timing exception, or a new sort of ownership.

A Hearthstone study called Cardsformer performed a more direct test. It withheld simple cards during
practice and later put as many as thirty unseen cards into a deck. The player remained useful, but
its results declined as more cards were replaced. With every card replaced, it won 47.5% against
the comparison player. More importantly, the test deliberately avoided cards with unique triggers.
The player could understand a new combination of direct damage and healing, but failed on an unusual
effect that copied a spell whenever one was played.

A Magic: The Gathering study learned from 75 million human draft choices involving 2,990 cards. It
could often predict human picks from an entirely withheld set. That is strong evidence for judging
an unfamiliar card's draft value from its properties and wording. It did not play matches or resolve
the interactions in Magic's rulebook.

## Four meanings of “a card the computer has never seen”

Claims about unseen cards are easy to misunderstand. These four cases are progressively harder:

1. **A familiar card in an unfamiliar situation.** The card is known, but the hand, board, deck,
   opponents, or timing is new. Strong specialist players already do this.
2. **A new name with familiar properties.** The cost, tags, numbers, and standard effects are known,
   but their combination is new. Procedurally generated card games show that this can work well.
3. **A new recipe using familiar game actions.** The card joins several known effects or conditions
   in a new way. Current systems show partial success, especially when the referee supplies the legal
   choices and exact outcome.
4. **A new rule.** The card introduces a new trigger, timing window, target relationship, resource,
   hidden fact, or exception. There is no convincing demonstration of immediate expert play for
   this case in a game as rich as Terraforming Mars.

The dividing line is therefore not whether the card's *name* appeared in practice. It is whether the
card is written in a game language the player already understands.

## The sensible design for Solarnet

The engine should remain the referee and rulebook. It should reveal a private view for each seat,
never the master copy of the game. It should also hand the player a list of legal choices. This
prevents accidental peeking and prevents the player from inventing illegal moves.

Cards should be described to the player as structured recipes. For example, the description should
distinguish:

* cost, requirements, and tags;
* immediate gains or losses;
* production changes;
* board placement and targets;
* effects that remain in force;
* actions usable once per generation;
* what event sets off a later effect;
* resources stored on the card; and
* end-game points.

Card names and printed wording can be included, but they should not be the only information. A
structured recipe lets knowledge learned from one plant-production card help with another. It also
keeps apart effects that use the same words but occur at different times or belong to different
players.

During practice, a helper may see the full deal and use it to give better lessons, much as a coach
can review every player's hand after a game. The player that sits at the real table must never
receive that hidden information. This separation needs to be enforced by the game interface, not
merely promised by the training procedure.

The player should also remember the visible history. An opponent's earlier passes, purchases,
placements, tags, and tempo can change what their hidden hand is likely to contain. The current board
alone does not tell the whole story.

## How we would know it is genuinely good

Winning against its own practice partner is weak evidence. A serious test should include:

* simple house players, look-ahead players, old champions, and players built to exploit weaknesses;
* experienced humans, with all games and the humans' current strength reported;
* the same deals played again with seats exchanged where possible;
* separate results for each player count and option set;
* unusual strategies such as fast terraforming, slow engines, board denial, and milestone races;
* cards withheld at random;
* whole families of effects withheld together; and
* checks that renaming or reordering cards does not fool the player.

Holding out a whole family is the revealing test. If practice includes ten cards that raise oxygen
and the test hides an eleventh, success may only show resemblance. If practice contains no
“whenever another player does X” effects and the player later handles one well, that is much stronger
evidence that it understands the recipe.

Results should be reported as wins and finishing places, not just high scores. In Terraforming
Mars, scoring more while allowing an opponent to score even more is not success. Tests should also
report how long each decision takes and confirm that no private information reached the player.

## A realistic first goal

A strong first milestone would be a two-player computer that:

* sees exactly what its seat is allowed to see;
* chooses only from options supplied by the referee;
* reliably beats the project's current simple and look-ahead players on exchanged deals; and
* loses little strength when tested with a meaningful group of cards it never practiced, provided
  those cards use familiar game ingredients.

The next test should withhold an entire kind of effect. That will reveal where “new card” stops and
“new rule” begins. Only after measuring that divide should we expect printed card wording or a
general-purpose text-reading assistant to close the gap.

## Bottom line

The best route is not to make the computer memorize every card, and it is not to trust it to
interpret any prose correctly. Give it an exact referee, describe cards using shared game
ingredients, let it practice against a varied club of opponents, and test it on cards and whole
effect families deliberately kept out of practice.

That approach has good evidence behind it for unfamiliar combinations of familiar ideas. Immediate
competitive understanding of genuinely new rules remains an unsolved research problem.
