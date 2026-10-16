package dev.martianzoo.state

import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Deferral
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameEvent.ChangeEvent.Cause
import dev.martianzoo.state.GameEvent.TaskAddedEvent
import dev.martianzoo.state.GameEvent.TaskEditedEvent
import dev.martianzoo.state.GameEvent.TaskRemovedEvent
import dev.martianzoo.state.Task.TaskId
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Exact event field codec nested inside [GameFileJson]; it does not define a standalone file. */
internal object EventLogJson {

  internal fun encodeEvents(events: List<GameEvent>): JsonArray {
    val tasks = mutableMapOf<TaskId, Task>()
    return buildJsonArray {
      events.forEach { event ->
        add(encodeEvent(event))
        if (event is GameEvent.TaskEvent) applyTaskEvent(tasks, event)
      }
    }
  }

  internal fun decodeEvents(source: JsonArray, premise: GamePremise): List<GameEvent> {
    val actorsByName = premise.actors.associateBy { it.className.toString() }
    return Decoder(premise.classTable) { encoded ->
          requireNotNull(actorsByName[encoded]) { "unknown Actor: $encoded" }
        }
        .decodeEvents(source)
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

  private fun encodeTask(task: Task): JsonObject = buildJsonObject {
    put("id", task.id.ordinal)
    put("assignee", task.assignee.className.toString())
    put("selected", task.selected)
    put("instruction", task.instruction.toString())
    put(
        "then",
        task.then?.instructions?.let { instructions ->
          buildJsonArray { instructions.forEach { add(JsonPrimitive(it.toString())) } }
        } ?: JsonNull,
    )
    put("cause", encodeCause(task.cause))
    put("deferral", task.deferral.name)
  }

  private fun encodeCause(cause: Cause?): JsonElement =
      cause?.let {
        buildJsonObject {
          put("context", it.context.toString())
          put("triggerEvent", it.triggerEvent)
        }
      } ?: JsonNull

  // Keep parsed values within this import and its ClassTable; different files share no cache.
  private class Decoder(
      private val classTable: ClassTable,
      private val decodeActor: (String) -> Actor,
  ) {
    private val components = mutableMapOf<String, Component>()
    private val instructions = mutableMapOf<String, Instruction>()
    private val expressions = mutableMapOf<String, Expression>()

    fun decodeEvents(source: JsonArray): List<GameEvent> {
      val tasks = mutableMapOf<TaskId, Task>()
      return source.mapIndexed { index, element ->
        try {
          decodeEvent(element.jsonObject, tasks).also { event ->
            if (event is GameEvent.TaskEvent) applyTaskEvent(tasks, event)
          }
        } catch (e: RuntimeException) {
          throw IllegalArgumentException("invalid event at index $index", e)
        }
      }
    }

    private fun decodeEvent(
        source: JsonObject,
        tasks: Map<TaskId, Task>,
    ): GameEvent {
      val event =
          when (requiredString(source, "event")) {
            "change" -> {
              ChangeEvent(
                  requiredInt(source, "ordinal"),
                  decodeActor(requiredString(source, "actor")),
                  decodeChange(requiredObject(source, "change")),
                  decodeCause(requiredElement(source, "cause")),
              )
            }
            "task-added" -> {
              TaskAddedEvent(
                  requiredInt(source, "ordinal"),
                  decodeTask(requiredObject(source, "task")),
              )
            }
            "task-removed" -> {
              TaskRemovedEvent(
                  requiredInt(source, "ordinal"),
                  tasks.getValue(TaskId(requiredInt(source, "taskId"))),
              )
            }
            "task-edited" -> {
              val task = decodeTask(requiredObject(source, "task"))
              TaskEditedEvent(requiredInt(source, "ordinal"), tasks.getValue(task.id), task)
            }
            else -> throw IllegalArgumentException("unknown event kind")
          }
      event.notes = nullableString(source, "notes")
      return event
    }

    private fun decodeChange(source: JsonObject): ComponentChange =
        when (requiredString(source, "kind")) {
          "gain" -> {
            ComponentChange.Gain(
                requiredInt(source, "count"),
                decodeComponent(requiredString(source, "component")),
            )
          }
          "remove" -> {
            ComponentChange.Remove(
                requiredInt(source, "count"),
                decodeComponent(requiredString(source, "component")),
            )
          }
          "transmute" -> {
            ComponentChange.Transmute(
                requiredInt(source, "count"),
                decodeComponent(requiredString(source, "gaining")),
                decodeComponent(requiredString(source, "removing")),
            )
          }
          else -> throw IllegalArgumentException("unknown component-change kind")
        }

    private fun decodeComponent(source: String): Component =
        components.getOrPut(source) { classTable.resolve(parse<Expression>(source)).toComponent() }

    private fun decodeTask(source: JsonObject): Task {
      val then =
          when (val encoded = requiredElement(source, "then")) {
            JsonNull -> null
            else ->
                InstructionGroup(
                    encoded.jsonArray.map { decodeInstruction(it.jsonPrimitive.content) }
                )
          }
      return Task(
          TaskId(requiredInt(source, "id")),
          assignee = decodeActor(requiredString(source, "assignee")),
          selected = requiredBoolean(source, "selected"),
          instruction = decodeInstruction(requiredString(source, "instruction")),
          then = then,
          cause = decodeCause(requiredElement(source, "cause")),
          deferral = Deferral.valueOf(requiredString(source, "deferral")),
      )
    }

    private fun decodeCause(source: JsonElement): Cause? {
      if (source == JsonNull) return null
      val cause = source.jsonObject
      return Cause(
          decodeExpression(requiredString(cause, "context")),
          requiredInt(cause, "triggerEvent"),
      )
    }

    private fun decodeInstruction(source: String): Instruction =
        instructions.getOrPut(source) { parse<Instruction>(source) }

    private fun decodeExpression(source: String): Expression =
        expressions.getOrPut(source) { parse<Expression>(source) }
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

  private fun requiredBoolean(source: JsonObject, name: String): Boolean =
      requiredElement(source, name).jsonPrimitive.boolean
}
