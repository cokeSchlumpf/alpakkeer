package alpakkeer.core.processes;

import akka.Done;
import akka.actor.ActorSystem;
import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.Adapter;
import alpakkeer.core.processes.actor.ProcessActor;
import alpakkeer.core.processes.actor.protocol.*;
import alpakkeer.core.processes.model.ProcessStatus;
import alpakkeer.core.processes.model.ProcessStatusDetails;
import alpakkeer.core.util.ActorPatterns;
import lombok.AllArgsConstructor;

import java.util.concurrent.CompletionStage;

public final class Processes {

   private Processes() {

   }

   @AllArgsConstructor(staticName = "apply")
   private static class ActorProcess implements Process {

      private final ProcessDefinition definition;

      private final ActorRef<Message> actor;

      private final ActorPatterns patterns;

      @Override
      public ProcessDefinition getDefinition() {
         return definition;
      }

      @Override
      public CompletionStage<Done> start() {
         return patterns.ask(actor, Start::apply);
      }

      @Override
      public CompletionStage<Done> stop() {
         return patterns.ask(actor, Stop::apply);
      }

      @Override
      public CompletionStage<ProcessStatus> getStatus() {
         return patterns.ask(actor, Status::apply);
      }

      @Override
      public CompletionStage<ProcessStatusDetails> getStatusDetails() {
         return patterns.ask(actor, StatusDetails::apply);
      }

   }

   public static Process apply(ActorSystem system, ProcessDefinition definition) {
      var behavior = ProcessActor.create(definition);
      var actor = Adapter.spawn(system, behavior, definition.getName());
      var process = ActorProcess.apply(definition, actor, ActorPatterns.apply(system));

      if (definition.isInitiallyStarted()) {
         process.start();
      }

      return process;
   }

}
