package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.FromExpression.Compact
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.gameView
import dev.martianzoo.pets.types.loadTypes
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

/**
 * Section 9 of `docs/pets-language-spec.md`: filling in what a physical game leaves implicit. Every
 * elaboration here happens in Player1's context, against the declarations in `langTestSupport.kt`.
 */
internal class Lang09ElaborationTest {

  private fun metric(source: String, context: String = "This"): Metric =
      langElaborator.elaborateMetricInput(parse("OWN[$source]"), parse(context), player1)

  private fun classEffects(className: String): List<Effect> =
      langElaborator.classEffects(langTable.getClass(parse(className)))

  private fun refinementMetric(source: String): Metric {
    val table =
        loadTypes(
            "ABSTRACT CLASS Area { CLASS Land1 }",
            "ABSTRACT CLASS Adjacency<Area, Area>",
            "CLASS Marker<Area> { DEFAULT Marker<Land1> }",
            "CLASS Neighbor<Marker, Area>",
        )
    return PetElaborator(table).elaborateMetricInput(parse(source), parse("Area"))
  }

  @Test
  internal fun `L9-13 an ownerless System trigger cannot infer a Player actor`() {
    listOf("Phase", "This").forEach { trigger ->
      val table =
          loadTypes(
              "ABSTRACT CLASS Player : Owner, Actor",
              "CLASS Player1 : Player",
              "CLASS Player2 : Player",
              "CLASS Plant : Owned<Player>",
              "CLASS Phase : System",
              "CLASS Rule : System { OWN[$trigger: Plant] }",
          )
      shouldThrow<InvalidPetDefinitionException> {
            PetElaborator(table).classEffects(table.getClass(cn("Rule")))
          }
          .message
          .orEmpty() shouldContain "a System trigger cannot implicitly supply a Player"
    }
  }

  @Test
  internal fun `L9-1 scopes follow defaults without making references request defaults`() {
    val table =
        loadTypes(
            """
      ABSTRACT CLASS Area {
        CLASS First
        CLASS Second
      }
      CLASS Piece<Area> { DEFAULT +Piece<First>; DEFAULT -Piece<Second> }
      CLASS Marker
      CLASS Notice<Piece<Area>>
    """
        )
    val elaborator = PetElaborator(table)
    val cases =
        listOf(
            Triple(
                "@Piece<> THEN Notice<@Piece>",
                "First",
                "Piece<First>! THEN Notice<Piece<First>>!",
            ),
            Triple(
                "Notice<Piece<First>>(HAS @Piece) THEN @Piece<>",
                "First",
                "Notice<Piece<First>>(HAS Piece<First>)! THEN Piece<First>!",
            ),
            Triple(
                "-@Piece<> THEN Notice<@Piece>",
                "Second",
                "-Piece<Second>! THEN Notice<Piece<Second>>!",
            ),
            Triple(
                "@Piece<> FROM Marker THEN Notice<@Piece>",
                "First",
                "Piece<First> FROM Marker! THEN Notice<Piece<First>>!",
            ),
        )
    cases.forEach { (source, area, expected) ->
      val lowered = elaborator.elaborateInput(parse<InstructionTree>(source))
      val variable = lowered.typeVariables.variables.single()
      val binding =
          lowered.typeVariables.bind(
              mapOf(variable to table.resolve(parse<Expression>("Piece<$area>"))),
              table,
          )
      binding.transformInstructionTree(lowered) shouldBe parse<InstructionTree>(expected)
    }
  }

  // L9-1 The stages

  @Test
  internal fun `L9-1 a submitted element goes through the submitted pipeline's stages`() {
    val submitted =
        langElaborator.elaborateInput(
            parse<InstructionTree>("OWN[2 ProjectCard, UNWRAP[Tile<>]]"),
            player1,
        )

    // The atomized gain split; the gain default and the owned context were inserted; and the
    // marked syntax was dispatched away.
    submitted shouldBe
        parse<InstructionTree>(
            "ProjectCard<Player1>!, ProjectCard<Player1>!, Tile<Player1, LandArea>!"
        )

    // Scope recording precedes the later elaboration stages.
    val sequence = elaborate("@Token THEN @Token") as Instruction.Then
    sequence.typeVariables.isEmpty shouldBe false
  }

