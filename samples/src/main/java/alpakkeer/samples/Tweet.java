package alpakkeer.samples;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@AllArgsConstructor(staticName = "apply")
public class Tweet {

   String id;

   LocalDateTime dateTime;

   String user;

   String text;

}
