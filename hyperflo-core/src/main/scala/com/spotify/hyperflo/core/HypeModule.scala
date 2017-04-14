package com.spotify.hyperflo.core

trait HypeModule[T] extends Serializable {
  def run: T

  def docker: String = "us.gcr.io/datawhere-test/hype-examples-base:4" // FIXME
}
