package alpakkeer.core.jobs.actor.protocol;

import akka.actor.typed.ActorRef;
import alpakkeer.core.jobs.model.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(staticName = "apply")
public class Status<P> implements Message<P> {

   ActorRef<JobStatus<P>> replyTo;

}
