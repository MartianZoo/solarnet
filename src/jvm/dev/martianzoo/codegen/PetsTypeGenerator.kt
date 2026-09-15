package dev.martianzoo.codegen

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.NOTHING
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue
import dev.martianzoo.pets.ast.PropertyValue.AbsentRequirementValue
import dev.martianzoo.pets.ast.PropertyValue.MetricType
import dev.martianzoo.pets.ast.PropertyValue.MetricValue
import dev.martianzoo.pets.ast.PropertyValue.NumberType
import dev.martianzoo.pets.ast.PropertyValue.NumberValue
import dev.martianzoo.pets.ast.PropertyValue.OptionalRequirementType
import dev.martianzoo.pets.ast.PropertyValue.RequirementType
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.Dependency.Key
import dev.martianzoo.pets.types.Dependency.TypeDependency
import dev.martianzoo.pets.types.DependencySet.DependencyPath
import dev.martianzoo.pets.types.Type
import dev.martianzoo.tfm.canon.Canon
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.system.exitProcess

/** Generates a Kotlin type vocabulary from the fully resolved types in a Pets class table. */
internal class PetsTypeGenerator(
    private val table: ClassTable,
    private val packageName: String,
    private val filePrefix: String,
    classes: Set<Class> = table.allClasses(),
    private val generatedAt: Instant = Instant.now(),
) {
  internal data class Options(
      val packageName: String = "dev.martianzoo.generated",
      val filePrefix: String = "CanonicalPets",
      val outputDirectory: Path? = null,
  )

  private val classes = classes.toSet()
  private val orderedClasses: List<Class> by lazy(::hierarchyOrder)
  private val kotlinClasses: Map<Class, ClassName> = classes.associateWith {
    ClassName(packageName, it.className.toString())
  }
  private val cardClass: Class = table.getClass(cn("Card"))
  private val areaClass: Class = table.getClass(cn("Area"))
  private val milestoneClass: Class = table.getClass(cn("Milestone"))
  private val awardClass: Class = table.getClass(cn("Award"))

  init {
    val missingSuperclasses = classes.flatMap(Class::directSuperclasses).toSet() - classes
    require(missingSuperclasses.isEmpty()) {
      "Generated class set omits superclasses: ${missingSuperclasses.map(Class::className)}"
    }
  }

  fun generate(): List<FileSpec> =
      orderedClasses.groupBy(::fileName).map { (fileName, fileClasses) ->
        val file =
            FileSpec.builder(packageName, fileName)
                .addFileComment(
                    "Generated at %L from a resolved Pets ClassTable. Do not edit.",
                    generatedAt,
                )
                .addAnnotation(
                    AnnotationSpec.builder(Suppress::class)
                        .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                        .addMember("%S", "FINAL_UPPER_BOUND")
                        .build()
                )
        if (fileName == "${filePrefix}Types") {
          file.addFunction(generatedExpressionFunction())
          file.addFunction(
              generatedComponentFactory(
                  orderedClasses.filterNot(Class::abstract).map { kotlinClass(it) }
              )
          )
        }
        fileClasses.forEach { file.addType(typeSpec(it)) }
        file.build()
      }

  private fun fileName(klass: Class): String =
      when {
        !klass.abstract && klass.isSubtypeOf(cardClass) -> "${filePrefix}Cards"
        !klass.abstract && (klass.isSubtypeOf(milestoneClass) || klass.isSubtypeOf(awardClass)) ->
            "${filePrefix}Goals"
        !klass.abstract && klass.isSubtypeOf(areaClass) -> "${filePrefix}MapAreas"
        else -> "${filePrefix}Types"
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
    val properties = generatedProperties(klass)
    val authoredEffects = generatedAuthoredEffects(klass)
    val builder =
        if (klass.abstract) {
          TypeSpec.interfaceBuilder(klass.className.toString()).addModifiers(KModifier.SEALED)
        } else {
          TypeSpec.classBuilder(klass.className.toString())
        }
    builder.addModifiers(KModifier.PUBLIC)
    if (klass == table.componentClass) {
      builder.addSuperinterface(PETS_HAS_EXPRESSION)
      builder.addProperty(
          PropertySpec.builder(EXPRESSION_PROPERTY, PETS_EXPRESSION)
              .addModifiers(KModifier.PUBLIC, KModifier.ABSTRACT, KModifier.OVERRIDE)
              .build()
      )
      builder.addFunction(
          FunSpec.builder("toString")
              .addModifiers(KModifier.PUBLIC, KModifier.ABSTRACT, KModifier.OVERRIDE)
              .returns(STRING)
              .build()
      )
      builder.addProperty(authoredEffectsContract())
    } else if (!klass.abstract) {
      builder.primaryConstructor(
          FunSpec.constructorBuilder()
              .addAnnotation(PublishedApi::class)
              .addModifiers(KModifier.INTERNAL)
              .addParameter(EXPRESSION_PROPERTY, PETS_EXPRESSION)
              .build()
      )
      builder.addProperty(
          PropertySpec.builder(EXPRESSION_PROPERTY, PETS_EXPRESSION)
              .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
              .initializer(EXPRESSION_PROPERTY)
              .build()
      )
      builder.addFunction(
          FunSpec.builder("toString")
              .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
              .returns(STRING)
              .addStatement("return %N.toString()", EXPRESSION_PROPERTY)
              .build()
      )
    }
    properties.forEach { builder.addProperty(it.member) }
    authoredEffects?.let { builder.addProperty(it.first) }
    if (klass == table.classClass) {
      builder.addSuperinterface(ClassName("dev.martianzoo.pets", "HasClassName"))
      builder.addProperty(representedClassNameProperty())
    }
    val companionProperties =
        properties.mapNotNull(GeneratedProperty::companion) + listOfNotNull(authoredEffects?.second)
    if (!klass.abstract || companionProperties.isNotEmpty()) {
      builder.addType(companion(klass, variables.declarations, companionProperties))
    }
    variables.declarations.forEach(builder::addTypeVariable)
    klass.directSuperclasses.forEach { superclass ->
      val resolvedSupertype = klass.baseType.asSupertype(superclass)
      builder.addSuperinterface(typeName(resolvedSupertype, variables.byPath))
    }
    return builder.build()
  }

  private data class GeneratedProperty(
      val member: PropertySpec,
      val companion: PropertySpec? = null,
  )

  private enum class PropertyKind {
    NUMBER,
    METRIC,
    REQUIREMENT,
    OPTIONAL_REQUIREMENT,
  }

  private fun generatedProperties(klass: Class): List<GeneratedProperty> =
      if (klass.abstract) abstractProperties(klass) else concreteProperties(klass)

  private fun abstractProperties(klass: Class): List<GeneratedProperty> =
      klass.declaration.properties.mapNotNull { (name, declared) ->
        val inherited = klass.directSuperclasses.any { name in it.properties }
        if (declared.abstract && inherited) return@mapNotNull null
        val origin = propertyOrigin(klass, name)
        if (declared.abstract) {
          GeneratedProperty(
              PropertySpec.builder(name.value, propertyType(origin, name))
                  .addModifiers(KModifier.PUBLIC, KModifier.ABSTRACT)
                  .build()
          )
        } else {
          concreteProperty(klass, name, declared, inherited, interfaceMember = true)
        }
      }

  private fun concreteProperties(klass: Class): List<GeneratedProperty> =
      klass.properties.mapNotNull { (name, value) ->
        val source = propertySource(klass, name)
        val declaredAtSource = source.declaration.properties.getValue(name)
        if (source.abstract && !declaredAtSource.abstract) return@mapNotNull null
        concreteProperty(
            klass,
            name,
            value,
            inherited = klass.directSuperclasses.any { name in it.properties },
            interfaceMember = false,
        )
      }

  private fun concreteProperty(
      klass: Class,
      name: PropertyName,
      value: PropertyValue,
      inherited: Boolean,
      interfaceMember: Boolean,
  ): GeneratedProperty {
    val origin = propertyOrigin(klass, name)
    val kind = propertyKind(origin.declaration.properties.getValue(name))
    val type = propertyType(kind)
    val parsed = parsedPropertyValue(name, kind, value)
    val initializer = parsed?.first ?: literalPropertyValue(kind, value)
    val builder = PropertySpec.builder(name.value, type).addModifiers(KModifier.PUBLIC)
    if (inherited) builder.addModifiers(KModifier.OVERRIDE)
    if (interfaceMember) {
      builder.getter(FunSpec.getterBuilder().addStatement("return %L", initializer).build())
    } else {
      builder.initializer(initializer)
    }
    return GeneratedProperty(builder.build(), parsed?.second)
  }

  private fun parsedPropertyValue(
      name: PropertyName,
      kind: PropertyKind,
      value: PropertyValue,
  ): Pair<CodeBlock, PropertySpec>? {
    val parsedType =
        when (kind) {
          PropertyKind.METRIC -> PETS_METRIC
          PropertyKind.REQUIREMENT,
          PropertyKind.OPTIONAL_REQUIREMENT -> PETS_REQUIREMENT
          PropertyKind.NUMBER -> return null
        }
    val source =
        when (value) {
          is NumberValue -> value.toString()
          is MetricValue -> value.value.toString()
          is RequirementValue -> value.value.toString()
          AbsentRequirementValue -> return null
          else -> error("Property $name is not concrete: $value")
        }
    val backingName = "parsed${name.value.replaceFirstChar(Char::uppercaseChar)}"
    val backing =
        PropertySpec.builder(backingName, parsedType)
            .addModifiers(KModifier.PRIVATE)
            .initializer("%T.parse<%T>(%S)", PETS_PARSING, parsedType, source)
            .build()
    return CodeBlock.of("%N", backingName) to backing
  }

  private fun literalPropertyValue(kind: PropertyKind, value: PropertyValue): CodeBlock =
      when (kind) {
        PropertyKind.NUMBER ->
            CodeBlock.of("%L", (value as? NumberValue)?.value ?: error("Expected Number: $value"))
        PropertyKind.OPTIONAL_REQUIREMENT -> {
          check(value === AbsentRequirementValue) { "Expected absent Requirement: $value" }
          CodeBlock.of("null")
        }
        PropertyKind.METRIC,
        PropertyKind.REQUIREMENT -> error("Expected parsed property value: $value")
      }

  private val propertyOrigins = mutableMapOf<Pair<Class, PropertyName>, Class>()

  private fun propertyOrigin(klass: Class, name: PropertyName): Class =
      propertyOrigins.getOrPut(klass to name) {
        if (
            name in klass.declaration.properties &&
                klass.directSuperclasses.none { name in it.properties }
        ) {
          klass
        } else {
          klass.directSuperclasses
              .filter { name in it.properties }
              .map { propertyOrigin(it, name) }
              .distinct()
              .single()
        }
      }

  private val propertySources = mutableMapOf<Pair<Class, PropertyName>, Class>()

  private fun propertySource(klass: Class, name: PropertyName): Class =
      propertySources.getOrPut(klass to name) {
        if (name in klass.declaration.properties) {
          klass
        } else {
          val candidates =
              klass.directSuperclasses
                  .filter { name in it.properties }
                  .map { propertySource(it, name) }
                  .distinct()
          candidates.singleOrNull()
              ?: candidates.single { candidate ->
                candidates.all { other -> candidate.isSubtypeOf(other) }
              }
        }
      }

  private fun propertyType(origin: Class, name: PropertyName): TypeName =
      propertyType(propertyKind(origin.declaration.properties.getValue(name)))

  private fun propertyType(kind: PropertyKind): TypeName =
      when (kind) {
        PropertyKind.NUMBER -> INT
        PropertyKind.METRIC -> PETS_METRIC
        PropertyKind.REQUIREMENT -> PETS_REQUIREMENT
        PropertyKind.OPTIONAL_REQUIREMENT -> PETS_REQUIREMENT.copy(nullable = true)
      }

  private fun propertyKind(value: PropertyValue): PropertyKind =
      when (value) {
        NumberType,
        is NumberValue -> PropertyKind.NUMBER
        MetricType,
        is MetricValue -> PropertyKind.METRIC
        RequirementType,
        is RequirementValue -> PropertyKind.REQUIREMENT
        OptionalRequirementType,
        AbsentRequirementValue -> PropertyKind.OPTIONAL_REQUIREMENT
      }

  private fun companion(
      klass: Class,
      classVariables: List<TypeVariableName>,
      properties: List<PropertySpec>,
  ): TypeSpec {
    val builder = TypeSpec.companionObjectBuilder()
    properties.forEach(builder::addProperty)
    if (klass.abstract) return builder.build()

    builder.addProperty(
        PropertySpec.builder("className", PETS_CLASS_NAME)
            .addModifiers(KModifier.PUBLIC)
            .initializer("%T.cn(%S)", PETS_CLASS_NAME, klass.className.toString())
            .build()
    )
    val representedType = starProjected(klass)
    builder.addFunction(
        generatedExpressionAdapter(
            klass.className.toString(),
            kotlinClass(klass),
            classVariables.size,
            representedType,
        )
    )
    val classLiteralType = kotlinClass(table.classClass).parameterizedBy(representedType)
    builder.addProperty(
        PropertySpec.builder("c", classLiteralType)
            .addModifiers(KModifier.PUBLIC)
            .initializer(
                "%T(%N(%M<%T>()))",
                classLiteralType,
                GENERATED_EXPRESSION,
                TYPE_OF,
                classLiteralType,
            )
            .build()
    )

    val factoryVariables = classVariables.map { variable ->
      TypeVariableName(variable.name, variable.bounds).copy(reified = true)
    }
    val generatedType =
        if (factoryVariables.isEmpty()) kotlinClass(klass)
        else kotlinClass(klass).parameterizedBy(factoryVariables)
    val factory =
        FunSpec.builder("invoke")
            .addModifiers(KModifier.PUBLIC, KModifier.OPERATOR)
            .returns(generatedType)
            .addStatement(
                "return %T(%N(%M<%T>()))",
                generatedType,
                GENERATED_EXPRESSION,
                TYPE_OF,
                generatedType,
            )
    if (factoryVariables.isNotEmpty()) {
      factory.addModifiers(KModifier.INLINE)
      factoryVariables.forEach(factory::addTypeVariable)
    }
    return builder.addFunction(factory.build()).build()
  }

  private fun generatedExpressionFunction(): FunSpec =
      FunSpec.builder(GENERATED_EXPRESSION)
          .addAnnotation(PublishedApi::class)
          .addModifiers(KModifier.INTERNAL)
          .addParameter("type", KTYPE)
          .returns(PETS_EXPRESSION)
          .addCode(
              """
              val classifier = type.classifier as? %T<*>
                  ?: error("Generated Pets type has no class classifier: ${'$'}type")
              val classifierName =
                  classifier.simpleName
                      ?: error("Generated Pets class has no simple name: ${'$'}classifier")
              val arguments =
                  if (classifier == %T::class) {
                    val representedType =
                        type.arguments.single().type
                            ?: error("A Pets class literal cannot be star-projected")
                    val representedClass = representedType.classifier as? %T<*>
                        ?: error("Represented Pets type has no class classifier: ${'$'}representedType")
                    val representedName =
                        representedClass.simpleName
                            ?: error("Represented Pets class has no simple name: ${'$'}representedClass")
                    listOf(%T.cn(representedName).of())
                  } else {
                    type.arguments.map { projection ->
                      val argumentType =
                          projection.type
                              ?: error("A component dependency cannot be star-projected in ${'$'}type")
                      %N(argumentType)
                    }
                  }
              return %T.cn(classifierName).of(arguments)
              """
                  .trimIndent(),
              KCLASS,
              kotlinClass(table.classClass),
              KCLASS,
              PETS_CLASS_NAME,
              GENERATED_EXPRESSION,
              PETS_CLASS_NAME,
          )
          .build()

  private data class GeneratedTypeVariables(
      val declarations: List<TypeVariableName>,
      val byPath: Map<DependencyPath, TypeVariableName>,
  )

  private data class VariableLayout(
      val identities: List<DependencyPath>,
      val identityByPath: Map<DependencyPath, DependencyPath>,
  )

  private val variableLayouts = mutableMapOf<Class, VariableLayout>()

  private fun typeVariables(klass: Class): GeneratedTypeVariables {
    val resolved = ordinaryDependencies(klass.baseType)
    val layout = variableLayout(klass)
    val rootPathSet = klass.dependencies.keys.mapTo(linkedSetOf()) { DependencyPath(listOf(it)) }
    val flattened = klass.baseType.dependencies.flatten()
    val boundClasses =
        layout.identities.associateWith { identity ->
          flattened[identity]
              ?: checkNotNull(if (klass == table.classClass) table.componentClass else null) {
                "No resolved dependency at $identity on ${klass.className}"
              }
        }
    val names = typeVariableNames(layout.identities, boundClasses)
    val provisionalByIdentity =
        layout.identities.associateWith { identity ->
          TypeVariableName(names.getValue(identity), variance = KModifier.OUT)
        }
    val provisionalByPath =
        layout.identityByPath.mapValues { provisionalByIdentity.getValue(it.value) }
    val resolvedByIdentity =
        layout.identities.associateWith { identity ->
          val rootKey = identity.keyList.singleOrNull()?.takeIf { identity in rootPathSet }
          val boundName =
              if (rootKey == null) {
                starProjected(boundClasses.getValue(identity))
              } else {
                val bound =
                    resolved[rootKey]?.boundType
                        ?: checkNotNull(
                            if (klass == table.classClass) table.componentClass.baseType else null
                        ) {
                          "No resolved type dependency for $rootKey on ${klass.className}"
                        }
                if (klass == table.classClass) kotlinClass(table.componentClass)
                else typeName(bound, provisionalByPath, identity, setOf(klass))
              }
          provisionalByIdentity.getValue(identity).copy(bounds = listOf(boundName))
        }
    return GeneratedTypeVariables(
        layout.identities.map(resolvedByIdentity::getValue),
        layout.identityByPath.mapValues { resolvedByIdentity.getValue(it.value) },
    )
  }

  private fun variableLayout(klass: Class): VariableLayout =
      variableLayouts.getOrPut(klass) { createVariableLayout(klass) }

  private fun createVariableLayout(klass: Class): VariableLayout {
    val resolvedDependencies = ordinaryDependencies(klass.baseType)
    val fixedRootKeys =
        resolvedDependencies.values
            .filterTo(linkedSetOf()) { dependency -> !dependency.boundType.abstract }
            .mapTo(linkedSetOf(), TypeDependency::key)
    val allRootPaths = klass.dependencies.keys.map { DependencyPath(listOf(it)) }
    val allRootPathSet = allRootPaths.toSet()
    val fixedRootPaths =
        allRootPaths.filterTo(linkedSetOf()) { it.keyList.single() in fixedRootKeys }
    val linkageGroups = klass.dependencyLinkages
    val promotedClassLiteralGroups = promoteClassLiteralLinkages(klass, linkageGroups)

    val parent = allRootPaths.associateWithTo(mutableMapOf()) { it }
    fun representative(path: DependencyPath): DependencyPath {
      val current = parent.getValue(path)
      if (current == path) return path
      val result = representative(current)
      parent[path] = result
      return result
    }
    fun union(paths: List<DependencyPath>) {
      val first = paths.firstOrNull() ?: return
      paths.drop(1).forEach { path -> parent[representative(path)] = representative(first) }
    }
    (linkageGroups + promotedClassLiteralGroups).forEach { group ->
      union(group.filter { it in allRootPathSet })
    }

    val rootsByIdentity = allRootPaths.groupBy(::representative)
    val variableRoots = allRootPaths.filter { root ->
      rootsByIdentity.getValue(representative(root)).any { it !in fixedRootPaths }
    }
    val variableRootSet = variableRoots.toSet()

    val identityByPath = mutableMapOf<DependencyPath, DependencyPath>()
    variableRoots.forEach { identityByPath[it] = representative(it) }
    val extraIdentities = mutableListOf<DependencyPath>()
    linkageGroups.forEach { group ->
      val directRoots = group.filter { it in variableRootSet }
      if (directRoots.isNotEmpty()) {
        val identity = representative(directRoots.first())
        group.forEach { identityByPath[it] = identity }
      } else {
        val enclosingIdentities = group.mapNotNull { path ->
          variableRoots
              .singleOrNull { it.keyList.single() == path.keyList.first() }
              ?.let(::representative)
        }
        if (enclosingIdentities.isEmpty()) return@forEach
        if (enclosingIdentities.distinct().size > 1) {
          val identity = group.minBy { it.toString() }
          extraIdentities += identity
          group.forEach { identityByPath[it] = identity }
        }
      }
    }

    return VariableLayout(
        (extraIdentities + variableRoots.map(::representative)).distinct(),
        identityByPath,
    )
  }

  private fun promoteClassLiteralLinkages(
      klass: Class,
      linkageGroups: List<Set<DependencyPath>>,
  ): List<Set<DependencyPath>> {
    val classDependencyKey = table.classClass.dependencies.keys.single()
    val flattened = klass.baseType.dependencies.flatten()
    return linkageGroups.mapNotNull { group ->
      if (
          group.size < 2 ||
              group.any { it.keyList.size < 2 || it.keyList.last() != classDependencyKey }
      ) {
        return@mapNotNull null
      }
      val parents = group.map { DependencyPath(it.keyList.dropLast(1)) }.toSet()
      parents.takeIf { paths -> paths.all { flattened[it] == table.classClass } }
    }
  }

  private fun typeName(
      type: Type,
      variables: Map<DependencyPath, TypeVariableName>,
      path: DependencyPath? = null,
      visiting: Set<Class> = emptySet(),
  ): TypeName {
    if (type.rootClass == table.classClass) {
      val representedName = type.expressionFull.arguments.single().className
      val represented = table.getClass(representedName)
      val nonrecursiveRoot =
          if (represented in visiting || referencesAny(represented.baseType, visiting)) {
            represented.directSuperclasses.firstOrNull { it !in visiting } ?: table.componentClass
          } else {
            represented
          }
      val representedPath = path?.let {
        DependencyPath(it.keyList + table.classClass.dependencies.keys.single())
      }
      val representedType = representedPath?.let(variables::get) ?: starProjected(nonrecursiveRoot)
      return kotlinClass(type.rootClass).parameterizedBy(representedType)
    }
    if (type.rootClass in visiting) return starProjected(type.rootClass)

    val arguments =
        variableLayout(type.rootClass).identities.map { identity ->
          typeArgumentAtPath(
              type,
              identity.keyList,
              variables,
              path?.keyList.orEmpty(),
              visiting + type.rootClass,
          )
        }
    return if (arguments.isEmpty()) kotlinClass(type.rootClass)
    else kotlinClass(type.rootClass).parameterizedBy(arguments)
  }

  private fun typeArgumentAtPath(
      type: Type,
      remainingKeys: List<Key>,
      variables: Map<DependencyPath, TypeVariableName>,
      prefix: List<Key>,
      visiting: Set<Class>,
  ): TypeName {
    val fullPath = DependencyPath(prefix + remainingKeys)
    variables[fullPath]?.let {
      return it
    }
    val key = remainingKeys.first()
    val dependency = type.dependencies.at(DependencyPath(listOf(key)))
    val dependencyPath = DependencyPath(prefix + key)
    if (remainingKeys.size == 1) {
      return if (dependency is TypeDependency) {
        typeName(dependency.boundType, variables, dependencyPath, visiting)
      } else {
        starProjected(table.getClass(dependency.className))
      }
    }
    check(dependency is TypeDependency) { "Cannot descend through $dependency at $dependencyPath" }
    return typeArgumentAtPath(
        dependency.boundType,
        remainingKeys.drop(1),
        variables,
        dependencyPath.keyList,
        visiting + dependency.boundType.rootClass,
    )
  }

  private fun referencesAny(
      type: Type,
      candidates: Set<Class>,
      seen: MutableSet<Type> = mutableSetOf(),
  ): Boolean {
    if (!seen.add(type)) return false
    if (type.rootClass in candidates || type.representedClass in candidates) return true
    return type.typeDependencies.any { referencesAny(it.boundType, candidates, seen) }
  }

  private fun ordinaryDependencies(type: Type): Map<Key, TypeDependency> =
      type.typeDependencies.associateBy(TypeDependency::key)

  private fun starProjected(klass: Class): TypeName {
    val root = kotlinClass(klass)
    val parameterCount = variableLayout(klass).identities.size
    return if (parameterCount == 0) root else root.parameterizedBy(List(parameterCount) { STAR })
  }

  private fun kotlinClass(klass: Class): ClassName =
      checkNotNull(kotlinClasses[klass]) { "${klass.className} is not active in the class table" }

  private companion object {
    const val EXPRESSION_PROPERTY = "expression"
    const val GENERATED_EXPRESSION = "generatedPetsExpression"
    val KCLASS = ClassName("kotlin.reflect", "KClass")
    val KTYPE = ClassName("kotlin.reflect", "KType")
    val PETS_CLASS_NAME = ClassName("dev.martianzoo.pets.ast", "ClassName")
    val PETS_EXPRESSION = ClassName("dev.martianzoo.pets.ast", "Expression")
    val PETS_HAS_EXPRESSION = ClassName("dev.martianzoo.pets", "HasExpression")
    val PETS_METRIC = ClassName("dev.martianzoo.pets.ast", "Metric")
    val PETS_PARSING = ClassName("dev.martianzoo.pets", "Parsing")
    val PETS_REQUIREMENT = ClassName("dev.martianzoo.pets.ast", "Requirement")
    val TYPE_OF = MemberName("kotlin.reflect", "typeOf")
  }
}

