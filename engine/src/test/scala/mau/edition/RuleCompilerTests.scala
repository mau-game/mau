/* package mau.edition

import mau.*
import mau.Rank.*
import mau.Suit.*
import mau.edition.Compilation
import munit.*

class RuleCompilerTests extends FunSuite:
  test("compile rule"):
    val ruleString: String = "case Play(_, card) if card.rank == Ten => Decision.ReverseDirection"
    val rule = RuleCompiler.default.compile(ruleString).asInstanceOf[Rule]
    val state0 = Values.threePlayers(currentPlayer = Values.player3).ruling
    rule(state0).apply(Action.Play(Values.player3, Card(Ten, Diamond))) match
      case Decision.ReverseDirection => assert(true)
      case _ => assert(false)
 */