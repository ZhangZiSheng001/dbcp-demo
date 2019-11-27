# 简介  
DBCP用于创建和管理连接，利用“池”的方式复用连接对象，减少了资源开销。 
 
连接池的参数可以采用`properties`文件来配置：配置包括驱动、链接、账号密码，连接池基本参数，事务相关参数，连接测试的参数以及内存回收参数等。  

# 使用例子
## 需求
使用DBCP连接池获取连接对象，对用户数据进行增删改查。

## 工程环境
JDK：1.8.0_201  

maven：3.6.1  

IDE：STS4  

mysql-connector-java：8.0.15  

mysql：5.7  

DBCP：2.6.0  


## 主要步骤

1. 编写`jdbc.properties`，设置数据库连接参数和连接池参数。  

2. 通过`BasicDataSourceFactory`加载`jdbc.properties`，并获得`BasicDataDource`对象。  

3. 通过`BasicDataDource`对象获取`Connection`对象。  

4. 使用`Connection`对象对用户表进行增删改查。  

## 创建项目
项目类型Maven Project，打包方式jar。  

## 引入依赖
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

## 编写jdbc.prperties
路径resources目录下，考虑篇幅，这里仅给出数据库连接参数和连接池参数，具体参见项目源码。另外，数据库sql脚本也在该目录下。  

```properties
#数据库基本配置
driverClassName=com.mysql.cj.jdbc.Driver
url=jdbc:mysql://localhost:3306/github_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8&useSSL=true
username=root
password=root

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

## 获取连接池和获取连接  
项目中编写了JDBCUtil来初始化连接池、获取连接、管理事务和释放资源等，具体参见项目源码。  

路径：`cn.zzs.dbcp`
```java
	// 导入配置文件
	Properties properties = new Properties();
	InputStream in = JDBCUtil.class.getClassLoader().getResourceAsStream("jdbc.properties");
	properties.load(in);
	// 根据配置文件内容获得数据源对象
	DataSource dataSource = BasicDataSourceFactory.createDataSource(properties);
	// 获得连接
	Connection conn = dataSource.getConnection();
```

## 编写测试类
这里以保存用户为例，路径test目录下的`cn.zzs.dbcp`。

```java
	@Test
	public void save() {
		// 创建sql
		String sql = "insert into demo_user values(null,?,?,?,?,?)";
		Connection connection = null;
		PreparedStatement statement = null;
		try {
			// 获得连接
			connection = JDBCUtil.getConnection();
			// 开启事务设置非自动提交
			JDBCUtil.startTrasaction();
			// 获得Statement对象
			statement = connection.prepareStatement(sql);
			// 设置参数
			statement.setString(1, "zzf003");
			statement.setInt(2, 18);
			statement.setDate(3, new Date(System.currentTimeMillis()));
			statement.setDate(4, new Date(System.currentTimeMillis()));
			statement.setBoolean(5, false);
			// 执行
			statement.executeUpdate();
			// 提交事务
			JDBCUtil.commit();
		} catch(Exception e) {
			JDBCUtil.rollback();
			log.error("保存用户失败", e);
		} finally {
			// 释放资源
			JDBCUtil.release(connection, statement, null);
		}
	}
```

# 配置文件详解
这部分内容从网上参照过来，因为发的到处都是，所以暂时没找到出处。因为最新版本更新了不少内容，所以我修正了下，后面找到出处再补上参考资料。  

## 数据库基本配置
注意，这里在url后面拼接了多个参数用于避免乱码、时区报错问题。  

```properties
driverClassName=com.mysql.cj.jdbc.Driver
url=jdbc:mysql://localhost:3306/github_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8&useSSL=true
username=root
password=root
```

## 连接池数据相关参数
这几个参数都比较常用，具体设置多少需根据具体项目调整。  

```properties
#-------------连接数据相关参数--------------------------------
#初始化连接数量:连接池启动时创建的初始化连接数量
#默认为0
initialSize=0

#最大活动连接数量:连接池在同一时间能够分配的最大活动连接的数量, 如果设置为负数则表示不限制
#默认为8
maxTotal=8

#最大空闲连接:连接池中容许保持空闲状态的最大连接数量,超过的空闲连接将被释放,如果设置为负数表示不限制
#默认为8
maxIdle=8

#最小空闲连接:连接池中容许保持空闲状态的最小连接数量,低于这个数量将创建新的连接,如果设置为0则不创建
#注意：需要开启空闲对象回收器，这个参数才能生效。
#默认为0
minIdle=0

