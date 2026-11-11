package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.agent.Agent
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.TaskForm
import dev.martianzoo.agent.TaskForm.Decision.Kind.ALTERNATIVE
import dev.martianzoo.agent.TaskForm.Decision.Kind.AMOUNT
import dev.martianzoo.agent.TaskForm.Decision.Kind.TARGET
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.state.Player
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.script.TfmMapRenderer
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Three-player physical game begun Friday, 2026-08-28. */
internal class OtbGame20260828Test : FullGameTest(EAGER) {
  // This replay knows its choices in advance. Check that the current chooser offers each
  // recorded choice without fixing the number of unrelated options in the catalog.
  private fun Agent.fillInTask(
      vararg keyClasses: String,
      count: Int? = null,
  ): ExpectedTaskForm {
    val matches =
        tasks
            .matching { task ->
              (count == null ||
                  task.instruction.descendantsOfType<Change>().firstOrNull()?.count ==
                      ActualScalar(count)) &&
                  keyClasses.all { key ->
                    task.instruction.descendantsOfType<ClassName>().any { it == cn(key) }
                  }
            }
            .map(tasks::getTaskData)
            // Identical offers (such as Great Aquifer's oceans) are interchangeable.
            .distinctBy { it.copy(id = TaskId(0), cause = null) }
    val id =
        tasks.selectedTask()?.takeIf { selected -> matches.any { it.id == selected } }
            ?: matches.single().id
    return ExpectedTaskForm(fillInTask(id))
  }

  private class ExpectedTaskForm(private val form: TaskForm) {
    fun chooseAlternative(className: String, count: Int? = null): ExpectedTaskForm {
      val decision =
          form.decisions().singleOrNull { it.kind == ALTERNATIVE }
              ?: error("expected an alternative decision in ${form.decisions()}")
      val options = form.options(decision).toList()
      val option = options.single { option ->
        (className == "Ok" && option == "Ok") ||
            Parsing.parse<InstructionTree>(option).descendantsOfType<Change>().any { change ->
              (change.gaining ?: change.removing)?.className == cn(className) &&
                  (count == null || change.count == ActualScalar(count))
            }
      }
      form.choose(decision, option)
      return this
    }

    fun choose(decisionAndChoice: Pair<String, String>): ExpectedTaskForm {
      val (typeClass, choice) = decisionAndChoice
      val decision =
          form.decisions().singleOrNull {
            it.kind == TARGET && it.focus?.className == cn(typeClass)
          } ?: error("expected a $typeClass decision in ${form.decisions()}")
      assertTrue(choice in form.options(decision), "$choice is not offered for $typeClass")
      form.choose(decision, choice)
      return this
    }

    fun chooseAmount(amount: Int): ExpectedTaskForm {
      val decision =
          form.decisions().singleOrNull { it.kind == AMOUNT }
              ?: error("expected an amount decision in ${form.decisions()}")
      val choice = if (amount == 0) "Ok" else amount.toString() // Decline the optional amount.
      assertTrue(choice in form.options(decision), "$choice is not an offered amount")
      form.choose(decision, choice)
      return this
    }

    fun narrow(instruction: String): ExpectedTaskForm {
      form.narrow(instruction)
      return this
    }

    fun done(): TaskResult {
      check(form.decisions().isEmpty()) { "another decision remains: " + form.decisions() }
      return commit()
    }

    fun commit(): TaskResult = form.commit()
  }

  private val colonyTiles = listOf("Ganymede", "Io", "Luna", "Miranda", "Titan")
  override val config =
      GameConfig(
          """
          CimmeriaMap
          VenusNextExpansion, PreludeExpansion, prelude2:cards, ColoniesExpansion, promos:cards
          fakes:cards

          Engineer, Fundraiser, Landshaper, Merchant, Metallurgist
          Benefactor, EstateDealer, Industrialist, Metropolist, SpaceBaron
          ${colonyTiles.joinToString()}
          """,
          "Green",
          "Blue",
          "Yellow",
      )

