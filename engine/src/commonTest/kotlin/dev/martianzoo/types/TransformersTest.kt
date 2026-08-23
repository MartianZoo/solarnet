package dev.martianzoo.types

import dev.martianzoo.api.Exceptions.KindException
import dev.martianzoo.api.Exceptions.PetSyntaxException
import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.engine.Transformers
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.tfm.data.Prod
import dev.martianzoo.tfm.engine.CanonClassesTest
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
        "GreeneryTile<>",
        "GreeneryTile<LandArea(HAS? Neighbor<OwnedTile<Owner>>, MAX 0 Tile), Owner>!",
    )

    checkApplyDefaults(
        "Heat FROM Owed<>!",
        "Heat<Owner> FROM Owed<Owner, Class<Megacredit>>!",
    )
    checkApplyDefaults(
        "Heat FROM Owed<>.",
        "Heat<Owner> FROM Owed<Owner, Class<Megacredit>>.",
    )
    checkApplyDefaults(
        "Heat FROM Owed<>",
        "Heat<Owner> FROM Owed<Owner, Class<Megacredit>>!",
    )
    checkApplyDefaults("Owed<>", "Owed<Owner, Class<Megacredit>>!")
    checkApplyDefaults("-Owed", "-Owed<Owner>.")
    checkApplyDefaults("-Owed<>", "-Owed<Owner, Class<Megacredit>>.")
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
    val transformers = Transformers(CanonClassesTest.table)
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
    val ep: Effect = Prod.deprodify(transformers.classTable).transformEffect(e)
    ep.toString() shouldBe s
  }

  @Test
  internal fun testDeprodify_simple() {
    val prodden: Effect = parse("This: PROD[Plant / PlantTag]")
    val deprodden: Effect = Prod.deprodify(setOf(cn("Plant"))).transformEffect(prodden)
    deprodden.toString() shouldBe "This: Production<Class<Plant>> / PlantTag"
  }

  @Test
  internal fun deprodifyPreservesAResourceRefinementOnItsClassDependency() {
    val prodden: Instruction = parse("PROD[StandardResource(HAS LowestProduction)]")

    Prod.deprodify(setOf(cn("StandardResource"))).transformInstruction(prodden).toString() shouldBe
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
    val deprodden: Effect = Prod.deprodify(transformers.classTable).transformEffect(prodden)
    deprodden shouldBe expected
  }

  @Test
  internal fun `invalid atomic change after trigger specialization becomes Die`() {
    val general = CanonClassesTest.table.resolve(parse<Expression>("CardFront(HAS BioTag)"))
    val specific = CanonClassesTest.table.resolve(parse<Expression>("IndustrialMicrobes<Player1>"))
    val instruction = parse<Instruction>("Plant OR CardResource<CardFront(HAS BioTag)>")

    transformers
        .checkedSubstituter(general, specific)
        .transformInstruction(instruction)
        .toString() shouldBe "Plant OR Die!"
  }

  @Test
  internal fun `nested abstract dependency specializes to the concrete changed component`() {
    val general =
        CanonClassesTest.table.resolve(parse<Expression>("MicrobeTag<Player1, CardFront<Player1>>"))
    val specific =
        CanonClassesTest.table.resolve(
            parse<Expression>("MicrobeTag<Player1, Decomposers<Player1>>")
        )
    val instruction = parse<Instruction>("Microbe<CardFront<Player1>>")

    transformers
        .checkedSubstituter(general, specific)
        .transformInstruction(instruction)
        .toString() shouldBe "Microbe<Decomposers<Player1>>"
  }

  @Test
  internal fun `specialization reaches a nested dependency when its containing class also specializes`() {
    val general =
        CanonClassesTest.table.resolve(
            parse<Expression>("AcceptFromCard<Player1, ResourceCard<Player1, Class<CardResource>>>")
        )
    val specific =
        CanonClassesTest.table.resolve(
            parse<Expression>("AcceptFromCard<Player1, Dirigibles<Player1>>")
        )
    val instruction = parse<Instruction>("CardResource<ResourceCard>")

    transformers
        .checkedSubstituter(general, specific)
        .transformInstruction(instruction)
        .toString() shouldBe "Floater<Dirigibles>"
  }

  @Test
  internal fun `linkage specialization leaves an unlinked occurrence of the same class independent`() {
    val general =
        CanonClassesTest.table.resolve(parse<Expression>("MicrobeTag<Player1, CardFront<Player1>>"))
    val specific =
        CanonClassesTest.table.resolve(
            parse<Expression>("MicrobeTag<Player1, Decomposers<Player1>>")
        )
    val instruction =
        parse<Instruction>("Microbe<CardFront<Player1>> OR Microbe<CardFront<Player2>>")

    transformers
        .checkedLinkageSubstituter(
            general,
            specific,
            setOf(parse("CardFront<Player1>")),
        )
        .transformInstruction(instruction)
        .toString() shouldBe "Microbe<Decomposers<Player1>> OR Microbe<CardFront<Player2>>"
  }

  @Test
  internal fun `linked complemented dependency specializes to the concrete event dependency`() {
    val general = CanonClassesTest.table.resolve(parse<Expression>("Resource<!Player2>"))
    val specific = CanonClassesTest.table.resolve(parse<Expression>("Plant<Player3>"))
    val instruction = parse<Instruction>("Steel<!Player2>")

    transformers
        .checkedLinkageSubstituter(general, specific, setOf(parse("!Player2")))
        .transformInstruction(instruction)
        .toString() shouldBe "Steel<Player3>"
  }
}
