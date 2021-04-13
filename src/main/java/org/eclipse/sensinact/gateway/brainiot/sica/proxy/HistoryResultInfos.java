package org.eclipse.sensinact.gateway.brainiot.sica.proxy;

import java.io.IOException;

import org.apache.http.HttpResponse;

public class HistoryResultInfos {
	

	public static HistoryResultInfos from(HistoryCommand command, HttpResponse response) throws IOException {		
		return new HistoryResultInfos(command, HistoryResult.from(response));
	} 
	
	public static HistoryResultInfos error(HistoryCommand command, String message) {
		return new HistoryResultInfos(command, HistoryResult.error(message));
	}
	
	public final HistoryCommand command;
	public final HistoryResult result;
	
	private HistoryResultInfos(HistoryCommand command, HistoryResult result) {
		this.command = command;
		this.result = result;
	}
	
	@Override
	public String toString() {
		return "HistoryResultInfos [" + command.profile + " " + result + "]";
	}
}
