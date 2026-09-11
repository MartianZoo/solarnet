package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.Parsing.parseOneLinerClass
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.SystemClasses.ANYONE
import dev.martianzoo.pets.api.SystemClasses.ATOMIZED
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.api.SystemClasses.OK
import dev.martianzoo.pets.api.SystemClasses.OWNED
import dev.martianzoo.pets.api.SystemClasses.OWNER
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
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
import dev.martianzoo.pets.data.ClassDeclaration
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Section 1 of `docs/pets-language-spec.md`: what a Pets source is made of. */
internal class Lang01SourceTest {

  // L1-1 A source is a sequence of declarations

  @Test
  internal fun `L1-1 declarations come back in source order`() {
    val declarations = parseClasses("CLASS Alpha\nCLASS Beta\nCLASS Gamma")

    declarations.map { it.className } shouldContainExactly
        listOf(cn("Alpha"), cn("Beta"), cn("Gamma"))
  }

  @Test
  internal fun `L1-1 a source of whitespace and comments declares nothing`() {
    parseClasses("").shouldBeEmpty()
    parseClasses("\n\n  // just a note\n\n").shouldBeEmpty()
  }

  @Test
  internal fun `L1-1 an incomplete final declaration rejects the whole source`() {
    listOf(
            "CLASS Alpha\nABSTRACT",
            "CLASS Alpha\nCLASS",
            "CLASS Alpha\nCLASS Beta<",
            "CLASS Alpha\nCLASS Beta {",
            "CLASS Alpha\n\"a docstring with no class\"",
        )
        .forEach { shouldThrow<PetSyntaxException> { parseClasses(it) } }
  }

  // L1-2 Signatures

  @Test
  internal fun `L1-2 a signature carries a kind, dependencies and supertypes`() {
    val declaration = parseClasses("ABSTRACT CLASS Tile<Area> : Occupant, Owned<Owner>").single()

    declaration.className shouldBe cn("Tile")
    declaration.abstract shouldBe true
    declaration.dependencies shouldContainExactly listOf(parse<Expression>("Area"))
    declaration.supertypes shouldBe
        setOf(parse<Expression>("Occupant"), parse<Expression>("Owned<Owner>"))
    parseClasses("CLASS GreeneryTile").single().abstract shouldBe false
  }

  // L1-3 Several signatures under one keyword

  @Test
  internal fun `L1-3 one CLASS keyword can introduce several classes`() {
    val declarations = parseClasses("ABSTRACT CLASS Alpha, Beta<Area>, Gamma : Root")

    declarations.map { it.className } shouldContainExactly
        listOf(cn("Alpha"), cn("Beta"), cn("Gamma"))
    declarations.forEach { it.abstract shouldBe true }
    declarations[1].dependencies shouldContainExactly listOf(parse<Expression>("Area"))
    declarations[2].supertypes shouldBe setOf(parse<Expression>("Root"))
  }

  @Test
  internal fun `L1-3 a comma after a supertype list continues that list`() {
    val declarations = parseClasses("CLASS Alpha : Root, Beta")

    declarations.map { it.className } shouldContainExactly listOf(cn("Alpha"))
    declarations.single().supertypes shouldBe
        setOf(parse<Expression>("Root"), parse<Expression>("Beta"))
  }

  @Test
  internal fun `L1-3 several signatures cannot share a body`() {
    shouldThrow<PetSyntaxException> { parseClasses("CLASS Alpha, Beta { HAS Qux }") }
  }

  // L1-4 Bodies

  @Test
  internal fun `L1-4 a body holds invariants, defaults, properties, effects and actions`() {
    val declaration =
        parseClasses(
                """
                CLASS Gardener : Scored {
                  HAS MAX 1 This
                  DEFAULT +Gardener<LandArea>
                  cost = 8
                  This: Plant
                  Plant -> Heat
                }
                """
                    .trimIndent()
            )
            .single()

    declaration.invariants shouldBe setOf(parse<Requirement>("MAX 1 This"))
    declaration.defaultsDeclaration.gainOnly.specs shouldContainExactly
        listOf(parse<Expression>("LandArea"))
    declaration.properties shouldBe mapOf(PropertyName("cost") to NumberValue(8))
    declaration.authoredEffects shouldContainExactly listOf(parse<Effect>("This: Plant"))
    declaration.authoredActions shouldContainExactly listOf(parse<Action>("Plant -> Heat"))
  }

  @Test
  internal fun `L1-4 semicolons separate body elements on one line`() {
    val multiline = parseClasses("CLASS Alpha {\n  HAS MC\n  cost = 2\n}").single()

    parseClasses("CLASS Alpha { HAS MC; cost = 2 }").single() shouldBe multiline
  }

