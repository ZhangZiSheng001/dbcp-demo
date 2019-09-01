
# DBCP

## 简介  
DBCP用于创建和管理连接，利用连接池的方式复用连接减少了资源开销。 
 
连接池的参数可以采用`properties`文件来配置：配置包括驱动、链接、账号密码，连接池基本参数，事务相关参数，连接测试的参数以及内存回收参数等。  

DBCP对外交互主要是一个`BasicDataDource`，用于设置连接池参数和获取连接对象，作用有点类似于JDK的`DriverManager`。通过源码可以看到，`BasicDataSource`内部有一个`dataSource` 和`connectionPool`字段。  

`dataSource`用于从连接池中获取连接。  
`connectionPool`用于创建，存储和管理池中的连接，里面有一个`Map`对象和`LinkedBlockingDeque`对象，分别存储着所有连接和空闲连接，构成所谓的“池”。


## 使用例子
### 需求
使用DBCP连接池获取连接对象，对用户数据进行增删改查。

### 工程环境
JDK：1.8.0_201  
maven：3.6.1  
IDE：Spring Tool Suites4 for Eclipse  
mysql驱动：8.0.15
mysql：5.7 

### 主要步骤
DBCP对外交互主要是一个`BasicDataDource`，用于设置连接池参数和获取连接对象。
1. 通过`BasicDataSourceFactory.createDataSource(properties)`设置连接池参数，并获得`BasicDataDource`对象；
2. 获取连接对象：调用`BasicDataDource`对象的`getConnection()`方法获取`Connection`对象。

### 创建表
```sql
CREATE DATABASE `demo`CHARACTER SET utf8 COLLATE utf8_bin;
User `demo`;
CREATE TABLE `user` (
  `id` tinyint(3) unsigned NOT NULL AUTO_INCREMENT COMMENT '用户id',
  `name` varchar(32) COLLATE utf8_bin NOT NULL COMMENT '用户名',
  `age` int(10) unsigned DEFAULT NULL COMMENT '用户年龄',
  `gmt_create` datetime DEFAULT NULL COMMENT '记录创建时间',
  `gmt_modified` datetime DEFAULT NULL COMMENT '记录最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
```

### 创建项目
项目类型Maven Project，打包方式jar

### 引入依赖
```xml
<!-- junit -->
<dependency>
	<groupId>junit</groupId>
	<artifactId>junit</artifactId>
	<version>4.12</version>
	<scope>test</scope>
</dependency>
<!-- dbcp -->
<dependency>
	<groupId>org.apache.commons</groupId>
	<artifactId>commons-dbcp2</artifactId>
	<version>2.6.0</version>
</dependency>
<!-- log4j -->
<dependency>
	<groupId>log4j</groupId>
	<artifactId>log4j</artifactId>
	<version>1.2.17</version>
</dependency>
<!-- mysql驱动的jar包 -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.15</version>
</dependency>
```

### 编写jdbc.prperties
路径：resources目录下
```properties
#数据库配置
driverClassName=com.mysql.cj.jdbc.Driver
url=jdbc:mysql://localhost:3306/demo?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8&useSSL=true
username=root
password=root
```

### 编写JDBCUtil用于获得连接对象
这里设置工具类的目的是避免多个线程使用同一个连接对象，并提供了释放资源的方法（注意，考虑到重用性，这里并不会关闭连接）。  
路径：`cn.zzs.jdbc`
```java
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

```

### 编写测试类
路径：test目录下的`cn.zzs.jdbc`

#### 添加用户
注意：这里引入了事务
```java
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
```
#### 更新用户
```java
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
```
#### 查询用户
```java
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
```
#### 删除用户
```java
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
```

### dbcp配置文件详解
#### 数据库基本配置
```properties
driverClassName=com.mysql.cj.jdbc.Driver
url=jdbc:mysql://localhost:3306/demo?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8&useSSL=true
username=root
password=root
```
#### 连接数据相关参数
```properties
#-------------连接数据相关参数--------------------------------
#初始化连接:连接池启动时创建的初始化连接数量
#默认为0
initialSize=0
#最大活动连接
#连接池在同一时间能够分配的最大活动连接的数量, 如果设置为非正数则表示不限制
#默认为8
maxActive=8
#最大空闲连接
#连接池中容许保持空闲状态的最大连接数量,超过的空闲连接将被释放,如果设置为负数表示不限制
#默认为8
maxIdle=8
#最小空闲连接
#连接池中容许保持空闲状态的最小连接数量,低于这个数量将创建新的连接,如果设置为0则不创建
#默认为0
minIdle=0
#最大等待时间
#当没有可用连接时,连接池等待连接被归还的最大时间(以毫秒计数),超过时间则抛出异常,如果设置为-1表示无限等待
#默认无限
maxWait=-1
```

