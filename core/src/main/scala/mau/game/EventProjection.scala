package mau.game

import io.circe.Codec
import mau.round.EffectProjection
import mau.round.Player

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

enum EventProjection(time: Instant) derives Codec:
  case GameStarted(time: Instant) extends EventProjection(time)
  case PlayerEntered(time: Instant, player: Player) extends EventProjection(time)
  case PlayerExited(time: Instant, player: Player) extends EventProjection(time)
  case PlayerChecked(time: Instant, player: Player) extends EventProjection(time)
  case RoundStarted(time: Instant, firstPlayer: Player) extends EventProjection(time)
  case RoundAction(time: Instant, effect: EffectProjection) extends EventProjection(time)
  case RoundWon(time: Instant, winner: Player) extends EventProjection(time)
  case RoundStopped(time: Instant) extends EventProjection(time)
  case RuleAdded(time: Instant, editor: Player) extends EventProjection(time)
  case RuleRejected(time: Instant, editor: Player) extends EventProjection(time)
  case RulesReset(time: Instant, player: Player) extends EventProjection(time)

  def localTime: LocalTime = time.atZone(ZoneId.systemDefault).toLocalTime
