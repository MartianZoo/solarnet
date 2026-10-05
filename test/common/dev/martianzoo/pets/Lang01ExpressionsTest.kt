package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.Refinement
import dev.martianzoo.pets.ast.Expression.Refinement.And
import dev.martianzoo.pets.ast.Expression.Refinement.Has
import dev.martianzoo.pets.ast.Expression.Refinement.Not
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Reference
import dev.martianzoo.pets.ast.InstructionTree
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

/** Section 1 of `docs/pets-language-spec.md`: how an expression is written. */
internal class Lang01ExpressionsTest {

  // L1-1 The shape of an expression

  @Test
  internal fun `L1-1 a class name, optional arguments, optional refinement`() {
    val simple = parse<Expression>("Plant")
    simple.className shouldBe cn("Plant")
    simple.arguments.shouldContainExactly(emptyList())
    simple.refinement shouldBe null
    simple.simple shouldBe true

    val complex = parse<Expression>("CityTile<Player2, MarsArea>(HAS MAX 0 OceanTile)")
    complex.className shouldBe cn("CityTile")
    complex.arguments shouldContainExactly
        listOf(parse<Expression>("Player2"), parse<Expression>("MarsArea"))
    complex.refinement shouldBe Has(parse("MAX 0 OceanTile"))
  }

  @Test
  internal fun `L1-1 arguments are themselves expressions, to any depth`() {
    parse<Expression>("Aa<Bb<Cc<Dd, Ee, Ff<Gg<Hh<Me>>, Jj>>, Kk>>").toString() shouldBe
        "Aa<Bb<Cc<Dd, Ee, Ff<Gg<Hh<Me>>, Jj>>, Kk>>"
  }

  // L1-2 The empty argument list

  @Test
  internal fun `L1-2 an empty argument list is a different spelling from none`() {
    val empty = parse<Expression>("OceanTile<>")
    val absent = parse<Expression>("OceanTile")

    empty.argumentsSpecified shouldBe true
    absent.argumentsSpecified shouldBe false
    empty.arguments shouldBe absent.arguments
    empty.toString() shouldBe "OceanTile<>"
    empty shouldNotBe absent
  }

  // L1-3 Refinements

  @Test
  internal fun `L1-3 there are two kinds of refinement clause`() {
    (parse<Refinement>("(HAS Plant)") as Has).requirement shouldBe parse("Plant")
    (parse<Refinement>("(NOT Player1)") as Not).excluded shouldBe parse("Player1")
    parse<Expression>("Anyone(NOT Player1)").toString() shouldBe "Anyone(NOT Player1)"
  }

  @Test
  internal fun `L1-3 a refinement conjoins its clauses`() {
    val refinement = parse<Refinement>("(HAS Plant, NOT Player1)") as And
    refinement.refinements.map { it::class } shouldBe listOf(Has::class, Not::class)

    shouldThrow<PetSyntaxException> { parse<Expression>("Anyone(NOT Player1)(HAS Plant)") }
  }

  @Test
  internal fun `L1-3 a top-level comma separates clauses`() {
    shouldThrow<PetSyntaxException> { parse<Expression>("Anyone(HAS Plant, Steel)") }
    parse<Expression>("Anyone(HAS (Plant, Steel) OR Heat, NOT Player1)").toString() shouldBe
        "Anyone(HAS (Plant, Steel) OR Heat, NOT Player1)"
  }

  @Test
  internal fun `L1-3 a refinement may appear inside an argument`() {
    parse<Expression>("Marker<Player(NOT Player1)>").toString() shouldBe
        "Marker<Player(NOT Player1)>"
  }

  // L1-4 Class literals

  @Test
  internal fun `L1-4 a class literal wraps one bare class name`() {
    parse<Expression>("Class<Steel>") shouldBe cn("Steel").classExpression()
    parse<Expression>("Class<Component>") shouldBe COMPONENT.classExpression()
    parse<Expression>("Class<Class>") shouldBe CLASS.classExpression()
    parse<Expression>("Production<Class<Steel>>").arguments shouldContainExactly
        listOf(cn("Steel").classExpression())
  }

  // L1-5 This

  @Test
  internal fun `L1-5 This is an expression and may take arguments`() {
    parse<Expression>("This").className shouldBe cn("This")
    parse<Expression>("This<Player1>").toString() shouldBe "This<Player1>"
    parse<Expression>("Marker<This>").arguments shouldContainExactly listOf(parse("This"))
  }

