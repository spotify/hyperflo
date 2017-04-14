package com.spotify.hyperflo.core

import java.nio.file.{Files, Paths}

import breeze.linalg.DenseVector
import org.slf4j.LoggerFactory

import scala.io.Source
import scala.sys.process._


case class CmdLineEmbeddingModelToken(gcsOuput: String)

object CmdLineEmbeddingModel {
  def getVectors(token: CmdLineEmbeddingModelToken): Map[String, DenseVector[Double]] = {
    // Download from GCS to a temp dir
    val tempDirectory = Files.createTempDirectory("")
    val localOutput = tempDirectory.resolve(Paths.get(token.gcsOuput).getFileName)
    GSUtilCp(token.gcsOuput, localOutput.toString).run
    localOutput.toFile.deleteOnExit()

    // Parse Vectors
    Source.fromFile(localOutput.toString)
      .getLines()
      .map(line => {
        val tokens = line.split("\\s", 2)
        print(tokens)
        tokens(0) -> DenseVector(tokens(1).split("\\s").map(_.toDouble))
      }).toMap
  }
}

trait CmdLineEmbeddingModel extends HypeModule[CmdLineEmbeddingModelToken] {

  @transient private lazy val log = LoggerFactory.getLogger(classOf[CommandLine])

  def getCmd(localOutput: String): String

  def getGcsOutput: String

  override def run: CmdLineEmbeddingModelToken = {
    // Temporary output file
    val tempDirectory = Files.createTempDirectory("")
    val localOutput = tempDirectory.resolve(Paths.get(getGcsOutput).getFileName)
    localOutput.toFile.deleteOnExit

    // Run model
    val cmd = getCmd(localOutput.toString)
    log.info("Executing: " + cmd)
    cmd !

    // Persist output into GCS
    GSUtilCp(localOutput.toString, getGcsOutput).run

    CmdLineEmbeddingModelToken(getGcsOutput)
  }

  def formatArgs(args: Seq[(String, String)]): String = {
    args.map { case (k, v) => s"-$k $v" }.mkString(" ")
  }
}