#最大等待时间
#当没有可用连接时,连接池等待连接被归还的最大时间(以毫秒计数),超过时间则抛出异常,如果设置为<=0表示无限等待
#默认-1
maxWaitMillis=-1
```

## 连接检查情况

```properties
#-------------连接检查情况--------------------------------
#通过SQL查询检测连接,注意必须返回至少一行记录
#默认为空。即会调用Connection的isValid和isClosed进行检测
#注意：如果是oracle数据库的话，应该改为select 1 from dual
validationQuery=select 1 from dual

#SQL检验超时时间
validationQueryTimeout=-1

#是否从池中取出连接前进行检验。
#默认为true
testOnBorrow=true

#是否在归还到池中前进行检验 
#默认为false
testOnReturn=false

#是否开启空闲对象回收器。
#默认为false
testWhileIdle=false

#空闲对象回收器的检测周期(单位为毫秒)。
#默认-1。即空闲对象回收器不工作。
timeBetweenEvictionRunsMillis=-1

#做空闲对象回收器时，每次的采样数。
#默认3，单位毫秒。如果设置为-1，就是对所有连接做空闲监测。
numTestsPerEvictionRun=3

#资源池中资源最小空闲时间(单位为毫秒)，达到此值后将被移除。
#默认值1000*60*30 = 30分钟
minEvictableIdleTimeMillis=1800000

#资源池中资源最小空闲时间(单位为毫秒)，达到此值后将被移除。但是会保证minIdle
#默认值-1
#softMinEvictableIdleTimeMillis=-1

#空闲对象回收器的回收策略
#默认org.apache.commons.pool2.impl.DefaultEvictionPolicy
#如果要自定义的话，需要实现EvictionPolicy重写evict方法
evictionPolicyClassName=org.apache.commons.pool2.impl.DefaultEvictionPolicy

#连接最大存活时间。非正数表示不限制
#默认-1
maxConnLifetimeMillis=-1

#当达到maxConnLifetimeMillis被关闭时，是否打印相关消息
#默认true
#注意：maxConnLifetimeMillis设置为正数时，这个参数才有效
logExpiredConnections=true
```

## 缓存语句
```properties
#-------------缓存语句--------------------------------
#是否缓存PreparedStatements，这个功能在一些支持游标的数据库中可以极大提高性能（Oracle、SQL Server、DB2、Sybase）
#默认为false
poolPreparedStatements=false

#缓存PreparedStatements的最大个数
#默认为-1
#注意：poolPreparedStatements为true时，这个参数才有效
maxOpenPreparedStatements=-1

#缓存read-only和auto-commit状态。设置为true的话，所有连接的状态都会是一样的。
#默认是true
cacheState=true

```

## 事务相关的属性

```properties
#-------------事务相关的属性--------------------------------
#连接池创建的连接的默认的auto-commit状态
#默认为空，由驱动决定
defaultAutoCommit=true

#连接池创建的连接的默认的read-only状态。
#默认值为空，由驱动决定
defaultReadOnly=false

#连接池创建的连接的默认的TransactionIsolation状态
#可用值为下列之一：NONE,READ_UNCOMMITTED, READ_COMMITTED, REPEATABLE_READ, SERIALIZABLE
#默认值为空，由驱动决定
defaultTransactionIsolation=REPEATABLE_READ

#归还连接时是否设置自动提交为true
#默认true
autoCommitOnReturn=true

#归还连接时是否设置回滚事务
#默认true
rollbackOnReturn=true

#连接池创建的连接的默认的数据库名
#defaultCatalog=github_demo

#连接池创建的连接的默认的schema。如果是mysql，这个设置没什么用。
#defaultSchema=github_demo
```

## 连接泄漏回收参数
```properties
#-------------连接泄漏回收参数--------------------------------
#当未使用的时间超过removeAbandonedTimeout时，是否视该连接为泄露连接并删除（当getConnection()被调用时检测）
#默认为false
#注意：这个机制在(getNumIdle() < 2) and (getNumActive() > (getMaxActive() - 3))时被触发
removeAbandonedOnBorrow=false

#当未使用的时间超过removeAbandonedTimeout时，是否视该连接为泄露连接并删除
#默认为false
#注意：当空闲对象回收器开启才生效
removeAbandonedOnMaintenance=false

