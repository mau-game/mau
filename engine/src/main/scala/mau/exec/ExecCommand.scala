package mau.exec

import io.circe.Codec
import mau.round.Action
import mau.round.RulingState

import java.net.URI

enum ExecCommand derives Codec:
  case Compile(code: String)
  case Complete(code: String, position: Int)
  case Run(ruleURI: URI, ruleName: String, state: RulingState, action: Action)

