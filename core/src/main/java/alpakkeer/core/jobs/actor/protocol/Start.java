package alpakkeer.core.jobs.actor.protocol;

import akka.Done;
import akka.actor.typed.ActorRef;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(staticName = "apply")
public class Start<P, C> implements Message<P, C> {

   boolean queue;

   P properties;

   ActorRef<Done> replyTo;

   ActorRef<Throwable> errorTo;

}
