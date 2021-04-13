package org.eclipse.sensinact.gateway.brainiot.sica.proxy;

public class SetResultInfos {
	
	public final SetCommand command;
	public final SetResult result;

	public SetResultInfos(SetCommand command, SetResult result) {
		this.command = command;
		this.result = result;
	}
	
	@Override
	public String toString() {
		return "SetResultInfos [" + command.profile + " " + command.date + " = " + result + "]";
	}
}
