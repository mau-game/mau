package mau

import com.raquo.laminar.api.L.*
import mau.edition.*
import mau.game.RuleSuggestion
import mau.round.Player
import org.scalajs.dom

import scala.concurrent.ExecutionContext
import scala.scalajs.js

import game.Command

class RuleSelectionTab(
  you: Player,
  send: Observer[Command],
  edit: Observer[CompilationReport]
):
  val selectionVar: Var[Option[String]] = Var(None)
  def apply($suggestions: Signal[Seq[RuleSuggestion]])(using ExecutionContext): HtmlElement =
    div(
      className := "tab-content",
      ruleSelectionBar,
      div(
        idAttr := "suggestion-list",
        className := "deliberation-input",
        children <-- $suggestions.map(ss => ss.map(suggestionRow)),
      ),
      div(
        className := "commands",
        child <-- selectionVar.signal.map {
          case Some(id) => commandButton("Validate", onClick.mapTo(Command.SelectRule(id)) --> send)
          case None => div(className := "command-button", button("Validate", disabled := true))}
      )
    )

  private def ruleSelectionBar: Div = div(
    className := "navbar",
    div(
      className := "navbar-button",
      marginLeft.auto,
      span(className := "material-symbols-outlined", "refresh"),
      div(className := "navbar-caption", "Refresh"),
      onClick.mapTo(None) --> selectionVar,
      onClick.mapTo(Command.RefreshSuggestions) --> send
    ),
  )

  private def suggestionRow(suggestion: RuleSuggestion): Div =
    val idStr = s"rule-${suggestion.id}"
    div(
      className := "suggestion-row",
      className <-- selectionVar.signal.map:
        case Some(id) if id == suggestion.id => "selected"
        case _ => "",
      input(
        `type` := "radio",
        idAttr := idStr,
        nameAttr := "suggestion",
        onClick.mapTo(Some(suggestion.id)) --> selectionVar,
      ),
      label(suggestion.description, forId := idStr),
      div(
        className := "edit-button",
        span(className := "material-symbols-outlined", "code"),
        span(className := "edit-caption", "Edit"),
        onClick.mapTo(suggestion.compilation) --> edit,
      ),
    )
