package com.kongtianyi.incrementer;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.incrementer.AbstractDataFieldMaxValueIncrementer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 多行MySQL自增发号器
 *
 * 依赖数据表建表SQL:
 * CREATE TABLE `biz_sequence` (
 * `id`  int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID' ,
 * `seq_name`  varchar(10) NOT NULL COMMENT '序列名' ,
 * `sequence`  bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '序列' ,
 * PRIMARY KEY (`id`)
 * )
 * ENGINE=InnoDB
 * DEFAULT CHARACTER SET=utf8 COLLATE=utf8_bin
 * COMMENT='多行自增发号表';
 *
 * @author kongtianyi
 * @date 2019/11/17
 */
public class MultiRowMySQLMaxValueIncrementer extends AbstractDataFieldMaxValueIncrementer {

    private static final String VALUE_SQL = "select last_insert_id()";
    /*** 序列名 */
    private String seqName;
    /**
     * 内存缓存大小
     */
    private int cacheSize = 1;
    /**
     * 下一个序列号
     */
    private long nextId = 0;
    /**
     * 当前缓存中序列号最大值
     */
    private long maxId = 0;
    /**
     * 发号器是否使用新连接
     */
    private boolean useNewConnection = true;

    public MultiRowMySQLMaxValueIncrementer() {
    }

    public MultiRowMySQLMaxValueIncrementer(DataSource dataSource, String incrementerName) {
        super(dataSource, incrementerName);
    }

    public void setUseNewConnection(boolean useNewConnection) {
        this.useNewConnection = useNewConnection;
    }

    @Override
    public synchronized long getNextKey() {
        if (this.maxId == this.nextId) {
            Connection con = null;
            Statement stmt = null;
            boolean mustRestoreAutoCommit = false;
            try {
                if (this.useNewConnection) {
                    con = getDataSource().getConnection();
                    if (con.getAutoCommit()) {
                        mustRestoreAutoCommit = true;
                        con.setAutoCommit(false);
                    }
                } else {
                    con = DataSourceUtils.getConnection(getDataSource());
                }
                stmt = con.createStatement();
                if (!this.useNewConnection) {
                    DataSourceUtils.applyTransactionTimeout(stmt, getDataSource());
                }
                try {
                    stmt.executeUpdate("update " + getIncrementerName() +
                            " set sequence = last_insert_id(sequence + " + getCacheSize() +
                            ") where seq_name='" + getSeqName() + "';");
                } catch (SQLException ex) {
                    throw new DataAccessResourceFailureException("Could not increment " + getSeqName() + " for " +
                            getIncrementerName() + " sequence table", ex);
                }
                ResultSet rs = stmt.executeQuery(VALUE_SQL);
                try {
                    if (!rs.next()) {
                        throw new DataAccessResourceFailureException("last_insert_id() failed after executing an update");
                    }
                    this.maxId = rs.getLong(1);
                } finally {
                    JdbcUtils.closeResultSet(rs);
                }
                this.nextId = this.maxId - getCacheSize() + 1;
            } catch (SQLException ex) {
                throw new DataAccessResourceFailureException("Could not obtain last_insert_id()", ex);
            } finally {
                JdbcUtils.closeStatement(stmt);
                if (con != null) {
                    if (this.useNewConnection) {
                        try {
                            con.commit();
                            if (mustRestoreAutoCommit) {
                                con.setAutoCommit(true);
                            }
                        } catch (SQLException ignore) {
                            throw new DataAccessResourceFailureException(
                                    "Unable to commit new sequence value changes for " + getIncrementerName());
                        }
                        JdbcUtils.closeConnection(con);
                    } else {
                        DataSourceUtils.releaseConnection(con, getDataSource());
                    }
                }
            }
        } else {
            this.nextId++;
        }
        return this.nextId;
    }

    public String getSeqName() {
        return seqName;
    }

    public void setSeqName(String seqName) {
        this.seqName = seqName;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

}
