package org.eclipse.sensinact.gateway.brainiot.sica.proxy;

public class SetResult {
	public final boolean isSuccess;
	public final String message;

	private SetResult(boolean isSuccess, String message) {
		this.isSuccess = isSuccess;
		this.message = message;
	}
		
	public static SetResult success(String message) {
		return new SetResult(true, message); 
	}

	public static SetResult failure(String message) {
		return new SetResult(false, message); 
	}

	@Override
	public String toString() {
		String msg = isSuccess ? "OK" : "ERROR";		
		if (message != null && message.length() != 0)
			msg += " message=" + message;
		return msg;
	}
}
