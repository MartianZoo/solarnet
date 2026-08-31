package dev.martianzoo.engine

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.KindException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.api.TypeInfo.NoGameState
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.types.inferTypeVariables
import dev.martianzoo.tfm.canon.Canon
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TransformersTest {
  @Test
  internal fun atomizerChangesKindOnlyThroughTheBroaderInstructionTreeKind() {
    val instruction = parse<Instruction>("2 OxygenStep!")
    val atomizer = transformers.atomizer()
    val transformed = atomizer.transformInstructionTree(instruction)

    transformed shouldBe parse<InstructionTree>("OxygenStep!, OxygenStep!")
    shouldThrow<KindException> { atomizer.transformInstruction(instruction) }
  }

  @Test
  internal fun test() {
    checkApplyDefaults("Heat", "Heat<Owner>!")
    checkApplyDefaults("-5 Heat", "-5 Heat<Owner>!")
    checkApplyDefaults("VictoryPoint", "VictoryPoint<Owner>!")
    checkApplyDefaults("OceanTile<>", "OceanTile<WaterArea>.")
    checkApplyDefaults("MoholeArea_SpecialTile<>", "MoholeArea_SpecialTile<Owner>!")
    checkApplyDefaults("-OceanTile", "-OceanTile.")
    checkApplyDefaults(
        "CityTile<>",
        "CityTile<LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>), Owner>!",
    )
    checkApplyDefaults("-CityTile", "-CityTile<Owner>!")
    checkApplyDefaults("CityTile<WaterArea>", "CityTile<WaterArea, Owner>!")
    checkApplyDefaults("CityTile<Owner, WaterArea>", "CityTile<WaterArea, Owner>!")
    checkApplyDefaults("CityTile<Anyone, WaterArea>", "CityTile<WaterArea, Anyone>!")
    checkApplyDefaults("CityTile<Player3, WaterArea>", "CityTile<WaterArea, Player3>!")

    checkApplyDefaults("CityTile<This>", "CityTile<This, Owner>!", cn("Area").expression)
    checkApplyDefaults(
        "CityTile<This>",
        "CityTile<LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>), This>!",
        cn("Owner").expression,
    )

    checkApplyDefaults("OwnedTile", "OwnedTile<Owner>!")
    checkApplyDefaults("Neighbor<OwnedTile>", "Neighbor<OwnedTile<Owner>>!")
    checkApplyDefaults(
        "LandArea(HAS Neighbor<OwnedTile>)",
        "LandArea(HAS Neighbor<OwnedTile<Owner>>)!",
    )
    checkApplyDefaults(
        "MC<Anyone(HAS VenusTag)>",
        "MC<Anyone(HAS VenusTag)>!",
    )
    checkApplyDefaults(
        "MC<Anyone(HAS VenusTag<>)>",
        "MC<Anyone(HAS VenusTag<Owner>)>!",
    )
    checkApplyDefaults(
        "GreeneryTile<>",
        "GreeneryTile<LandArea(HAS? Neighbor<OwnedTile<Owner>>, MAX 0 Tile), Owner>!",
    )

    checkApplyDefaults(
        "Heat FROM Owed<>!",
        "Heat<Owner> FROM Owed<Owner, Class<MC>>!",
    )
    checkApplyDefaults(
        "Heat FROM Owed<>.",
        "Heat<Owner> FROM Owed<Owner, Class<MC>>.",
    )
    checkApplyDefaults(
        "Heat FROM Owed<>",
        "Heat<Owner> FROM Owed<Owner, Class<MC>>!",
    )
    checkApplyDefaults("Owed<>", "Owed<Owner, Class<MC>>!")
    checkApplyDefaults("-Owed", "-Owed<Owner>.")
    checkApplyDefaults("-Owed<>", "-Owed<Owner, Class<MC>>.")
  }

  @Test
  internal fun dependencyDefaultsMustBeAcceptedOrPartiallySpecified() {
    shouldThrow<PetSyntaxException> { applyDefaults("OceanTile") }.message shouldBe
        "`OceanTile` has gain dependency defaults; write `OceanTile<>` to accept them or provide dependency arguments"
    shouldThrow<PetSyntaxException> { applyDefaults("Owed") }.message shouldBe
        "`Owed` has gain dependency defaults; write `Owed<>` to accept them or provide dependency arguments"
    checkApplyDefaults("-Owed", "-Owed<Owner>.")
    checkApplyDefaults("CityTile<WaterArea>", "CityTile<WaterArea, Owner>!")
    checkApplyDefaults("-OceanTile", "-OceanTile.")
  }

  @Test
  internal fun triggerDependencyDefaultsMustBeAcceptedOrPartiallySpecified() {
    applyEffectDefaults("ScienceTag<>: Heat") shouldBe
        applyEffectDefaults("ScienceTag<CardFront>: Heat")
    applyEffectDefaults("-ScienceTag<>: Heat") shouldBe
        applyEffectDefaults("-ScienceTag<CardFront>: Heat")

    shouldThrow<PetSyntaxException> { applyEffectDefaults("ScienceTag: Heat") }.message shouldBe
        "`ScienceTag` has trigger dependency defaults; write `ScienceTag<>` to accept them or provide dependency arguments"
  }

  private companion object {
    val transformers = Transformers(Canon.classTable)
  }

  @Test
  internal fun `an action variable survives lowering and binds from its first stage`() {
    val component = Component(Canon.classTable.resolve(parse("UtopiaInvest<Player1>")))
    val effect =
        LiveEffect.compile(component, transformers).single {
          "4 StandardResource" in it.effect.instruction.toString()
        }
    val then = effect.effect.instruction as Then
    val structuralInfo =
        object : TypeInfo {
          override fun isAbstract(e: Expression): Boolean = Canon.classTable.resolve(e).abstract

          override fun ensureNarrows(wide: Expression, narrow: Expression) {
            Canon.classTable.resolve(narrow).ensureNarrows(Canon.classTable.resolve(wide), NoGameState)
          }

          override fun has(requirement: dev.martianzoo.pets.ast.Requirement): Boolean =
              error("No refinement is expected")
        }

    then.bindFirstStage(
            parse("-Production<Player1, Class<Plant>>!"),
            structuralInfo,
        )
        .toString() shouldBe
        "-Production<Player1, Class<Plant>>! THEN 4 Plant<Player1>!"
  }

  @Test
  internal fun `a card-payment offer keeps its resource-card linkage`() {
    val component =
        Component(
            Canon.classTable.resolve(
                parse("AcceptFromCard<Player1, KuiperCooperative<Player1>>")
            )
        )

    LiveEffect.compile(component, transformers)
        .map { it.effect.toString() }
        .single { "PayFromCard" in it } shouldBe
        "Billing<Player1>: X PayFromCard<Player1, KuiperCooperative<Player1>> " +
            "FROM Asteroid<KuiperCooperative<Player1>>?"
  }

  private fun checkApplyDefaults(
      original: String,
      expected: String,
      context: Expression = THIS.expression,
  ) {
    applyDefaults(original, context).toString() shouldBe expected
  }

  private fun applyDefaults(
      original: String,
      context: Expression = THIS.expression,
  ): Instruction = transformers.insertDefaults(context).transformInstruction(parse(original))

  private fun applyEffectDefaults(original: String): Effect =
      transformers.insertDefaults().transformEffect(parse(original))

  @Test
  internal fun testDeprodify_noProd() {
    val s = "Foo<Bar>: Bax OR Qux"
    val e: Effect = parse(s)
    val ep: Effect = transformers.transformMarkedSyntax().transformEffect(e)
    ep.toString() shouldBe s
  }

  @Test
  internal fun testDeprodify_simple() {
    val prodden: Effect = parse("This: PROD[Plant / PlantTag]")
    val deprodden: Effect = transformers.transformMarkedSyntax().transformEffect(prodden)
    deprodden.toString() shouldBe "This: Production<Class<Plant>> / PlantTag"
  }

  @Test
  internal fun configuredDispatcherAlsoLowersCardSyntax() {
    val source: Instruction = parse("CARDS[2 ProjectCard(HAS VenusTag)]")

    transformers.transformMarkedSyntax().transformInstruction(source).toString() shouldBe
        "2 ProjectCard"
  }

  @Test
  internal fun deprodifyPreservesAResourceRefinementOnItsClassDependency() {
    val prodden: Instruction = parse("PROD[StandardResource(HAS LowestProduction)]")

    transformers.transformMarkedSyntax().transformInstruction(prodden).toString() shouldBe
        "Production<Class<StandardResource>(HAS LowestProduction)>"
  }

  @Test
  internal fun testDeprodify_lessSimple() {
    val prodden: Effect =
        parse(
            "PROD[Plant]: PROD[Ooh?, Steel. / Ahh, Foo<Xyz> FROM " +
                "Foo<Heat>, -Qux!, 5 Ahh<Qux> FROM StandardResource], Heat"
        )
    val expected: Effect =
        parse(
            "Production<Class<Plant>>:" +
                " Ooh?, Production<Class<Steel>>. / Ahh, Foo<Xyz> FROM Foo<Production<Class<Heat>>>," +
                " -Qux!, 5 Ahh<Qux> FROM Production<Class<StandardResource>>, Heat"
        )
    val deprodden: Effect = transformers.transformMarkedSyntax().transformEffect(prodden)
    deprodden shouldBe expected
  }

  @Test
  internal fun `variable specialization leaves an ordinary occurrence of the same class independent`() {
    val general =
        Canon.classTable.resolve(parse<Expression>("MicrobeTag<Player1, CardFront<Player1>>"))
    val specific =
        Canon.classTable.resolve(parse<Expression>("MicrobeTag<Player1, Decomposers<Player1>>"))
    val effect =
        Canon.classTable
            .inferTypeVariables()
            .transformEffect(
                parse(
                    "MicrobeTag<Player1, CardFront<Player1>>: " +
                        "Microbe<CardFront<Player1>> OR Microbe<CardFront<Player2>>"
                )
            )

    transformers
        .bindVariablesFrom(
            general,
            specific,
            parse("MicrobeTag<Player1, CardFront<Player1>>"),
            effect.typeVariables,
        )
        .transformInstructionTree(effect.instruction)
        .toString() shouldBe "Microbe<Decomposers<Player1>> OR Microbe<CardFront<Player2>>"
  }

  @Test
  internal fun `Class-token variables retain dependency constraints supplied by each use`() {
    val playCard = Canon.classTable.getClass(parse<Expression>("PlayCard").className)
    val effect =
        transformers.classEffects(playCard).single { "CardInvoice" in it.instruction.toString() }
    val cardFront =
        effect.typeVariables.variables.single {
          it.declaration.expression.toString() == "CardFront"
        }

    effect.typeVariables.expressionsOf(cardFront).map(Any::toString).toSet() shouldBe
        setOf("CardFront<Owner>")

    val component =
        Component(
            Canon.classTable.resolve(
                parse("PlayCard<Player1, Class<ProjectCard>, Class<AiCentral>>")
            )
        )
    LiveEffect.compile(component, transformers)
        .map(LiveEffect::effect)
        .single {
          "CardInvoice" in it.instruction.toString()
        }
        .instruction
        .toString() shouldBe
        "Owed<Player1, Class<MC>>! / AiCentral<Player1>.cost THEN " +
            "HandleCardTags<Player1, Class<AiCentral>>! " +
            "THEN CardInvoice<Player1, Class<AiCentral>>! THEN MAX 0 Barrier: " +
            "AiCentral<Player1> FROM ProjectCard<Hand<Player1>, Player1>!"
  }

  @Test
  internal fun `represented Class capture specializes every FakeResourceGiver effect`() {
    val klass = Canon.classTable.getClass(parse<Expression>("FakeResourceGiver").className)
    val component = Component(Canon.classTable.resolve(parse("FakeResourceGiver<Class<MC>>")))
    val resource =
        klass.typeVariables.single {
          it.declaration.expression.toString() == "StandardResource"
        }

    component.type
        .variableBindingsFrom(klass.defaultType, listOf(resource))[resource]
        .toString() shouldBe "MC"

    val productionEffect =
        transformers.classEffects(klass).single {
          it.instruction.toString().startsWith("42 Production")
        }
    productionEffect.typeVariables.variables.associate { variable ->
      variable.declaration.expression.toString() to
          productionEffect.typeVariables.expressionsOf(variable).map(Any::toString).toSet()
    } shouldBe mapOf("StandardResource" to setOf("StandardResource<Owner>"))

    LiveEffect.compile(component, transformers).map { it.effect.toString() }.toSet() shouldBe
        setOf(
            "SetupPhase: 42 MC<SoloOpponent>!",
            "SetupPhase: 42 Production<SoloOpponent, Class<MC>>!",
            "-MC<SoloOpponent> BY Player:: MC<SoloOpponent>! BY Engine",
            "MC<SoloOpponent> BY Player:: -MC<SoloOpponent>! BY Engine",
            "-Production<SoloOpponent, Class<MC>> BY Player:: " +
                "Production<SoloOpponent, Class<MC>>! BY Engine",
            "Production<SoloOpponent, Class<MC>> BY Player:: " +
                "-Production<SoloOpponent, Class<MC>>! BY Engine",
        )
  }

  @Test
  internal fun `explicit empty arguments distinguish a fresh Class-body choice`() {
    val klass = Canon.classTable.getClass(parse<Expression>("MonsInsurance").className)
    val effect = transformers.classEffects(klass).single { "MyResourceWasRemoved" in it.toString() }

    effect.toString() shouldBe
        "MyResourceWasRemoved<Anyone, Player<>> OR " +
            "MyProductionWasDecreased<Anyone, Player<>>: 3 MC<Anyone> FROM MC<Owner>."
    effect.typeVariables.variables.map { it.declaration.expression.toString() } shouldBe
        listOf("Anyone")
    LiveEffect.compile(
            Component(Canon.classTable.resolve(parse("MonsInsurance<Player1>"))),
            transformers,
        )
        .single { "MyResourceWasRemoved" in it.effect.toString() }
        .effect
        .toString() shouldBe
        "MyResourceWasRemoved<Anyone, Player<>> OR " +
            "MyProductionWasDecreased<Anyone, Player<>>: 3 MC<Anyone> FROM MC<Player1>."
  }

  @Test
  internal fun `trigger variable survives Production lowering`() {
    val klass = Canon.classTable.getClass(parse<Expression>("Manutech").className)
    val effect = transformers.classEffects(klass).single { "Production" in it.trigger.toString() }

    effect.typeVariables.variables.associate { variable ->
      variable.declaration.expression.toString() to
          effect.typeVariables.expressionsOf(variable).map(Any::toString).toSet()
    } shouldBe mapOf("StandardResource" to setOf("StandardResource<Owner>"))
    effect.trigger.toString() shouldBe "Production<Owner, Class<StandardResource>>"
    val trigger = (effect.trigger as Effect.Trigger.OnGainOf).expression
    val variable = effect.typeVariables.variables.single()
    val bindings =
        effect.typeVariables.bindingsFrom(
            trigger,
            Canon.classTable.resolve(trigger),
            Canon.classTable.resolve(parse("Production<Player1, Class<Plant>>")),
        )
    bindings[variable].toString() shouldBe "Plant"
    effect.typeVariables
        .bind(bindings)
        .transformInstructionTree(effect.instruction)
        .toString() shouldBe "Plant<Owner>!"
  }
}
