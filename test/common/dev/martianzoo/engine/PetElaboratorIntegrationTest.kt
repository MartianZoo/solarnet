package dev.martianzoo.engine

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.Vocabulary
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.api.TypeInfo.NoGameState
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.types.inferTypeVariables
import dev.martianzoo.tfm.canon.Canon
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PetElaboratorIntegrationTest {
  private companion object {
    val catalog = Canon.withPlayers(3)
    val table = catalog.classTable
    val elaborator = PetElaborator(table)
    val vocabulary = Vocabulary.create(catalog, activeClassNames = table.allClassNames)
  }

  @Test
  internal fun `input elaboration preserves default edge cases through its public operation`() {
    checkInput("Heat", "Heat<Owner>!")
    checkInput("-5 Heat", "-5 Heat<Owner>!")
    checkInput("VictoryPoint", "VictoryPoint<Owner>!")
    checkInput("OceanTile<>", "OceanTile<WaterArea(HAS MAX 0 Tile)>.")
    checkInput("MoholeArea_SpecialTile", "MoholeArea_SpecialTile<Owner>!")
    checkInput("-OceanTile", "-OceanTile.")
    checkInput(
        "CityTile<>",
        "CityTile<LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>), Owner>!",
    )
    checkInput("-CityTile", "-CityTile<Owner>!")
    checkInput("CityTile<WaterArea>", "CityTile<WaterArea, Owner>!")
    checkInput("CityTile<Owner, WaterArea>", "CityTile<WaterArea, Owner>!")
    checkInput("CityTile<Anyone, WaterArea>", "CityTile<WaterArea, Anyone>!")
    checkInput("CityTile<Player3, WaterArea>", "CityTile<WaterArea, Player3>!")
    checkInput("OwnedTile", "OwnedTile<Owner>!")
    checkInput("Neighbor<OwnedTile>", "Neighbor<OwnedTile<Owner>>!")
    checkInput(
        "LandArea(HAS Neighbor<OwnedTile>)",
        "LandArea(HAS Neighbor<OwnedTile<Owner>>)!",
    )
    checkInput("MC<Anyone(HAS VenusTag)>", "MC<Anyone(HAS VenusTag)>!")
    checkInput(
        "MC<Anyone(HAS VenusTag<>)>",
        "MC<Anyone(HAS VenusTag<Owner>)>!",
    )
    checkInput("Heat FROM Owed<>!", "Heat<Owner> FROM Owed<Owner, Class<MC>>!")
    checkInput("Heat FROM Owed<>.", "Heat<Owner> FROM Owed<Owner, Class<MC>>.")
    checkInput("Heat FROM Owed<>", "Heat<Owner> FROM Owed<Owner, Class<MC>>!")
    checkInput("Owed<>", "Owed<Owner, Class<MC>>!")
    checkInput("-Owed", "-Owed<Owner>.")
    checkInput("-Owed<>", "-Owed<Owner, Class<MC>>.")
  }

  @Test
  internal fun `input elaboration enforces explicit dependency-default acceptance`() {
    shouldThrow<PetSyntaxException> { elaborateInput("OceanTile") }.message shouldBe
        "`OceanTile` has gain dependency defaults; write `OceanTile<>` to accept them or provide dependency arguments"
    shouldThrow<PetSyntaxException> { elaborateInput("Owed") }.message shouldBe
        "`Owed` has gain dependency defaults; write `Owed<>` to accept them or provide dependency arguments"
    shouldThrow<PetSyntaxException> { elaborateInput("Plant<>") }.message shouldBe
        "`Plant<>` has no gain dependency defaults to accept"
    shouldThrow<PetSyntaxException> { elaborateInput("-Plant<>") }.message shouldBe
        "`Plant<>` has no removal dependency defaults to accept"
    shouldThrow<PetSyntaxException> {
          elaborator.elaborateInput(parse<Expression>("Player<>"), vocabulary)
        }
        .message shouldBe "`Player<>` has no all-use dependency defaults to accept"
  }

  @Test
  internal fun `input elaboration permits instruction cardinality changes`() {
    elaborateInput("2 OxygenStep!") shouldBe "OxygenStep!, OxygenStep!"
  }

  private fun checkInput(source: String, expected: String) {
    elaborateInput(source) shouldBe expected
  }

  private fun elaborateInput(source: String): String =
      elaborator.elaborateInput(parse<InstructionTree>(source), vocabulary).toString()

  @Test
  internal fun `an action variable survives lowering and binds from its first stage`() {
    val component = Component(table.resolve(parse("UtopiaInvest<Player1>")))
    val effect =
        LiveEffect.compile(component, elaborator).single {
          "4 StandardResource" in it.effect.instruction.toString()
        }
    val then = effect.effect.instruction as Then
    val structuralInfo =
        object : TypeInfo {
          override fun isAbstract(e: Expression): Boolean = table.resolve(e).abstract

          override fun ensureNarrows(wide: Expression, narrow: Expression) {
            table.resolve(narrow).ensureNarrows(table.resolve(wide), NoGameState)
          }

          override fun has(requirement: dev.martianzoo.pets.ast.Requirement): Boolean =
              error("No refinement is expected")
        }

    then
        .bindFirstStage(
            parse("-Production<Player1, Class<Plant>>!"),
            structuralInfo,
        )
        .toString() shouldBe "-Production<Player1, Class<Plant>>! THEN 4 Plant<Player1>!"
  }

  @Test
  internal fun `a card-payment offer keeps its resource-card linkage`() {
    val component =
        Component(table.resolve(parse("AcceptingFromCard<Player1, KuiperCooperative<Player1>>")))

    LiveEffect.compile(component, elaborator)
        .map { it.effect.toString() }
        .single { "PayFromCard" in it } shouldBe
        "Billing<Player1>: X PayFromCard<Player1, KuiperCooperative<Player1>> " +
            "FROM Asteroid<KuiperCooperative<Player1>>?"
  }

  @Test
  internal fun `variable specialization leaves an ordinary occurrence of the same class independent`() {
    val general = table.resolve(parse<Expression>("MicrobeTag<Player1, CardFront<Player1>>"))
    val specific = table.resolve(parse<Expression>("MicrobeTag<Player1, Decomposers<Player1>>"))
    val effect =
        table
            .inferTypeVariables()
            .transformEffect(
                parse(
                    "MicrobeTag<Player1, CardFront<Player1>>: " +
                        "Microbe<CardFront<Player1>> OR Microbe<CardFront<Player2>>"
                )
            )

    elaborator
        .specializeVariables(
            general,
            specific,
            parse("MicrobeTag<Player1, CardFront<Player1>>"),
            effect.typeVariables,
        )
        .transformInstructionTree(effect.instruction)
        .toString() shouldBe "Microbe<Decomposers<Player1>> OR Microbe<CardFront<Player2>>"
  }

  @Test
  internal fun `Class-scoped variables retain dependency constraints supplied by each use`() {
    val playCard = table.getClass(parse<Expression>("PlayCard").className)
    val effect =
        elaborator.classEffects(playCard).single { "CardInvoice" in it.instruction.toString() }
    val cardFront =
        effect.typeVariables.variables.single {
          it.declaration.expression.toString() == "CardFront"
        }

    effect.typeVariables.expressionsOf(cardFront).map(Any::toString).toSet() shouldBe
        setOf("CardFront<Owner>")
    val cardLocation =
        effect.typeVariables.variables.single {
          it.declaration.expression.toString() == "CardLocation"
        }
    effect.typeVariables.expressionsOf(cardLocation).map(Any::toString).toSet() shouldBe
        setOf("CardLocation")

    val component =
        Component(
            table.resolve(parse("PlayCard<Player1, Class<ProjectCard>, Class<AiCentral>, Hand>"))
        )
    LiveEffect.compile(component, elaborator)
        .map(LiveEffect::effect)
        .single {
          "CardInvoice" in it.instruction.toString()
        }
        .instruction
        .toString() shouldBe
        "Owed<Player1, Class<MC>>! / AiCentral<Player1>.cost THEN " +
            "HandleCardTags<Player1, Class<AiCentral>>! " +
            "THEN CardInvoice<Player1, Class<AiCentral>>! THEN MAX 0 Barrier: " +
            "AiCentral<Player1> FROM ProjectCard<Player1, Hand>!"
  }

  @Test
  internal fun `represented Class capture specializes every SoloStandardResourceReserve effect`() {
    val klass = table.getClass(parse<Expression>("SoloStandardResourceReserve").className)
    val component = Component(table.resolve(parse("SoloStandardResourceReserve<Class<MC>>")))
    val resource =
        klass.typeVariables.single {
          it.declaration.expression.toString() == "StandardResource"
        }

    component.type
        .variableBindingsFrom(klass.defaultType, listOf(resource))[resource]
        .toString() shouldBe "MC"

    val productionEffect =
        elaborator.classEffects(klass).single {
          it.instruction.toString().startsWith("42 Production")
        }
    productionEffect.typeVariables.variables.associate { variable ->
      variable.declaration.expression.toString() to
          productionEffect.typeVariables.expressionsOf(variable).map(Any::toString).toSet()
    } shouldBe mapOf("StandardResource" to setOf("StandardResource<Owner>"))

    LiveEffect.compile(component, elaborator).map { it.effect.toString() }.toSet() shouldBe
        setOf(
            "This BY Actor(NOT Admin): Die!",
            "SetupPhase: 42 MC<SoloOpponent>!",
            "SetupPhase: 42 Production<SoloOpponent, Class<MC>>!",
            "-MC<SoloOpponent> BY Player:: MC<SoloOpponent>! BY Admin",
            "MC<SoloOpponent> BY Player:: -MC<SoloOpponent>! BY Admin",
            "-Production<SoloOpponent, Class<MC>> BY Player:: " +
                "Production<SoloOpponent, Class<MC>>! BY Admin",
            "Production<SoloOpponent, Class<MC>> BY Player:: " +
                "-Production<SoloOpponent, Class<MC>>! BY Admin",
        )
  }

  @Test
  internal fun `trigger variable survives Production lowering`() {
    val klass = table.getClass(parse<Expression>("Manutech").className)
    val effect = elaborator.classEffects(klass).single { "Production" in it.trigger.toString() }

    effect.typeVariables.variables.associate { variable ->
      variable.declaration.expression.toString() to
          effect.typeVariables.expressionsOf(variable).map(Any::toString).toSet()
    } shouldBe mapOf("StandardResource" to setOf("StandardResource<Owner>"))
    effect.trigger.toString() shouldBe "Production<Owner, Class<StandardResource>>"
    val trigger = (effect.trigger as Effect.Trigger.OnGainOf).expression
    elaborator
        .specializeVariables(
            table.resolve(trigger),
            table.resolve(parse("Production<Player1, Class<Plant>>")),
            trigger,
            effect.typeVariables,
        )
        .transformInstructionTree(effect.instruction)
        .toString() shouldBe "Plant<Owner>!"
  }
}
