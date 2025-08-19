package mau.utils

object Base64:
  private val encoder = java.util.Base64.getEncoder
  private val decoder = java.util.Base64.getDecoder

  def encode(value: String): String = String(encoder.encode(value.getBytes()))
  def decode(value: String): String = String(decoder.decode(value))
