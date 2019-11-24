import org.springframework.jdbc.support.incrementer.AbstractDataFieldMaxValueIncrementer;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author kongtianyi
 * @date 2019/11/24
 */
public class IncrementTask implements Callable<List<Long>> {

    private AbstractDataFieldMaxValueIncrementer incrementer;
    private int loopTime = 10000;

    public IncrementTask(AbstractDataFieldMaxValueIncrementer incrementer) {
        this.incrementer = incrementer;
    }

    public IncrementTask(AbstractDataFieldMaxValueIncrementer incrementer, int loopTime) {
        this.incrementer = incrementer;
        this.loopTime = loopTime;
    }

    public List<Long> call() throws Exception {
        List<Long> costs = new LinkedList<Long>();
        for (int i = 0; i < loopTime; i++) {
            long startTime = System.currentTimeMillis();
            incrementer.nextLongValue();
            costs.add(System.currentTimeMillis() - startTime);
        }
        return costs;
    }
}
