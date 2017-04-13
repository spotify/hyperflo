package com.spotify.hyperflo.core

trait HypeModule[T] extends Serializable {
  def run: T

  def docker: String
}
