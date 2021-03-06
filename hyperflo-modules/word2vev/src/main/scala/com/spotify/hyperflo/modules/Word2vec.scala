package com.spotify.hyperflo.modules

import com.spotify.hyperflo.core.CmdLineEmbeddingModel

/**
  * arguments: https://github.com/yonromai/word2vec/blob/master/src/word2vec.c#L672
  */
case class Word2vec(corpus: String,
                    gcsOutput: String,
                    args: (String, String)*) extends CmdLineEmbeddingModel {

  override def getCmd(localOutput: String): String = {
    s"word2vec -train $corpus -output $localOutput ${formatArgs(args)}"
  }

  override def getGcsOutput: String = gcsOutput

  override def image = "us.gcr.io/datawhere-test/hype-word2vec:9"
}
