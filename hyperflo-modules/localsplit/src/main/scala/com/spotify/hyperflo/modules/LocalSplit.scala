package com.spotify.hyperflo.modules

import java.io.{File, PrintWriter}

import com.spotify.hyperflo.core.HypeModule
import org.slf4j.LoggerFactory

import scala.io.Source
import scala.util.Random

case class LocalSplit(input: String, destinations: (String, Double)*) extends HypeModule[Unit] {

  @transient private lazy val log = LoggerFactory.getLogger(classOf[LocalSplit])


  override def run: Unit = {
    val probas = destinations.map(_._2).toList

    assert(Math.abs(probas.sum - 1) < 0.000001)

    log.info(s"Splitting $input into ${
      destinations.map {
        case (fn, pc) => s"$fn ($pc)"
      }.mkString(", ")
    }")

    val writers = destinations.map { case (filename, _) => new PrintWriter(new File(filename))
    }.toList

    val rng = Random
    rng.setSeed(42)

    for (line <- Source.fromFile(input).getLines()) {
      var r = rng.nextDouble()
      var i = 0
      while (r > probas(i)) {
        r -= probas(i)
        i += 1
      }
      writers(i).println(line)
    }
    writers.foreach(_.close)
  }

  override def docker: String = "us.gcr.io/datawhere-test/hype-examples-base:4" // FIXME
}
