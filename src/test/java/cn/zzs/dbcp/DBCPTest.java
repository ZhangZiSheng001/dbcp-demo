package cn.zzs.dbcp;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Test;

/**
 * @ClassName: DBCPTest
 * @Description: 测试DBCP
 * @author: zzs
 * @date: 2019年8月31日 下午9:39:54
 */
public class DBCPTest {
	/**
	 * 测试添加用户
	 * @throws SQLException 
	 */
	@Test
	public void saveUser() throws Exception {
		//创建sql
		String sql = "insert into user values(null,?,?,?,?)";
		//获得连接
		Connection connection = JDBCUtil.getConnection();
		PreparedStatement statement = null;
		try {
			//设置非自动提交
			connection.setAutoCommit(false);
			//获得Statement对象
			statement = connection.prepareStatement(sql);
			//设置参数
			statement.setString(1, "zzs001");
			statement.setInt(2, 18);
			statement.setDate(3, new Date(System.currentTimeMillis()));
			statement.setDate(4, new Date(System.currentTimeMillis()));
			//执行
			statement.executeUpdate();
			//提交事务
			connection.commit();
		} catch (Exception e) {
			System.out.println("异常导致操作回滚");
			connection.rollback();
			e.printStackTrace();
		} finally {
			//释放资源
			JDBCUtil.release(connection, statement,null);
		}
	}

	/**
	 * 测试更新用户
	 */
	@Test
	public void updateUser() throws Exception {
		//创建sql
		String sql = "update user set age = ?,gmt_modified = ? where name = ?";
		//获得连接
		Connection connection = JDBCUtil.getConnection();
		PreparedStatement statement = null;
		try {
			//设置非自动提交
			connection.setAutoCommit(false);
			//获得Statement对象
			statement = connection.prepareStatement(sql);
			//设置参数
			statement.setInt(1, 19);
			statement.setDate(2, new Date(System.currentTimeMillis()));
			statement.setString(3, "zzs001");
			//执行
			statement.executeUpdate();
			//提交事务
			connection.commit();
		} catch (Exception e) {
			System.out.println("异常导致操作回滚");
			connection.rollback();
			e.printStackTrace();
		} finally {
			//释放资源
			JDBCUtil.release(connection, statement,null);
		}
	}

	/**
	 * 测试查找用户
	 */
	@Test
	public void findUser() throws Exception {
		//创建sql
		String sql = "select * from user where name = ?";
		//获得连接
		Connection connection = JDBCUtil.getConnection();
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
			//获得Statement对象
			statement = connection.prepareStatement(sql);
			//设置参数
			statement.setString(1, "zzs001");
			//执行
			resultSet = statement.executeQuery();
			//遍历结果集
			while (resultSet.next()) {
				String name = resultSet.getString(2);
				int age = resultSet.getInt(3);
				System.out.println("用户名：" + name + ",年龄：" + age);
			}
		} finally {
			//释放资源
			JDBCUtil.release(connection, statement,resultSet);
		}
	}

	/**
	 * 测试删除用户
	 */
	@Test
	public void deleteUser() throws Exception {
		//创建sql
		String sql = "delete from user where name = ?";
		//获得连接
		Connection connection = JDBCUtil.getConnection();
		PreparedStatement statement = null;
		try {
			//设置非自动提交
			connection.setAutoCommit(false);
			//获得Statement对象
			statement = connection.prepareStatement(sql);
			//设置参数
			statement.setString(1, "zzs001");
			//执行
			statement.executeUpdate();
			//提交事务
			connection.commit();
		} catch (Exception e) {
			System.out.println("异常导致操作回滚");
			connection.rollback();
			e.printStackTrace();
		} finally {
			//释放资源
			JDBCUtil.release(connection, statement,null);
		}
	}
}
