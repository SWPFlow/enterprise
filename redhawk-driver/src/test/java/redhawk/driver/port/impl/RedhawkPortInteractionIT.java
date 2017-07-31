package redhawk.driver.port.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import redhawk.driver.RedhawkDriver;
import redhawk.driver.application.RedhawkApplication;
import redhawk.driver.bulkio.Packet;
import redhawk.driver.exceptions.ApplicationStopException;
import redhawk.driver.exceptions.CORBAException;
import redhawk.driver.exceptions.MultipleResourceException;
import redhawk.driver.port.PortListener;
import redhawk.driver.port.RedhawkPort;

public class RedhawkPortInteractionIT {
	private static RedhawkDriver driver;

	@BeforeClass
	public static void setup() {
		driver = new RedhawkDriver();
	}

	@Test
	public void connectOctet() throws ApplicationStopException {
		RedhawkApplication app = null;
		String portName = "dataOctet_out";
		try {
			app = driver.getDomain().getApplications().get(0);

			// Get port
			RedhawkPort port = app.getComponentByName("DataConverter_1.*").getPort(portName);

			// Send To Port
			RedhawkPort sendToPort = app.getComponentByName("DataConverter_2.*").getPort("dataOctet");

			// Listen to data on port
			GenericPortListener pl = new GenericPortListener(sendToPort, true);

			port.connect(pl);
			//Active SRI should be empty because no data has been sent
			System.out.println(sendToPort.getActiveSRIs());
			
			if (!app.isStarted())
				app.start();
			while (pl.getMessagesReceived() < 10) {
				// Loop can't be empty
				Thread.sleep(1);
				
				//Send data out
				if(pl.receivedData)
					sendToPort.send(pl.getPacket());
			}

			port.disconnect();
			assertEquals("Should have 10 messages", new Integer(10), pl.getMessagesReceived());
			
			//ActiveSRI should be non empty because data has been sent
			System.out.println(sendToPort.getActiveSRIs());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("Test failure " + e.getMessage());
		} finally {
			if (app != null)
				app.stop();
		}
	}

	/*
	 * Test sending data from a DataConverter to the various ports that data
	 * converter has available via the driver
	 */
	@Test
	public void test() {
		try {
			RedhawkApplication app = driver.getDomain().getApplications().get(0);

			/*
			 * Get the out(uses) ports on the DataConverter Component and it's matching
			 * DataConverter for this test.
			 */
			List<RedhawkPort> usesPorts = new ArrayList<>();
			// Don't check char with this test
			for (RedhawkPort port : app.getComponentByName("DataConverter_1.*").getPorts()) {
				if (port.getName().endsWith("out") && !port.getName().contains("Char")
						&& !port.getName().contains("Octet")) {
					System.out.println(port.getName());
					usesPorts.add(port);
				}
			}

			/*
			 * For Each Uses port run a test to take it's data and send it into the other
			 * data converter make sure you can get the data out
			 */
			for (RedhawkPort usesPort : usesPorts) {
				System.out.println(usesPort);
				/*
				 * Make a port connection
				 */
				GenericPortListener pl = new GenericPortListener();
				usesPort.connect(pl);
				Boolean appStarted = false;
				System.out.println(usesPort.getName());
				while (!pl.getReceivedData()) {
					/*
					 * Start app and make sure I can receive data
					 */
					if (!appStarted) {
						app.start();
						appStarted = true;
					}
				}

				/*
				 * Stop app and disconnect ports to clean up
				 */
				app.stop();
				usesPort.disconnect();
				System.out.println("Received this many messages: " + pl.getMessagesReceived());
				// Make sure you've cleaned up properly
				assertTrue("Should no longer be port connections", usesPort.getConnectionIds().isEmpty());
			}
		} catch (MultipleResourceException | CORBAException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	class GenericPortListener extends PortListener<Object[]> {
		private Boolean receivedData = false;

		private Integer messagesReceived = 0;

		private RedhawkPort portToSendTo;

		private Boolean sendForward;
		
		private Packet<Object[]> packet; 

		public GenericPortListener() {
			sendForward = false;
		}

		public GenericPortListener(RedhawkPort port, Boolean sendForward) {
			portToSendTo = port;
		}

		@Override
		public void onReceive(Packet<Object[]> packet) {
			messagesReceived++;
			receivedData = true;
			this.packet = packet;
			System.out.println("Received data " + messagesReceived);

			/*if (sendForward && portToSendTo != null) {
				System.out.println("Made it inside if");
				try {
					System.out.println("Hello World");
					portToSendTo.send(packet);
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println("Doesn't work with driver send");
					try {
						dataOctet t = dataOctetHelper.narrow(portToSendTo.getCorbaObject());
						Object[] data = packet.getData();
						byte[] temp = new byte[data.length];
						for(int i=0; i<packet.getData().length; i++){
							temp[i] = (byte) data[i];
						}
						t.pushPacket(temp, packet.getTime(), packet.isEndOfStream(), packet.getStreamId());
					}catch(Exception ex) {
						System.err.println("Still not working :-(");
						ex.printStackTrace();
					}
				}
			}
			*/
		}

		public Boolean getReceivedData() {
			return receivedData;
		}

		public Integer getMessagesReceived() {
			return messagesReceived;
		}
		
		public Packet getPacket() {
			return this.packet;
		}
	}
}
