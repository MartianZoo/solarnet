package dev.martianzoo.tfm.web.playground

import dev.martianzoo.agent.AutoExecPolicy
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.GameFileJson
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.cardBack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest

internal class PlaySessionTest {
  @Test
  fun unfinishedLiveGameOpensAtItsCurrentPosition() = runTest {
    val session = session()
    assertEquals(false, session.completed)

    val file = GameFileJson.parse(session.replayFile())
    assertTrue(file.hasEvents)
    val playback = file.open(Canon)
    assertEquals(session.world.timeline.checkpoint(), playback.positions.last())
    playback.seek(playback.positions.lastIndex)
    assertEquals(
        session.world.events.entriesSince(Checkpoint(0)).size,
        playback.world.events.entriesSince(Checkpoint(0)).size,
    )
  }

  @Test
  fun savedLiveGameRecoversWithAutoexecOffAndCanContinue() = runTest {
    val original = session(seed = 7531)
    val originalPrompt = original.prompt()
    val originalPosition = original.world.timeline.checkpoint()
    saveRecovery(original.recovery())

    val reopened = RecordingStore()
    assertTrue(reopened.recoverLastGame(), reopened.error)
    assertFalse(reopened.safeAutoexec)
    assertTrue(reopened.playing)
    assertEquals(originalPosition.ordinal, reopened.eventOrdinal)
    assertEquals(originalPrompt.heading, reopened.prompt?.heading)
    assertEquals(
        originalPrompt.options.map { it.label },
        reopened.prompt?.options?.map { it.label },
    )
    val green = assertNotNull(reopened.specimen).players.single { it.name == "Green" }
    assertEquals(original.held(cn("Hand")), green.hand)
    assertEquals(original.held(cn("Selecting")), green.selecting)

    val next = assertNotNull(reopened.prompt).options.first { it.enabled }
    reopened.choose(next)
    assertNull(reopened.problem)
    val nextPosition = reopened.eventOrdinal
    assertTrue(nextPosition > originalPosition.ordinal)

    var savedAgain: PlaySession.Recovery? = null
    for (attempt in 1..40) {
      savedAgain =
          savedRecovery()?.takeIf { saved ->
            GameFileJson.parse(saved.gameFile).open(Canon).positions.last().ordinal == nextPosition
          }
      if (savedAgain != null) break
      delay(25)
    }
    assertNotNull(savedAgain, "The recovered game did not save its next completed position")
    val reopenedAgain = RecordingStore()
    assertTrue(reopenedAgain.recoverLastGame(), reopenedAgain.error)
    assertEquals(nextPosition, reopenedAgain.eventOrdinal)
    assertFalse(reopenedAgain.safeAutoexec)
  }

  @Test
  fun victoryDoesNotEndGameWhileTasksRemain() = runTest {
    val session = session()
    assertEquals(false, session.completed)
    session.agents[ADMIN].sneak("Victory<Green>")
    session.world.timeline.recordPosition()
    assertEquals(false, session.completed)

    val file = GameFileJson.parse(session.replayFile())
    assertTrue(file.hasEvents)
    val playback = file.open(Canon)
    assertEquals(session.world.timeline.checkpoint(), playback.positions.last())
  }

  @Test
  fun emptySoloTaskQueueShowsAnEndPromptWithoutVictoryPhase() = runTest {
    val session = session()
    assertNull(session.world.classTable.findClass(cn("VictoryPhase")))

    session.agents[ADMIN].sneak("-GameScope")
    for (task in session.world.tasks.extract { it }) {
      session.agents[task.assignee].dropTask(task.id)
    }
    session.world.timeline.recordPosition()

    assertTrue(session.world.tasks.isEmpty(), "${session.world.tasks}")
    assertTrue(session.completed, "${session.world.tasks}")
    assertEquals("Game ended", session.prompt().heading)
  }

  @Test
  fun displayedHexSeedRoundTripsAll32Bits() {
    assertEquals("0x00000000", formatPlaySeed(0))
    assertEquals("0xffffffff", formatPlaySeed(-1))
    assertEquals(-1, parsePlaySeed("0xffffffff"))
    assertEquals(Int.MIN_VALUE, parsePlaySeed("0x80000000"))
    assertEquals(42, parsePlaySeed("42"))
    assertNull(parsePlaySeed("0x100000000"))
    assertNull(parsePlaySeed("0xnothex"))
  }