#泄露的连接可以被删除的超时值, 单位秒
#默认为300
removeAbandonedTimeout=300

#标记当Statement或连接被泄露时是否打印程序的stack traces日志。
#默认为false
logAbandoned=true

#这个不是很懂
#默认为false
abandonedUsageTracking=false

```

## 其他
```properties
#-------------其他--------------------------------
#是否使用快速失败机制
#默认为空，由驱动决定
fastFailValidation=false

#当使用快速失败机制时，设置触发的异常码
#多个code用","隔开
#disconnectionSqlCodes

#borrow连接的顺序
#默认true
lifo=true

#每个连接创建时执行的语句
#connectionInitSqls=

#连接参数：例如username、password、characterEncoding等都可以在这里设置
#多个参数用";"隔开
#connectionProperties=

#指定数据源的jmx名
#jmxName=

#查询超时时间
#默认为空，即根据驱动设置
#defaultQueryTimeout=

#控制PoolGuard是否容许获取底层连接
#默认为false
accessToUnderlyingConnectionAllowed=false

#如果容许则可以使用下面的方式来获取底层物理连接:
#    Connection conn = ds.getConnection();
#    Connection dconn = ((DelegatingConnection) conn).getInnermostDelegate();
#    ...
#    conn.close();
```

# 源码分析
通过使用例子可知，DBCP的`BasicDataSource`是我们获取连接对象的入口，至于`BasicDataSourceFactory`只是创建和初始化`BasicDataSource`实例，就不看了。这里直接从`BasicDataSource`的`getConnection()`方法开始分析。  

注意：考虑篇幅和可读性，以下代码经过删减，仅保留所需部分。  

## 数据源创建
研究数据源创建之前，先来看下DBCP的几种数据源：  

类名|描述
-|-
BasicDataSource|用于满足基本数据库操作需求的数据源
BasicManagedDataSource|BasicDataSource的子类，用于创建支持XA事务或JTA事务的连接
PoolingDataSource|BasicDataSource中实际调用的数据源，可以说BasicDataSource只是封装了PoolingDataSource
ManagedDataSource|PoolingDataSource的子类，用于支持XA事务或JTA事务的连接。是BasicManagedDataSource中实际调用的数据源，可以说BasicManagedDataSource只是封装了ManagedDataSource
InstanceKeyDataSource|用于支持JDNI环境的数据源
PerUserPoolDataSource|InstanceKeyDataSource的子类，针对每个用户会单独分配一个连接池，每个连接池可以设置不同属性。例如以下需求，相比user，admin可以创建更多地连接以保证
SharedPoolDataSource|InstanceKeyDataSource的子类，不同用户共享一个连接池

本文的源码分析仅会涉及到BasicDataSource（包含它封装的PoolingDataSource），其他的数据源暂时不扩展。  

### BasicDataSource.getConnection()
```java
    public Connection getConnection() throws SQLException {
        return createDataSource().getConnection();
    }
