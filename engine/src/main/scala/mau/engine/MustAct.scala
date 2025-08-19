package mau.engine

import mau.round.*

import java.time.Instant

enum MustAct:
  case Play(endTime: Instant, player: Player)
  case Click(endTime: Instant, player: Player, buttons: Seq[Button])

  def endTime: Instant
  def player: Player
  def isOverdue(now: Instant): Boolean = now.isAfter(endTime)
  def isPlay: Boolean = isInstanceOf[Play]
  def isClick(player: Player): Boolean = isInstanceOf[Click] && this.player == player
  def reset: MustAct = this match
    case mustPlay: Play => MustAct.play(player)
    case mustClick: Click => MustAct.click(player, mustClick.buttons)

  def project: MustActProjection = this match
    case mustPlay: Play => MustActProjection.Play(player)
    case mustClick: Click => MustActProjection.Click(player)

object MustAct:
  def play(player: Player): Play =
    Play(Instant.now().plusMillis(5000), player)

  def click(player: Player, buttons: Seq[Button]): Click =
    Click(Instant.now().plusMillis(3000), player, buttons)
