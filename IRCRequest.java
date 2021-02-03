
import java.io.Serializable;

public class IRCRequest implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2193091130641718696L;
	
	protected String command;
	
	protected String information;
	
	public IRCRequest(String cmmd, String info) {
		command = cmmd;
		information = info;
	}
}
