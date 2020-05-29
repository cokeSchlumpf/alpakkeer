package alpakkeer.core.resources;

import akka.actor.ActorSystem;
import alpakkeer.core.jobs.ContextStore;
import alpakkeer.core.jobs.Job;
import alpakkeer.core.jobs.JobDefinition;
import alpakkeer.core.jobs.Jobs;
import alpakkeer.core.monitoring.MetricStore;
import alpakkeer.core.monitoring.values.TimeSeries;
import alpakkeer.core.scheduler.CronScheduler;
import alpakkeer.core.values.Name;
import com.google.common.collect.Maps;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Resources {

   private final ActorSystem system;

   private final CronScheduler scheduler;

   private final ContextStore contextStore;

   private final Map<Name, Job<?, ?>> jobs;

   private final Map<String, MetricStore<TimeSeries>> tsMetrics;

   public static Resources apply(ActorSystem system, CronScheduler scheduler, ContextStore contextStore) {
      return new Resources(system, scheduler, contextStore, Maps.newHashMap(), Maps.newHashMap());
   }

   public <P, C> Job<P, C> addJob(JobDefinition<P, C> jobDefinition) {
      if (jobs.containsKey(jobDefinition.getName())) {
         throw JobAlreadyExistsException.apply(jobDefinition.getName());
      } else {
         var job = Jobs.apply(system, scheduler, contextStore, jobDefinition);
         jobs.put(jobDefinition.getName(), job);
         return job;
      }
   }

   public void addTimeSeriesMetric(MetricStore<TimeSeries> metric) {
      tsMetrics.put(metric.getName(), metric);
   }

   public List<Job<?, ?>> getJobs() {
      return jobs
         .values()
         .stream()
         .sorted(Comparator.comparing(j -> j.getDefinition().getName().getValue()))
         .collect(Collectors.toList());
   }

   public Optional<Job<?, ?>> getJob(Name name) {
      return Optional.ofNullable(jobs.get(name));
   }

   public List<MetricStore<TimeSeries>> getTimeSeriesMetrics() {
      return List.copyOf(tsMetrics.values());
   }

}
