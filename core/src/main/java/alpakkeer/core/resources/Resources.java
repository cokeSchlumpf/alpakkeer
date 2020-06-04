package alpakkeer.core.resources;

import alpakkeer.config.RuntimeConfiguration;
import alpakkeer.core.jobs.Job;
import alpakkeer.core.jobs.JobDefinition;
import alpakkeer.core.jobs.Jobs;
import alpakkeer.core.monitoring.MetricStore;
import alpakkeer.core.monitoring.values.TimeSeries;
import alpakkeer.core.processes.Process;
import alpakkeer.core.processes.ProcessDefinition;
import alpakkeer.core.processes.Processes;
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
public final class Resources {

   private final RuntimeConfiguration runtime;

   private final Map<Name, Job<?, ?>> jobs;

   private final Map<String, Process> processes;

   private final Map<String, MetricStore<TimeSeries>> tsMetrics;

   public static Resources apply(RuntimeConfiguration runtime) {
      return new Resources(runtime, Maps.newHashMap(), Maps.newHashMap(), Maps.newHashMap());
   }

   public <P, C> Job<P, C> addJob(JobDefinition<P, C> jobDefinition) {
      if (jobs.containsKey(jobDefinition.getName())) {
         throw JobAlreadyExistsException.apply(jobDefinition.getName());
      } else {
         var job = Jobs.apply(runtime.getSystem(), runtime.getScheduler(), runtime.getContextStore(), jobDefinition);
         job.getDefinition().extendApi(runtime.getApp(), job);
         jobs.put(jobDefinition.getName(), job);
         return job;
      }
   }

   public void addTimeSeriesMetric(MetricStore<TimeSeries> metric) {
      tsMetrics.put(metric.getName(), metric);
   }

   public Process addProcess(ProcessDefinition processDefinition) {
      if (processes.containsKey(processDefinition.getName().getValue())) {
         throw ProcessAlreadyExistsException.apply(processDefinition.getName());
      } else {
         var process = Processes.apply(runtime.getSystem(), processDefinition);
         process.getDefinition().extendApi(runtime.getApp(), process);
         processes.put(processDefinition.getName().getValue(), process);
         return process;
      }
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

   public List<Process> getProcesses() {
      return processes
         .values()
         .stream()
         .sorted(Comparator.comparing(p -> p.getDefinition().getName().getValue()))
         .collect(Collectors.toList());
   }

   public Optional<Process> getProcess(String name) {
      return Optional.ofNullable(processes.get(name));
   }

}