```

### BasicDataSource.createDataSource()
这里涉及到四个类，如下：  

类名 | 描述
-|-
ConnectionFactory | 用于生成原生的Connection对象
PoolableConnectionFactory | 用于生成包装过的Connection对象，持有ConnectionFactory对象的引用
GenericObjectPool | 数据库连接池，用于管理连接。持有PoolableConnectionFactory对象的引用
PoolingDataSource | 数据源，持有GenericObjectPool的引用。我们调用BasicDataSource获取连接对象，实际上调用的是它的getConnection()方法

```java
    // 数据源
    private volatile DataSource dataSource;
    // 连接池
    private volatile GenericObjectPool<PoolableConnection> connectionPool;

    protected DataSource createDataSource() throws SQLException {
        if (closed) {
            throw new SQLException("Data source is closed");
        }
        if (dataSource != null) {
            return dataSource;
        }

        synchronized (this) {
            if (dataSource != null) {
                return dataSource;
            }
            // 注册MBean，用于支持JMX，这方面的内容不在这里扩展
            jmxRegister();

            // 创建原生Connection工厂：本质就是持有数据库驱动对象和几个连接参数
            final ConnectionFactory driverConnectionFactory = createConnectionFactory();

            // 将driverConnectionFactory包装成池化Connection工厂
            boolean success = false;
            PoolableConnectionFactory poolableConnectionFactory;
            try {
                poolableConnectionFactory = createPoolableConnectionFactory(driverConnectionFactory);
                // 设置PreparedStatements缓存（其实上面创建工厂就设置了，这里没必要再设置一遍）
                poolableConnectionFactory.setPoolStatements(poolPreparedStatements);
                poolableConnectionFactory.setMaxOpenPreparedStatements(maxOpenPreparedStatements);
                success = true;
            } catch (final SQLException se) {
                throw se;
            } catch (final RuntimeException rte) {
                throw rte;
            } catch (final Exception ex) {
                throw new SQLException("Error creating connection factory", ex);
            }

            if (success) {
                // 创建数据库连接池对象GenericObjectPool，用于管理连接
                // BasicDataSource将持有GenericObjectPool对象
                createConnectionPool(poolableConnectionFactory);
            }
            
            // 创建PoolingDataSource对象
            //该对象持有GenericObjectPool对象的引用
            DataSource newDataSource;
            success = false;
            try {
                newDataSource = createDataSourceInstance();
                newDataSource.setLogWriter(logWriter);
                success = true;
            } catch (final SQLException se) {
                throw se;
            } catch (final RuntimeException rte) {
                throw rte;
            } catch (final Exception ex) {
                throw new SQLException("Error creating datasource", ex);
            } finally {
                if (!success) {
                    closeConnectionPool();
                }
            }

            // 根据我们设置的initialSize创建初始连接
            try {
                for (int i = 0; i < initialSize; i++) {
                    connectionPool.addObject();
                }
            } catch (final Exception e) {
                closeConnectionPool();
                throw new SQLException("Error preloading the connection pool", e);
            }

            // 开启连接池的evictor线程
            startPoolMaintenance();
            // 最后BasicDataSource将持有上面创建的PoolingDataSource对象
            dataSource = newDataSource;
            return dataSource;
        }
    }
```

## 获取连接对象
在介绍下面内容前先了解下DBCP中几个Connection实现类。  

类名|描述
-|-
DelegatingConnection|Connection实现类，是以下几个类的父类
PoolingConnection|用于包装原生的Connection，支持prepareStatement和prepareCall
PoolableConnection|用于包装原生的PoolingConnection(如果没有开启poolPreparedStatements，则包装的只是原生Connection)，调用close()时只是将连接还给连接池
PoolableManagedConnection|PoolableConnection的子类，用于包装ManagedConnection，支持JTA和XA事务
ManagedConnection|用于包装原生的Connection，支持JTA和XA事务
PoolGuardConnectionWrapper|用于包装PoolableConnection，当accessToUnderlyingConnectionAllowed才能获取底层连接对象。我们获取到的就是这个对象



### PoolingDataSource.getConnection()
```java
    public Connection getConnection() throws SQLException {
        // 这个泛型C指的是PoolableConnection对象
        // 调用的是GenericObjectPool的方法返回PoolableConnection对象，这个方法后面会展开
        final C conn = pool.borrowObject();
        if (conn == null) {
            return null;
        }
        // 包装PoolableConnection对象，当accessToUnderlyingConnectionAllowed为true时，可以使用底层连接
        return new PoolGuardConnectionWrapper<>(conn);
    }
