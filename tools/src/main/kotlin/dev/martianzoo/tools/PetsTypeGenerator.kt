package dev.martianzoo.tools

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import dev.martianzoo.pets.systemClassDeclarations
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.StandardFormBundle
import dev.martianzoo.types.Class
import dev.martianzoo.types.ClassLoader
import dev.martianzoo.types.ClassTable
import dev.martianzoo.types.Dependency.Key
import dev.martianzoo.types.Dependency.TypeDependency
import dev.martianzoo.types.Type
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.system.exitProcess

internal data class PetsTypeGeneratorOptions(
    val packageName: String = "dev.martianzoo.generated",
    val fileName: String = "CanonicalPetsTypes",
    val output: Path? = null,
)

/** Generates a Kotlin type vocabulary from the fully resolved types in a Pets class table. */
internal class PetsTypeGenerator(
    private val table: ClassTable,
    private val packageName: String,
    private val fileName: String,
    classes: Set<Class> = table.allClasses(),
    private val generatedAt: Instant = Instant.now(),
) {
  private val classes = classes.toSet()
  private val orderedClasses: List<Class> by lazy(::hierarchyOrder)
  private val kotlinClasses: Map<Class, ClassName> = classes.associateWith {
    ClassName(packageName, it.className.toString())
  }
  private val classSymbols: Map<Class, ClassName> = classes.associateWith {
    ClassName(packageName, CLASS_SYMBOLS_CONTAINER, it.className.toString())
  }

  init {
    val missingSuperclasses = classes.flatMap(Class::directSuperclasses).toSet() - classes
    require(missingSuperclasses.isEmpty()) {
      "Generated class set omits superclasses: ${missingSuperclasses.map(Class::className)}"
    }
  }

  fun generate(): FileSpec {
    val file =
        FileSpec.builder(packageName, fileName)
            .addFileComment(
                "Generated at %L from a resolved Pets ClassTable. The component hierarchy comes " +
                    "first; PetsClasses at the end models class literals. Do not edit.",
                generatedAt,
            )
            .addAnnotation(
                AnnotationSpec.builder(Suppress::class)
                    .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                    .addMember("%S", "FINAL_UPPER_BOUND")
                    .build()
            )
    orderedClasses.forEach { file.addType(typeSpec(it)) }
    file.addType(classSymbolsType())
    return file.build()
  }

  private fun classSymbolsType(): TypeSpec {
    val symbols = TypeSpec.objectBuilder(CLASS_SYMBOLS_CONTAINER).addModifiers(KModifier.PUBLIC)
    orderedClasses.forEach { klass ->
      val symbol =
          if (klass.abstract) {
            TypeSpec.interfaceBuilder(klass.className.toString())
                .addModifiers(KModifier.PUBLIC, KModifier.SEALED)
          } else {
            TypeSpec.classBuilder(klass.className.toString()).addModifiers(KModifier.PUBLIC)
          }
      klass.directSuperclasses.forEach { symbol.addSuperinterface(classSymbol(it)) }
      symbols.addType(symbol.build())
    }
    return symbols.build()
  }

  private fun hierarchyOrder(): List<Class> {
    val remaining = classes.toMutableSet()
    val emitted = mutableSetOf<Class>()
    val result = mutableListOf<Class>()
    while (remaining.isNotEmpty()) {
      val next =
          remaining
              .filter { emitted.containsAll(it.directSuperclasses) }
              .minByOrNull(::hierarchySortKey)
              ?: error("Class hierarchy contains a cycle: ${remaining.map(Class::className)}")
      remaining -= next
      emitted += next
      result += next
    }
    return result
  }

  private fun hierarchySortKey(klass: Class): String {
    val primarySuperclass =
        klass.directSuperclasses.firstOrNull() ?: return klass.className.toString()
    return "${hierarchySortKey(primarySuperclass)}\u0000${klass.className}"
  }

  private fun typeSpec(klass: Class): TypeSpec {
    val variables = typeVariables(klass)
    val builder =
        if (klass.abstract) {
          TypeSpec.interfaceBuilder(klass.className.toString()).addModifiers(KModifier.SEALED)
        } else {
          TypeSpec.classBuilder(klass.className.toString())
        }
    builder.addModifiers(KModifier.PUBLIC)
    if (klass == table.componentClass) {
      builder.addProperty(
          PropertySpec.builder(TYPE_PROPERTY, PETS_TYPE)
              .addModifiers(KModifier.PUBLIC, KModifier.ABSTRACT)
              .build()
      )
    } else if (!klass.abstract) {
      builder.primaryConstructor(
          FunSpec.constructorBuilder().addParameter(TYPE_PROPERTY, PETS_TYPE).build()
      )
      builder.addProperty(
          PropertySpec.builder(TYPE_PROPERTY, PETS_TYPE)
              .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
              .initializer(TYPE_PROPERTY)
              .build()
      )
    }
    variables.values.forEach(builder::addTypeVariable)
    klass.directSuperclasses.forEach { superclass ->
      val resolvedSupertype = klass.baseType.asSupertype(superclass)
      builder.addSuperinterface(typeName(resolvedSupertype, variables))
    }
    return builder.build()
  }

  private fun typeVariables(klass: Class): LinkedHashMap<Key, TypeVariableName> {
    val resolved = ordinaryDependencies(klass.baseType)
    val names = typeVariableNames(klass, resolved)
    val provisional =
        klass.dependencies.keys.associateTo(linkedMapOf()) { key ->
          key to TypeVariableName(names.getValue(key), variance = KModifier.OUT)
        }
    return klass.dependencies.keys.associateTo(linkedMapOf()) { key ->
      val bound =
          resolved[key]?.boundType
              ?: checkNotNull(
                  if (klass == table.classClass) table.componentClass.baseType else null
              ) {
                "No resolved type dependency for $key on ${klass.className}"
              }
      val boundName =
          if (klass == table.classClass) classSymbol(table.componentClass)
          else typeName(bound, provisional)
      key to provisional.getValue(key).copy(bounds = listOf(boundName))
    }
  }

  private fun typeVariableNames(
      klass: Class,
      dependencies: Map<Key, TypeDependency>,
  ): Map<Key, String> {
    val abbreviations =
        klass.dependencies.keys.associateWith { key ->
          val boundClass =
              dependencies[key]?.boundType?.rootClass
                  ?: checkNotNull(if (klass == table.classClass) table.componentClass else null) {
                    "No resolved type dependency for $key on ${klass.className}"
                  }
          boundClass.abbreviation()
        }
    val totals = abbreviations.values.groupingBy { it }.eachCount()
    val nextIndex = mutableMapOf<String, Int>()
    return abbreviations.mapValues { (_, abbreviation) ->
      if (totals.getValue(abbreviation) == 1) {
        abbreviation
      } else {
        "$abbreviation${nextIndex.getOrPut(abbreviation) { 0 }.also { nextIndex[abbreviation] = it + 1 }}"
      }
    }
  }

  private fun Class.abbreviation(): String {
    if (shortName != className) return shortName.toString()
    return className.toString().filter(Char::isUpperCase).ifEmpty { className.toString().take(1) }
  }

  private fun typeName(
      type: Type,
      variables: Map<Key, TypeVariableName>,
      visiting: Set<Class> = emptySet(),
  ): TypeName {
    if (type.rootClass == table.classClass) {
      val representedName = type.expressionFull.arguments.single().className
      val represented = table.getClass(representedName)
      return kotlinClass(type.rootClass).parameterizedBy(classSymbol(represented))
    }
    if (type.rootClass in visiting) return starProjected(type.rootClass)

    val dependencies = ordinaryDependencies(type)
    val arguments =
        type.rootClass.dependencies.keys.map { key ->
          variables[key]
              ?: dependencies[key]?.boundType?.let {
                typeName(it, variables, visiting + type.rootClass)
              }
              ?: error("No resolved type dependency for $key on $type")
        }
    return if (arguments.isEmpty()) kotlinClass(type.rootClass)
    else kotlinClass(type.rootClass).parameterizedBy(arguments)
  }

  private fun ordinaryDependencies(type: Type): Map<Key, TypeDependency> =
      type.typeDependencies.associateBy(TypeDependency::key)

  private fun starProjected(klass: Class): TypeName {
    val root = kotlinClass(klass)
    val keys = klass.dependencies.keys
    return if (keys.isEmpty()) root else root.parameterizedBy(keys.map { STAR })
  }

  private fun kotlinClass(klass: Class): ClassName =
      checkNotNull(kotlinClasses[klass]) { "${klass.className} is not active in the class table" }

  private fun classSymbol(klass: Class): ClassName =
      checkNotNull(classSymbols[klass]) { "${klass.className} is not active in the class table" }

  private companion object {
    const val CLASS_SYMBOLS_CONTAINER = "PetsClasses"
    const val TYPE_PROPERTY = "type"
    val PETS_TYPE = ClassName("dev.martianzoo.types", "Type")
  }
}

