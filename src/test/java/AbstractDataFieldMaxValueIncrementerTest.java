import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.jdbc.support.incrementer.AbstractDataFieldMaxValueIncrementer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author kongtianyi
 * @date 2019/11/24
 */
public abstract class AbstractDataFieldMaxValueIncrementerTest {

    protected BasicDataSource dataSource;
    protected int cacheSize;
    /*** 序列个数 */
    protected int seqNum;
    /*** 每个序列上的并发数 */
    protected int seqThreadNum;

    {
        dataSource = new BasicDataSource();
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost/my_test?characterEncoding=utf-8&amp;autoReconnect=true&amp;useUnicode=true&amp;zeroDateTimeBehavior=convertToNull");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        dataSource.setMaxActive(30);
        dataSource.setInitialSize(30);
        try {
            // 触发初始化连接池
            dataSource.getLogWriter();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        cacheSize = 1;
        seqNum = 5;
        seqThreadNum = 10;
    }

    /**
     * 测试多行下的运行效率
     */
    protected abstract void testGetNextLong() throws ExecutionException, InterruptedException;

    protected abstract AbstractDataFieldMaxValueIncrementer buildIncrementer(String seqName);

    protected void writeListToFile(String fileName, List<Long> content) {
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(new File(fileName)));
            int num = 0;
            for (Object item : content) {
                if (num % cacheSize == 0) {
                    // 只记录访问数据库的耗时，访问内存都小于0ms，记录没意义
                    bufferedWriter.write(item.toString() + "\n");
                }
                num++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedWriter != null) {
                try {
                    bufferedWriter.close();
                } catch (IOException ignore) {
                }
            }
        }
    }
}