```

### GenericObjectPool.borrowObject()
```java
    // 存放着连接池所有的连接对象（但不包含已经释放的）
    private final Map<IdentityWrapper<T>, PooledObject<T>> allObjects =
        new ConcurrentHashMap<>();
    // 存放着空闲连接对象的阻塞队列
    private final LinkedBlockingDeque<PooledObject<T>> idleObjects;
    // 为1表示当前正在创建新连接对象
    private long makeObjectCount = 0;
    // 创建连接对象时所用的锁
    private final Object makeObjectCountLock = new Object();
    // 连接对象创建总数量
    private final AtomicLong createCount = new AtomicLong(0);
    // 连接对象借出总数量
    private final AtomicLong borrowedCount = new AtomicLong(0);
    // 连接对象归还总数量
    private final AtomicLong returnedCount = new AtomicLong(0);
    // 连接对象销毁总数量
    final AtomicLong destroyedCount = new AtomicLong(0);
    final AtomicLong destroyedByEvictorCount = new AtomicLong(0);
    // 三个计时相关对象
    private final StatsStore activeTimes = new StatsStore(MEAN_TIMING_STATS_CACHE_SIZE);
    private final StatsStore idleTimes = new StatsStore(MEAN_TIMING_STATS_CACHE_SIZE);
    private final StatsStore waitTimes = new StatsStore(MEAN_TIMING_STATS_CACHE_SIZE);

    public T borrowObject() throws Exception {
        return borrowObject(getMaxWaitMillis());
    }

    public T borrowObject(final long borrowMaxWaitMillis) throws Exception {
        // 校验连接池是否打开状态
        assertOpen();
        
        // 如果设置了removeAbandonedOnBorrow，达到触发条件是会遍历所有连接，未使用时长超过removeAbandonedTimeout的将被释放掉
        final AbandonedConfig ac = this.abandonedConfig;
        if (ac != null && ac.getRemoveAbandonedOnBorrow() &&
                (getNumIdle() < 2) &&
                (getNumActive() > getMaxTotal() - 3) ) {
            removeAbandoned(ac);
        }
        
        
        PooledObject<T> p = null;
        // 连接数达到maxTotal是否阻塞等待
        final boolean blockWhenExhausted = getBlockWhenExhausted();

        boolean create;
        final long waitTime = System.currentTimeMillis();
        
        // 如果获取的连接对象为空，会再次进入获取
        while (p == null) {
            create = false;
            // 获取空闲队列的第一个元素，如果为空就试图创建新连接
            p = idleObjects.pollFirst();
            if (p == null) {
                // 后面分析这个方法
                p = create();
                if (p != null) {
                    create = true;
                }
            }
            // 连接数达到maxTotal需要阻塞等待，会等待空闲队列中连接
            if (blockWhenExhausted) {
                if (p == null) {
                    if (borrowMaxWaitMillis < 0) {
                        // 无限等待
                        p = idleObjects.takeFirst();
                    } else {
                        // 等待maxWaitMillis
                        p = idleObjects.pollFirst(borrowMaxWaitMillis,
                                TimeUnit.MILLISECONDS);
                    }
                }
                // 这个时候还是没有就会抛出异常
                if (p == null) {
                    throw new NoSuchElementException(
                            "Timeout waiting for idle object");
                }
            } else {
                if (p == null) {
                    throw new NoSuchElementException("Pool exhausted");
                }
            }
            // 如果连接处于空闲状态，会修改连接的state、lastBorrowTime、lastUseTime、borrowedCount等，并返回true
            if (!p.allocate()) {
                p = null;
            }

            if (p != null) {
                // 利用工厂重新初始化连接对象，这里会去校验连接存活时间、设置lastUsedTime、及其他初始参数
                try {
                    factory.activateObject(p);
                } catch (final Exception e) {
                    try {
                        destroy(p);
                    } catch (final Exception e1) {
                        // Ignore - activation failure is more important
                    }
                    p = null;
                    if (create) {
                        final NoSuchElementException nsee = new NoSuchElementException(
                                "Unable to activate object");
                        nsee.initCause(e);
                        throw nsee;
                    }
                }
                // 根据设置的参数，判断是否检测连接有效性
                if (p != null && (getTestOnBorrow() || create && getTestOnCreate())) {
                    boolean validate = false;
                    Throwable validationThrowable = null;
                    try {
                        // 这里会去校验连接的存活时间是否超过maxConnLifetimeMillis，以及通过SQL去校验执行时间
                        validate = factory.validateObject(p);
                    } catch (final Throwable t) {
                        PoolUtils.checkRethrow(t);
                        validationThrowable = t;
                    }
                    // 如果校验不通过，会释放该对象
                    if (!validate) {
                        try {
                            destroy(p);
                            destroyedByBorrowValidationCount.incrementAndGet();
                        } catch (final Exception e) {
                            // Ignore - validation failure is more important
                        }
                        p = null;
                        if (create) {
                            final NoSuchElementException nsee = new NoSuchElementException(
                                    "Unable to validate object");
                            nsee.initCause(validationThrowable);
                            throw nsee;
                        }
                    }
                }
            }
        }
        // 更新borrowedCount、idleTimes和waitTimes
        updateStatsBorrow(p, System.currentTimeMillis() - waitTime);

        return p.getObject();
    }
