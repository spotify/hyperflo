package com.spotify.hyperflo.modules

import com.spotify.hyperflo.core.CmdLineEmbeddingModel

// FIXME: mutli-threading appears to be broken in docker (but not locally)
case class LexVec(corpus: String,
                  gcsOutput: String,
                  args: (String, String)*) extends CmdLineEmbeddingModel {
  override def getCmd(localOutput: String): String = {
    s"lexvec -corpus $corpus -output $localOutput ${formatArgs(args)}"
  }

  override def getGcsOutput: String = gcsOutput

  override def image = "us.gcr.io/datawhere-test/hype-lexvec:1"
}
