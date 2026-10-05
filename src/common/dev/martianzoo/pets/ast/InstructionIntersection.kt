package dev.martianzoo.pets.ast

import dev.martianzoo.pets.Transforming.bindXTo
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.FromExpression.Full
import dev.martianzoo.pets.ast.Instruction.By
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Each
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.XScalar
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.GroundType

/** Structural intersection; the ordinary narrowing relation remains the validity check. */
internal class InstructionIntersection(private val table: ClassTable, private val info: TypeInfo) {
  fun intersect(left: InstructionTree, right: InstructionTree): InstructionTree? {
    if (left.narrows(right, info)) return left
    if (right.narrows(left, info)) return right
    if (left is Or) return alternatives(left.instructions.mapNotNull { intersect(it, right) })
    if (right is Or) return alternatives(right.instructions.mapNotNull { intersect(left, it) })

    // A constraint on one occurrence constrains the whole choice before its parts intersect.
    val specializedLeft = specialize(left, right) ?: return null
    val specializedRight = specialize(right, left) ?: return null
    if (specializedLeft != left || specializedRight != right) {
      return validate(intersect(specializedLeft, specializedRight), left, right)
    }

    val result =
        when {
          left is Change && right is Change -> changes(left, right)
          left is InstructionGroup && right is InstructionGroup -> {
            val members = parts(left.instructions, right.instructions) ?: return null
            // A group cannot contain Ok or change its arity during narrowing.
            if (members.any { it !is Instruction || it == NoOp }) return null
            InstructionGroup(members.filterIsInstance<Instruction>())
          }
          left is Then && right is Then -> {
            val stages = parts(left.stages, right.stages) ?: return null
            if (stages.any { it !is Instruction }) return null
            val continuation = intersect(left.continuation, right.continuation) ?: return null
            left.withParts(stages.filterIsInstance<Instruction>(), continuation)
          }
          left is Per && right is Per && left.metric == right.metric ->
              (intersect(left.inner, right.inner) as? Change)?.let { left.copy(inner = it) }
          left is By && right is By && left.actor == right.actor ->
              (intersect(left.inner, right.inner) as? Instruction)?.let { left.copy(inner = it) }
          left is Gated && right is Gated && left.gate == right.gate ->
              intersect(left.inner, right.inner)?.let { left.copy(inner = it) }
          left is Each && right is Each && left.selector == right.selector ->
              intersect(left.body, right.body)
                  ?.takeUnless { it == NoOp }
                  ?.let { left.copy(body = it) }
          else -> null
        }
    return validate(result, left, right)
  }

  private fun validate(
      result: InstructionTree?,
      left: InstructionTree,
      right: InstructionTree,
  ): InstructionTree? {
    if (result != null && (!result.narrows(left, info) || !result.narrows(right, info))) {
      throw NarrowingException(
          "intersection of `$left` and `$right` cannot retain their shared choices; supply a more specific choice"
      )
    }
    return result
  }

  private fun specialize(source: InstructionTree, proposal: InstructionTree): InstructionTree? {
    if (source !is Then && source !is Transmute) return source
    try {
      val scope = source.typeVariablesFor(info)
      val bindings =
          scope.variables
              .mapNotNull { variable ->
                val bound = table.resolve(scope.expressionOf(variable.declaration))
                val constrained =
                    scope.bindings(source, proposal, variable, info, table).fold(bound) {
                        result,
                        expression ->
                      table.glb(result, table.resolve(expression)) ?: return null
                    }
                (variable to constrained).takeIf { constrained != bound }
              }
              .toMap()
      val specialized =
          if (bindings.isEmpty()) source
          else scope.bind(bindings, table).transformInstructionTree(source)
      if (source !is Then || !source.hasSharedX.value) return specialized

      val values = mutableSetOf<Int>()
      fun captureX(wide: PetNode, narrow: PetNode) {
        if (wide is XScalar && narrow is ActualScalar) {
          narrow.ensureNarrows(wide, info)
          values += narrow.value / wide.multiple
        } else if (wide::class == narrow::class && wide !is Or) {
          wide.immediateChildren().zip(narrow.immediateChildren()).forEach { (w, n) ->
            captureX(w, n)
          }
        }
      }
      captureX(source, proposal)
      if (values.size > 1) return null
      return values.singleOrNull()?.let { bindXTo(it).transformInstructionTree(specialized) }
          ?: specialized
    } catch (_: NarrowingException) {
      return null
    }
  }

  private fun alternatives(instructions: List<InstructionTree>): InstructionTree? {
    val flattened = instructions.flatMap { if (it is Or) it.instructions else listOf(it) }
    return flattened.takeIf { it.isNotEmpty() }?.let(Or::createTree)
  }

  private fun parts(
      left: List<InstructionTree>,
      right: List<InstructionTree>,
  ): List<InstructionTree>? {
    if (left.size != right.size) return null
    return left.zip(right).map { (a, b) -> intersect(a, b) ?: return null }
  }

  private fun changes(left: Change, right: Change): InstructionTree? {
    val skip = NoOp.takeIf { it.narrows(left, info) && it.narrows(right, info) }
    if (left::class != right::class) return skip
    val leftQuantifier = left.quantifier!!
    val rightQuantifier = right.quantifier!!
    val quantifier =
        when {
          leftQuantifier.narrows(rightQuantifier, info) -> leftQuantifier
          rightQuantifier.narrows(leftQuantifier, info) -> rightQuantifier
          else -> return null
        }
    // Counts must overlap before Type matching: optional counts are ceilings, fixed counts and
    // X follow their ordinary narrowing rules.
    val count =
        listOf(left.count, right.count).distinct().firstOrNull { candidate ->
          try {
            left.ensureCountIsNarrowedBy(candidate, info)
            right.ensureCountIsNarrowedBy(candidate, info)
            true
          } catch (_: NarrowingException) {
            false
          }
        } ?: return null
    fun expression(a: Expression, b: Expression): Expression? {
      val aType = table.resolve(a)
      val bType = table.resolve(b)
      val common = table.glb(aType, bType)
      if (
          common == null &&
              table.allSubclasses(aType.rootClass).any { it.isSubtypeOf(bType.rootClass) } &&
              table.allConcreteSubtypes(aType).any {
                it.narrows(aType, info) && it.narrows(bType, info)
              }
      ) {
        throw NarrowingException(
            "intersection of `$a` and `$b` has no single Type; supply a more specific choice"
        )
      }
      return common?.takeIf(table::isInhabited)?.let(::checkRefinements)?.expressionFull
    }

    val gaining = left.gaining?.let { expression(it, right.gaining!!) ?: return skip }
    val removing = left.removing?.let { expression(it, right.removing!!) ?: return skip }
    return when (left) {
      is Gain -> Gain.gain(scaledEx(gaining!!, count), quantifier)
      is Remove -> Remove.remove(scaledEx(removing!!, count), quantifier)
      is Transmute -> Transmute(Full(gaining!!, removing!!), count, quantifier)
    }
  }

  private fun checkRefinements(type: GroundType): GroundType? {
    val checked =
        type.copy(
            dependencies =
                type.dependencies.mapWithKey { _, bound ->
                  checkRefinements(bound) ?: return null
                }
        )
    if (checked.rootClass.abstract || checked.dependencies.abstract) return checked
    val concrete = checked.copy(refinement = null)
    return concrete.takeIf { it.narrows(checked, info) }
  }
}