  @Test
  fun addingACardKeepsExistingCardsInTheSameRelativeOrder() {
    val cards =
        listOf(
                "AiCentral",
                "EarthOffice",
                "NoctisCity",
                "IndustrialComplex",
                "Donation",
                "AcquiredSpaceAgency",
                "AppliedScience",
                "SpaceLanes",
            )
            .map(::cn)
    val seed = 0x1a2b3c4d
    val original = orderedDeck(cards, seed)
    val added = cn("NewCard")

    assertEquals(
        listOf(
                "Donation",
                "SpaceLanes",
                "AiCentral",
                "AcquiredSpaceAgency",
                "EarthOffice",
                "AppliedScience",
                "IndustrialComplex",
                "NoctisCity",
            )
            .map(::cn),
        original,
    )
    assertEquals(original, orderedDeck(cards + added, seed).filterNot { it == added })
    assertEquals(original, orderedDeck(cards.reversed(), seed))
  }

  private fun session(seed: Int = 42): PlaySession {
    val session =
        PlaySession(
            Canon.gamePremise(GameConfig("TharsisMap, SoloMode, CorporateEraExpansion", "Green")),
            seed = seed,
            safeAutoexec = false,
        )
    for (back in listOf("CorporationCard", "ProjectCard")) {
      val deal =
          session.prompt().options.single { option ->
            session.world.tasks
                .getTaskData(requireNotNull(option.taskId))
                .instruction
                .descendantsOfType<Change>()
                .any { it.gaining?.className == cn(back) && it.removing == null }
          }
      session.choose(deal)
      assertNull(session.problem)
    }
    return session
  }

  @Test
  fun theSameSeedReproducesOpeningCardsAndNeutralTiles() {
    val first = session()
    val second = session()

    assertEquals(first.held(cn("Hand")), second.held(cn("Hand")))
    assertEquals(first.held(cn("Selecting")), second.held(cn("Selecting")))
    assertEquals(
        first.agents[ADMIN].list("Tile").entries.associate { it.key.toString() to it.value },
        second.agents[ADMIN].list("Tile").entries.associate { it.key.toString() to it.value },
    )
    assertEquals(4, first.agents[ADMIN].count("Tile"))
    assertTrue(first.held(cn("Selecting")).isNotEmpty())
    assertEquals(AutoExecPolicy.NONE, first.agents[first.player].autoExecPolicy)
  }

  @Test
  fun safeAutoexecDealsOpeningCardsAndLeavesKeepChoices() {
    val config =
        GameConfig("TharsisMap, SoloMode, CorporateEraExpansion, PreludeExpansion", "Green")
    val premise = Canon.gamePremise(config)
    val manual = PlaySession(premise, seed = 42, safeAutoexec = false)
    val safe = PlaySession(premise, seed = 42, safeAutoexec = true)
    val backs = listOf("CorporationCard", "ProjectCard", "PreludeCard")

    for (back in backs) {
      val draw =
          manual.prompt().options.single { option ->
            manual.world.tasks
                .getTaskData(requireNotNull(option.taskId))
                .instruction
                .descendantsOfType<Change>()
                .any { it.gaining?.className == cn(back) && it.removing == null }
          }
      manual.choose(draw)
      assertNull(manual.problem)
    }

    assertNull(safe.problem)
    assertEquals(manual.held(cn("Hand")).toSet(), safe.held(cn("Hand")).toSet())
    assertEquals(manual.held(cn("Selecting")).toSet(), safe.held(cn("Selecting")).toSet())
    assertEquals(6, safe.held(cn("Hand")).size)
    assertEquals(10, safe.held(cn("Selecting")).size)
    for (back in backs) {
      val pending = safe.world.tasks.extract { it.instruction }
      assertTrue(
          pending.any { task ->
            task.descendantsOfType<Remove>().any { it.removing.className == cn(back) }
          }
      )
      assertFalse(
          pending.any { task ->
            task.descendantsOfType<Change>().any { it.gaining?.className == cn(back) }
          }
      )
    }
  }

