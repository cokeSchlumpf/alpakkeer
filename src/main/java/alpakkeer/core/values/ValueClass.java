package alpakkeer.core.values;

public abstract class ValueClass<T> {

   abstract public T getValue();

   public String toString() {
      return getValue().toString();
   }

}
