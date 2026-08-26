package dev.martianzoo.tfm.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.Parsing.parseOneLinerClass
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.MetricType
import dev.martianzoo.pets.ast.PropertyValue.MetricValue
import dev.martianzoo.pets.ast.PropertyValue.NumberType
import dev.martianzoo.pets.ast.PropertyValue.NumberValue
import dev.martianzoo.pets.ast.PropertyValue.OptionalRequirementType
import dev.martianzoo.pets.ast.PropertyValue.RequirementType
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import dev.martianzoo.pets.ast.Requirement
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertFailsWith

internal class ClassDeclarationParsingTest {
  @Test
  internal fun propertiesUseBoundsLiteralsMetricsAndRequirements() {
    val declaration =
        parseClasses(
                """
                ABSTRACT CLASS Area {
                  HAS =1 This
                  DEFAULT +Area
                  row = Number
                  column = 2
                  score = Metric
                  scoreBasis = COUNT "TemperatureStep OR VenusScaleStep"
                  scaledScore = COUNT "8 TemperatureStep"
                  requirement = Requirement
                  optionalRequirement = Requirement?
                  specificRequirement = HAS "3 Plant, MAX 2 Steel"
                  This: Area
                }
                """
                    .trimIndent()
            )
            .single()

    declaration.properties shouldBe
        mapOf(
            PropertyName("row") to NumberType,
            PropertyName("column") to NumberValue(2),
            PropertyName("score") to MetricType,
            PropertyName("scoreBasis") to
                MetricValue(parse<Metric>("TemperatureStep OR VenusScaleStep")),
            PropertyName("scaledScore") to MetricValue(parse<Metric>("8 TemperatureStep")),
            PropertyName("requirement") to RequirementType,
            PropertyName("optionalRequirement") to OptionalRequirementType,
            PropertyName("specificRequirement") to
                RequirementValue(parse<Requirement>("3 Plant, MAX 2 Steel")),
        )
    declaration.properties.getValue(PropertyName("scoreBasis")).toString() shouldBe
        "COUNT \"TemperatureStep OR VenusScaleStep\""
    declaration.properties.getValue(PropertyName("specificRequirement")).toString() shouldBe
        "HAS \"3 Plant, MAX 2 Steel\""
  }

  @Test
  internal fun invalidDeclarationSourceUsesThePetsSyntaxDomain() {
    shouldThrow<PetSyntaxException> { parseClasses("CLASS Foo : Bar, Bar") }
    shouldThrow<PetSyntaxException> { parseClasses("CLASS Foo { DEFAULT Foo(HAS Bar) }") }
    shouldThrow<PetSyntaxException> { parseClasses("CLASS Foo @ CLASS Bar") }
    shouldThrow<PetSyntaxException> { parseClasses("CLASS Foo { cost = -1 }") }
    shouldThrow<PetSyntaxException> { parseClasses("CLASS Foo { score = TemperatureStep }") }
    shouldThrow<PetSyntaxException> { parseClasses("CLASS Foo { score = COUNT TemperatureStep }") }
    shouldThrow<PetSyntaxException> {
      parseClasses("CLASS Foo { requirement = HAS TemperatureStep }")
    }
    shouldThrow<PetSyntaxException> {
      parseClasses("""CLASS Foo { requirement = HAS "Temperature\"Step" }""")
    }
  }

  @Test
  internal fun ownerLocalClassesRequireDeclarationFileContext() {
    val source = "CLASS Sponsor { This: Mandate { -> Colony<> } }"
    val declarations = parseClasses(source)

    declarations.map { it.className }.shouldContainExactly(cn("Sponsor"), cn("Sponsor_Mandate"))
    declarations
        .first()
        .authoredEffects
        .shouldContainExactly(parse<Effect>("This: Sponsor_Mandate"))
    declarations.last().supertypes.shouldContainExactly(parse<Expression>("Mandate"))
    declarations.last().authoredActions.shouldContainExactly(parse<Action>("-> Colony<>"))
    shouldThrow<PetSyntaxException> { parseOneLinerClass(source) }
  }