  @Test
  fun aCardDrawnFromAScienceResourceIsDealtFromTheDeck() {
    val session = session()
    val agent = session.agents[session.player]
    agent.sneak("OlympusConference")
    agent.sneak("Science<OlympusConference>")
    val scienceBefore = agent.count("Science<OlympusConference>")
    val handBefore = session.held(cn("Hand"))
    val task = agent.addTasks("ProjectCard FROM Science<OlympusConference>").single()

    session.choose(session.prompt().options.single { it.taskId == task })

    assertNull(session.problem)
    assertEquals(scienceBefore - 1, agent.count("Science<OlympusConference>"))
    assertEquals(handBefore.size + 1, session.held(cn("Hand")).size)
  }

  @Test
  fun selectingDiscardsUseTheClickedCardsAndTheirCount() {
    val session = session()
    session.agents[session.player].autoExecPolicy = AutoExecPolicy.NONE
    val before = session.held(cn("Selecting"))
    val task = session.agents[session.player].addTasks("-3? ProjectCard<Selecting>").single()

    session.choose(session.prompt().options.single { it.taskId == task })
    assertEquals(false, session.prompt().cards?.drawing)
    val chosen = session.prompt().cards!!.available.take(2)
    chosen.forEach(session::toggleCard)
    assertEquals(chosen.toSet(), session.prompt().cards?.chosen)
    assertTrue(session.prompt().cards?.canConfirm == true)
    session.confirmCards()

    assertNull(session.problem)
    assertEquals(before.filterNot { it in chosen }, session.held(cn("Selecting")))
  }

  @Test
  fun optionalDiscardCanCommitNoCards() {
    val session = session()
    session.agents[session.player].autoExecPolicy = AutoExecPolicy.NONE
    val before = session.held(cn("Selecting"))
    val task = session.agents[session.player].addTasks("-3? ProjectCard<Selecting>").single()

    session.choose(session.prompt().options.single { it.taskId == task })
    assertTrue(session.prompt().cards?.canConfirm == true)
    session.confirmCards()

    assertNull(session.problem)
    assertEquals(before, session.held(cn("Selecting")))
  }

  @Test
  fun handDiscardUsesTheClickedCard() {
    val session = session()
    session.agents[session.player].autoExecPolicy = AutoExecPolicy.NONE
    val before = session.held(cn("Hand"))
    val task = session.agents[session.player].addTasks("-CorporationCard<Hand>").single()

    session.choose(session.prompt().options.single { it.taskId == task })
    val cards = session.prompt().cards!!
    assertTrue(cards.fromHand)
    val chosen = cards.available.first()
    session.toggleCard(chosen)
    session.confirmCards()

    assertNull(session.problem)
    assertEquals(before.filterNot { it == chosen }, session.held(cn("Hand")))
  }

  @Test
  fun drawingFromSelectingUsesTheClickedCards() {
    val session = session()
    session.agents[session.player].autoExecPolicy = AutoExecPolicy.NONE
    val selecting = session.held(cn("Selecting"))
    val hand = session.held(cn("Hand"))
    val task =
        session.agents[session.player].addTasks("2 ProjectCard<Hand FROM Selecting>").single()
    val beforeChoice = session.world.timeline.checkpoint()

    session.choose(session.prompt().options.single { it.taskId == task })
    assertEquals(beforeChoice, session.world.timeline.checkpoint())
    assertEquals(selecting, session.held(cn("Selecting")))
    assertEquals(hand, session.held(cn("Hand")))
    assertEquals(true, session.prompt().cards?.drawing)
    val chosen = session.prompt().cards!!.available.take(2)
    session.toggleCard(chosen.first())
    assertEquals(false, session.prompt().cards?.canConfirm)
    session.toggleCard(chosen.last())
    assertTrue(session.prompt().cards?.canConfirm == true)
    session.confirmCards()

    assertNull(session.problem)
    assertEquals(selecting.filterNot { it in chosen }, session.held(cn("Selecting")))
    assertEquals(hand.toSet() + chosen.toSet(), session.held(cn("Hand")).toSet())
  }

