package dev.martianzoo.state

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.GamePremise
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameEvent.ChangeEvent.Cause
import dev.martianzoo.state.GameEvent.TaskAddedEvent
import dev.martianzoo.state.GameEvent.TaskEditedEvent
import dev.martianzoo.state.GameEvent.TaskRemovedEvent
import dev.martianzoo.state.Task.Selection
import dev.martianzoo.state.Task.TaskId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Opaque JSON encoding for an exact sequence of [GameEvent] values. */
public object EventLogJson {
  private val json = Json

  /** Encodes [events] without embedding the Catalog needed to interpret their component Types. */
  public fun encode(events: List<GameEvent>): String =
      json.encodeToString(JsonArray.serializer(), encodeEvents(events))

  /** Decodes events against the [classTable] that gives their component Types meaning. */
  public fun decode(text: String, classTable: ClassTable): List<GameEvent> =
      decodeEvents(json.parseToJsonElement(text).jsonArray, classTable)

  internal fun encodeEvents(events: List<GameEvent>): JsonArray {
    val tasks = mutableMapOf<TaskId, Task>()
    return buildJsonArray {
      events.forEach { event ->
        add(encodeEvent(event))
        if (event is GameEvent.TaskEvent) applyTaskEvent(tasks, event)
      }
    }
  }

  internal fun decodeEvents(source: JsonArray, classTable: ClassTable): List<GameEvent> =
      decodeEvents(source, classTable, ::decodeActor)

  internal fun decodeEvents(source: JsonArray, premise: GamePremise): List<GameEvent> {
    val actorsByName = premise.actors.associateBy { it.className.toString() }
    return decodeEvents(source, premise.classTable) { encoded ->
      requireNotNull(actorsByName[encoded]) { "unknown Actor: $encoded" }
    }
  }

  private fun decodeEvents(
      source: JsonArray,
      classTable: ClassTable,
      decodeActor: (String) -> Actor,
  ): List<GameEvent> {
    val tasks = mutableMapOf<TaskId, Task>()
    return source.mapIndexed { index, element ->
      try {
        decodeEvent(element.jsonObject, classTable, tasks, decodeActor).also { event ->
          if (event is GameEvent.TaskEvent) applyTaskEvent(tasks, event)
        }
      } catch (e: RuntimeException) {
        throw IllegalArgumentException("invalid event at index $index", e)
      }
    }
  }

  private fun encodeEvent(event: GameEvent): JsonObject = buildJsonObject {
    when (event) {
      is ChangeEvent -> {
        put("event", "change")
        put("ordinal", event.ordinal)
        put("actor", event.actor.className.toString())
        put("change", encodeChange(event.change))
        put("cause", encodeCause(event.cause))
      }
      is TaskAddedEvent -> {
        put("event", "task-added")
        put("ordinal", event.ordinal)
        put("task", encodeTask(event.task))
      }
      is TaskRemovedEvent -> {
        put("event", "task-removed")
        put("ordinal", event.ordinal)
        put("taskId", event.task.id.ordinal)
      }
      is TaskEditedEvent -> {
        put("event", "task-edited")
        put("ordinal", event.ordinal)
        put("task", encodeTask(event.task))
      }
    }
    put("notes", event.notes?.let(::JsonPrimitive) ?: JsonNull)
  }

  private fun decodeEvent(
      source: JsonObject,
      classTable: ClassTable,
      tasks: Map<TaskId, Task>,
      decodeActor: (String) -> Actor,
  ): GameEvent {
    val event =
        when (requiredString(source, "event")) {
          "change" -> {
            ChangeEvent(
                requiredInt(source, "ordinal"),
                decodeActor(requiredString(source, "actor")),
                decodeChange(requiredObject(source, "change"), classTable),
                decodeCause(requiredElement(source, "cause")),
            )
          }
          "task-added" -> {
            TaskAddedEvent(
                requiredInt(source, "ordinal"),
                decodeTask(requiredObject(source, "task"), decodeActor),
            )
          }
          "task-removed" -> {
            TaskRemovedEvent(
                requiredInt(source, "ordinal"),
                tasks.getValue(TaskId(requiredInt(source, "taskId"))),
            )
          }
          "task-edited" -> {
            val task = decodeTask(requiredObject(source, "task"), decodeActor)
            TaskEditedEvent(requiredInt(source, "ordinal"), tasks.getValue(task.id), task)
          }
          else -> throw IllegalArgumentException("unknown event kind")
        }
    event.notes = nullableString(source, "notes")
    return event
  }

