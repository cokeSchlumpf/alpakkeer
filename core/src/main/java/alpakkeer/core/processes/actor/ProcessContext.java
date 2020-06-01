package alpakkeer.core.processes.actor;

import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.TimerScheduler;
import alpakkeer.core.processes.ProcessDefinition;
import alpakkeer.core.processes.actor.protocol.Message;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.slf4j.Logger;

@Value
@AllArgsConstructor(staticName = "apply")
public class ProcessContext {

   ActorContext<Message> actor;

   TimerScheduler<Message> scheduler;

   ProcessDefinition definition;

   Logger log;

}
