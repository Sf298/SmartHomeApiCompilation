package LifxCommander.Messages.Device;

import LifxCommander.Messages.DataTypes.Payload;

public class GetService extends Payload {
	int code = 2;
	
	public GetService() {}
	
	public int getCode() {
		return code;
	}
}
