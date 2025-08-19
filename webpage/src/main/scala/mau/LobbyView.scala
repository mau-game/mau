package mau

import com.raquo.laminar.api.L.*
import com.raquo.laminar.modifiers.EventListener
import mau.game.Command
import mau.game.StateProjection.View.Lobby
import mau.game.StatusProjection
import mau.round.Player
import org.scalajs.dom.MouseEvent

class LobbyView(you: Player, customRulesNumber: Option[Int], latestWinner: Option[Player], send: Observer[Command], exitGame: Observer[Unit]):
  def apply($state: Signal[Lobby]): HtmlElement =
    val $yourStatus = $state.map(_.statuses.collectFirst { case (p, s) if p == you => s }.get).distinct
    val $admin = $state.map(_.admin)
    val isDeliberatingSignal = $state.map(_.statuses.exists(_._2 == StatusProjection.Deliberating))

    div(
      idAttr := "lobby",
      className := "page-container",
      navbar(exitGame),
      div(
        idAttr := "content-container",
        div(
          idAttr := "content",
          div(
            idAttr := "title",
            h1("Game Lobby"),
            h2("Connected players")
          ),
          div(
            idAttr := "player-list",
            children <-- $state.map(_.statuses).split((p, _) => p):
              case (p, _, playerStatus) => playerRow(p, $admin, playerStatus.map((_, s) => s)),
            children <-- $state.map(s => 0.until(5 - s.statuses.size): Seq[Int]).split(identity):
              case (_, _, _) => emptyPlayerRow
          ),
          div(
            className := "custom-rules-number",
            child <-- $state.map(_.customRulesNumber).map {
              case Some(0) => "There are no custom rules"
              case Some(1) => "There is 1 custom rule"
              case Some(number) => s"There are $number custom rules"
              case None => "There are no custom rules"
            }
          ),
          div(
            className := Seq("commands"),
            commandButton(
              className := "button-primary","Start playing", 
              onClick.mapTo(Command.StartPlaying) --> send,
              disabled <-- isDeliberatingSignal
            ),
            children <-- $admin.map: adminOpt =>
              if adminOpt.contains(you) then Seq(commandButton(className := "button-secondary", "Reset rules", onClick.mapTo(Command.ResetRules) --> send))
              else Seq.empty
          ),
          latestWinner.map(victoryScreen(_, false))
        )
      )
    )

  def emptyPlayerRow: Div =
    div(
      className := "player-row",
      img(
        className := s"empty-player-profile",
        className := "player-profile",
        src := Assets.Avatars("avatar-0"),
        alt := s"Empty player picture",
      ),
      span(
        className := Seq("empty-player-name"),
        "empty"
      ),
    )
  
  def playerRow(player: Player, $admin: Signal[Option[Player]], statusSig: Signal[StatusProjection]): Div =
    val isYou = player == you
    div(
      className := "player-row",
      if isYou then className := "you" else emptyNode,
      img(
        idAttr := s"player-${player.id}-profile",
        className := "player-profile",
        src := Assets.Avatars(player.user.avatar),
        alt := s"Player picture ${player.id}",
      ),
      span(
        className := "player-name",
        player.user.name,
        child <-- $admin.map: adminOpt =>
          if adminOpt.contains(player) then " (Admin)" else emptyNode
      ),
      span(
        className := "player-status",
        child <-- statusSig.map:
          case StatusProjection.InLobby => ""
          case StatusProjection.Deliberating => "Deliberating" 
          case StatusProjection.Playing => "Playing"
      )
    )
