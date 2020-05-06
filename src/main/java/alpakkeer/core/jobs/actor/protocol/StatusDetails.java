package alpakkeer.core.jobs.actor.protocol;

import akka.actor.typed.ActorRef;
import alpakkeer.core.jobs.model.JobStatus;
import alpakkeer.core.jobs.model.JobStatusDetails;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(staticName = "apply")
public class StatusDetails<P> implements Message<P> {

   ActorRef<JobStatusDetails<P>> replyTo;

}
