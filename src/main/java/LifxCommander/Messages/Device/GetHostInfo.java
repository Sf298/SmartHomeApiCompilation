package LifxCommander.Messages.Device;

import LifxCommander.Messages.DataTypes.Payload;

public class GetHostInfo extends Payload {
	int code = 12;
	
	public GetHostInfo() {}
	
	public int getCode() {
		return code;
	}

}
