package com.spotify.hyperflo.examples

import com.spotify.hype.ContainerEngineCluster.containerEngineCluster
import com.spotify.hype.model.Secret.secret
import com.spotify.hype.model.{RunEnvironment, VolumeRequest}
import com.spotify.hype.util.Fn
import com.spotify.hype.{ContainerEngineCluster, Submitter}
import com.spotify.hyperflo.modules.{GSUtilCp, LocalSplit}

object CrossVal {

  // Mount disk
  val ssd: VolumeRequest = VolumeRequest.volumeRequest("fast", "20Gi")
  val mount: String = "/usr/share/volume"

  def run(submitter: Submitter): Unit = {

    val gcsInput = "gs://hype-test/data/wiki/WestburyLab.Wikipedia.Corpus.10MB.txt"
    val localInput = mount + "/input.txt"

    val gsUtilCp = GSUtilCp(gcsInput, localInput)
    submitter.runOnCluster(gsUtilCp.run, getEnv(gsUtilCp.docker)
      .withMount(ssd.mountReadWrite(mount)))

    val trainingSet = mount + "/training.txt"
    val testSet = mount + "/test.txt"
    val localSplit = LocalSplit(localInput, trainingSet -> .8, testSet -> .2)
    submitter.runOnCluster(localSplit.run, getEnv(localSplit.docker)
      .withMount(ssd.mountReadWrite(mount)))
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
