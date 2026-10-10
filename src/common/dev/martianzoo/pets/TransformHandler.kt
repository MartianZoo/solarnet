package dev.martianzoo.pets

import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.Action.Cost
import dev.martianzoo.pets.ast.ActionTree
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.EffectTree
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Declaration
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Reference
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.TransformNode

/**
 * Rewrites an explicitly marked subtree in its lexical scope. The result must retain the marked
 * Pets family; null preserves the mark. See
 * [section 8](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#8-transform-blocks).
 * Dispatch is inside-out. Deferred property evaluations retain their transforms until expansion.
 */
public fun interface TransformHandler {
  /** Rewrites available syntax; [scope] supplies the meaning of lexical names at this mark. */
  public fun transform(inner: PetNode, scope: Scope): PetNode?

  /** Whether nested applications with no intervening transform kind can be coalesced. */
  public val idempotent: Boolean
    get() = false

  /** Lexical facts supplied by the enclosing source, never inferred from the event Actor. */
  public data class Scope(
      /** The visible Me expression; null means no enclosing binding. */
      public val me: Expression? = null,
      /** The receiver used to interpret This while matching dependency arguments. */
      public val context: Expression = THIS.expression,
      /**
       * Identities of visible type variables that select represented Classes rather than
       * components.
       */
      internal val representedClassMarkers: Set<Any> = emptySet(),
  )

  /**
   * A handler's recursive rewrite. It captures lexical Me at EVAL sites and preserves [kind] around
   * values that cannot yet expand. Override [rewrite] for the transform's actual rules; use
   * [transformChildren] to recurse. A null kind performs only lexical capture for dispatch.
   */
  public abstract class Rewriter
  protected constructor(
      private val kind: String?,
      initialScope: Scope,
  ) : PetTransformer() {
    /** Lexical context for the current node; changes apply to its recursive traversal. */
    protected var scope: Scope = initialScope

    final override fun transformNode(node: PetNode): PetNode {
      val previous = scope
      scope =
          when (node) {
            is Effect ->
                scope.copy(
                    me = scope.me ?: triggerMe(node.trigger),
                    representedClassMarkers =
                        scope.representedClassMarkers +
                            node.typeVariables.variables
                                .filter { it.selectsClass }
                                .mapNotNull {
                                  it.declaration.expression.typeVariableName?.identity
                                } +
                            node.trigger
                                .descendantsOfType<Expression>()
                                .filter { it.className == CLASS }
                                .flatMap { it.arguments }
                                .mapNotNull { it.typeVariableName?.identity },
                )
            is Instruction.Each -> scope.copy(me = selectedMe(node.selector) ?: scope.me)
            is Metric.Rank -> scope.copy(me = node.selector?.let(::selectedMe) ?: scope.me)
            else -> scope
          }
      return try {
            // Other deferred transforms stay outside EVAL in the same order. The new transform
            // encloses them rather than traversing and reversing their composition.
            if (
                node is Metric.Eval ||
                    node is Requirement.Eval ||
                    (kind != null && node is TransformNode<*> && deferredEvaluation(node) != null)
            ) {
              val captured = captureDeferred(node)
              if (kind == null) captured else wrap(captured, kind)
            } else rewrite(node)
          } finally {
            scope = previous
          }
          .also { result ->
            if (result.sourceLocation == null) result.sourceLocation = node.sourceLocation
          }
    }

    /**
     * Rewrites available syntax under [scope]; deferred EVAL is handled by the shared traversal.
     */
    protected open fun rewrite(node: PetNode): PetNode = transformChildren(node)

    private fun captureDeferred(node: PetNode): PetNode =
        when (node) {
          is TransformNode<*> -> wrap(captureDeferred(node.extract()), node.transformKind)
          is Metric.Eval ->
              node.copy(property = transformProperty(node.property), me = node.me ?: scope.me)
          is Requirement.Eval ->
              node.copy(property = transformProperty(node.property), me = node.me ?: scope.me)
          else -> error("not a deferred evaluation: $node")
        }
  }

  public companion object {
    /** Dispatches the registered kinds, preserving other kinds for later processing. */
    public fun dispatcher(
        handlers: Map<String, TransformHandler>,
        scope: Scope = Scope(),
    ): PetTransformer = Dispatcher(handlers, scope)

    internal fun deferredEvaluation(node: PetNode): PetNode? =
        when (node) {
          is Metric.Eval,
          is Requirement.Eval -> node
          is TransformNode<*> -> deferredEvaluation(node.extract())
          else -> null
        }

    internal fun selectedMe(selector: Expression): Expression? {
      val marker = selector.typeVariableName as? Declaration ?: return null
      if (marker.name != "Me") return null
      return selector.copy(
          refinement = null,
          typeVariableName =
              Reference(marker.name, marker.boundClassName)
                  .resolved(requireNotNull(marker.resolution)),
      )
    }

    private fun triggerMe(trigger: Trigger): Expression? =
        trigger.descendantsOfType<Expression>().firstNotNullOfOrNull(::selectedMe)

    internal fun wrap(inner: PetNode, kind: String): PetNode =
        when (inner) {
          is EffectTree -> EffectTree.Transform(inner, kind)
          is ActionTree -> ActionTree.Transform(inner, kind)
          is Cost -> Cost.Transform(inner, kind)
          is InstructionTree -> Instruction.Transform(inner, kind)
          is Metric -> Metric.Transform(inner, kind)
          is Requirement -> Requirement.Transform(inner, kind)
          is Trigger -> Trigger.Transform(inner, kind)
          else -> error("cannot mark $inner")
        }
  }

  private class Dispatcher(
      private val handlers: Map<String, TransformHandler>,
      initialScope: Scope,
  ) : Rewriter(null, initialScope) {
    private val activeKinds = mutableListOf<String>()

    override fun rewrite(node: PetNode): PetNode {
      if (node !is TransformNode<*>) return transformChildren(node)
      val kind = node.transformKind
      val handler = handlers[kind]
      if (handler != null && kind in activeKinds) {
        if (!handler.idempotent) {
          throw ExpressionException(
              "`$kind` transforms cannot be nested",
              sourceLocation = node.sourceLocation,
          )
        }
        if (activeKinds.last() == kind) return transformWithoutKindCheck(node.extract())
      }
      activeKinds.add(kind)
      return try {
        val inner = transformWithoutKindCheck(node.extract())
        if (deferredEvaluation(inner) != null)
            return wrap(inner, kind).also { it.sourceLocation = node.sourceLocation }
        val replacement = handler?.transform(inner, scope) ?: return wrap(inner, kind)
        if (!accepts(node, replacement)) {
          throw IllegalStateException(
              "`$kind` handler returned `${replacement.kind.simpleName}` for `${inner.kind.simpleName}`"
          )
        }
        replacement
      } finally {
        activeKinds.removeLast()
      }
    }

    private fun accepts(zone: TransformNode<*>, replacement: PetNode): Boolean =
        when (zone) {
          is EffectTree.Transform -> replacement is EffectTree
          is ActionTree.Transform -> replacement is ActionTree
          is Cost.Transform -> replacement is Cost
          is Instruction.Transform -> replacement is InstructionTree
          is Metric.Transform -> replacement is Metric
          is Requirement.Transform -> replacement is Requirement
          is Trigger.Transform -> replacement is Trigger
          else -> false
        }
  }
}
