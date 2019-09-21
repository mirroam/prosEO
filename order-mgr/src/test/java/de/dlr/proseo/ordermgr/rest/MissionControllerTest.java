package de.dlr.proseo.ordermgr.rest;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.ordermgr.OrdermgrSecurityConfig;
import de.dlr.proseo.ordermgr.rest.model.Mission;
import de.dlr.proseo.ordermgr.rest.model.MissionUtil;

public class MissionControllerTest {
	
	/* The base URI of the Missions */
	private static String MISSION_BASE_URI = "/proseo/order-mgr/v0.1";

	@LocalServerPort
	private int port;
	
	/** Test configuration */
	@Autowired
	OrdermgrTestConfiguration config;
	
	/** The security environment for this test */
	//@Autowired
	OrdermgrSecurityConfig ordermgrSecurityConfig;
	
	/** REST template builder */
	@Autowired
	RestTemplateBuilder rtb;
	
	private static Logger logger = LoggerFactory.getLogger(MissionControllerTest.class);
	
	/* Test missions */
	private static String[][] testMissionData = {
		// id, version, mission code, name
		{ "0", "1", "ABCe", "ABCD Testing"},
		{ "0", "1", "DEFg", "DefrostMission"},
		{ "0", "1", "XY1Z", "XYZ Testing" }
	};
	
	private de.dlr.proseo.model.Mission createMission(String[] testData) {
		de.dlr.proseo.model.Mission testMission = new de.dlr.proseo.model.Mission();
		
		logger.info("... creating mission ");
		testMission.setCode(testData[2]);
		testMission.setName(testData[3]);
		
		testMission = RepositoryService.getMissionRepository().save(testMission);
		
		logger.info("Created test mission {}", testMission.getId());
		return testMission;
	}
	
	private List<de.dlr.proseo.model.Mission> createTestMissions() {
		logger.info("Creating test missions");
		List<de.dlr.proseo.model.Mission> testMissions = new ArrayList<>();
		for (int i = 0; i < testMissionData.length; ++i) {
			testMissions.add(createMission(testMissionData[i]));
		}
		return testMissions;
	}
	
	private void deleteTestMissions(List<de.dlr.proseo.model.Mission> testMissions) {
		for (de.dlr.proseo.model.Mission testMission: testMissions) {
			RepositoryService.getMissionRepository().delete(testMission);
		}
	}
	@Test
	public final void testCreateMission() {
		// Create a mission in the database
		de.dlr.proseo.model.Mission missionToCreate = createMission(testMissionData[0]);
		Mission restMission = MissionUtil.toRestMission(missionToCreate);

		String testUrl = "http://localhost:" + this.port + MISSION_BASE_URI + "/missions";
		logger.info("Testing URL {} / POST", testUrl);
		
		ResponseEntity<Mission> postEntity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.postForEntity(testUrl, restMission, Mission.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.CREATED, postEntity.getStatusCode());
		restMission = postEntity.getBody();
		assertNotEquals("Id should not be 0 (zero): ", 0L, restMission.getId().longValue());
		
		// Test that the mission exists
		testUrl += "/" + restMission.getId();
		ResponseEntity<Mission> getEntity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity(testUrl, Mission.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, getEntity.getStatusCode());
		
		// Test that the Production Planner was informed
		// TODO Using mock production planner
		
		// Clean up database
		ArrayList<de.dlr.proseo.model.Mission> testMission = new ArrayList<>();
		testMission.add(missionToCreate);
		deleteTestMissions(testMission);

		logger.info("Test OK: Create mission");		
	}	
	
	public final void testGetMissions() {
		// Make sure test missions exist
		List<de.dlr.proseo.model.Mission> testMissions = createTestMissions();
		
		// Get missions using different selection criteria (also combined)
		String testUrl = "http://localhost:" + this.port + MISSION_BASE_URI + "/missions";
		logger.info("Testing URL {} / GET, no params, with user {} and password {}", testUrl, config.getUserName(), config.getUserPassword());

		RestTemplate restTemplate = rtb.basicAuthentication(config.getUserName(), config.getUserPassword()).build();
		@SuppressWarnings("rawtypes")
		ResponseEntity<List> entity = restTemplate.getForEntity(testUrl, List.class);
		
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, entity.getStatusCode());
		
