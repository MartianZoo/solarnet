## repl commands

Type `help` for a list; `help command-name` for more.

## class names

Most "things" in the game are called just what you think: `Plant`, `TerraformRating`, `VictoryPoint`, `EarthCatapult`. They also have shortnames (`P`, `TR`, `VP`, `C070`). Tiles end in `-Tile`, like `CityTile`. Global parameters end in `-Step`, like `TemperatureStep` counts how many times the temp has been raised (from zero to 19).

`CardBack` and `CardFront` are two different things! The first turns into the second when you play it (which might then turn into a `PlayedEvent`). Card backs of the same type, like `PreludeCard`, are indistinguishable; the card doesn't take on its unique identity until it's played.

Map areas are named `Tharsis_1_1`, `Elysium_9_9`, etc. First comes the row and then the column, but you have to picture the column as slanting up-and-to-the-right. (Using the `map`/`tfm_map` command in the repl will help picture this.)

## what does this class mean?

Type `help <ClassName>` or, for the gruesome details, `desc <ClassName>`.

## instructions cheat sheet

* `Plant` means to gain a plant
* `-Plant` means to remove one
* `4` is a shortcut for `4 Megacredit`
* `Plant / PlantTag` means to gain a plant for each plant tag you have
* `PROD[Plant / PlantTag]` means to increase plant production for each plant tag you have
* `PROD[Plant OR 3 PlantTag: 4 Plant]` means to increase plant production 1 step, or, if you have 3 plant tags, 4 steps
* `2 Steel<Player1> FROM Steel<Player2>` or `2 Steel<Player1 FROM Player2>` means to transfer 2 steel directly from P2 to P1
* `-2 Microbe<This> THEN PROD[Plant]` means to remove 2 microbes from that card; only you've done that, a new task will appear to increase your plant production
* `-3 Megacredit.` (with a dot) means to lose 3 megacredits or as much of that amount as possible
* `-6 Plant<Anyone>?` (with a question mark) means to remove *up to* 6 plants from any player (i.e., optional)
* `Ok` means to do nothing, which is sometimes needed because instructions are mandatory

## other syntax

* `Steel -> 5` is an action, meaning to spend one steel to gain 5 megacredits
* `X Microbe<This> -> 3X` means to spend one or more microbes from this card to get 3 megacredits each
* `CityTile<Anyone>: PROD[1]` is a triggered effect, meaning when anyone gains a city tile, you get a money production
* A requirement of `MAX 5 OxygenStep` means the oxygen level must be 5% or lower to do the thing
