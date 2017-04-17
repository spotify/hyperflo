package com.spotify.hyperflo.modules

import java.nio.charset.StandardCharsets

import com.google.common.hash.Hashing
import com.spotify.hyperflo.core.HypeModule
import com.spotify.scio.ContextAndArgs
import org.slf4j.LoggerFactory


case class ScioSplit(scioArguments: Array[String],
                     input: String,
                     destinations: (String, Double)*) extends HypeModule[Unit] {

  @transient private lazy val log = LoggerFactory.getLogger(classOf[ScioSplit])

  private val seed = 42
  private val totalBuckets = 1000000 // resolution

  override def run: Unit = {

    val (sc, _) = ContextAndArgs(scioArguments)

    val bucketRanges: List[Int] = destinations.map {
      case (_, percent) => (percent * totalBuckets).toInt
    }.toList

    assert(bucketRanges.sum == totalBuckets)

    log.info(s"Splitting $input into ${
      destinations.map {
        case (fn, pc) => s"$fn ($pc)"
      }.mkString(", ")
    }")

    val partitions = sc.textFile(input)
      .partition(bucketRanges.size, line => {
        var h = Hashing.murmur3_32(seed)
          .hashString(line, StandardCharsets.UTF_8)
          .asInt() % totalBuckets

        // Linear bucket search
        var b = 0
        while (h > bucketRanges(b)) {
          h -= bucketRanges(b)
          b += 1
        }
        b
      })

    destinations.map(_._1).zip(partitions).map {
      case (dirname, partition) =>
        partition.saveAsTextFile(dirname, numShards = 1)
    }

    sc.close().waitUntilDone()
  }
}
