package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.loadTypes
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass

/** Support shared by the Pets language tests (`docs/pets-language-spec.md`). */

/** Asserts that [source] parses as [P] and renders back as [rendered]. */
internal inline fun <reified P : PetNode> roundTrip(source: String, rendered: String = source) {
  val parsed = parse(P::class, source)
  parsed.toString() shouldBe rendered
  parse(P::class, rendered) shouldBe parsed
}

/** Asserts that every line of [sources] parses as [P] and renders back exactly. */
internal inline fun <reified P : PetNode> roundTripAll(sources: String): Unit =
    roundTripAll(P::class, sources)

/** Non-reified form of [roundTripAll]. */
internal fun <P : PetNode> roundTripAll(type: KClass<P>, sources: String) {
  val broken =
      sources.trimIndent().lines().filter { source ->
        val parsed = parse(type, source)
        val rendered = parsed.toString()
        rendered != source || parse(type, rendered) != parsed
      }
  if (broken.any())
      throw AssertionError("did not round-trip exactly:\n${broken.joinToString("\n")}")
}

/**
 * The declarations the language tests elaborate against. They are deliberately small and generic;
 * the Terraforming Mars names in the specification's examples are illustrations, not fixtures.
 */
private const val LANG_DECLARATIONS: String =
    """
    ABSTRACT CLASS Player : Owner, Actor {
      CLASS Player1
      CLASS Player2
    }
    ABSTRACT CLASS Area {
      CLASS Mars1
      CLASS Mars2
      ABSTRACT CLASS LandArea {
        CLASS Land1
        CLASS Land2
      }
    }

    CLASS Plant : Owned
    CLASS Heat : Owned
    CLASS Steel : Owned
    CLASS StartToken : Owned
    CLASS ProjectCard : Owned, Atomized

    ABSTRACT CLASS Tile<Area> : Owned { DEFAULT +Tile<LandArea> }
    CLASS GreeneryTile : Tile
    CLASS OceanTile : Tile

    ABSTRACT CLASS Token : Owned {
      CLASS RedToken
      CLASS BlueToken
    }

    "Classes whose gain and removal quantifier defaults differ"
    CLASS Chit : Owned { DEFAULT +Chit? }
    CLASS Slug : Owned { DEFAULT -Slug. }

    "A card, and a resource whose owner is forced to be its card's owner (T3-8)"
    ABSTRACT CLASS CardFront : Owned { CLASS Ants }
    ABSTRACT CLASS Cardbound<CardFront<@Owner>> : Owned<@Owner> { CLASS Animal }

    "A class whose removal-only default differs from its all-use default"
    CLASS Marker<Area> : Owned { DEFAULT -Marker<LandArea> }

    ABSTRACT CLASS Scored { score = Metric }
    CLASS Gardener : Scored {
      score = COUNT "2 Plant"
      This: Plant / EVAL This.score
    }
    ABSTRACT CLASS Rule { This: 2 ProjectCard, Plant }
    CLASS SimpleRule : Rule
    CLASS OwnedRule : Owned { This: Plant }
    """

/**
 * The declarations above, plus one registered transform handler so that dispatching marked syntax
 * (L8-1) is observable. `UNWRAP[x]` rewrites to `x`.
 */
internal val langTable: ClassTable by lazy {
  loadTypes(
      LANG_DECLARATIONS.trimIndent(),
      transformHandlerFactories = mapOf("UNWRAP" to { _ -> TransformHandler { inner -> inner } }),
  )
}

internal val langElaborator: PetElaborator by lazy { PetElaborator(langTable) }

internal val player1: Expression = parse("Player1")

/** A world that resolves types in [classTable] and answers [answer] to every requirement. */
internal class TableWorld(
    override val classTable: ClassTable,
    private val answer: Boolean = true,
) : TypeInfo {
  override fun isAbstract(e: Expression): Boolean = classTable.resolve(e).abstract

  override fun ensureNarrows(wide: Expression, narrow: Expression): Unit =
      classTable.resolve(narrow).ensureNarrows(classTable.resolve(wide), this)

  override fun ensureSelectionNarrows(wide: Expression, narrow: Expression): Unit =
      classTable.resolve(narrow).ensureSelectionNarrows(classTable.resolve(wide), this)

  override fun has(requirement: Requirement): Boolean = answer
}

internal val langWorld: TypeInfo = TableWorld(langTable)

/** Elaborates [source] as one player-submitted instruction tree, in Player1's context. */
internal fun elaborate(source: String): InstructionTree =
    langElaborator.elaborateInput(parse<InstructionTree>(source), player1)
