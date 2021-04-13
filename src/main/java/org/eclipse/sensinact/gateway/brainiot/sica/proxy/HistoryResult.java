package org.eclipse.sensinact.gateway.brainiot.sica.proxy;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;

public class HistoryResult {

	public static HistoryResult error(String message) {
		return new HistoryResult(false, null, message);
	}

	public static HistoryResult from(HttpResponse response) throws IOException {
		int code = response.getStatusLine().getStatusCode();
		boolean success = code >= 200 && code < 300;

		if (success) {
			try {
				HistoricalMeasure[] measures = parseResponse(response);
				return new HistoryResult(true, measures, null);
			} catch (Exception e) {
				return error("Payload parsing error : " + e.getMessage());
			}				
		} else {
			return error("return code is " + code);
		}
	}
	
	private static HistoricalMeasure[] parseResponse(HttpResponse response) throws IOException {
		String payload = EntityUtils.toString(response.getEntity());
		JSONArray array = new JSONArray(payload);
		HistoricalMeasure[] measures = new HistoricalMeasure[array.length()];
		for (int i=0; i< array.length(); i++) {
			measures[i] = new HistoricalMeasure(array.getJSONObject(i));
		}
		return measures;
	}
	
	public final boolean isSuccess;
	public final HistoricalMeasure[] measures;
	public final String message;
	
	private HistoryResult(boolean isSuccess, HistoricalMeasure[] measures, String message) {
		this.isSuccess = isSuccess;
		this.measures = measures;
		this.message = message;
	}
	
	@Override
	public String toString() {
		String msg = isSuccess ? "OK" : "ERROR";
		int l = measures != null ? measures.length : 0;
		msg+= ": " + l + " values";
		if (message != null && message.length() != 0)
			msg += " message=" + message;
		return msg;
	}
}