  @Test
  internal fun `L1-5 an empty argument list on This accepts nothing`() {
    val bound = Transforming.replaceThisExpressionsWith(parse("Ants<Player1>"))

    bound.transformExpression(parse("This<>")) shouldBe parse<Expression>("Ants<Player1>")
    bound.transformExpression(parse("This")) shouldBe parse<Expression>("Ants<Player1>")
    bound.transformExpression(parse("This<Steel>")) shouldBe parse<Expression>("Ants<Steel>")
  }

  // L1-6 Anyone

  @Test
  internal fun `L1-6 Anyone and Owner are ordinary expressions here`() {
    parse<Expression>("Plant<Owner>").arguments shouldContainExactly listOf(parse("Owner"))
    parse<Expression>("Plant<Anyone>").arguments shouldContainExactly listOf(parse("Anyone"))
  }

  // L1-7 Type-variable markers

  @Test
  internal fun `L1-7 a marker precedes its complete bound expression`() {
    roundTripAll<Expression>(
        """
        Class<@Component>(HAS @Component)
        Class<C@Component>(HAS C@Component)
        Class<@Component>(HAS @Component<Anyone>)
        """
    )
  }

  @Test
  internal fun `L1-7 the former suffix and numeric marker forms are rejected`() {
    shouldThrow<PetSyntaxException> {
      parse<Expression>("Class<Component^C>(HAS Component^C)")
    }
    shouldThrow<PetSyntaxException> {
      parse<Expression>("Class<1@Component>(HAS 1@Component)")
    }
  }

  @Test
  internal fun `L1-7 a named reference can omit its supplied type`() {
    parse<InstructionTree>("EACH Me@Player { Plant<Me@> }") shouldBe
        parse<InstructionTree>("EACH Me@Player { Plant<Me@Player> }")
    parse<Expression>("Class<Card@CardFront>(HAS Card@)") shouldBe
        parse<Expression>("Class<Card@CardFront>(HAS Card@CardFront)")
  }

  @Test
  internal fun `L1-7 an inner selector supplies the short name in its own scope`() {
    Parsing.parseClasses(
        "ABSTRACT CLASS Rule<Me@Person> { This: Token<Me@>, EACH Me@Area { Tile<Me@> } }"
    ) shouldBe
        Parsing.parseClasses(
            "ABSTRACT CLASS Rule<Me@Person> { This: Token<Me@Person>, EACH Me@Area { Tile<Me@Area> } }"
        )
  }

  @Test
  internal fun `L1-7 effect references can precede their supplying occurrence`() {
    parse<Effect>("-Plant<Victim@Owner(NOT Attacker@)> BY Attacker@Player: Plant<Victim@>") shouldBe
        parse<Effect>(
            "-Plant<Victim@Owner(NOT Attacker@Player)> BY Attacker@Player: Plant<Victim@Owner>"
        )
  }

  @Test
  internal fun `L1-7 short references share a choice across sequence stages`() {
    parse<InstructionTree>("Tile<Chosen@Area> THEN Marker<Chosen@>") shouldBe
        parse<InstructionTree>("Tile<Chosen@Area> THEN Marker<Chosen@Area>")
  }

  @Test
  internal fun `L1-7 a non-observing short reference cannot precede its sequence supplier`() {
    shouldThrow<PetSyntaxException> {
          parse<InstructionTree>("Tile<Chosen@> THEN Marker<Chosen@Area>")
        }
        .message shouldContain "`Chosen@` precedes its supplying occurrence"
  }

  @Test
  internal fun `L1-7 an observing short reference can precede its sequence supplier`() {
    parse<InstructionTree>("Tile<Area(NOT Chosen@)> THEN Marker<Chosen@Area>") shouldBe
        parse<InstructionTree>("Tile<Area(NOT Chosen@Area)> THEN Marker<Chosen@Area>")
  }

  @Test
  internal fun `L1-7 a gated sequence keeps the supplying type outside observations`() {
    parse<InstructionTree>("(Tag<First@>: Copy<First@Card>) THEN Copy<Card(NOT First@)>") shouldBe
        parse<InstructionTree>(
            "(Tag<First@Card>: Copy<First@Card>) THEN Copy<Card(NOT First@Card)>"
        )
  }

