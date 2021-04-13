package org.eclipse.sensinact.gateway.brainiot.sica.proxy;

import java.time.Instant;
import java.time.OffsetDateTime;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class SicaLatestValues {

	private final GetCommand command;
	private final String json;
	private final Instant instant;
	private final double values[];

	private final boolean isValid;
	private final String message;

	private SicaLatestValues(GetCommand command, String json, Instant instant, double[] values, boolean isValid, String message) {
		this.command = command;
		this.json = json;
		this.instant = instant;
		this.values = values;
		this.isValid = isValid;
		this.message = message;
	}

	public static SicaLatestValues error(GetCommand command, String message) {
		return new SicaLatestValues(command, null, null, new double[0], false, message);
	}

	public static SicaLatestValues fromResponse(GetCommand command, CloseableHttpResponse response) {
		int code = response.getStatusLine().getStatusCode();
		if (code < 200 || code >= 300)
			return SicaLatestValues.error(command, "Status code " + code);
		else {
			try {
				String payload = EntityUtils.toString(response.getEntity());
				return SicaLatestValues.fromJson(command, payload);
			} catch (Exception e) {
				return SicaLatestValues.error(command, "Invalid payload: " + e.getMessage());
			}
		}
	}

	private static SicaLatestValues fromJson(GetCommand command, String json) {
		try {
			JSONObject root = new JSONObject(json);
			Instant instant = OffsetDateTime.parse(root.getString("Date")).toInstant();
			JSONArray tab = root.getJSONArray("Values");

			double[] array = new double[tab.length()];
			for (int i = 0; i < tab.length(); i++) {
				if ("null".equalsIgnoreCase(tab.getString(i)))
					array[i] = 0;
				else {
					double cur = tab.getDouble(i);
					array[i] = cur;
				}
			}
			return new SicaLatestValues(command, json, instant, array, true, null);
		} catch (Exception e) {
			return new SicaLatestValues(command, json, null, new double[0], false, "Json malformed: " + e.getMessage());
		}
	}

	
	public GetResult toResult() {
		if (isValid) {
			if ("all".equals(command.field)) {
				return GetResult.valueOf(json);
			} else if ("fecha".equals(command.field)) {
				return GetResult.valueOf(instant);
			} else {
				
				try {
					int idx =  command.index;
					double[] valList = values;
					if (idx >= valList.length) {
						String msg = "idx=" + idx + " length=" + valList.length;
						return GetResult.errorOf("not enough values in list", msg);
					} else {
						return GetResult.valueOf(Double.toString(valList[idx]));
					}
				} catch (Exception e) {
					return GetResult.errorOf(e);
				}
			}
		} else {
			String msg = message + " - json : " + json;
			return GetResult.errorOf("SicaLatestValues fetching error", msg);
		}
	}
	
	public boolean isValid() {
		return isValid;
	}

	public Instant getInstant() {
		return instant;
	}

	public String getMessage() {
		return message;
	}

	public String getJson() {
		return json;
	}

	public double[] getValues() {
		return values;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("SicaLatestValues ");
		if (isValid) {
			sb.append("OK ");
			sb.append(instant);
			for (int i = 0; i < values.length; i++) {
				sb.append(" ");
				sb.append(values[i]);
			}
		} else {
			sb.append("ERROR ");
			sb.append(message);
		}

		return sb.toString();
	}
}
