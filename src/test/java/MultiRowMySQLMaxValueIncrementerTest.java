import com.kongtianyi.incrementer.MultiRowMySQLMaxValueIncrementer;
import org.junit.Test;
import org.springframework.jdbc.support.incrementer.AbstractDataFieldMaxValueIncrementer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author kongtianyi
 * @date 2019/11/17
 */
public class MultiRowMySQLMaxValueIncrementerTest extends AbstractDataFieldMaxValueIncrementerTest {

    @Test
    public void testGetNextLong() throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newCachedThreadPool();
        Map<Integer, Future<List<Long>>> futureMap = new HashMap<Integer, Future<List<Long>>>(seqNum);
        for (int i = 1; i <= seqNum; i++) {
            String seqName = "seq" + i;
            Future<List<Long>> future = null;
            for (int j = 0; j < seqThreadNum; j++) {
                future = executorService.submit(new IncrementTask(buildIncrementer(seqName)));
            }
            // 每个序列抽样保存最后一组
            futureMap.put(i, future);
        }
        for (Map.Entry<Integer, Future<List<Long>>> item : futureMap.entrySet()) {
            String fileName = "./row_result" + item.getKey() + ".txt";
            writeListToFile(fileName, item.getValue().get());
        }
        executorService.shutdown();
        // 保证所有的线程执行完毕主线程再退出
        executorService.awaitTermination(1, TimeUnit.HOURS);
        System.out.println(executorService.isTerminated());
    }

    protected AbstractDataFieldMaxValueIncrementer buildIncrementer(String seqName) {
        MultiRowMySQLMaxValueIncrementer incrementer;
        incrementer = new MultiRowMySQLMaxValueIncrementer();
        incrementer.setCacheSize(cacheSize);
        incrementer.setSeqName(seqName);
        incrementer.setIncrementerName("biz_sequence");
        incrementer.setDataSource(dataSource);
        return incrementer;
    }

}