  @Test
  fun adminCleanupDiscardsTheFiveUnkeptSelectedCardsWithoutAsking() {
    val session = session()
    val selecting = session.held(cn("Selecting"))
    val keep = selecting.take(5)
    val move =
        session.agents[session.player].addTasks("5 ProjectCard<Hand FROM Selecting>").single()
    session.choose(session.prompt().options.single { it.taskId == move })
    keep.forEach(session::toggleCard)
    session.confirmCards()
    assertNull(session.problem, "${session.prompt()}")
    assertFalse(session.prompt().heading.startsWith("Which"), "${session.prompt()}")
    assertEquals(selecting.drop(5), session.held(cn("Selecting")))

    repeat(5) {
      session.agents[ADMIN].addTasks("-ProjectCard<Green, Selecting>")
    }
    session.world.timeline.recordPosition()
    session.setSafeAutoexec(true)

    assertNull(session.problem, "${session.prompt()}")
    assertTrue(session.held(cn("Selecting")).isEmpty())
    assertTrue(keep.all { it in session.held(cn("Hand")) })
    assertFalse(session.prompt().heading.startsWith("Which ProjectCard leaves Selecting"))
  }

  @Test
  fun cancellingCardChoiceLeavesTheOfferAndHistoryIntact() {
    val session = session()
    // Discarding a provisional card choice must leave the game untouched.
    val task = session.agents[session.player].addTasks("-ProjectCard<Selecting>").single()
    val before = session.world.timeline.checkpoint()
    val cards = session.held(cn("Selecting"))
    session.choose(session.prompt().options.single { it.taskId == task })
    assertEquals("CANCEL CARD CHOICE", session.prompt().retreat)
    val chosen = session.prompt().cards!!.available.first()
    session.toggleCard(chosen)
    assertEquals("CLEAR SELECTION", session.prompt().retreat)

    session.retreat()

    assertNull(session.problem)
    assertEquals(before, session.world.timeline.checkpoint())
    assertEquals(cards, session.held(cn("Selecting")))
    session.choose(session.prompt().options.single { it.taskId == task })
    session.toggleCard(chosen)
    session.confirmCards()
    assertNull(session.problem)
    assertEquals(cards.filterNot { it == chosen }, session.held(cn("Selecting")))
  }

  @Test
  fun clickingAnOrArmSelectsAndNarrowsTheTask() {
    val session = session()
    val task = session.agents[session.player].addTasks("Heat OR Plant").single()
    val option = session.prompt().options.single { it.taskId == task }

    assertEquals(listOf("Heat<Green>!", "Plant<Green>!"), option.arms)
    session.chooseTaskArm(option, option.arms.last())

    assertNull(session.problem)
    assertEquals(1, session.agents[session.player].count("Plant"))
    assertEquals(0, session.agents[session.player].count("Heat"))
  }

  @Test
  fun standardActionOrPassListsTheNextActionsAndUsesTheSameChoicePath() {
    val direct = session()
    val flattened = session()
    val directTask = direct.agents[direct.player].addTasks("StandardAction OR Pass").single()
    val flatTask = flattened.agents[flattened.player].addTasks("StandardAction OR Pass").single()
    val directOption = direct.prompt().options.single { it.taskId == directTask }
    val flatOption = flattened.prompt().options.single { it.taskId == flatTask }
    val standard =
        (direct.world.tasks.getTaskData(directTask).instruction as Or)
            .instructions
            .filterIsInstance<Change>()
            .single { it.gaining?.className == cn("StandardAction") }
            .toString()

    direct.chooseTaskArm(directOption.copy(arms = listOf(standard)), standard)
    val nextActions = direct.prompt().options.map { it.label }
    assertTrue(nextActions.isNotEmpty())
    assertEquals(nextActions, flatOption.arms.dropLast(1))
    assertTrue(flatOption.arms.last().contains("Pass"))

    val directAction =
        PlayChoices(direct.prompt(), direct::choose, direct::chooseTaskArm).action("PlayProject")
    val flatAction =
        PlayChoices(flattened.prompt(), flattened::choose, flattened::chooseTaskArm)
            .action("PlayProject")
    assertTrue(directAction != null)
    assertTrue(flatAction != null)
    directAction()
    flatAction()

    assertNull(direct.problem)
    assertNull(flattened.problem)
    assertEquals(direct.prompt().heading, flattened.prompt().heading)
    assertEquals(direct.prompt().detail, flattened.prompt().detail)
    assertEquals(
        direct.prompt().options.map { it.label },
        flattened.prompt().options.map { it.label },
    )
  }