```
### GenericObjectPool.create()
```java
    private PooledObject<T> create() throws Exception {
        int localMaxTotal = getMaxTotal();
        if (localMaxTotal < 0) {
            localMaxTotal = Integer.MAX_VALUE;
        }

        final long localStartTimeMillis = System.currentTimeMillis();
        final long localMaxWaitTimeMillis = Math.max(getMaxWaitMillis(), 0);

        // 创建标识：
        // - TRUE:  调用工厂创建返回对象
        // - FALSE: 直接返回null
        // - null:  继续循环
        Boolean create = null;
        while (create == null) {
            synchronized (makeObjectCountLock) {
                final long newCreateCount = createCount.incrementAndGet();
                if (newCreateCount > localMaxTotal) {
                    // 当前池已经达到maxTotal，或者有另外一个线程正在试图创建一个新的连接使之达到容量极限
                    createCount.decrementAndGet();
                    if (makeObjectCount == 0) {
                        // 连接池确实已达到容量极限
                        create = Boolean.FALSE;
                    } else {
                        // 当前另外一个线程正在试图创建一个新的连接使之达到容量极限，此时需要等待
                        makeObjectCountLock.wait(localMaxWaitTimeMillis);
                    }
                } else {
                    // 当前连接池容量未到达极限，可以继续创建连接对象
                    makeObjectCount++;
                    create = Boolean.TRUE;
                }
            }

            // 当达到maxWaitTimeMillis时不创建连接对象，直接退出循环
            if (create == null &&
                (localMaxWaitTimeMillis > 0 &&
                 System.currentTimeMillis() - localStartTimeMillis >= localMaxWaitTimeMillis)) {
                create = Boolean.FALSE;
            }
        }

        if (!create.booleanValue()) {
            return null;
        }

        final PooledObject<T> p;
        try {
            // 调用工厂创建对象，后面对这个方法展开分析
            p = factory.makeObject();
        } catch (final Throwable e) {
            createCount.decrementAndGet();
            throw e;
        } finally {
            synchronized (makeObjectCountLock) {
                // 创建标识-1
                makeObjectCount--;
                // 唤醒makeObjectCountLock锁住的对象
                makeObjectCountLock.notifyAll();
            }
        }

        final AbandonedConfig ac = this.abandonedConfig;
        if (ac != null && ac.getLogAbandoned()) {
            p.setLogAbandoned(true);
            // TODO: in 3.0, this can use the method defined on PooledObject
            if (p instanceof DefaultPooledObject<?>) {
                ((DefaultPooledObject<T>) p).setRequireFullStackTrace(ac.getRequireFullStackTrace());
            }
        }
        // 连接数量+1
        createdCount.incrementAndGet();
        // 将创建的对象放入allObjects
        allObjects.put(new IdentityWrapper<>(p.getObject()), p);
        return p;
    }

```

### PoolableConnectionFactory.makeObject()
```java
    public PooledObject<PoolableConnection> makeObject() throws Exception {
        // 创建原生的Connection对象
        Connection conn = connectionFactory.createConnection();
        if (conn == null) {
            throw new IllegalStateException("Connection factory returned null from createConnection");
        }
        try {
            // 执行我们设置的connectionInitSqls
            initializeConnection(conn);
        } catch (final SQLException sqle) {
            // Make sure the connection is closed
            try {
                conn.close();
            } catch (final SQLException ignore) {
                // ignore
            }
            // Rethrow original exception so it is visible to caller
            throw sqle;
        }
        // 连接索引+1
        final long connIndex = connectionIndex.getAndIncrement();
        
        // 如果设置了poolPreparedStatements，则创建包装连接为PoolingConnection对象
        if (poolStatements) {
            conn = new PoolingConnection(conn);
            final GenericKeyedObjectPoolConfig<DelegatingPreparedStatement> config = new GenericKeyedObjectPoolConfig<>();
            config.setMaxTotalPerKey(-1);
            config.setBlockWhenExhausted(false);
            config.setMaxWaitMillis(0);
            config.setMaxIdlePerKey(1);
            config.setMaxTotal(maxOpenPreparedStatements);
            if (dataSourceJmxObjectName != null) {
                final StringBuilder base = new StringBuilder(dataSourceJmxObjectName.toString());
                base.append(Constants.JMX_CONNECTION_BASE_EXT);
                base.append(Long.toString(connIndex));
                config.setJmxNameBase(base.toString());
                config.setJmxNamePrefix(Constants.JMX_STATEMENT_POOL_PREFIX);
            } else {
                config.setJmxEnabled(false);
            }
            final PoolingConnection poolingConn = (PoolingConnection) conn;
            final KeyedObjectPool<PStmtKey, DelegatingPreparedStatement> stmtPool = new GenericKeyedObjectPool<>(
                    poolingConn, config);
            poolingConn.setStatementPool(stmtPool);
            poolingConn.setCacheState(cacheState);
        }

        // 用于注册连接到JMX
        ObjectName connJmxName;
        if (dataSourceJmxObjectName == null) {
            connJmxName = null;
        } else {
            connJmxName = new ObjectName(
                    dataSourceJmxObjectName.toString() + Constants.JMX_CONNECTION_BASE_EXT + connIndex);
        }
        
        // 创建PoolableConnection对象
        final PoolableConnection pc = new PoolableConnection(conn, pool, connJmxName, disconnectionSqlCodes,
                fastFailValidation);
        pc.setCacheState(cacheState);
        
        // 包装成连接池所需的对象
        return new DefaultPooledObject<>(pc);
    }
