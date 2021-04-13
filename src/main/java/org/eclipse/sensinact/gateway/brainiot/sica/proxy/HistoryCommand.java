package org.eclipse.sensinact.gateway.brainiot.sica.proxy;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public class HistoryCommand {

	public final Instant startInstant;
	public final Instant endInstant;
	public final String profile;
	public final String[] fields;

	public HistoryCommand(String profile, Instant startInstant, Instant endInstant, String[] fields) {
		this.profile = profile;
		this.startInstant = startInstant;
		this.endInstant = endInstant;
		this.fields = fields;
	}
	
	public String getStartTimestamp() {
		return OffsetDateTime.ofInstant(startInstant, ZoneId.of("UTC")).toString();
	}
	
	public String getEndTimestamp() {
		return OffsetDateTime.ofInstant(endInstant, ZoneId.of("UTC")).toString();
	}
	
	public String getPath() {
		String path = "/api/information/" + profile + "/export?startdate=" + getStartTimestamp() + "&enddate=" + getEndTimestamp();
		for (String field : fields) {
			path += "&fields=" + field;
		}
		return path;
	}
}