  @Test
  fun secondActionOfferExposesCardAndProjectShortcuts() {
    val session = session()
    val handCard = session.held(cn("Selecting")).first()
    val move = session.agents[session.player].addTasks("ProjectCard<Hand FROM Selecting>").single()
    session.choose(session.prompt().options.single { it.taskId == move })
    session.toggleCard(handCard)
    session.confirmCards()
    assertTrue(handCard in session.held(cn("Hand")))
    session.agents[session.player].sneak("ElectroCatapult, Steel")
    session.agents[session.player].addTasks("StandardAction OR PassBaton")
    val choices =
        PlayChoices(
            session.prompt(),
            session::choose,
            session::chooseTaskArm,
            session::prompt,
            session::cardActionAvailable,
            session::projectCardAvailable,
            session::playProjectCard,
        )
    assertTrue(choices.cardAction("ElectroCatapult", 2) != null)
    val play = choices.playProject(handCard)
    assertNotNull(play, "Manual mode should leave a held card clickable: ${session.prompt()}")

    val actionSession = session()
    actionSession.agents[actionSession.player].sneak("ElectroCatapult, Steel")
    actionSession.agents[actionSession.player].addTasks("StandardAction OR PassBaton")
    val action =
        PlayChoices(
                actionSession.prompt(),
                actionSession::choose,
                actionSession::chooseTaskArm,
                actionSession::prompt,
                actionSession::cardActionAvailable,
            )
            .cardAction("ElectroCatapult", 2)
    assertTrue(action != null)
    action()
    assertNull(actionSession.problem, "After action click: ${actionSession.prompt()}")
    assertEquals(
        1,
        actionSession.agents[actionSession.player].count("ActionUsedMarker<ElectroCatapult>"),
    )
  }

  @Test
  fun projectSourceFollowsTheSelectedCard() {
    val session = session()
    val card = session.held(cn("Selecting")).first()
    session.agents[session.player].addTasks("StandardAction OR PassBaton")
    val action =
        PlayChoices(session.prompt(), session::choose, session::chooseTaskArm).action("PlayProject")
    assertTrue(action != null)
    action()
    session.choose(
        session.prompt().options.single { it.label == card || it.label == "Class<$card>" }
    )

    assertNull(session.problem, "After card choice: ${session.prompt()}")
    assertTrue(session.prompt().heading != "Narrow CardLocation", "${session.prompt()}")
    assertTrue(session.prompt().options.any { card in it.label }, "${session.prompt()}")
  }

  @Test
  fun realSoloTurnSelectsProjectSourceWithoutSafetyProof() {
    val session = session()
    session.setSafeAutoexec(true)
    session.choose(session.prompt().options.single { "CorporationCard" in it.label })
    session.choose(session.prompt().options.first())
    session.held(cn("Selecting")).drop(2).forEach(session::toggleCard)
    session.confirmCards()
    val held = session.held(cn("Hand"))
    val card = held.first()
    val other = held.last()
    session.choose(session.prompt().options.first())
    val play =
        PlayChoices(
                session.prompt(),
                session::choose,
                session::chooseTaskArm,
                session::prompt,
                projectCardAvailable = session::projectCardAvailable,
                playProjectCard = session::playProjectCard,
            )
            .playProject(card)
    assertNotNull(play, "${session.prompt()}")
    play()

    assertNull(session.problem, "After project choice: ${session.prompt()}")
    assertTrue(session.prompt().heading != "Narrow CardLocation", "${session.prompt()}")
    assertTrue(card !in session.held(cn("Hand")), "Project remained in Hand: ${session.prompt()}")
    assertTrue(other in session.held(cn("Hand")), "A different project was played")
  }

