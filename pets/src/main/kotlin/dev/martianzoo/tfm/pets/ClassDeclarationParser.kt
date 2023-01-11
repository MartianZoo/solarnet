package dev.martianzoo.tfm.pets

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.oneOrMore
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.data.ClassDeclaration.DefaultsDeclaration
import dev.martianzoo.tfm.data.ClassDeclaration.DependencyDeclaration
import dev.martianzoo.tfm.pets.PetsParser.Instructions
import dev.martianzoo.tfm.pets.PetsParser.Types
import dev.martianzoo.tfm.pets.PetsParser._abstract
import dev.martianzoo.tfm.pets.PetsParser._class
import dev.martianzoo.tfm.pets.PetsParser._default
import dev.martianzoo.tfm.pets.PetsParser._has
import dev.martianzoo.tfm.pets.PetsParser.action
import dev.martianzoo.tfm.pets.PetsParser.char
import dev.martianzoo.tfm.pets.PetsParser.commaSeparated
import dev.martianzoo.tfm.pets.PetsParser.effect
import dev.martianzoo.tfm.pets.PetsParser.nls
import dev.martianzoo.tfm.pets.PetsParser.optionalList
import dev.martianzoo.tfm.pets.PetsParser.requirement
import dev.martianzoo.tfm.pets.PetsParser.skipChar
import dev.martianzoo.tfm.pets.PetsParser.tokenizer
import dev.martianzoo.tfm.pets.PetsParser.typeExpression
import dev.martianzoo.tfm.pets.SpecialComponent.Component
import dev.martianzoo.tfm.pets.SpecialComponent.This
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.gte
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.util.toSetStrict

/** Parses the Petaform language. */
object ClassDeclarationParser {

  /**
   * Parses an entire PETS component defs source file.
   */
  fun parseClassDeclarations(text: String): List<ClassDeclaration> {
    val tokens = tokenizer.tokenize(stripLineComments(text))
    return parseRepeated(Components.nestedClassDeclarations, tokens)
  }

  // TODO move
  internal fun stripLineComments(text: String) = Regex(""" *(//[^\n]*)*\n""").replace(text, "\n")

  private object Components { // -------------------------------------------------------
    private val isAbstract = optional(_abstract) and skip(_class) map { it != null }
    private val dependency = typeExpression map ::DependencyDeclaration
    private val dependencies =
        optionalList(skipChar('<') and commaSeparated(dependency) and skipChar('>'))
    private val supertypes = optionalList(skipChar(':') and commaSeparated(Types.genericType))

    private val signature =
        Types.className and dependencies and Types.refinement and supertypes map { (c, d, r, s) ->
          Signature(c, d, r, s)
        }

    private val moreSignatures: Parser<List<Signature>> =
        skipChar(',') and separatedTerms(signature, char(','))

    private val gainDefault =
        skipChar('+') and Types.genericType and Instructions.intensity map { (type, int) ->
          require(type.className == This.className)
          require(type.refinement == null)
          DefaultsDeclaration(gainOnlySpecs = type.specs, gainIntensity = int)
        }

    private val typeDefault = Types.genericType map {
      require(it.className == This.className)
      require(it.refinement == null)
      DefaultsDeclaration(universalSpecs = it.specs)
    }

    private val defaultStmt: Parser<DefaultsDeclaration> =
        skip(_default) and (gainDefault or typeDefault)
    private val invariant = skip(_has) and requirement

    private val bodyElement = defaultStmt or invariant or action or effect

    private val oneLineBody =
        skipChar('{') and separatedTerms(bodyElement, char(';')) and skipChar('}')

    val ocd: Parser<ClassDeclaration> =
        isAbstract and signature and optionalList(oneLineBody) map { (abs, sig, body) ->
          createIncomplete(abs, sig, body).first().declaration()
        }

    private val blockBodyContents = separatedTerms(
        parser { incompleteComponentDefs } or bodyElement,
        oneOrMore(char('\n')),
        acceptZero = true
    )
    private val blockBody: Parser<List<Any>> = skipChar('{') and
        skip(nls) and
        blockBodyContents and
        skip(nls) and
        skipChar('}')

    private val incompleteComponentDefs =
        skip(nls) and
        isAbstract and
        signature and
        (blockBody or optionalList(moreSignatures)) map {
          (abs, sig, bodyOrSigs) ->
            if (bodyOrSigs.firstOrNull() !is Signature) { // sigs
              createIncomplete(abs, sig, bodyOrSigs)
            } else { // body
              @Suppress("UNCHECKED_CAST")
              val signatures = listOf(sig) + (bodyOrSigs as List<Signature>)
              signatures.flatMap { createIncomplete(abs, it) }
            }
        }

    val nestedClassDeclarations =
        incompleteComponentDefs map { defs -> defs.map { it.declaration() } }
  }

  private fun createIncomplete(
      abst: Boolean,
      sig: Signature,
      contents: List<Any> = listOf(),
  ): List<DeclarationInProgress> {
    val invs = contents.filterIsInstance<Requirement>().toSetStrict()
    val defs = contents.filterIsInstance<DefaultsDeclaration>().toSetStrict()
    val acts = contents.filterIsInstance<Action>().toSetStrict()
    val effs = contents.filterIsInstance<Effect>().toSetStrict()
    val subs = contents.filterIsInstance<List<*>>().toSetStrict()

    val mergedDefaults = DefaultsDeclaration(
        universalSpecs = defs.firstNotNullOfOrNull { it.universalSpecs } ?: listOf(),
        gainOnlySpecs = defs.firstNotNullOfOrNull { it.gainOnlySpecs } ?: listOf(),
        gainIntensity = defs.firstNotNullOfOrNull { it.gainIntensity },
    )

    val comp = ClassDeclaration(
        className = sig.className,
        abstract = abst,
        dependencies = sig.dependencies,
        supertypes = sig.supertypes.toSetStrict(),
        topInvariant = sig.topInvariant,
        otherInvariants = invs,
        effectsRaw = effs + actionsToEffects(acts),
        defaultsDeclaration = mergedDefaults,
    )
    return listOf(DeclarationInProgress(comp, false)) + subs.flatten()
        .map { (it as DeclarationInProgress).fillInSuperclass(sig.className) }
  }

  val oneLineClassDeclaration = Components.ocd

  private class Signature(
      val className: ClassName,
      val dependencies: List<DependencyDeclaration>,
      val topInvariant: Requirement?,
      val supertypes: List<GenericTypeExpression>,
  )

  class DeclarationInProgress(
      private val declaration: ClassDeclaration,
      private val isComplete: Boolean,
  ) {
    fun declaration() = if (isComplete) declaration else fixSupertypes()

    fun fillInSuperclass(className: ClassName) =
        if (isComplete || declaration.supertypes.any { it.className == className }) {
          this
        } else {
          val supes = (listOf(gte(className)) + declaration.supertypes)
          DeclarationInProgress(declaration.copy(supertypes = supes.toSetStrict()), true)
        }

    private fun fixSupertypes(): ClassDeclaration {
      val supes = declaration.supertypes
      return when {
        declaration.className == Component.className -> declaration.also { require(supes.isEmpty()) }
        supes.isEmpty() -> declaration.copy(supertypes = setOf(Component.type))
        else -> declaration.also { require(Component.type !in supes) }
      }
    }
  }
}
