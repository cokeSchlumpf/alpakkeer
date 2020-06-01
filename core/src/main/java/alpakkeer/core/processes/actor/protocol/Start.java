package alpakkeer.core.processes.actor.protocol;

import akka.Done;
import akka.actor.typed.ActorRef;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(staticName = "apply")
public class Start implements Message {

   ActorRef<Done> replyTo;

   ActorRef<Throwable> errorTo;

}
