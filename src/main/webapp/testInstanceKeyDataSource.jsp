<%@page import="java.sql.Connection"%>
<%@page import="javax.naming.Context"%>
<%@page import="javax.naming.RefAddr"%>
<%@page import="javax.naming.CompositeName"%>
<%@page import="org.apache.commons.dbcp2.datasources.SharedPoolDataSourceFactory"%>
<%@page import="org.apache.commons.dbcp2.datasources.SharedPoolDataSource"%>
<%@page import="javax.naming.StringRefAddr"%>
<%@page import="javax.naming.Reference"%>
<%@page import="javax.sql.DataSource"%>
<%@page import="javax.naming.InitialContext"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Insert title here</title>
</head>
<body>
    <%  
        // Obtain our environment naming context
    	Context initCtx = new InitialContext();
    	Context envCtx = (Context)initCtx.lookup("java:comp/env/");
    	
    	// Look up our data source
    	DataSource ds = (DataSource)envCtx.lookup("bean/SharedPoolDataSourceFactory");
    	
        DataSource ds2 = (DataSource)envCtx.lookup("bean/PerUserPoolDataSourceFactory");
    	
    	// Allocate and use a connection from the pool
    	Connection conn = ds.getConnection("root","root");
    	System.out.println("conn" + conn); 
        Connection conn2 = ds2.getConnection("root","root");
        System.out.println("conn2" + conn2); 
        
        // ... use this connection to access the database ...
    	conn.close();
    	conn2.close();
    %>
</body>
</html>