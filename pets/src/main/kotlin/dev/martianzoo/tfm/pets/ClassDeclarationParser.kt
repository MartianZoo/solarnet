package dev.martianzoo.tfm.pets

import com.github.h0tk3y.betterParse.combinators.AndCombinator
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
import dev.martianzoo.tfm.pets.PetParser.Actions.action
import dev.martianzoo.tfm.pets.PetParser.Effects.effect
import dev.martianzoo.tfm.pets.PetParser.Instructions
import dev.martianzoo.tfm.pets.PetParser.Requirements.requirement
import dev.martianzoo.tfm.pets.PetParser.Types
import dev.martianzoo.tfm.pets.PetParser.Types.typeExpression
import dev.martianzoo.tfm.pets.PetParser._abstract
import dev.martianzoo.tfm.pets.PetParser._class
import dev.martianzoo.tfm.pets.PetParser._default
import dev.martianzoo.tfm.pets.PetParser._has
import dev.martianzoo.tfm.pets.PetParser.char
import dev.martianzoo.tfm.pets.PetParser.commaSeparated
import dev.martianzoo.tfm.pets.PetParser.nls
import dev.martianzoo.tfm.pets.PetParser.optionalList
import dev.martianzoo.tfm.pets.PetParser.skipChar
import dev.martianzoo.tfm.pets.PetParser.tokenizer
import dev.martianzoo.tfm.pets.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.pets.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.util.toSetStrict

/** Parses the Petaform language. */
object ClassDeclarationParser {

  /**
   * Parses an entire PETS class declarations source file.
   */
  fun parseClassDeclarations(text: String): List<ClassDeclaration> {
    val tokens = tokenizer.tokenize(stripLineComments(text))
    return parseRepeated(Declarations.nestedDecls, tokens)
  }

  // TODO move
  internal fun stripLineComments(text: String) = Regex(""" *(//[^\n]*)*\n""").replace(text, "\n")

  private object Declarations { // -------------------------------------------------------

    private val isAbstract: Parser<Boolean> = optional(_abstract) and skip(_class) map { it != null }

    private val dependency: Parser<DependencyDeclaration> = typeExpression map ::DependencyDeclaration

    private val dependencies: Parser<List<DependencyDeclaration>> =
        optionalList(skipChar('<') and commaSeparated(dependency) and skipChar('>'))

    private val supertypes: Parser<List<GenericTypeExpression>> =
        optionalList(skipChar(':') and commaSeparated(parser { Types.genericType }))

    private val signature: Parser<Signature> =
        parser { Types.classFullName } and
        dependencies and
        parser { Types.optlRefinement } and
        // optional(skipChar('[') and parser {Types.classShortName} and skipChar(']')) and
        supertypes map {
          (c, d, r, s) -> Signature(c, d, r, s)
        }

    private val moreSignatures: Parser<List<Signature>> =
        skipChar(',') and separatedTerms(signature, char(','))

    private val gainOnlyDefaults: Parser<DefaultsDeclaration> =
        skipChar('+') and Types.genericType and Instructions.intensity map { (type, int) ->
          require(type.className == THIS)
          require(type.refinement == null)
          DefaultsDeclaration(gainOnlySpecs = type.specs, gainIntensity = int)
        }

    private val allCasesDefault: Parser<DefaultsDeclaration> = Types.genericType map {
      require(it.className == THIS)
      require(it.refinement == null)
      DefaultsDeclaration(universalSpecs = it.specs)
    }

    private val default: Parser<DefaultsDeclaration> =
        skip(_default) and (gainOnlyDefaults or allCasesDefault)

    private val invariant: Parser<Requirement> = skip(_has) and requirement

    private val bodyElementNoClass: Parser<Any> = default or invariant or action or effect

    private val oneLineBody: Parser<List<Any>> =
        skipChar('{') and separatedTerms(bodyElementNoClass, char(';')) and skipChar('}')

    val singleDecl: Parser<ClassDeclaration> =
        isAbstract and signature and optionalList(oneLineBody) map { (abs, sig, body) ->
          createIncomplete(abs, sig, body).first().finish()
        }

    val bodyElement: Parser<Any> = bodyElementNoClass or parser { nestableDecls }

    private val multilineBodyInterior: Parser<List<Any>> =
        separatedTerms(bodyElement, oneOrMore(char('\n')), acceptZero = true)

    private val multilineBody: Parser<List<Any>> =
        skipChar('{') and
        skip(nls) and
        multilineBodyInterior and
        skip(nls) and
        skipChar('}')

    private val nestableDecls: Parser<List<NestableDecl>> =
        skip(nls) and
        isAbstract and
        signature and
        (multilineBody or optionalList(moreSignatures)) map {
          (abs, sig, bodyOrSigs) ->
            if (bodyOrSigs.firstOrNull() !is Signature) { // sigs
              createIncomplete(abs, sig, bodyOrSigs)
            } else { // body
              @Suppress("UNCHECKED_CAST")
              val signatures = listOf(sig) + (bodyOrSigs as List<Signature>)
              signatures.flatMap { createIncomplete(abs, it) }
            }
        }

    val nestedDecls: Parser<List<ClassDeclaration>> = nestableDecls map { defs -> defs.map { it.finish() } }
  }

  private fun createIncomplete(
      abst: Boolean,
      sig: Signature,
      contents: List<Any> = listOf(),
  ): List<NestableDecl> {
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
        id = sig.className, // TODO
        name = sig.className,
        abstract = abst,
        dependencies = sig.dependencies,
        supertypes = sig.supertypes.toSetStrict(),
        topInvariant = sig.topInvariant,
        otherInvariants = invs,
        effectsRaw = effs + actionsToEffects(acts),
        defaultsDeclaration = mergedDefaults,
    )
    return listOf(NestableDecl(comp, false)) + subs.flatten()
        .map { (it as NestableDecl).withSuperclass(sig.className) }
  }

  val oneLineClassDeclaration = Declarations.singleDecl

  private class Signature(
      val className: ClassName,
      val dependencies: List<DependencyDeclaration>,
      val topInvariant: Requirement?,
      val supertypes: List<GenericTypeExpression>,
  )

  data class NestableDecl(
      private val declaration: ClassDeclaration,
      internal val isComplete: Boolean,
  ) {
    fun withSuperclass(className: ClassName): NestableDecl =
        if (className == COMPONENT || isComplete) {
          this
        } else if (declaration.supertypes.any { it.className == className }) {
          this // it's redundant to add it again
        } else {
          val allSupertypes = listOf(className.type) + declaration.supertypes
          val fixedDeclaration = declaration.copy(supertypes = allSupertypes.toSetStrict())
          NestableDecl(fixedDeclaration, true)
        }

    internal fun finish(): ClassDeclaration { // TODO
      val supes = declaration.supertypes
      return when {
        declaration.name == COMPONENT -> declaration.also { require(supes.isEmpty()) }
        supes.isEmpty() -> declaration.copy(supertypes = setOf(COMPONENT.type))
        else -> declaration.also {
          require(COMPONENT.type !in supes) {
            "${declaration.name}: ${declaration.supertypes}"
          }
        }
      }
    }
  }
}
