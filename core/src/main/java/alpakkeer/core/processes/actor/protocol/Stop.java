package alpakkeer.core.processes.actor.protocol;

import akka.Done;
import akka.actor.typed.ActorRef;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(staticName = "apply")
public class Stop implements Message {

   ActorRef<Done> replyTo;

}
