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
package redhawk.driver.port.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import BULKIO.PrecisionUTCTime;
import BULKIO.UsesPortStatistics;
import BULKIO.UsesPortStatisticsProvider;
import BULKIO.UsesPortStatisticsProviderHelper;
import BULKIO.dataChar;
import BULKIO.dataDouble;
import BULKIO.dataFile;
import BULKIO.dataFloat;
import BULKIO.dataLong;
import BULKIO.dataLongLong;
import BULKIO.dataOctet;
import BULKIO.dataSDDS;
import BULKIO.dataShort;
import BULKIO.dataUlong;
import BULKIO.dataUlongLong;
import BULKIO.dataUshort;
import BULKIO.dataXML;
import CF.Port;
import CF.PortHelper;
import CF.PortPackage.InvalidPort;
import CF.PortPackage.OccupiedPort;
import redhawk.driver.bulkio.BulkIOData;
import redhawk.driver.bulkio.DataTypeFactory;
import redhawk.driver.bulkio.DataTypes;
import redhawk.driver.bulkio.Packet;
import redhawk.driver.exceptions.PortException;
import redhawk.driver.port.PortListener;
import redhawk.driver.port.RedhawkPort;
import redhawk.driver.port.RedhawkPortStatistics;

/*
 * A redhawk PROVIDES port is an input port and doesn't actually implement CF.Port, so we simulate it here
 */
public class RedhawkPortImpl implements RedhawkPort {
	private static Logger logger = Logger.getLogger(RedhawkPortImpl.class.getName());

	private static final long serialVersionUID = 1L;

	/**
	 * CORBA Object representing port
	 */
	private Object port;

	/**
	 * CORBA Object representing port being connected too
	 */
	private CF.Port remotePort;

	private ORB orb;
	private String connectionId;
	private String repId;
	private String portName;
	private String portType;
	private DataTypeFactory factory;

	// TODO: connectionId should be mapping to a data connection refactor
	// private List<String> connectionIds = new ArrayList<String>();
	// private List<BulkIOData> dataConnections = new ArrayList<BulkIOData>();
	/**
	 * Connections that are managed via the driver.
	 */
	private Map<String, BulkIOData> driverManagedConnections = new HashMap<>();

	private static List<DataTypes> dataTypeList = new ArrayList<DataTypes>();

	static {
		dataTypeList.add(DataTypes.DATA_FLOAT);
		dataTypeList.add(DataTypes.DATA_DOUBLE);
		dataTypeList.add(DataTypes.DATA_CHAR);
		dataTypeList.add(DataTypes.DATA_SHORT);
		dataTypeList.add(DataTypes.DATA_ULONG);
		dataTypeList.add(DataTypes.DATA_LONG);
		dataTypeList.add(DataTypes.DATA_LONGLONG);
		dataTypeList.add(DataTypes.DATA_OCTET);
		dataTypeList.add(DataTypes.DATA_XML);
		dataTypeList.add(DataTypes.DATA_FILE);
		dataTypeList.add(DataTypes.DATA_ULONGLONG);
		dataTypeList.add(DataTypes.DATA_USHORT);
	}

	public RedhawkPortImpl(Object port, ORB orb, String repId, String portName, String portType) {
		this.port = port;
		this.orb = orb;
		this.repId = repId;
		this.portName = portName;
		this.portType = portType;
		factory = new DataTypeFactory(port);
	}

	@Override
	public String getRepId() {
		return repId;
	}

	@Override
	public String getName() {
		return portName;
	}

	@Override
	public org.omg.CORBA.Object getCorbaObject() {
		return port;
	}

	@Override
	public String getType() {
		return portType;
	}

