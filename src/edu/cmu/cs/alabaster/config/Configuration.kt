package edu.cmu.cs.alabaster.config

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.Okio
import java.io.IOException
import java.io.InputStream

data class Configuration(
  val transforms: List<CompletionConfig> = emptyList(),
  val parameters: Map<String, List<CompletionConfig>> = emptyMap(),
  val subtransforms: Map<String, List<CompletionConfig>> = emptyMap()
) {

  companion object {
    operator fun invoke(file: InputStream): Configuration {

      val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
      val adapter = moshi.adapter(Configuration::class.java)
      return Okio.source(file)?.use {
        Okio.buffer(it)?.use { adapter.fromJson(it) }
      } ?: throw IOException("Unable to read config file")

    }
  }
}
