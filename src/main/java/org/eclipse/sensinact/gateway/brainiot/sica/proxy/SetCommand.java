package org.eclipse.sensinact.gateway.brainiot.sica.proxy;

import org.json.JSONArray;
import org.json.JSONObject;

public class SetCommand {
	public final long date;
	public final double[] values;
	public final String profile;
	public final String field;
	public final int index;
	
	public SetCommand(String profile, String field, int index, long date, double[] values) {
		this.profile = profile;
		this.field = field;
		this.index = index;
		this.date = date;
		this.values = values;
	}
		
	public String getPath() {
		return "/api/information/add";
	}
	
	public String getPayload() {
		JSONArray data = new JSONArray();
		JSONObject object = new JSONObject();
		String[] profileElements = profile.split("_");
		data.put(object);
		object.put("ServerId", Integer.parseInt(profileElements[0]));
		object.put("GroupId", Integer.parseInt(profileElements[1]));
		object.put("Date", "/Date(" + date + ")/");
		JSONArray array = new JSONArray();
		for (double element : values)
			array.put(element);
		object.put("Values", array);
		
		return data.toString();
	}
}
