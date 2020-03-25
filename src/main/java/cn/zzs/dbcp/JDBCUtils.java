package cn.zzs.dbcp;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSourceFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 用于获取数据库连接对象的工具类。这里使用BasicDataSource获取连接对象，获取到的连接对象可满足一般的数据库操作
 * @author: zzs
 * @date: 2019年8月31日 下午9:05:08
 */
public class JDBCUtils {

	private static DataSource dataSource;

	private static ThreadLocal<Connection> tl = new ThreadLocal<>();

	private static final Object obj = new Object();

	private static final Log log = LogFactory.getLog(JDBCUtils.class);

	static {
		init();
	}

	/**
	 * 
	 * <p>获取数据库连接对象的方法，线程安全</p>
	 * @author: zzs
	 * @date: 2019年8月31日 下午9:22:29
	 * @return: Connection
	 */
	public static Connection getConnection() throws SQLException {
		// 从当前线程中获取连接对象
		Connection connection = tl.get();
		// 判断为空的话，创建连接并绑定到当前线程
		if(connection == null) {
			synchronized(obj) {
				if(tl.get() == null) {
					connection = createConnection();
					tl.set(connection);
				}
			}
		}
		return connection;
	}

	/**
	 * 
	 * <p>释放资源</p>
	 * @author: zzs
	 * @date: 2019年8月31日 下午9:39:24
	 * @param conn
	 * @param statement
	 * @return: void
	 */
	public static void release(Connection conn, Statement statement, ResultSet resultSet) {
		if(resultSet != null) {
			try {
				resultSet.close();
			} catch(SQLException e) {
				log.error("关闭ResultSet对象异常", e);
			}
		}
		if(statement != null) {
			try {
				statement.close();
			} catch(SQLException e) {
				log.error("关闭Statement对象异常", e);
			}
		}
		// 注意：这里不关闭连接
		if(conn != null) {
			try {
				conn.close();
				tl.remove();
			} catch(SQLException e) {
				log.error("关闭Connection对象异常", e);
			}
		}
	}

	/**
	 * 
	 * <p>创建数据库连接</p>
	 * @author: zzs
	 * @date: 2019年8月31日 下午9:27:03
	 * @return: Connection
	 * @throws SQLException 
	 */
	private static Connection createConnection() throws SQLException {
		Connection conn = null;
		// 获得连接
		conn = dataSource.getConnection();
		return conn;
	}

	/**
	 * <p>根据指定配置文件创建数据源对象</p>
	 * @author: zzs
	 * @date: 2019年9月1日 上午10:53:05
	 * @return: void
	 * @throws Exception 
	 */
	private static void init() {
		// 导入配置文件
		Properties properties = new Properties();
		InputStream in = JDBCUtils.class.getClassLoader().getResourceAsStream("dbcp.properties");
		try {
			properties.load(in);
			// 根据配置文件内容获得数据源对象
			dataSource = BasicDataSourceFactory.createDataSource(properties);
		} catch(Exception ex) {
			throw new RuntimeException("根据指定配置文件创建数据源出错", ex);
		}
	}
}
