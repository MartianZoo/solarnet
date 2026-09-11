package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.Refinement
import dev.martianzoo.pets.ast.Expression.Refinement.And
import dev.martianzoo.pets.ast.Expression.Refinement.Has
import dev.martianzoo.pets.ast.Expression.Refinement.Not
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

/** Section 3 of `docs/pets-language-spec.md`: how an expression is written. */
internal class Lang03ExpressionsTest {

  // L3-1 The shape of an expression

  @Test
  internal fun `L3-1 a class name, optional arguments, optional refinement`() {
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
  internal fun `L3-1 arguments are themselves expressions, to any depth`() {
    parse<Expression>("Aa<Bb<Cc<Dd, Ee, Ff<Gg<Hh<Me>>, Jj>>, Kk>>").toString() shouldBe
        "Aa<Bb<Cc<Dd, Ee, Ff<Gg<Hh<Me>>, Jj>>, Kk>>"
  }

  // L3-2 The empty argument list

  @Test
  internal fun `L3-2 an empty argument list is a different spelling from none`() {
    val empty = parse<Expression>("OceanTile<>")
    val absent = parse<Expression>("OceanTile")

    empty.argumentsSpecified shouldBe true
    absent.argumentsSpecified shouldBe false
    empty.arguments shouldBe absent.arguments
    empty.toString() shouldBe "OceanTile<>"
    empty shouldNotBe absent
  }

  // L3-3 Refinements

  @Test
  internal fun `L3-3 there are two kinds of refinement clause`() {
    (parse<Refinement>("(HAS Plant)") as Has).requirement shouldBe parse("Plant")
    (parse<Refinement>("(NOT Player1)") as Not).excluded shouldBe parse("Player1")
    parse<Expression>("Owner(NOT Player1)").toString() shouldBe "Owner(NOT Player1)"
  }

  @Test
  internal fun `L3-3 a refinement conjoins its clauses`() {
    val refinement = parse<Refinement>("(HAS Plant, NOT Player1)") as And
    refinement.refinements.map { it::class } shouldBe listOf(Has::class, Not::class)

    shouldThrow<PetSyntaxException> { parse<Expression>("Owner(NOT Player1)(HAS Plant)") }
  }

  @Test
  internal fun `L3-3 a top-level comma separates clauses`() {
    shouldThrow<PetSyntaxException> { parse<Expression>("Owner(HAS Plant, Steel)") }
    parse<Expression>("Owner(HAS (Plant, Steel) OR Heat, NOT Player1)").toString() shouldBe
        "Owner(HAS (Plant, Steel) OR Heat, NOT Player1)"
  }

  @Test
  internal fun `L3-3 a refinement may appear inside an argument`() {
    parse<Expression>("Marker<Player(NOT Player1)>").toString() shouldBe
        "Marker<Player(NOT Player1)>"
  }

  // L3-4 Class literals

  @Test
  internal fun `L3-4 a class literal wraps one bare class name`() {
    parse<Expression>("Class<Steel>") shouldBe cn("Steel").classExpression()
    parse<Expression>("Class<Component>") shouldBe COMPONENT.classExpression()
    parse<Expression>("Class<Class>") shouldBe CLASS.classExpression()
    parse<Expression>("Production<Class<Steel>>").arguments shouldContainExactly
        listOf(cn("Steel").classExpression())
  }

  // L3-5 This

  @Test
  internal fun `L3-5 This is an expression and may take arguments`() {
    parse<Expression>("This").className shouldBe cn("This")
    parse<Expression>("This<Player1>").toString() shouldBe "This<Player1>"
    parse<Expression>("Marker<This>").arguments shouldContainExactly listOf(parse("This"))
  }

  @Test
  internal fun `L3-5 an empty argument list on This accepts nothing`() {
    val bound = Transforming.replaceThisExpressionsWith(parse("Ants<Player1>"))

    bound.transformExpression(parse("This<>")) shouldBe parse<Expression>("Ants<Player1>")
    bound.transformExpression(parse("This")) shouldBe parse<Expression>("Ants<Player1>")
    bound.transformExpression(parse("This<Steel>")) shouldBe parse<Expression>("Ants<Steel>")
  }

  // L3-6 Owner

  @Test
  internal fun `L3-6 Owner and Anyone are ordinary expressions here`() {
    parse<Expression>("Plant<Owner>").arguments shouldContainExactly listOf(parse("Owner"))
    parse<Expression>("Plant<Anyone>").arguments shouldContainExactly listOf(parse("Anyone"))
  }

  // L3-7 Rendering

  @Test
  internal fun `L3-7 an expression renders exactly as authored`() {
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
  internal fun `L3-7 an expression built through the API renders the same way`() {
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
  internal fun `L3-7 an authored expression is not rewritten into a canonical form`() {
    val bare = parse<Expression>("Tile")
    val explicit = parse<Expression>("Tile<Area>")

    bare.toString() shouldBe "Tile"
    explicit.toString() shouldBe "Tile<Area>"
    bare shouldNotBe explicit
    langTable.resolve(bare) shouldBe langTable.resolve(explicit)
  }

  // L3-8 Equality of expressions

  @Test
  internal fun `L3-8 two expressions are equal when their spellings agree`() {
    parse<Expression>(" Marker < Mars1 , Player1 > ") shouldBe
        cn("Marker").of(cn("Mars1"), cn("Player1"))
    parse<Expression>("Marker<Player1, Mars1>") shouldNotBe
        parse<Expression>("Marker<Mars1, Player1>")
    langTable.resolve(parse("Marker<Player1, Mars1>")) shouldBe
        langTable.resolve(parse("Marker<Mars1, Player1>"))
  }

  @Test
  internal fun `L3-8 refinement clause order and duplication do not affect equality`() {
    val reorderedWithDuplicate = parse<Expression>("Plant(NOT Heat, HAS Steel, HAS Steel)")

    parse<Expression>("Plant(HAS Steel, NOT Heat)") shouldBe reorderedWithDuplicate
    "$reorderedWithDuplicate" shouldBe "Plant(NOT Heat, HAS Steel)"
  }
}
