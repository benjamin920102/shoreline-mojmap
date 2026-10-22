package net.shoreline.client.util;

import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Stream;

public class StreamUtils {
   public static <T, U extends Comparable<? super U>> Stream<T> sortCached(Stream<T> stream, Function<? super T, ? extends U> keyExtractor) {
      return stream.<StreamUtils.Intermediary<T, U>>map(t -> {
         U key = (U)keyExtractor.apply(t);
         return new StreamUtils.Intermediary<T, U>(t, key);
      }).sorted(Comparator.comparing(StreamUtils.Intermediary::key)).map(StreamUtils.Intermediary::value);
   }

   private record Intermediary<T, U extends Comparable<? super U>>(T value, U key) {
   }
}
