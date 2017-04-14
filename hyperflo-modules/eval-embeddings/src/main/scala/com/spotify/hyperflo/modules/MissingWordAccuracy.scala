package com.spotify.hyperflo.modules

import breeze.linalg.functions.cosineDistance
import breeze.linalg.{DenseVector, sum}
import breeze.stats._
import breeze.stats.distributions.{Multinomial, RandBasis}
import com.spotify.hyperflo.core.{CmdLineEmbeddingModel, CmdLineEmbeddingModelToken, HypeModule}

import scala.collection.mutable
import scala.io.Source

object MissingWordAccuracy {
  def mkGuess(sentenceVec: DenseVector[Double],
              targetWordVec: DenseVector[Double],
              randomWordVec: DenseVector[Double]): Boolean = {
    val contextVec = sentenceVec - targetWordVec
    cosineDistance(targetWordVec, contextVec) < cosineDistance(randomWordVec, contextVec)
  }
}

case class MissingWordAccuracy(testDataset: String,
                               modelToken: CmdLineEmbeddingModelToken
                              ) extends HypeModule[Seq[(String, String)]] {

  override def run: Seq[(String, String)] = {

    // Parse vectors
    val wordVecs = CmdLineEmbeddingModel.getVectors(modelToken)

    // Count words
    val wordCnt = Source.fromFile(testDataset)
      .getLines()
      .flatMap(_.split("\\W+"))
      .foldLeft(Map.empty[String, Int]) {
        (count, word) => count + (word -> (count.getOrElse(word, 0) + 1))
      }
      .filterKeys(wordVecs.contains)
    val words = wordCnt.keys.toList
    val totalWords = wordCnt.values.sum
    implicit val basis: RandBasis = RandBasis.withSeed(42)
    val wordMult = new Multinomial(
      DenseVector(wordCnt.map(_._2 / totalWords.toDouble).toArray))

    // Missing word accuracy
    val guesses = mutable.ListBuffer[Float]()
    for (lines <- Source.fromFile(testDataset).getLines().grouped(1024)) {
      for (line <- lines.par) {
        val sentence = line.split("\\W+").filter(wordVecs.contains).toList
        var rightGuesses = 0
        if (sentence.length >= 2) {
          for (word <- sentence) {
            val sentenceVec = sum(sentence.map(wordVecs))
            val rightGuess = if (MissingWordAccuracy.mkGuess(sentenceVec,
              wordVecs(word),
              wordVecs(words(wordMult.sample)))) 1
            else 0
            rightGuesses += rightGuess
          }
          this.synchronized {
            guesses += rightGuesses / sentence.length.toFloat
          }
        }
      }
    }

    // Some descriptive stats
    val d = DenseVector(guesses.toArray)
    List(
      "#sentences" -> guesses.length.toString,
      "mean" -> mean(d).toString,
      "stddev" -> stddev(d).toString,
      "median" -> median(d).toString
    )
  }
}
