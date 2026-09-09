package dev.martianzoo.pets.types

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.AbsentRequirementValue
import dev.martianzoo.pets.ast.PropertyValue.MetricType
import dev.martianzoo.pets.ast.PropertyValue.MetricValue
import dev.martianzoo.pets.ast.PropertyValue.NumberType
import dev.martianzoo.pets.ast.PropertyValue.NumberValue
import dev.martianzoo.pets.ast.PropertyValue.OptionalRequirementType
import dev.martianzoo.pets.ast.PropertyValue.RequirementType
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import dev.martianzoo.pets.ast.Requirement
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Section 9 of `docs/type-system-spec.md`: class properties. */
internal class Spec09PropertiesTest {

  private fun property(table: ClassTable, klass: String, name: String) =
      table.getClass(cn(klass)).properties[PropertyName(name)]

  // 9-1 Declaring a property

  @Test
  internal fun `9-1 a property is declared either as a bound or as a value`() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS TemperatureStep
            ABSTRACT CLASS Award {
              metric = Metric
            }
            CLASS Thermalist : Award {
              metric = COUNT "TemperatureStep"
            }
            ABSTRACT CLASS CardFront {
              cost = Number
            }
            CLASS Ants : CardFront {
              cost = 9
            }
            """
                .trimIndent()
        )

    property(table, "Award", "metric") shouldBe MetricType
    property(table, "Thermalist", "metric") shouldBe MetricValue(parse<Metric>("TemperatureStep"))
    property(table, "CardFront", "cost") shouldBe NumberType
    property(table, "Ants", "cost") shouldBe NumberValue(9)
  }

  @Test
  internal fun `9-1 requirement properties may be required or optional`() {
    val table =
        loadTypes(
            """
            CLASS Plant
            ABSTRACT CLASS Milestone {
              requirement = Requirement
            }
            CLASS Gardener : Milestone {
              requirement = HAS "3 Plant"
            }
            ABSTRACT CLASS CardFront {
              requirement = Requirement?
            }
            CLASS Ants : CardFront
            CLASS Tardigrades : CardFront {
              requirement = HAS "3 Plant"
            }
            """
                .trimIndent()
        )

    property(table, "Milestone", "requirement") shouldBe RequirementType
    property(table, "Gardener", "requirement") shouldBe
        RequirementValue(parse<Requirement>("3 Plant"))
    property(table, "CardFront", "requirement") shouldBe OptionalRequirementType
    property(table, "Ants", "requirement") shouldBe AbsentRequirementValue
    property(table, "Tardigrades", "requirement") shouldBe
        RequirementValue(parse<Requirement>("3 Plant"))
  }

  // 9-2 Narrowing through inheritance

  @Test
  internal fun `9-2 a subclass may narrow an inherited bound`() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS TemperatureStep
            ABSTRACT CLASS Area { score = Metric }
            ABSTRACT CLASS FixedArea : Area { score = Number }
            CLASS EightPointArea : FixedArea { score = 8 }
            CLASS ThermalArea : Area { score = COUNT "TemperatureStep" }
            """
                .trimIndent()
        )

    property(table, "Area", "score") shouldBe MetricType
    property(table, "FixedArea", "score") shouldBe NumberType
    property(table, "EightPointArea", "score") shouldBe NumberValue(8)
    property(table, "ThermalArea", "score") shouldBe MetricValue(parse<Metric>("TemperatureStep"))
  }

  @Test
  internal fun `9-2 a subclass may not override a value that is already fixed`() {
    shouldThrow<PetException> {
      loadTypes(
          "ABSTRACT CLASS Area { row = Number }",
          "ABSTRACT CLASS FixedArea : Area { row = 8 }",
          "CLASS Tharsis_2_2 : FixedArea { row = 9 }",
      )
    }
  }

  @Test
  internal fun `9-2 a subclass may not widen or sidestep an inherited bound`() {
    shouldThrow<PetException> {
      loadTypes(
          "ABSTRACT CLASS TemperatureStep",
          "ABSTRACT CLASS Area { row = Number }",
          "CLASS Tharsis_2_2 : Area { row = TemperatureStep }",
      )
    }
    shouldThrow<PetException> {
      loadTypes(
          "ABSTRACT CLASS TemperatureStep",
          "ABSTRACT CLASS Milestone { requirement = Requirement }",
          "CLASS Gardener : Milestone { requirement = TemperatureStep }",
      )
    }
  }

  // 9-3 Concrete classes are complete

  @Test
  internal fun `9-3 a concrete class must fix every property it inherits`() {
    shouldThrow<PetException> {
      loadTypes("ABSTRACT CLASS Area { row = Number }", "CLASS Tharsis_2_2 : Area")
    }
    shouldThrow<PetException> {
      loadTypes("ABSTRACT CLASS Award { metric = Metric }", "CLASS Thermalist : Award")
    }
    shouldThrow<PetException> {
      loadTypes(
          "ABSTRACT CLASS Milestone { requirement = Requirement }",
          "CLASS Gardener : Milestone",
      )
    }
  }

  @Test
  internal fun `9-3 an optional requirement is the one bound a concrete class may leave open`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS CardFront { requirement = Requirement? }",
            "CLASS Ants : CardFront",
        )

    property(table, "Ants", "requirement") shouldBe AbsentRequirementValue
  }

  // 9-4 Inheriting from several supertypes

  @Test
  internal fun `9-4 the same inherited fact arriving by two paths is one fact`() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS Area { row = 8 }
            ABSTRACT CLASS FirstArea : Area
            ABSTRACT CLASS SecondArea : Area
            CLASS Tharsis_2_2 : FirstArea, SecondArea
            """
                .trimIndent()
        )

    property(table, "Tharsis_2_2", "row") shouldBe NumberValue(8)
  }

  @Test
  internal fun `9-4 a narrower fact wins when the other path merely restates its origin`() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS Area { score = Metric }
            ABSTRACT CLASS FixedArea : Area { score = Number }
            ABSTRACT CLASS PlainArea : Area
            ABSTRACT CLASS RejoinedArea : FixedArea, PlainArea
            CLASS Tharsis_2_2 : RejoinedArea { score = 8 }
            """
                .trimIndent()
        )

    property(table, "RejoinedArea", "score") shouldBe NumberType
    property(table, "Tharsis_2_2", "score") shouldBe NumberValue(8)
  }

  @Test
  internal fun `9-4 two properties with one name but unrelated origins are an error`() {
    shouldThrow<PetException> {
      loadTypes(
          "ABSTRACT CLASS FirstArea { row = 8 }",
          "ABSTRACT CLASS SecondArea { row = 8 }",
          "CLASS Tharsis_2_2 : FirstArea, SecondArea",
      )
    }
  }

  @Test
  internal fun `9-4 divergent narrowings of one property are an error`() {
    shouldThrow<PetException> {
      loadTypes(
          "ABSTRACT CLASS Area { row = Number }",
          "ABSTRACT CLASS FirstArea : Area { row = 8 }",
          "ABSTRACT CLASS SecondArea : Area { row = 8 }",
          "CLASS Tharsis_2_2 : FirstArea, SecondArea",
      )
    }
    shouldThrow<PetException> {
      loadTypes(
          "ABSTRACT CLASS TemperatureStep",
          "ABSTRACT CLASS Area { score = Metric }",
          "ABSTRACT CLASS FirstArea : Area { score = Number }",
          "ABSTRACT CLASS SecondArea : Area { score = COUNT \"TemperatureStep\" }",
          "CLASS Tharsis_2_2 : FirstArea, SecondArea",
      )
    }
  }

  // 9-5 Reading a property from a type

  @Test
  internal fun `9-5 a type reads the concrete property values of its root class`() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS TemperatureStep
            ABSTRACT CLASS Area {
              row = Number
              score = Metric
              requirement = Requirement
            }
            CLASS Tharsis_2_2 : Area {
              row = 8
              score = COUNT "TemperatureStep"
              requirement = HAS "TemperatureStep"
            }
            """
                .trimIndent()
        )
    val area = table.resolve(te("Tharsis_2_2"))

    area.getNumberPropertyValue("row") shouldBe 8
    area.getMetricPropertyValue("score") shouldBe parse<Metric>("TemperatureStep")
    area.getRequirementPropertyValue("requirement") shouldBe parse<Requirement>("TemperatureStep")
  }

  @Test
  internal fun `9-5 an absent optional requirement reads as none`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS CardFront { requirement = Requirement? }",
            "CLASS Ants : CardFront",
        )

    table.resolve(te("Ants")).getRequirementPropertyValue("requirement") shouldBe null
  }

  @Test
  internal fun `9-5 reading a property that is still a bound is a programming error`() {
    val table = loadTypes("ABSTRACT CLASS Milestone { requirement = Requirement }")

    shouldThrow<IllegalStateException> {
      table.resolve(te("Milestone")).getRequirementPropertyValue("requirement")
    }
    shouldThrow<Exception> { table.resolve(te("Milestone")).getNumberPropertyValue("nope") }
  }

  // 9-6 Properties are class facts

  @Test
  internal fun `9-6 properties take no part in type identity or subtyping`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS CardFront { cost = Number }",
            "CLASS Ants : CardFront { cost = 9 }",
            "CLASS Tardigrades : CardFront { cost = 4 }",
        )

    table.resolve(te("Ants")).isSubtypeOf(table.resolve(te("CardFront"))) shouldBe true
    table.resolve(te("Ants")).dependencies.keys shouldBe listOf()
    (table.resolve(te("Ants")) == table.resolve(te("Tardigrades"))) shouldBe false
  }
}
