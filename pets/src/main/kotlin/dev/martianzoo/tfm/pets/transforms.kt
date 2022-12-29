package dev.martianzoo.tfm.pets

import com.google.common.flogger.FluentLogger
import dev.martianzoo.tfm.pets.PetsParser.parse
import dev.martianzoo.tfm.pets.SpecialComponent.PRODUCTION
import dev.martianzoo.tfm.pets.SpecialComponent.THIS
import dev.martianzoo.tfm.pets.SpecialComponent.USE_ACTION
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnGain
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.PetsNode
import dev.martianzoo.tfm.pets.ast.PetsNode.ProductionBox
import dev.martianzoo.tfm.pets.ast.QuantifiedExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression

internal fun actionToEffect(action: Action, index: Int): Effect {
  val merged = if (action.cost == null) {
    action.instruction
  } else {
    Instruction.Then(listOf(action.cost.toInstruction(), action.instruction))
  }
  val trigger: Trigger = parse("$USE_ACTION${index + 1}<$THIS>")
  return Effect(trigger, merged).also {
    log.atInfo().log("Converted action: $it")
  }
}

internal fun actionsToEffects(actions: Collection<Action>) =
    actions.withIndex().map { (i, act) -> actionToEffect(act, i) }

internal fun immediateToEffect(immediate: Instruction): Effect {
  return Effect(OnGain(THIS.type), immediate)
}

internal fun <P : PetsNode> resolveThisIn(node: P, resolveTo: TypeExpression): P {
  return replaceTypesIn(node, THIS.type, resolveTo).also {
    log.atInfo().log("Resolved `This` to `$resolveTo` in ${node.kind}: $it")
  }
}

internal fun <P : PetsNode> replaceTypesIn(node: P, from: TypeExpression, to: TypeExpression) =
    TypeReplacer(from, to).transform(node)

private class TypeReplacer(val from: TypeExpression, val to: TypeExpression) : AstTransformer() {
  override fun <P : PetsNode?> transform(node: P) = if (node == from) {
    to as P
  } else {
    super.transform(node)
  }
}

internal fun <P : PetsNode> spellOutQes(node: P): P {
  return QeSpellerOuter.transform(node).also {
    if (it != node) {
      log.atInfo().log("spelled out QEs in ${node.kind}: $it")
    }
  }
}

private object QeSpellerOuter : AstTransformer() {
  override fun <P : PetsNode?> transform(node: P): P {
    return when {
      node is QuantifiedExpression -> node.explicit() as P
      else -> super.transform(node)
    }
  }
}

internal fun <P : PetsNode> deprodify(node: P, producibleClassNames: Set<String>): P {
  return Deprodifier(producibleClassNames).transform(node).also {
    if (it != node) {
      log.atInfo().log("Deprodified a ${node.kind}: $it")
    }
  }
}

private class Deprodifier(val producible: Set<String>) : AstTransformer() {
  var inProd: Boolean = false

  override fun <P : PetsNode?> transform(node: P): P = when {
    node is ProductionBox<*> -> {
      require(!inProd)
      inProd = true
      transform(node.extract()).also { inProd = false }
    }

    inProd && node is TypeExpression && node.className in producible -> PRODUCTION.type.copy(
        specializations = listOf(node)
    )

    else -> super.transform(node)
  } as P
}

private val log: FluentLogger = FluentLogger.forEnclosingClass()