  @Test
  fun switchingAutoexecOffRestoresARejectedHandCardShortcut() {
    val premise =
        Canon.gamePremise(GameConfig("TharsisMap, SoloMode, CorporateEraExpansion", "Green"))
    val front = premise.classTable.getClass(cn("CardFront"))
    val projects =
        premise.classTable
            .allClasses()
            .filter { !it.abstract && it.isSubtypeOf(front) && premise.classTable.isInhabited(it) }
            .filter { cardBack(it)?.className == cn("ProjectCard") }
            .map { it.className }
    val seed = (0..1000).first { cn("Birds") in orderedDeck(projects, it).drop(4).take(10) }
    val session = session(seed)
    session.setSafeAutoexec(true)
    session.choose(session.prompt().options.single { "CorporationCard" in it.label })
    session.choose(session.prompt().options.first())
    session.held(cn("Selecting")).filterNot { it == "Birds" }.forEach(session::toggleCard)
    session.confirmCards()

    assertFalse(session.projectCardAvailable("Birds"))
    session.setSafeAutoexec(false)
    assertTrue(
        session.projectCardAvailable("Birds"),
        "After disabling autoexec: ${session.prompt()}, hand=${session.held(cn("Hand"))}",
    )
  }

  @Test
  fun nuclearPowerInHandDoesNotStopAtItsOnlySource() {
    val premise =
        Canon.gamePremise(GameConfig("TharsisMap, SoloMode, CorporateEraExpansion", "Green"))
    val front = premise.classTable.getClass(cn("CardFront"))
    val projects =
        premise.classTable
            .allClasses()
            .filter { !it.abstract && it.isSubtypeOf(front) && premise.classTable.isInhabited(it) }
            .filter { cardBack(it)?.className == cn("ProjectCard") }
            .map { it.className }
    val seed = (0..1000).first { cn("NuclearPower") in orderedDeck(projects, it).drop(4).take(10) }
    val session = session(seed)
    session.setSafeAutoexec(true)
    session.choose(session.prompt().options.single { "CorporationCard" in it.label })
    session.choose(session.prompt().options.first())
    assertTrue("NuclearPower" in session.held(cn("Selecting")))
    session.held(cn("Selecting")).filterNot { it == "NuclearPower" }.forEach(session::toggleCard)
    session.confirmCards()
    session.agents[session.player].sneak("100 MC, 20 Steel")
    val searchTrace = mutableListOf<String>()
    session.searchTrace = searchTrace::add
    session.choose(session.prompt().options.first())

    assertNull(session.problem, "${session.prompt()}\n$searchTrace")
    assertTrue(
        session.prompt().heading != "Narrow CardLocation",
        "${session.prompt()}\n$searchTrace",
    )
    assertTrue(
        session.prompt().detail?.contains("Pay<") == true,
        "${session.prompt()}\n$searchTrace",
    )
    assertTrue(searchTrace.none { it.startsWith("unknown:") }, "$searchTrace")
  }

  @Test
  fun cardActionShortcutSelectsItsCardAndPrintedSlot() {
    val session = session()
    session.agents[session.player].sneak("ElectroCatapult, Steel")
    session.agents[session.player].addTasks("StandardAction OR Pass")
    val choices =
        PlayChoices(
            session.prompt(),
            session::choose,
            session::chooseTaskArm,
            session::prompt,
            session::cardActionAvailable,
        )
    assertNull(choices.cardAction("Ants", 1), "An unplayed card showed an action")
    assertNotNull(
        choices.cardAction("ElectroCatapult", 1),
        "Manual mode should leave an offered action clickable despite no Plant",
    )
    val click = choices.cardAction("ElectroCatapult", 2)
    assertTrue(click != null, "No printed action choice from ${session.prompt()}")

    click()

    assertNull(session.problem, "After click: ${session.prompt()}")
    assertEquals(1, session.agents[session.player].count("ActionUsedMarker<ElectroCatapult>"))
    val invocation =
        session.world.events
            .changesSince(Checkpoint(0))
            .mapNotNull { it.change.gaining }
            .lastOrNull { it.className == cn("UseAction") }
    assertTrue(invocation?.toString()?.contains("Action2") == true)
  }

