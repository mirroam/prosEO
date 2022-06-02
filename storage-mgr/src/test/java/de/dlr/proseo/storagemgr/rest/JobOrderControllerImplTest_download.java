package de.dlr.proseo.storagemgr.rest;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import de.dlr.proseo.storagemgr.StorageManager;
import de.dlr.proseo.storagemgr.StorageTestUtils;
import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.UniqueStorageTestPaths;
import de.dlr.proseo.storagemgr.version2.PathConverter;
import de.dlr.proseo.storagemgr.version2.StorageProvider;
import de.dlr.proseo.storagemgr.version2.model.StorageType;

/**
 * Mock Mvc test for Product Controller
 * 
 * @author Denys Chaykovskiy
 * 
 */
/**
 * @throws Exception
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class JobOrderControllerImplTest_download {

	@Autowired
	private MockMvc mockMvc;
	
	@Autowired
	private StorageTestUtils storageTestUtils;
	
 	@Autowired
	private StorageProvider storageProvider;

	@Rule
	public TestName testName = new TestName();
	
	private static final String REQUEST_STRING = "/proseo/storage-mgr/x/joborders";

	/**
	 * Download prosEO Job Order File as Base64-encoded string
	 * 
	 * GET /joborders pathInfo="/.."
	 * 
	 * @return String
	 */

	@Test
	public void testDownload_v2Posix() throws Exception {
		
		// StorageProvider storageProvider = new StorageProvider();
		StorageType storageType = StorageType.POSIX; 
		storageProvider.loadVersion2();
		storageProvider.setStorage(storageType);
	
		downloadRestJobOrder();
		
		assertTrue("Expected: SM Version2, " + " Exists: 1", storageProvider.isVersion2());
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM POSIX, " + " Exists: " + realStorageType, storageType == realStorageType);
	}

	@Test
	public void testDownload_v1Posix() throws Exception {
		
		//StorageProvider storageProvider = new StorageProvider();
		StorageType storageType = StorageType.POSIX;
		storageProvider.loadVersion1();
		storageProvider.setStorage(storageType);
		
		downloadRestJobOrder();
		
		assertTrue("Expected: SM Version1, " + " Exists: 2", !storageProvider.isVersion2());
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM POSIX, " + " Exists: " + realStorageType, storageType == realStorageType);

	}
	
	private void downloadRestJobOrder() throws Exception {

		TestUtils.printMethodName(this, testName);
		UniqueStorageTestPaths uniquePaths = new UniqueStorageTestPaths(this, testName); 
		
		String fileName = "file.txt";
		String relativePath = new PathConverter(uniquePaths.getUniqueTestFolder(), fileName).getPath();

		String pathInfo = storageTestUtils.createSourceFile(relativePath);

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(REQUEST_STRING).param("pathInfo", pathInfo);

		MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

		System.out.println("REQUEST: " + REQUEST_STRING);
		System.out.println("Status: " + mvcResult.getResponse().getStatus());
		System.out.println("Content: " + mvcResult.getResponse().getContentAsString());
		
		uniquePaths.deleteUniqueTestDirectories();
	}
}
