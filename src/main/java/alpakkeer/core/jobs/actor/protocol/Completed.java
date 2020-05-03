package alpakkeer.core.jobs.actor.protocol;

public final class Completed<P> implements Message<P> {

   private Completed() {

   }

    public static <P> Completed<P> apply() {
       return new Completed<>();
    }

}
