package LifxCommander.Messages.Device;

import LifxCommander.Messages.DataTypes.Payload;

public class GetGroup extends Payload{
	int code = 51;
	
	public GetGroup() {}
	
	public int getCode() {
		return code;
	}

}
