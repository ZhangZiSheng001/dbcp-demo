package cn.zzs.dbcp;

import java.sql.Connection;
import java.sql.Statement;

import javax.transaction.TransactionManager;

import org.apache.commons.dbcp2.managed.BasicManagedDataSource;
import org.junit.Test;

import com.atomikos.icatch.jta.UserTransactionManager;

/**
 * <p>测试使用BasicManagedDataSource获得XA连接并操作数据库</p>
 * @author: zzs
 * @date: 2019年12月7日 上午9:23:55
 */
public class BasicManagedDataSourceTest {

    public BasicManagedDataSource getBasicManagedDataSource(TransactionManager transactionManager, String url, String username, String password) {
        BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource();
        basicManagedDataSource.setTransactionManager(transactionManager);
        basicManagedDataSource.setUrl(url);
        basicManagedDataSource.setUsername(username);
        basicManagedDataSource.setPassword(password);
        basicManagedDataSource.setDefaultAutoCommit(false);
        basicManagedDataSource.setXADataSource("com.mysql.cj.jdbc.MysqlXADataSource");
        return basicManagedDataSource;
    }

    @Test
    public void test01() throws Exception {
        // 获得事务管理器
        TransactionManager transactionManager = new UserTransactionManager();

        // 获取第一个数据库的数据源
        BasicManagedDataSource basicManagedDataSource1 = getBasicManagedDataSource(transactionManager, "jdbc:mysql://localhost:3306/github_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8&useSSL=true", "root", "root");
        // 注意，这一步非常重要
        basicManagedDataSource1.setDefaultCatalog("github_demo");

        // 获取第二个数据库的数据源
        BasicManagedDataSource basicManagedDataSource2 = getBasicManagedDataSource(transactionManager, "jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8&useSSL=true", "zzf", "zzf");
        // 注意，这一步非常重要
        basicManagedDataSource1.setDefaultCatalog("test");

        Connection connection1 = null;
        Statement statement1 = null;
        Connection connection2 = null;
        Statement statement2 = null;
        transactionManager.begin();
        try {
            // 获取连接并进行数据库操作，这里会将会将XAResource注册到当前线程的XA事务对象
            /**
             * XA START xid1;-- 启动一个事务，并使之为active状态
             */
            connection1 = basicManagedDataSource1.getConnection();
            statement1 = connection1.createStatement();
            /**
             * update github_demo.demo_user set deleted = 1 where id = '1'; -- 事务中的语句
             */
            boolean result1 = statement1.execute("update github_demo.demo_user set deleted = 1 where id = '1'");
            System.out.println(result1);

            /**
             * XA START xid2;-- 启动一个事务，并使之为active状态
             */
            connection2 = basicManagedDataSource2.getConnection();
            statement2 = connection2.createStatement();
            /**
             * update test.demo_user set deleted = 1 where id = '1'; -- 事务中的语句
             */
            boolean result2 = statement2.execute("update test.demo_user set deleted = 1 where id = '1'");
            System.out.println(result2);

            /**
             * 当这执行以下语句：
             * XA END xid1; -- 把事务置为idle状态
             * XA PREPARE xid1; -- 把事务置为prepare状态
             * XA END xid2; -- 把事务置为idle状态
             * XA PREPARE xid2; -- 把事务置为prepare状态	
             * XA COMMIT xid1; -- 提交事务
             * XA COMMIT xid2; -- 提交事务
             */
            transactionManager.commit();
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            statement1.close();
            statement2.close();
            connection1.close();
            connection2.close();
        }
    }
}
