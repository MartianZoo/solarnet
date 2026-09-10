package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.gameView
import dev.martianzoo.pets.types.testCatalog
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Section 12 of `docs/pets-language-spec.md`: filling in what a physical game leaves implicit.
 * Every elaboration here happens in Player1's context, against the declarations in
 * `langTestSupport.kt`.
 */
internal class Lang12ElaborationTest {

  private fun metric(source: String, context: String = "This"): Metric =
      langElaborator.elaborateMetricInput(parse(source), langVocabulary, parse(context), player1)

  private fun classEffects(className: String): List<Effect> =
      langElaborator.classEffects(langTable.getClass(parse(className)))

  // L12-1 The stages

  @Test
  internal fun `L12-1 a submitted element goes through the submitted pipeline's stages`() {
    val vocabulary =
        Vocabulary.create(
            langCatalog,
            activeClassNames = langTable.allClassNames,
            inputOnlySynonyms = listOf("Chip" to "ProjectCard"),
        )
    val submitted =
        langElaborator.elaborateInput(
            parse<InstructionTree>("2 Chip, UNWRAP[Tile<>]"),
            vocabulary,
            player1,
        )

    // An input-only synonym became the canonical name; the atomized gain split; the gain default
    // and the all-use owner default were inserted; contextual `Owner` was bound; and the marked
    // syntax was dispatched away.
    submitted shouldBe
        parse<InstructionTree>(
            "ProjectCard<Player1>!, ProjectCard<Player1>!, Tile<Player1, LandArea>!"
        )

    // Type-variable inference ran too, on a shape that records one.
    val sequence = elaborate("Token THEN Token") as Instruction.Then
    sequence.typeVariables.isEmpty shouldBe false
  }

  @Test
  internal fun `L12-1 a class's own effects go through a different set of stages`() {
    // No session vocabulary and no contextual owner: an ownerless class keeps `Owner` open and
    // takes it from the event instead (L12-13). Defaults and atomizing still run.
    classEffects("SimpleRule").single() shouldBe
        parse<Effect>("This BY Owner: ProjectCard<Owner>!, ProjectCard<Owner>!, Plant<Owner>!")
  }

  // L12-2 This

  @Test
  internal fun `L12-2 This is replaced by the context expression`() {
    val bound = Transforming.replaceThisExpressionsWith(parse("It<Worked>"))

    bound.transformInstructionTree(parse("Plant<This>")).toString() shouldBe "Plant<It<Worked>>"
    bound.transformInstructionTree(parse("Class<This>")).toString() shouldBe "Class<It>"
    bound.transformInstructionTree(parse("This<Plant>")).toString() shouldBe "It<Plant>"
    bound
        .transformEffect(parse("-Ooh<Plant<Xyz, This, Gizmo>>: 5 This?, =0 This: -Widget"))
        .toString() shouldBe
        "-Ooh<Plant<Xyz, It<Worked>, Gizmo>>: 5 It<Worked>?, =0 It<Worked>: -Widget"
  }

  // L12-3 Owner

  @Test
  internal fun `L12-3 Owner is replaced by the context owner`() {
    elaborate("Plant<Owner>") shouldBe parse<InstructionTree>("Plant<Player1>!")
    elaborate("Plant") shouldBe parse<InstructionTree>("Plant<Player1>!")
    elaborate("Plant<Anyone>") shouldBe parse<InstructionTree>("Plant<Anyone>!")
  }

  @Test
  internal fun `L12-3 an owner-selecting fanout shields its body but not its selector`() {
    elaborate("EACH Player { Plant<Owner> }") shouldBe
        parse<InstructionTree>("EACH Player { Plant<Owner>! }")
    elaborate("EACH Area { Plant<Owner> }") shouldBe
        parse<InstructionTree>("EACH Area { Plant<Player1>! }")
    elaborate("EACH Token<Owner> { Plant<Owner> }") shouldBe
        parse<InstructionTree>("EACH Token<Player1> { Plant<Player1>! }")
  }

  @Test
  internal fun `L12-3 a RANK selector shields nothing`() {
    metric("RANK Player { Plant<Player> }") shouldBe parse<Metric>("RANK Player { Plant<Player> }")
    metric("RANK Player { Plant<Owner> }") shouldBe parse<Metric>("RANK Player { Plant<Player1> }")
  }

