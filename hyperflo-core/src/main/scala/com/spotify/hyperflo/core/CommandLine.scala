package com.spotify.hyperflo.core

import com.spotify.hype.HFn
import org.slf4j.LoggerFactory

import scala.sys.process._


trait CommandLine extends HFn[Int] {

  @transient private lazy val log = LoggerFactory.getLogger(classOf[CommandLine])

  def handleReturnCode(rc: Int): Unit = {
    if (rc != 0) {
      log.error(s"`$getCmd` failed - return code $rc")
      System.exit(rc)
    }
  }

  def getCmd: String

  override def run: Int = {
    val cmd = getCmd
    log.info("Executing: " + cmd)
    val rc = (cmd !)
    handleReturnCode(rc)
    rc
  }

  override def image: String = "us.gcr.io/datawhere-test/hype-examples-base:4"
}