internal fun generateCanonicalPetsTypes(options: PetsTypeGeneratorOptions): FileSpec {
  val standardOut = System.out
  val table =
      try {
        // Canon reports ignored data files with println; keep generated stdout valid Kotlin.
        System.setOut(System.err)
        ClassLoader(Canon).loadEverything()
      } finally {
        System.setOut(standardOut)
      }
  val petsClassNames =
      systemClassDeclarations.mapTo(linkedSetOf()) { it.className } +
          Canon.bundles.filterIsInstance<StandardFormBundle>().flatMapTo(linkedSetOf()) { bundle ->
            bundle.petsClassDeclarations.map { it.className }
          }
  val petsClasses = petsClassNames.mapTo(linkedSetOf(), table::getClass)
  return PetsTypeGenerator(table, options.packageName, options.fileName, petsClasses).generate()
}

internal fun parsePetsTypeGeneratorOptions(arguments: List<String>): PetsTypeGeneratorOptions {
  var packageName = "dev.martianzoo.generated"
  var fileName = "CanonicalPetsTypes"
  var output: Path? = null
  var index = 0
  while (index < arguments.size) {
    fun valueFor(option: String): String {
      require(index + 1 < arguments.size) { "$option requires a value" }
      return arguments[++index]
    }

    when (val argument = arguments[index]) {
      "--package" -> packageName = valueFor(argument)
      "--file-name" -> fileName = valueFor(argument)
      "--output" -> output = Path.of(valueFor(argument))
      else -> throw IllegalArgumentException("unknown argument: $argument\n$PETS_TYPES_USAGE")
    }
    index++
  }
  require(packageName.isNotBlank()) { "--package must not be blank" }
  require(fileName.isNotBlank()) { "--file-name must not be blank" }
  return PetsTypeGeneratorOptions(packageName, fileName, output)
}

private const val PETS_TYPES_USAGE =
    "usage: pets-type-generator [--package PACKAGE] [--file-name NAME] [--output FILE]"

public fun main(args: Array<String>) {
  try {
    val options = parsePetsTypeGeneratorOptions(args.toList())
    val generated = generateCanonicalPetsTypes(options)
    if (options.output == null) {
      print(generated)
    } else {
      options.output.parent?.let(Files::createDirectories)
      Files.newBufferedWriter(options.output).use(generated::writeTo)
    }
  } catch (e: IllegalArgumentException) {
    System.err.println(e.message)
    exitProcess(2)
  }
}
