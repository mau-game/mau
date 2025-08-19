package mau.engine

import mau.round.*

import java.util.concurrent.atomic.AtomicInteger
import scala.quoted.*

object Macro:
  private val idGen: AtomicInteger = AtomicInteger(0)

  inline def rule(inline expr: RulingState => PartialFunction[Action, Decision]): InMemoryRule =
    ${ ruleExpr('expr) }

  inline def rule(inline expr: RulingState => PartialFunction[Action, Decision], id: String): InMemoryRule =
    ${ ruleExpr('expr, 'id) }

  private def ruleExpr(
    expr: Expr[RulingState => PartialFunction[Action, Decision]],
    id: Expr[String]
  )(using Quotes): Expr[InMemoryRule] =
    ruleExpr(expr, Some(id))
  
  private def ruleExpr(
    expr: Expr[RulingState => PartialFunction[Action, Decision]]
  )(using Quotes): Expr[InMemoryRule] =
    ruleExpr(expr, None)

  private def ruleExpr(
    expr: Expr[RulingState => PartialFunction[Action, Decision]],
    idOpt: Option[Expr[String]]
  )(using Quotes): Expr[InMemoryRule] =
    val buttons = collectButtons(expr)
    val code = extractCode(expr)
    val id = idOpt.getOrElse(Expr(idGen.getAndAdd(1).toString))
    '{ InMemoryRule($id, $expr, code = Some($code), buttons = $buttons)}

  private def collectButtons[T: Type](expr: Expr[T])(using Quotes): Expr[Seq[Button]] =
    import quotes.reflect.*
    var buttons = Set.empty[String]
    val collector = new ExprMap:
      override def transform[T](e: Expr[T])(using Type[T])(using Quotes): Expr[T] =
        e match
          case '{ ($button: Button).name == ($name: String) } =>
            val buttonValue = name.value.getOrElse(
              report.errorAndAbort("The right-hand side of `$button.name ==` should be a constant string, such as \"CLICK ME!\".", name)
            )
            if buttonValue.toUpperCase != buttonValue then
              report.errorAndAbort("The button name must be uppercase.", name)
            else buttons += buttonValue.toUpperCase
            e
          case '{ Decision.MustClick($button)} =>
            val buttonValue = button.value.getOrElse(
              report.errorAndAbort("The argument of `Decision.MustClick` should be a constant string, such as \"CLICK ME!\".", button)
            )
            if buttonValue.toUpperCase != buttonValue then
              report.errorAndAbort("The button name must be uppercase.", button)
            else buttons += buttonValue.toUpperCase
            e
          case _ => transformChildren(e)
    collector.transform(expr)
    Expr.ofSeq(buttons.toSeq.sorted.map(b => '{Button(${Expr(b)})}))

  private def extractCode(expr: Expr[RulingState => PartialFunction[Action, Decision]])(using Quotes): Expr[String] =
    import quotes.reflect.{report, asTerm}
    expr match
      case '{(state: RulingState) => ($body(state): PartialFunction[Action, Decision])} =>
        val pos = body.asTerm.pos
        Expr(pos.sourceFile.content.get.slice(pos.start, pos.end).stripBraces.removeEmptyLines.stripIndent)
      case _ => report.errorAndAbort("Malformed rule, should be of the form (state: RulingState) => body")

  extension (code: String)
    def stripBraces: String =
      val stripped = code.stripLeading.stripTrailing
      if stripped.startsWith("{") && stripped.endsWith("}") then
        stripped.stripPrefix("{").stripSuffix("}")
      else stripped

    def removeEmptyLines: String =
      code.split("\n").map(_.stripTrailing).filter(_.nonEmpty).mkString("\n")
      
    


