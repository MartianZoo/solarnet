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
import dev.martianzoo.tfm.pets.SpecialComponent.COMPONENT
import dev.martianzoo.tfm.pets.SpecialComponent.THIS
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.te
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.util.toSetStrict

/** Parses the Petaform language. */
object ClassDeclarationParser {

  /**
   * Parses an entire PETS component defs source file.
   */
  fun parseClassDeclarations(text: String): List<ClassDeclaration> =
      parseRepeated(Components.nestedClassDeclarations, tokenizer.tokenize(text))

  private object Components { // -------------------------------------------------------
    private val isAbstract = optional(_abstract) and skip(_class) map { it != null }
    private val dependency = typeExpression map ::DependencyDeclaration
    private val dependencies = optionalList(
        skipChar('<') and commaSeparated(dependency) and skipChar('>')
    )
    private val supertypes = optionalList(skipChar(':') and commaSeparated(Types.gte))

    private val signature =
        Types.className and dependencies and Types.refinement and supertypes map { (c, d, r, s) ->
          Signature(c, d, r, s)
        }

    private val moreSignatures: Parser<List<Signature>> =
        skipChar(',') and separatedTerms(signature, char(','))

    private val gainDefault =
        skipChar('+') and Types.gte and Instructions.intensity map { (type, int) ->
          require(type.className == "$THIS")
          require(type.refinement == null)
          DefaultsDeclaration(gainOnlySpecs = type.specs, gainIntensity = int)
        }

    private val typeDefault = Types.gte map {
      require(it.className == "$THIS")
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
    private val blockBody: Parser<List<Any>> =
        skipChar('{') and nls and blockBodyContents and nls and skipChar('}')

    private val incompleteComponentDefs =
        nls and isAbstract and signature and (blockBody or optionalList(moreSignatures)) map { (abs, sig, bodyOrSigs) ->
          if (bodyOrSigs.firstOrNull() !is Signature) { // sigs
            createIncomplete(abs, sig, bodyOrSigs)
          } else { // body
            @Suppress("UNCHECKED_CAST") val signatures =
                listOf(sig) + (bodyOrSigs as List<Signature>)
            signatures.flatMap { createIncomplete(abs, it) }
          }
        }

    val nestedClassDeclarations = incompleteComponentDefs map { defs -> defs.map { it.declaration() } }
  }
  private fun createIncomplete(
      abst: Boolean,
      sig: Signature,
      contents: List<Any> = listOf()
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
        defaultsDeclaration = mergedDefaults
    )
    return listOf(DeclarationInProgress(comp, false)) + subs.flatten()
        .map { (it as DeclarationInProgress).fillInSuperclass(sig.className) }
  }

  val oneLineClassDeclaration = Components.ocd

  private data class Signature(
      val className: String,
      val dependencies: List<DependencyDeclaration>,
      val topInvariant: Requirement?,
      val supertypes: List<GenericTypeExpression>
  )

  class DeclarationInProgress(
      private val declaration: ClassDeclaration,
      private val isComplete: Boolean
  ) {
    fun declaration() = if (isComplete) declaration else fixSupertypes()

    fun fillInSuperclass(name: String) =
        if (isComplete || declaration.supertypes.any { it.className == name }) {
          this
        } else {
          val supes = (listOf(te(name)) + declaration.supertypes)
          DeclarationInProgress(declaration.copy(supertypes = supes.toSetStrict()), true)
        }

    private fun fixSupertypes(): ClassDeclaration {
      val supes = declaration.supertypes
      return when {
        declaration.className == "$COMPONENT" -> declaration.also { require(supes.isEmpty()) }
        supes.isEmpty() -> declaration.copy(supertypes = setOf(COMPONENT.type))
        else -> declaration.also { require(COMPONENT.type !in supes) }
      }
    }
  }
}
