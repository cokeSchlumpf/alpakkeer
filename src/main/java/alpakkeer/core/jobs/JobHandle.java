package alpakkeer.core.jobs;

import akka.Done;

import java.util.concurrent.CompletionStage;

public interface JobHandle {

   CompletionStage<Done> getCompletion();

   CompletionStage<Done> stop();

}
