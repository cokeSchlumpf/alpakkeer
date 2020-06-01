package alpakkeer.core.processes.actor.protocol;

import akka.actor.typed.ActorRef;
import alpakkeer.core.processes.model.ProcessStatusDetails;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(staticName = "apply")
public class StatusDetails implements Message {

   ActorRef<ProcessStatusDetails> replyTo;

}
