package edu.cmu.cs.alabaster.config

data class CompletionConfig(
  val lookup: String,
  val tail: String?,
  val import: ImportConfig?,
  val documentation: String?,
  val insertBefore: String = "",
  val insertSelect: String = "",
  val insertAfter: String = "",
  val newline: Boolean = false,
  val popup: Boolean = false
)
