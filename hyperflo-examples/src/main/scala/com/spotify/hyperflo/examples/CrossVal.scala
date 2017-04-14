package com.spotify.hyperflo.examples

import com.spotify.hype.ContainerEngineCluster.containerEngineCluster
import com.spotify.hype.model.Secret.secret
import com.spotify.hype.model.{RunEnvironment, VolumeRequest}
import com.spotify.hype.util.Fn
import com.spotify.hype.{ContainerEngineCluster, Submitter}
import com.spotify.hyperflo.core.GSUtilCp
import com.spotify.hyperflo.modules.{LexVec, LocalSplit, MissingWordAccuracy, Word2vec}
import org.slf4j.LoggerFactory

object CrossVal {

  private val log = LoggerFactory.getLogger(CrossVal.getClass)

  // Mount disk
  val ssd: VolumeRequest = VolumeRequest.volumeRequest("fast", "20Gi")
  val mount: String = "/usr/share/volume"

  def run(submitter: Submitter): Unit = {

    // Get training data
    val gcsInput = "gs://hype-test/data/wiki/WestburyLab.Wikipedia.Corpus.10MB.txt"
    val localInput = mount + "/input.txt"

    val gsUtilCp = GSUtilCp(gcsInput, localInput)
    submitter.runOnCluster(gsUtilCp.run, getEnv(gsUtilCp.docker)
      .withMount(ssd.mountReadWrite(mount)))

    // Split data
    val trainingSet = mount + "/training.txt"
    val testSet = mount + "/test.txt"
    val localSplit = LocalSplit(localInput, trainingSet -> .8, testSet -> .2)
    submitter.runOnCluster(localSplit.run, getEnv(localSplit.docker)
      .withMount(ssd.mountReadWrite(mount)))

    // Train Models
    val w2vOutput = "gs://hype-test/output/w2v/model.txt"
    val lexVecOutput = "gs://hype-test/output/lexvec/model.txt"

    val models = Seq(
      Word2vec(trainingSet, w2vOutput),
      LexVec(trainingSet, w2vOutput)
    )

    val tokens = for (model <- models.par)
      yield submitter.runOnCluster(model.run, getEnv(model.docker)
        .withMount(ssd.mountReadOnly(mount)))

    // Evaluate Models
    val evals = for (token <- tokens.par; eval = MissingWordAccuracy(testSet, token))
      yield submitter.runOnCluster(eval.run, getEnv(eval.docker)
        .withMount(ssd.mountReadOnly(mount)))
    evals.map(_.toMap.toString()).foreach(log.info)
  }

  def main(args: Array[String]): Unit = {
    val cluster: ContainerEngineCluster = containerEngineCluster(
      "datawhere-test", "us-east1-d", "hype-ml-test")
    val submitter: Submitter = Submitter.create("gs://hype-test", cluster)

    try {
      run(submitter)
    } finally {
      submitter.close()
    }
  }

  def getEnv(image: String) = RunEnvironment.environment(
    image,
    secret("gcp-key", "/etc/gcloud"))


  implicit def funToFn[T](f: => T): Fn[T] = {
    new Fn[T] {
      override def run(): T = f
    }
  }
}