#### 事务相关的属性
```properties
#-------------事务相关的属性--------------------------------
#连接池创建的连接的默认的auto-commit状态
#默认为true
defaultAutoCommit=false
#连接池创建的连接的默认的read-only状态. 如果没有设置则setReadOnly方法将不会被调用. (某些驱动不支持只读模式,比如:Informix)
#默认值由驱动决定
#defaultReadOnly=false
#连接池创建的连接的默认的TransactionIsolation状态
#可用值为下列之一：NONE,READ_UNCOMMITTED, READ_COMMITTED, REPEATABLE_READ, SERIALIZABLE
#默认值由驱动决定
defaultTransactionIsolation=REPEATABLE_READ
#连接池创建的连接的默认的catalog
#defaultCatalog
```
#### 连接检查情况
```properties
#-------------连接检查情况--------------------------------
#SQL查询,用来验证从连接池取出的连接,在将连接返回给调用者之前.如果指定,则查询必须是一个SQL SELECT并且必须返回至少一行记录
validationQuery= select 1
#指明是否在从池中取出连接前进行检验,如果检验失败,则从池中去除连接并尝试取出另一个.
#注意: 设置为true后如果要生效,validationQuery参数必须设置为非空字符串
#默认为true
testOnBorrow=true
#指明是否在归还到池中前进行检验 
#注意: 设置为true后如果要生效,validationQuery参数必须设置为非空字符串
#默认为false
testOnReturn=false
#是否开启空闲资源监测。
#注意: 设置为true后如果要生效,validationQuery参数必须设置为非空字符串
#默认为false
testWhileIdle= true
#空闲资源的检测周期(单位为毫秒)。默认-1：不检测。建议设置，周期自行选择。timeBetweenEvictionRunsMillis=30000
#做空闲资源检测时，每次的采样数。默认3。
#可根据自身应用连接数进行微调,如果设置为-1，就是对所有连接做空闲监测。
numTestsPerEvictionRun=3
#资源池中资源最小空闲时间(单位为毫秒)，达到此值后空闲资源将被移除。
#默认值1000*60*30 = 30分钟。建议默认，或根据自身业务选择。
minEvictableIdleTimeMillis=1800000
```

#### 缓存语句
```properties
#-------------缓存语句--------------------------------
#开启池的prepared statement 池功能
#注意: 确认连接还有剩余资源可以留给其他statement
#默认为false
poolPreparedStatements=false
#statement池能够同时分配的打开的statements的最大数量, 如果设置为0表示不限制
#默认为0
maxOpenPreparedStatements=0
```

#### 连接泄漏回收参数
```properties
#-------------连接泄漏回收参数--------------------------------
#标记是否删除泄露的连接,如果他们超过了removeAbandonedTimout的限制.
#如果设置为true, 连接被认为是被泄露并且可以被删除,如果空闲时间超过removeAbandonedTimeout. 
#设置为true可以为写法糟糕的没有关闭连接的程序修复数据库连接.
#默认为false
removeAbandoned=false
#泄露的连接可以被删除的超时值, 单位秒
#默认为300
removeAbandonedTimeout=300
#标记当Statement或连接被泄露时是否打印程序的stack traces日志。
#被泄露的Statements和连接的日志添加在每个连接打开或者生成新的Statement,因为需要生成stack trace。
#默认为false
logAbandoned=false
#如果开启"removeAbandoned",那么连接在被认为泄露时可能被池回收. 
#这个机制在(getNumIdle() < 2) and (getNumActive() > getMaxActive() - 3)时被触发.
#举例当maxActive=20, 活动连接为18,空闲连接为1时可以触发"removeAbandoned".
#但是活动连接只有在没有被使用的时间超过"removeAbandonedTimeout"时才被删除,默认300秒.在resultset中游历不被计算为被使用.
```

#### 其他
```properties
#-------------其他--------------------------------
#控制PoolGuard是否容许获取底层连接
#默认为false
accessToUnderlyingConnectionAllowed=false
#如果容许则可以使用下面的方式来获取底层物理连接:
#    Connection conn = ds.getConnection();
#    Connection dconn = ((DelegatingConnection) conn).getInnermostDelegate();
#    ...
#    conn.close();
```

> 学习使我快乐！！