  @Test
  internal fun `L1-4 only a newline-separated body may nest a declaration`() {
    parseClasses("CLASS Alpha {\n  CLASS Beta\n}").map { it.className } shouldContainExactly
        listOf(cn("Alpha"), cn("Beta"))
    shouldThrow<PetSyntaxException> { parseClasses("CLASS Alpha { HAS MC; CLASS Beta }") }
  }

  // L1-5 Nesting

  @Test
  internal fun `L1-5 a nested declaration becomes a sibling naming its container`() {
    val declarations =
        parseClasses(
            """
            ABSTRACT CLASS Area {
              ABSTRACT CLASS MarsArea {
                CLASS Mars1
              }
              CLASS RemoteArea : Extra
            }
            """
                .trimIndent()
        )

    declarations.map { it.className } shouldContainExactly
        listOf(cn("Area"), cn("MarsArea"), cn("Mars1"), cn("RemoteArea"))
    declarations.map { it.supertypes } shouldContainExactly
        listOf(
            setOf(),
            setOf(parse<Expression>("Area")),
            setOf(parse<Expression>("MarsArea")),
            setOf(parse<Expression>("Area"), parse<Expression>("Extra")),
        )
  }

  // L1-6 Docstrings

  @Test
  internal fun `L1-6 a docstring precedes the CLASS keyword and survives rendering`() {
    val declaration = parseClasses("\"The one root\"\nABSTRACT CLASS Component").single()

    declaration.docstring shouldBe "The one root"
    parseClasses(declaration.toString()).single() shouldBe declaration
  }

  // L1-7 DEFAULT clauses

  @Test
  internal fun `L1-7 a DEFAULT clause names the class declaring it`() {
    val declaration =
        parseClasses("CLASS Tile<Area> {\n  DEFAULT Tile<Owner>\n  DEFAULT +Tile<LandArea>\n}")
            .single()

    declaration.defaultsDeclaration.universal.specs shouldContainExactly
        listOf(parse<Expression>("Owner"))
    declaration.defaultsDeclaration.gainOnly.specs shouldContainExactly
        listOf(parse<Expression>("LandArea"))
    shouldThrow<PetSyntaxException> { parseClasses("CLASS Tile<Area> { DEFAULT Other<LandArea> }") }
  }

  // L1-8 Properties

  @Test
  internal fun `L1-8 a property is assigned at most once per body`() {
    parseClasses("CLASS Alpha { cost = 1; score = 2 }").single().properties.keys shouldBe
        setOf(PropertyName("cost"), PropertyName("score"))
    shouldThrow<PetSyntaxException> { parseClasses("CLASS Alpha { cost = 1; cost = 2 }") }
  }

