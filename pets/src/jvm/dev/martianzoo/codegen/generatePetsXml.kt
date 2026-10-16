package dev.martianzoo.codegen

import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.PetNode
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KVisibility
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/** Generates the structural field visitor used by Search; no AST field list is maintained here. */
internal fun main(args: Array<String>) {
  val output = File(args.single(), "dev/martianzoo/search/visitPetsFields.kt")
  output.parentFile.mkdirs()
  output.writeText(
      buildString {
        appendLine("// Generated from PetNode constructor properties. Do not edit.")
        appendLine("package dev.martianzoo.search")
        appendLine("import dev.martianzoo.pets.ast.*")
        appendLine("internal fun visitPetsFields(value: PetNode, field: (String, Any?) -> Unit) {")
        appendLine("  when (value) {")
        for (node in concreteNodes(PetNode::class).sortedBy { it.qualifiedName }) {
          val typeName = node.qualifiedName!!.removePrefix("dev.martianzoo.pets.ast.")
          val constructor = node.primaryConstructor
          require(constructor != null || node.objectInstance != null) {
            "No primary constructor: $node"
          }
          appendLine("    is $typeName -> {")
          for (parameter in constructor?.parameters.orEmpty()) {
            val property = node.memberProperties.single { it.name == parameter.name }
            require(property.visibility == KVisibility.PUBLIC) {
              "Non-public AST field: $node.$property"
            }
            require(supportedField(property.returnType)) {
              "Unsupported AST field: $node.$property"
            }
            appendLine("      field(\"${property.name}\", value.${property.name})")
          }
          appendLine("    }")
        }
        appendLine("  }")
        appendLine("}")
      }
  )
}

private fun concreteNodes(root: KClass<out PetNode>): List<KClass<out PetNode>> =
    if (root.isSealed) root.sealedSubclasses.flatMap(::concreteNodes) else listOf(root)

private fun supportedField(type: KType): Boolean {
  val klass = type.classifier as KClass<*>
  return klass.isSubclassOf(PetNode::class) ||
      klass == Expression.TypeVariableName::class ||
      klass == String::class ||
      klass == Int::class ||
      klass == Boolean::class ||
      klass.java.isEnum ||
      (klass.isSubclassOf(Iterable::class) &&
          (type.arguments.single().type?.classifier as? KClass<*>)?.isSubclassOf(PetNode::class) ==
              true)
}
