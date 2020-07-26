package alpakkeer.core.resources;

import alpakkeer.javadsl.AlpakkeerRuntime;
import alpakkeer.core.jobs.Job;
import alpakkeer.core.jobs.JobDefinition;
import alpakkeer.core.jobs.Jobs;
import alpakkeer.core.monitoring.MetricStore;
import alpakkeer.core.monitoring.values.TimeSeries;
import alpakkeer.core.processes.Process;
import alpakkeer.core.processes.ProcessDefinition;
import alpakkeer.core.processes.Processes;
import com.google.common.collect.Maps;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The Alpakkeer resource manager controls all existing resources, such as jobs, process, etc.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Resources {

   private final AlpakkeerRuntime runtime;

   private final Map<String, Job<?, ?>> jobs;

   private final Map<String, Process> processes;

   private final Map<String, MetricStore<TimeSeries>> tsMetrics;

   /**
    * Creates a new instance.
    *
    * @param runtime Alpakkeer's runtime configuration.
    * @return A new resources instance.
    */
   public static Resources apply(AlpakkeerRuntime runtime) {
      return new Resources(runtime, Maps.newHashMap(), Maps.newHashMap(), Maps.newHashMap());
   }

   /**
    * Adds a new job. The job name must be unique.
    *
    * @param jobDefinition The definition of the job
    * @param <P> The properties type of the job
    * @param <C> The context type of the job
    * @return The job instance
    */
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

   /**
    * Adds a custom time-series metric for exposure to Grafana, etc.. The name of the metric must be unique.
    *
    * @param metric The custom metric definition
    */
   public void addTimeSeriesMetric(MetricStore<TimeSeries> metric) {
      tsMetrics.put(metric.getName(), metric);
   }

   /**
    * Adds a process. The name of the process must be unique.
    *
    * @param processDefinition The definition of the process
    * @return The process instance
    */
   public Process addProcess(ProcessDefinition processDefinition) {
      if (processes.containsKey(processDefinition.getName())) {
         throw ProcessAlreadyExistsException.apply(processDefinition.getName());
      } else {
         var process = Processes.apply(runtime.getSystem(), processDefinition);
         process.getDefinition().extendApi(runtime.getApp(), process);
         processes.put(processDefinition.getName(), process);
         return process;
      }
   }

   /**
    * Like {{@link Resources#findJob(String)}} but will throw {@link JobNotFoundException} if job is not found.
    *
    * @param name The name of the job
    * @param <P> The property type of the job
    * @param <C> The context type of the job
    * @return The job
    */
   @SuppressWarnings("unchecked")
   public <P, C> Job<P, C> getJob(String name) {
      return (Job<P, C>) findJob(name).orElseThrow(() -> JobNotFoundException.apply(name));
   }

   /**
    * Like {@link Resources#getJob(String)} but with additional type hints.
    *
    * @param name The name of the job
    * @param pType The property type of the job
    * @param cType The context type of the job
    * @param <P> The property type
    * @param <C> The context type
    * @return The job
    */
   @SuppressWarnings("unchecked")
   public <P, C> Job<P, C> getJob(String name, Class<P> pType, Class<C> cType) {
      return (Job<P, C>) findJob(name).orElseThrow(() -> JobNotFoundException.apply(name));
   }

   /**
    * Returns a list of registered jobs.
    *
    * @return The job list
    */
   public List<Job<?, ?>> getJobs() {
      return jobs
         .values()
         .stream()
         .sorted(Comparator.comparing(j -> j.getDefinition().getName()))
         .collect(Collectors.toList());
   }

   /**
    * Find a job by its name.
    *
    * @param name The name of the job
    * @return Maybe a job or None of not found
    */
   public Optional<Job<?, ?>> findJob(String name) {
      return Optional.ofNullable(jobs.get(name));
   }

   /**
    * Returns the list of registered time-series metrics.
    *
    * @return The list of metrics
    */
   public List<MetricStore<TimeSeries>> getTimeSeriesMetrics() {
      return List.copyOf(tsMetrics.values());
   }

   /**
    * Returns a list of registered processes.
    *
    * @return The list of processes.
    */
   public List<Process> getProcesses() {
      return processes
         .values()
         .stream()
         .sorted(Comparator.comparing(p -> p.getDefinition().getName()))
         .collect(Collectors.toList());
   }

   public Process getProcess(String name) {
      return findProcess(name).orElseThrow(() -> ProcessNotFoundException.apply(name));
   }

   /**
    * Find a process by its name.
    *
    * @param name The name of the process
    * @return Maybe the process or None if the process was not found
    */
   public Optional<Process> findProcess(String name) {
      return Optional.ofNullable(processes.get(name));
   }

}
