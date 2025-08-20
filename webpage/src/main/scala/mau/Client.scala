package mau

import com.raquo.laminar.api.L.*
import io.circe.*
import io.circe.syntax.*
import mau.edition.SyntaxHighlighting
import mau.game.*
import mau.game.StateProjection.*
import mau.round.Action
import org.scalajs.dom

import scala.concurrent.ExecutionContext
import scala.scalajs.js

object Client:
  given ExecutionContext = ExecutionContext.Implicits.global

  var socket: dom.WebSocket = null
  val stateBus = EventBus[Option[StateProjection]]()

  val sendAction: Observer[Action] = Observer(action => send(Command.RoundAction(action)))
  val sendCommand: Observer[Command] = Observer(command => send(command))
  
  @main def start: Unit =
    println("Mau is a game of rules. Rule number 1: Don't talk about the rules.")
    play()
    val syntaxHighlighting = SyntaxHighlighting.init // load tree-sitter parser as soon as possible
    val exitGame = stateBus.writer.contramap((_: Unit) => None)
    val mainElement = div(
      idAttr := "main-container",
      child <-- stateBus.events.distinctBy(_.map(_.you)).flatMapSwitch {
        case None =>
          /* val player = Player("adpi2", 1)
          val rules = Seq(Rule.aceAtAnyTurn, Rule.clickFaceOnFace, Rule.colorToDirection)
          val selectionView = RuleSelectionView(player, sendCommand)(
            Var(new View.RuleSelection(rules.flatMap(RuleSuggestion.apply))).signal
          )
          val lobbyView = LobbyView(player, sendCommand)(
            Var(new View.Lobby(Seq(player -> StatusProjection.InLobby))).signal
          ) */
          val invitationView = InvitationView(Observer(_ => play()))
          Val(invitationView)
        case Some(init) =>
          val viewStream = stateBus.events.collect { case Some(state) => state.view }
          viewStream.toSignal(init.view).distinctBy(_.getClass.getName).map { view =>
            view match
              case roundInit: View.Round =>
                val $round = viewStream.collect { case r: View.Round => r }.toSignal(roundInit).distinct
                val $mustAct = $round.map(r => r.mustAct)
                val roundView = RoundView(init.you, sendAction, $mustAct, exitGame)($round)
                // div(
                //   history.elements,
                //   roundView.amend(position.absolute, history.leftBinder, right := 0.px)
                // )
                roundView
              case deliberationInit: View.Deliberation =>
                val $deliberation = viewStream.collect { case r: View.Deliberation => r }.toSignal(deliberationInit)
                DeliberationView(
                  init.you,
                  deliberationInit.initCompilation,
                  sendCommand,
                  syntaxHighlighting,
                  exitGame
                )($deliberation)
              case lobbyInit: View.Lobby =>
                val $lobby = viewStream.collect { case w: View.Lobby => w }.toSignal(lobbyInit)
                val latestWinner = lobbyInit.latestWinner.filter(winner => winner != init.you && lobbyInit.latestRoundPlayers.contains(init.you))
                LobbyView(init.you, lobbyInit.customRulesNumber, latestWinner, sendCommand, exitGame)($lobby)
        }
      }
    )
    render(dom.document.body, mainElement)

  private def play(): Unit =
    val host = dom.document.location.host
    val ws = if host.startsWith("localhost") then "ws" else "wss"
    socket = new dom.WebSocket(s"$ws://$host/play")
    socket.onerror = _ => stateBus.emit(None)
    socket.onclose = _ => stateBus.emit(None)
    socket.onmessage = receive(_)

  private def send(command: Command): Unit =
    val data = Printer.noSpaces.print(command.asJson)
    socket.send(data)

  private def receive(message: dom.MessageEvent): Unit =
    val data = message.data.asInstanceOf[String]
    parser.decode[StateProjection](data) match
      case Left(error) => println(error)
      case Right(state) => stateBus.emit(Some(state))
