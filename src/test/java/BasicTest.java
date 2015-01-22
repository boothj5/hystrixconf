import com.netflix.hystrix.*;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertTrue;

public class BasicTest {

    private static boolean hystrixDebug = true;

    @Test
    public void callsFallbackFromHystrixThread() {
        HystrixCommand<String> command = new HystrixCommand<String>(HystrixCommand.Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey("GRP_FALLBACK"))
                .andCommandKey(HystrixCommandKey.Factory.asKey("CMD_FALLBACK"))) {

            @Override
            protected String run() throws Exception {
                Thread.currentThread().setName("HYSTRIX-THREAD");
                throw new RuntimeException();
            }

            @Override
            protected String getFallback() {
                return Thread.currentThread().getName();
            }
        };

        preExecuteDebug(command);
        String result = command.execute();
        assertEquals("HYSTRIX-THREAD", result);
    }

    @Test
    public void throwsRuntimeExceptionWhenFallbackDisabled() {
        HystrixCommand<String> command = new HystrixCommand<String>(HystrixCommand.Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey("GRP_DISABLE_FALLBACK"))
                .andCommandKey(HystrixCommandKey.Factory.asKey("CMD_DISABLE_FALLBACK"))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withFallbackEnabled(false))) {

            @Override
            protected String run() throws Exception {
                throw new RuntimeException();
            }

            @Override
            protected String getFallback() {
                fail("Unexpected call to fallback");
                return null;
            }
        };

        try {
            preExecuteDebug(command);
            command.execute();
            fail("Expected HystrixRuntimeException");
        } catch (Exception e) {
            assertTrue(e instanceof HystrixRuntimeException);
        }
    }

    @Test(timeout = 1000)
    public void threadPoolNotEmptiedOnPropertySet() throws InterruptedException {
        final HystrixCommand<String> command1 = new HystrixCommand<String>(HystrixCommand.Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey("GRP_RESET_QUEUE"))
                .andCommandKey(HystrixCommandKey.Factory.asKey("CMD_RESET_QUEUE1"))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withExecutionIsolationThreadTimeoutInMilliseconds(500))
                .andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter()
                        .withCoreSize(1)
                        .withQueueSizeRejectionThreshold(1))) {

            @Override
            protected String run() throws Exception {
                Thread.sleep(600);
                return "done";
            }

            @Override
            protected String getFallback() {
                return "fallback1";
            }
        };

        preExecuteDebug(command1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                command1.execute();
            }
        }).start();

        // give the first thread time to get into run() method
        Thread.sleep(300);

        final HystrixCommand<String> command2 = new HystrixCommand<String>(HystrixCommand.Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey("GRP_RESET_QUEUE"))
                .andCommandKey(HystrixCommandKey.Factory.asKey("CMD_RESET_QUEUE2"))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withExecutionIsolationThreadTimeoutInMilliseconds(500))
                .andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter()
                        .withCoreSize(1)
                        .withQueueSizeRejectionThreshold(1))) {

            @Override
            protected String run() throws Exception {
                return "done";
            }

            @Override
            protected String getFallback() {
                return "fallback2";
            }
        };

        preExecuteDebug(command2);
        String result2 = command2.execute();
        postExecuteDebug(command2);
        assertEquals("fallback2", result2);
        assertTrue(command2.isResponseRejected());

        // To make sure we get the post command output from the sleeping thread
        Thread.sleep(300);
        postExecuteDebug(command1);
        assertTrue(command1.isResponseTimedOut());
    }

    private void log(Object msg) {
        System.out.println(Thread.currentThread().getName() + ": " + msg.toString());
    }

    private void log() {
        System.out.println("");
    }

    private synchronized void preExecuteDebug(HystrixCommand command) {
        if (hystrixDebug) {
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
//            try {
//                Integer coreSize = ConfigurationManager.getConfigInstance().getInt("hystrix.threadpool." + command.getThreadPoolKey().name() + ".coreSize");
//                log("  CoreSize command property                 : " + coreSize);
//            } catch (NoSuchElementException nse) {
//                try {
//                    Integer coreSize = ConfigurationManager.getConfigInstance().getInt("hystrix.threadpool.default.coreSize");
//                    log("  CoreSize default property                 : " + coreSize);
//                } catch (NoSuchElementException nse2) {
//                    log("  CoreSize code default                     : " + 10);
//                }
//            }
            log("  ThreadPoolKeyOverride                     : " + command.getProperties().executionIsolationThreadPoolKeyOverride().get());
            log("  ThreadInterruptOnTimeout                  : " + command.getProperties().executionIsolationThreadInterruptOnTimeout().get());
            log("  ThreadTimeoutInMilliseconds               : " + command.getProperties().executionIsolationThreadTimeoutInMilliseconds().get());
            log();
        }
    }

    private synchronized void postExecuteDebug(HystrixCommand command) {
        if (hystrixDebug) {
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
}