  // L12-4 All-use defaults

  @Test
  internal fun `L12-4 every expression receives its class's all-use defaults`() {
    // `Owned` declares `DEFAULT Owned<Owner>`, so every owned expression gets an owner, at depth.
    metric("Plant") shouldBe parse<Metric>("Plant<Player1>")
    metric("Marker<Land1>") shouldBe parse<Metric>("Marker<Player1, Land1>")
    metric("Area(HAS Marker<Land1>)") shouldBe parse<Metric>("Area(HAS Marker<Player1, Land1>)")
  }

  // L12-5 Use-specific defaults, and opting in

  @Test
  internal fun `L12-5 a gain receives its gain defaults, and must opt in to them`() {
    elaborate("Tile<>") shouldBe parse<InstructionTree>("Tile<Player1, LandArea>!")
    elaborate("Tile<Mars1>") shouldBe parse<InstructionTree>("Tile<Player1, Mars1>!")
    shouldThrow<PetSyntaxException> { elaborate("Tile") }
  }

  // L12-6 A removal declines by writing nothing

  @Test
  internal fun `L12-6 a removal with no argument list declines its removal-only defaults`() {
    // `Marker` declares `DEFAULT -Marker<LandArea>`; only the all-use owner default lands here.
    elaborate("-Marker") shouldBe parse<InstructionTree>("-Marker<Player1>!")
    elaborate("-Marker<>") shouldBe parse<InstructionTree>("-Marker<Player1, LandArea>!")
    elaborate("-Marker<Mars1>") shouldBe parse<InstructionTree>("-Marker<Player1, Mars1>!")
  }

  // L12-7 An empty list is an acceptance

  @Test
  internal fun `L12-7 an empty argument list is invalid where there is nothing to accept`() {
    shouldThrow<PetSyntaxException> { elaborate("Plant<>") }
    shouldThrow<PetSyntaxException> { elaborate("-Plant<>") }
    shouldThrow<PetSyntaxException> { elaborate("Ok<>") }
  }

  // L12-8 Transmutation halves

  @Test
  internal fun `L12-8 the two halves of a transmutation are defaulted independently`() {
    elaborate("Tile<> FROM Marker") shouldBe
        parse<InstructionTree>("Tile<Player1, LandArea> FROM Marker<Player1>!")
    shouldThrow<PetSyntaxException> { elaborate("Tile FROM Marker") }
  }

  @Test
  internal fun `L12-8 the two default quantifiers are intersected, the stricter winning`() {
    // `Chit` defaults its gain to `?`; `Slug` defaults its removal to `.`; `Plant` inherits `!`.
    elaborate("Chit") shouldBe parse<InstructionTree>("Chit<Player1>?")
    elaborate("-Slug") shouldBe parse<InstructionTree>("-Slug<Player1>.")

    elaborate("Chit FROM Slug") shouldBe parse<InstructionTree>("Chit<Player1> FROM Slug<Player1>.")
    elaborate("Chit FROM Plant") shouldBe
        parse<InstructionTree>("Chit<Player1> FROM Plant<Player1>!")
    elaborate("Chit FROM Slug!") shouldBe
        parse<InstructionTree>("Chit<Player1> FROM Slug<Player1>!")
  }

  // L12-9 Reserving a slot for the refinement candidate

  @Test
  internal fun `L12-9 a bare dependent expression in a HAS refinement reserves a slot`() {
    metric("Player(HAS StartToken)") shouldBe parse<Metric>("Player(HAS StartToken)")
    metric("Player(HAS StartToken<Owner>)") shouldBe
        parse<Metric>("Player(HAS StartToken<Player1>)")
    metric("Player(HAS StartToken<>)") shouldBe parse<Metric>("Player(HAS StartToken<Player1>)")
    metric("StartToken") shouldBe parse<Metric>("StartToken<Player1>")
  }

  // L12-10 Deferring a class-header variable's default

  @Test
  internal fun `L12-10 a default is deferred inside a refinement for a header variable`() {
    metric("Animal") shouldBe parse<Metric>("Animal<Player1>")
    metric("CardFront(HAS Animal)") shouldBe parse<Metric>("CardFront<Player1>(HAS Animal)")
    metric("CardFront(HAS Animal<>)") shouldBe
        parse<Metric>("CardFront<Player1>(HAS Animal<Player1>)")
  }

