import org.junit.Test;
import org.springframework.jdbc.support.incrementer.AbstractColumnMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.AbstractDataFieldMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.MySQLMaxValueIncrementer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author kongtianyi
 * @date 2019/11/24
 */
public class MySQLMaxValueIncrementerTest extends AbstractDataFieldMaxValueIncrementerTest {

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
        for (Map.Entry<Integer, Future<List<Long>>> item: futureMap.entrySet()) {
            String fileName = "./col_result" + item.getKey() + ".txt";
            writeListToFile(fileName, item.getValue().get());
        }
        executorService.shutdown();
        // 保证所有的线程执行完毕主线程再退出
        executorService.awaitTermination(1, TimeUnit.HOURS);
        System.out.println(executorService.isTerminated());
    }

    protected AbstractColumnMaxValueIncrementer buildIncrementer(String colName) {
        MySQLMaxValueIncrementer incrementer;
        incrementer = new MySQLMaxValueIncrementer();
        incrementer.setCacheSize(cacheSize);
        incrementer.setColumnName(colName);
        incrementer.setIncrementerName("col_sequence");
        incrementer.setDataSource(dataSource);
        return incrementer;
    }

}
