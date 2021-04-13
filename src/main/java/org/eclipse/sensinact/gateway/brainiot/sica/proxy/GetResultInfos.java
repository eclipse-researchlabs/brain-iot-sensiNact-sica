package org.eclipse.sensinact.gateway.brainiot.sica.proxy;

public class GetResultInfos {
	
	public final GetCommand command;
	public final GetResult result;
	
	public GetResultInfos(GetCommand command, GetResult result) {
		this.command = command;
		this.result = result;
	}
		
	@Override
	public String toString() {
		return "GetResultInfos [" + command.profile + "/" + command.field + " = " + result + "]";
	}
}