  @Test
  internal fun `L1-7 transmutations can refer to the removed type by short name`() {
    parse<InstructionTree>("Tile(NOT Source@) FROM Source@Tile") shouldBe
        parse<InstructionTree>("Tile(NOT Source@Tile) FROM Source@Tile")
  }

  @Test
  internal fun `L1-7 a short name needs one supplying type`() {
    shouldThrow<PetSyntaxException> { parse<InstructionTree>("Plant<Me@>") }
    shouldThrow<PetSyntaxException> { parse<InstructionTree>("EACH Me@ { Plant }") }
    shouldThrow<PetSyntaxException> {
          Parsing.parseClasses(
              "ABSTRACT CLASS Rule<Chosen@Person, Chosen@Area> { This: Marker<Chosen@> }"
          )
        }
        .message!!
        .contains("ambiguous") shouldBe true
  }

  // L1-8 Equality of expressions

  @Test
  internal fun `L1-8 two expressions are equal when their spellings agree`() {
    parse<Expression>(" Marker < Mars1 , Player1 > ") shouldBe
        cn("Marker").of(cn("Mars1"), cn("Player1"))
    parse<Expression>("Marker<Player1, Mars1>") shouldNotBe
        parse<Expression>("Marker<Mars1, Player1>")
    langTable.resolve(parse("Marker<Player1, Mars1>")) shouldBe
        langTable.resolve(parse("Marker<Mars1, Player1>"))
  }

  @Test
  internal fun `L1-8 refinement clause order and duplication do not affect equality`() {
    val reorderedWithDuplicate = parse<Expression>("Plant(NOT Heat, HAS Steel, HAS Steel)")

    parse<Expression>("Plant(HAS Steel, NOT Heat)") shouldBe reorderedWithDuplicate
    "$reorderedWithDuplicate" shouldBe "Plant(NOT Heat, HAS Steel)"
  }

  // L1-9 Rendering

  @Test
  internal fun `L1-9 an expression renders exactly as authored`() {
    roundTripAll<Expression>(
        """
        Plant
        Plant<>
        Plant<Player1>
        Plant<Player1, Ants>
        Plant<Ants<Player1>, Steel>
        Plant(HAS Steel)
        Plant(HAS MAX 0 Steel)
        Plant<Player1>(HAS Steel, HAS 2 Heat)
        Plant(HAS (Steel, Heat) OR Microbe, NOT Animal)
        Plant(NOT Steel, NOT Heat)
        Class<Plant>(HAS Plant<Player1>)
        Has<By, Max>
        Plant(NOT Steel)
        Plant<Steel(NOT Heat)>
        A_foo
        """
    )
  }

  @Test
  internal fun `L1-9 a represented-Class reference preserves its explicit empty arguments`() {
    val expression = parse<Expression>("Class<@Component>(HAS @Component<>)")

    expression.toString() shouldBe "Class<@Component>(HAS @Component<>)"
    parse<Expression>(expression.toString()) shouldBe expression
  }

  @Test
  internal fun `L1-9 reference equality includes whether arguments were authored`() {
    val structural = cn("Component").of(cn("Anyone"))
    val bare =
        structural.copy(typeVariableName = Reference(null, cn("Component"), false, resolved = true))
    val applied =
        structural.copy(typeVariableName = Reference(null, cn("Component"), true, resolved = true))

    bare shouldNotBe applied
    bare.toString() shouldBe "@Component"
    applied.toString() shouldBe "@Component<Anyone>"
  }

  @Test
  internal fun `L1-9 an expression built through the API renders the same way`() {
    cn("Aa")
        .of(
            cn("Bb").expression,
            cn("Cc").of(cn("Dd")),
            cn("Ee").of(cn("Ff").of(cn("Gg"), cn("Hh")), cn("Me").expression),
            cn("Jj").expression,
        )
        .toString() shouldBe "Aa<Bb, Cc<Dd>, Ee<Ff<Gg, Hh>, Me>, Jj>"
  }

  @Test
  internal fun `L1-9 an authored expression is not rewritten into a canonical form`() {
    val bare = parse<Expression>("Tile")
    val explicit = parse<Expression>("Tile<Area>")

    bare.toString() shouldBe "Tile"
    explicit.toString() shouldBe "Tile<Area>"
    bare shouldNotBe explicit
    langTable.resolve(bare) shouldBe langTable.resolve(explicit)
  }
}