  @Test
  internal fun simpleOneLiners() {
    parseClasses("CLASS Foo") // minimal
    parseClasses("ABSTRACT CLASS Foo") // abstract
    parseClasses("CLASS Foo<Bar>") // with spec
    parseClasses("CLASS Foo : Bar") // with supertype
    parseClasses("CLASS Foo { HAS MC }") // with same-line body
    parseClasses(
        "CLASS MC"
    ) // short class names use the same grammar in declarations and expressions
    parseClasses(" CLASS Foo") // with space first
    parseClasses("\nCLASS Foo") // with newline first
    parseClasses("CLASS Foo ") // with space after
    parseClasses("CLASS Foo\n") // with newline after
  }

  @Test
  internal fun declarationShortNamesAreNotPetsSyntax() {
    shouldThrow<PetSyntaxException> { parseClasses("CLASS Foo[FOO]") }
  }

  @Test
  internal fun ordinaryWhitespaceLineEndingsAndFinalComments() {
    parseClasses("CLASS\tFoo\r\nCLASS\tBar // final comment") shouldHaveSize 2
  }

  @Test
  internal fun incompleteFinalDeclarationIsRejected() {
    listOf(
            "CLASS Foo\nABSTRACT",
            "CLASS Foo\nCLASS",
            "CLASS Foo\nCLASS Bar<",
            "CLASS Foo\nCLASS Bar {",
            "CLASS Foo\nCLASS Bar { HAS",
            "CLASS Foo\n\"Bar docs\"",
        )
        .forEach { source -> assertFailsWith<PetSyntaxException>(source) { parseClasses(source) } }
  }

  @Test
  internal fun slightlyMoreComplex() {
    parseClasses(
        """
      CLASS Foo
      CLASS Bar
    """
    ) // two separate

    parseClasses(
        """
      CLASS Foo {
      }
    """
    ) // empty body

    parseClasses(
        """
      CLASS Foo {
        HAS Bar
      }
    """
    ) // invariant
    parseClasses(
        """
      CLASS Foo {
        DEFAULT +Foo!
      }
    """
    ) // default
    parseClasses(
        """
      CLASS Foo {
        DEFAULT +Foo!
      }
      CLASS Bar {
        DEFAULT +Bar!
      }
    """
    ) // two blocks
    parseClasses(
        """
      CLASS Foo {
        DEFAULT +Foo!
      }
      CLASS Bar, Qux
    """
    )
  }

  @Test
  internal fun body() {
    parseClasses(
            """
              CLASS Bar : Qux { DEFAULT +Bar?
                Foo -> Bar


                Foo: Bar
                CLASS Foo

              }
            """
        )
        .shouldHaveSize(2)
  }

  @Test
  internal fun series() {
    parseClasses(
        """
          CLASS Die {
          }
          CLASS DieHard {
            // whatever
          }

          CLASS Atomized

          CLASS Generation

        """
    )
  }

  @Test
  internal fun nesting() {
    val cs =
        parseClasses(
            """
              ABSTRACT CLASS Component

              CLASS One
              CLASS Two: One
              CLASS Three {
                  CLASS Four
                  CLASS Five: One
                  CLASS Six {
                      CLASS Seven
                      CLASS Eight: One
                  }
              }
            """
        )

    cs.map { it.supertypes }
        .shouldContainExactlyInAnyOrder(
            setOf<Expression>(),
            setOf<Expression>(),
            setOf(cn("One").expression),
            setOf<Expression>(),
            setOf(cn("Three").expression),
            setOf(cn("One").expression, cn("Three").expression),
            setOf(cn("Three").expression),
            setOf(cn("Six").expression),
            setOf(cn("One").expression, cn("Six").expression),
        )
  }

  @Test
  internal fun nestedOneLiner() {
    parseClasses(
        """
      CLASS One {
        CLASS Two { This: That }
        CLASS Three { This: That }
      }
    """
    )
  }

  @Test
  internal fun withDefaults() {
    parseClasses(
        """
        ABSTRACT CLASS Component {
           DEFAULT +Component!
           DEFAULT Component<Foo>
           DEFAULT Component<Foo>:

           CLASS What   // comment


           ABSTRACT CLASS Phase { // comment
               // comment

               CLASS End
           }
        }
    """
    )
  }
}
