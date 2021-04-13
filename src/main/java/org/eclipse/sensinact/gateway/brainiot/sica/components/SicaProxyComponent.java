package org.eclipse.sensinact.gateway.brainiot.sica.components;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.eclipse.sensinact.gateway.brainiot.sica.proxy.GetCommand;
import org.eclipse.sensinact.gateway.brainiot.sica.proxy.GetResult;
import org.eclipse.sensinact.gateway.brainiot.sica.proxy.GetResultInfos;
import org.eclipse.sensinact.gateway.brainiot.sica.proxy.HistoryCommand;
import org.eclipse.sensinact.gateway.brainiot.sica.proxy.HistoryResultInfos;
import org.eclipse.sensinact.gateway.brainiot.sica.proxy.SetCommand;
import org.eclipse.sensinact.gateway.brainiot.sica.proxy.SetResult;
import org.eclipse.sensinact.gateway.brainiot.sica.proxy.SetResultInfos;
import org.eclipse.sensinact.gateway.brainiot.sica.proxy.SicaLatestValues;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = SicaProxyComponent.class,
configurationPolicy = ConfigurationPolicy.REQUIRE, 
configurationPid = {"sna.sica.bridge.SicaConnectionConfig"})
public class SicaProxyComponent {

	private CloseableHttpClient client;
	private String host;
	private String user;
	private String password;

	private final static Logger LOG = LoggerFactory.getLogger(SicaProxyComponent.class);

	private final CloseableHttpResponse request(HttpRequestBase request) {
		HttpContext httpContext = new BasicHttpContext();
		CloseableHttpResponse response = null;
		try {
			response = client.execute(request, httpContext);

			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
				Header authHeader = response.getFirstHeader(AUTH.WWW_AUTH);
				DigestScheme digestScheme = new DigestScheme();
				digestScheme.overrideParamter("realm", "DataWebService");
				digestScheme.processChallenge(authHeader);
				response.close();

				UsernamePasswordCredentials creds = new UsernamePasswordCredentials(this.user,this.password);
				request.addHeader(digestScheme.authenticate(creds, request, httpContext));
				response = client.execute(request);
				int code = response.getStatusLine().getStatusCode();
				LOG.debug(String.format("%s [%s]",response,code));
			}

		} catch (IOException | MalformedChallengeException | AuthenticationException e) {
			LOG.error(e.getMessage(),e);
			e.printStackTrace();
		}
		return response;
	} 
	
	@Activate
	public void activate(SicaBridgeConfiguration config) {
		LOG.info("SicaProxyImpl ACTIVATED");
		client = HttpClients.custom().build();
		this.host = config.host();
		this.user = config.user();
		this.password = config.password();
	}

	@Deactivate
	public void deactivate() {
		if(this.client != null)
			try {
				this.client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		this.client = null;
		LOG.info("SicaProxyImpl DEACTIVATED");
	}

	public GetResultInfos get(GetCommand command) {
		GetResult res = getLatestValues(command).toResult();
		GetResultInfos result = new GetResultInfos(command, res);
		LOG.info(result.toString());
		return result;
	}

	public SetResultInfos set(SetCommand command) {
//		String[] keys = ResourcesUtils.SICA_PROFILES.get(command.sicaId);
//		if (keys == null) {
//			String msg = "Invalid serverId/groupId: " + command.sicaId;
//			return new SetResultInfos(command, SetResult.failure(msg));
//		}
//		if (keys.length != command.values.length) {
//			String msg = command.values.length + " values provided whereas " + keys.length + " are expected";
//			return new SetResultInfos(command, SetResult.failure(msg));
//		}

		return pushValues(command);
	}

	public HistoryResultInfos getHistory(HistoryCommand command) {
		String url = host + command.getPath();
		HttpGet get = new HttpGet(url);
		LOG.debug("Executing request " + get.getRequestLine());
		CloseableHttpResponse response = null;
		try{
			response = request(get);
			System.out.println(response);
			LOG.debug("Response received " + response.getStatusLine());
			return HistoryResultInfos.from(command, response);
		} catch (Exception e) {
			return HistoryResultInfos.error(command, e.getMessage());
		} finally {
			if(response != null){
				try {
					response.close();
				} catch(IOException e) {
					e.printStackTrace();
				}
				response =null;
			}
		}
	}

	private SicaLatestValues getLatestValues(GetCommand command) {
		String url = host + command.getPath();
		HttpGet get = new HttpGet(url);
		LOG.debug("Executing request " + get.getRequestLine());
		CloseableHttpResponse response = null;
		try{
			response = request(get);
			LOG.debug("Response received " + response.getStatusLine());
			return SicaLatestValues.fromResponse(command, response);
		} catch (Exception e) {
			return SicaLatestValues.error(command, "Reques(context.getConnection().t failed: " + e.getMessage());
		} finally {
			if(response != null){
				try {
					response.close();
				} catch(IOException e) {
					e.printStackTrace();
				}
				response =null;
			}
		}
	}

	private SetResultInfos pushValues(SetCommand command) {
		String url = host + command.getPath();
		HttpPut put = new HttpPut(url);
		CloseableHttpResponse response = null;
		try{
			StringEntity requestEntity = new StringEntity(command.getPayload(), ContentType.APPLICATION_JSON);
			put.setEntity(requestEntity);

			LOG.debug("Executing request " + put.getRequestLine());
			response = request(put);
			LOG.debug("Response received " + response.getStatusLine());
			int code = response.getStatusLine().getStatusCode();
			return new SetResultInfos(command, SetResult.success("Status code " + code));
		} catch (Exception e) {
			return new SetResultInfos(command, SetResult.failure(e.getMessage()));
		} finally {
			if(response != null){
				try {
					response.close();
				} catch(IOException e) {
					e.printStackTrace();
				}
				response =null;
			}
		}
	}
}
