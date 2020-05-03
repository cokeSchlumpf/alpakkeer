package alpakkeer.core.jobs.actor.protocol;

import akka.Done;
import akka.actor.typed.ActorRef;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(staticName = "apply")
public class Stop<P> implements Message<P> {

   boolean clearQueue;

   ActorRef<Done> replyTo;

}
