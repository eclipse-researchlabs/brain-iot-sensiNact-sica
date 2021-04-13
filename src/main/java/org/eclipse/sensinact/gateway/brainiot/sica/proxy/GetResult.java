package org.eclipse.sensinact.gateway.brainiot.sica.proxy;

import java.time.Instant;

public class GetResult {

	static GetResult valueOf(String value) {
		return new GetResult(value, null, null);
	}

	public static GetResult valueOf(Instant instant) {
		return new GetResult(Long.toString(instant.toEpochMilli()), null, null);
	}
	
	private static GetResult errorOf(String error) {
		return new GetResult(null, error, null);
	}
	
	static GetResult errorOf(String error, String message) {
		return new GetResult(null, error, message);
	}
	
	static GetResult errorOf(Exception e) {
		String msg = e.getMessage();
		if (msg == null || msg.length() == 0)
			msg = "Error";
		return errorOf(msg);
	}
	
	public final String value;
	public final String error;
	public final String message;
	public final boolean isSuccess;
			
	private GetResult(String value, String error, String message) {
		this.value = value;
		this.error = error;
		this.message = message;
		this.isSuccess = (error == null ||  error.length() == 0);
	}

	@Override
	public String toString() {
		String msg = isSuccess ? "OK" : "ERROR";
		
		if (value != null && value.length() != 0)
			msg += " value=" + value;
		if (error != null && error.length() != 0)
			msg += " error=" + error;
		if (message != null && message.length() != 0)
			msg += " message=" + message;
		
		return msg;
	}
}