  // L12-11 Atomized gains

  @Test
  internal fun `L12-11 a gain of several Atomized components becomes several gains of one`() {
    elaborate("3 ProjectCard") shouldBe
        parse<InstructionTree>(
            "ProjectCard<Player1>!, ProjectCard<Player1>!, ProjectCard<Player1>!"
        )
    elaborate("ProjectCard") shouldBe parse<InstructionTree>("ProjectCard<Player1>!")
    elaborate("-3 ProjectCard") shouldBe parse<InstructionTree>("-3 ProjectCard<Player1>!")
  }

  // L12-12 EVAL

  @Test
  internal fun `L12-12 EVAL includes a class property's own syntax`() {
    classEffects("Gardener") shouldBe
        listOf(parse<Effect>("This BY Owner: Plant<Owner>! / 2 Plant<Owner>"))
  }

  @Test
  internal fun `L12-12 EVAL needs a receiver context, so an ordinary instruction rejects it`() {
    shouldThrow<PetSyntaxException> { elaborate("Plant / EVAL Gardener.score") }
    metric("EVAL Gardener.score") shouldBe parse<Metric>("2 Plant<Player1>")
  }

  @Test
  internal fun `L12-12 an evaluation stays unexpanded while its receiver is abstract`() {
    // `Scored.score` is only a bound, so nothing can be substituted for it yet.
    classEffects("SimpleRule").single() shouldBe
        parse<Effect>("This BY Owner: ProjectCard<Owner>!, ProjectCard<Owner>!, Plant<Owner>!")
  }

  // L12-13 Class effects

  @Test
  internal fun `L12-13 effects are gathered from every superclass and elaborated in context`() {
    classEffects("SimpleRule").size shouldBe 1
    classEffects("OwnedRule").single() shouldBe parse<Effect>("This: Plant<Owner>!")
  }

  @Test
  internal fun `L12-13 an unowned class whose result needs an owner gets BY Owner`() {
    // `Rule` is neither an `Owner` nor `Owned`, so its instruction has no owner of its own.
    classEffects("SimpleRule").single().trigger.toString() shouldBe "This BY Owner"
    classEffects("OwnedRule").single().trigger.toString() shouldBe "This"
  }

  // L12-14 Inactive types

  @Test
  internal fun `L12-14 a change to a type this game cannot hold becomes Die or Ok`() {
    val catalog =
        testCatalog(
            """
            ABSTRACT CLASS Seat : Owner, Actor { CLASS Seat1 }
            CLASS Plant : Owned<Anyone>
            CLASS Steel : Owned<Anyone>
            """
                .trimIndent()
        )
    val view: ClassTable = gameView(catalog, "Seat1", "Plant")
    val elaborator = PetElaborator(view)
    val bearer = view.getClass(parse("Plant")).defaultType
    val seat1 = Player(parse("Seat1"))

    view.isActive(cn("Plant")) shouldBe true
    view.isActive(cn("Steel")) shouldBe false

    fun specialize(effect: String): Effect =
        elaborator.specializeEffect(bearer, bearer, parse(effect), parse("Plant"), seat1)

    specialize("This: Steel<Seat1>!").instruction shouldBe parse<InstructionTree>("Die!")
    specialize("This: Steel<Seat1>?").instruction shouldBe parse<InstructionTree>("Ok")
    specialize("This: Plant<Seat1>!").instruction shouldBe parse<InstructionTree>("Plant<Seat1>!")
  }

  // L12-15 Specializing an effect

  @Test
  internal fun `L12-15 specializing closes an effect over one exact component`() {
    val ruleClass = langTable.getClass(parse("OwnedRule"))
    val component = langTable.resolve(parse<Expression>("OwnedRule<Player2>"))
    val authored = classEffects("OwnedRule").single()

    langElaborator.specializeEffect(
        ruleClass.defaultType,
        component,
        authored,
        component.expressionFull,
        Player(parse("Player2")),
    ) shouldBe parse<Effect>("This: Plant<Player2>!")
  }
}
