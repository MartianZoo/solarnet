# AI Background: Competitive Play in Complex Hidden-Information Games

Research current through **2026-08-08**. This is an engineering research note, not a claim that the
field has agreed on a single definition of “complexity” or “human level.” The companion
[board-gamer overview](AI_BACKGROUND_FOR_BOARD_GAMERS.md) explains the conclusions without assuming
AI knowledge.

## Executive conclusion

There is no demonstrated system that can be handed an arbitrary, previously unseen,
Terraforming-Mars-style card with a genuinely new effect and then play a comparably complex game at
a competitive human level. There are strong results on each half of that problem, but not on both at
once:

* Fixed-game specialists have reached expert or superhuman play in very large hidden-information
  games: Stratego, multiplayer poker, Mahjong, DouDizhu, Diplomacy, and restricted versions of
  Hearthstone and competitive Pokémon.
* Card-aware representations can transfer to unseen content when the content is composed from
  familiar attributes or effect patterns. The best direct evidence comes from procedurally generated
  cards in Legends of Code and Magic (LOCM), simple held-out Hearthstone effects, and held-out Magic:
  The Gathering cards during drafting.
* None of those unseen-content results establishes robust handling of a new trigger, timing rule,
  target structure, resource, or other rule-changing mechanic during strong full-game play.

The practical answer for Solarnet is therefore a hybrid:

1. Keep legality, card execution, random events, and scoring in the exact engine.
2. Give the learned player a player-relative observation and the engine-generated legal choices.
3. Represent cards and choices by shared, typed game semantics, not only by card identity.
4. Train on the exact game, but deliberately hold out cards, corporations, combinations, and whole
   effect families to measure what actually transfers.
5. Expect a new card composed only from known effect primitives to need little or no new strategic
   training. Expect a new primitive or unusual interaction to require engine work and usually some
   fine-tuning or renewed self-play.

Raw card text is useful auxiliary information, not a dependable replacement for the canonical rule
representation. The research supports language as a way to improve similarity and sample efficiency;
it does not support trusting a language model to adjudicate arbitrary new card rules during
competitive play.

## What was researched

### Inclusion standard

The main comparison set was limited to discrete or turn-based games with private state, hidden
orders, hidden identities, or hidden hands/decks. Perfect-information achievements such as chess and
Go are mentioned only where they explain a method. A system received the most weight when it had at
least one of the following:

* a state/action/search space plausibly comparable to or larger than Terraforming Mars;
* long games, multiplayer interaction, stochasticity, and/or a large amount of heterogeneous game
  content;
* evaluation against strong humans or a mature agent competition;
* a controlled held-out-card or held-out-content experiment.

Smaller card games such as LOCM were retained only where they test a question that larger games have
not tested, especially procedural cards. Draft-only Magic work was retained for the same reason. A
large tree size by itself was not treated as equivalence to Terraforming Mars: poker can have an
enormous state space while still having a tiny vocabulary of card effects, and a game with hundreds
of unique effects can be hard in a different way.

### Search process

The survey started with the direct Terraforming Mars implementation in the Tabletop Games (TAG)
framework and followed its references and later PyTAG work. It then searched across four partly
disjoint literatures:

1. imperfect-information game solving and self-play (CFR descendants, ReBeL, Student of Games,
   Ataraxos/DeepNash, poker, Mahjong, DouDizhu, Diplomacy);
2. collectible/deck-building card-game agents and competitions (Hearthstone, LOCM, Tales of Tribute,
   Magic);
3. content generalization (held-out cards, procedural cards, language-conditioned policies,
   generalized card features, rules supplied as text);
4. 2025–2026 work on Pokémon, multi-game language-model fine-tuning, and new Magic benchmarks.

Queries combined game names with terms such as `imperfect information`, `hidden information`,
`self-play`, `human-level`, `competition`, `unseen cards`, `new cards`, `language grounding`,
`procedural cards`, `generalization`, `exploitability`, and `rules`. Primary papers, official
proceedings, and project pages were preferred. Survey and secondary sources were used to discover
work, not to raise a strength claim beyond its original evaluation.

For every important result the audit asked:

* What exact ruleset—or, in the aspirational model, Authority—and content pool was implemented?
* Was the evaluation against humans, agents, or the system's own checkpoints?
* Was it head-to-head, statistically substantial, and free of privileged information at play time?
* Did “generalization” mean new card identities, new combinations, new descriptions, new decks, new
  opponents, new rules, or a different game?
* Did an exact simulator already know the held-out card?
* Was this a peer-reviewed full paper, a short extended abstract, or a recent preprint?

This process found no later competitive Terraforming Mars agent paper beyond the TAG/PyTAG line.
That absence is not proof that no private or unpublished bot exists; it is evidence that there is no
publicly substantiated state-of-the-art result to build on.

