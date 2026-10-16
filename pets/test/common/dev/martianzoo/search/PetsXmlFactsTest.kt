package dev.martianzoo.search

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.InstructionTree
import kotlin.test.Test
import kotlin.test.assertEquals

internal class PetsXmlFactsTest {
  @Test
  fun generatedFieldsPreserveNamesAndNestedStructure() {
    val root = XmlElement("Example")
    PetsXml().node(root, parse<InstructionTree>("Plant: (2 Heat / 3 Animal)"))
    val query = XPathDocument(root.toString())
    assertEquals(
        listOf("2"),
        query
            .evaluate(
                "/Example/Gated/inner/Per/inner/Gain/scaledEx/ScaledExpression/scalar/ActualScalar/@value"
            )
            .map { it.value },
    )
    assertEquals(
        listOf("Animal"),
        query
            .evaluate(
                "/Example/Gated/inner/Per/metric/Scaled/inner/Count/expression/Expression/@className"
            )
            .map { it.value },
    )
    assertEquals(
        listOf("3"),
        query.evaluate("/Example/Gated/inner/Per/metric/Scaled/@unit").map { it.value },
    )
  }

  @Test
  fun gainArgumentsAreUniformAcrossGainsAndTransfers() {
    for ((source, expected) in
        listOf(
            "CityTile" to "0:false",
            "CityTile<>" to "0:true",
            "CityTile<LandArea>" to "1:true",
            "CityTile<LandArea> FROM CityTile<LandArea, Anyone>" to "1:true",
            "CityTile<LandArea, Player FROM Neutral>" to "2:true",
        )) {
      val root = XmlElement("Example")
      PetsXml().node(root, parse<InstructionTree>(source))
      val query = XPathDocument(root.toString())
      assertEquals(
          expected,
          query
              .evaluate(
                  "concat(//*[@gainingClass]/@gainingArgumentCount, ':', //*[@gainingClass]/@gainingArgumentsSpecified)"
              )
              .single()
              .value,
          source,
      )
    }
  }

  @Test
  fun triggerSummariesLookThroughWrappersAndRequireEveryAlternative() {
    for ((source, expected) in
        listOf(
            "This" to "true:false",
            "This IF Plant" to "true:false",
            "This BY Anyone" to "true:false",
            "End" to "false:true",
            "End IF Plant" to "false:true",
            "End OR End" to "false:true",
            "Plant OR End" to "false:false",
            "-End" to "false:false",
        )) {
      val root = XmlElement("Example")
      PetsXml().node(root, parse<Trigger>(source))
      val query = XPathDocument(root.toString())
      assertEquals(
          expected,
          query
              .evaluate("concat(/Example/*/@selfGainOnly, ':', /Example/*/@endOnly)")
              .single()
              .value,
          source,
      )
      assertEquals(
          parse<Trigger>(source).toString(),
          query.evaluate("/Example/*/@pets").single().value,
      )
    }
  }

  @Test
  fun expressionShapeKeysIgnoreOnlyTheOuterClass() {
    for ((other, matches) in
        listOf(
            "ProdOffset<Class<MC>, Player>" to true,
            "ProdOffset<Class<Steel>, Player>" to false,
            "ProdOffset<Class<MC>, SoloOpponent>" to false,
            "ProdOffset<Player, Class<MC>>" to false,
            "ProdOffset<Class<MC>, Player>(HAS Plant)" to false,
        )) {
      assertShapeEquality("Production<Class<MC>, Player>", other, matches)
    }
    assertShapeEquality("Production", "ProdOffset<>", false)
    assertShapeEquality("Production<Class<MC>>", "ProdOffset<Class<MC>>", true)
    assertShapeEquality(
        "Production<Class<MC>(HAS Plant)>",
        "ProdOffset<Class<MC>(HAS Heat)>",
        false,
    )
    val root = XmlElement("Example")
    PetsXml()
        .node(
            root,
            parse<InstructionTree>(
                "EACH Class<@StandardResource> { Plant / (Production<Class<@StandardResource>> - ProdOffset<Class<@StandardResource>>) }"
            ),
        )
    assertEquals(
        "true",
        XPathDocument(root.toString())
            .evaluate(
                "//Count[@countedClass = 'Production']/@countedShapeKey = //Count[@countedClass = 'ProdOffset']/@countedShapeKey"
            )
            .single()
            .value,
    )
  }

  private fun assertShapeEquality(left: String, right: String, expected: Boolean) {
    val root = XmlElement("Example")
    val xml = PetsXml()
    xml.node(root, parse<Expression>(left))
    xml.node(root, parse<Expression>(right))
    val query = XPathDocument(root.toString())
    assertEquals(
        expected.toString(),
        query
            .evaluate("/Example/Expression[1]/@shapeKey = /Example/Expression[2]/@shapeKey")
            .single()
            .value,
        "$left vs $right",
    )
  }
}
