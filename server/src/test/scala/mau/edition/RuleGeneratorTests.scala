/* package mau.edition

import mau.Env

class RuleGeneratorTests extends munit.FunSuite:
  private val compiler = mau.exec.RuleCompiler.default
  private val token: String = Env.envOrThrow("MAU_PASSWORD")
  private val generator = RuleGenerator("l47vg0b974.execute-api.eu-west-1.amazonaws.com", token, compiler)
  
  test("generate"):
    val generation = generator.generate("Playing a seven switches the direction of play")
    generation match
      case Generation.Success(compilation) => println(compilation)
      case Generation.Failure(error) => fail(s"expect success, got $error") */
