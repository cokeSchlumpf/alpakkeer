package alpakkeer.scaladsl

import alpakkeer.core.jobs.JobDefinitions.JobSettingsConfiguration
import alpakkeer.core.jobs.{JobDefinitions => JJobDefinitions, JobHandle => JJobHandle}

import scala.concurrent.Future
import scala.jdk.FutureConverters._
import scala.jdk.OptionConverters._

class JobDefinitions(d: JJobDefinitions) {



}

object JobDefinitions {

  class JobHandle[C](d: JJobHandle[C]) {

    def completion: Future[Option[C]] = {
      d.getCompletion.thenApply(_.toScala).asScala
    }

    def stop(): Unit = {
      d.stop()
    }

  }

  class JobStreamBuilder[P, C]

  class JobRunnableConfiguration[P, C](d: JJobDefinitions.JobRunnableConfiguration[P, C]) {

    def run(): JobSettingsConfiguration[P, C] = {
      ???
    }

  }

}