  @Test
  fun cardActionShortcutAlsoWorksAfterOpeningUseCardAction() {
    val session = session()
    session.agents[session.player].sneak("ElectroCatapult, Steel")
    session.agents[session.player].addTasks("StandardAction OR Pass")
    val menu = PlayChoices(session.prompt(), session::choose, session::chooseTaskArm)
    val open = menu.action("UseCardAction")
    assertTrue(open != null)
    open()

    val choices =
        PlayChoices(
            session.prompt(),
            session::choose,
            session::chooseTaskArm,
            session::prompt,
            session::cardActionAvailable,
        )
    val click = choices.cardAction("ElectroCatapult", 2)
    assertTrue(click != null, "No printed action choice from ${session.prompt()}")
    click()

    assertNull(session.problem, "After click: ${session.prompt()}")
    assertEquals(1, session.agents[session.player].count("ActionUsedMarker<ElectroCatapult>"))
  }

  @Test
  fun resourceCardTargetCanFollowResourceTypeChoice() {
    val session = session()
    session.agents[session.player].sneak("Ants, Livestock")
    val task = session.agents[session.player].addTasks("CardResource<ResourceCard>").single()
    session.choose(session.prompt().options.single { it.taskId == task })
    assertEquals("Narrow CardResource", session.prompt().heading)
    val choices =
        PlayChoices(session.prompt(), session::choose, session::chooseTaskArm, session::prompt)
    assertTrue(choices.resourceCard("Livestock", "Animal", 0) != null)
    val click = choices.resourceCard("Ants", "Microbe", 0)
    assertTrue(click != null)

    click()

    assertNull(session.problem, "After click: ${session.prompt()}")
    assertEquals(1, session.agents[session.player].count("Microbe<Ants>"))

    session.agents[session.player].sneak("Tardigrades, Microbe<Tardigrades>")
    val removal = session.agents[session.player].addTasks("-CardResource<ResourceCard>").single()
    session.choose(session.prompt().options.single { it.taskId == removal })
    val remove =
        PlayChoices(session.prompt(), session::choose, session::chooseTaskArm, session::prompt)
            .resourceCard("Ants", "Microbe", 1)
    assertTrue(remove != null, "No resource removal choice from ${session.prompt()}")
    remove()

    assertNull(session.problem, "After removal: ${session.prompt()}")
    assertEquals(0, session.agents[session.player].count("Microbe<Ants>"))
  }

  @Test
  fun emptyResourceCardIsNotOfferedForRemovalAtTypeChoice() {
    val session = session()
    session.agents[session.player].sneak(
        "Ants, Tardigrades, Livestock, Microbe<Tardigrades>, Animal<Livestock>"
    )
    val task = session.agents[session.player].addTasks("-CardResource<ResourceCard>").single()
    session.choose(session.prompt().options.single { it.taskId == task })
    assertEquals("Narrow CardResource", session.prompt().heading)
    val choices =
        PlayChoices(session.prompt(), session::choose, session::chooseTaskArm, session::prompt)

    assertNull(choices.resourceCard("Ants", "Microbe", 0))
    assertTrue(choices.resourceCard("Tardigrades", "Microbe", 1) != null)
    assertTrue(choices.resourceCard("Livestock", "Animal", 1) != null)
  }

  @Test
  fun selectedStandardActionOrPassStillListsTheActionsAndPass() {
    val pending = session()
    val selected = session()
    val pendingTask = pending.agents[pending.player].addTasks("StandardAction OR Pass").single()
    val selectedTask = selected.agents[selected.player].addTasks("StandardAction OR Pass").single()
    val pendingOption = pending.prompt().options.single { it.taskId == pendingTask }
    selected.choose(selected.prompt().options.single { it.taskId == selectedTask })

    val choices = selected.prompt()
    assertEquals("Choose an alternative", choices.heading)
    assertEquals(pendingOption.arms, choices.options.map { it.label })
    assertTrue(choices.options.last().label.contains("Pass"))

    selected.choose(choices.options.first())
    pending.chooseTaskArm(pendingOption, pendingOption.arms.first())

    assertNull(selected.problem)
    assertNull(pending.problem)
    assertEquals(pending.prompt().heading, selected.prompt().heading)
    assertEquals(
        pending.prompt().options.map { it.label },
        selected.prompt().options.map { it.label },
    )
  }