  @Test
  internal fun otbGame20260828() {
    startGame()
    val green = p1
    val blue = p2
    val yellow = p3
    green.fillInTask("ProjectCard").chooseAmount(6).done()
    blue.fillInTask("ProjectCard").chooseAmount(5).done()
    yellow.fillInTask("ProjectCard").chooseAmount(5).done()

    // "Okay, I am the start player, and I play Paladin Shipping. That gives me 36 money and five
    // titanium. Then I use this little buy-cards slider to buy four cards."
    green.fillInTask("PlayCard").choose("CardFront" to "$PalladinShipping").done()
    // "I play Celestic, so I start with 42 megacredits." "You do get 42 money and then buy how many
    // cards? Five cards?" "Oh, yes."
    blue.fillInTask("PlayCard").choose("CardFront" to "$Celestic").done()
    // "And I am Point Luna." "Oh, man. That corp is strong." "Start with 38 money, titanium
    // production. It immediately gives me a card. And I slide five cards. Boop."
    yellow.fillInTask("PlayCard").choose("CardFront" to "$PointLuna").done()

    with(green) {
      // "I've played Biofuels and Supplier. I get two energy production and four steel, and then I
      // get one energy production, one plant production, and two plants."
      fillInTask("PlayCard").chooseAlternative("PlayCard").choose("CardFront" to "Biofuels").done()
      fillInTask("PlayCard").chooseAlternative("PlayCard").choose("CardFront" to "Supplier").done()
    }
    with(blue) {
      // "First I'm going to play Great Aquifer. I place two ocean tiles." "I'll put one here.
      // That's row two, column one, for two titanium." The second placement is stated below.
      fillInTask("PlayCard")
          .chooseAlternative("PlayCard")
          .choose("CardFront" to "GreatAquifer")
          .done()
      fillInTask("OceanTile").choose("WaterArea" to "Cimmeria_2_1").done()
      fillInTask("OceanTile").choose("WaterArea" to "Cimmeria_9_5").done()
      // "I'm going to put this one on row nine, column five. That gives me two plants." "And then
      // Atmospheric Enhancers. I'm enhancing our atmosphere. I'm gonna go ahead and raise Venus
      // two steps."
      fillInTask("PlayCard")
          .chooseAlternative("PlayCard")
          .choose("CardFront" to "AtmosphericEnhancers")
          .done()
      fillInTask("VenusStep").chooseAlternative("VenusStep").done()
    }
    with(yellow) {
      // "Orbital Construction Yard: I gain titanium production and four titanium."
      fillInTask("PlayCard")
          .chooseAlternative("PlayCard")
          .choose("CardFront" to "OrbitalConstructionYard")
          .done()
      // "Early Colonization: I place a colony on Luna, which means I gain 2 money production, I
      // gain 3 energy—not energy production, 3 energy—and raise all colony tracks 2 steps."
      fillInTask("PlayCard")
          .chooseAlternative("PlayCard")
          .choose("CardFront" to "EarlyColonization")
          .done()
      fillInTask("Colony").choose("ColonyTile" to "Luna").done()
    }

    // board-17-33-49.jpg: post-Prelude setup, before the first project action.
    with(green) {
      assertProduction(m = 0, s = 0, t = 0, p = 1, e = 3, h = 0)
      assertResources(m = 24, s = 4, t = 5, p = 2, e = 0, h = 0)
      assertCounts(20 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 0, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertResources(m = 27, s = 0, t = 2, p = 2, e = 0, h = 0)
      assertCounts(24 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 2, s = 0, t = 2, p = 0, e = 0, h = 0)
      assertResources(m = 23, s = 0, t = 4, p = 0, e = 3, h = 0)
      assertCounts(20 to "TerraformRating", 1 to "Colony<Luna>")
    }
    assertSidebar(gen = 1, temp = -30, oxygen = 0, oceans = 2, venus = 4)

    with(green) {
      // "I'm gonna play Titan Shuttles." "Oh, shit, that's competition." "Yep. Floater on floater
      // warfare. Floatfare." "I will spend three titanium, which counts as nine money, so I need
      // 14 more. That's my turn."
      // Green explicitly considers keeping titanium for Palladin Shipping, then keeps two.
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$TitanShuttles").done()
      fillInTask("Pay", "Titanium").chooseAlternative("Pay").chooseAmount(3).done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "I had a plan; it's gone now." "Your first action should be to draw this stuff."
      // "There's one: Jet Stream Microscrappers. And Floater Technology. Very cool."
      fillInTask("DoRequiredActions")
          .chooseAlternative("DoRequiredActions")
          .done()
          .expect("2 ProjectCard")
      // "Now I'm gonna play Local Shading. Pay four for it. And I guess that's my two actions."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$LocalShading").done()
    }
    with(yellow) {
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$RimFreighters").done()
      fillInTask("Pay", "Titanium").chooseAlternative("Pay").chooseAmount(1).done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "I'm going to use the Titan Shuttles action to put two floaters on Titan Shuttles."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$TitanShuttles, Action1>").done()
      fillInTask("Floater").choose("ResourceCard" to "TitanShuttles").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "Where's my plan? FML. You have all these floater cards now. I'm going to play
      // Floater Technology for seven."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$FloaterTechnology").done()
      // "I'm going to use the Local Shading action to add a floater to Local Shading. Wait, no,
      // you're right. I'm going to use the Floater Technology action to add it to Local Shading."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$FloaterTechnology, Action1>").done()
      fillInTask("Floater").choose("ResourceHolder" to "LocalShading").done()
    }
    with(yellow) {
      // "For some reason, I thought I was rushing to trade. It occurs to me now that it's probably
      // not worth it for you guys to trade. I probably should have played Business Network first.
      // That's four real monies and an Earth-tag discount card."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$BusinessNetwork").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }

    // She forgot to reduce her money production (fixed later)
    yellow.exMachina("PROD[MC]")

    green.fillInTask("StandardAction").chooseAlternative("Pass").done()
    with(blue) {
      // "I'm going to play Nitrite Reducing Bacteria. That costs me 11 money."
      // "You immediately get three free microbes on Nitrite Reducing Bacteria."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$NitriteReducingBacteria").done()
      // "For my next trick, I will use the Local Shading action to remove a floater and increase
      // my money production one step."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$LocalShading, Action2>").done()
    }
    with(yellow) {
      // "I use Business Network action. I look at a card... not feeling it."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$BusinessNetwork, Action1>").done()
      fillInTask("ProjectCard").chooseAmount(1).done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "I will use my Nitrite Reducing Bacteria action to spend three microbes and increase my TR
      // one step. I guess I pass." "No, you have one more action."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$NitriteReducingBacteria, Action2>").done()
      // "Oh, my Celestic action. I forgot. I will use my Celestic action to add a floater to Local
      // Shading."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Celestic, Action1>").done()
      fillInTask("Floater").choose("ResourceHolder" to "LocalShading").done()
    }
    with(yellow) {
      // "I will spend two energy to trade with Luna. That's seven from the track plus my bonus of
      // two, so nine." "Jesus."
      expectChanges("-2 Energy, 9 MC") {
        fillInTask("StandardAction").narrow("UseAction<TradeAction, Action2>").done()
        fillInTask("Trade").choose("ColonyTile" to "Luna").done()
      }
      // "One more thing. I spent one for Fuel Generators, lose a money production, and gain energy
      // production. And I think that's it."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$FueledGenerators").done()
    }
    blue.fillInTask("StandardAction").chooseAlternative("Pass").done()
    yellow.fillInTask("StandardAction").chooseAlternative("Pass").done()

    // "I will raise the oxygen to one percent as my World Government step."
    green
        .fillInTask("GlobalParameter")
        .choose("GlobalParameter" to "OxygenStep")
        .done()
        .expect("0 TerraformRating")

    // board-17-45-16.jpg and all three app histories: Generation 2 before Research.
    with(green) {
      assertProduction(m = 0, s = 0, t = 0, p = 1, e = 3, h = 0)
      assertResources(m = 30, s = 4, t = 2, p = 3, e = 3, h = 0)
      assertCounts(20 to "TerraformRating")
      assertCardResources(2 to TitanShuttles)
    }
    with(blue) {
      assertProduction(m = 1, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertResources(m = 31, s = 0, t = 2, p = 2, e = 0, h = 0)
      assertCounts(25 to "TerraformRating")
      assertCardResources(1 to LocalShading, 0 to NitriteReducingBacteria)
    }
    with(yellow) {
      assertProduction(m = 1, s = 0, t = 2, p = 0, e = 1, h = 0)
      assertResources(m = 47, s = 0, t = 5, p = 0, e = 1, h = 1)
      assertCounts(20 to "TerraformRating")
    }
    assertSidebar(gen = 2, temp = -30, oxygen = 1, oceans = 2, venus = 4)

    blue.fillInTask("ProjectCard").chooseAmount(2).done()
    yellow.fillInTask("ProjectCard").chooseAmount(2).done()
    green.fillInTask("ProjectCard").chooseAmount(1).done()

    with(blue) {
      // "I'm going to start by taking my Local Shading action, spending a floater to increase my
      // money production."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$LocalShading, Action2>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "I'm going to use my Business Network. And I will buy it."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$BusinessNetwork, Action1>").done()
      fillInTask("ProjectCard").chooseAmount(0).done() // Keep the offered project.
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "Minority Refuge. I'll spend one titanium and two real money. I lose two money production,
      // and I get to place a colony, which I'm going to place on Titan."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$MinorityRefuge").done()
      fillInTask("Pay", "Titanium").chooseAlternative("Pay").chooseAmount(1).done()
      fillInTask("Colony").choose("ColonyTile" to "Titan").done()
      fillInTask("Floater").choose("ResourceHolder" to "TitanShuttles").done()
      // "Then I'm going to use my second action to grab all these floaters." "Remember to get 3
      // for your placing." "Holy shit. I get three and three and one. I get seven floaters."
      fillInTask("StandardAction").narrow("UseAction<TradeAction, Action2>").done()
      fillInTask("Trade").choose("ColonyTile" to "Titan").done()
      fillInTask("Floater", count = 3).choose("ResourceHolder" to "TitanShuttles").done()
      fillInTask("Floater").choose("ResourceHolder" to "TitanShuttles").done()
    }
    with(blue) {
      // "I'm going to play Jet Stream Microscrappers. Pay 12 for it." "You want me to actually pay
      // for the things that I place? You're crazy."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$JetStreamMicroscrappers").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "I'm going to pay five titanium and five real for Solar Logistics." "Oh, man. She gets to
      // draw a card any time any one of us plays a space event. Plus she gets a discount on Earth
      // tags."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$SolarLogistics").done()
      fillInTask("Pay", "Titanium").chooseAlternative("Pay").chooseAmount(5).done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "You'll never imagine what I'm going to do. I'm going to use Titan Shuttles to spend nine
      // floaters and get nine titanium." "Gawah."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$TitanShuttles, Action2>").done()
      fillInTask("Floater").chooseAmount(9).done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "I'm going to play Dirigibles, which costs 11 money."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$Dirigibles").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "On top of Solar Logistics, I will place Optimal Aerobraking. For that, I will spend
      // seven." "We cannot let her get space events."
      // Yellow keeps Solar Logistics' two titanium to trade with Io next generation.
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$OptimalAerobraking").done()
      fillInTask("Pay", "Titanium").chooseAlternative("Ok").done() // No titanium paid.
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "Here we go. Spache Elevator." "Am I spending all titanium for it? I'm gonna hold back one
      // titanium. I'm gonna spend nine titanium. Not 99, just nine." "Stupid fucking Spache
      // Elevator."
      // The retained titanium and Space Elevator's new production pay for Palladin Shipping next
      // generation.
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$SpaceElevator").done()
      fillInTask("Pay", "Titanium").chooseAlternative("Pay").chooseAmount(9).done()
      fillInTask("Pay", "Steel").chooseAlternative("Ok").done() // No steel paid.
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "I'm going to take my Nitrite Reducing Bacteria action to add a microbe."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$NitriteReducingBacteria, Action1>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Solar Logistics also gives me a discount of two on my Earth tag. I'm gonna pay seven for
      // Imported Advanced GHG. Increase heat production two steps. I get a card for Solar
      // Logistics,
      // and from Optimal Aerobraking I get three money and three heat."
      // Yellow continues to keep two titanium to trade with Io next generation.
      expectChanges("-4 MC, 3 Heat, PROD[2 Heat]") {
        fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
        fillInTask("PlayCard").choose("CardFront" to "$ImportOfAdvancedGhg").done()
        fillInTask("Pay", "Titanium").chooseAlternative("Ok").done() // No titanium paid.
      }
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "I am going to use my Spache Elevator action to lose one steel and gain five real."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$SpaceElevator, Action1>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "Oh my god, I'm an idiot. Dude, I have plants. I can plant Potatoes. Why didn't I do this
      // last freaking generation? That cost me two money. I lose two plants, and I get two money
      // production." "Yep. Potatoes are mine. Andy Weir up in here."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$Potatoes").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    yellow.fillInTask("StandardAction").chooseAlternative("Pass").done()
    with(green) {
      // "I'm spending three steel and 12 real on Research Outpost, which lets me place a city tile.
      // I can place it right here, pay five money, and get another colony, which I'm just gonna
      // drop
      // on Luna. That gives me two money production, getting me back to zero again."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$ResearchOutpost").done()
      fillInTask("Pay", "Steel").chooseAlternative("Pay").chooseAmount(3).done()
      fillInTask("CityTile")
          .choose("CityTile" to "NormalCityTile")
          .choose("LandArea" to "Cimmeria_3_3")
          .done()
      fillInTask("Colony").choose("ColonyTile" to "Luna").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "I'm going to use my Floater Technology action to add a floater to Local Shading."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$FloaterTechnology, Action1>").done()
      fillInTask("Floater").choose("ResourceHolder" to "LocalShading").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "[Yellow]'s already passed, so I'm the one holding things up. We play Peroxide Power, which
      // costs me six real money. I lose one money production and add two energy production."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$PeroxidePower").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "I'm going to take my Dirigibles action, which lets me add a floater to any card, and add
      // it
      // to Jet Stream Microscrappers. Then I take my Celestic action to add another floater to Jet
      // Stream Microscrappers." "I forgot I'm supposed to pay titanium for both of those." "You
      // don't have to pay." "I don't have to pay, thank God."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Dirigibles, Action1>").done()
      fillInTask("Floater").choose("ResourceHolder" to "JetStreamMicroscrappers").done()
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Celestic, Action1>").done()
      fillInTask("Floater").choose("ResourceHolder" to "JetStreamMicroscrappers").done()
    }
    green.fillInTask("StandardAction").chooseAlternative("Pass").done()
    with(blue) {
      // "Then I'm going to use my Jet Stream Microscrappers action to remove two floaters and raise
      // Venus one step." "Ah shit, I made it easier for [Yellow] to get the bonus."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$JetStreamMicroscrappers, Action2>").done()
    }
    blue.fillInTask("StandardAction").chooseAlternative("Pass").done()

    // "I'm going to raise Venus." "Venus is now at eight. No one gets the card. We're still at one
    // oxygen, two oceans, and no temperature raises yet."
    blue
        .fillInTask("GlobalParameter")
        .choose("GlobalParameter" to "VenusStep")
        .done()
        .expect("0 TerraformRating")

    with(green) {
      assertProduction(m = -1, s = 0, t = 1, p = 1, e = 5, h = 0)
      assertResources(m = 20, s = 0, t = 2, p = 4, e = 5, h = 0)
      assertCounts(20 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 4, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertResources(m = 30, s = 0, t = 2, p = 0, e = 0, h = 0)
      assertCounts(26 to "TerraformRating")
      assertCardResources(1 to LocalShading, 1 to NitriteReducingBacteria)
    }
    with(yellow) {
      assertProduction(m = 1, s = 0, t = 2, p = 0, e = 1, h = 2)
      assertResources(m = 43, s = 0, t = 4, p = 0, e = 1, h = 7)
      assertCounts(20 to "TerraformRating")
    }
    assertSidebar(gen = 3, temp = -30, oxygen = 1, oceans = 2, venus = 8)

    yellow.fillInTask("ProjectCard").chooseAmount(1).done()
    green.fillInTask("ProjectCard").chooseAmount(2).done()
    blue.fillInTask("ProjectCard").chooseAmount(3).done()

    with(yellow) {
      // "I think I will start by paying two titanium to trade with Io. That's ten heat for me."
      expectChanges("-2 Titanium, 10 Heat") {
        fillInTask("StandardAction").narrow("UseAction<TradeAction, Action3>").done()
        fillInTask("Trade").choose("ColonyTile" to "Io").done()
      }
      // "Probably should have done this sooner, but Business Network."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$BusinessNetwork, Action1>").done()
      fillInTask("ProjectCard").chooseAmount(1).done()
    }
    with(green) {
      // "What I'm gonna do is fly my boat. I'm gonna spend three energy, fly my boat to Ganymede,
      // take five planta. When you want to convert plants to greenery, you hit plants, but then
      // there's this 'plant forest'—for whatever reason they call it plant forest. It automatically
      // gives me the TR and takes away the eight plants and everything."
      expectChanges("-3 Energy, 5 Plant") {
        fillInTask("StandardAction").narrow("UseAction<TradeAction, Action2>").done()
        fillInTask("Trade").choose("ColonyTile" to "Ganymede").done()
      }
      // "My forest is gonna go on 3-2, so that it's next to my city and gets two money from ocean.
      // That raises oxygen to two percent, and we got my TR already. That was my two actions."
      expectChanges("-8 Plant, 2 MC, TerraformRating") {
        fillInTask("UseAction").narrow("UseAction<ConvertPlantsAction, Action1>").done()
        fillInTask("GreeneryTile").choose("LandArea" to "Cimmeria_3_2").done()
      }
    }
    with(blue) {
      // "How much money do I have? Fuck. Okay, FML. I guess I'm playing Io Sulphur Research. I pay
      // for it first, then draw three cards. I have two Venus tags."
      // "I'll buy Reno."
      expectChanges("2 ProjectCard") {
        fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
        fillInTask("PlayCard").choose("CardFront" to "$IoSulphurResearch").done()
        fillInTask("ProjectCard").chooseAlternative("ProjectCard", count = 3).done()
      }
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "For six monies, Carbonate Processing. Lose energy production, gain three heat production."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$CarbonateProcessing").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "This is really cool, actually. I play Mining Area. It costs three money. That solves both
      // my—because I need steels for this thing, you know? It gives me one for this turn and solves
      // my problem for the future as well. That gives me one steel and one steel production."
      expectChanges("Steel, PROD[Steel]") {
        fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
        fillInTask("PlayCard").choose("CardFront" to "$MiningArea").done()
        fillInTask("MiningArea_SpecialTile").choose("LandArea" to "Cimmeria_4_3").done()
      }
      // "As my second action, I can spend eight on the Land Shaper."
      fillInTask("StandardAction").narrow("UseAction<ClaimMilestoneAction, Action1>").done()
      fillInTask("Milestone").choose("Milestone" to "Landshaper").done()
    }
    with(blue) {
      // "I'm going to play Mars University."
      // "Did you get that from the—"
      // "When I just drew three cards from Io Sulphur Research, I did draw Mars University, and I'm
      // playing it now. I lose eight money. I get a science tag and an Earth tag."
      // "It doesn't have anything that happens right now."
      // "Well, it does, if you would like to pitch a card and draw a card."
      // "Nah, I like my cards."
      // "The struggle is real."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$MarsUniversity").done()
      fillInTask("ProjectCard")
          .chooseAlternative("Ok")
          .done() // Decline Mars University's discard and draw.
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "For Power Infrastructure."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$PowerInfrastructure").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "Now I can use Spache Elevator to use one steel and get five real. Was there anything else
      // I was in a hurry to do? No."
      // "Look, guys, I'm jet-lagged. Jet lag makes me stupid, okay? We know this. This is a true
      // fact about me."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$SpaceElevator, Action1>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "I'm just gonna use my Nitrite Reducing Bacteria action to add a microbe."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$NitriteReducingBacteria, Action1>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "I now have two power tags, so I can play Fusion Tower [Fusion Power]."
      // "Dang it, I didn't think you were gonna be able to play that. I thought it might come back
      // to me."
      // "Increase energy production three steps."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$FusionPower").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "I'm gonna use Titan Shuttles to add two floaters."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$TitanShuttles, Action1>").done()
      fillInTask("Floater").choose("ResourceCard" to "TitanShuttles").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "You'll never guess: I'm gonna use my Local Shading action to spend a floater to increase
      // my
      // money production by one."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$LocalShading, Action2>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "I will use my remaining ten money to play House Printing, gain steel production."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$HousePrinting").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "I will use my Paladin Shipping action to pay two titanium to raise the temperature to
      // minus
      // 28 and get my second TR."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$PalladinShipping, Action1>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "I'm gonna use my Floater Technology action to add one floater to Local Shading."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$FloaterTechnology, Action1>").done()
      fillInTask("Floater").choose("ResourceHolder" to "LocalShading").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "I have 17 heat."
      // Yellow's app and the photographed track require both ordinary heat conversions.
      fillInTask("UseAction").narrow("UseAction<ConvertHeatAction, Action1>").done()
      expectChanges("PROD[Heat]") {
        fillInTask("UseAction").narrow("UseAction<ConvertHeatAction, Action1>").done()
      }
    }
    green.fillInTask("StandardAction").chooseAlternative("Pass").done()
    with(blue) {
      // The recording goes silent here. Blue's app adds one TR, and these three unused actions
      // are the ordinary card sequence that produces exactly that result.
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Dirigibles, Action1>").done()
      fillInTask("Floater").choose("ResourceHolder" to "JetStreamMicroscrappers").done()
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Celestic, Action1>").done()
      fillInTask("Floater").choose("ResourceHolder" to "JetStreamMicroscrappers").done()
    }
    yellow.fillInTask("StandardAction").chooseAlternative("Pass").done()
    with(blue) {
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$JetStreamMicroscrappers, Action2>").done()
    }
    blue.fillInTask("StandardAction").chooseAlternative("Pass").done()

    // board-18-39-07.jpg: end of the Generation 3 action phase, before production.
    assertSidebar(gen = 3, temp = -24, oxygen = 2, oceans = 2, venus = 10)
    green.assertCounts(
        22 to "TerraformRating",
        1 to "GreeneryTile<Cimmeria_3_2>",
        1 to "CityTile<Cimmeria_3_3>",
        1 to "MiningArea_SpecialTile<Cimmeria_4_3>",
        1 to "Landshaper",
    )
    blue.assertCounts(27 to "TerraformRating")
    yellow.assertCounts(22 to "TerraformRating")

    // Venus is the only World Government choice consistent with the stated Generation 4 values.
    yellow
        .fillInTask("GlobalParameter")
        .choose("GlobalParameter" to "VenusStep")
        .done()
        .expect("0 TerraformRating")

    with(green) {
      assertProduction(m = -1, s = 1, t = 1, p = 1, e = 5, h = 0)
      assertResources(m = 31, s = 1, t = 1, p = 2, e = 5, h = 2)
    }
    with(blue) {
      assertProduction(m = 5, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertResources(m = 34, s = 0, t = 2, p = 0, e = 0, h = 0)
    }
    with(yellow) {
      assertProduction(m = 1, s = 1, t = 2, p = 0, e = 3, h = 6)
      assertResources(m = 23, s = 1, t = 4, p = 0, e = 3, h = 8)
    }
    assertSidebar(gen = 4, temp = -24, oxygen = 2, oceans = 2, venus = 12)

    // "I think I'll buy just one."
    // "Much to think about."
    // "Is it crazy if I buy three?"
    // "Three is just a lot, because that's nine money."
    // "Three clicks."
    // "I think I'm also buying three. Three cards for nine money."
    with(green) {
      green.fillInTask("ProjectCard").chooseAmount(1).done()
    }
    with(blue) {
      blue.fillInTask("ProjectCard").chooseAmount(1).done()
    }
    with(yellow) {
      yellow.fillInTask("ProjectCard").chooseAmount(3).done()
    }

    with(green) {
      // "Research Colony. One titanium and 16 real money. Not only does it let me place another
      // colony, it lets me go somewhere that I'm already at. I get another two money production, so
      // I'm back to positive money production again."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$ResearchColony").done()
      fillInTask("Pay", "Titanium").chooseAlternative("Pay").chooseAmount(1).done()
      fillInTask("Colony").choose("ColonyTile" to "Luna").done()
      // "When I use my second action to fly my boat, I get ten plus two plus two, and [Yellow] gets
      // two. So I get 14. And that was my two actiones. Oh, and I draw two cards."
      // "They're just lousy Earth-tag cards."
      // "Are you saying that to piss me off?"
      // "Yes."
      // "Nice."
      expectChanges("-3 Energy, 14 MC, 2 MC<Yellow>") {
        fillInTask("StandardAction").narrow("UseAction<TradeAction, Action2>").done()
        fillInTask("Trade").choose("ColonyTile" to "Luna").done()
      }
    }
    with(blue) {
      // "I'm gonna play Ice Moon Colony. I'm gonna spend two titanium, which equals six money, and
      // 17 money. I'm gonna place an ocean first: row eight, column nine. I get two plants and one
      // TR."
      // "I'm a little behind on my boops here. We have two oxygen, and we have three temperature
      // boops. We're at negative 24."
      // "Let the record show I just caught up on my boops."
      // "So did I."
      // "As did I. We all did."
      // "Then I place my colony on Titan. I immediately get three floaters for one card, so I'm
      // gonna
      // put them on Jet Stream Microscrappers."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$IceMoonColony").done()
      fillInTask("Pay", "Titanium").chooseAlternative("Pay").chooseAmount(2).done()
      fillInTask("OceanTile").choose("WaterArea" to "Cimmeria_8_9").done()
      fillInTask("Colony").choose("ColonyTile" to "Titan").done()
      fillInTask("Floater").choose("ResourceHolder" to "JetStreamMicroscrappers").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "This is a lot of colonies on the board."
      // "I need more money. Why do I have so little money? And so little everything? Why are all my
      // productions so low? I feel like I never draw any cards that just give me productions
      // without
      // also costing other productions that I already don't have."
      // "Business Network. A card to inspect. Inspect, protect, detect."
      // "You know what I just realized. Minus one money—"
      // "You never did that?"
      // "I never."
      // "Wow. Take yourself a minus one money production now and maybe minus two or three money."
      // "I know I played this first gen, and it is gen four. So that means minus three money.
      // Thanks
      // for noticing that. We would have caught it later."
      // "What are you going to do with that card?"
      // "Nah. Booped."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$BusinessNetwork, Action1>").done()

      fillInTask("ProjectCard").chooseAmount(1).done()

      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    // Yellow then corrected Business Network's previously omitted production loss and the three
    // generations of extra income it had caused.
    yellow.exMachina("-3 MC, PROD[-MC]")
    with(green) {
      // "Space Elevate. A steel for five money."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$SpaceElevator, Action1>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "I will do my Nitrite Reducing Bacteria action to add a microbe."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$NitriteReducingBacteria, Action1>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "I will pay 15 money for Love Tube Settlement [Lava Tube Settlement]. Lose energy
      // production, gain two money production, and place a city tile."
      // "I will place it on Hadriacus Mons, 6-2, for two cards."
      // "I'm 6'2”, by the way."
      // "Oh no, I dropped my feminist literature."
      // "You did all your stuff you had to do?"
      // "Yes."
      // "I have money."
      // "Yeah, yeah, fuck you."
      // No reason for keeping the steel is evident. Yellow next spends it, together with one steel
      // from production, to buy Electro Catapult; both cards accept steel at the same value.
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$LavaTubeSettlement").done()
      fillInTask("Pay", "Steel").chooseAlternative("Ok").done() // No steel paid.
      fillInTask("CityTile")
          .choose("CityTile" to "NormalCityTile")
          .choose("VolcanicArea" to "Cimmeria_6_2")
          .done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "With this money that, as previously observed, I have. How did I get that money, by the
      // way?
      // Oh, Space Elevating. Spillivate."
      // "It's your lucky day, [Yellow]. I'm playing a space event. That cost me 22. Paladin
      // Shipping
      // gives me a titanium. You get a card. I'll take a titanium, take two plants, raise oxygen to
      // three and get a TR, and place an ocean tile and get a TR. The ocean tile will be 7-9 for
      // two
      // plants and two money. Au revoir."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$TowingAComet").done()
      fillInTask("OceanTile").choose("WaterArea" to "Cimmeria_7_9").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "I'm gonna take my Local Shading action to remove a floater and gain a money production."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$LocalShading, Action2>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "When did we get a third oxygen?"
      // "Just now."
      // "Good, because I wasn't paying attention."
      // "I pitch a card."
      // "You sell a Patento? As your turn?"
      // "Yes."
      fillInTask("StandardAction").narrow("UseAction<UseStandardProjectAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<SellPatentsProject, Action1>").done()
      fillInTask("ProjectCard").chooseAmount(1).done()

      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "I am going to Titan Shuttles to place two floaters."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$TitanShuttles, Action1>").done()

      fillInTask("Floater").choose("ResourceCard" to "TitanShuttles").done()

      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "I'm going to use my Floater Technology action to add a floater to Local Shading."
      // "Keep in mind, it can be useful to park them on Dirigibles too, because then you have the
      // option of using them as money. Local Shading, you can always next turn put it on and take
      // it
      // off, so it doesn't really do any advantage to have it there this turn. On the other hand,
      // there might be a card that does even better that collects floaters."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$FloaterTechnology, Action1>").done()

      fillInTask("Floater").choose("ResourceHolder" to "LocalShading").done()

      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "I use Power Infrastructure: spend an energy to gain money."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$PowerInfrastructure, Action1>").done()
      fillInTask("Energy").chooseAmount(1).done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    green.fillInTask("StandardAction").chooseAlternative("Pass").done()
    with(blue) {
      // "I will take my Jet Stream Microscrappers action to remove two floaters and raise Venus to
      // 14."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$JetStreamMicroscrappers, Action2>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Water to Venus. Spend three titanium."
      // "She always gets it. Such an asshole."
      // "I raise it to 16, which gives an extra TR. And I get three money and three heat from
      // Optimal
      // Aerobraking."
      // "I never told you that I hate you."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$WaterToVenus").done()
      fillInTask("Pay", "Titanium").chooseAlternative("Pay").chooseAmount(3).done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "I use my Dirigibles action to put a floater on Jet Stream Microscrappers, and my Celestic
      // action to put a floater on Dirigibles. And that's all, folks."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Dirigibles, Action1>").done()

      fillInTask("Floater").choose("ResourceHolder" to "JetStreamMicroscrappers").done()

      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Celestic, Action1>").done()

      fillInTask("Floater").choose("ResourceHolder" to "Dirigibles").done()
    }
    yellow.fillInTask("StandardAction").chooseAlternative("Pass").done()
    blue.fillInTask("StandardAction").chooseAlternative("Pass").done()

    // board-18-58-23.jpg: end of the Generation 4 action phase.
    assertSidebar(gen = 4, temp = -24, oxygen = 3, oceans = 4, venus = 16)
    green.assertCounts(24 to "TerraformRating", 1 to "Milestone")
    blue.assertCounts(29 to "TerraformRating")
    yellow.assertCounts(24 to "TerraformRating", 1 to "CityTile<Cimmeria_6_2>")
    admin.assertCounts(
        1 to "OceanTile<Cimmeria_2_1>",
        1 to "OceanTile<Cimmeria_9_5>",
        1 to "OceanTile<Cimmeria_8_9>",
        1 to "OceanTile<Cimmeria_7_9>",
    )
    green.assertCardResources(4 to TitanShuttles)
    blue.assertCardResources(
        1 to LocalShading,
        3 to NitriteReducingBacteria,
        2 to JetStreamMicroscrappers,
        1 to Dirigibles,
    )

    // "I'll put another ocean out here at 1-1. Oceans go up to five."
    // "Oceans rise and pass fall."
    green
        .fillInTask("GlobalParameter")
        .choose("GlobalParameter" to "OceanTile")
        .choose("MarsArea" to "Cimmeria_1_1")
        .done()
        .expect("0 TerraformRating")

    // All three app histories: post-production Generation 5 pause, before Research.
    with(green) {
      assertProduction(m = 1, s = 1, t = 1, p = 1, e = 5, h = 0)
      assertResources(m = 30, s = 1, t = 2, p = 7, e = 5, h = 4)
      assertCounts(24 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 6, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertResources(m = 43, s = 0, t = 0, p = 2, e = 0, h = 0)
      assertCounts(29 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 2, s = 1, t = 2, p = 0, e = 2, h = 6)
      assertResources(m = 35, s = 2, t = 3, p = 0, e = 2, h = 19)
      assertCounts(24 to "TerraformRating")
    }
    assertSidebar(gen = 5, temp = -24, oxygen = 3, oceans = 5, venus = 16)
    // "Six, nine, and eight. Perfect. Perfect."
    green.assertCounts(6 to "ProjectCard")
    blue.assertCounts(9 to "ProjectCard")
    yellow.assertCounts(8 to "ProjectCard")

    // The resumed table forms to the right. The app histories record the resulting purchases.
    blue.fillInTask("ProjectCard").chooseAmount(2).done()
    yellow.fillInTask("ProjectCard").chooseAmount(1).done()
    green.fillInTask("ProjectCard").chooseAmount(1).done()

    with(blue) {
      // "I'm gonna play Protected Valley."
      // "We're gonna save the River Valley. Yay."
      // "I pay 23 money. I don't give myself 23 money. I pay 23 money. I gain two money production,
      // and I place a greenery on an area reserved for ocean."
      // "Row nine, column nine. This is a city. Oh, it flips. Yes. Row nine, column nine, greenery.
      // Number nine, which gets me a plant."
      // "And two money."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$ProtectedValley").done()
      fillInTask("GreeneryTile").choose("WaterArea" to "Cimmeria_9_9").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "This feels like getting in the way of doing actual stuff, but I'm a bit paranoid. So,
      // Subterranean Reservoir."
      // "I know what you're doing."
      // "Yep. Pay 11 for an ocean to go top right, 1-5. Oceans are now at 6. Give myself TR as
      // well.
      // Can't forget."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$SubterraneanReservoir").done()
      fillInTask("OceanTile").choose("WaterArea" to "Cimmeria_1_5").done()
      // "And then pay 8."
      // "Eight money."
      // "Yes."
      // "And you claim the Merchant."
      // "As you could see, I was very close to being able to get that."
      // "Yeah. I just needed to place a greenery there. [Yellow] gets the Merchant."
      fillInTask("StandardAction").narrow("UseAction<ClaimMilestoneAction, Action1>").done()
      fillInTask("Milestone").choose("Milestone" to "Merchant").done()
    }
    with(green) {
      // "Oh, yeah. I was going to try to steal [Yellow]'s milestone."
      // "I'm afraid I'm going to spend three energy and fly my boat to Titan. Three floaters and a
      // floater. And [Blue], you get one floater."
      // "Yay!"
      // "I put my three and one on my only floater card, Titan Travels."
      // "I'm going to put my floater on Dirigibles."
      fillInTask("StandardAction").narrow("UseAction<TradeAction, Action2>").done()
      fillInTask("Trade").choose("ColonyTile" to "Titan").done()
      blue.fillInTask("Floater").choose("ResourceHolder" to "Dirigibles").done()
      fillInTask("Floater", count = 3).choose("ResourceHolder" to "TitanShuttles").done()
      fillInTask("Floater", "Green", count = 1).choose("ResourceHolder" to "TitanShuttles").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "Okay, so let me think. So that's now six money. Using 16 money and two floaters for my
      // Dirigibles card, I am going to play Stratopolis."
      // "Nice."
      // "And so I pay my 16 money first. And then I gain two money production. And I place a city
      // on
      // Stratopolis."
      // "You are the floater queen."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$Stratopolis").done()
      fillInTask("PayFromCard")
          .narrow("2 PayFromCard<$Dirigibles> FROM Floater<$Dirigibles>")
          .done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Now that I have much enticed, spend two energies to trade with Luna. I get 12. You get 12
      // and I get four."
      expectChanges("-2 Energy, 12 MC, 4 MC<Green>") {
        fillInTask("StandardAction").narrow("UseAction<TradeAction, Action2>").done()
        fillInTask("Trade").choose("ColonyTile" to "Luna").done()
      }
      // "And I will Business Network."
      // "You are business networking."
      // "Nah."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$BusinessNetwork, Action1>").done()
      fillInTask("ProjectCard").chooseAmount(1).done()
    }
    with(green) {
      // "Well, I'll do my thing where I spend one steel to get five real by using Spache Elevator.
      // That's my turn. Go off."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$SpaceElevator, Action1>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "I'm out of money, so I am going to use my Nitrite Reducing Bacteria action to remove three
      // microbes and raise my TR one step."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$NitriteReducingBacteria, Action2>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Man, I want to do stuff, but—wait. Wait. I think I can. You know what? I can always just
      // start by—can't really lose anything if I—yes, I will spend my two steel and 13 real. Then
      // immediately spend a plant to gain seven."
      // "Oh, I should remember to lose an energy production."
      // "Oh, yeah. Almost wrong."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$ElectroCatapult").done()
      fillInTask("Pay", "Steel").chooseAlternative("Pay").chooseAmount(2).done()
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$ElectroCatapult, Action1>").done()
    }
    with(green) {
      // "I think I should probably use Titan Shuttles to take eight titanium. Oyga."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$TitanShuttles, Action2>").done()
      fillInTask("Floater").chooseAmount(8).done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "I am going to use my Local Shading action. I remove a floater and I get a money
      // production."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$LocalShading, Action2>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Robotic Workforce."
      // "Yeah, I didn't like giving that to you."
      // "Spend nine. You have so many building production cards."
      // "Yeah, I will replicate Fusion Power."
      // "Fuck. I didn't see that one."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$RoboticWorkforce").done()
      fillInTask("CopyProductionBox").choose("CardFront" to "FusionPower").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "On my turn, it's time to play Io Mining Industries. That cost me ten titanium, which is
      // worth three, and I get two titanium production, two money production, and I'm going to get
      // bank on Jupiter tags."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$IoMiningIndustries").done()
      fillInTask("Pay", "Titanium").chooseAlternative("Pay").chooseAmount(10).done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "I'm using Jet Stream Microscrappers to spend two floaters and raise the Venus."
      // "To 18."
      // "To 18, and I give myself a TR for that."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$JetStreamMicroscrappers, Action2>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Alright, I think I've done the urgent stuff. Now I can double heat boop."
      // "Okay. Boop. That takes temp to minus 20, and I get a heat product."
      fillInTask("UseAction").narrow("UseAction<ConvertHeatAction, Action1>").done()
      expectChanges("PROD[Heat]") {
        fillInTask("UseAction").narrow("UseAction<ConvertHeatAction, Action1>").done()
      }
    }
    with(green) {
      // "Let's just direct some impactors. Direct impact. I pay seven for that."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$DirectedImpactors").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "I'm going to use Floater Technology to add one floater to Dirigibles."
      // "Okay. Alright. Dirigibles."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$FloaterTechnology, Action1>").done()
      fillInTask("Floater").choose("ResourceHolder" to "Dirigibles").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Actually, you know what the hell. Why not? I'm going to sell a card for a money and then
      // pay five for Floating Habs."
      // "Ah, that's the one that's two floaters to a victory point."
      // "Yep. I has it. I finally have one, two science tags."
      fillInTask("StandardAction").narrow("UseAction<UseStandardProjectAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<SellPatentsProject, Action1>").done()
      fillInTask("MC").chooseAmount(1).done()
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$FloatingHabs").done()
    }
    with(green) {
      // "I'm going to Release my Inert Gases, which costs all 13 of my money. And it just gives me
      // two TR. And it flips."
      // "Oh, I forgot to use Directed Impactors. Built it for nothing."
      // "Yeah, I think I'm kind of fading too. In terms of my awareness."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$ReleaseOfInertGases").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "I'm going to use my Stratopolis action to add two floaters to my Dirigibles. Sweet."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Stratopolis, Action1>").done()
      fillInTask("Floater").choose("ResourceCard" to "Dirigibles").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "I'm going to pitch two cards for two money. Then spend two money to add a floater to any
      // card, and that will be Floating Habs."
      fillInTask("StandardAction").narrow("UseAction<UseStandardProjectAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<SellPatentsProject, Action1>").done()
      fillInTask("MC").chooseAmount(2).done()
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$FloatingHabs, Action1>").done()
      fillInTask("Floater").choose("ResourceHolder" to "FloatingHabs").done()
    }
    green.fillInTask("StandardAction").chooseAlternative("Pass").done()
    with(blue) {
      // "I'm going to use my Extremophiles action to add two floaters to my Dirigibles."
      // "I'm going to play Extremophiles."
      // "Does that have a requirement?"
      // "Two science tags?"
      // The app history shows that a Dirigibles floater paid the three-M€ cost; Blue had no M€.
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$Extremophiles").done()
      fillInTask("PayFromCard").narrow("PayFromCard<$Dirigibles> FROM Floater<$Dirigibles>").done()
      // "Awesome. Okay, I'm going to use my Extremophiles action to add one microbe to Nitrite
      // Reducing Bacteria."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Extremophiles, Action1>").done()
      fillInTask("Microbe").choose("ResourceHolder" to "NitriteReducingBacteria").done()
    }
    yellow.fillInTask("StandardAction").chooseAlternative("Pass").done()
    with(blue) {
      // "I guess I'm going to use my Celestic action to add a dirigible."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Celestic, Action1>").done()
      fillInTask("Floater").choose("ResourceHolder" to "Dirigibles").done()
      // "And I'm probably going to use my Dirigibles action to add another dirigible. Right? Do I
      // even have enough Venus tags for that to be worth it? No. So I'm going to use my Dirigibles
      // action to add a floater to Local Shading."
      // "Wait, no, that's not true. I did math wrong. Actually, I am going to, instead of adding it
      // to Local Shading, I'm going to add this floater to Dirigibles. So be it."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Dirigibles, Action1>").done()
      fillInTask("Floater").choose("ResourceHolder" to "Dirigibles").done()
      // "Now that's $12 worth. And then I'm going to use them to pay for Venus Trade Hub."
      // "Damn."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$VenusTradeHub").done()
      fillInTask("PayFromCard")
          .narrow("4 PayFromCard<$Dirigibles> FROM Floater<$Dirigibles>")
          .done()
      fillInTask("StandardAction").chooseAlternative("Pass").done()
    }

    // "Okay, Jaybird, what do you want to move for World Government? Venus, temperature, oxygen,
    // ocean."
    // "Let's move oxygen."
    // "Okay. Oxygen to five percent."
    // board-21-08-05.jpg is after production and this World Government step.
    blue
        .fillInTask("GlobalParameter")
        .choose("GlobalParameter" to "OxygenStep")
        .done()
        .expect("0 TerraformRating")
    with(green) {
      assertProduction(m = 3, s = 1, t = 3, p = 1, e = 5, h = 0)
      assertResources(m = 29, s = 1, t = 3, p = 8, e = 5, h = 6)
      assertCounts(26 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 11, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertResources(m = 43, s = 0, t = 0, p = 3, e = 0, h = 0)
      assertCounts(32 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 2, s = 1, t = 2, p = 0, e = 4, h = 7)
      assertResources(m = 29, s = 1, t = 5, p = 1, e = 4, h = 10)
      assertCounts(27 to "TerraformRating", 1 to "Milestone")
    }
    assertSidebar(gen = 6, temp = -20, oxygen = 5, oceans = 6, venus = 18)

    // "Motherfucker. Bro, I don't want any of these fuck-ass things."
    // "Yeah, I like fuck-ass things."
    // "I'm going to buy two cards."
    // "You know what? I have too many fucking cards. I'm going to buy four cards."
    // "Yay! I'm going to live a little and do that, too, I think. I'm going to buy four cards for
    // 12 money. Boop."
    blue.fillInTask("ProjectCard").chooseAmount(2).done()
    yellow.fillInTask("ProjectCard").chooseAmount(0).done() // Keep all four Research cards.
    green.fillInTask("ProjectCard").chooseAmount(0).done() // Keep all four Research cards.

    with(yellow) {
      // "Well, why don't I go ahead and pay eight for Engineer. And I think I will pay two energies
      // to trade with Luna. I'll collect my ten—no, twelve monies. You get your four."
      // "Oh, put your boat on."
      // "Yes, thank you."
      fillInTask("StandardAction").narrow("UseAction<ClaimMilestoneAction, Action1>").done()
      fillInTask("Milestone").choose("Milestone" to "Engineer").done()
      expectChanges("-2 Energy, 12 MC, 4 MC<Green>") {
        fillInTask("StandardAction").narrow("UseAction<TradeAction, Action2>").done()
        fillInTask("Trade").choose("ColonyTile" to "Luna").done()
      }
    }
    with(green) {
      // "This kind of sucks. I shouldn't have taken so many cards. Okay, well, alright. I'm not
      // racing for anything anymore. I'll use Space Elevator to spend one steel and take five
      // real."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$SpaceElevator, Action1>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "I am going to buy some Red Ships. Oh, wait, that does—okay, which costs me two money."
      // "I should actually warn you that it doesn't pay out anything just yet."
      // "I know. Why would I not know that?"
      // "It will. It will, yeah."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$RedShips").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Alright, let me start with Imported GHG. Pay one titanium, two monies. Increase heat
      // production one, gain three heat, get a card from Earth card, from space event—yes, both
      // Earth
      // and space. Doesn't make any sense. And gain three money, three heat. Jesus Christ, that's
      // insane. Be nicer if I got more space events."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$ImportedGhg").done()
      fillInTask("Pay", "Titanium").chooseAlternative("Pay").chooseAmount(1).done()
      // "Sure, Business Network to inspect the card."
      // "Wait, didn't we say we can't—"
      // "I decided to put it in."
      // "Aw, thank you."
      // "The thing is that the card I think would be best with that I'd kind of want to play soon,
      // so
      // I'm gonna reject anyways."
      // "Reject."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$BusinessNetwork, Action1>").done()
      fillInTask("ProjectCard").chooseAmount(1).done()
    }
    with(green) {
      // "I'm gonna play Research Coordination for three."
      // "Oh God, you like the wild tags."
      // "I love them. They're so useful."
      // "You like to live on the wild side."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$ResearchCoordination").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "I'm going to play Snow Algae, which costs me 12 money, and it gives me one plant
      // production
      // and one heat production."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$SnowAlgae").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Spend a plant to gain seven money production—no, not money production, seven money
      // resource. That would be ridiculous."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$ElectroCatapult, Action1>").done()
      // "I probably may as well Cartel. One, two, three, four. Be nice if I had more good Point
      // Luna, but whatever. Four money production, yes, and the card."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$Cartel").done()
    }

    // She forgot to pay for Cartel (fixed later)
    yellow.exMachina("6 MC")

    with(green) {
      // "I am going to play Olympus Conferencia, which costs nine, and it gives me a science
      // resource
      // because it's a science tag."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$OlympusConference").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "Okay, I'm now going to play Red Spot Observatory, which costs 17 monies, and with Mars
      // University, can I draw the two cards that I get from this first and then decide how to
      // discard
      // one?"
      // "Okay, good. I draw two cards."
      // "Come on. Dropped card. Stupid-ass card."
      // "Let the record show that [Blue] dropped her stupid-ass card."
      // "Fuck you guys."
      // "I'm going to discard this one to draw a new card because of my Mars University effect.
      // Thanks. That is better."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$RedSpotObservatory").done()
      fillInTask("ProjectCard").narrow("-ProjectCard").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "I realize, you know, I got some cash flow. Stratospheric Expedition. Pay four titaniums."
      // "Add two floaters to any card. That will be Floating Habs. Draw two Venus cards. But also
      // save a card for me to draw because of—"
      // "Do you want that first or do you want the Venus cards first?"
      // "Venus cards first."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$StratosphericExpedition").done()
      fillInTask("Pay", "Titanium").chooseAlternative("Pay").chooseAmount(4).done()
      fillInTask("Floater").choose("ResourceHolder" to "FloatingHabs").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "I am going to spend three energy to fly my boat to Titan."
      // "To Titan."
      // "And [Blue] gets one floater, and I get two plus one floaters."
      // "I guess I'll go ahead and put my floater onto Local Shading."
      fillInTask("StandardAction").narrow("UseAction<TradeAction, Action2>").done()
      fillInTask("Trade").choose("ColonyTile" to "Titan").done()
      blue.fillInTask("Floater").choose("ResourceHolder" to "LocalShading").done()
      fillInTask("Floater", count = 2).choose("ResourceHolder" to "TitanShuttles").done()
      fillInTask("Floater", "Green", count = 1).choose("ResourceHolder" to "TitanShuttles").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "I don't have money to pay for any of my cards. I'm just doing actions."
      // "I'm going to take my Nitrate Reducing Bacteria action and add a microbe to my Nitrate
      // Reducing Bacteria. Bet you guys never would have guessed that I'm going to do something
      // crazy
      // like that."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$NitriteReducingBacteria, Action1>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Can I see your player board?"
      // "You suck. Apparently not. No, you can't see it."
      // "You know what? I don't have shit for my ass. I don't have anything. You know what? Leave
      // me
      // alone."
      // "[Green], I'm hiring raiders. Stealing three money from you. Boop. Boop, boop, doot, doot.
      // And
      // pay them one money for it."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$HiredRaiders").done()
      fillInTask("MC").chooseAlternative("MC", count = 3).choose("Owner" to "Green").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "That might just possibly screw me up, actually."
      // "Yay! Everybody dance! Everybody dance! Life is good!"
      // "God, I keep forgetting. Every time I trade, I forget to play Market Manipulation first.
      // Over and over."
      // "I'm gonna use Titan Shuttles to take three titanium."
      expectChanges("-3 Floater<$TitanShuttles>, 3 Titanium") {
        fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
        fillInTask("UseAction").narrow("UseAction<$TitanShuttles, Action2>").done()
        fillInTask("Floater").chooseAmount(3).done()
      }
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "I'm gonna use my Local Shading action to—not to add a floater—to spend a floater to add a
      // money production."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$LocalShading, Action2>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "I will play Cutting Edge Technology for 12."
      // "Nice. Love it?"
      // "Yeah."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$CuttingEdgeTechnology").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "I'm gonna plant a forest, as they say. Oxygen is now six, and my forest will go right
      // where
      // you think it will go, 2-2."
      // "Then, for my next trick, what I'm gonna do is I'm gonna play Sky Docks."
      // "Do you actually say 'for my next trick'? Did you take that from me, or did I take that
      // from
      // you?"
      // "I used to say that all the time."
      // "Okay, I took it from you."
      // "I do have the two Earth tags. I have to assign my wild tag as an Earth tag when I play it.
      // I'm gonna pay four titanium and five real. And I get a trade fleet."
      // "Yeah, you do. Heck. H-E-K-K."
      fillInTask("UseAction").narrow("UseAction<ConvertPlantsAction, Action1>").done()
      fillInTask("GreeneryTile").choose("LandArea" to "Cimmeria_2_2").done()
      // Green keeps exactly the two titanium used for Palladin Shipping later this generation.
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$SkyDocks").done()
      fillInTask("Pay", "Titanium").chooseAlternative("Pay").chooseAmount(4).done()
    }
    with(blue) {
      // "I'm using my Floater Technology action to put a floater somewhere fun, I guess. I don't
      // know."
      // "You're starting to drown in floaters."
      // "Put it on Celestic."
      // The spoken choice conflicts with the later definite Observatory draw, which establishes the
      // physical floater's destination.
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$FloaterTechnology, Action1>").done()
      fillInTask("Floater").choose("ResourceHolder" to "RedSpotObservatory").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Expat Ishtar for four. I gain three titanium, and it requires Venus ten percent."
      // "Yeah, we got that."
      // "Draw two Venus cards. What gives you a two discount?"
      // "Cutting Edge."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$IshtarExpedition").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "I'm gonna play Inventors' Guild. I call it Inventioner's Guild. That cost me seven money."
      // "And then I'm gonna use it."
      // "Use it tonight."
      // "Yeah, that's not bad. I'll pay three for that."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$InventorsGuild").done()
      fillInTask("ProjectCard")
          .chooseAlternative(
              "ProjectCard",
          )
          .done()
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$InventorsGuild, Action1>").done()
      fillInTask("ProjectCard").chooseAmount(0).done() // Keep the offered project.
    }
    with(blue) {
      // "I'm gonna use my Extremophiles action to put a microbe on my sulfite reducing bacteria.
      // Bet
      // you weren't expecting that."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Extremophiles, Action1>").done()
      fillInTask("Microbe").choose("ResourceHolder" to "NitriteReducingBacteria").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    // "I am now realizing I miscalculated. Bummer, dude. How dare I miscalculate."
    // "In multiple ways, I really could have played my cards better, quite literally."
    with(yellow) {
      // "We probably have at least three more generations left in this game, right?"
      // "Probably. Maybe even four? I don't know. It's hard to say."
      // "We're kind of focusing on not terraforming."
      // "Well, except for Venus, man."
      // "By the way, Venus doesn't need to be completed to end the game. Only the other three."
      // "But it's raising my terraforming."
      // "I know, I know. It's fun, yeah."
      // "11-4, Aerial Mappers. It's like the only way I have to conveniently raise my TR right now.
      // It's working wonders for me."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$AerialMappers").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "I am gonna use Paladin Shipping for almost the first time ever to get a Temp Boop to minus
      // 18."
      // "I don't know about that. It's at least the second."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$PalladinShipping, Action1>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "Wait a minute. When I played Inventor's Guild, I should have lost a Science Cube and drawn
      // a
      // card. But it's still your turn."
      // "I'm going to use my Red Spot Observatory action to spend a floater and draw a card. Card
      // me."
      // "Card. Cardi B."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$RedSpotObservatory, Action2>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "All right, for two, I had to actually get my second Venus tag down. Venus Governator."
      // "Governator."
      // "I gain two money per Earth."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$VenusGovernor").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    green.fillInTask("StandardAction").chooseAlternative("Pass").done()
    with(blue) {
      // "I'm going to use my Stratopolis action to add two floaters to Jet Stream Microscrappers."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Stratopolis, Action1>").done()
      fillInTask("Floater").choose("ResourceCard" to "JetStreamMicroscrappers").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "You know, before I forget, I can do this. Power Infrastructure. Spend two monies. Get—no,
      // spend two energies. Get two monies."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$PowerInfrastructure, Action1>").done()
      fillInTask("Energy").chooseAmount(2).done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "I'm going to use my Jet Stream Microscrappers action to raise the Venus. [Yellow], can you
      // move
      // the Venus? And I get a TR."
      // "Yay! Yay! Life is good."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$JetStreamMicroscrappers, Action2>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "I spend seven money on Floating Refinery. And I get one, two, three, four, five floaters
      // immediately added."
      // "Damn. And where are you adding them?"
      // "To Floating Refinery. It makes you add them there."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$FloatingRefinery").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "[Blue] would have really liked that card because it lets you pull floaters off of any card
      // you want."
      // "Oh, that would have been cool."
      // "Anyway, I'm going to use my Dirigibles action to add a floater to Dirigibles."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Dirigibles, Action1>").done()
      fillInTask("Floater").choose("ResourceHolder" to "Dirigibles").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "You know what? Before I forget, I'm going to heat boop. Boop up to minus 16."
      fillInTask("UseAction").narrow("UseAction<ConvertHeatAction, Action1>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "I'm going to use my Celestic action to add a cube to—to add a floater to, I guess, to
      // Celestic."
      // "I've never seen so many float boys."
      // "So many what?"
      // "Floater cards in my life. Did you say float boys?"
      // "Yeah, float boys. That's what [Yellow] calls them."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Celestic, Action1>").done()
      fillInTask("Floater").choose("ResourceHolder" to "Celestic").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "I'm going to add an Aerial Mapper."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$AerialMappers, Action1>").done()
      fillInTask("Floater").choose("ResourceHolder" to "AerialMappers").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "Okay, for one Dirigible and two monies, I'm gonna play Ishtar Mining, and I get a titanium
      // production."
      // "Yay! Yay! Yay! Yay! Kermit the Frog. Kermit the Frog goes, 'Yay!'"
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$IshtarMining").done()
      fillInTask("PayFromCard").narrow("PayFromCard<$Dirigibles> FROM Floater<$Dirigibles>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "I am feeling like I did not play this generation optimally."
      // "Nope. Sure didn't."
      // "Floating Refinery. Remove two from Floating Refinery to gain a titanium and two money."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$FloatingRefinery, Action2>").done()
      fillInTask("Floater").choose("ResourceHolder" to "FloatingRefinery").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "Just to have something to do, I'm gonna take my Red Ships action, which doesn't give me
      // anything."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$RedShips, Action1>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "I'm gonna use Floating Habs. Spend two money, add to Floating Habs."
      // "How do you have so much shit to do? I freaking pass."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$FloatingHabs, Action1>").done()
      fillInTask("Floater").choose("ResourceHolder" to "FloatingHabs").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    blue.fillInTask("StandardAction").chooseAlternative("Pass").done()
    with(yellow) {
      // "Heat boop. Heat up to 14—minus 14."
      // "Oh shit, I have not been tracking the temperature or anything up to minus 14."
      // "And then six and six."
      // "There's six oceans now?"
      // "Yeah."
      // "Has been for a while. Always has been."
      fillInTask("UseAction").narrow("UseAction<ConvertHeatAction, Action1>").done()
      fillInTask("StandardAction").chooseAlternative("Pass").done()
    }

    // "We decided that we would do World Government, which is whose choice?"
    // "Me."
    // "Okay, what are you gonna choose?"
    // "Venus. Venus to 22. Then it's picture time. Picture pages, picture pages."
    // board-21-40-27.jpg follows production and this WGT step.
    yellow
        .fillInTask("GlobalParameter")
        .choose("GlobalParameter" to "VenusStep")
        .done()
        .expect("0 TerraformRating")
    with(green) {
      assertProduction(m = 3, s = 1, t = 3, p = 1, e = 5, h = 0)
      assertResources(m = 31, s = 1, t = 3, p = 1, e = 5, h = 8)
      assertCounts(28 to "TerraformRating", 8 to "ProjectCard")
    }
    with(blue) {
      assertProduction(m = 12, s = 0, t = 1, p = 1, e = 0, h = 1)
      assertResources(m = 49, s = 0, t = 1, p = 4, e = 0, h = 1)
      assertCounts(33 to "TerraformRating", 8 to "ProjectCard")
    }
    with(yellow) {
      assertProduction(m = 8, s = 1, t = 2, p = 0, e = 4, h = 8)
      assertResources(m = 37, s = 2, t = 6, p = 0, e = 4, h = 11)
      assertCounts(29 to "TerraformRating", 7 to "ProjectCard")
    }
    assertSidebar(gen = 7, temp = -14, oxygen = 6, oceans = 6, venus = 22)

    // "We made [Yellow] pay six to account for her previous mistake. And then I bought three cards
    // and [Yellow] bought three cards. Yeah, and [Blue]'s buying one card."
    yellow.exMachina("-6 MC")
    green.fillInTask("ProjectCard").chooseAmount(1).done()
    yellow.fillInTask("ProjectCard").chooseAmount(1).done()
    blue.fillInTask("ProjectCard").chooseAmount(3).done()

    with(green) {
      // "Do you mind if we say that I played Market Manipulation just before I did that?" "Sure."
      // "I'll reduce Titan." Luna's raised track makes the following trade worth three more M€.
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "MarketManipulation").done()
      fillInTask("ColonyProduction")
          .choose("ColonyTile" to "Luna")
          .choose("ColonyTile" to "Titan")
          .done()

      // "I want to fly my boat to Luna. And I get 14 money and yellow gets two." After correcting
      // Market Manipulation's order, Green records the full 17 M€ trade result.
      expectChanges("-3 Energy, 17 MC, 2 MC<Yellow>") {
        fillInTask("StandardAction").narrow("UseAction<TradeAction, Action2>").done()
        fillInTask("Trade").choose("ColonyTile" to "Luna").done()
      }
    }
    with(blue) {
      // "I'm going to play Indentured Workers, Giant Ice Asteroid."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "IndenturedWorkers").done()
      // "I'm going to pay one titanium and 25 money." "I took your one plant, and I'm going to
      // place two oceans." "Row nine, column ... six and seven."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$GiantIceAsteroid").done()
      fillInTask("Pay", "Titanium").chooseAlternative("Pay").chooseAmount(1).done()
      fillInTask("Plant").choose("Owner" to "Green").chooseAmount(1).done()
      fillInTask("OceanTile").choose("WaterArea" to "Cimmeria_9_6").done()
      fillInTask("OceanTile").choose("WaterArea" to "Cimmeria_9_7").done()
    }
    with(yellow) {
      // "I will Business Network. Look at the card. Keep."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$BusinessNetwork, Action1>").done()
      fillInTask("ProjectCard").chooseAmount(0).done() // Keep the offered project.
      // "Technology Demonstration. I paid five, which will be one titanium, two real. Draw two
      // cards, but actually I draw three because of that. And I gain my three money, three heat."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$TechnologyDemonstration").done()
      fillInTask("Pay", "Titanium").chooseAlternative("Pay").chooseAmount(1).done()
    }
    with(green) {
      // "Play Invention Contest for free. That gives me a little ... science resource on Olympus
      // Conference. And then I look at three cards from the deck. And I get to keep ... this one."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$InventionContest").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "Use my Nitrite Reducing Bacteria action to remove three microbes and raise my TR."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$NitriteReducingBacteria, Action2>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "I will Asteroid. That's four titanium and two real." "[Blue], you lose three plants."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$AsteroidCard").done()
      fillInTask("Pay", "Titanium").chooseAlternative("Pay").chooseAmount(4).done()
      fillInTask("Plant").chooseAmount(3).done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "Quantum Extractor. That costs me 11. It gives me four energy production."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$QuantumExtractor").done()
      fillInTask("ProjectCard")
          .chooseAlternative(
              "ProjectCard",
          )
          .done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "Use my Floater Technology action to add a floater to Local Shading."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$FloaterTechnology, Action1>").done()
      fillInTask("Floater").choose("ResourceHolder" to "LocalShading").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Use Floating Refinery. Remove two things ... to take a titanium and two money."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$FloatingRefinery, Action2>").done()
      fillInTask("Floater").choose("ResourceHolder" to "FloatingRefinery").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    green.assertCounts(3 to "Titanium") // Green applog entry 170 starts from three titanium.
    with(green) {
      // "Use two titanium and my Paladin action ... to raise the temperature to minus six."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$PalladinShipping, Action1>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "Take my Local Shading action to turn that floater into a money production."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$LocalShading, Action2>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Pay four titanium and thirteen real money for L1 Trade Terminal."
      // "I add one to Floating Habs ... and add Aerial Mapper, add Floating Refineries."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$L1TradeTerminal").done()
      fillInTask("Pay", "Titanium").chooseAlternative("Pay").chooseAmount(4).done()
      fillInTask("FloatingHabs").chooseAmount(1).done()
      fillInTask("AerialMappers").chooseAmount(1).done()
      fillInTask("FloatingRefinery").chooseAmount(1).done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "Use my Space Elevator to destroy one steel and gain five real."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$SpaceElevator, Action1>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "Use my Stratopolis action to add two floaters to Jetstream Microscrapper."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Stratopolis, Action1>").done()
      fillInTask("Floater").choose("ResourceCard" to "JetStreamMicroscrappers").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Spend two energies to trade with Io ... 13 heateroonies for me."
      expectChanges("-2 Energy, 13 Heat") {
        fillInTask("StandardAction").narrow("UseAction<TradeAction, Action2>").done()
        fillInTask("Trade").choose("ColonyTile" to "Io").done()
        fillInTask("ColonyProduction").chooseAlternative("ColonyProduction", count = 2).done()
      }
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "Use my Inventors' Guild and decide whether to buy this ... card. Nothing."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$InventorsGuild, Action1>").done()
      fillInTask("ProjectCard").chooseAmount(1).done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "Spend two floaters and raise Venus one step. Venus is at 24 and I get a TR."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$JetStreamMicroscrappers, Action2>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "I will spend a steel to gain seven real."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$ElectroCatapult, Action2>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "I will play Molecular Printing. That cost me nine ... it gives me eight."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$MolecularPrinting").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "Use my Extremophiles action to add a microbe to Nitrate Reducing Bacteria."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Extremophiles, Action1>").done()
      fillInTask("Microbe").choose("ResourceHolder" to "NitriteReducingBacteria").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Aerial Mappers remove to card."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$AerialMappers, Action2>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "Anti-Gravity Technology. I pay 12 for that."
      // "I have one, two, three, four, five, six, seven science tags."
      // The seven are Research Outpost, Research Colony, Olympus Conference, Inventors' Guild,
      // Quantum Extractor, Molecular Printing, and Research Coordination's wild tag.
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$AntiGravityTechnology").done()
      fillInTask("ProjectCard")
          .chooseAlternative(
              "ProjectCard",
          )
          .done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "Get ready for some grass. Grass is grass production and three plants."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$Grass").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Comet for Venus. I pay 11. Raise Venus one step ... you lose four money."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$CometForVenus").done()
      fillInTask("MC").choose("Owner" to "Blue").chooseAmount(4).done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "Take my Titan Shuttles action to put two Jovian floaters on Titan Shuttles."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$TitanShuttles, Action1>").done()
      fillInTask("Floater").choose("ResourceCard" to "TitanShuttles").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "Take my Red Spot Observatory action to add a floater to Red Spot Observatory."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$RedSpotObservatory, Action1>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Five for Sister Planet Support ... increase money production three steps and take a card."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$SisterPlanetSupport").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "This makes it four Earth tags. And that cost me seven money ... two titanium production."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$LunarMining").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "Use the Dirigibles action to add a floater to Celestic."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Dirigibles, Action1>").done()
      fillInTask("Floater").choose("ResourceHolder" to "Celestic").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Luna Governor ... for free. Two money production, two cards."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "LunaGovernor").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "Atmoscoop ... one titanium and 13 real. ... two floaters on Titan Shuttles. I raise the
      // temperature to minus two and get my two TR."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$Atmoscoop").done()
      fillInTask("Pay", "Titanium").chooseAlternative("Pay").chooseAmount(1).done()
      fillInTask("TemperatureStep").chooseAlternative("TemperatureStep").done()
      fillInTask("Floater").choose("ResourceHolder" to "TitanShuttles").done()
      // "I convert heat ... another TR for the ocean ... two-six for one plant and two money."
      fillInTask("UseAction").narrow("UseAction<ConvertHeatAction, Action1>").done()
      fillInTask("OceanTile").choose("WaterArea" to "Cimmeria_2_6").done()
    }
    with(blue) {
      // "Use my Celestic action to add a floater to Celestic."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Celestic, Action1>").done()
      fillInTask("Floater").choose("ResourceHolder" to "Celestic").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "I do two heat boops. Temp is at plus four now."
      fillInTask("UseAction").narrow("UseAction<ConvertHeatAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<ConvertHeatAction, Action1>").done()
    }
    green.fillInTask("StandardAction").chooseAlternative("Pass").done()
    with(blue) {
      // "Take my Red Ships action, for which I get nothing."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$RedShips, Action1>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "I'm going to do two heat boops. Another two heat? ... temp is maxed."
      fillInTask("UseAction").narrow("UseAction<ConvertHeatAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<ConvertHeatAction, Action1>").done()
    }
    blue.fillInTask("StandardAction").chooseAlternative("Pass").done()
    with(yellow) {
      // "Power Infrastructure, spend two energy, gain two money."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$PowerInfrastructure, Action1>").done()
      fillInTask("Energy").chooseAmount(2).done()
      // "Pay three for Energy Market. Use the effect of lose an energy production. Gain eight."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$EnergyMarket").done()
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$EnergyMarket, Action2>").done()
      // "Floating Habs, spend two and add ... to itself."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$FloatingHabs, Action1>").done()
      fillInTask("Floater").choose("ResourceHolder" to "FloatingHabs").done()
      // "Spend my 11 monies on Cloud Tourism, increase money production one step per set of Earth
      // and Venus tags." "Cloud Tourism, add to self."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$CloudTourism").done()
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$CloudTourism, Action1>").done()
    }

    // board-16-51-02.jpg: all players have passed in generation 7, before production.
    with(green) {
      assertProduction(m = 3, s = 1, t = 5, p = 1, e = 9, h = 0)
      assertResources(m = 2, s = 0, t = 0, p = 1, e = 2, h = 0)
      assertCounts(33 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 13, s = 0, t = 1, p = 2, e = 0, h = 1)
      assertResources(m = 10, s = 0, t = 0, p = 6, e = 0, h = 1)
      assertCounts(39 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 20, s = 1, t = 2, p = 0, e = 3, h = 8)
      assertResources(m = 2, s = 1, t = 0, p = 0, e = 0, h = 1)
      assertCounts(35 to "TerraformRating")
    }
    assertSidebar(gen = 7, temp = 8, oxygen = 6, oceans = 9, venus = 26)

    yellow.fillInTask("StandardAction").chooseAlternative("Pass").done()
    // "It is my turn to be the world government ... I'm going to eat up a Venus slot."
    green
        .fillInTask("GlobalParameter")
        .choose("GlobalParameter" to "VenusStep")
        .done()
        .expect("0 TerraformRating")
    with(green) {
      assertProduction(m = 3, s = 1, t = 5, p = 1, e = 9, h = 0)
      assertResources(m = 38, s = 1, t = 5, p = 2, e = 9, h = 2)
      assertCounts(33 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 13, s = 0, t = 1, p = 2, e = 0, h = 1)
      assertResources(m = 62, s = 0, t = 1, p = 8, e = 0, h = 2)
      assertCounts(39 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 20, s = 1, t = 2, p = 0, e = 3, h = 8)
      assertResources(m = 57, s = 2, t = 2, p = 0, e = 3, h = 9)
      assertCounts(35 to "TerraformRating")
    }
    assertSidebar(gen = 8, temp = 8, oxygen = 6, oceans = 9, venus = 28)

    // Research: "I buy one card." "I'm gonna buy two cards." "I buy one card."
    blue.fillInTask("ProjectCard").chooseAmount(3).done()
    green.fillInTask("ProjectCard").chooseAmount(2).done()
    yellow.fillInTask("ProjectCard").chooseAmount(3).done()

    with(blue) {
      // "Use my Stratopolis action to add two floaters to Jetstream Microscrappers and then my
      // Jetstream Microscrappers action to raise Venus."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Stratopolis, Action1>").done()
      fillInTask("Floater").choose("ResourceCard" to "JetStreamMicroscrappers").done()
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$JetStreamMicroscrappers, Action2>").done()
    }
    with(yellow) {
      // "Suppose I can start with a Business Network."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$BusinessNetwork, Action1>").done()
      fillInTask("ProjectCard").chooseAmount(1).done()
      // "Atalanta Planitia Lab. For a ten. I draw two cards. ... Only eight because my cutting
      // edge."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$AtalantaPlanitiaLab").done()
    }
    with(green) {
      // "I'm gonna pay 22. Damn, two awards. Industrialist and Space Baron."
      fillInTask("StandardAction").narrow("UseAction<FundAwardAction, Action1>").done()
      fillInTask("Award").choose("Award" to "Industrialist").done()
      fillInTask("StandardAction").narrow("UseAction<FundAwardAction, Action1>").done()
      fillInTask("Award").choose("Award" to "SpaceBaron").done()
    }
    with(blue) {
      // "I'm gonna play Gene Repair. So it costs 12 money and I gain two money production."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$GeneRepair").done()
      fillInTask("ProjectCard")
          .chooseAlternative("Ok")
          .done() // Decline Mars University's discard and draw.
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "I'm gonna play Cry Yourself to Sleep for 10."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$CryoSleep").done()
      // "Solar Probe: two titanium, three monies. ... I get two, but then a third for Solar, and I
      // also get three money, three heat from the thing."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$SolarProbe").done()
      fillInTask("Pay", "Titanium").chooseAlternative("Pay").chooseAmount(2).done()
    }
    with(green) {
      // "I pay three energy to fly my boat to Luna. I get 14 and [Yellow] gets two."
      expectChanges("-3 Energy, 14 MC, 2 MC<Yellow>") {
        fillInTask("StandardAction").narrow("UseAction<TradeAction, Action2>").done()
        fillInTask("Trade").choose("ColonyTile" to "Luna").done()
      }
      // "Pay three energy to fly another boat to Ganymede for five plants."
      expectChanges("-3 Energy, 5 Plant") {
        fillInTask("StandardAction").narrow("UseAction<TradeAction, Action2>").done()
        fillInTask("Trade").choose("ColonyTile" to "Ganymede").done()
      }
    }
    with(blue) {
      // "I'm gonna play Orbital Cleanup. ... I put 17. ... give myself three more money. ... my
      // titanium ... three more money."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$OrbitalCleanup").done()
      fillInTask("Pay", "Titanium").chooseAlternative("Pay").chooseAmount(1).done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "First remove an Aerial Mapper to draw another card."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$AerialMappers, Action2>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "Something's waiting for us in the bushes of love. And that cost me six money. It gives me
      // two plant production and two plants."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$Bushes").done()
      // "Then I plant forest ... three, four to the right of my city."
      fillInTask("UseAction").narrow("UseAction<ConvertPlantsAction, Action1>").done()
      fillInTask("GreeneryTile").choose("LandArea" to "Cimmeria_3_4").done()
    }
    with(blue) {
      // "For 10 money, I'm going to play Bacto Viral Research. ... discard a card from hand to draw
      // a card. ... add all six of mine to my nitrate reducing bacteria."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$BactoviralResearch").done()
      fillInTask("ProjectCard").narrow("-ProjectCard").done()
      fillInTask("Microbe").choose("ResourceHolder" to "NitriteReducingBacteria").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Pitch a steel for seven real."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$ElectroCatapult, Action2>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "Pitch a steel for five real."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$SpaceElevator, Action1>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "Now I'm going to play Titan Air Scrapping for 21 money."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$TitanAirScrapping").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Floating Refinery, spend two floaters ... gain a titanium and two monies."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$FloatingRefinery, Action2>").done()
      fillInTask("Floater").choose("ResourceHolder" to "FloatingRefinery").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "Play Quantum Communications because it costs me four and it gives me five money
      // production."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$QuantumCommunications").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "Use my nitrate reducing bacteria action to ... increase my TR."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$NitriteReducingBacteria, Action2>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Spend two energy, get two money."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$PowerInfrastructure, Action1>").done()
      fillInTask("Energy").chooseAmount(2).done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "I'm going to use Inventor's Guild. No. I'm not going to buy it."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$InventorsGuild, Action1>").done()
      fillInTask("ProjectCard").chooseAmount(1).done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "I use Floater Technology to add a floater to Local Shading."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$FloaterTechnology, Action1>").done()
      fillInTask("Floater").choose("ResourceHolder" to "LocalShading").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Add a Cloud Tourism, if you will."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$CloudTourism, Action1>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "Water Splitting Plant. That costs me eight."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$WaterSplittingPlant").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "Take my Local Shading action ... increase my money production by one step."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$LocalShading, Action2>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "For 12 plus 4, Mangrove. ... that'll be 8-4. Get two money. Get a TR."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$Mangrove").done()
      fillInTask("GreeneryTile").choose("WaterArea" to "Cimmeria_8_4").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "Use my Water Splitting Plant ... lose three energy."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$WaterSplittingPlant, Action1>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "Use my Dirigibles action to add a floater to Titan Air Scrapping."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Dirigibles, Action1>").done()
      fillInTask("Floater").choose("ResourceHolder" to "TitanAirScrapping").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Tundra Farming. Right, for 16. ... increase plant production one step, increase money
      // production two steps, and gain a plant."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$TundraFarming").done()
      // "I got Livestock. ... Decrease your plant production one step. Increase your money
      // production two steps."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$Livestock").done()
    }
    with(green) {
      // "I've spent seven on Breathing Filters and I'm putting a science resource on Olympus
      // Conference."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$BreathingFilters").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "I take my Extremophiles action and put a microbe on Nitrate Reducing Bacteria."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Extremophiles, Action1>").done()
      fillInTask("Microbe").choose("ResourceHolder" to "NitriteReducingBacteria").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // L1 Trade Terminal: "I boost the thing two things first."
      // "Because I have two of these bonuses, only pay one energy to trade. And it'll be with
      // Miranda. ... I get two animals to Livestock."
      fillInTask("StandardAction").narrow("UseAction<TradeAction, Action2>").done()
      fillInTask("Trade").choose("ColonyTile" to "Miranda").done()
      fillInTask("ColonyProduction").chooseAlternative("ColonyProduction", count = 2).done()
      fillInTask("Animal").choose("ResourceHolder" to "Livestock").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "I'm going to play a Trans-Neptune Probe ... it's free. ... remove a science resource from
      // Olympus Conference and I draw a card."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "TransNeptuneProbe").done()
      fillInTask("ProjectCard")
          .chooseAlternative(
              "ProjectCard",
          )
          .done()

      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "I'm going to take my Orbital Cleanup action and I gain six money because I have six
      // science tags."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$OrbitalCleanup, Action1>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Spend 5 for Mineral Deposit, gain 5 steel."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$MineralDeposit").done()
      // "Spend 4 steel and 1 real on Mining Rights. ... 8, 6."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$MiningRights").done()
      fillInTask("Pay", "Steel").chooseAlternative("Pay").chooseAmount(4).done()
      fillInTask("MiningRights_SpecialTile").choose("LandArea" to "Cimmeria_8_6").done()
    }
    with(green) {
      // "Use Titan Shuttles to add 2 floaters to Titan Shuttles."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$TitanShuttles, Action1>").done()
      fillInTask("Floater").choose("ResourceCard" to "TitanShuttles").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "Use my Celestic action to add a floater to Titan Air Scrapping."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Celestic, Action1>").done()
      fillInTask("Floater").choose("ResourceHolder" to "TitanAirScrapping").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Ants, pay 7, remember my Cutting Edge this time. ... immediately ants one of your
      // nitrites."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$Ants").done()
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Ants, Action1>").done()
    }
    green.fillInTask("StandardAction").chooseAlternative("Pass").done()
    with(blue) {
      // "I take my Titan Air Scrapping action. I remove two floaters and I gain a TR."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$TitanAirScrapping, Action2>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "I add a Livestock."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Livestock, Action1>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "I take my Red Spot Observatory action and I draw a card, please."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$RedSpotObservatory, Action2>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    // Blue did not remove the floater spent by the Observatory draw: the generation-end photo
    // still shows it, and every flexible floater action this generation has another destination.
    blue.exMachina("Floater<$RedSpotObservatory>")
    with(yellow) {
      // "Paid two things for Floating Habs. Add to self."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$FloatingHabs, Action1>").done()
      fillInTask("Floater").choose("ResourceHolder" to "FloatingHabs").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "I'm gonna buy some Algae. Cost me 10 money. And I gain one plant and two plant
      // productions."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$Algae").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Energy Market, lose energy production, gain eight."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$EnergyMarket, Action2>").done()
      // "Spend my thirteen on Asteroid Mining Consortium. ... decrease [Blue]'s titanium
      // production by one, increase my own."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$AsteroidMiningConsortium").done()
      fillInTask("Production").choose("Owner" to "Blue").done()
    }
    with(blue) {
      // "I'm gonna play a greenery. ... row eight, column eight. For two money and two plants."
      fillInTask("UseAction").narrow("UseAction<ConvertPlantsAction, Action1>").done()
      fillInTask("GreeneryTile").choose("LandArea" to "Cimmeria_8_8").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    yellow.fillInTask("StandardAction").chooseAlternative("Pass").done()
    with(blue) {
      // "I take my Red Ships action and I gain one money."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$RedShips, Action1>").done()
      // "I can sell these three ... patents."
      fillInTask("StandardAction").narrow("UseAction<UseStandardProjectAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<SellPatentsProject, Action1>").done()
      fillInTask("MC").chooseAmount(3).done()
      // "I have seven money and I can play my Lichen! ... one more plant production."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$Lichen").done()
    }
    // board-17-27-05.jpg: generation 8 is complete; app ledgers provide the exact pre-production
    // resource checkpoint because the photograph was captured while players were pressing Produce.
    with(green) {
      assertProduction(m = 8, s = 1, t = 5, p = 3, e = 9, h = 0)
      assertResources(m = 4, s = 0, t = 5, p = 1, e = 0, h = 2)
      assertCounts(35 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 14, s = 0, t = 0, p = 5, e = 0, h = 1)
      assertResources(m = 0, s = 0, t = 0, p = 3, e = 0, h = 2)
      assertCounts(43 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 24, s = 1, t = 4, p = 0, e = 2, h = 8)
      assertResources(m = 2, s = 2, t = 2, p = 1, e = 0, h = 12)
      assertCounts(36 to "TerraformRating")
    }
    assertSidebar(gen = 8, temp = 8, oxygen = 10, oceans = 9, venus = 30)

    blue.fillInTask("StandardAction").chooseAlternative("Pass").done()
    // "[Blue] would be the world government, but there's no choice. ... raise the oxygen."
    blue
        .fillInTask("GlobalParameter")
        .choose("GlobalParameter" to "OxygenStep")
        .done()
        .expect("0 TerraformRating")
    with(green) {
      assertResources(m = 47, s = 1, t = 10, p = 4, e = 9, h = 2)
      assertCounts(35 to "TerraformRating")
    }
    with(blue) {
      assertResources(m = 57, s = 0, t = 0, p = 8, e = 0, h = 3)
      assertCounts(43 to "TerraformRating")
    }
    with(yellow) {
      assertResources(m = 62, s = 3, t = 6, p = 1, e = 2, h = 20)
      assertCounts(36 to "TerraformRating")
    }
    assertSidebar(gen = 9, temp = 8, oxygen = 11, oceans = 9, venus = 30)
    // Research: Yellow buys one, Green buys one, and Blue buys none.
    yellow.fillInTask("ProjectCard").chooseAmount(3).done()
    green.fillInTask("ProjectCard").chooseAmount(3).done()
    blue.fillInTask("ProjectCard").chooseAmount(4).done()

    with(yellow) {
      // Yellow's applog records two 23-M€ greenery standard projects. "Oxygen to 13 ... 7-3 ...
      // and 7-4 for a titanium."
      fillInTask("StandardAction").narrow("UseAction<UseStandardProjectAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<GreeneryProject, Action1>").done()
      fillInTask("GreeneryTile").choose("LandArea" to "Cimmeria_7_3").done()
      fillInTask("StandardAction").narrow("UseAction<UseStandardProjectAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<GreeneryProject, Action1>").done()
      fillInTask("GreeneryTile").choose("LandArea" to "Cimmeria_7_4").done()
    }
    with(green) {
      // "Use Water Splitting Plant to use up three energy and take the last oxygen ... I give
      // myself a TR."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$WaterSplittingPlant, Action1>").done()
      // "Build the Immigrant City ... cost me nine ... 9-8 for two money and a plant."
      // Green keeps his steel for Space Elevator, where it is worth five M€ instead of two.
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$ImmigrantCity").done()
      fillInTask("Pay", "Steel").chooseAlternative("Ok").done() // No steel paid.
      fillInTask("CityTile")
          .choose("CityTile" to "NormalCityTile")
          .choose("LandArea" to "Cimmeria_9_8")
          .done()
    }
    with(blue) {
      // "Take the city standard project ... this is 8-5."
      fillInTask("StandardAction").narrow("UseAction<UseStandardProjectAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<CityProject, Action1>").done()
      fillInTask("CityTile")
          .choose("CityTile" to "NormalCityTile")
          .choose("LandArea" to "Cimmeria_8_5")
          .done()
      assertCounts(36 to "MC")
      // "Place a city for 25 more money ... 6-8." [sic]
      fillInTask("StandardAction").narrow("UseAction<UseStandardProjectAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<CityProject, Action1>").done()
      fillInTask("CityTile")
          .choose("CityTile" to "NormalCityTile")
          .choose("LandArea" to "Cimmeria_7_8")
          .done()
    }
    with(yellow) {
      // "Play Kaguya Tech for 10 ... 7-3 ... flipping the tile ... becoming a city."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$KaguyaTech").done()
      fillInTask("CityTile")
          .choose("CityTile" to "NormalCityTile")
          .choose("MarsArea" to "Cimmeria_7_3")
          .done()
      // "Energy Market an energy production into eight monies."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$EnergyMarket, Action2>").done()
    }
    // Green forgot to take his Immigrant City effect.
    green.exMachina("PROD[-MC]")
    with(green) {
      // "Play Open City. It cost me 19 ... go to 5-3."
      // Green continues to keep the same steel for his next Space Elevator action.
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$OpenCity").done()
      fillInTask("Pay", "Steel").chooseAlternative("Ok").done() // No steel paid.
      fillInTask("CityTile")
          .choose("CityTile" to "NormalCityTile")
          .choose("LandArea" to "Cimmeria_5_3")
          .done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }

    with(blue) {
      // "I'm going to plant forest ... at 7-7 ... that gives you two plants."
      fillInTask("UseAction").narrow("UseAction<ConvertPlantsAction, Action1>").done()
      fillInTask("GreeneryTile").choose("LandArea" to "Cimmeria_7_7").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Never did a Business Network. ... draw a card? No."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$BusinessNetwork, Action1>").done()
      fillInTask("ProjectCard").chooseAmount(1).done()
      // "Topsoil Contract for eight. ... no, no, no. Six. Solar Logistics."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$TopsoilContract").done()
    }
    with(green) {
      // "Use my Space Elevator to consume a steel and take five real."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$SpaceElevator, Action1>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "Take my Nitrate Reducing Bacteria action. Remove three microbes, increase my TR."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$NitriteReducingBacteria, Action2>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Spend an energy to trade with Miranda ... get two animals ... Livestock."
      fillInTask("StandardAction").narrow("UseAction<TradeAction, Action2>").done()
      fillInTask("Trade").choose("ColonyTile" to "Miranda").done()
      fillInTask("ColonyProduction").chooseAlternative("ColonyProduction", count = 2).done()
      fillInTask("Animal").choose("ResourceHolder" to "Livestock").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "Spend three energy to trade with Luna ... get 14 and yellow gets two."
      expectChanges("-3 Energy, 14 MC, 2 MC<Yellow>") {
        fillInTask("StandardAction").narrow("UseAction<TradeAction, Action2>").done()
        fillInTask("Trade").choose("ColonyTile" to "Luna").done()
      }
      // "Spend three energy to fly to Ganymede and take one plant."
      expectChanges("-3 Energy, Plant") {
        fillInTask("StandardAction").narrow("UseAction<TradeAction, Action2>").done()
        fillInTask("Trade").choose("ColonyTile" to "Ganymede").done()
      }
    }
    with(blue) {
      // "Take my Red Spot Observatory action ... draw a card."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$RedSpotObservatory, Action2>").done()
      // "Sell this patent for one money."
      fillInTask("StandardAction").narrow("UseAction<UseStandardProjectAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<SellPatentsProject, Action1>").done()
      fillInTask("MC").chooseAmount(1).done()
    }
    with(yellow) {
      // "Take your final nitrite ... and my thing gives me a money."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Ants, Action1>").done()

      fillInTask("Microbe")
          .choose("Owner" to "Blue")
          .choose("ResourceHolder" to "NitriteReducingBacteria")
          .done()

      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "Now I can plant forest ... six, four to get one titanium and two steel."
      fillInTask("UseAction").narrow("UseAction<ConvertPlantsAction, Action1>").done()
      fillInTask("GreeneryTile").choose("LandArea" to "Cimmeria_6_4").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "Take my Stratopolis action to add two floaters to Titan Air Scrapping."
      // Resolve the action on its own eligible card before reproducing Blue's illegal
      // destination.
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Stratopolis, Action1>").done()
      fillInTask("Floater").choose("ResourceCard" to "Stratopolis").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    blue.exMachina("-2 Floater<$Stratopolis>, 2 Floater<$TitanAirScrapping>")
    with(yellow) {
      // "Power Infrastructure, spend an energy, gain a money."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$PowerInfrastructure, Action1>").done()
      fillInTask("Energy").chooseAmount(1).done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "Use Inventors' Guild ... two cards, out of cards."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$InventorsGuild, Action1>").done()
      fillInTask("ProjectCard").chooseAmount(1).done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "Titan Air Scrapping ... move two floaters and increase my TR."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$TitanAirScrapping, Action2>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "I'm gonna Livestock."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Livestock, Action1>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // The green tableau and the turn order identify Green as the Interstellar Colony Ship player.
      // "Cost me 18, which I'll spend at six titanium." Solar Logistics gives Yellow the card.
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$InterstellarColonyShip").done()
      fillInTask("Pay", "Titanium").chooseAlternative("Pay").chooseAmount(6).done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    // Green forgets the Palladin effect for paying a space event
    green.exMachina("-Titanium")
    with(blue) {
      // "Orbital Cleanup. I gain six monies."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$OrbitalCleanup, Action1>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // The clipped "Cloud..." response between Blue's cleanup and Green's turn is Yellow's
      // otherwise-unused Cloud Tourism action, corroborated by its final-tableau floater.
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$CloudTourism, Action1>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "Use Titan Shuttles to take six titanium."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$TitanShuttles, Action2>").done()
      fillInTask("Floater").chooseAmount(6).done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "I'm gonna fund Benefactor ... cost me 20."
      fillInTask("StandardAction").narrow("UseAction<FundAwardAction, Action1>").done()
      fillInTask("Award").choose("Award" to "Benefactor").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Add a floater to Floating Refinery."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$FloatingRefinery, Action1>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "I might as well play it ... spend two steel and ... only spent 13. ... draw two cards."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$AiCentral").done()
      fillInTask("Pay", "Steel").chooseAlternative("Pay").chooseAmount(2).done()
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$AiCentral, Action1>").done()
    }
    with(blue) {
      // "Use my Dirigibles action to add a floater to Celestic."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Dirigibles, Action1>").done()
      fillInTask("Floater").choose("ResourceHolder" to "Celestic").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "I'm going to use aerial mappers to add to floating heads."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$AerialMappers, Action1>").done()
      fillInTask("Floater").choose("ResourceHolder" to "FloatingHabs").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "Plant forest ... here for a card and two money." The final board identifies 8-7.
      fillInTask("StandardAction").narrow("UseAction<UseStandardProjectAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<GreeneryProject, Action1>").done()
      fillInTask("GreeneryTile").choose("LandArea" to "Cimmeria_8_7").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "Use my Floater Technology action to add a floater to Celestic."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$FloaterTechnology, Action1>").done()
      fillInTask("Floater").choose("ResourceHolder" to "Celestic").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "I sell a patent."
      fillInTask("StandardAction").narrow("UseAction<UseStandardProjectAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<SellPatentsProject, Action1>").done()
      fillInTask("MC").chooseAmount(1).done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(green) {
      // "I'm going to sell 5 patentos."
      fillInTask("StandardAction").narrow("UseAction<UseStandardProjectAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<SellPatentsProject, Action1>").done()
      fillInTask("MC").chooseAmount(5).done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "Use my Celestic action to add a floater to Celestic."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Celestic, Action1>").done()
      fillInTask("Floater").choose("ResourceHolder" to "Celestic").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Plant a forest ... seven, six ... a plant and two steel."
      fillInTask("UseAction").narrow("UseAction<ConvertPlantsAction, Action1>").done()
      fillInTask("GreeneryTile").choose("LandArea" to "Cimmeria_7_6").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    green.fillInTask("StandardAction").chooseAlternative("Pass").done()
    with(blue) {
      // "Take my Red Ships action ... one, two, three, four."
      expectChanges("4 MC") {
        fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
        fillInTask("UseAction").narrow("UseAction<$RedShips, Action1>").done()
      }
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Spend a steel to gain ... seven real."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$ElectroCatapult, Action2>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "Take my Local Shading action to add a floater to Local Shading."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$LocalShading, Action1>").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Pay seven for Robot Pollinators ... three plants."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$RobotPollinators").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(blue) {
      // "Take my Extremophiles action and add a microbe to Extremophiles."
      fillInTask("StandardAction").narrow("UseAction<UseActionOnCardAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<$Extremophiles, Action1>").done()
      fillInTask("Microbe").choose("ResourceHolder" to "Extremophiles").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    with(yellow) {
      // "Pay seven for Insects ... increase plant production ... for each plant tag ... three."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$Insects").done()
      fillInTask("StandardAction").chooseAlternative("PassBaton").done()
    }
    blue.fillInTask("StandardAction").chooseAlternative("Pass").done()
    with(yellow) {
      // "Stanford Torus ... four titanium ... reserved area."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$StanfordTorus").done()
      fillInTask("Pay", "Titanium").chooseAlternative("Pay").chooseAmount(4).done()
      // "Sell one, two, three, four, five patents."
      fillInTask("StandardAction").narrow("UseAction<UseStandardProjectAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<SellPatentsProject, Action1>").done()
      fillInTask("MC").chooseAmount(5).done()
      // "Carbon Nanosystems ... four steel ... and then six real."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$CarbonNanosystems").done()
      fillInTask("Pay", "Steel").chooseAlternative("Pay").chooseAmount(4).done()
      // "Vesta's Shipyard ... three titanium ... Carbon Nano is worth four ... my two real."
      fillInTask("StandardAction").narrow("UseAction<PlayCardAction, Action1>").done()
      fillInTask("PlayCard").choose("CardFront" to "$VestaShipyard").done()
      fillInTask("PayFromCard")
          .narrow("PayFromCard<$CarbonNanosystems> FROM Graphene<$CarbonNanosystems>")
          .done()
      fillInTask("Pay", "Titanium").chooseAlternative("Pay").chooseAmount(3).done()
      // Yellow's app ledger records one final M€ after Vesta and before Pass.
      fillInTask("StandardAction").narrow("UseAction<UseStandardProjectAction, Action1>").done()
      fillInTask("UseAction").narrow("UseAction<SellPatentsProject, Action1>").done()
      fillInTask("MC").chooseAmount(1).done()
    }
    // Floating Habs being unused makes sense since it has an even number of floaters (eight).
    yellow.fillInTask("StandardAction").chooseAlternative("Pass").done()

    // All three applogs after the final production phase and before final greenery placement.
    with(green) {
      assertProduction(m = 15, s = 1, t = 5, p = 3, e = 6, h = 0)
      assertResources(m = 59, s = 1, t = 16, p = 3, e = 6, h = 2)
      assertCounts(36 to "TerraformRating", 0 to "$InventionContest")
    }
    with(blue) {
      assertProduction(m = 16, s = 0, t = 0, p = 5, e = 0, h = 1)
      assertResources(m = 67, s = 0, t = 0, p = 9, e = 0, h = 4)
      assertCounts(45 to "TerraformRating", 0 to "$CryoSleep")
    }
    with(yellow) {
      assertProduction(m = 26, s = 1, t = 5, p = 4, e = 1, h = 8)
      assertResources(m = 65, s = 1, t = 5, p = 8, e = 1, h = 28)
      assertCounts(
          38 to "TerraformRating",
          1 to "$CryoSleep",
          1 to "$AerialMappers",
          1 to "$Insects",
      )
    }
    assertSidebar(gen = 9, temp = 8, oxygen = 14, oceans = 9, venus = 30)

    // Final greenery placement, in start-player order.
    yellow.fillInTask("UseAction").narrow("UseAction<ConvertPlantsAction, Action1>").done()
    yellow.fillInTask("GreeneryTile").choose("LandArea" to "Cimmeria_6_3").done()
    yellow.fillInTask("ConvertPlantsAction").chooseAlternative("PassBaton").done()
    green.fillInTask("ConvertPlantsAction").chooseAlternative("PassBaton").done()
    blue.fillInTask("UseAction").narrow("UseAction<ConvertPlantsAction, Action1>").done()
    blue.fillInTask("GreeneryTile").choose("LandArea" to "Cimmeria_6_8").done()
    blue.fillInTask("ConvertPlantsAction").chooseAlternative("PassBaton").done()

    green.assertCounts(
        0 to "ProjectCard",
        1 to "Science<$OlympusConference>",
        1 to "CardResource", // and that's it
    )
    blue.assertCounts(
        0 to "ProjectCard",
        6 to "Floater<$Celestic>",
        1 to "Floater<$LocalShading>",
        0 to "Floater<$JetStreamMicroscrappers>",
        0 to "Floater<$Dirigibles>",
        0 to "Floater<$Stratopolis>",
        0 to "Floater<$RedSpotObservatory>",
        0 to "Microbe<$NitriteReducingBacteria>",
        1 to "Microbe<$Extremophiles>",
        8 to "CardResource", // and that's it
    )
    yellow.assertCounts(
        0 to "ProjectCard",
        8 to "Floater<$FloatingHabs>",
        0 to "Floater<$AerialMappers>",
        1 to "Floater<$FloatingRefinery>",
        3 to "Floater<$CloudTourism>",
        2 to "Microbe<$Ants>",
        6 to "Animal<$Livestock>",
        0 to "Graphene<$CarbonNanosystems>",
        20 to "CardResource", // and that's it
    )

    val score = Summarizer(game)
    assertEquals(5, score.net("FirstPlace", "VictoryPoint<Blue>"))
    assertEquals(4, score.net("GreeneryTile", "VictoryPoint<Blue>"))
    assertEquals(5, score.net("CityTile", "VictoryPoint<Blue>"))
    assertEquals(13, score.net("Card", "VictoryPoint<Blue>"))
    green.assertCounts(88 to "VictoryPoint", 0 to "Victory")
    blue.assertCounts(72 to "VictoryPoint", 0 to "Victory")
    yellow.assertCounts(94 to "VictoryPoint", 1 to "Victory")

    assertEquals(
        """
        |                      1     2     3     4     5     6     7     8     9
        |                     /     /     /     /     /     /     /     /     /
        |
        | 1 -             [O]    LP    VS    LP   [O]
        |
        | 2 -          [O]   [G1]   L     L    LPS   [O]
        |
        | 3 -        L    [G1]  [C1]  [G1]   L     LP    L
        |
        | 4 -     VS    L    [S1]   L    LSS    L    VTT    LC
        |
        | 5 -  L     L    [C1]   LS    LS    LC    L    LSC    W
        |
        | 6 -    [C3]  [G3]  [G1]   L     LT   LSS   [G2]  LSS
        |
        | 7 -       [C3]  [G3]   L    [G3]  [G2]  [C2]  [O]
        |
        | 8 -          [G3]  [C2]  [S3]  [G1]  [G2]  [O]
        |
        | 9 -             [O]   [O]   [O]   [C1]  [G2]
        """
            .trimMargin(),
        TfmMapRenderer(game.reader, game.actors.filterIsInstance<Player>(), useAnsiColors = false)
            .render()
            .joinToString("\n"),
    )
  }
}
