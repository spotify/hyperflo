package com.spotify.hyperflo.examples

import com.spotify.hype.Submitter
import org.scalatest.FlatSpec

class CrossValTest extends FlatSpec {

  val submitter = Submitter.createLocal()

  CrossVal.run(submitter)

}