	public <T> void send(Packet<T> packet) throws Exception {

		if (portType.equalsIgnoreCase(RedhawkPort.PORT_TYPE_USES)) {
			throw new PortException("Uses ports do not implement send()");
		}

		boolean endOfDataStream = packet.isEndOfStream();
		String id = packet.streamId;
		PrecisionUTCTime time = packet.getTime();
		T data = packet.getData();

		try {
			switch (factory.getOperationsType()) {
			case DATA_CHAR:
				dataChar charObj = factory.getDataCharObj();
				if (packet.getStreamSri() != null)
					charObj.pushSRI(packet.getStreamSri());

				charObj.pushPacket((char[]) data, time, endOfDataStream, id);
				break;
			case DATA_DOUBLE:
				dataDouble doubleObj = factory.getDataDoubleObj();

				if (packet.getStreamSri() != null)
					doubleObj.pushSRI(packet.getStreamSri());

				doubleObj.pushPacket((double[]) data, time, endOfDataStream, id);
				break;
			case DATA_LONG:
				dataLong longObj = factory.getDataLongObj();
				if (packet.getStreamSri() != null)
					longObj.pushSRI(packet.getStreamSri());

				longObj.pushPacket((int[]) data, time, endOfDataStream, id);
				break;
			case DATA_OCTET:
				dataOctet octObj = factory.getDatOctetObj();
				if (packet.getStreamSri() != null)
					octObj.pushSRI(packet.getStreamSri());

				octObj.pushPacket((byte[]) data, time, endOfDataStream, id);
				break;
			case DATA_SHORT:
				dataShort shortObj = factory.getDataShortObj();
				if (packet.getStreamSri() != null)
					shortObj.pushSRI(packet.getStreamSri());

				shortObj.pushPacket((short[]) data, time, endOfDataStream, id);
				break;
			case DATA_ULONG:
				dataUlong ulongObj = factory.getDataULongObj();
				if (packet.getStreamSri() != null)
					ulongObj.pushSRI(packet.getStreamSri());

				ulongObj.pushPacket((int[]) data, time, endOfDataStream, id);
				break;
			case DATA_USHORT:
				dataUshort ushortObj = factory.getDataUShortObj();
				if (packet.getStreamSri() != null)
					ushortObj.pushSRI(packet.getStreamSri());

				ushortObj.pushPacket((short[]) data, time, endOfDataStream, id);
				break;
			case DATA_XML:
				dataXML xmlObj = factory.getDataXMLObj();
				if (packet.getStreamSri() != null)
					xmlObj.pushSRI(packet.getStreamSri());

				xmlObj.pushPacket((String) data, endOfDataStream, id);
				break;
			case DATA_FILE:
				dataFile fileObj = factory.getDataFileObj();
				if (packet.getStreamSri() != null)
					fileObj.pushSRI(packet.getStreamSri());

				fileObj.pushPacket((String) data, time, endOfDataStream, id);
				break;
			case DATA_FLOAT:
				dataFloat floatObj = factory.getDataFloatObj();
				if (packet.getStreamSri() != null)
					floatObj.pushSRI(packet.getStreamSri());

				floatObj.pushPacket((float[]) data, time, endOfDataStream, id);
				break;
			case DATA_ULONGLONG:
				dataUlongLong uLongLongObj = factory.getDataUlongLongObj();
				if (packet.getStreamSri() != null)
					uLongLongObj.pushSRI(packet.getStreamSri());

				uLongLongObj.pushPacket((long[]) data, time, endOfDataStream, id);
				break;
			case DATA_LONGLONG:
				dataLongLong longLongObj = factory.getDataLongLongObj();
				if (packet.getStreamSri() != null)
					longLongObj.pushSRI(packet.getStreamSri());

				longLongObj.pushPacket((long[]) data, time, endOfDataStream, id);
				break;
			case DATA_SDDS:
				dataSDDS sddsObj = factory.getDataSDDSObj();
				sddsObj.pushSRI(packet.getStreamSri(), packet.getTime());
				break;
			default:
				throw new Exception("This port does not have a push packet method");
			}
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
	}
	
	@Override
	@Deprecated
	public void connect(PortListener<?> portListener) throws Exception {
		this.listen(portListener);
	}
	
	@Override
	public void listen(PortListener<?> portListener) throws Exception {
		if (portType.equalsIgnoreCase(RedhawkPort.PORT_TYPE_PROVIDES)) {
			throw new PortException("Provides ports do not implement connect()");
		}

		logger.fine("IN Connect");
		POA rootPOA = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
		rootPOA.the_POAManager().activate();
		ClassLoader classloader = this.getClass().getClassLoader();

		boolean foundValidPort = false;
		Constructor<?> c = null;
		java.lang.Object poaTie = null;
		Method meth = null;

		// TODO: At this point we should have the info to know exactly which dataType
		// the port is
		// So why do we go through this list...
		for (DataTypes dataType : dataTypeList) {
			c = Class.forName(dataType.poaTieClass, true, classloader)
					.getConstructor(Class.forName(dataType.operationsClass, true, classloader));
			BulkIOData dataConnection = new BulkIOData(portListener);
			poaTie = (java.lang.Object) c.newInstance(dataConnection);
			meth = poaTie.getClass().getSuperclass().getMethod("_this", ORB.class);
			org.omg.CORBA.Object pipeline = (Object) meth.invoke(poaTie, orb);

			String connectionId = "REI_" + UUID.randomUUID().toString();
			try {
				if (portType.equalsIgnoreCase("provides")) {
					throw new UnsupportedOperationException(
							"You cannot connect to an input (provides) port.  Only output (uses) ports are allowed.");
				}
				remotePort = PortHelper.narrow(port);
				remotePort.connectPort(pipeline, connectionId);

				foundValidPort = true;

				driverManagedConnections.put(connectionId, dataConnection);
				break;
			} catch (BAD_PARAM e) {
				logger.fine("PROB with: " + dataType.poaTieClass);
			} catch (InvalidPort p) {
				logger.fine("PROB with: " + dataType.poaTieClass);
			} catch (Throwable t) {
				logger.fine("PROB with: " + dataType.poaTieClass);
			} finally {
				if (!foundValidPort) {
					dataConnection.disconnect();
				}
			}
		}

		if (!foundValidPort) {
			throw new Exception("Could Not Locate a Valid BULKIO Data Type for this Port.");
		}
	}

	@Override
	public void disconnect() throws PortException {
		if (portType.equalsIgnoreCase(RedhawkPort.PORT_TYPE_PROVIDES)) {
			throw new PortException("Provides ports do not implement disconnect()");
		}

		/*
		 * Disconnection any driver managed connections
		 */
		for (String connectionId : driverManagedConnections.keySet()) {
			this.disconnect(connectionId);
		}

		if (remotePort != null) {
			remotePort._release();
		}
	}

	@Override
	public void disconnect(String connectionId) throws PortException {
		if (portType.equalsIgnoreCase(RedhawkPort.PORT_TYPE_PROVIDES)) {
			throw new PortException("Provides ports do not implement disconnect()");
		}

		/*
		 * If it's a driver managed connection make sure to disconnect BulkIOData
		 */
		if (driverManagedConnections.containsKey(connectionId)) {
			BulkIOData data = driverManagedConnections.get(connectionId);
			data.disconnect();
		}

		try {
			if (remotePort == null)
				remotePort = PortHelper.narrow(port);

			remotePort.disconnectPort(connectionId);
		} catch (InvalidPort e) {
			throw new PortException("Error disconnecting from CF.Port", e);
		} finally {
			remotePort._release();
		}
	}

	@Override
	public List<RedhawkPortStatistics> getPortStatistics() {
		List<RedhawkPortStatistics> list = new ArrayList<>();
		if (portType.equalsIgnoreCase("uses")) {
			UsesPortStatisticsProvider stats = UsesPortStatisticsProviderHelper.narrow(this.port);
			for (UsesPortStatistics stat : stats.statistics()) {
				list.add(new RedhawkPortStatistics(stat.connectionId, stat.statistics));
			}
		} else {
			/*
			 * Already narrowed down to actual port type no need to narrow again w/
			 * ProvidesPortStatisticsProviderHelper.
			 * 
			 * Note: By doing this you miss out on state in the stats. But user can still
			 * get state from the actual port object.
			 */
			if (factory.getStatistics() != null)
				list.add(new RedhawkPortStatistics(factory.getStatistics()));
		}

		return list;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RedhawkPortImpl [");
		if (portName != null)
			builder.append("portName=").append(portName).append(", ");
		if (portType != null)
			builder.append("portType=").append(portType).append(", ");
		if (repId != null)
			builder.append("repId=").append(repId).append(", ");
		if (port != null)
			builder.append("interfaces=").append(Arrays.toString(port.getClass().getInterfaces()));
		builder.append("]");
		return builder.toString();
	}
}