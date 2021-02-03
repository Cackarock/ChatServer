import java.io.Serializable;

@SuppressWarnings("serial")
public class Request implements Serializable
{
	private String type;
	private String data;
	private boolean returns;
	
	public Request(String type, String data)
	{
		this.setType(type);
		this.setData(data);
	}
	
	public Request(String type)
	{
		this.setType(type);
	}

	public String getType() 
	{
		return type;
	}

	public void setType(String type) 
	{
		this.type = type;
	}

	public String getData() 
	{
		return data;
	}

	public void setData(String data) 
	{
		this.data = data;
	}

	public boolean getReturns() 
	{
		return returns;
	}

	public void setReturns(boolean returns) 
	{
		this.returns = returns;
	}
}