  @Test
  internal fun `L9-1 a class's own effects go through a different set of stages`() {
    // An ownerless class binds `Me` from the trigger. Defaults and atomizing still run.
    classEffects("SimpleRule").single() shouldBe
        parse<Effect>(
            "This BY Me@Player: ProjectCard<Me@Player>!, ProjectCard<Me@Player>!, Plant<Me@Player>!"
        )
  }

  // L9-2 This

  @Test
  internal fun `L9-2 This is replaced by the context expression`() {
    val bound = Transforming.replaceThisExpressionsWith(parse("It<Worked>"))

    bound.transformInstructionTree(parse("Plant<This>")).toString() shouldBe "Plant<It<Worked>>"
    bound.transformInstructionTree(parse("Class<This>")).toString() shouldBe "Class<It>"
    bound.transformInstructionTree(parse("This<Plant>")).toString() shouldBe "It<Plant>"
    bound
        .transformEffect(parse("-Ooh<Plant<Xyz, This, Gizmo>>: 5 This?, =0 This: -Widget"))
        .toString() shouldBe
        "-Ooh<Plant<Xyz, It<Worked>, Gizmo>>: 5 It<Worked>?, =0 It<Worked>: -Widget"
  }

  // L9-3 Owned context

  @Test
  internal fun `L9-3 bare Owned takes the supplied context and Anyone stays literal`() {
    elaborate("Plant") shouldBe parse<InstructionTree>("Plant<Player1>!")
    elaborate("Plant<Anyone>") shouldBe parse<InstructionTree>("Plant<Anyone>!")
  }

  @Test
  internal fun `L9-3 a class literal's predicate takes the lexical owner`() {
    metric("Class<@Token>(HAS @Token)") shouldBe metric("Class<@Token>(HAS @Token<Player1>)")
    metric("Class<@Token>(HAS @Token<Anyone>)") shouldBe
        parse<Metric>("Class<@Token>(HAS @Token<Anyone>)")
  }

  @Test
  internal fun `L9-3 a class literal in a rank uses each candidate's owner`() {
    metric("RANK Me@Player { Class<@Token>(HAS @Token) }") shouldBe
        metric("RANK Me@Player { Class<@Token>(HAS @Token<Me@Player>) }")
  }

  @Test
  internal fun `L9-3 a represented class in an EACH body takes the lexical owner`() {
    elaborate("EACH Class<@Token> { @Token }") shouldBe
        elaborate("EACH Class<@Token> { @Token<Player1> }")
    elaborate("EACH Class<@Token> { @Token<Anyone> }") shouldBe
        parse<InstructionTree>("EACH Class<@Token> { @Token<Anyone>! }")
  }

