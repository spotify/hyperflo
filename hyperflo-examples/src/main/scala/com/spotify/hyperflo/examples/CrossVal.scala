package com.spotify.hyperflo.examples

import com.spotify.hype._
import com.spotify.hype.model.VolumeRequest
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

  def run(implicit submitter: HypeSubmitter): Unit = {
    val env = RunEnvironment().withSecret("gcp-key", "/etc/gcloud")

    // Scio Split
    val gcsInput = "gs://hype-test/data/wiki/WestburyLab.Wikipedia.Corpus.1GB.txt"
    val baseScioData = "gs://hype-test/data/wiki1GB"
    val trainingSet = baseScioData + "/training"
    val testSet = baseScioData + "/test"
    log.info("Running dataset split...")
    val scioSplit = ScioSplit(scioArguments, gcsInput, trainingSet -> .9, testSet -> .1)
//    submitter.submit(scioSplit, env)

    // DL training data locally
    log.info("Downloading training data locally...")
    val localTrainingSet = mount + "/training.txt"
    val dlTrainingData = GSUtilCp(trainingSet + "/*.txt", localTrainingSet)
    submitter.submit(dlTrainingData, env.withMount(ssd.mountReadWrite(mount)))

    // Train Models
    val w2vOutput = "gs://hype-test/output/w2v/models"

    val cpus = 16
    val models = Seq(
      Word2vec(localTrainingSet, w2vOutput + "/1.bin", "threads" -> cpus.toString, "cbow" -> "0"),
      Word2vec(localTrainingSet, w2vOutput + "/2.bin", "threads" -> cpus.toString, "cbow" -> "1")
    )

    log.info("Training word2vec models...")
    val tokens = for (model <- models.par)
      yield submitter.submit(model, env.withMount(ssd.mountReadOnly(mount))
        .withRequest("cpu", cpus.toString))

    // DL eval data locally
    log.info("Downloading test data locally...")
    val localTestSet = mount + "/test.txt"
    val dlTestData = GSUtilCp(testSet + "/*.txt", localTestSet)

    submitter.submit(dlTestData, env.withMount(ssd.mountReadWrite(mount)))

    // Evaluate Models
    log.info("Running evaluation...")
    val evals = for (token <- tokens.par; eval = MissingWordAccuracy(localTestSet, token))
      yield submitter.submit(eval, env.withMount(ssd.mountReadOnly(mount)))

    evals.map(_.toMap.toString()).foreach(log.info)
  }

  def main(args: Array[String]): Unit = {
    val submitter = GkeSubmitter(project, "us-east1-d", "hype-ml-test", "gs://hype-test")
    try {
      run(submitter)
    } finally {
      submitter.close()
    }
  }
}
