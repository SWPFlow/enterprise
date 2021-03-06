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
package redhawk.testutils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import redhawk.driver.RedhawkDriver;
import redhawk.driver.application.RedhawkApplication;
import redhawk.driver.domain.RedhawkFileManager;
import redhawk.driver.exceptions.ApplicationReleaseException;
import redhawk.driver.exceptions.CORBAException;
import redhawk.driver.exceptions.ConnectionException;
import redhawk.driver.exceptions.MultipleResourceException;
import redhawk.driver.exceptions.ResourceNotFoundException;

/**
 * Base test class giving you access to a RedhawkDriver that you 
 * can configure using to use jacorb by setting the system property
 * -Djacorb=true
 * 
 * A NodeBooterProxy is also available from this class giving you access to running 
 * nodeBooter commands from your test class. 
 */
public class RedhawkTestBase {
	public static Logger logger = Logger.getLogger(RedhawkTestBase.class.getName());
	
	public static RedhawkDriver driver; 
	
	public static NodeBooterProxy proxy;
	
	public static String sdrRoot = "/var/redhawk/sdr";
	
	public static String deviceManagerHome = sdrRoot+"/dev";
	
	public static String domainName = "REDHAWK_DEV";
	
	public static String domainHost = "127.0.0.1"; 
	
	public static Integer domainPort = 2809;
	
	public static String nameServer = domainHost+":"+domainPort;
	
	static String propFileLocation = "src/test/resources/test.properties";
	
	public RedhawkTestBase(){
		this.setupB4Class();
	}
	
	@BeforeClass
	public static void setupB4Class(){
		logger.info("Jacorb prop is: "+System.getProperty("jacorb"));
		Boolean jacorbTest = Boolean.parseBoolean(System.getProperty("jacorb", "false"));
		
		Properties buildProps = new Properties();
		try {
			String newPropLocation = System.getProperty("testProps");
			if(newPropLocation!=null){
				propFileLocation=newPropLocation;
				logger.info("READING Properties from: "+propFileLocation);
				buildProps.load(new FileInputStream(propFileLocation));
				logger.info("Loaded properties");
				domainName = buildProps.getProperty("domainName");
				domainHost = buildProps.getProperty("domainHost");
				domainPort = Integer.parseInt(buildProps.getProperty("domainPort"));
				nameServer = domainHost+":"+domainPort;
			}
			
			//Add ability to override domainHost 
			domainHost = System.getProperty("domainHost", domainHost);
			
			logger.info("Domain name: "+domainName+" Host: "+domainHost+" Port: "+domainPort);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(jacorbTest){
			logger.info("Testing with jacorb");
			
			//TODO: Add a way to pass in properties
			Properties props = new Properties(); 
			props.put("org.omg.CORBA.ORBClass", "org.jacorb.orb.ORB");
			props.put("org.omg.CORBA.ORBSingletonClass", "org.jacorb.orb.ORBSingleton");
			driver = new RedhawkDriver(domainHost, domainPort, props);
		}else{
			logger.info("Testing with default orb for JDK");			
			driver = new RedhawkDriver(domainHost, domainPort); 
		}
		
		//Create proxy utility 
		proxy = new NodeBooterProxy();
	}
	
	@AfterClass
	public static void afterClass() throws MultipleResourceException, CORBAException, ApplicationReleaseException, ResourceNotFoundException{
		//Always make sure you delete waveforms you create
		//TODO: Clean up this logic
		try {
			//TODO: Just get rid of everything that's not under RH
			RedhawkFileManager manager = driver.getDomain(domainName).getFileManager();
			manager.removeDirectory("/waveforms/testWaveform");
		} catch (ConnectionException | IOException | CORBAException | ResourceNotFoundException e) {
			logger.info("Unable to delete wavemform likely cause it doesn't exist.");
		}
		
		if(driver!=null){
			for(RedhawkApplication application : driver.getDomain(domainName).getApplications()){
				//Clean up applications
				application.release();
			}
			driver.disconnect();
		}
	}
	
	
}