  @Test
  internal fun `L9-3 an ownerless rule supplies an owner for a class literal predicate`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Player : Owner",
            "ABSTRACT CLASS Token : Owned",
            "CLASS Counted",
            "CLASS Rule { OWN[This: Counted / Class<@Token>(HAS @Token)] }",
        )
    PetElaborator(table).classEffects(table.getClass(cn("Rule"))).single() shouldBe
        parse<Effect>("This BY Me@Player: Counted! / Class<@Token>(HAS @Token<Me@Player>)")
  }

  @Test
  internal fun `L9-3 an explicitly named selector rebinds Me in its body`() {
    elaborate("EACH Me@Player { Plant }") shouldBe
        parse<InstructionTree>("EACH Me@Player { Plant<Me@Player>! }")
    elaborate("EACH Area { Plant }") shouldBe
        parse<InstructionTree>("EACH Area { Plant<Player1>! }")
    elaborate("EACH Token<Anyone> { Plant }") shouldBe
        parse<InstructionTree>("EACH Token<Anyone> { Plant<Player1>! }")
  }

  @Test
  internal fun `L9-3 a RANK selector can rebind Me`() {
    metric("RANK Player { Plant }") shouldBe parse<Metric>("RANK Player { Plant<Player1> }")
    metric("RANK Player { Plant<Player> }") shouldBe parse<Metric>("RANK Player { Plant<Player> }")
    metric("RANK Me@Player { Plant }") shouldBe parse<Metric>("RANK Me@Player { Plant<Me@Player> }")
    metric("RANK Player { Plant<Anyone> }") shouldBe parse<Metric>("RANK Player { Plant<Anyone> }")
  }

  @Test
  internal fun `L9-3 EACH and RANK rebind Me in their selector refinements`() {
    val table =
        loadTypes(
            """
      ABSTRACT CLASS Player : Owner, Actor {
        CLASS Player1
        CLASS Player2
      }
      CLASS Plant : Owned<Player>
      CLASS Box<Plant>
    """
        )
    val elaborator = PetElaborator(table)
    val selector = "Me@Player(HAS Box<Plant>)"
    val expected = "Me@Player(HAS Box<Plant<Me@Player>>)"
    elaborator.elaborateInput(
        parse<InstructionTree>("OWN[EACH $selector { Plant }]"),
        player1,
    ) shouldBe parse<InstructionTree>("EACH $expected { Plant<Me@Player>! }")
    elaborator.elaborateMetricInput(
        parse("OWN[RANK $selector { Plant }]"),
        parse("This"),
        player1,
    ) shouldBe parse<Metric>("RANK $expected { Plant<Me@Player> }")
  }

  @Test
  internal fun `L9-12 EVAL cannot inherit a HAS candidate by its position`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Player : Owner",
            "CLASS Token : Owned<Player>",
            "CLASS Rule { requirement = HAS \"Token\"; metric = COUNT \"Token\" }",
        )
    listOf("requirement", "metric").forEach { property ->
      listOf("Player(HAS EVAL Rule.$property)", "OWN[Player(HAS EVAL Rule.$property)]").forEach {
          source ->
        shouldThrow<PetSyntaxException> {
              PetElaborator(table).elaborateMetricInput(parse(source), parse("Rule"))
            }
            .message
            .orEmpty() shouldContain "EVAL cannot supply a HAS candidate's requirement"
      }
    }
  }

  @Test
  internal fun `L8-6 an earlier inner OWN cannot use an outer effect's inferred Me`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Player : Owner, Actor",
            "CLASS Player1 : Player",
            "CLASS Player2 : Player",
            "CLASS Plant : Owned<Player>",
            "CLASS Pulse",
            "CLASS Rule { OWN[Pulse: UNWRAP[OWN[Plant]]] }",
            transformHandlerFactories =
                mapOf("UNWRAP" to { TransformHandler { inner, _ -> inner } }),
        )
    shouldThrow<InvalidPetDefinitionException> {
          PetElaborator(table).classEffects(table.getClass(cn("Rule")))
        }
        .message
        .orEmpty() shouldContain "remove a redundant inner OWN or bind Me explicitly"
  }

  // L9-4 All-use defaults

  @Test
  internal fun `L9-4 every expression receives its class's all-use defaults`() {
    // Bare Owned expressions take the supplied lexical context, at depth.
    metric("Plant") shouldBe parse<Metric>("Plant<Player1>")
    metric("Marker<Land1>") shouldBe parse<Metric>("Marker<Player1, Land1>")
    metric("Area(HAS Marker<Land1>)") shouldBe parse<Metric>("Area(HAS Marker<Player1, Land1>)")
  }

  // L9-5 Use-specific defaults, and opting in

  @Test
  internal fun `L9-5 a gain receives its gain defaults, and must opt in to them`() {
    elaborate("Tile<>") shouldBe parse<InstructionTree>("Tile<Player1, LandArea>!")
    elaborate("Tile<Mars1>") shouldBe parse<InstructionTree>("Tile<Player1, Mars1>!")
    shouldThrow<ExpressionException> { elaborate("Tile") }
  }

  // L9-6 A removal declines by writing nothing

  @Test
  internal fun `L9-6 a removal with no argument list declines its removal-only defaults`() {
    // `Marker` declares `DEFAULT -Marker<LandArea>`; a bare removal declines it.
    elaborate("-Marker") shouldBe parse<InstructionTree>("-Marker<Player1>!")
    elaborate("-Marker<>") shouldBe parse<InstructionTree>("-Marker<Player1, LandArea>!")
    elaborate("-Marker<Mars1>") shouldBe parse<InstructionTree>("-Marker<Player1, Mars1>!")
  }

  // L9-7 An empty list is an acceptance

  @Test
  internal fun `L9-7 an empty argument list is invalid where there is nothing to accept`() {
    shouldThrow<ExpressionException> { elaborate("Plant<>") }
    shouldThrow<ExpressionException> { elaborate("-Plant<>") }
    shouldThrow<ExpressionException> { elaborate("Ok<>") }
  }

  // L9-8 Transmutation projections

  @Test
  internal fun `L9-8 transmutation projections are defaulted independently`() {
    elaborate("Tile<> FROM Marker") shouldBe
        parse<InstructionTree>("Tile<Player1, LandArea> FROM Marker<Player1>!")
    shouldThrow<ExpressionException> { elaborate("Tile FROM Marker") }

    val compact = elaborate("Marker<Mars1 FROM Mars2>") as Transmute
    compact shouldBe parse<Instruction>("Marker<Player1, Mars1 FROM Mars2>!")
    (compact.fromEx is Compact) shouldBe true
    compact.typeVariables.isEmpty shouldBe true
  }

  @Test
  internal fun `L9-8 defaults cannot introduce a second changed compact argument`() {
    val table =
        loadTypes(
            """
                CLASS Player1 : Owner
                CLASS Player2 : Owner
                ABSTRACT CLASS Area {
                  CLASS Land1
                  CLASS Land2
                }
                CLASS Marker<Area> : Owned {
                  DEFAULT +Marker<Land1>
                  DEFAULT -Marker<Land2>
                }
                """
        )

    shouldThrow<PetSyntaxException> {
      PetElaborator(table).elaborateInput(parse<Instruction>("Marker<Player1 FROM Player2>"))
    }
  }

  @Test
  internal fun `L9-8 the two default quantifiers are intersected, the stricter winning`() {
    // `Chit` defaults its gain to `?`; `Slug` defaults its removal to `.`; `Plant` inherits `!`.
    elaborate("Chit") shouldBe parse<InstructionTree>("Chit<Player1>?")
    elaborate("-Slug") shouldBe parse<InstructionTree>("-Slug<Player1>.")

    elaborate("Chit FROM Slug") shouldBe parse<InstructionTree>("Chit<Player1> FROM Slug<Player1>.")
    elaborate("Chit FROM Plant") shouldBe
        parse<InstructionTree>("Chit<Player1> FROM Plant<Player1>!")
    elaborate("Chit FROM Slug!") shouldBe
        parse<InstructionTree>("Chit<Player1> FROM Slug<Player1>!")
  }

  // L9-9 Reserving a slot for the refinement candidate

  @Test
  internal fun `L9-9 a HAS candidate fills a compatible omitted dependency first`() {
    metric("Player(HAS StartToken)") shouldBe parse<Metric>("Player(HAS StartToken)")
    metric("Player(HAS StartToken<Anyone>)") shouldBe
        parse<Metric>("Player(HAS StartToken<Anyone>)")
    shouldThrow<ExpressionException> { metric("Player(HAS StartToken<>)") }
    metric("StartToken") shouldBe parse<Metric>("StartToken<Player1>")
    metric("LandArea(HAS StartToken)") shouldBe parse<Metric>("LandArea(HAS StartToken<Player1>)")
  }

  @Test
  internal fun `L9-9 an exact candidate-compatible written argument is legal`() {
    refinementMetric("Area(HAS Adjacency<Land1>)") shouldBe
        parse<Metric>("Area(HAS Adjacency<Land1>)")
  }

  @Test
  internal fun `L9-9 a broad candidate-compatible written argument is legal`() {
    refinementMetric("Area(HAS Adjacency<Area>)") shouldBe
        parse<Metric>("Area(HAS Adjacency<Area>)")
  }

  @Test
  internal fun `L9-9 a refinement candidate does not enter nested arguments`() {
    refinementMetric("Area(HAS Marker)") shouldBe parse<Metric>("Area(HAS Marker)")
    refinementMetric("Area(HAS Neighbor<Marker>)") shouldBe
        parse<Metric>("Area(HAS Neighbor<Marker<Land1>>)")
  }

  @Test
  internal fun `L9-9 nested owned arguments still receive lexical ownership`() {
    val table =
        loadTypes(
            "CLASS Player1 : Owner",
            "CLASS Leaf : Owned",
            "CLASS Holder<Leaf, Owner>",
        )

    PetElaborator(table)
        .elaborateMetricInput(
            parse("OWN[Owner(HAS Holder<Leaf>)]"),
            parse("Owner"),
            parse<Expression>("Player1"),
        ) shouldBe parse<Metric>("Owner(HAS Holder<Leaf<Player1>>)")
  }

  @Test
  internal fun `L9-9 reserving a candidate slot does not discard another slot's default`() {
    val table =
        loadTypes(
            "CLASS Land1",
            "CLASS Pointer<Component> : Owned { DEFAULT Pointer<Land1> }",
        )

    PetElaborator(table).elaborateMetricInput(parse("Owner(HAS Pointer)"), parse("Owner")) shouldBe
        parse<Metric>("Owner(HAS Pointer<Land1>)")
  }

  // L9-10 Deferring a class-header variable's default

  @Test
  internal fun `L9-10 a nested HAS candidate takes precedence over lexical ownership`() {
    metric("Animal") shouldBe parse<Metric>("Animal<Player1>")
    metric("CardFront(HAS Animal)") shouldBe parse<Metric>("CardFront<Player1>(HAS Animal)")
    shouldThrow<ExpressionException> { metric("CardFront(HAS Animal<>)") }
  }

  // L9-11 Atomized gains

  @Test
  internal fun `L9-11 a gain of several Atomized components becomes several gains of one`() {
    elaborate("3 ProjectCard") shouldBe
        parse<InstructionTree>(
            "ProjectCard<Player1>!, ProjectCard<Player1>!, ProjectCard<Player1>!"
        )
    elaborate("ProjectCard") shouldBe parse<InstructionTree>("ProjectCard<Player1>!")
    elaborate("-3 ProjectCard") shouldBe parse<InstructionTree>("-3 ProjectCard<Player1>!")
  }

  // L9-12 EVAL

  @Test
  internal fun `L9-12 EVAL includes a class property's own syntax`() {
    classEffects("Gardener") shouldBe
        listOf(parse<Effect>("This BY Me@Player: Plant<Me@Player>! / 2 Plant<Me@Player>"))
  }

  @Test
  internal fun `L9-12 EVAL needs a receiver context, so an ordinary instruction rejects it`() {
    shouldThrow<PetSyntaxException> { elaborate("Plant / EVAL Gardener.score") }
    metric("EVAL Gardener.score") shouldBe parse<Metric>("2 Plant<Player1>")
  }

  @Test
  internal fun `L9-12 a class effect retains an evaluation whose property value is still a bound`() {
    // Simplified from Award.metric and Landlord: the selected award supplies the scoring metric.
    val table =
        loadTypes(
            """
                CLASS VictoryPoint
                CLASS OwnedTile
                ABSTRACT CLASS Award {
                  metric = Metric
                  This: VictoryPoint / EVAL This.metric
                }
                CLASS Landlord : Award { metric = COUNT "OwnedTile" }
                """
        )
    val elaborator = PetElaborator(table)

    elaborator.classEffects(table.getClass(cn("Award"))).single() shouldBe
        parse<Effect>("This: VictoryPoint! / EVAL This.metric")
    elaborator.classEffects(table.getClass(cn("Landlord"))).single() shouldBe
        parse<Effect>("This: VictoryPoint! / OwnedTile")

    val error =
        shouldThrow<ExpressionException> {
          elaborator.evaluateProperties(
              parse<InstructionTree>("VictoryPoint / EVAL Award.metric"),
              parse("Award"),
          )
        }
    error.detail shouldContain "property `metric` is not a concrete metric on `Award`"
  }

  @Test
  internal fun `L9-12 an abstract receiver can expand a fixed property independent of This`() {
    val table =
        loadTypes(
            """
            CLASS VictoryPoint
            CLASS OwnedTile
            ABSTRACT CLASS Award {
              metric = COUNT "OwnedTile"
              This: VictoryPoint / EVAL This.metric
            }
            CLASS Landlord : Award
            """
        )

    PetElaborator(table).classEffects(table.getClass(cn("Award"))).single() shouldBe
        parse<Effect>("This: VictoryPoint! / OwnedTile")
  }

  @Test
  internal fun `L9-12 a property using This retains its receiver until specialization`() {
    val table =
        loadTypes(
            """
            CLASS VictoryPoint
            ABSTRACT CLASS ResourceCard {
              metric = COUNT "Stock<This>"
              This: VictoryPoint / EVAL This.metric
            }
            CLASS Ants : ResourceCard
            CLASS Stock<ResourceCard>
            """
        )
    val elaborator = PetElaborator(table)

    elaborator.classEffects(table.getClass(cn("ResourceCard"))).single() shouldBe
        parse<Effect>("This: VictoryPoint! / EVAL This.metric")
    elaborator.classEffects(table.getClass(cn("Ants"))).single() shouldBe
        parse<Effect>("This: VictoryPoint! / Stock<Ants>")
  }

  @Test
  internal fun `L9-12 invalid property evaluations explain the invalid expression`() {
    val table =
        loadTypes(
            """
            CLASS Plant
            CLASS Holder {
              score = 1
              requirement = HAS "Plant"
            }
            CLASS Recursive { score = COUNT "EVAL This.score" }
            """
                .trimIndent()
        )
    val elaborator = PetElaborator(table)

    shouldThrow<ExpressionException> {
      elaborator.evaluateProperties(parse<Metric>("EVAL score"), parse("Holder"))
    }
    shouldThrow<ExpressionException> {
      elaborator.evaluateProperties(parse<Metric>("EVAL Holder.missing"), parse("Holder"))
    }
    shouldThrow<ExpressionException> {
      elaborator.evaluateProperties(parse<Metric>("EVAL Holder.requirement"), parse("Holder"))
    }
    shouldThrow<ExpressionException> {
      elaborator.evaluateProperties(
          parse<InstructionTree>("EVAL Holder.score: Plant"),
          parse("Holder"),
      )
    }
    shouldThrow<ExpressionException> {
      elaborator.evaluateProperties(parse<Metric>("EVAL Recursive.score"), parse("Recursive"))
    }
  }

  // L9-13 Class effects

  @Test
  internal fun `L9-13 effects are gathered from every superclass and elaborated in context`() {
    classEffects("SimpleRule").size shouldBe 1
    classEffects("OwnedRule").single().toString() shouldBe "This: Plant<Me@Owner>!"
  }

  @Test
  internal fun `L9-13 effects are available only for an included class`() {
    val universe = loadTypes("CLASS Included\nCLASS Excluded")
    val table = gameView(universe, "Included")

    shouldThrow<IllegalArgumentException> {
      PetElaborator(table).classEffects(table.getClass(cn("Excluded")))
    }
  }

  @Test
  internal fun `L9-13 an unowned class whose result needs an owner binds Me in the trigger`() {
    // `Rule` is neither an `Anyone` nor `Owned`, so its instruction has no owner of its own.
    classEffects("SimpleRule").single().trigger.toString() shouldBe "This BY Me@Player"
    classEffects("OwnedRule").single().trigger.toString() shouldBe "This"
  }

  // L9-14 Uninhabited Types

  @Test
  internal fun `L9-14 changes to uninhabited Types become Die or Ok`() {
    val universe =
        loadTypes(
            """
            ABSTRACT CLASS Seat : Owner, Actor { CLASS Seat1 }
            ABSTRACT CLASS Empty
            CLASS Plant : Owned
            CLASS Steel : Owned
            """
                .trimIndent()
        )
    val view: ClassTable = gameView(universe, "Seat1", "Empty", "Plant")
    val elaborator = PetElaborator(view)
    val bearer = view.getClass(parse("Plant")).defaultType

    view.isInhabited(cn("Plant")) shouldBe true
    view.isInhabited(cn("Steel")) shouldBe false
    view.isInhabited(cn("Empty")) shouldBe false

    fun specialize(effect: String): Effect =
        elaborator.specializeEffect(bearer, bearer, parse(effect), parse("Plant"))

    specialize("This: Steel<Seat1>!").instruction shouldBe parse<InstructionTree>("Die!")
    specialize("This: Steel<Seat1>?").instruction shouldBe parse<InstructionTree>("Ok")
    specialize("This: Steel<Seat1>.").instruction shouldBe parse<InstructionTree>("Ok")
    specialize("This: Empty!").instruction shouldBe parse<InstructionTree>("Die!")
    specialize("This: Empty?").instruction shouldBe parse<InstructionTree>("Ok")
    specialize("This: Plant<This>!").instruction shouldBe parse<InstructionTree>("Die!")
    specialize("This: Plant<This>?").instruction shouldBe parse<InstructionTree>("Ok")
    specialize("This: Plant<This>.").instruction shouldBe parse<InstructionTree>("Ok")
    specialize("This: Empty? / Plant<Seat1>").instruction shouldBe parse<InstructionTree>("Ok")
    specialize("This: EACH Plant<Seat1> { Empty? }").instruction shouldBe
        parse<InstructionTree>("Ok")
    specialize("This: Plant<Seat1>!").instruction shouldBe parse<InstructionTree>("Plant<Seat1>!")
  }

  @Test
  internal fun `L9-14 a change invalidated by dependency specialization becomes Die`() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS Target
            ABSTRACT CLASS Allowed : Target { CLASS Good }
            CLASS Bad : Target
            CLASS Wrapper<Allowed>
            CLASS Holder<@Target> { This: Good OR Wrapper<@Target> }
            """
                .trimIndent()
        )
    val elaborator = PetElaborator(table)
    val holderClass = table.getClass(parse("Holder"))
    val specific = table.resolve(parse("Holder<Bad>"))
    val authored = elaborator.classEffects(holderClass).single()

    elaborator
        .specializeEffect(
            holderClass.defaultType,
            specific,
            authored,
            specific.expressionFull,
        )
        .instruction shouldBe parse<InstructionTree>("Good! OR Die!")
  }

  // L9-15 Specializing an effect

  @Test
  internal fun `L9-15 specializing closes an effect over one exact component`() {
    val ruleClass = langTable.getClass(parse("OwnedRule"))
    val component = langTable.resolve(parse<Expression>("OwnedRule<Player2>"))
    val authored = classEffects("OwnedRule").single()

    langElaborator.specializeEffect(
        ruleClass.defaultType,
        component,
        authored,
        component.expressionFull,
    ) shouldBe parse<Effect>("This: Plant<Player2>!")
  }
}
