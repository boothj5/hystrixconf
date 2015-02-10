import com.netflix.config.ConfigurationManager;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class CircuitBreakerTest {
    @Test
    public void circuitBreakerOpen() throws InterruptedException {
        ConfigurationManager.getConfigInstance().setProperty("hystrix.command.CMD_CIRCUIT_OPEN.circuitBreaker.forceOpen", String.valueOf(true));

        final HystrixCommand<String> command = new HystrixCommand<String>(HystrixCommand.Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey("GRP_CIRCUIT"))
                .andCommandKey(HystrixCommandKey.Factory.asKey("CMD_CIRCUIT_OPEN"))) {

            @Override
            protected String run() throws Exception {
                return "done";
            }

            @Override
            protected String getFallback() {
                return "fallback";
            }
        };

        Log.preExecuteDebug(command);

        String result = command.execute();

        Log.postExecuteDebug(command);
        assertEquals("fallback", result);
//        assertTrue(command.isCircuitBreakerOpen());
    }
}
