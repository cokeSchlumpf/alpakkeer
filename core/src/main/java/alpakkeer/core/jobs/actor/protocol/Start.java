package alpakkeer.core.jobs.actor.protocol;

import akka.actor.typed.ActorRef;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.concurrent.CompletionStage;

@Value
@AllArgsConstructor(staticName = "apply")
public class Start<P, C> implements Message<P, C> {

   boolean queue;

   P properties;

   ActorRef<CompletionStage<C>> replyTo;

   ActorRef<Throwable> errorTo;

}