  private fun applyTaskEvent(tasks: MutableMap<TaskId, Task>, event: GameEvent.TaskEvent) {
    when (event) {
      is TaskAddedEvent -> require(tasks.put(event.task.id, event.task) == null)
      is TaskRemovedEvent -> require(tasks.remove(event.task.id) == event.task)
      is TaskEditedEvent -> {
        require(tasks[event.task.id] == event.oldTask)
        tasks[event.task.id] = event.task
      }
    }
  }

  private fun encodeChange(change: ComponentChange): JsonObject = buildJsonObject {
    when (change) {
      is ComponentChange.Gain -> {
        put("kind", "gain")
        put("count", change.count)
        put("component", change.component.expressionFull.toString())
      }
      is ComponentChange.Remove -> {
        put("kind", "remove")
        put("count", change.count)
        put("component", change.component.expressionFull.toString())
      }
      is ComponentChange.Transmute -> {
        put("kind", "transmute")
        put("count", change.count)
        put("gaining", change.gaining.expressionFull.toString())
        put("removing", change.removing.expressionFull.toString())
      }
    }
  }

  private fun decodeChange(source: JsonObject, classTable: ClassTable): ComponentChange =
      when (requiredString(source, "kind")) {
        "gain" -> {
          ComponentChange.Gain(
              requiredInt(source, "count"),
              decodeComponent(requiredString(source, "component"), classTable),
          )
        }
        "remove" -> {
          ComponentChange.Remove(
              requiredInt(source, "count"),
              decodeComponent(requiredString(source, "component"), classTable),
          )
        }
        "transmute" -> {
          ComponentChange.Transmute(
              requiredInt(source, "count"),
              decodeComponent(requiredString(source, "gaining"), classTable),
              decodeComponent(requiredString(source, "removing"), classTable),
          )
        }
        else -> throw IllegalArgumentException("unknown component-change kind")
      }

  private fun decodeComponent(source: String, classTable: ClassTable): Component =
      classTable.resolve(parse<Expression>(source)).toComponent()

  private fun encodeTask(task: Task): JsonObject = buildJsonObject {
    put("id", task.id.ordinal)
    put("controller", task.controller.className.toString())
    put("actor", task.actor.className.toString())
    put("selection", task.selection.name)
    put("instruction", task.instruction.toString())
    put(
        "then",
        task.then?.instructions?.let { instructions ->
          buildJsonArray { instructions.forEach { add(JsonPrimitive(it.toString())) } }
        } ?: JsonNull,
    )
    put("cause", encodeCause(task.cause))
  }

  private fun decodeTask(source: JsonObject, decodeActor: (String) -> Actor): Task {
    val then =
        when (val encoded = requiredElement(source, "then")) {
          JsonNull -> null
          else ->
              InstructionGroup(
                  encoded.jsonArray.map { parse<Instruction>(it.jsonPrimitive.content) }
              )
        }
    return Task(
        TaskId(requiredInt(source, "id")),
        decodeActor(requiredString(source, "controller")),
        decodeActor(requiredString(source, "actor")),
        Selection.valueOf(requiredString(source, "selection")),
        parse(requiredString(source, "instruction")),
        then,
        decodeCause(requiredElement(source, "cause")),
    )
  }

  private fun decodeActor(source: String): Actor {
    val className = parse<ClassName>(source)
    return if (className == Actor.ADMIN.className) Actor.ADMIN else Player(className)
  }

  private fun encodeCause(cause: Cause?): JsonElement =
      cause?.let {
        buildJsonObject {
          put("context", it.context.toString())
          put("triggerEvent", it.triggerEvent)
        }
      } ?: JsonNull

  private fun decodeCause(source: JsonElement): Cause? {
    if (source == JsonNull) return null
    val cause = source.jsonObject
    return Cause(
        parse(requiredString(cause, "context")),
        requiredInt(cause, "triggerEvent"),
    )
  }

  private fun requiredElement(source: JsonObject, name: String): JsonElement =
      requireNotNull(source[name]) { "missing field: $name" }

  private fun requiredObject(source: JsonObject, name: String): JsonObject =
      requiredElement(source, name).jsonObject

  private fun requiredString(source: JsonObject, name: String): String =
      requiredElement(source, name).jsonPrimitive.let {
        require(it.isString) { "field $name must be a string" }
        it.content
      }

  private fun nullableString(source: JsonObject, name: String): String? =
      when (val value = requiredElement(source, name)) {
        JsonNull -> null
        else ->
            value.jsonPrimitive.let {
              require(it.isString) { "field $name must be a string or null" }
              it.content
            }
      }

  private fun requiredInt(source: JsonObject, name: String): Int =
      requiredElement(source, name).jsonPrimitive.int
}
