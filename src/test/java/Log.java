import com.netflix.config.ConfigurationManager;
import com.netflix.hystrix.HystrixCommand;

import java.util.NoSuchElementException;

public class Log {
    public static void log(Object msg) {
        System.out.println(Thread.currentThread().getName() + ": " + msg.toString());
    }

    public static void log() {
        System.out.println("");
    }

    public static synchronized void preExecuteDebug(HystrixCommand command) {
        log();
        log("PRE-COMMAND");
        log("Group: " + command.getCommandGroup().name() + ", Key: " + command.getCommandKey().name());
        log("ThreadPoolKey                               : " + command.getThreadPoolKey().name());
        log("RequestCacheEnabled                         : " + command.getProperties().requestCacheEnabled().get());
        log("RequestLogEnabled                           : " + command.getProperties().requestLogEnabled().get());
        log("Fallback:");
        log("  Enabled                                   : " + command.getProperties().fallbackEnabled().get());
        log("  IsolationSemaphoreMaxConcurrentRequests   : " + command.getProperties().fallbackIsolationSemaphoreMaxConcurrentRequests().get());
        log("CircuitBreaker:");
        log("  Enabled                                   : " + command.getProperties().circuitBreakerEnabled().get());
        log("  ForceClosed                               : " + command.getProperties().circuitBreakerForceClosed().get());
        log("  ForceOpen                                 : " + command.getProperties().circuitBreakerForceOpen().get());
        log("  IsOpen                                    : " + command.isCircuitBreakerOpen());
        log("  ErrorThresholdPercentage                  : " + command.getProperties().circuitBreakerErrorThresholdPercentage().get());
        log("  RequestVolumeThreshold                    : " + command.getProperties().circuitBreakerRequestVolumeThreshold().get());
        log("  SleepWindowInMilliseconds                 : " + command.getProperties().circuitBreakerSleepWindowInMilliseconds().get());
        log("ExecutionIsolation:");
        log("  Strategy                                  : " + command.getProperties().executionIsolationStrategy().get());
        log("  SemaphoreMaxConcurrentRequests            : " + command.getProperties().executionIsolationSemaphoreMaxConcurrentRequests().get());
        log("  ThreadPoolKeyOverride                     : " + command.getProperties().executionIsolationThreadPoolKeyOverride().get());
        log("  ThreadInterruptOnTimeout                  : " + command.getProperties().executionIsolationThreadInterruptOnTimeout().get());
        log("  ThreadTimeoutInMilliseconds               : " + command.getProperties().executionIsolationThreadTimeoutInMilliseconds().get());
        log("Threading properties:");
        debugThreadIsolation(command, "coreSize");
        debugThreadIsolation(command, "queueSizeRejectionThreshold");
        debugThreadIsolation(command, "maxQueueSize");
        log();
    }

    public static void debugThreadIsolation(HystrixCommand command, String property) {
        try {
            Integer coreSize = ConfigurationManager.getConfigInstance().getInt("hystrix.threadpool." + command.getThreadPoolKey().name() + "." + property);
            log("  threadpool." + property + " (group) : " + coreSize);
        } catch (NoSuchElementException nse1) {
            try {
                Integer coreSize = ConfigurationManager.getConfigInstance().getInt("hystrix.threadpool.default." + property);
                log("  threadpool." + property + " (default) : " + coreSize);
            } catch (NoSuchElementException nse2) {
                log("  threadpool." + property + " (code) : unknown");
            }
        }
    }

    public static synchronized void postExecuteDebug(HystrixCommand command) {
        log();
        log("POST-COMMAND");
        log("Group: " + command.getCommandGroup().name() + ", Key: " + command.getCommandKey().name());
        log("ExecutionEvents                : " + command.getExecutionEvents());
        log("ExecutionTimeInMilliseconds    : " + command.getExecutionTimeInMilliseconds());
        log("ExecutionComplete              : " + command.isExecutionComplete());
        log("ExecutedInThread               : " + command.isExecutedInThread());
        log("SuccessfulExecution            : " + command.isSuccessfulExecution());
        log("FailedExecution                : " + command.isFailedExecution());
        log("FailedExecutionException       : " + command.getFailedExecutionException());
        log("ResponseRejected               : " + command.isResponseRejected());
        log("ResponseShortCircuited         : " + command.isResponseShortCircuited());
        log("ResponseTimedOut               : " + command.isResponseTimedOut());
        log("ResponseFromCache              : " + command.isResponseFromCache());
        log("ResponseFromFallback           : " + command.isResponseFromFallback());
        log();
    }


}