  @Test
  fun checkedAutoexecKeepsTradePaymentChoicesThatCanFinish() {
    val premise =
        Canon.gamePremise(
            GameConfig(
                "TharsisMap, SoloMode, CorporateEraExpansion, ColoniesExpansion, Luna, Ceres, Triton, Ganymede, Callisto",
                "Green",
            )
        )
    fun choices(enableBeforeTrade: Boolean): List<String> {
      val session = PlaySession(premise, seed = 42)
      val player = session.agents[session.player]
      player.sneak("9 MC, 3 Energy, 3 Titanium")
      val turn = player.addTasks("StandardAction OR Pass").single()
      session.choose(session.prompt().options.single { it.taskId == turn })
      if (enableBeforeTrade) session.setSafeAutoexec(true)
      session.choose(session.prompt().options.single { it.label == "TradeAction" })
      if (!enableBeforeTrade) session.setSafeAutoexec(true)
      assertNull(session.problem)
      return session.prompt().options.map { it.label }
    }

    val payments = listOf("Action1", "Action2", "Action3")
    assertEquals(payments, choices(enableBeforeTrade = true))
    assertEquals(payments, choices(enableBeforeTrade = false))
  }

  @Test
  fun checkedAutoexecKeepsTradeWhenOnlyEnergyCanPay() {
    val premise =
        Canon.gamePremise(
            GameConfig(
                "TharsisMap, SoloMode, CorporateEraExpansion, ColoniesExpansion, Luna, Ceres, Triton, Ganymede, Callisto",
                "Green",
            )
        )
    val session = PlaySession(premise, seed = 42)
    val player = session.agents[session.player]
    player.sneak("3 Energy")
    val turn = player.addTasks("StandardAction OR Pass").single()
    session.choose(session.prompt().options.single { it.taskId == turn })
    session.setSafeAutoexec(true)

    assertTrue(session.prompt().options.any { it.label == "TradeAction" }, "${session.prompt()}")
  }

  @Test
  fun checkedAutoexecListsAmountsWithoutSearchingThem() {
    val session = session()
    session.agents[ADMIN].sneak("ActionPhase FROM SetupPhase")
    val player = session.agents[session.player]
    val task = player.addTasks("X MC, -2 MC").first()
    player.selectTask(task)
    session.world.timeline.recordPosition()
    session.setSafeAutoexec(true)

    val prompt = session.prompt()
    assertTrue(prompt.options.any { it.label == "2" }, prompt.heading)
    assertTrue(prompt.options.none { it.unproven }, prompt.heading)
    assertTrue(prompt.options.size > 100, prompt.heading)
    session.choose(prompt.options.single { it.label == "2" })
    assertNull(session.problem)
    assertNotNull(session.prompt())
  }

  @Test
  fun truncatingBeforeAnAutomaticDealDoesNotImmediatelyDealAgain() {
    val session = session()
    val cards = session.held(cn("Hand"))
    // A successful player command followed by an Admin deal is the same client protocol used
    // when passing into research. Both completed positions remain available in history.
    val task = session.agents[session.player].addTasks("Heat").single()
    session.agents[ADMIN].addTasks("Heat<Green>: ProjectCard<Green>")
    session.world.timeline.recordPosition()
    val beforeCommand = session.world.timeline.checkpoint()
    val oldStops = session.stops.size
    session.choose(session.prompt().options.single { it.taskId == task })
    assertNull(session.problem)
    assertEquals(cards.size + 1, session.held(cn("Hand")).size)
    val dealtCards = session.held(cn("Hand"))
    assertEquals(oldStops + 2, session.stops.size)
    val beforeDeal = session.stops[session.stops.lastIndex - 1]

    session.seek(beforeDeal)
    session.truncate()

    assertEquals(beforeDeal, session.world.timeline.checkpoint())
    assertEquals(cards, session.held(cn("Hand")))
    assertEquals(1, session.agents[session.player].count("Heat"))

    val continuation = session.prompt().options.single()
    assertEquals(ADMIN.toString(), continuation.who)
    session.choose(continuation)
    assertNull(session.problem)
    assertEquals(dealtCards, session.held(cn("Hand")))
    assertEquals(1, session.agents[session.player].count("Heat"))

    session.seek(beforeDeal)
    session.truncate()
    session.seek(session.stops[session.stops.lastIndex - 1])
    session.truncate()
    assertEquals(beforeCommand, session.world.timeline.checkpoint())
    assertEquals(0, session.agents[session.player].count("Heat"))
    assertEquals(cards, session.held(cn("Hand")))
  }
}
