package task6;

import src.Course;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

// собственный collector: supplier -> accumulator -> combiner -> finisher
public final class DurationStatsCollector
    implements Collector<Course, DurationStatsAccumulator, DurationStats> {

  @Override
  public Supplier<DurationStatsAccumulator> supplier() {
    return DurationStatsAccumulator::new;
  }

  @Override
  public BiConsumer<DurationStatsAccumulator, Course> accumulator() {
    return DurationStatsAccumulator::add;
  }

  @Override
  public BinaryOperator<DurationStatsAccumulator> combiner() {
    return DurationStatsAccumulator::combine;
  }

  @Override
  public Function<DurationStatsAccumulator, DurationStats> finisher() {
    return DurationStatsAccumulator::finish;
  }

  @Override
  public Set<Characteristics> characteristics() {
    return Set.of(Characteristics.UNORDERED);
  }
}