```

## 空闲对象回收器Evictor
前面已经讲到当创建完数据源对象时会开启连接池的evictor线程。  

### BasicDataSource.startPoolMaintenance()
```java
    protected void startPoolMaintenance() {
        // 只有timeBetweenEvictionRunsMillis为正数，才会开启空闲对象回收器
        if (connectionPool != null && timeBetweenEvictionRunsMillis > 0) {
            connectionPool.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
        }
    }
```
### BaseGenericObjectPool.setTimeBetweenEvictionRunsMillis(long)
这个BaseGenericObjectPool是上面说到的GenericObjectPool的父类。  

```java
    public final void setTimeBetweenEvictionRunsMillis(
            final long timeBetweenEvictionRunsMillis) {
        // 设置回收线程运行间隔时间
        this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
        // 
        startEvictor(timeBetweenEvictionRunsMillis);
    }
```

### BaseGenericObjectPool.startEvictor(long)
```java
    final void startEvictor(final long delay) {
        synchronized (evictionLock) {
            if (null != evictor) {
                EvictionTimer.cancel(evictor, evictorShutdownTimeoutMillis, TimeUnit.MILLISECONDS);
                evictor = null;
                evictionIterator = null;
            }
            // 创建回收器任务，并执行定时调度
            if (delay > 0) {
                evictor = new Evictor();
                EvictionTimer.schedule(evictor, delay, delay);
            }
        }
    }
```

### EvictionTimer.schedule(Evictor, long, long)
DBCP是使用ScheduledThreadPoolExecutor来实现回收器的定时检测。  

```java
    static synchronized void schedule(
            final BaseGenericObjectPool<?>.Evictor task, final long delay, final long period) 
        if (null == executor) {
            // 创建线程池，队列为DelayedWorkQueue，corePoolSize为1，maximumPoolSize为无限大
            executor = new ScheduledThreadPoolExecutor(1, new EvictorThreadFactory());
            // 当任务被取消的同时从等待队列中移除
            executor.setRemoveOnCancelPolicy(true);
        }
        // 设置任务定时调度
        final ScheduledFuture<?> scheduledFuture =
                executor.scheduleWithFixedDelay(task, delay, period, TimeUnit.MILLISECONDS);
        task.setScheduledFuture(scheduledFuture);
    }
```
### BaseGenericObjectPool.Evictor
Evictor是BaseGenericObjectPool的内部类，这里看下它的run方法。  

```java
    class Evictor implements Runnable {

        private ScheduledFuture<?> scheduledFuture;

        @Override
        public void run() {
            final ClassLoader savedClassLoader =
                    Thread.currentThread().getContextClassLoader();
            try {
                // 确保回收器使用的类加载器和工厂对象的一样
                if (factoryClassLoader != null) {
                    final ClassLoader cl = factoryClassLoader.get();
                    if (cl == null) {
                        cancel();
                        return;
                    }
                    Thread.currentThread().setContextClassLoader(cl);
                }

                // 回收符合条件的对象
                try {
                    evict();
                } catch(final Exception e) {
                    swallowException(e);
                } catch(final OutOfMemoryError oome) {
                    // Log problem but give evictor thread a chance to continue
                    // in case error is recoverable
                    oome.printStackTrace(System.err);
                }
                try {
                    // 确保最小空闲对象
                    ensureMinIdle();
                } catch (final Exception e) {
                    swallowException(e);
                }
            } finally {
                Thread.currentThread().setContextClassLoader(savedClassLoader);
            }
        }


        void setScheduledFuture(final ScheduledFuture<?> scheduledFuture) {
            this.scheduledFuture = scheduledFuture;
        }


        void cancel() {
            scheduledFuture.cancel(false);
        }
    }
