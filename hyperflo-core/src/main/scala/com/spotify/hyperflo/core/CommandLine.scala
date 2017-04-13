package com.spotify.hyperflo.core

import org.slf4j.LoggerFactory

import scala.sys.process._


trait CommandLine extends HypeModule[Int] {

  @transient private lazy val log = LoggerFactory.getLogger(classOf[CommandLine])

  def getCmd: String

  override def run: Int = {
    val cmd = getCmd
    log.info("Executing: " + cmd)
    cmd !
  }

  override def docker: String = "us.gcr.io/datawhere-test/hype-examples-base:4" // FIXME

}
