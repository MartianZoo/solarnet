package dev.martianzoo.search

import dev.martianzoo.pets.ast.*
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.XScalar

/** Projects existing AST constructor fields; never evaluates gameplay. */
internal class PetsXml {
  // Intern structural expressions after replacing only their root class. Keys are document-local,
  // collision-free equality labels, not types or resolved variable-binding identities.
  private val shapeKeys = mutableMapOf<Expression, String>()

  private fun shapeKey(expression: Expression): String =
      shapeKeys.getOrPut(expression.copy(className = ClassName.cn("Shape"))) {
        shapeKeys.size.toString()
      }

  fun element(
      parent: XmlElement,
      name: String,
      attributes: Map<String, Any?> = emptyMap(),
  ): XmlElement =
      XmlElement(name).also { child ->
        parent.appendChild(child)
        attributes.forEach { (key, value) ->
          if (value != null) child.setAttribute(key, value.toString())
        }
      }

  fun nodes(parent: XmlElement, role: String, nodes: Iterable<PetNode>) {
    val container = element(parent, role)
    nodes.forEach { node(container, it) }
  }

  fun node(parent: XmlElement, value: PetNode): XmlElement {
    val result =
        element(
            parent,
            value::class.simpleName!!,
            mapOf("kind" to value.kind.simpleName, "pets" to value.toString()),
        )
    value.sourceLocation?.let {
      result.setAttribute("line", it.line.toString())
      result.setAttribute("column", it.column.toString())
    }
    visitPetsFields(value) { name, fieldValue -> field(result, name, fieldValue) }
    if (value is Expression) result.setAttribute("shapeKey", shapeKey(value))
    if (value is Effect.Trigger) {
      result.setAttribute("selfGainOnly", isSelfGainOnly(value).toString())
      result.setAttribute("endOnly", isEndOnly(value).toString())
    }
    if (value is Metric.Count) {
      result.setAttribute("countedClass", value.expression.className.toString())
      result.setAttribute("countedShapeKey", shapeKey(value.expression))
    }
    if (value is Instruction.Change) {
      when (val count = value.count) {
        is ActualScalar -> result.setAttribute("count", count.value.toString())
        is XScalar -> result.setAttribute("xMultiple", count.multiple.toString())
      }
      value.gaining?.let { result.setAttribute("gaining", it.toString()) }
      value.removing?.let { result.setAttribute("removing", it.toString()) }
      value.gaining?.let {
        result.setAttribute("gainingClass", it.className.toString())
        result.setAttribute("gainingArgumentCount", it.arguments.size.toString())
        result.setAttribute("gainingArgumentsSpecified", it.argumentsSpecified.toString())
      }
      value.removing?.let { result.setAttribute("removingClass", it.className.toString()) }
    }
    if (value is Requirement.Counting) result.setAttribute("target", value.target.toString())
    return result
  }

  private fun field(parent: XmlElement, name: String, value: Any?) {
    when (value) {
      null -> Unit
      is ClassName,
      is PropertyName -> parent.setAttribute(name, value.toString())
      is Expression.TypeVariableName -> parent.setAttribute(name, value.authoredSpelling)
      is PetNode -> node(element(parent, name), value)
      is Iterable<*> -> {
        val container = element(parent, name)
        value.forEach { item -> node(container, item as PetNode) }
      }
      is String,
      is Number,
      is Boolean,
      is Enum<*> -> parent.setAttribute(name, value.toString())
      else -> error("Unmapped Pets field $name: ${value::class}")
    }
  }
}
