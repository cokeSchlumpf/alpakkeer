package alpakkeer.core.util;

public final class Strings {

   private Strings() {

   }

   public static StringConverters.Converter convert(String s) {
      return StringConverters.Converter(s);
   }

}
