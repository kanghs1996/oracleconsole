package console;

public interface OracleConsoleClientInterface {
	//상수
	String ORACLE_DRIVER = "oracle.jdbc.OracleDriver";
	String ORACLE_URL = "jdbc:oracle:thin:@127.0.0.1:1521:xe";
	//메소드
	String getCommand();
	String getQuery();
	String getValue(String message);
	void connect(String ... conn);
	void changeUser(String ... conn);
	void prompt();
	void execute() throws Exception;	
	void close();
}
