package alpakkeer.samples;

import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import alpakkeer.core.config.Configs;
import com.typesafe.config.Config;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterApplication {

   public static void main(String ...args) throws TwitterException {
      Config config = Configs.application.getConfig("twitter");

      ConfigurationBuilder cb = new ConfigurationBuilder();
      cb.setDebugEnabled(true)
         .setOAuthConsumerKey(config.getString("api-key"))
         .setOAuthConsumerSecret(config.getString("api-key-secret"))
         .setOAuthAccessToken(config.getString("access-token"))
         .setOAuthAccessTokenSecret(config.getString("access-token-secret"));
      TwitterFactory tf = new TwitterFactory(cb.build());
      Twitter twitter = tf.getInstance();

      twitter
         .getHomeTimeline()
         .stream()
         .limit(100)
         .forEach(status -> {
            System.out.println("---");
            System.out.println(status.getUser().getName() + ", " + status.getUser().getScreenName());
            System.out.println(status.getText());
            System.out.println("---");
         });
   }

}
