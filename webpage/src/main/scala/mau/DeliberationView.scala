package mau

import com.raquo.laminar.api.L.*
import mau.edition.*
import mau.game.Command
import mau.game.StateProjection.View.Deliberation
import mau.round.Player
import org.scalajs.dom

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.scalajs.js

class DeliberationView(
  you: Player,
  initCompilation: CompilationReport,
  send: Observer[Command],
  syntaxHighlighting: Future[SyntaxHighlighting],
  exitGame: Observer[Unit]
)(using ExecutionContext):
  private enum Tab:
    case Suggestions
    case Code(code: Option[CompilationReport])
    case Prompt
  
  def apply(stateVar: Signal[Deliberation]): HtmlElement =
    val tabVar: Var[Tab] = Var(Tab.Suggestions)
    val edit = tabVar.writer.contramap((code: CompilationReport) => Tab.Code(Some(code)))
    val ruleSelectionTab = RuleSelectionTab(you, send, edit)
    val ruleEditionTab = RuleEditionTab(you, initCompilation, send, syntaxHighlighting)
    val ruleGenerationTab = RuleGenerationTab(you, send, syntaxHighlighting, edit)
    div(
      idAttr := "deliberation",
      className := "page-container",
      navbar(exitGame),
      div(
        idAttr := "content-container",
        div(
          idAttr := "content",
          div(
            idAttr := "title",
            h1("ADD NEW RULE"),
          ),
          navTag(
            className := "tabs",
            tabLink(tabVar, Tab.Suggestions),
            tabLink(tabVar, Tab.Code(None)),
            tabLink(tabVar, Tab.Prompt),
            span(className := "tab-filler")
          ),
          child <-- tabVar.signal.map {
            case Tab.Suggestions => ruleSelectionTab(stateVar.map(_.suggestions).distinct)
            case Tab.Code(impl) => ruleEditionTab(impl)
            case Tab.Prompt => ruleGenerationTab.elem
          }
        )
      ),
      victoryScreen(you, true)
    )

  private def tabLink(tabVar: Var[Tab], target: Tab): HtmlElement =
    button(
      className := "tab",
      target.productPrefix,
      onClick.mapTo(target) --> tabVar,
      className <-- tabVar.signal.map: current =>
        if current.productPrefix == target.productPrefix then Seq("active") else Seq.empty,
      disabled <-- tabVar.signal.map(_ == target)
    )
