package org.eclipse.sensinact.gateway.brainiot.sica.proxy;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONObject;

public class HistoricalMeasure {
	public final Instant date;
	public final Double[] values;

	public HistoricalMeasure(JSONObject json) {
		JSONArray vals = json.getJSONArray("Values");
		values = new Double[vals.length()];
		for (int i=0; i<vals.length(); i++)
			values[i] = vals.optDouble(i);
		String dateStr = json.getString("Date");
		date = OffsetDateTime.parse(dateStr).toInstant(); 
	}

	@Override
	public String toString() {
		return "{\"date\":" + date.toEpochMilli() + ", \"values\":" + Arrays.toString(values) + "}";
	}	
}
