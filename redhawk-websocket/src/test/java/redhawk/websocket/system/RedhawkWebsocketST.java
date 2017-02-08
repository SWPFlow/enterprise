/*
 * This file is protected by Copyright. Please refer to the COPYRIGHT file
 * distributed with this source distribution.
 *
 * This file is part of REDHAWK __REDHAWK_PROJECT__.
 *
 * REDHAWK __REDHAWK_PROJECT__ is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * REDHAWK __REDHAWK_PROJECT__ is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package redhawk.websocket.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.Gson;

import redhawk.driver.application.RedhawkApplication;
import redhawk.websocket.model.DomainManagementAction;
import redhawk.websocket.model.DomainManagementModel;
import redhawk.websocket.test.util.JettySupport;
import redhawk.websocket.test.util.RedhawkTestUtil;
import redhawk.websocket.test.util.RedhawkWebSocketTestUtil;

public class RedhawkWebsocketST {
    private static Log logger = LogFactory.getLog(RedhawkWebsocketST.class);

	private static RedhawkApplication application;

	private final String jsonRegex = ".*[{\\[].*[\\]}]";

	private static Server server;

	@BeforeClass
	public static void setup() throws Exception {
		assertTrue(
				"REDHAWK_DEV must exist to run this System test. Pass in param -DskipSystemTests=true to skip this test.",
				RedhawkTestUtil.redhawkDevExists());
		application = RedhawkTestUtil.launchApplication(true);
		server = JettySupport.setupJettyServer();
	}

	@Test
	public void testWebsocketOnJsonPort() throws Exception {
		Pattern pattern = Pattern.compile(jsonRegex);
		String endpoint; 
		if(RedhawkTestUtil.getRHMajorVersion()>=2){
			endpoint = RedhawkTestUtil.sampleWebSocketPortEndpoint("dataFloat_out.json");
		}else{
			endpoint = RedhawkTestUtil.sampleWebSocketPortEndpoint("out.json");
		}
		RedhawkWebSocketTestUtil socket = new RedhawkWebSocketTestUtil(3);

		this.performSocketConnection(endpoint, socket);

		assertTrue(
				"Socket data should indicate that it was able to connect to web server",
				socket.getData().contains("socket connected: "));
		assertTrue(
				"Socket data should indicate that it was able to disconnect from web server",
				socket.getData().contains("socket disconnected: "));

		String[] messages = socket.getData().split("\n");
		assertTrue("Should have 5 messages ", messages.length == 5);
		for (String message : messages) {
			if (message.startsWith("received ")) {
				Matcher matcher = pattern.matcher(message);
				assertTrue("Message should contain json pattern ",
						matcher.find());
			}
		}
	}

	@Test
	public void testWebsocketOnBinaryPort() throws Exception {
		String endpoint; 
		
		if(RedhawkTestUtil.getRHMajorVersion()>=2){
			endpoint = RedhawkTestUtil.sampleWebSocketPortEndpoint("dataFloat_out");
		}else{
			endpoint = RedhawkTestUtil.sampleWebSocketPortEndpoint("out");
		}
		RedhawkWebSocketTestUtil socket = new RedhawkWebSocketTestUtil(3);

		this.performSocketConnection(endpoint, socket);

		assertTrue(
				"Socket data should indicate that it was able to connect to web server",
				socket.getData().contains("socket connected: "));
		assertTrue(
				"Socket data should indicate that it was able to disconnect from web server",
				socket.getData().contains("socket disconnected: "));

		String[] messages = socket.getData().split("\n");
		assertTrue("Should have 5 messages ", messages.length == 5);
		int binaryMessageCount = 0; 
		for (String message : messages) {
			if (message.startsWith("received binary")) {
				binaryMessageCount++; 
			}
		}
		assertEquals("Should have received 2 binary messages.", 2, binaryMessageCount);
	}

	@Test
	public void testWebsocketEventChannel() throws Exception {
		URI uri = URI.create(RedhawkTestUtil
				.sampleWebSocketEventChannel("standardEvent"));

		WebSocketClient client = new WebSocketClient();
		RedhawkWebSocketTestUtil socket = new RedhawkWebSocketTestUtil(2);
		try {
			try {
				client.start();

				// Attempt Connect
				Future<Session> fut = client.connect(socket, uri);
				application.release();
				application = RedhawkTestUtil.launchApplication(true);
				while (socket.getMessageCount() < socket.getMessagesToKeep()) {
					logger.info("hello world");
				}
				// client.stop();
			} finally {
				client.stop();
			}
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
		
		String[] json = socket.getData().split("\n");
		assertTrue(
				"Socket data should indicate that it was able to connect to web server",
				socket.getData().contains("socket connected: "));
		assertTrue(
				"Socket data should indicate that it was able to disconnect from web server",
				socket.getData().contains("socket disconnected: "));
		Gson gson = new Gson();
		Boolean remove = false, add = false;
		for (String a : json) {
			int beginJson = a.indexOf("{");
			int endJson = a.indexOf("}");
			if (a.startsWith("received text message:") && beginJson != -1 && endJson != -1) {
				//System.out.println("JSON: "+a.substring(beginJson, endJson + 1));
				DomainManagementModel model = gson.fromJson(
						a.substring(beginJson, endJson + 1),
						DomainManagementModel.class);
				if (model.getAction().equals(DomainManagementAction.ADD)) {
					add = true;
				} else {
					remove = true;
				}
			}
		}

		assertTrue(remove && add);
	}

	@AfterClass
	public static void cleanup() throws Exception {
		application.release();
	}

	public void performSocketConnection(String uriString,
			RedhawkWebSocketTestUtil socket) {
		/*
		 * All this needs to be cleaned up but need to test
		 */
		URI uri = URI.create(uriString);

		WebSocketClient client = new WebSocketClient();
		try {
			try {
				client.start();

				// Attempt Connect
				Future<Session> fut = client.connect(socket, uri);
				while (socket.getMessageCount() < socket.getMessagesToKeep()) {
					logger.info("hello world");
				}

				// client.stop();
			} finally {
				client.stop();
			}
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}
}
