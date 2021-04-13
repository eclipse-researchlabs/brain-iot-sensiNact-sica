package org.eclipse.sensinact.gateway.brainiot.sica.proxy;

public class GetCommand {

	public final String profile;
	public final String field;
	public final int index;

	public GetCommand(String profile, String field, int index) {
		this.profile = profile;
		this.field = field;
		this.index = index;
	}

	public String getPath() {
		return "/api/information/" + this.profile + "/last";
	}
}
