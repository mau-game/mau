package mau.engine

import mau.edition.*
import mau.exec.*
import mau.game.Command
import mau.game.Command.*
import mau.round.User

import java.time.Instant
import scala.concurrent.duration.*

object GameEngine:
  def apply(executor: RuleExecutor): GameEngine = new GameEngine(executor, RoundEngine(executor))

class GameEngine(executor: RuleExecutor, roundEngine: RoundEngine):
  def initState: GameState =
    val initCompilation = executor
      .compile(
        """|// Define your custom rule by fixing the code below. 
           |// You can hover the red squiggles for guidance.
           |
           |case Action.FIXME(card) if Condition.FIXME => Decision.FIXME
           |
           |// -- EXAMPLES --
           |//
           |// You can play a diamond card on top of any other card.
           |// case Action.Play(card) if card.isDiamond => Decision.ValidCard
           |// 
           |// Drawing a card reverses the direction of play.
           |// case Action.Draw => Decision.ReverseDirection
           |//   
           |// Anybody can play on top of a face card, even if it's not their turn.
           |// case Action.Play(card) if state.discard.isFace => Decision.ValidTurn
           |""".stripMargin,
        timeout = 10.seconds
      )
      .asInstanceOf[RuleCompilation.Report]
      .report
    GameState.init(initCompilation)

  def enterGame(state: GameState, user: User): Option[GameState] =
    if state.getPlayer(user).isEmpty && state.players.size < 5 then
      Some(state.addPlayer(user))
    else None

  def exitGame(state: GameState, user: User): Option[GameState] =
    state.getPlayer(user).map(state.removePlayer)

  def submit(state: GameState, user: User, rule: Rule): Option[GameState] =
    state.getPlayer(user).filter(state.isDeliberating).map(state.addRule(_, rule))

  def handle(state: GameState, user: User, command: Command): Option[GameState] =
    (command, state.getPlayer(user)) match
      case (StartPlaying, Some(player)) if !state.statuses.values.exists(_.isInstanceOf[Status.Deliberating]) =>
        Some(state.startPlaying(player))
      case (RoundAction(action), Some(player)) if state.roundOpt.isDefined && roundEngine.isAllowed(state.roundOpt.get, action, player)  =>
        val WithUntrustedRules(round, untrustedRules) = roundEngine.handle(state.roundOpt.get, state.rules, action, player)
        Some(state.updateRound(round, untrustedRules))
      case (ResetRules, Some(player)) if state.admin.contains(player) && state.roundOpt.isEmpty => Some(state.resetRules(player))
      case (SelectRule(ruleId), Some(player)) if state.isDeliberating(player) && state.missingRules.contains(ruleId) =>
        Some(state.addRule(player, state.missingRules(ruleId)))
      case (RefreshSuggestions, Some(player)) if state.isDeliberating(player) =>
        Some(state.refreshSuggestions(player))
      case command =>
        println(s"Invalid $command")
        None

  def tick(state: GameState, now: Instant): Option[GameState] =
    for
      round <- state.roundOpt
      newRound <- roundEngine.tick(round, state.rules, now)
    yield state.updateRound(newRound, Set.empty)