private fun authoredEffectsContract(): PropertySpec {
  val effect = ClassName("dev.martianzoo.pets.ast", "Effect")
  val effectList = ClassName("kotlin.collections", "List").parameterizedBy(effect)
  return PropertySpec.builder("_authoredEffects", effectList)
      .addModifiers(KModifier.PUBLIC)
      .getter(FunSpec.getterBuilder().addStatement("return emptyList()").build())
      .build()
}

private fun representedClassNameProperty(): PropertySpec =
    PropertySpec.builder("className", ClassName("dev.martianzoo.pets.ast", "ClassName"))
        .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
        .getter(
            FunSpec.getterBuilder()
                .addStatement("return expression.arguments.single().className")
                .build()
        )
        .build()

private fun generatedAuthoredEffects(klass: Class): Pair<PropertySpec, PropertySpec>? {
  if (klass.abstract || klass.declaration.authoredEffects.isEmpty()) return null
  val effect = ClassName("dev.martianzoo.pets.ast", "Effect")
  val effectList = ClassName("kotlin.collections", "List").parameterizedBy(effect)
  val parsing = ClassName("dev.martianzoo.pets", "Parsing")
  val initializer = CodeBlock.builder().add("listOf(\n").indent()
  klass.declaration.authoredEffects.forEach { authored ->
    initializer.add("%T.parse<%T>(%S),\n", parsing, effect, authored.toString())
  }
  initializer.unindent().add(")")
  val backing =
      PropertySpec.builder("parsedAuthoredEffects", effectList)
          .addModifiers(KModifier.PRIVATE)
          .initializer(initializer.build())
          .build()
  val member =
      PropertySpec.builder("_authoredEffects", effectList)
          .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
          .getter(FunSpec.getterBuilder().addStatement("return %N", backing).build())
          .build()
  return member to backing
}

