package alpakkeer.core.jobs.actor.protocol;

public final class Completed<P, C> implements Message<P, C> {

   private Completed() {

   }

    public static <P, C> Completed<P, C> apply() {
       return new Completed<>();
    }

}
