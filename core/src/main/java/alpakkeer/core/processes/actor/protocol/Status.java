package alpakkeer.core.processes.actor.protocol;

import akka.actor.typed.ActorRef;
import alpakkeer.core.processes.model.ProcessStatus;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(staticName = "apply")
public class Status implements Message {

   ActorRef<ProcessStatus> replyTo;

}
