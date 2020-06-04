package alpakkeer.core.jobs.actor.protocol;

import akka.actor.typed.ActorRef;
import alpakkeer.core.jobs.model.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(staticName = "apply")
public class Status<P, C> implements Message<P, C> {

   ActorRef<JobStatus> replyTo;

}
