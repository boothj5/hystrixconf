import com.netflix.config.ConfigurationManager;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertTrue;

public class BasicTest {

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

        Log.preExecuteDebug(command);
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
            Log.preExecuteDebug(command);
            command.execute();
            fail("Expected HystrixRuntimeException");
        } catch (Exception e) {
            assertTrue(e instanceof HystrixRuntimeException);
        }
    }

    @Test(timeout = 1000)
    public void threadPoolNotEmptiedOnPropertySet() throws InterruptedException {
        ConfigurationManager.getConfigInstance().setProperty("hystrix.threadpool.GRP_RESET_QUEUE.coreSize", String.valueOf(1));
        ConfigurationManager.getConfigInstance().setProperty("hystrix.threadpool.GRP_RESET_QUEUE.queueSizeRejectionThreshold", String.valueOf(1));

        final HystrixCommand<String> command1 = new HystrixCommand<String>(HystrixCommand.Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey("GRP_RESET_QUEUE"))
                .andCommandKey(HystrixCommandKey.Factory.asKey("CMD_RESET_QUEUE1"))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withExecutionIsolationThreadTimeoutInMilliseconds(500))) {

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

        Log.preExecuteDebug(command1);
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
                        .withExecutionIsolationThreadTimeoutInMilliseconds(500))) {

            @Override
            protected String run() throws Exception {
                return "done";
            }

            @Override
            protected String getFallback() {
                return "fallback2";
            }
        };

        Log.preExecuteDebug(command2);
        String result2 = command2.execute();
        Log.postExecuteDebug(command2);
        assertEquals("fallback2", result2);
        assertTrue(command2.isResponseRejected());

        // To make sure we get the post command output from the sleeping thread
        Thread.sleep(300);
        Log.postExecuteDebug(command1);
        assertTrue(command1.isResponseTimedOut());
    }
}
