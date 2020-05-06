package alpakkeer.core.jobs.actor.protocol;

import akka.Done;
import akka.actor.typed.ActorRef;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(staticName = "apply")
public class Stop<P, C> implements Message<P, C> {

   boolean clearQueue;

   ActorRef<Done> replyTo;

}
