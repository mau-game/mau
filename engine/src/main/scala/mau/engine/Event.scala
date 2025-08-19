package mau.engine

import mau.game.EventProjection
import mau.round.Player

import java.time.Instant

enum Event:
  case GameStarted(time: Instant)
  case PlayerEntered(time: Instant, player: Player)
  case PlayerExited(time: Instant, player: Player)
  case PlayerChecked(time: Instant, player: Player)
  case RoundStarted(time: Instant, firstPlayer: Player)
  case RoundAction(time: Instant, effect: Effect)
  case RoundWon(time: Instant, winner: Player)
  case RoundStopped(time: Instant)
  case RuleAdded(time: Instant, editor: Player)
  case RuleRejected(time: Instant, editor: Player)
  case RulesReset(time: Instant, player: Player)
  
  def project(target: Player): EventProjection =
    this match
      case GameStarted(time) => EventProjection.GameStarted(time)
      case PlayerEntered(time, player) => EventProjection.PlayerEntered(time, player)
      case PlayerExited(time, player) => EventProjection.PlayerExited(time, player)
      case PlayerChecked(time, player) => EventProjection.PlayerChecked(time, player)
      case RoundStarted(time, firstPlayer) => EventProjection.RoundStarted(time, firstPlayer)
      case RoundAction(time, effect) => EventProjection.RoundAction(time, effect.project(target))
      case RoundWon(time, winner) => EventProjection.RoundWon(time, winner)
      case RoundStopped(time) => EventProjection.RoundStopped(time)
      case RuleAdded(time, editor) => EventProjection.RuleAdded(time, editor)
      case RuleRejected(time, editor) => EventProjection.RuleRejected(time, editor)
      case RulesReset(time, player) => EventProjection.RulesReset(time, player)

object Event:
  def gameStarted: Event = GameStarted(Instant.now)
  def playerEntered(player: Player): Event = PlayerEntered(Instant.now, player)
  def playerExited(player: Player): Event = PlayerExited(Instant.now, player)
  def playerChecked(player: Player): Event = PlayerChecked(Instant.now, player)
  def roundStarted(firstPlayer: Player): Event = RoundStarted(Instant.now, firstPlayer)
  def roundAction(effect: Effect): Event = RoundAction(Instant.now, effect)
  def roundWon(winner: Player): Event = RoundWon(Instant.now, winner)
  def roundStopped: Event = RoundStopped(Instant.now)
  def ruleAdded(editor: Player): Event = RuleAdded(Instant.now, editor)
  def ruleRejected(editor: Player): Event = RuleRejected(Instant.now, editor)
  def rulesReset(player: Player): Event = RulesReset(Instant.now, player)