  @Test
  internal fun `L1-8 a value is a bound word, a number, or quoted Pets`() {
    val declaration =
        parseClasses(
                """
                ABSTRACT CLASS Area {
                  row = Number
                  column = 2
                  score = Metric
                  scoreBasis = COUNT "TemperatureStep OR VenusScaleStep"
                  scaledScore = COUNT "8 TemperatureStep"
                  requirement = Requirement
                  optionalRequirement = Requirement?
                  specificRequirement = HAS "3 Plant, MAX 2 Steel"
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
  internal fun `L1-8 other right-hand sides are rejected`() {
    listOf(
            "CLASS Alpha { cost = -1 }",
            "CLASS Alpha { score = TemperatureStep }",
            "CLASS Alpha { score = COUNT TemperatureStep }",
            "CLASS Alpha { requirement = HAS TemperatureStep }",
            """CLASS Alpha { requirement = HAS "Temperature\"Step" }""",
        )
        .forEach { shouldThrow<PetSyntaxException> { parseClasses(it) } }
  }

  // L1-9 Signature expressions carry no refinements

  @Test
  internal fun `L1-9 a signature expression may not be refined at any depth`() {
    listOf(
            "CLASS Alpha<Beta(HAS Qux)>",
            "CLASS Alpha<Beta(NOT Qux)>",
            "CLASS Alpha : Beta(NOT Qux)",
            "CLASS Alpha<Beta<Qux(HAS Eep)>>",
        )
        .forEach { shouldThrow<PetSyntaxException> { parseClasses(it) } }
  }

  @Test
  internal fun `L1-9 other malformed declaration sources are rejected too`() {
    listOf(
            "CLASS Alpha : Beta, Beta",
            "CLASS Alpha @ CLASS Beta",
            "CLASS Alpha { DEFAULT Alpha(HAS Beta) }",
            "CLASS Alpha[ALPHA]",
        )
        .forEach { shouldThrow<PetSyntaxException> { parseClasses(it) } }
  }

  // L1-10 Whitespace and comments

  @Test
  internal fun `L1-10 whitespace is insignificant and comments run to end of line`() {
    parseClasses("CLASS\tAlpha\r\nCLASS  Beta  // trailing note").map {
      it.className
    } shouldContainExactly listOf(cn("Alpha"), cn("Beta"))
  }

  @Test
  internal fun `L1-10 whitespace is required where two tokens would run together`() {
    parse<Instruction>("2 MC").toString() shouldBe "2 MC"
    shouldThrow<PetSyntaxException> { parse<Instruction>("2MC") }
  }

  @Test
  internal fun `L1-10 a backslash before a line ending continues the line`() {
    parseClasses("CLASS Alpha {\n  This: Plant\\\n OR Heat\n}")
        .single()
        .authoredEffects shouldContainExactly listOf(parse<Effect>("This: Plant OR Heat"))
  }

  @Test
  internal fun `L1-10 a declaration may be spelled many ways`() {
    listOf(
            "CLASS Alpha",
            "ABSTRACT CLASS Alpha",
            "CLASS Alpha<Beta>",
            "CLASS Alpha : Beta",
            "CLASS Alpha { HAS MC }",
            "CLASS MC",
            " CLASS Alpha",
            "\nCLASS Alpha",
            "CLASS Alpha ",
            "CLASS Alpha\n",
        )
        .forEach { parseClasses(it).size shouldBe 1 }

    parseClasses("CLASS Alpha {\n}").size shouldBe 1
    parseClasses("CLASS Alpha {\n  // just a comment\n}").size shouldBe 1
    parseClasses("CLASS Alpha {\n  DEFAULT +Alpha?\n}\nCLASS Beta, Gamma").size shouldBe 3
    parseClasses("CLASS Alpha : Root {\n  Beta -> Alpha\n\n\n  Beta: Alpha\n  CLASS Beta\n\n}")
        .size shouldBe 2
    parseClasses("CLASS One {\n  CLASS Two { This: That }\n  CLASS Three { This: That }\n}")
        .size shouldBe 3
  }

  // L1-11 Rendering

  @Test
  internal fun `L1-11 a declaration renders as parseable source in both shapes`() {
    val source =
        """
        "A useful class"
        ABSTRACT CLASS Alpha<Beta, Qux> : Eep, Root {
          HAS =1 This
          DEFAULT Alpha<Xyz>
          DEFAULT +Alpha<Abc>?
          DEFAULT -Alpha<Def>!
          row = Number
          column = 2
          This: DoStuff
        }
        """
            .trimIndent()
    val declaration = parseClasses(source).single()

    declaration.toString() shouldBe source
    parseClasses(declaration.toString()).single() shouldBe declaration
    parseClasses(declaration.toString(oneLine = true)).single() shouldBe declaration
    declaration.toString(oneLine = true) shouldBe
        "\"A useful class\"\nABSTRACT CLASS Alpha<Beta, Qux> : Eep, Root " +
            "{ HAS =1 This; DEFAULT Alpha<Xyz>; DEFAULT +Alpha<Abc>?; DEFAULT -Alpha<Def>!; " +
            "row = Number; column = 2; This: DoStuff }"
  }

  // L1-12 One declaration on its own

  @Test
  internal fun `L1-12 parseOneLinerClass reads exactly one declaration`() {
    val declaration: ClassDeclaration = parseOneLinerClass("CLASS Alpha : Beta { This: Qux }")

    declaration.className shouldBe cn("Alpha")
    declaration.authoredEffects shouldContainExactly listOf(parse<Effect>("This: Qux"))
    shouldThrow<PetSyntaxException> { parseOneLinerClass("CLASS Alpha, Beta") }
  }

  // L1-13 The system declarations

  @Test
  internal fun `L1-13 every catalog receives the vocabulary these rules depend on`() {
    val byName = systemClassDeclarations.associateBy { it.className }

    byName.keys shouldContainAll
        listOf(COMPONENT, CLASS, ANYONE, OWNER, OWNED, OK, ATOMIZED, cn("Actor"), cn("Die"))
    byName.getValue(COMPONENT).abstract shouldBe true
    byName.getValue(COMPONENT).supertypes.shouldBeEmpty()
    byName.getValue(OWNED).defaultsDeclaration.universal.specs shouldContainExactly
        listOf(parse<Expression>("Owner"))
    byName.getValue(OK).abstract shouldBe false
  }
}