Contract bridge, Skat, Gin Rummy, Hanabi, and social-deduction work were also checked. They offer
useful techniques for partnership, opponent-hand inference, conventions, and reasoning about what
other players know. They were not promoted into the main comparison set because they use a fixed
ordinary deck or a small role/card vocabulary and do not test heterogeneous new content. The 2024
[reproducible bridge-bidding baseline](https://arxiv.org/abs/2406.10306), for example, improves on
the champion WBridge5 program in bidding experiments, but is not an end-to-end new-card result.

## The difficulty is not one number

Terraforming Mars combines several kinds of difficulty that the literature often studies
separately:

* **Partial observability:** opponents' hands and the future deck order are hidden.
* **Long credit assignment:** an early discount, production increase, or tag can matter many
  generations later.
* **Variable legal choices:** a turn can involve passing, projects, standard projects, board
  placements, payments, targets, and follow-up choices.
* **Heterogeneous content:** card consequences are not permutations of one small rule. They include
  immediate effects, production, persistent modifiers, conditional triggers, stored resources,
  actions, requirements, tags, placement, and end scoring.
* **Combinatorial engines:** much of a card's value comes from the player's existing engine and the
  remaining content distribution.
* **Multiplayer and shared tempo:** players compete over board space, milestones, awards, cards, and
  the time at which global parameters end the game. Maximizing raw score is not a two-player
  zero-sum problem.
* **Chance and opponent diversity:** cards, board opportunities, corporations, and human styles
  change the useful strategy distribution.

This matters because the strongest theoretical guarantees in the literature generally apply to a
fixed, two-player, zero-sum game. Terraforming Mars at three or more players is outside that clean
setting. Even a two-player version retains a much richer content vocabulary than poker, Stratego,
Mahjong, or DouDizhu.

## Evidence map

“Unseen content” below means content excluded from the agent's task-specific game training. It does
not imply that a pretrained language model had never encountered the name or rules on the public
internet.

| System/domain | Why the game is relevant | Best strength evidence | Exact-content dependence | What it establishes |
|---|---|---|---|---|
| TAG Terraforming Mars (2021) | Exact target game; hidden hands/deck, long play, 208 projects | MCTS beat simple baselines; comparison with aggregate historical human scores was not head-to-head | Full engine implements each supported card; no held-out-card test | A usable benchmark and forward model, not competitive-human AI |
| Ataraxos / DeepNash / Stratego (2022–2025) | Roughly `10^535` game tree, `10^66` deployments, hidden identities, hundreds of moves | Ataraxos beat the most decorated player 15–4–1 and went 38–2 in a World Championship demo; DeepNash preceded it with strong online results | Fixed twelve piece types and fixed rules | Self-play plus sampled-belief lookahead can master a huge fixed hidden-information game; no content transfer is tested |
| Pluribus / six-player poker (2019) | Multiplayer, deception, hidden cards, general-sum complications | Defeated elite professionals in controlled experiments | Standard 52-card semantics and fixed betting rules | Self-play plus limited lookahead can be superhuman in multiplayer hidden-information play |
| Student of Games / ReBeL (2020–2023) | Principled search with hidden information | Superhuman heads-up poker; strong Scotland Yard | A separately trained, game-specific model and interface for every game | One algorithm can span games; one trained player cannot automatically do so |
| Suphx / Riichi Mahjong (2020) | Four players, stochastic draws, rich scoring, long hidden-information play | Rated above 99.99% of ranked Tenhou players | Fixed 34 tile identities; separate decision models and substantial task-specific training | Human data, self-play, privileged training, and adaptation work at scale |
| PerfectDou / DouDizhu (2022) | Three players, cooperation/competition, large variable card-combination action set | Beat prior public agents | Fixed ordinary deck and rank-combination grammar | Scoring structured legal actions and training with a full-information critic are highly effective |
| ByteRL / restricted Hearthstone (2023) | 350+ implemented cards, deck building plus battle, hidden hands/decks | Normal agent won two 3–0 best-of-five series against one strong human; a privileged variant won two more | Card embeddings and self-play cover the fixed old card pool; no unseen-card test | A specialist can learn strong end-to-end deck construction and play, at very high training cost |
| Cardsformer / restricted Hearthstone (2023) | Directly tests language representations and held-out cards | Beat 2020 competition winner with 0–20 unseen simple cards; 47.5% with all 30 unseen | Held-out cards deliberately excluded unique triggers; engine still supplied legal moves | Familiar effect descriptions can transfer; arbitrary effects cannot yet |
| LOCM 1.5 / ByteRL (2022–2023) | A new pool of 120 procedurally generated cards every game | Clear winner of both 2022 competition tracks | Cards come from a fixed structured grammar of stats, keywords, and deterministic effects | Strong play on never-before-seen *combinations of known primitives* is practical |
| Metamon and PokéAgent / competitive Pokémon (2025–2026) | 1,000+ species, many moves/items/abilities, hidden opponent team, stochastic games up to 100+ turns | Metamon variants reached roughly the top human decile; 2025 competition winners improved further | Specialist models tokenize exact game vocabulary and train on millions of exact-game trajectories | Current strongest analogue for encyclopedic fixed content; elite play and robust transfer remain open |
| PokéChamp / competitive Pokémon (2025) | Same large content universe, with language-model knowledge plus minimax lookahead | 84% vs strongest rule bot; actual ladder run crossed 1300 after 50 games, with a projected 1300–1500 excluding timeouts | No extra fine-tuning, but prompts, a simulator, damage tools, historical team data, and pretrained Pokémon knowledge | A general language model can be a useful search guide, but latency, stale knowledge, and exploitability matter |
| Magic draft representation (2024) | Thousands of semantically varied cards and genuinely new set releases | Predicted 55.44% of human picks for a completely held-out set after training on 2,990 cards / 75M decisions | Uses shared features, text, images, and usage metadata | Strong unseen-card *valuation* during drafting, not rules execution or competitive battle play |
| Multi-game LLM fine-tuning (2025) | One architecture trained on eight hidden-information card games | Approached teacher agents in DouDizhu, Guandan, and Mahjong after supervised fine-tuning | Up to 1M winning teacher decisions per complex game; no new-effect test | Architecture reuse is real, but strategy still comes from game-specific examples |
| Tales of Tribute (2023–2025) | Hidden decks/hands, in-match deck building, long-term planning | PPO competitive with established MCTS agents | Fixed patron/card implementation; no human or unseen-card benchmark | Useful modern-tabletop testbed, but explicitly smaller than Hearthstone |
| MTG-Causal-RL (2026 preprint) | Real Magic engine, partial observation, 478 masked actions | PPO variants beat random/heuristic baselines in five fixed archetypes | Fixed archetypes; leave-one-archetype gaps, not new-card mastery | Promising benchmark and evaluation design, not a competitive-Magic result yet |

The table intentionally does not call all rows “state of the art” in one shared ranking. They answer
different questions under incompatible games and evaluation protocols.

## Direct evidence: Terraforming Mars is still an open benchmark

The most relevant published work is Gaina, Goodman, and Perez-Liebana's
[TAG: Terraforming Mars](https://ojs.aaai.org/index.php/AIIDE/article/view/18902) (AIIDE 2021).
Their implementation covers the base game plus Corporate Era: 208 project cards and 12 corporations.
Four unusually conditional or temporary cards were not fully implemented. Opponent hands and deck
order are hidden.

The paper measured a representative random two-player game at roughly 400 state components and 460
decisions. The mean immediate legal-action count was 7.1 and the observed maximum was 46. Those
numbers understate the strategic space because the engine decomposes playing a card into later
payment, target, effect, and placement choices. A shallow search can select the first step without
reaching the decision's important consequences.

The evaluated agents were random, one-step lookahead, and forward-model planning methods including
MCTS. The reported MCTS configuration used a rollout depth of ten and a budget of 1,000 forward-model
calls. MCTS was the strongest tested agent, but the evidence does **not** show competitive human
strength:

* historical human aggregate scores were used as a rough external comparison, not head-to-head
  games under controlled settings;
* score distributions across different player populations and settings are not equivalent to win
  rates;
* none of the agents beat the solo scenario;
* the authors describe efficient training and stronger AI as open work.

The later [PyTAG paper](https://arxiv.org/abs/2405.18123) exposes TAG games to reinforcement-learning
tooling and demonstrates PPO self-play on smaller games such as Love Letter, Exploding Kittens,
Stratego, and Sushi Go. It does not train on Terraforming Mars. The paper explicitly leaves the more
complex games for future work. Thus TAG/PyTAG provide useful infrastructure and baselines, not an
answer to the competitive Terraforming Mars problem.

## What the strongest fixed-game systems teach

### Robust self-play must address a moving opponent

Ordinary self-play is unstable: every improvement changes the training problem, strategies can
cycle, and a player can become excellent against its current partner while forgetting how to handle
older styles. The strongest systems use one or more of:

* a population or archive of old opponents;
* explicit best-response iterations such as fictitious play;
* equilibrium-oriented updates;
* randomized or mixed policies, which are essential when predictable play is exploitable.

[Ataraxos](https://arxiv.org/abs/2511.07312) is the current clearest board-game example. This 2025
preprint reports a 15-win, four-draw, one-loss match against the most decorated Stratego player, then
38 wins and two losses against World Championship attendees. It trained separate setup and move
policies through tabula-rasa self-play. At play time a learned belief model samples hidden piece
configurations, and a 40-ply, 1,000-rollout search averages each candidate move across those samples.
It used 16 H100 GPUs for one week for the playing models and four H100s for four days for the belief
model; the reported live search averaged 1.26 seconds per move.

[DeepNash](https://arxiv.org/abs/2206.15378) is the important precursor. It used Regularized Nash
Dynamics and model-free self-play, without search or an explicit opponent belief model, and reported
84% wins against expert humans plus top-three Gravon rankings. Ataraxos's authors argue that those
opponents were not elite and that the ranking pool was small, which is why the later controlled
match is stronger evidence.

Both results show that a value network can learn the strategic value of concealing or revealing
information. However, their networks, observations, action structures, and legal masks were designed
for Stratego's fixed twelve piece types. They are algorithmic lessons, not reusable trained players
or card-generalization results.

ByteRL uses Optimistic Smooth Fictitious Play in both LOCM and Hearthstone. It is especially relevant
because it learns drafting/deck construction and battle as one trajectory. Its success supports
population-aware self-play for Solarnet, but the follow-up
[exploitability study](https://arxiv.org/abs/2404.16689) is an important warning: a simple cloned
policy, then a best-response fine-tune, could beat the published LOCM agent on hundreds of fixed deck
pools. Tournament wins and a tiny human match do not prove that a strategy is robust.

### Hidden-information search is not ordinary MCTS

Sampling one possible opponent hand, pretending it is real, and running perfect-information search
creates two classic errors:

* **strategy fusion:** the search effectively chooses a different action for private worlds the
  actual player cannot distinguish;
* **nonlocality:** the value of a current action depends on what earlier play has signalled and how
  the opponent's beliefs will change, not only on the sampled current state.

Information-set MCTS and repeated determinization can be useful approximations, and competition
agents use them, but more principled systems search over beliefs and information sets.
[ReBeL](https://papers.nips.cc/paper_files/paper/2020/hash/c61f571dbd2fb949d3fe5ae1608dd48b-Abstract.html)
combines self-play learning with public-belief-state search and has a two-player zero-sum convergence
argument. [Student of Games](https://arxiv.org/abs/2112.03178) combines guided search, self-play,
value/policy learning, and game-theoretic re-solving in one algorithm, beating strong public agents
in heads-up no-limit poker and Scotland Yard.

The limitation is central for Terraforming Mars: these methods can require enumerating or
representing all private information states compatible with a public state. That is manageable in
some poker subgames and prohibitive when a hidden hand could be one of many subsets from hundreds of
cards. Student of Games also uses separate game-specific networks and training; “unified algorithm”
does not mean “one model plays arbitrary games.” A learned policy without explicit belief search,
or a sampled-belief search guided by that policy, is more plausible initially for Solarnet.
Ataraxos is new evidence that the latter can work even when exact belief enumeration is impossible,
provided the simulator is extremely fast and the search aggregates one candidate root action across
all sampled hidden worlds.

### Privileged training can teach inference without cheating at play time

[Suphx](https://arxiv.org/abs/2003.13590) first imitates top Mahjong data and then uses distributed
policy-gradient self-play. Its “oracle guiding” gradually removes privileged hidden-state features
during training. It also separates discard, riichi, chow, pong, and kong decisions and adapts its
risk-taking to the current match situation. Suphx was rated above 99.99% of ranked Tenhou players.

[PerfectDou](https://proceedings.neurips.cc/paper_files/paper/2022/hash/e26f31de8b13ec569bf507e6ae2cd952-Abstract-Conference.html)
uses the same general idea in actor-critic form: the critic sees all hands during training while the
deployed actor sees only legal observations. This is not information leakage if the actor never
receives the private features and evaluation is run through the same player-facing interface.

For Solarnet this suggests a full-state training critic or auxiliary targets for final score and
opponent holdings. It also creates a strict engineering requirement: training data and the deployed
policy path must make it impossible to accidentally include private cards, hidden deck order, or
private event-log facts.

### Variable actions should be represented as objects, not fixed labels

[DouZero](https://proceedings.mlr.press/v139/zha21a.html) handles DouDizhu's large, variable set of
legal combinations by encoding each candidate action and estimating its return. It trained from
scratch on one server with four GPUs, reached the top of a 344-agent leaderboard, and avoided an
enormous fixed action head. PerfectDou retained structured card/action matrices and action masking.

This maps closely to Solarnet's task system. The engine can enumerate a variable list of legal task
revisions or complete choices. A model should score each `(observation, candidate choice)` pair or
use attention over the choice set. That avoids reserving one output neuron for every card-target-
payment combination and naturally permits new choices whose structure is composed from known
fields.

### Human data is an accelerator, not a requirement

DeepNash and DouZero show that pure self-play can work when simulation is cheap enough. Other strong
systems use human traces to avoid spending enormous computation rediscovering basic play:

* Suphx starts with supervised learning from top Mahjong games.
* Cicero combines a strategic model with a dialogue model trained on human Diplomacy games and
  messages. Across 40 anonymous online games it scored more than twice the human average and ranked
  in the top 10% of participants who played more than one game
  ([Science abstract](https://pubmed.ncbi.nlm.nih.gov/36413172/)).
* Metamon reconstructs player-relative Pokémon trajectories from public battle logs, trains by
  imitation and offline reinforcement learning, then fine-tunes on synthetic self-play.
* The 2025 multi-card-game study fine-tunes 7–9B language models on 0.4–1.0 million filtered teacher
  choices per game, using eight H100 GPUs. The same architecture can learn eight games, but it is
  copying extensive task-specific experience rather than discovering each game from its rules.

If Solarnet has no large human log, a strong heuristic/search teacher can provide an initial
curriculum. The teacher need not be optimal; self-play can later exceed it. Training only on winning
teacher moves is dangerous, though: the multi-game study found role imbalance in DouDizhu because
its filtering retained both cooperating winners even when one partner's decisions were poor.

## The closest content-rich evidence

### Restricted Hearthstone: strong specialists know the card pool

[ByteRL for Hearthstone](https://arxiv.org/abs/2303.05197) is the strongest published end-to-end
card-game result located in this survey. It uses a modified Hearthbreaker implementation with more
than 350 cards and only three hero classes, corresponding roughly to the 2015 Blackrock Mountain
era—not contemporary Hearthstone's several-thousand-card environment. It jointly learns deck
building and battle using card embeddings, recurrent memory, deep reinforcement learning, and
Optimistic Smooth Fictitious Play.

The compute scale was substantial: 24 V100 GPUs and 5,856 CPU cores; individual configurations were
trained for 2–23 days, with hundreds of millions of samples per learning period. Human evidence was
four best-of-five Conquest series against one streamer whose historical best was top ten in China
but whose current rank was reported around 60–70. The normal 23-day model won two series 3–0. A
separate model allowed to peek at a random prefix of the opponent's deck, though not the hand, won
the other series 3–1 and 3–2. The privileged model must not be mixed into the normal agent's claim.

This is impressive but narrow evidence: one opponent, few matches, an old subset, and no unseen-card
evaluation. Cards are learned in a fixed vocabulary. Adding a card requires at minimum adding its
engine implementation and representation; the work gives no reason to expect strong zero-shot use.

### Competitive Pokémon: the best large-content analogue

Pokémon battles are not literally card games, but their 1,000+ species, moves, abilities, items,
types, niche interactions, hidden team, probabilistic effects, and evolving metagame make them the
closest current analogue to a game with an encyclopedia of distinct card-like objects.

[Metamon](https://rlj.cs.umass.edu/2025/papers/RLJ_RLC_2025_340.pdf) trained sequence models up to
200M parameters from approximately 950,000 reconstructed point-of-view trajectories (38M steps) for
generations 1–4, followed by offline reinforcement learning and synthetic self-play. Its strongest
agent played more than 600 anonymous human ladder games and reached the top decile across those
formats; one reported Gen 1 result was 79.9% GXE with Glicko-1 1761 ± 35. The representation
tokenizes the exact Pokémon vocabulary and includes an unknown token; it is not an effect-language
interpreter or held-out-species study.

The March 2026 [PokéAgent Challenge report](https://arxiv.org/abs/2603.15563) expands this line to
more than 20M battle trajectories, thirty specialist policies, Generation 9 support, and a 2025
competition with over 100 teams. Thirteen of sixteen finalist slots extended the provided RL models;
the other top methods were independent RL or search. Pure language-model approaches did not lead.
The report explicitly finds a remaining gap among generalist language models, specialist RL, and
elite humans. This is a valuable contemporary reality check: large heterogeneous content has not
made specialist training obsolete.

[PokéChamp](https://proceedings.mlr.press/v267/karten25a.html) takes the opposite approach. It does
not fine-tune the language model. GPT-4o or Llama 3.1 supplies candidate actions, an opponent model,
and leaf evaluations for a shallow minimax search; an exact local simulator, damage calculator,
historical team statistics, and legal-action list do the mechanical work. GPT-4o PokéChamp won 84%
against the strongest rule-based bot and crossed ladder Elo 1300 after 50 games. The often-cited
1300–1500 / top-30%-to-top-10% number is a projection that excludes timeout losses: roughly one
third of live games timed out. Later known-human demonstrations could exploit its excessive
switching, stale pretraining knowledge hurt it after a metagame change, and the paper does not test
unseen species or mechanics.

The useful conclusion is not that a general language model already solves content-rich games. It is
that pretrained knowledge can guide candidate selection and evaluation when wrapped in an exact,
domain-specific harness. The harness—not merely the model—contains the rules, calculator, state
tracking, memory, and time management.

## What “unseen card” results actually show

### A five-level generalization ladder

It is useful to separate five increasingly difficult claims:

1. **New instance:** the same known card appears in a new hand, deck, board, or opponent matchup.
2. **New identity, known attributes:** a never-seen card has a new combination of familiar cost,
   tags, numbers, and keywords.
3. **New composition of known effects:** familiar primitives appear in a new order or combination,
   such as “pay energy, then add two animals and gain one TR.”
4. **New phrasing or conditional structure:** familiar consequences are expressed differently or
   behind an unusual trigger/target condition.
5. **New mechanic:** the card changes timing, legality, action structure, information, scoring, or
   introduces a new resource/effect primitive.

Competitive fixed-game agents routinely handle level 1. LOCM demonstrates level 2 and a constrained
form of level 3. Cardsformer demonstrates levels 2–3 for selected simple effects. No located work
demonstrates levels 4–5 at competitive strength in a Terraforming-Mars-scale game.

### Procedural LOCM: genuine new cards, small effect language

LOCM 1.5 procedurally generates 120 cards before every game. Agents draft 30, then battle with hidden
and shuffled decks. Its numerical observations describe card cost, attack, defense, keywords, and a
small set of deterministic effects. The 2022
[Strategy Card Game AI Competition summary](https://jakubkowalski.tech/Publications/Kowalski2023SummarizingStrategy.pdf)
reports that neural agents dominated after fixed card orderings became impossible; ByteRL won both
tracks by a large margin.

This is the cleanest evidence that a policy need not train on every card identity. It learns a
function over card properties. But LOCM was explicitly designed as a small research CCG. Its
procedural generator recombines a closed grammar; it does not invent something analogous to a new
Terraforming Mars timing rule, persistent trigger, or expansion subsystem. “Infinite cards” here
means infinite parameter combinations in a fixed effect language.

### Cardsformer: language helps, within a carefully limited test

[Cardsformer](https://journals.sagepub.com/doi/pdf/10.3233/FAIA230581) uses an MPNet sentence
embedding for card descriptions, a learned one-step state-change predictor, and a transformer that
scores legal actions. The exact environment still supplies the complete legal-action list. The
transition predictor was trained on roughly 800,000 examples from 10,000 random games; policy
self-play used 100M frames on 8 Titan XP GPUs and 64 CPU cores.

The task-specific training scope was only twenty predefined decks across nine classes, comprising
186 distinct description sentences. Test decks replaced 10, 20, or all 30 cards with held-out
cards. Against Dynamic Lookahead, the 2020 Hearthstone competition winner, Cardsformer won 78.0%
with no held-out cards, 70.7% with ten, 64.0% with twenty, and 47.5% with all thirty.

The qualification is decisive. The authors restricted held-out cards to simple effects without
unique triggers. The predictor could transfer direct damage plus healing, but failed a unique
“whenever a spell is cast” copying trigger. They explicitly report failure on effects activated by
unique conditions. Thus the experiment shows semantic similarity and learned local consequences,
not arbitrary rule reading.

The 2025 AAMAS
[LLM-plus-RL extended abstract](https://ifaamas.csc.liv.ac.uk/Proceedings/aamas2025/pdfs/p2795.pdf)
adds a fine-tuned T5 encoder, 5,000 GPT-4-summarized deck strategies, self-play, and a latent
transition loss. Five test decks contain up to 50% unseen cards. It reports 68% against its strongest
tree-search level, versus 48% for Cardsformer and 12% for a directly prompted GPT-4o agent. This is
promising, but it is a three-page extended abstract with sparse protocol details and no demonstrated
coverage of novel mechanics. It should not outweigh Cardsformer's explicit limitation.

### Magic drafting: impressive valuation, no rules execution

[Learning With Generalised Card Representations for Magic](https://arxiv.org/abs/2407.05879) replaces
one-hot card IDs with numerical and categorical features, text, images, and usage metadata. A model
trained on 75M decisions spanning 2,990 cards predicted 55.44% of human choices in a completely held-
out set; the broader data collection contains about 100M draft decisions. Fine-tuning on the new set
improved quickly.

That is strong evidence for estimating card quality and deck fit without seeing the identity during
training. It does not test playing Magic, applying the comprehensive rules, resolving interactions,
reasoning about a hidden opponent, or winning games. Usage metadata also would not exist at a new
set's first reveal. Solarnet should borrow the shared-feature representation and held-out-set
protocol, not infer that Magic battle play is solved.

### Rules as language: proof of principle only

[RTFM](https://discovery.ucl.ac.uk/id/eprint/10101221/) procedurally changes a small grid world's
dynamics and supplies a natural-language manual. Its policy transfers to held-out dynamics by
reading. This validates the basic idea of conditioning action on instructions. The environment is a
small single-agent task with four movement directions, far below the target game and competitive
standard.

[Chaos Cards](https://ojs.aaai.org/index.php/AIIDE/article/view/7430) takes a more dependable route:
generate executable Hearthstone-like cards from a grammar, simulate an evolving metagame, and learn
to predict card strength. It is a playtesting/content-evaluation system rather than a competitive
player, but it reinforces a key engineering principle: machine-readable effect structure gives both
the engine and the learner a stable way to handle new compositions.

## Architecture implications for Solarnet

### Separate rule competence from strategic competence

Solarnet already has the valuable half that many research projects had to build: an exact,
transactional rule engine. `ComponentGraph`, `Effector`, `EventLog`, and task preparation can provide
state transitions, rollback, histories, and legal choices. The AI should not learn whether an action
is legal or be asked to reproduce card effects from prose.

This separation gives three benefits:

* illegal actions have zero probability because they are absent or masked;
* new cards work mechanically as soon as the engine supports them;
* a model can focus its capacity on value, timing, uncertainty, and opponent behavior.

It also clarifies “handling a new card.” The engine must always know how it works. The learned player
may or may not already know how valuable its consequences are.

### Build an explicit player observation boundary

The live `World`, event log, and trusted engine APIs can contain facts a player must not see. The
learning interface needs a deterministic, player-relative observation builder. It should expose:

* public state and the acting player's private state;
* public action/event history and what this player previously observed;
* counts for hidden zones, never their contents or order;
* stable masks/unknown markers when information is absent;
* turn, phase, active player, generation, global parameters, map, milestones, awards, scores, and
  every public persistent effect;
* the model's own cards, productions, resources, tags, discounts, stored card resources, and legal
  pending tasks.

The same builder must be used for logged human examples, self-play, evaluation, and deployment. A
separate full-state view may feed a training-only critic. Event attribution and rollback are useful
for labels, but raw events must be filtered so a draw event cannot reveal the other player's card.

### Encode cards by canonical semantics

A card representation should combine:

* general metadata: kind, cost, tags, expansion/options, requirements;
* immediate effect tree: gains, removals, production, parameter changes, placement, targets, and
  alternatives;
* persistent effects: trigger, condition, affected object, transformation, owner/actor relation;
* actions: cost, usage limit, target/choice schema, result;
* scoring expression and stored-resource behavior;
* current instance facts: owner, resources on the card, used-this-generation state, and whether its
  effect is live.

The PETS type/effect structure is a better primary representation than English card text because it
is canonical, executable, and already distinguishes relationships that prose may leave implicit.
A learned encoder can recursively embed the typed expression/effect tree. Raw text, card ID, and
name can be additional features, but ablations should determine whether they help or merely let the
model memorize.

Do not flatten an effect tree into a handful of card categories if that loses triggers, ownership,
or target relationships. The aim is compositional reuse: two cards that both add animal resources
should share machinery, while “add animals when anyone plays a microbe tag” remains distinguishable
from an immediate addition.

### Score structured legal choices

The most future-proof policy shape is:

```text
player observation -> context encoder
each engine-generated legal choice -> choice/card/effect encoder
context attends to legal-choice set -> score per choice + state value
```

A choice encoding should include its task instruction, narrowing path, source card/component,
targets, quantities, payment mix, placement, and any immediately computable deltas. Candidate choices
can share a common schema while retaining variable-length nested fields. This resembles DouZero's
action encoding and avoids a fixed vocabulary tied to today's card list.

Solarnet's decomposed tasks create a credit-assignment issue: `playProject`, payment, target, and
placement are parts of one strategic commitment. Training records should identify the enclosing
manual operation or causal card so value can be assigned coherently. Where safe, the AI interface
may expose complete prepared alternatives instead of forcing the policy to rediscover compatible
subchoices one at a time; the engine should remain the source of their legality.

### Preserve history or learn beliefs

A snapshot loses information revealed by earlier play: cards passed in a draft, discarded projects,
public card draw, inferred resource commitments, and the opponent's tempo. Initial choices are:

* a recurrent or transformer policy over the public/player-visible event history;
* a compact hand-coded sufficient-history summary plus current state;
* auxiliary prediction heads for opponent tags, likely hand categories, next action, remaining game
  length, and final score;
* later, a sampled belief model for shallow search.

Explicit enumeration of every possible opponent hand is unlikely to scale. A learned latent memory
or sampled generative belief is more plausible. Belief accuracy should be measured separately from
win rate.

### Train in stages

A practical sequence is:

1. **Throughput and correctness:** run deterministic seeded games, build observation/action
   serialization, and prove hidden facts cannot leak.
2. **Behavior cloning:** imitate existing heuristics, MCTS, scripted games, or human logs so the
   model learns legal sequencing and non-random play.
3. **Offline value learning:** predict winner, rank, final score, score delta, generation remaining,
   and useful engine-derived outcomes from historical positions.
4. **Two-player self-play:** begin with a fixed option set and population of opponents/checkpoints.
5. **Privileged critic:** optionally give the critic full state while keeping the actor's interface
   private.
6. **League/population training:** retain exploiters, old checkpoints, heuristic styles, and fixed
   evaluation opponents rather than training only against the latest self.
7. **Multiplayer:** randomize seats/player counts and optimize rank/win objectives, with score as an
   auxiliary target rather than the only reward.
8. **Expansion curriculum:** mix old and new cards and keep holdout suites frozen.

Terminal win/rank is the clean objective but is sparse. Intermediate score, production, or engine
growth can accelerate training and also teach pathological stalling or point farming. Use shaped
signals as auxiliary predictions or annealed rewards, and always evaluate on actual wins/ranks.

### Add search only after the policy is useful

The engine's rollback and exact transitions make limited online planning attractive. However, full
MCTS from the root is expensive in a 400-component, roughly 460-decision game, and naive
determinization mishandles hidden information. A reasonable later hybrid is:

* policy-guided expansion of a small number of legal choices;
* learned value at a short horizon;
* multiple sampled opponent hands/deck continuations consistent with observations;
* aggregation of action value across samples without allowing a different root choice per sample;
* a learned opponent policy, not random rollouts;
* strict per-move forward-model and wall-clock budgets.

Search should be benchmarked against the policy alone. PokéChamp shows that a language-guided search
can gain strength while becoming too slow for live play; TAG shows that shallow forward planning can
miss the delayed consequence of a decomposed card action.

## Evaluation design

A credible claim needs more than self-play Elo. Maintain a frozen evaluation matrix containing:

### Playing strength

* random, greedy, scripted, one-step, and search baselines;
* old model checkpoints and independently trained seeds;
* targeted best-response/exploiter agents;
* human players stratified by rating or demonstrated experience;
* duplicated deals/seeds with seats and first-player roles swapped where rules permit;
* two-, three-, four-, and five-player evaluation reported separately;
* win/rank, score margin, confidence intervals, illegal-action rate, decision latency, and forward-
  model calls.

Human tests should report every match, current player strength, rules/options, whether participants
knew the opponent was an AI, and whether the AI used privileged information. Avoid extrapolating a
human tier from a handful of games or from scores collected in different populations.

### Content transfer

Use multiple non-overlapping holdout suites:

1. random held-out card identities;
2. held-out corporations;
3. cards held out by expansion or release batch;
4. held-out combinations of individually known primitives;
5. held-out trigger/effect families;
6. new numeric ranges and rare targets;
7. deliberately adversarial interactions and cards that alter end timing or action availability.

Report at least four conditions:

* identity-only representation;
* canonical shared semantic representation;
* semantic representation plus raw text/pretrained text embedding;
* exact same model after a small amount of new-card fine-tuning.

The critical comparison is performance on **mechanic-held-out** cards, not only randomly held-out
cards. A random split leaks near-duplicates and common effect templates into training. Also compare
zero-shot use of a new card with zero-shot response when only the opponent has it.

### Robustness and leakage

* Train an explicit best response against each frozen candidate, as the ByteRL follow-up did.
* Audit observations and histories for private-state leakage.
* Test shuffled list order and renamed card IDs to detect positional or identity shortcuts.
* Test outside the self-play metagame: fast terraforming, engine building, board denial, milestone
  races, award manipulation, and unusual corporation/card combinations.
* Separate the strategic player from engine bugs. A model must never receive credit for exploiting
  invalid transitions unless exploit discovery is the stated goal.

## Recommended first research target

The first defensible milestone is not “one model reads any card.” It is:

> A two-player, no-draft or fixed-draft agent that uses only player-visible information, scores
> engine-generated legal tasks, beats the current search/script baselines over duplicated seeds,
> and loses little strength when a meaningful subset of cards with known effect primitives was never
> present during training.

This milestone tests the architecture decisions that matter most—observation privacy, variable legal
actions, semantic card encoding, long-horizon value, and population self-play—without conflating
them with all multiplayer and expansion problems at once.

The next milestone should hold out an entire effect family. Failure there is informative: it marks
the boundary between “new card” and “new rule.” Only after that boundary is measured is it worth
adding raw language conditioning or an LLM-based strategic advisor.

## Bottom line on specific-card training

Models do not inherently need one separate learned parameter per card. They do need an input
language that exposes why cards are similar and enough experience to learn the strategic meaning of
that language.

* With **ID-only embeddings**, assume every card needs task-specific exposure and new IDs require
  retraining or at least new embeddings.
* With **shared numeric/tag/keyword features**, expect transfer to new combinations of those fields.
* With a **typed executable effect tree**, expect the best chance of transfer to new compositions of
  known rule primitives.
* With **raw natural language**, expect improved similarity and useful prior knowledge, but also
  ambiguity, stale knowledge, and failure on unusual triggers.
* With a **truly new mechanic**, assume engine implementation and renewed training are required until
  demonstrated otherwise.

The state of the art supports “train on the game's semantic vocabulary, then generalize across cards
written in that vocabulary.” It does not yet support “train once, then competitively understand any
card that comes along.”

## Annotated primary sources

### Direct and tabletop frameworks

* Gaina, Goodman, Perez-Liebana (2021),
  [TAG: Terraforming Mars](https://ojs.aaai.org/index.php/AIIDE/article/view/18902) — direct game
  implementation, complexity measurements, and search baselines.
* Balla et al. (2024), [PyTAG](https://arxiv.org/abs/2405.18123) — RL interface and small-game PPO
  experiments; Terraforming Mars remains untrained.
* Lanctot et al. (2019), [OpenSpiel](https://arxiv.org/abs/1908.09453) — common terminology,
  algorithms, and environments spanning imperfect-information and general-sum games.
* Thielscher (2010),
  [GDL-II](https://ojs.aaai.org/index.php/AAAI/article/download/7647/7508) — rules-first general game
  descriptions with chance and hidden information; useful precedent, not a competitive complex-card
  result.

### Imperfect-information strength

* Sokota et al. (2025), [Ataraxos](https://arxiv.org/abs/2511.07312) — preprint reporting
  superhuman Stratego through dynamically damped self-play and sampled-belief test-time search.
* Perolat et al. (2022), [DeepNash](https://arxiv.org/abs/2206.15378) — strong Stratego through
  model-free equilibrium-oriented self-play; superseded in human evaluation by Ataraxos.
* Brown et al. (2020),
  [ReBeL](https://papers.nips.cc/paper_files/paper/2020/hash/c61f571dbd2fb949d3fe5ae1608dd48b-Abstract.html)
  — learned public-belief-state search in two-player zero-sum games.
* Schmid et al. (2023), [Student of Games](https://arxiv.org/abs/2112.03178) — unified algorithmic
  framework for guided search and self-play across perfect and imperfect games.
* Brown and Sandholm (2019),
  [Pluribus](https://doi.org/10.1126/science.aay2400) — superhuman six-player no-limit poker.
* Li et al. (2020), [Suphx](https://arxiv.org/abs/2003.13590) — supervised initialization,
  self-play, privileged training, and runtime adaptation in four-player Mahjong.
* Zha et al. (2021), [DouZero](https://proceedings.mlr.press/v139/zha21a.html) — structured variable
  action scoring and simple parallel self-play in DouDizhu.
* Yang et al. (2022),
  [PerfectDou](https://proceedings.neurips.cc/paper_files/paper/2022/hash/e26f31de8b13ec569bf507e6ae2cd952-Abstract-Conference.html)
  — full-information training critic with partial-information execution.
* Meta Fundamental AI Research Diplomacy Team (2022),
  [Cicero](https://pubmed.ncbi.nlm.nih.gov/36413172/) — strategic planning plus human-grounded
  dialogue in seven-player Diplomacy.

### Heterogeneous cards and large content sets

* Xiao et al. (2023), [ByteRL Hearthstone](https://arxiv.org/abs/2303.05197) — restricted 350+ card
  game, end-to-end construction/play, very large compute, and small human test.
* Xi et al. (2023), [ByteRL LOCM](https://arxiv.org/abs/2303.04096) — end-to-end policy and
  equilibrium-oriented self-play over procedural card pools.
* Haluška and Schmid (2024),
  [Learning to Beat ByteRL](https://arxiv.org/abs/2404.16689) — preliminary best-response evidence
  that the competition winner remains exploitable.
* Kowalski and Miernik (2023),
  [Strategy Card Game AI Competition summary](https://jakubkowalski.tech/Publications/Kowalski2023SummarizingStrategy.pdf)
  — fixed and procedurally generated LOCM versions, entrants, and results.
* Xia et al. (2023),
  [Cardsformer](https://journals.sagepub.com/doi/pdf/10.3233/FAIA230581) — strongest controlled
  full-play held-out-card study, with an explicit unique-trigger limitation.
* Xia et al. (2025),
  [LLM plus RL for strategic card games](https://ifaamas.csc.liv.ac.uk/Proceedings/aamas2025/pdfs/p2795.pdf)
  — promising but short follow-up with up to 50% held-out cards.
* Bertram, Fürnkranz, Müller (2024),
  [generalized Magic card representations](https://arxiv.org/abs/2407.05879) — unseen-set drafting,
  not battle play.
* Kowalski et al. (2023), [Tales of Tribute competition](https://arxiv.org/abs/2305.08234), and
  Lashmet and Dockhorn (2025),
  [PPO agent](https://research.uni-hannover.de/en/publications/training-a-reinforcement-learning-agent-for-tales-of-tribute/)
  — modern in-match deck-building testbed.
* Vieira, Tavares, Chaimowicz (2024),
  [CCG taxonomy](https://arxiv.org/abs/2410.06299) — useful map of cards, effects, timing, combat,
  zones, and remaining game-agnostic challenges.
* Chen and Guy (2020),
  [Chaos Cards](https://ojs.aaai.org/index.php/AIIDE/article/view/7430) — executable procedural card
  grammar and metagame-aware strength estimation.
* Cunha et al. (2026), [MTG-Causal-RL](https://arxiv.org/abs/2605.06066) — recent preprint benchmark
  with partial observations, masked actions, and cross-archetype evaluation.

### Language and multi-game work

* Grigsby et al. (2025),
  [Metamon](https://rlj.cs.umass.edu/2025/papers/RLJ_RLC_2025_340.pdf) — sequence policies trained
  offline on large exact-game Pokémon datasets.
* Karten, Nguyen, Jin (2025),
  [PokéChamp](https://proceedings.mlr.press/v267/karten25a.html) — language-model-guided minimax
  without additional fine-tuning, plus instructive latency/exploitability limitations.
* Karten et al. (2026), [PokéAgent Challenge](https://arxiv.org/abs/2603.15563) — latest broad
  Pokémon competition and the clearest comparison of specialist RL, search, and LLM agents.
* Wang et al. (2025),
  [Can Large Language Models Master Complex Card Games?](https://arxiv.org/abs/2509.01328) — one
  architecture fine-tuned on eight games from strong teacher data; no new-card semantics test.
* Zhong, Rocktäschel, Grefenstette (2020),
  [RTFM](https://discovery.ucl.ac.uk/id/eprint/10101221/) — small-scale proof that a policy can read
  a manual to adapt to procedurally changed dynamics.
