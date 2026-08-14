package dev.martianzoo.types

import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.engine.Transformers
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.tfm.engine.CanonClassesTest
import dev.martianzoo.tfm.engine.Prod
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class TransformersTest {
  @Test
  fun test() {
    checkApplyDefaults("Heat", "Heat<Owner>!")
    checkApplyDefaults("-5 Heat", "-5 Heat<Owner>!")
    checkApplyDefaults("VictoryPoint", "VictoryPoint<Owner>!")
    checkApplyDefaults("OceanTile", "OceanTile<WaterArea>.")
    checkApplyDefaults("-OceanTile", "-OceanTile.")
    checkApplyDefaults(
        "CityTile",
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
        "GreeneryTile",
        "GreeneryTile<LandArea(HAS? Neighbor<OwnedTile<Owner>>, MAX 0 Tile), Owner>!",
    )

    checkApplyDefaults(
        "Heat FROM Owed!",
        "Heat<Owner> FROM Owed<Owner, Class<Megacredit>>!",
    )
    checkApplyDefaults(
        "Heat FROM Owed.",
        "Heat<Owner> FROM Owed<Owner, Class<Megacredit>>.",
    )
    checkApplyDefaults(
        "Heat FROM Owed",
        "Heat<Owner> FROM Owed<Owner, Class<Megacredit>>!",
    )
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
  ): Instruction = transformers.insertDefaults(context).transform(parse(original))

  @Test
  fun testDeprodify_noProd() {
    val s = "Foo<Bar>: Bax OR Qux"
    val e: Effect = parse(s)
    val ep: Effect = Prod.deprodify(transformers.classTable).transform(e)
    ep.toString() shouldBe s
  }

  @Test
  fun testDeprodify_simple() {
    val prodden: Effect = parse("This: PROD[Plant / PlantTag]")
    val deprodden: Effect = Prod.deprodify(setOf(cn("Plant"))).transform(prodden)
    deprodden.toString() shouldBe "This: Production<Class<Plant>> / PlantTag"
  }

  @Test
  fun testDeprodify_lessSimple() {
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
    val deprodden: Effect = Prod.deprodify(transformers.classTable).transform(prodden)
    deprodden shouldBe expected
  }

  @Test
  fun `invalid atomic change after trigger specialization becomes Die`() {
    val general = CanonClassesTest.table.resolve(parse<Expression>("CardFront(HAS BioTag)"))
    val specific = CanonClassesTest.table.resolve(parse<Expression>("Card158<Player1>"))
    val instruction = parse<Instruction>("Plant OR CardResource<CardFront(HAS BioTag)>")

    transformers.checkedSubstituter(general, specific).transform(instruction).toString() shouldBe
        "Plant OR Die!"
  }

  @Test
  fun `nested abstract dependency specializes to the concrete changed component`() {
    val general =
        CanonClassesTest.table.resolve(parse<Expression>("MicrobeTag<Player1, CardFront<Player1>>"))
    val specific =
        CanonClassesTest.table.resolve(parse<Expression>("MicrobeTag<Player1, Card131<Player1>>"))
    val instruction = parse<Instruction>("Microbe<CardFront<Player1>>")

    transformers.checkedSubstituter(general, specific).transform(instruction).toString() shouldBe
        "Microbe<Card131<Player1>>"
  }

  @Test
  fun `linkage specialization leaves an unlinked occurrence of the same class independent`() {
    val general =
        CanonClassesTest.table.resolve(parse<Expression>("MicrobeTag<Player1, CardFront<Player1>>"))
    val specific =
        CanonClassesTest.table.resolve(parse<Expression>("MicrobeTag<Player1, Card131<Player1>>"))
    val instruction =
        parse<Instruction>("Microbe<CardFront<Player1>> OR Microbe<CardFront<Player2>>")

    transformers
        .checkedLinkageSubstituter(
            general,
            specific,
            setOf(parse("CardFront<Player1>")),
        )
        .transform(instruction)
        .toString() shouldBe "Microbe<Card131<Player1>> OR Microbe<CardFront<Player2>>"
  }

  @Test
  fun `linked complemented dependency specializes to the concrete event dependency`() {
    val general = CanonClassesTest.table.resolve(parse<Expression>("Resource<!Player2>"))
    val specific = CanonClassesTest.table.resolve(parse<Expression>("Plant<Player3>"))
    val instruction = parse<Instruction>("Steel<!Player2>")

    transformers
        .checkedLinkageSubstituter(general, specific, setOf(parse("!Player2")))
        .transform(instruction)
        .toString() shouldBe "Steel<Player3>"
  }
}
