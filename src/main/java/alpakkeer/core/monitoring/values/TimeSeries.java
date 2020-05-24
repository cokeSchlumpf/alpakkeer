package alpakkeer.core.monitoring.values;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.List;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TimeSeries {

   List<DataPoint> data;

   public static TimeSeries apply(List<DataPoint> data) {
      return new TimeSeries(List.copyOf(data));
   }

}
