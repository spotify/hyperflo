package com.spotify.hyperflo.modules

import com.spotify.hyperflo.core.CommandLine

import scala.sys.process._

case class GSUtilCp(inPath: String,
                    outPath: String) extends CommandLine {

  override def getCmd: String = {
    s"gcloud auth activate-service-account --key-file=${System.getenv("GOOGLE_APPLICATION_CREDENTIALS")}" !

    s"gsutil -m cp -r $inPath $outPath"
  }
}
