package org.geektimes.projects.user.sql;

import org.geektimes.projects.user.domain.User;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public class DBConnectionManager {

    /**
     * 1、如果要测试的话，需要修改DB的路径为你本机可访问的目录。<br>
     * 2、Derby只能本一个实例连接，所以如果要用客户端连接数据库查看结果，需要终止已有的连接。<br>
     * 3、使用客户端连接的时候，注意配置的路径要一致，不要在创建的时候使用了相对路径，然后在客户端中也配置相对路径，
     * 这样的话肯定是连不上你通过application创建的DB的。
     */
    public static final String DATABASE_URL = "jdbc:derby:" + System.getProperty("user.home") +
            "/Documents/Geek/小马哥/github/user-platform" + "/db/users;create=true";

    public static final String DROP_USERS_TABLE_DDL_SQL = "DROP TABLE users";

    public static final String CREATE_USERS_TABLE_DDL_SQL =
            "CREATE TABLE users(" +
            "id INT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), " +
            "name VARCHAR(16) NOT NULL, " +
            "password VARCHAR(64) NOT NULL, " +
            "email VARCHAR(64) NOT NULL, " +
            "phoneNumber VARCHAR(64) NOT NULL" +
            ")";

    public static final String INSERT_USER_DML_SQL =
            "INSERT INTO users(name,password,email,phoneNumber) VALUES " +
            "('A','******','a@gmail.com','1') , " +
            "('B','******','b@gmail.com','2') , " +
            "('C','******','c@gmail.com','3') , " +
            "('D','******','d@gmail.com','4') , " +
            "('E','******','e@gmail.com','5')";

    private Connection connection;

    static {
        try {
            /**
             * 加载驱动方式1 - ClassLoader 加载：
             * Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
             *
             * 加载驱动方式2 - SPI：
             * for (Driver driver : ServiceLoader.load(Driver.class)) {
             *     DriverManager.registerDriver(driver);
             * }
             *
             * 获取 connection 方式1：
             * Driver driver = DriverManager.getDriver(DATABASE_URL);
             * Connection connection = driver.connect(DATABASE_URL, new Properties());
             *
             * 获取 connection 方式2：
             * Connection connection = DriverManager.getConnection(DATABASE_URL);
             *
             * 获取 connection 方式3 - JNDI：
             * 参看下面 getConn() 方法
             */
            /*for (Driver driver : ServiceLoader.load(Driver.class)) {
                DriverManager.registerDriver(driver);
            }
            Connection connection = DriverManager.getConnection(DATABASE_URL);
            initTables(connection);
            */

            // JNDI
            initTables(getConn());
        } catch (SQLException | NamingException e) {
            // TODO
            e.printStackTrace();
        }
    }

    private static Connection getConn() throws NamingException, SQLException {
        // Obtain our environment naming context
        Context initCtx = new InitialContext();
        Context envCtx = (Context) initCtx.lookup("java:comp/env");

        // Look up our data source
        DataSource ds = (DataSource) envCtx.lookup("jdbc/UserPlatformDB");

        // Allocate and use a connection from the pool
        return ds.getConnection();
    }

    public Connection getConnection() {
        /*try {
            return this.connection = DriverManager.getConnection(DATABASE_URL);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }*/

        // JNDI 方式获取 connection
        try {
            return getConn();
        } catch (NamingException e) {
            e.printStackTrace();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public void releaseConnection() {
        releaseConnection(this.connection);
    }

    private static void releaseConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e.getCause());
            }
        }
    }

    private static void initTables(Connection connection) throws SQLException {
        System.out.println("Checking database for table...");
        Statement statement = connection.createStatement();
        DatabaseMetaData databaseMetadata = connection.getMetaData();
        ResultSet resultSet = databaseMetadata.getTables(null, null, "USERS", null);
        if (resultSet.next()) {
            statement.execute(DROP_USERS_TABLE_DDL_SQL);
            System.out.println("Table 'users' already exists, drop it!");
        }
        // 创建 users 表
        statement.execute(CREATE_USERS_TABLE_DDL_SQL);
        System.out.println("Table 'users' created!");
    }

    public static void main(String[] args) throws Exception {

        Connection connection = DriverManager.getConnection(DATABASE_URL);
        Statement statement = connection.createStatement();

        // 创建 Users 表
        initTables(connection);

        System.out.println(statement.executeUpdate(INSERT_USER_DML_SQL));

        // 执行查询语句（DML）
        ResultSet resultSet = statement.executeQuery("SELECT id,name,password,email,phoneNumber FROM users");

        // BeanInfo
        BeanInfo userBeanInfo = Introspector.getBeanInfo(User.class, Object.class);

        // 所有的 Properties 信息
        for (PropertyDescriptor propertyDescriptor : userBeanInfo.getPropertyDescriptors()) {
            System.out.println(propertyDescriptor.getName() + " , " + propertyDescriptor.getPropertyType());
        }

        // 写一个简单的 ORM 框架
        // 如果存在并且游标滚动
        while (resultSet.next()) {
            User user = new User();

            // ResultSetMetaData 元信息
            ResultSetMetaData metaData = resultSet.getMetaData();
//            System.out.println("当前表的名称：" + metaData.getTableName(1));
//            System.out.println("当前表的列个数：" + metaData.getColumnCount());
//            for (int i = 1; i <= metaData.getColumnCount(); i++) {
//                System.out.println("列名称：" + metaData.getColumnLabel(i) + ", 类型：" + metaData.getColumnClassName(i));
//            }

            StringBuilder queryAllUsersSQLBuilder = new StringBuilder("SELECT");
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                queryAllUsersSQLBuilder.append(" ").append(metaData.getColumnLabel(i)).append(",");
            }
            // 移除最后一个 ","
            queryAllUsersSQLBuilder.deleteCharAt(queryAllUsersSQLBuilder.length() - 1);
            queryAllUsersSQLBuilder.append(" FROM ").append(metaData.getTableName(1));

            System.out.println(queryAllUsersSQLBuilder);

            // 方法直接调用（编译时，生成字节码）
//            user.setId(resultSet.getLong("id"));
//            user.setName(resultSet.getString("name"));
//            user.setPassword(resultSet.getString("password"));
//            user.setEmail(resultSet.getString("email"));
//            user.setPhoneNumber(resultSet.getString("phoneNumber"));

            // 利用反射 API，来实现字节码提升

            // User 类是通过配置文件，类名成
            // ClassLoader.loadClass -> Class.newInstance()
            // ORM 映射核心思想：通过反射执行代码（性能相对开销大）
            for (PropertyDescriptor propertyDescriptor : userBeanInfo.getPropertyDescriptors()) {
                String fieldName = propertyDescriptor.getName();
                Class fieldType = propertyDescriptor.getPropertyType();
                String methodName = typeMethodMappings.get(fieldType);
                // 可能存在映射关系（不过此处是相等的）
                String columnLabel = mapColumnLabel(fieldName);
                Method resultSetMethod = ResultSet.class.getMethod(methodName, String.class);
                // 通过放射调用 getXXX(String) 方法
                Object resultValue = resultSetMethod.invoke(resultSet, columnLabel);
                // 获取 User 类 Setter方法
                // PropertyDescriptor ReadMethod 等于 Getter 方法
                // PropertyDescriptor WriteMethod 等于 Setter 方法
                Method setterMethodFromUser = propertyDescriptor.getWriteMethod();
                // 以 id 为例，  user.setId(resultSet.getLong("id"));
                setterMethodFromUser.invoke(user, resultValue);
            }

            System.out.println(user);
        }

        connection.close();
    }

    private static String mapColumnLabel(String fieldName) {
        return fieldName;
    }

    /**
     * 数据类型与 ResultSet 方法名映射
     */
    static Map<Class, String> typeMethodMappings = new HashMap<>();

    static {
        typeMethodMappings.put(Long.class, "getLong");
        typeMethodMappings.put(String.class, "getString");
    }
}
