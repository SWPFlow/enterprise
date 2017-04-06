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
package redhawk.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import redhawk.driver.devicemanager.RedhawkDeviceManager;
import redhawk.rest.model.FetchMode;
import redhawk.rest.model.TunerMode;
import redhawk.rest.util.NodeBooterProxy;

public class RedhawkManagerTestIT {
	private static RedhawkManager manager; 
	
	private static String sdrRoot = "/var/redhawk/sdr";
	
	private static String deviceManagerHome = sdrRoot+"/dev";
	
	private static Process process;
	
	private static File nodeDir;
	
	private static NodeBooterProxy proxy;
	
	@BeforeClass
	public static void setup() throws IOException, InterruptedException{
		manager = new RedhawkManager(); 
		proxy = new NodeBooterProxy();
	
		File file = new File("../redhawk-driver/src/test/resources/node/SimulatorNode");
		
		nodeDir = new File(deviceManagerHome+"/nodes/SimulatorNode");

		System.out.println(file.exists());
		
		/*
		 * Copy Nodes directory over  
		 */
		FileUtils.copyDirectory(file, nodeDir, FileFilterUtils.suffixFileFilter(".dcd.xml"));						
	}
	
	@Test
	public void TestLazyRetrieval() {
		try{
			manager.getAll("localhost:2809", "domain", null, FetchMode.LAZY);
			assertTrue("Passed test!", true);
		}catch(Exception ex){
			fail("Exception doing lazy retrieval of domain ");
			ex.printStackTrace();
		}
	}
	
	@Test
	public void TestEagerRetrieval(){
		try{
			manager.getAll("localhost:2809", "domain", null, FetchMode.EAGER);
			assertTrue("Passed test!", true);
		}catch(Exception ex){
			ex.printStackTrace();
			fail("Exception doing eager retrieval of domain ");
		}
	}
	
	/*
	 * Below tests work individually but not as a group. There's an 
	 * something wrong with how I'm shutting down the devicemanager. 
	 */
	@Test
	public void testShutdownDeviceManager() throws Exception{
		System.out.println("Hello World");
		/*
		 * Use the NodeBooter proxy to launch your device manager 
		 */		
		process = proxy.launchDeviceManager("/var/redhawk/sdr/dev/nodes/SimulatorNode/DeviceManager.dcd.xml");		
		
		List<RedhawkDeviceManager> managers = manager.getAll("localhost:2809", "devicemanager", "REDHAWK_DEV", FetchMode.LAZY);
		
		assertEquals("Should now be 2 Devicemanagers ", 2, managers.size());
		manager.shutdownDeviceManager("localhost:2809", "REDHAWK_DEV/SimulatorNode");
		
		managers = manager.getAll("localhost:2809", "devicemanager", "REDHAWK_DEV", FetchMode.LAZY);
		assertEquals("Should now be 1 Devicemanagers ", 1, managers.size());		
	}
	
	@Test
	public void testAllocateAndDellocate() throws Exception{
		/*
		 * Use the NodeBooter proxy to launch your device manager 
		 */		
		process = proxy.launchDeviceManager("/var/redhawk/sdr/dev/nodes/SimulatorNode/DeviceManager.dcd.xml");		
		
		String allocationId = "myAllocation";
		
		//Create Allocation Map 
		Map<String, Object> newAlloc = new HashMap<>();
		
		String allocId = "myTestAllocationId";
		newAlloc.put("FRONTEND::tuner_allocation::allocation_id", allocId);
		newAlloc.put("FRONTEND::tuner_allocation::tuner_type", "RX_DIGITIZER");
		newAlloc.put("FRONTEND::tuner_allocation::center_frequency", 101100000d);//101.1e6
		newAlloc.put("FRONTEND::tuner_allocation::sample_rate", 256000d);//256e3
		newAlloc.put("FRONTEND::tuner_allocation::bandwidth_tolerance", 20.0);
		newAlloc.put("FRONTEND::tuner_allocation::sample_rate_tolerance", 20.0);
		
		List<Map<String, Object>> usedTuners = manager.getTuners("localhost:2809", "REDHAWK_DEV/SimulatorNode/FmRdsSimulator.*", TunerMode.USED);		
		List<Map<String, Object>> unusedTuners = manager.getTuners("localhost:2809", "REDHAWK_DEV/SimulatorNode/FmRdsSimulator.*", TunerMode.UNUSED);		
		
		assertEquals("Should be 1 unused tuner ", 1, unusedTuners.size());
		assertEquals("Should be 0 used tuner ", 0, usedTuners.size());
		
		
		manager.allocateDevice("localhost:2809", "REDHAWK_DEV/SimulatorNode/FmRdsSimulator.*", newAlloc);
		
		usedTuners = manager.getTuners("localhost:2809", "REDHAWK_DEV/SimulatorNode/FmRdsSimulator.*", TunerMode.USED);		
		unusedTuners = manager.getTuners("localhost:2809", "REDHAWK_DEV/SimulatorNode/FmRdsSimulator.*", TunerMode.UNUSED);		
		
		assertEquals("Should be 1 used tuner ", 1, usedTuners.size());
		assertEquals("Should be 1 unused tuner ", 0, unusedTuners.size());
		
		manager.deallocateDevice("localhost:2809", "REDHAWK_DEV/SimulatorNode/FmRdsSimulator.*", allocId);

		usedTuners = manager.getTuners("localhost:2809", "REDHAWK_DEV/SimulatorNode/FmRdsSimulator.*", TunerMode.USED);		
		unusedTuners = manager.getTuners("localhost:2809", "REDHAWK_DEV/SimulatorNode/FmRdsSimulator.*", TunerMode.UNUSED);		

		assertEquals("Should be 1 unused tuner ", 1, unusedTuners.size());
		assertEquals("Should be 0 used tuner ", 0, usedTuners.size());
	
		manager.shutdownDeviceManager("localhost:2809", "REDHAWK_DEV/SimulatorNode");
	}
	
	@AfterClass
	public static void cleanup() throws IOException{
		if(process!=null){
			FileUtils.deleteDirectory(nodeDir);
			
			process.destroy();			
		}
	}
}
