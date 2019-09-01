package cn.zzs.dbcp;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSourceFactory;

/**
 * @ClassName: JDBCUtil
 * @Description: 用于获取数据库连接对象的工具类
 * @author: zzs
 * @date: 2019年8月31日 下午9:05:08
 */
public class JDBCUtil {
	private static DataSource dataSource;
	private static ThreadLocal<Connection> tl = new ThreadLocal<>();
	private static Object obj = new Object();
	
	static {
		init();
	}
	/**
	 * 
	 * @Title: getConnection
	 * @Description: 获取数据库连接对象的方法，线程安全
	 * @author: zzs
	 * @date: 2019年8月31日 下午9:22:29
	 * @return: Connection
	 */
	public static Connection getConnection(){
		//从当前线程中获取连接对象
		Connection connection = tl.get();
		//判断为空的话，创建连接并绑定到当前线程
		if(connection == null) {
			synchronized (obj) {
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
	 * @Title: release
	 * @Description: 释放资源
	 * @author: zzs
	 * @date: 2019年8月31日 下午9:39:24
	 * @param conn
	 * @param statement
	 * @return: void
	 */
	public static void release(Connection conn,Statement statement,ResultSet resultSet) {
		if(resultSet!=null) {
			try {
				resultSet.close();
			} catch (SQLException e) {
				System.err.println("关闭ResultSet对象异常");
				e.printStackTrace();
			}
		}
		if(statement != null) {
			try {
				statement.close();
			} catch (SQLException e) {
				System.err.println("关闭Statement对象异常");
				e.printStackTrace();
			}
		}
		//注意：这里不关闭连接
		if(conn!=null) {
			try {
				//如果连接失效的话，从当前线程的绑定中删除
				if(!conn.isValid(3)) {
					tl.remove();
				}
			} catch (SQLException e) {
				System.err.println("校验连接有效性");
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 
	 * @Title: createConnection
	 * @Description: 创建数据库连接
	 * @author: zzs
	 * @date: 2019年8月31日 下午9:27:03
	 * @return: Connection
	 */
	private static Connection createConnection(){ 
		Connection conn = null;
		//获得连接
		try {
			conn = dataSource.getConnection();
		} catch (SQLException e) {
			System.err.println("从数据源获取连接失败");
			e.printStackTrace();
		}
		return conn;
	}
	
	/**
	 * @Title: init
	 * @Description: 根据指定配置文件创建数据源对象
	 * @author: zzs
	 * @date: 2019年9月1日 上午10:53:05
	 * @return: void
	 */
	private static void init() {
		//导入配置文件
		Properties properties = new Properties();
		InputStream in = JDBCUtil.class.getClassLoader().getResourceAsStream("jdbc.properties");
		try {
			properties.load(in);
			//根据配置文件内容获得数据源对象
			dataSource = BasicDataSourceFactory.createDataSource(properties);
		} catch (IOException e) {
			System.err.println("导入配置文件出错");
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("根据指定配置文件创建数据源出错");
			e.printStackTrace();
		}
	}
}

