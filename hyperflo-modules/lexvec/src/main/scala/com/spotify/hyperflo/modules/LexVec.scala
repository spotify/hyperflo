package com.spotify.hyperflo.modules

import com.spotify.hyperflo.core.CmdLineEmbeddingModel

case class LexVec(corpus: String,
                  gcsOutput: String,
                  args: (String, String)*) extends CmdLineEmbeddingModel {
  override def getCmd(localOutput: String): String = {
    s"lexvec -corpus $corpus -output $localOutput ${formatArgs(args)}"
  }

  override def getGcsOutput: String = gcsOutput

  override def docker: String = "us.gcr.io/datawhere-test/hype-lexvec:1"
}