		// Test that the correct missions provided above are in the results
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> body = entity.getBody();
		logger.info("Found {} missions", body.size());
		
		boolean[] missionFound = new boolean[testMissions.size()];
		Arrays.fill(missionFound, false);
		for (Map<String, Object> mission: body) {
			// Check, if any of the test missions was returned
			long missionId = (Integer) mission.get("id");
			logger.info("... found mission with ID {}", missionId);
			for (int i = 0; i < testMissions.size(); ++i) {
				de.dlr.proseo.model.Mission testMission = testMissions.get(i);
				if (missionId == testMission.getId()) {
					missionFound[i] = true;
					assertEquals("Wrong code for test mission " + i, testMission.getCode(), mission.get("code"));
					assertEquals("Wrong name for test mission " + i, testMission.getName(), mission.get("name"));

				}
			}
		}
		boolean[] expectedMissionFound = new boolean[testMissions.size()];
		Arrays.fill(expectedMissionFound, true);
		assertArrayEquals("Not all missions found", expectedMissionFound, missionFound);
		
		// TODO Tests with different selection criteria
		
		// Clean up database
		deleteTestMissions(testMissions);

		logger.info("Test OK: Get Missions");
	}
	
	public final void testGetMissionById() {
		// Make sure test missions exist
		List<de.dlr.proseo.model.Mission> testMissions = createTestMissions();
		de.dlr.proseo.model.Mission missionToFind = testMissions.get(0);

		// Test that a mission can be read
		String testUrl = "http://localhost:" + this.port + MISSION_BASE_URI + "/missions/" + missionToFind.getId();
		logger.info("Testing URL {} / GET", testUrl);

		ResponseEntity<Mission> getEntity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity(testUrl, Mission.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, getEntity.getStatusCode());
		assertEquals("Wrong mission ID: ", missionToFind.getId(), getEntity.getBody().getId().longValue());
		
		// Clean up database
		deleteTestMissions(testMissions);

		logger.info("Test OK: Get Mission By ID");
	}
	
	@Test
	public final void testDeleteMissionById() {
		// Make sure test missions exist
		List<de.dlr.proseo.model.Mission> testMissions = createTestMissions();
		de.dlr.proseo.model.Mission missionToDelete = testMissions.get(0);
		testMissions.remove(0);
		
		// Delete the first test mission
		String testUrl = "http://localhost:" + this.port + MISSION_BASE_URI + "/missions/" + missionToDelete.getId();
		logger.info("Testing URL {} / DELETE", testUrl);
		
		new TestRestTemplate(config.getUserName(), config.getUserPassword()).delete(testUrl);
		
		// Test that the mission is gone
		ResponseEntity<Mission> entity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity(testUrl, Mission.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.NOT_FOUND, entity.getStatusCode());
		
		// Clean up database
		deleteTestMissions(testMissions);

		logger.info("Test OK: Delete Mission By ID");
	}
	
	public final void testModifyMission() {
		// Make sure test missions exist
		List<de.dlr.proseo.model.Mission> testMissions = createTestMissions();
		de.dlr.proseo.model.Mission missionToModify = testMissions.get(0);
		
		// Update a mission attribute
		missionToModify.setCode("MOD Code");

		Mission restMission = MissionUtil.toRestMission(missionToModify);
		
		String testUrl = "http://localhost:" + this.port + MISSION_BASE_URI + "/missions/" + missionToModify.getId();
		logger.info("Testing URL {} / PATCH", testUrl);

		restMission = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.patchForObject(testUrl, restMission, Mission.class);
		assertNotNull("Modified mission not set", restMission);
		
		// Test that the mission attribute was changed as expected
		ResponseEntity<Mission> getEntity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity(testUrl, Mission.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, getEntity.getStatusCode());
		assertEquals("Wrong Code: ", missionToModify.getCode(), getEntity.getBody().getCode());
		
		// Clean up database
		deleteTestMissions(testMissions);

		logger.info("Test OK: Modify mission");
	}



}
