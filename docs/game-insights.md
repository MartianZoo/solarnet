Here's a few insights into the deep design of the game that working on this has illuminated for me, that *I* find interesting, so it's possible that someone else out there will too.

* Some gains/removes are "atomized" and some aren't. That is, any time you raise the temperature 2 steps, or gain 2 science tags, you have to address each one as its own independent event, or the right things won't happen. However, if (say) reducing plant production 2 steps, those have to be done as a single change, or else Mons Insurance would fire twice. Currently, Tags and GlobalParameters are the only things marked as Atomized. (OceanTile inherits that bit, although it would have "forced atommization" anyway due to the need to put the tiles on different areas.)
* Different component types have different instructions for "how to gain this type *by default*". For example:
    * Gaining tiles defaults to certain kinds of areas (`OceanTile<WaterArea>`, `SpecialTile<LandArea>`)
    * Gaining a colony defaults to `Colony<ColonyTile(HAS MAX 0 Colony)>` -- a colony tile where you don't already have a colony.
    * Gaining a GlobalParameter or a CardResource applies the "as many as possible" modifier by default
* We'd expect PharmacyUnion to have `This: 54`. But, like no other corporation, it has to have `This:: 54` instead; that is, we have to force that effect to be *immediate* instead of queued. This is because otherwise the engine tries to process the two "remove 4 money" instructions first *before* taking the money! Which, by the game rules, you *would* be able to do (don't try it IRL, you will not make friends).
* There are "conditional triggers" which are needed only by:
    * track bonuses (`This IF =3 This: PROD[Heat]`)
    * SearchForLife (`End IF Science<This>: 3 VictoryPoint`)
    * (obscure) adding the `Titan` colony tile to the game wouldn't normally put it immediately into play, but thanks to Aridor, we have to conditionally do so in case a Floater card had already been played.
    * PharmacyUnion has different behaviors on `ScienceTag` depending on whether any `Disease` are present (if we ever had `ELSE` working we could just do a single effect, `TR FROM Disease ELSE (PlayedEvent<..> FROM This, 3 TR)?`)
* We have to implement event cards as first going into play, then being removed from play at the end of the turn. This is technically correct, but affects the behavior of only *one* card in the entire game, Solar Probe.
* The game currently models `GreeneryTile` as having the intrinsic behavior `This: OxygenStep`. It does this because I very much like to think of effects as being intrinsic to the objects that trigger them (call it excessive "object-orientedness" if you like). There are only two cases where that gets us into trouble:
    * after solo game setup we have to reduce the oxygen count by 2!
    * technically during the endgame greenery placement round then greenery tiles aren't supposed to raise oxygen anymore. From my reading of the various threads, this rule came about "accidentally" and doesn't really make sense, and is of little import anyway, so I'm not inclined to "fix" this.