private fun typeVariableNames(
    identities: List<DependencyPath>,
    boundClasses: Map<DependencyPath, Class>,
): Map<DependencyPath, String> {
  val abbreviations = identities.associateWith { boundClasses.getValue(it).abbreviation() }
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

private fun Class.abbreviation(): String =
    className.toString().filter(Char::isUpperCase).ifEmpty { className.toString().take(1) }

private fun generatedExpressionAdapter(
    petsClassName: String,
    kotlinClass: ClassName,
    typeVariableCount: Int,
    representedType: TypeName,
): FunSpec {
  val constructorType =
      if (typeVariableCount == 0) kotlinClass
      else kotlinClass.parameterizedBy(List(typeVariableCount) { NOTHING })
  return FunSpec.builder("fromExpression")
      .addModifiers(KModifier.PUBLIC)
      .addParameter("expression", ClassName("dev.martianzoo.pets.ast", "Expression"))
      .returns(representedType)
      .addStatement(
          "require(expression.className == className) { %P }",
          "Expected $petsClassName expression, got \$expression",
      )
      .addStatement("return %T(expression)", constructorType)
      .build()
}

private fun generatedComponentFactory(classes: List<ClassName>): FunSpec {
  val expression = "expression"
  val body = CodeBlock.builder().beginControlFlow("return when (%N.className)", expression)
  classes.forEach { type ->
    body.addStatement("%T.className -> %T.fromExpression(%N)", type, type, expression)
  }
  body.addStatement("else -> error(%P)", "No generated Pets class for \$expression")
  body.endControlFlow()
  return FunSpec.builder("generatedPetsComponent")
      .addModifiers(KModifier.PUBLIC)
      .addParameter(expression, ClassName("dev.martianzoo.pets.ast", "Expression"))
      .returns(ClassName("dev.martianzoo.pets", "HasExpression"))
      .addCode(body.build())
      .build()
}

internal fun generateCanonicalPetsTypes(options: PetsTypeGenerator.Options): List<FileSpec> =
    PetsTypeGenerator(Canon.classTable, options.packageName, options.filePrefix).generate()

internal fun parsePetsTypeGeneratorOptions(arguments: List<String>): PetsTypeGenerator.Options {
  var packageName = "dev.martianzoo.generated"
  var filePrefix = "CanonicalPets"
  var outputDirectory: Path? = null
  var index = 0
  while (index < arguments.size) {
    fun valueFor(option: String): String {
      require(index + 1 < arguments.size) { "$option requires a value" }
      return arguments[++index]
    }

    when (val argument = arguments[index]) {
      "--package" -> packageName = valueFor(argument)
      "--file-prefix" -> filePrefix = valueFor(argument)
      "--output-dir" -> outputDirectory = Path.of(valueFor(argument))
      else -> throw IllegalArgumentException("unknown argument: $argument\n$PETS_TYPES_USAGE")
    }
    index++
  }
  require(packageName.isNotBlank()) { "--package must not be blank" }
  require(filePrefix.isNotBlank()) { "--file-prefix must not be blank" }
  return PetsTypeGenerator.Options(packageName, filePrefix, outputDirectory)
}

private const val PETS_TYPES_USAGE =
    "usage: pets-type-generator [--package PACKAGE] [--file-prefix PREFIX] " +
        "[--output-dir DIRECTORY]"

public fun main(args: Array<String>) {
  try {
    val options = parsePetsTypeGeneratorOptions(args.toList())
    val generatedFiles = generateCanonicalPetsTypes(options)
    if (options.outputDirectory == null) {
      generatedFiles.forEach { file ->
        println("// ${file.name}.kt")
        print(file)
      }
    } else {
      Files.createDirectories(options.outputDirectory)
      generatedFiles.forEach { file ->
        val output = options.outputDirectory.resolve("${file.name}.kt")
        Files.newBufferedWriter(output).use(file::writeTo)
      }
    }
  } catch (e: IllegalArgumentException) {
    System.err.println(e.message)
    exitProcess(2)
  }
}