```

### GenericObjectPool.evict()
这里的回收过程包括以下四道校验：  

1. 按照evictionPolicy校验idleSoftEvictTime、idleEvictTime；  

2. 利用工厂重新初始化样本，这里会校验maxConnLifetimeMillis（testWhileIdle为true）；  

3. 校验maxConnLifetimeMillis和validationQueryTimeout（testWhileIdle为true）；  

4. 校验所有连接的未使用时间是否超过removeAbandonedTimeout（removeAbandonedOnMaintenance为true）。  

```java
    public void evict() throws Exception {
        // 校验当前连接池是否关闭
        assertOpen();

        if (idleObjects.size() > 0) {

            PooledObject<T> underTest = null;
            // 介绍参数时已经讲到，这个evictionPolicy我们可以自定义
            final EvictionPolicy<T> evictionPolicy = getEvictionPolicy();

            synchronized (evictionLock) {
                final EvictionConfig evictionConfig = new EvictionConfig(
                        getMinEvictableIdleTimeMillis(),
                        getSoftMinEvictableIdleTimeMillis(),
                        getMinIdle());

                final boolean testWhileIdle = getTestWhileIdle();
                // 获取我们指定的样本数，并开始遍历
                for (int i = 0, m = getNumTests(); i < m; i++) {
                    if (evictionIterator == null || !evictionIterator.hasNext()) {
                        evictionIterator = new EvictionIterator(idleObjects);
                    }
                    if (!evictionIterator.hasNext()) {
                        // Pool exhausted, nothing to do here
                        return;
                    }

                    try {
                        underTest = evictionIterator.next();
                    } catch (final NoSuchElementException nsee) {
                        // 当前样本正被另一个线程借出
                        i--;
                        evictionIterator = null;
                        continue;
                    }
                    // 判断如果样本是空闲状态，设置为EVICTION状态
                    // 如果不是，说明另一个线程已经借出了这个样本
                    if (!underTest.startEvictionTest()) {
                        i--;
                        continue;
                    }

                    boolean evict;
                    try {
                        // 调用回收策略来判断是否回收该样本，按照默认策略，以下情况都会返回true：
                        // 1. 样本空闲时间大于我们设置的idleSoftEvictTime，且当前池中空闲连接数量>minIdle
                        // 2.  样本空闲时间大于我们设置的idleEvictTime
                        evict = evictionPolicy.evict(evictionConfig, underTest,
                                idleObjects.size());
                    } catch (final Throwable t) {
                        PoolUtils.checkRethrow(t);
                        swallowException(new Exception(t));
                        evict = false;
                    }
                    // 如果需要回收，则释放这个样本
                    if (evict) {
                        destroy(underTest);
                        destroyedByEvictorCount.incrementAndGet();
                    } else {
                        // 如果设置了testWhileIdle，会
                        if (testWhileIdle) {
                            boolean active = false;
                            try {
                                // 利用工厂重新初始化样本，这里会校验maxConnLifetimeMillis
                                factory.activateObject(underTest);
                                active = true;
                            } catch (final Exception e) {
                                // 抛出异常标识校验不通过，释放样本
                                destroy(underTest);
                                destroyedByEvictorCount.incrementAndGet();
                            }
                            if (active) {
                                // 接下来会校验maxConnLifetimeMillis和validationQueryTimeout
                                if (!factory.validateObject(underTest)) {
                                    destroy(underTest);
                                    destroyedByEvictorCount.incrementAndGet();
                                } else {
                                    try {
                                        // 这里会将样本rollbackOnReturn、autoCommitOnReturn等
                                        factory.passivateObject(underTest);
                                    } catch (final Exception e) {
                                        destroy(underTest);
                                        destroyedByEvictorCount.incrementAndGet();
                                    }
                                }
                            }
                        }
                        // 如果状态为EVICTION或EVICTION_RETURN_TO_HEAD，修改为IDLE
                        if (!underTest.endEvictionTest(idleObjects)) {
                            //空
                        }
                    }
                }
            }
        }
        // 校验所有连接的未使用时间是否超过removeAbandonedTimeout
        final AbandonedConfig ac = this.abandonedConfig;
        if (ac != null && ac.getRemoveAbandonedOnMaintenance()) {
            removeAbandoned(ac);
        }
    }
```
> 本文为原创文章，转载请附上原文出处链接：https://github.com/ZhangZiSheng001/dbcp-demo。
