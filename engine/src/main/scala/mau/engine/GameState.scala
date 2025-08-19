package mau.engine

import mau.edition.CompilationReport
import mau.game.StateProjection
import mau.round.*

import scala.util.Random

final case class GameState(
  admin: Option[Player],
  latestWinner: Option[Player],
  latestRoundPlayers: Seq[Player],
  players: Seq[Player],
  statuses: Map[Player, Status],
  rules: Seq[Rule],
  customRulesNumber: Option[Int],
  history: History,
  roundOpt: Option[RoundState],
  initCompilation: CompilationReport
):
  def contains(player: Player): Boolean = players.contains(player)

  def getPlayer(user: User): Option[Player] = players.find(_.user == user)

  def isDeliberating(player: Player): Boolean =
    contains(player) && statuses(player).isInstanceOf[Status.Deliberating]
  
  def addPlayer(user: User): GameState =
    val ids = players.map(p => p.id)
    val id = 1.to(5).filterNot(ids.contains).head
    val player = Player(user, id)
    update(
      players = players :+ player,
      statuses = statuses + (player -> Status.InLobby),
      history = history.add(Event.playerEntered(player))
    )

  def removePlayer(player: Player): GameState =
    update(
      players = players.filterNot(_ == player),
      statuses = statuses - player,
      history = history.add(Event.playerExited(player)),
      roundOpt = roundOpt.map(_.removePlayer(player))
    )

  def startPlaying(player: Player): GameState =
    val round = roundOpt match
      case Some(round) => round.addPlayer(player)
      case None => RoundEngine.init(Seq(player), rules.flatMap(_.buttons).distinct)
    update(
      statuses = statuses + (player -> Status.Playing),
      history = history.add(Event.playerChecked(player)),
      roundOpt = Some(round)
    )

  def resetRules(player: Player): GameState =
    copy(
      rules = Rule.baseRules,
      history = history.add(Event.rulesReset(player)),
      customRulesNumber = Some(0)
    )
  
  def addRule(player: Player, rule: Rule): GameState = 
    update(
      statuses = statuses + (player -> Status.InLobby),
      rules = rule +: rules,
      customRulesNumber = customRulesNumber.map(_ + 1),
      history = history.add(Event.ruleAdded(player))
    )

  def refreshSuggestions(player: Player): GameState =
    statuses(player) match
      case Status.Deliberating(suggestions) =>
        val others = (missingRules -- suggestions.map(_.id)).values
        val newSuggestions =
          Random.shuffle(others).take(3).flatMap(_.asSuggestion).toSeq ++ Random.shuffle(suggestions).take(3 - others.size)
        update(statuses = statuses + (player -> Status.Deliberating(newSuggestions)))
      case _ => ???

  def updateRound(round: RoundState, untrustedRules: Set[ExternalRule]): GameState =
    val event = Event.roundAction(round.effect.get)
    val trustedRules = rules.collect:
      case rule: InMemoryRule => rule
      case rule: ExternalRule if !untrustedRules.contains(rule) => rule
    untrustedRules.foreach: rule =>
      println(s"Rule ${rule.name} is removed due to suspicion of untrusted behavior:")
      println(rule.code.split("\n").mkString(" ", "\n  ", ""))
    round.winner match
      case None => copy(rules = trustedRules, history = history.add(event), roundOpt = Some(round))
      case Some(winner) => 
        val suggestedRules = Random.shuffle(missingRules.values).take(3).flatMap(_.asSuggestion).toSeq
        update(
          latestWinner = Some(winner),
          latestRoundPlayers = round.players,
          statuses = statuses.map { (p, _) =>
            if p == winner then  p -> Status.Deliberating(suggestedRules)
            else p -> Status.InLobby
          },
          rules = trustedRules,
          history = history.add(event).add(Event.roundWon(winner)),
          roundOpt = None
        )

  def missingRules: Map[String, InMemoryRule] = Rule.suggestions(players.size) -- rules.map(_.id)
  
  def project(user: User): Option[StateProjection] =
    getPlayer(user).map: player =>
      val view =
        statuses(player) match
          case Status.Playing =>
            val round = roundOpt.get
            val splitPlayers = round.players.splitAt(round.players.indexOf(player))
            val otherPlayers = splitPlayers(1).tail ++ splitPlayers(0)
            val otherHands =
              for 
                p <- otherPlayers
                hand <- round.hands.get(p)
              yield p -> hand.size
            StateProjection.View.Round(
              round.buttons,
              round.hands.get(player),
              round.discard.take(2),
              otherHands,
              round.effect.map(_.project(player)),
              round.mustAct.project
            )
          case Status.Deliberating(suggestions) =>
            StateProjection.View.Deliberation(suggestions, initCompilation)
          case status =>
            StateProjection.View.Lobby(
              admin,
              players.map(p => p -> statuses(p).projection),
              customRulesNumber,
              latestWinner,
              latestRoundPlayers
            )
          
      StateProjection(player, history.project(player), view)

  private def update(
    players: Seq[Player] = this.players,
    latestWinner: Option[Player] = this.latestWinner,
    latestRoundPlayers: Seq[Player] = this.latestRoundPlayers,
    statuses: Map[Player, Status],
    rules: Seq[Rule] = this.rules,
    customRulesNumber: Option[Int] = this.customRulesNumber,
    history: History = this.history,
    roundOpt: Option[RoundState] = this.roundOpt
  ): GameState =
    val newAdmin = admin.filter(players.contains).orElse(players.headOption)
    if statuses.isEmpty then
      GameState(
        newAdmin,
        latestWinner,
        latestRoundPlayers,
        players,
        statuses,
        Rule.baseRules,
        Some(0),
        history,
        roundOpt = None,
        initCompilation
      )
    else
      GameState(
        newAdmin,
        latestWinner,
        latestRoundPlayers,
        players,
        statuses,
        rules,
        customRulesNumber,
        history,
        roundOpt,
        initCompilation
      )
  
  override def toString: String =
    s"GameState($players, $statuses, ${rules.size} rules, $history, $roundOpt)"

object GameState:
  def init(initCompilation: CompilationReport): GameState =
    GameState(None, None, Seq.empty, Seq.empty, Map.empty, Rule.baseRules, Some(0), History.empty, None, initCompilation)
