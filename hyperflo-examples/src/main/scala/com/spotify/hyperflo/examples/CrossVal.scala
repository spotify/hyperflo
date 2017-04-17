package com.spotify.hyperflo.examples

import com.spotify.hype.ContainerEngineCluster.containerEngineCluster
import com.spotify.hype.model.Secret.secret
import com.spotify.hype.model.{ResourceRequest, RunEnvironment, VolumeRequest}
import com.spotify.hype.util.Fn
import com.spotify.hype.{ContainerEngineCluster, Submitter}
import com.spotify.hyperflo.core.GSUtilCp
import com.spotify.hyperflo.modules.{MissingWordAccuracy, ScioSplit, Word2vec}
import org.slf4j.LoggerFactory

object CrossVal {

  private val log = LoggerFactory.getLogger(CrossVal.getClass)

  // GCP params
  val project = "datawhere-test"
  val staging = "gs://hype-test"

  // Mount disk
  val ssd: VolumeRequest = VolumeRequest.volumeRequest("fast", "20Gi")
  val mount: String = "/usr/share/volume"

  // Init scio context
  val scioArguments = Array(
    s"--project=$project",
    s"--stagingLocation=$staging/scio",
    "--runner=DataflowRunner"
  )

  def run(submitter: Submitter): Unit = {

    val gcsInput = "gs://hype-test/data/wiki/WestburyLab.Wikipedia.Corpus.1GB.txt"

    // Scio Split
    val baseScioData = "gs://hype-test/data/wiki1GB"
    val trainingSet = baseScioData + "/training"
    val testSet = baseScioData + "/test"
    val scioSplit = ScioSplit(scioArguments, gcsInput, trainingSet -> .9, testSet -> .1)
    submitter.runOnCluster(scioSplit.run, getEnv(scioSplit.docker))

    // DL training data locally
    val localTrainingSet = mount + "/training.txt"
    val dlTrainingData = GSUtilCp(trainingSet + "/*.txt", localTrainingSet)

    submitter.runOnCluster(dlTrainingData.run, getEnv(dlTrainingData.docker)
      .withMount(ssd.mountReadWrite(mount)))


    // Train Models
    val w2vOutput = "gs://hype-test/output/w2v/models"

    val cpus = 16
    val models = Seq(
      Word2vec(localTrainingSet, w2vOutput + "/1.bin", "threads" -> cpus.toString, "cbow" -> "0"),
      Word2vec(localTrainingSet, w2vOutput + "/2.bin", "threads" -> cpus.toString, "cbow" -> "1")
    )

    val tokens = for (model <- models.par)
      yield submitter.runOnCluster(model.run, getEnv(model.docker)
        .withMount(ssd.mountReadOnly(mount))
        .withRequest(ResourceRequest.CPU.of(cpus.toString)))

    // DL eval data locally
    val localTestSet = mount + "/test.txt"
    val dlTestData = GSUtilCp(testSet + "/*.txt", localTestSet)

    submitter.runOnCluster(dlTestData.run, getEnv(dlTestData.docker)
      .withMount(ssd.mountReadWrite(mount)))

    // Evaluate Models
    val evals = for (token <- tokens.par; eval = MissingWordAccuracy(localTestSet, token))
      yield submitter.runOnCluster(eval.run, getEnv(eval.docker)
        .withMount(ssd.mountReadOnly(mount)))
    evals.map(_.toMap.toString()).foreach(log.info)
  }

  def main(args: Array[String]): Unit = {
    val cluster: ContainerEngineCluster = containerEngineCluster(
      project, "us-east1-d", "hype-ml-test")
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
