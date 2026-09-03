package dev.martianzoo.tfm.randomcards

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.NumberValue
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.data.ClassDeclaration.ClassKind.CONCRETE
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.testlib.RandomGenerator
import java.io.File
import kotlin.random.Random

/** Generates project-card Pets from weighted rules, using Canon only to validate the result. */
internal class RandomCardGenerator(seed: Int) :
    RandomGenerator<Any>(Registry, { 0.0 }, Random(seed)) {
  internal fun generate(count: Int): List<ClassDeclaration> {
    require(count > 0)
    val declarations =
        (1..count).map { number ->
          val name = cn("RandomProject${number.toString().padStart(3, '0')}")
          generateValidDeclaration(name)
        }
    val catalog = catalogWith(declarations)
    declarations.forEach { declaration -> catalog.card(declaration.className) }
    return declarations
  }

  private fun generateValidDeclaration(name: ClassName): ClassDeclaration {
    repeat(MAX_ATTEMPTS_PER_CARD) {
      try {
        val declaration = makeRandomNode<CardDraft>().toDeclaration(name) ?: return@repeat
        catalogWith(listOf(declaration)).card(name)
        return declaration
      } catch (_: Exception) {
        // Some independently generated combinations are not legal Pets; draw another candidate.
      }
    }
    error("Could not make a valid random card after $MAX_ATTEMPTS_PER_CARD attempts")
  }

  private data class Cost(val value: Int)

  private data class Tag(val className: ClassName)

  private data class CardResource(val className: ClassName)

  private data class StandardResource(val source: String)

  private data class RequirementAtom(val source: String)

  private data class RequirementPart(val value: Requirement)

  private data class PlayRequirement(val value: Requirement?)

  private data class CardSelector(val source: String)

  private data class AreaSelector(val source: String)

  private data class PlayerSelector(val source: String)

  private data class Tags(val values: Set<Tag>) {
    fun effect(event: Boolean): Effect? {
      val names = values.map(Tag::className) + if (event) listOf(EVENT_TAG) else emptyList()
      if (names.isEmpty()) return null
      return parse("This:: ${names.joinToString { "$it<This>" }}")
    }
  }

  private data class MetricPart(val source: String)

  private data class ProductionChange(val source: String)

  private data class InstructionAtom(
      val source: String,
      val resourceTypes: Set<ClassName> = emptySet(),
  )

  private data class InstructionPart(
      val source: String,
      val resourceTypes: Set<ClassName> = emptySet(),
  )

  private data class Immediate(val values: List<InstructionPart>)

  private data class ActionCost(
      val source: String?,
      val resourceTypes: Set<ClassName> = emptySet(),
  )

  private data class ActionPart(
      val value: Action,
      val resourceTypes: Set<ClassName>,
  )

  private data class Actions(val values: List<ActionPart>)

  private data class PersistentTrigger(val source: String)

  private data class PersistentEffect(
      val value: Effect,
      val resourceTypes: Set<ClassName>,
  )

  private data class PersistentEffects(val values: List<PersistentEffect>)

  private data class ScoringEffect(
      val value: Effect,
      val resourceTypes: Set<ClassName>,
  )

  private data class Scoring(val value: ScoringEffect?)

  private data class EventKind(val event: Boolean)

  private data class CardDraft(
      val cost: Cost,
      val requirement: PlayRequirement,
      val tags: Tags,
      val actions: Actions,
      val persistentEffects: PersistentEffects,
      val scoring: Scoring,
      val immediate: Immediate,
      val eventKind: EventKind,
  ) {
    fun toDeclaration(name: ClassName): ClassDeclaration? {
      val immediateEffect =
          immediate.values
              .takeIf { it.isNotEmpty() }
              ?.let { instructions ->
                parse<Effect>("This: ${instructions.joinToString { it.source }}")
              }
      val effects =
          listOfNotNull(tags.effect(event = false), immediateEffect, scoring.value?.value) +
              persistentEffects.values.map(PersistentEffect::value)
      val ruleNodeCount =
          actions.values.sumOf { it.value.descendantCount() } +
              effects.filterNot { it.automatic }.sumOf { it.descendantCount() }
      if (ruleNodeCount < MIN_RULE_NODE_COUNT) return null

      val resourceTypes =
          (immediate.values.flatMap(InstructionPart::resourceTypes) +
                  actions.values.flatMap(ActionPart::resourceTypes) +
                  persistentEffects.values.flatMap(PersistentEffect::resourceTypes) +
                  scoring.value?.resourceTypes.orEmpty())
              .distinct()
      if (resourceTypes.size > 1) return null

      val active = actions.values.isNotEmpty() || persistentEffects.values.isNotEmpty()
      val event = eventKind.event && !active && resourceTypes.isEmpty() && scoring.value == null
      val authoredEffects =
          listOfNotNull(tags.effect(event), immediateEffect, scoring.value?.value) +
              persistentEffects.values.map(PersistentEffect::value)
      val supertypes = buildSet {
        add(
            parse<Expression>(
                when {
                  event -> "EventCard<Class<ProjectCard>>"
                  active -> "ActiveCard<Class<ProjectCard>>"
                  else -> "AutomatedCard<Class<ProjectCard>>"
                }
            )
        )
        if (actions.values.isNotEmpty()) add(parse("ActionCard"))
        resourceTypes.singleOrNull()?.let { add(parse("ResourceCard<Class<$it>>")) }
      }
      val properties = buildMap {
        put(COST_PROPERTY, NumberValue(cost.value))
        requirement.value?.let { put(REQUIREMENT_PROPERTY, RequirementValue(it)) }
      }
      return ClassDeclaration(
          className = name,
          kind = CONCRETE,
          supertypes = supertypes,
          authoredEffects = authoredEffects,
          authoredActions = actions.values.map(ActionPart::value),
          properties = properties,
      )
    }
  }

  private object Registry : RandomGenerator.Registry<Any>() {
    init {
      register {
        Cost(
            choose(
                1 to 0,
                2 to 1,
                3 to 2,
                5 to 3,
                5 to 4,
                7 to 5,
                7 to 6,
                8 to 7,
                9 to 8,
                10 to 9,
                10 to 10,
                10 to 11,
                10 to 12,
                9 to 13,
                8 to 14,
                8 to 15,
                7 to 16,
                6 to 17,
                6 to 18,
                5 to 19,
                5 to 20,
                4 to 21,
                4 to 22,
                3 to 23,
                3 to 24,
                3 to 25,
                2 to 26,
                2 to 27,
                2 to 28,
                1 to 29,
                1 to 30,
                1 to 31,
                1 to 32,
                1 to 33,
                1 to 35,
                1 to 38,
                1 to 40,
            )
        )
      }
      register {
        Tag(
            cn(
                choose(
                    18 to "BuildingTag",
                    17 to "SpaceTag",
                    13 to "ScienceTag",
                    10 to "EarthTag",
                    9 to "PlantTag",
                    7 to "MicrobeTag",
                    6 to "PowerTag",
                    5 to "JovianTag",
                    5 to "VenusTag",
                    4 to "AnimalTag",
                    3 to "CityTag",
                )
            )
        )
      }
      register {
        CardResource(
            cn(
                choose(
                    31 to "Animal",
                    29 to "Microbe",
                    20 to "Floater",
                    13 to "Asteroid",
                    7 to "Science",
                )
            )
        )
      }
      register {
        StandardResource(
            choose(
                30 to "MC",
                18 to "Plant",
                17 to "Heat",
                14 to "Energy",
                11 to "Steel",
                10 to "Titanium",
            )
        )
      }
      register {
        RequirementAtom(
            chooseS(
                21 to { "${choose(TEMPERATURE_MINIMUMS)} TemperatureStep" },
                6 to { "MAX ${choose(TEMPERATURE_MAXIMUMS)} TemperatureStep" },
                17 to { "${choose(OXYGEN_MINIMUMS)} OxygenStep" },
                5 to { "MAX ${choose(OXYGEN_MAXIMUMS)} OxygenStep" },
                8 to { "${choose(VENUS_MINIMUMS)} VenusStep" },
                3 to { "MAX ${choose(VENUS_MAXIMUMS)} VenusStep" },
                8 to { "${choose(OCEAN_COUNTS)} OceanTile" },
                2 to { "MAX ${choose(OCEAN_COUNTS)} OceanTile" },
                17 to
                    {
                      val tag = recurse<Tag>().className
                      scaled(choose(1, 2, 2, 3, 3, 4, 5), tag.toString())
                    },
                5 to
                    {
                      val tags = setOfSize<Tag>(2).map(Tag::className)
                      tags.joinToString()
                    },
                4 to { "${choose(1, 2, 2, 3)} ${choose("CityTile", "GreeneryTile", "Colony")}" },
                2 to { "PROD[${recurse<StandardResource>().source}]" },
                2 to { "${choose(20, 25, 30, 35)} TerraformRating" },
                8 to
                    {
                      val tag = recurse<Tag>().className
                      "${choose(1, 1, 2, 2, 3)} CardFront<Anyone>(HAS $tag)"
                    },
                6 to
                    {
                      val tile = choose("CityTile<Anyone>", "GreeneryTile<Anyone>", "OceanTile")
                      "${choose(1, 1, 2, 2, 3)} OwnedTile<MarsArea(HAS Neighbor<$tile>)>"
                    },
                4 to
                    {
                      val resource = recurse<CardResource>().className
                      val tag = recurse<Tag>().className
                      "${choose(1, 1, 2, 3)} $resource<Anyone, CardFront(HAS $tag)>"
                    },
            )
        )
      }
      register {
        val atoms = listOfSize<RequirementAtom>(2)
        val source =
            chooseS(
                75 to { recurse<RequirementAtom>().source },
                18 to { atoms.joinToString { it.source } },
                7 to { atoms.joinToString(" OR ") { it.source } },
            )
        RequirementPart(parse(source))
      }
      register {
        PlayRequirement(
            chooseS(
                58 to { null },
                42 to { recurse<RequirementPart>().value },
            )
        )
      }
      register {
        val count = choose(4 to 0, 38 to 1, 43 to 2, 13 to 3, 2 to 4)
        Tags(setOfSize<Tag>(count))
      }
      register {
        val tags = setOfSize<Tag>(2).map(Tag::className)
        val firstTag = tags[0]
        val secondTag = tags[1]
        CardSelector(
            chooseS(
                55 to { "CardFront<Anyone>(HAS $firstTag)" },
                20 to
                    {
                      "CardFront<Anyone>(HAS ${scaled(choose(2, 2, 3, 3, 4), firstTag.toString())})"
                    },
                10 to { "CardFront<Anyone>(HAS $firstTag, $secondTag)" },
                6 to { "CardFront<Anyone>(HAS $firstTag OR $secondTag)" },
                5 to { "CardFront(HAS $firstTag, MAX 0 $secondTag)" },
                4 to { "CardFront<Anyone>(HAS $firstTag, MAX 0 CardResource)" },
            )
        )
      }
      register {
        val neighbor =
            choose(
                "CityTile<Anyone>",
                "GreeneryTile<Anyone>",
                "OceanTile",
                "OwnedTile<Anyone>",
            )
        AreaSelector(
            chooseS(
                30 to { "LandArea(HAS MAX 0 Tile)" },
                28 to { "LandArea(HAS MAX 0 Tile, Neighbor<$neighbor>)" },
                20 to { "LandArea(HAS MAX 0 Tile, MAX 0 Neighbor<$neighbor>)" },
                14 to
                    {
                      val other = choose("CityTile<Anyone>", "GreeneryTile<Anyone>", "OceanTile")
                      "LandArea(HAS MAX 0 Tile, Neighbor<$neighbor>, Neighbor<$other>)"
                    },
                8 to { "LandArea(HAS MAX 0 Tile, MAX 0 Neighbor<CityTile<Anyone>>)" },
            )
        )
      }
      register {
        val tag = recurse<Tag>().className
        PlayerSelector(
            chooseS(
                50 to { "Anyone" },
                32 to { "Anyone(HAS ${scaled(choose(1, 1, 2, 2, 3), tag.toString())})" },
                18 to
                    {
                      val other = recurse<Tag>().className
                      "Anyone(HAS $tag, $other)"
                    },
            )
        )
      }
      register {
        MetricPart(
            chooseS(
                23 to { recurse<Tag>().className.toString() },
                12 to { choose("CityTile<Anyone>", "GreeneryTile<Anyone>", "OceanTile") },
                8 to { "Colony<Anyone>" },
                9 to { scaled(choose(2, 2, 3, 3, 4), recurse<Tag>().className.toString()) },
                8 to { scaled(choose(2, 2, 3), "CityTile<Anyone>") },
                15 to { recurse<CardSelector>().source },
                10 to
                    {
                      val tile = choose("CityTile", "GreeneryTile")
                      "$tile<Anyone, MarsArea(HAS Neighbor<OceanTile>)>"
                    },
                8 to
                    {
                      val resource = recurse<CardResource>().className
                      "$resource<Anyone, ${recurse<CardSelector>().source}>"
                    },
                7 to
                    {
                      val first = choose("CityTile<Anyone>", "GreeneryTile<Anyone>")
                      val second = choose("OceanTile", "SpecialTile<Anyone>")
                      "Adjacency<$first, $second>"
                    },
            )
        )
      }
      register {
        val source =
            chooseS(
                72 to
                    {
                      val resource = recurse<StandardResource>().source
                      "PROD[${productionTerm(resource)}]"
                    },
                28 to
                    {
                      val resources = distinctStandardResources(2)
                      val first = productionTerm(resources[0])
                      val second = productionTerm(resources[1], preferGain = true)
                      "PROD[$first, $second]"
                    },
                12 to
                    {
                      val resource = recurse<StandardResource>().source
                      "PROD[${scaled(choose(1, 1, 2), resource)} / ${recurse<MetricPart>().source}]"
                    },
            )
        ProductionChange(source)
      }
      register {
        chooseS(
            28 to
                {
                  val resource = recurse<StandardResource>().source
                  InstructionAtom(scaled(smallCount(), resource))
                },
            23 to { InstructionAtom(recurse<ProductionChange>().source) },
            11 to
                {
                  InstructionAtom(
                      scaled(trackSteps(), choose("TemperatureStep", "OxygenStep", "VenusStep"))
                  )
                },
            5 to
                {
                  InstructionAtom(choose("OceanTile<>", "CityTile<>", "GreeneryTile<>"))
                },
            5 to
                {
                  val tile = choose("CityTile", "GreeneryTile")
                  InstructionAtom("$tile<${recurse<AreaSelector>().source}>")
                },
            6 to { InstructionAtom(scaled(choose(1, 1, 1, 2, 2, 3), "ProjectCard")) },
            4 to { InstructionAtom(scaled(choose(1, 1, 1, 2), "TerraformRating")) },
            7 to
                {
                  val resource = recurse<StandardResource>().source
                  InstructionAtom(
                      "-${scaled(choose(1, 2, 3, 4, 5), resource)}<${recurse<PlayerSelector>().source}>?"
                  )
                },
            4 to
                {
                  val resource = recurse<CardResource>().className
                  InstructionAtom(scaled(choose(1, 1, 2, 3), resource.toString()))
                },
            3 to
                {
                  val resource = recurse<CardResource>().className
                  InstructionAtom(
                      scaled(choose(1, 1, 2, 3), "$resource<This>"),
                      setOf(resource),
                  )
                },
            4 to
                {
                  val resource = recurse<CardResource>().className
                  InstructionAtom(
                      scaled(choose(1, 1, 2, 2, 3), "$resource<${recurse<CardSelector>().source}>")
                  )
                },
        )
      }
      register {
        chooseS(
            54 to
                {
                  val atom = recurse<InstructionAtom>()
                  InstructionPart(atom.source, atom.resourceTypes)
                },
            14 to
                {
                  val choices = listOfSize<InstructionAtom>(2)
                  InstructionPart(
                      choices.joinToString(" OR ") { it.source },
                      choices.flatMapTo(linkedSetOf(), InstructionAtom::resourceTypes),
                  )
                },
            11 to
                {
                  val resource = recurse<StandardResource>().source
                  val count = smallCount()
                  InstructionPart("${scaled(count, resource)} / ${recurse<MetricPart>().source}")
                },
            13 to
                {
                  val resource = recurse<StandardResource>().source
                  val result = recurse<InstructionAtom>()
                  InstructionPart(
                      "-${scaled(smallCount(), resource)} THEN ${result.source}",
                      result.resourceTypes,
                  )
                },
            8 to
                {
                  val instruction = recurse<InstructionAtom>()
                  InstructionPart(
                      "((${recurse<RequirementPart>().value}: ${instruction.source}) OR Ok)",
                      instruction.resourceTypes,
                  )
                },
        )
      }
      register {
        val count = choose(6 to 0, 35 to 1, 43 to 2, 16 to 3)
        Immediate(listOfSize(count))
      }
      register {
        chooseS(
            12 to { ActionCost(null) },
            48 to
                {
                  val resource = recurse<StandardResource>().source
                  ActionCost(scaled(smallCount(), resource))
                },
            14 to
                {
                  val resource = recurse<StandardResource>().source
                  ActionCost("PROD[${scaled(choose(1, 1, 1, 2), resource)}]")
                },
            8 to
                {
                  val resource = recurse<CardResource>().className
                  ActionCost(
                      scaled(choose(1, 1, 1, 2, 2, 3), "$resource<This>"),
                      setOf(resource),
                  )
                },
            11 to
                {
                  val resources = distinctStandardResources(2)
                  ActionCost(resources.joinToString { scaled(smallCount(), it) })
                },
            7 to
                {
                  val resource = recurse<StandardResource>().source
                  ActionCost(
                      "${recurse<RequirementPart>().value}: ${scaled(smallCount(), resource)}"
                  )
                },
        )
      }
      register {
        val cost = recurse<ActionCost>()
        val results = listOfSize<InstructionPart>(choose(4 to 1, 1 to 2))
        val source = "${cost.source.orEmpty()} -> ${results.joinToString { it.source }}"
        ActionPart(
            parse(source),
            cost.resourceTypes + results.flatMap(InstructionPart::resourceTypes),
        )
      }
      register {
        val count = choose(62 to 0, 32 to 1, 6 to 2)
        Actions(listOfSize(count))
      }
      register {
        PersistentTrigger(
            chooseS(
                26 to { recurse<CardSelector>().source },
                16 to
                    {
                      "${recurse<Tag>().className}<CardFront<Anyone>, Anyone>"
                    },
                10 to
                    {
                      "${recurse<Tag>().className}<${recurse<CardSelector>().source}, Anyone>"
                    },
                12 to
                    {
                      val tile = choose("CityTile", "GreeneryTile")
                      "$tile<Anyone, MarsArea(HAS Neighbor<OceanTile>)>"
                    },
                14 to
                    {
                      choose("CityTile<Anyone>", "GreeneryTile<Anyone>", "OceanTile")
                    },
                15 to { choose("TemperatureStep", "OxygenStep", "VenusStep", "TerraformRating") },
                5 to
                    {
                      val first = recurse<CardSelector>().source
                      val second = recurse<CardSelector>().source
                      "$first OR $second"
                    },
                2 to
                    {
                      "CardFront<Anyone> IF ${recurse<RequirementPart>().value}"
                    },
            )
        )
      }
      register {
        val trigger = recurse<PersistentTrigger>()
        val instruction = recurse<InstructionPart>()
        PersistentEffect(
            parse("${trigger.source}: ${instruction.source}"),
            instruction.resourceTypes,
        )
      }
      register {
        val count = choose(65 to 0, 30 to 1, 5 to 2)
        PersistentEffects(listOfSize(count))
      }
      register {
        chooseS(
            57 to
                {
                  val count = choose(-2, -1, 1, 1, 1, 1, 2, 2, 2, 3, 4)
                  ScoringEffect(parse("End: ${signedScale(count, "VictoryPoint")}"), emptySet())
                },
            30 to
                {
                  ScoringEffect(
                      parse("End: VictoryPoint / ${recurse<MetricPart>().source}"),
                      emptySet(),
                  )
                },
            13 to
                {
                  val resource = recurse<CardResource>().className
                  val denominator = choose(1, 1, 2, 2, 3, 3, 4)
                  ScoringEffect(
                      parse("End: VictoryPoint / ${scaled(denominator, "$resource<This>")}"),
                      setOf(resource),
                  )
                },
        )
      }
      register {
        Scoring(
            chooseS(
                52 to { null },
                48 to { recurse<ScoringEffect>() },
            )
        )
      }
      register { EventKind(choose(83 to false, 17 to true)) }
      register {
        CardDraft(
            recurse(),
            recurse(),
            recurse(),
            recurse(),
            recurse(),
            recurse(),
            recurse(),
            recurse(),
        )
      }
    }

    private fun RandomGenerator<Any>.smallCount(): Int = choose(1, 1, 1, 2, 2, 2, 3, 3, 4, 5)

    private fun RandomGenerator<Any>.trackSteps(): Int = choose(1, 1, 1, 1, 1, 2)

    private fun RandomGenerator<Any>.productionTerm(
        resource: String,
        preferGain: Boolean = false,
    ): String {
      val count = choose(1, 1, 1, 2, 2, 3)
      val removeWeight = if (preferGain) 15 else 30
      val remove = choose(removeWeight to true, 100 - removeWeight to false)
      return "${if (remove) "-" else ""}${scaled(count, resource)}"
    }

    private fun RandomGenerator<Any>.distinctStandardResources(count: Int): List<String> {
      val resources = linkedSetOf<String>()
      while (resources.size < count) resources += recurse<StandardResource>().source
      return resources.toList()
    }

    private fun scaled(count: Int, source: String): String =
        if (count == 1) source else "$count $source"

    private fun signedScale(count: Int, source: String): String =
        when (count) {
          -1 -> "-$source"
          1 -> source
          else -> "$count $source"
        }
  }

  internal companion object {
    private const val MAX_ATTEMPTS_PER_CARD = 500
    private const val MIN_RULE_NODE_COUNT = 14
    private val EVENT_TAG = cn("EventTag")
    private val COST_PROPERTY = PropertyName("cost")
    private val REQUIREMENT_PROPERTY = PropertyName("requirement")
    private val TEMPERATURE_MINIMUMS = (2..17).toList()
    private val TEMPERATURE_MAXIMUMS = (2..13).toList()
    private val OXYGEN_MINIMUMS = (2..13).toList()
    private val OXYGEN_MAXIMUMS = (2..9).toList()
    private val VENUS_MINIMUMS = (2..9).toList()
    private val VENUS_MAXIMUMS = (2..7).toList()
    private val OCEAN_COUNTS = (2..8).toList()

    private fun catalogWith(declarations: List<ClassDeclaration>): TfmCatalog {
      val additions =
          object : TfmCatalog() {
            override val explicitClassDeclarations: Set<ClassDeclaration> = declarations.toSet()
          }
      return TfmCatalog.compose(Canon, additions)
    }

    @JvmStatic
    fun main(args: Array<String>) {
      require(args.size <= 3) { "usage: RandomCardGenerator [count] [seed] [output-file]" }
      val count = args.getOrNull(0)?.toInt() ?: 12
      val seed = args.getOrNull(1)?.toInt() ?: Random.Default.nextInt()
      val cards = RandomCardGenerator(seed).generate(count)

      val report = buildString {
        appendLine("Random card seed: $seed")
        cards.forEachIndexed { index, card ->
          appendLine()
          appendLine("=== Card ${index + 1} ===")
          appendLine(card)
        }
      }
      args.getOrNull(2)?.let { output -> File(output).writeText(report) } ?: print(report)
    }
  }
}
