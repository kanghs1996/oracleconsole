package console;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

public class OracleConsoleClientLogic implements OracleConsoleClientInterface{
	//멤버변수
	private Connection conn;
	private ResultSet rs;
	private ResultSetMetaData rsmd;
	private PreparedStatement psmt;
	private CallableStatement csmt;
	private Scanner scanner = new Scanner(System.in);
	private String [] id_pw;	
	private String user_name;
	private String password;
	//static블락
	static {
		try {
			//드라이버 로딩
			Class.forName(ORACLE_DRIVER);
		} catch (ClassNotFoundException e) {
			System.out.println("드라이버 로딩 실패:" + e.getMessage());
		}
	}
	
	public OracleConsoleClientLogic() {
		prompt();
	}

	@Override
	public String getCommand() {
		System.out.print(System.getProperty("user.home") + "> ");
		return scanner.nextLine().trim();
	}

	@Override
	public String getQuery() {
		System.out.print("SQL> ");
		String query = scanner.nextLine();
		if(query.toUpperCase().startsWith("CONN"))
			if(query.toUpperCase().contains(";"))
				return query.substring(0, query.length()-1).trim();
			else
				return query;
		if(query.toUpperCase().startsWith("SHOW"))
			if(query.toUpperCase().contains(";"))
				return query.substring(0, query.length()-1).trim();
			else
				return query;
		if("EXIT".equalsIgnoreCase(query) ||
				"QUIT".equalsIgnoreCase(query))
			if(query.toUpperCase().contains(";"))
				return query.substring(0, query.length()-1).trim();
			else
				return query;
		if(query.contains(";"))
			return query.substring(0, query.length()-1).trim();
		int i = 2;
		while(true) {						
			System.out.printf("%3d  ", i);
			query = query.concat(" " + scanner.nextLine());
			if(query.contains(";"))
				return query.substring(0, query.length()-1).trim();
			i++;
		}		
	}

	@Override
	public String getValue(String message) {
		System.out.print(message + ": ");		
		return scanner.nextLine().trim();
	}

	@Override
	public void connect(String... id_pw) {
		try {			
			conn = DriverManager.getConnection(ORACLE_URL, id_pw[0], id_pw[1]);			
			System.out.println("Connected to:\r\n"
					+ "Oracle Database 11g Express Edition Release 11.2.0.2.0 - 64bit Production");
			user_name = id_pw[0];
			password = id_pw[1];
			execute();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	@Override
	public void changeUser(String... id_pw) {		
		try {
			close();
			conn = DriverManager.getConnection(ORACLE_URL, id_pw[0], id_pw[1]);			
			System.out.println("Connected");			
			user_name = id_pw[0];
			password = id_pw[1];
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			try {
				conn = DriverManager.getConnection(ORACLE_URL, user_name, password);
			} catch (SQLException e1) {
				System.out.println(e.getMessage());
			}
		}
	}

	@Override
	public void prompt() {
		while(true) {
			String command = getCommand();
			if("EXIT".equalsIgnoreCase(command)) {
				System.out.println("프로그램 종료합니다!!!");
				System.exit(0);
			}
			if(command.toUpperCase().contains("SQLPLUS")) {
				if(command.length() > 7) {
					id_pw = command.substring(7, command.length()).trim().split("/");
					if(id_pw.length == 1)
						connect(id_pw[0], getValue("Enter password"));
					else
						connect(id_pw[0], id_pw[1]);
				}else {
					connect(getValue("Enter user-name"), getValue("Enter password"));
				}
			}
			
		}
	}

	@Override
	public void execute() throws Exception {
		while(true) {
			String query = getQuery();			
			if("EXIT".equalsIgnoreCase(query) ||
				"QUIT".equalsIgnoreCase(query)) {				
				conn.commit();
				close();
				System.out.println("Disconnected from Oracle Database 11g Express Edition Release 11.2.0.2.0 - 64bit Production");
				return;
			}else if(query.toUpperCase().startsWith("CONN")) {
				if(query.length() > 4) {
					id_pw = query.substring(4, query.length()).trim().split("/");
					if(id_pw.length == 1) {
						changeUser(id_pw[0], getValue("Enter password"));
						continue;
					}
					else {					
						changeUser(id_pw[0], id_pw[1]);
						continue;
					}
				}else {
					changeUser(getValue("Enter user-name"), getValue("Enter password"));
					continue;
				}
			}else if(query.trim().toUpperCase().startsWith("SHOW") &&
					query.trim().toUpperCase().contains("USER")) {
				System.out.println("USER is \""+ user_name.toUpperCase() +"\"");
				continue;
			}else if(query.trim().toUpperCase().startsWith("DESC")) {
				psmt = conn.prepareStatement("SELECT * from " + query.substring(4, query.length()));				
				rs = psmt.executeQuery();
				rsmd = rs.getMetaData();
				int columnCount=rsmd.getColumnCount();						
				System.out.println(String.format("%-35s %-8s %-20s", "Name", "Null?", "Type"));
				List<Integer> dashCount = new Vector<>();
				dashCount.add(35);
				dashCount.add(8);
				dashCount.add(20);
				//(-)DASH출력]
				for(Integer count:dashCount) {
					for(int i=0;i<count;i++) System.out.print("-");
					System.out.print(" ");
				}
				System.out.println();
				for(int i=1; i<=columnCount; i++){
					String type= rsmd.getColumnTypeName(i);					
					int length = rsmd.getPrecision(i);
					String columnName = rsmd.getColumnName(i);
					int isnull = rsmd.isNullable(i);
					System.out.println(String.format("%-35s %-8s %-20s", columnName,
							isnull == 0 ? "NOT NULL" : "", 
							length == 0 ? type : type + "("+length+")"));
				}
				continue;
			}
			psmt = conn.prepareStatement(query);
			try {
				boolean flag = psmt.execute();
				if(flag) {					
					rs = psmt.getResultSet();
					rsmd = rs.getMetaData();
					int columnCount = rsmd.getColumnCount();					
					List<Integer> dashCount = new Vector<>();
					for(int i=1; i<=columnCount; i++) {	
						int types=rsmd.getColumnType(i);					
						int length = rsmd.getPrecision(i);
						switch(types) {
							case Types.NCHAR:
							case Types.NVARCHAR:
								dashCount.add(length*2);break;
							case Types.TIMESTAMP:
							case Types.NUMERIC:
								dashCount.add(10);break;
							default:dashCount.add(length);
						}						
						String columnName = rsmd.getColumnName(i).length() > dashCount.get(i-1) ?
											rsmd.getColumnName(i).substring(0,dashCount.get(i-1)) :
											rsmd.getColumnName(i);
						System.out.print(String.format("%-"+(dashCount.get(i-1)+1)+"s", columnName));
					}
					System.out.println();
					for(Integer count:dashCount) {
						for(int i=0;i<count;i++) System.out.print("-");
						System.out.print(" ");
					}
					System.out.println();//줄바꿈
					//데이터 출력]
					while(rs.next()) {					
						for(int i=1;i <=columnCount;i++) {
							int type=rsmd.getColumnType(i);
							String columnValue;
							if(type==Types.TIMESTAMP)
								columnValue =rs.getDate(i).toString().trim();
							else
								columnValue = rs.getString(i);
							System.out.print(String.format("%-"+(dashCount.get(i-1)+1)+"s", 
									columnValue==null?"":columnValue));
						}
						System.out.println();//줄바꿈
					}
				}else {
					int affected=psmt.getUpdateCount();
					if(query.trim().toUpperCase().startsWith("UPDATE"))
						System.out.println(affected+"rows updated.");
					else if(query.trim().toUpperCase().startsWith("DELETE"))
						System.out.println(affected+"rows deleted.");
					else if(query.trim().toUpperCase().startsWith("INSERT"))
						System.out.println(affected+"rows created.");
					else if(query.trim().toUpperCase().startsWith("CREATE")) {
						if(query.trim().toUpperCase().contains("TABLE"))
							System.out.println("Table created.");
						else if(query.trim().toUpperCase().contains("USER"))
							System.out.println("User created.");
						else if(query.trim().toUpperCase().contains("SEQUENCE"))
							System.out.println("Sequnce created.");
						else if(query.trim().toUpperCase().contains("VIEW"))
							System.out.println("View created.");
						else if(query.trim().toUpperCase().contains("FUNCTION"))
							System.out.println("Java created.");
						else if(query.trim().toUpperCase().contains("PROCEDURE"))
							System.out.println("Procedure created.");
						else if(query.trim().toUpperCase().contains("TRIGGER"))
							System.out.println("Trigger created.");
					}
					else if(query.trim().toUpperCase().startsWith("DROP")) {
						if(query.trim().toUpperCase().contains("TABLE"))
							System.out.println("Table dropped.");
						else if(query.trim().toUpperCase().contains("USER"))
							System.out.println("User dropped.");
						else if(query.trim().toUpperCase().contains("SEQUENCE"))
							System.out.println("Sequnce dropped.");
						else if(query.trim().toUpperCase().contains("VIEW"))							
							System.out.println("View dropped.");
						else if(query.trim().toUpperCase().contains("FUNCTION"))
							System.out.println("Java dropped.");
						else if(query.trim().toUpperCase().contains("PROCEDURE"))
							System.out.println("Procedure dropped.");
						else if(query.trim().toUpperCase().contains("TRIGGER"))
							System.out.println("Trigger dropped.");
						
					}						
					else if(query.trim().toUpperCase().startsWith("ALTER")) {
						if(query.trim().toUpperCase().contains("TABLE"))
							System.out.println("Table altered.");
						else if(query.trim().toUpperCase().contains("USER"))
							System.out.println("User altered.");
						else if(query.trim().toUpperCase().contains("SEQUENCE"))
							System.out.println("Sequnce altered.");
						else if(query.trim().toUpperCase().contains("VIEW"))
							System.out.println("View altered.");
						else if(query.trim().toUpperCase().contains("FUNCTION"))							
							System.out.println("Java altered.");
						else if(query.trim().toUpperCase().contains("PROCEDURE"))
							System.out.println("Procedure altered.");
						else if(query.trim().toUpperCase().contains("TRIGGER"))
							System.out.println("Trigger altered.");
					}
					else if(query.trim().toUpperCase().startsWith("GRANT"))
						System.out.println("Grant succeeded");
				}
			} catch (SQLException e) {
				System.out.println(e.getMessage());
				conn.rollback();				
			}
		}		
	}

	@Override
	public void close() {
		try {
			if(rs != null) rs.close();			
			if(csmt != null) csmt.close();
			if(psmt != null) psmt.close();			
			if(conn != null) conn.close();			
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
}
