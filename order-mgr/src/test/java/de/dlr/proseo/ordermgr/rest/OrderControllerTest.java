package de.dlr.proseo.ordermgr.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.ProcessingOrder.OrderSlicingType;
import de.dlr.proseo.model.ProcessingOrder.OrderState;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.ordermgr.OrderManager;
import de.dlr.proseo.ordermgr.OrdermgrSecurityConfig;
import de.dlr.proseo.ordermgr.rest.model.Orbit;
import de.dlr.proseo.ordermgr.rest.model.Order;
import de.dlr.proseo.ordermgr.rest.model.OrderUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = OrderManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
//@Transactional
@AutoConfigureTestEntityManager
public class OrderControllerTest {
	/* The base URI of the Orders */
	private static String ORDER_BASE_URI = "/proseo/order-mgr/v0.1";

	@LocalServerPort
	private int port;
	
	@Autowired
	EntityManagerFactory emf;
	
	/** Test configuration */
	@Autowired
	OrdermgrTestConfiguration config;
	
	/** The security environment for this test */
	//@Autowired
	OrdermgrSecurityConfig ordermgrSecurityConfig;
	
	/** REST template builder */
	@Autowired
	RestTemplateBuilder rtb;
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(OrderControllerTest.class);
	
	/* Test orders */
	private static String[][] testOrderData = {
		// mission_id, mission_version, mission_code, mission_name,spacecraft_version,spacecraft_code,spacecraft_name, order_id, order_version, execution_time, identifier, order_state, processing_mode,slice_duartion,slice_type,start_time, stop_time
		{ "1", "0", "ABCe", "ABCD Testing", "1","S_TDX1","Tandom-X", "111", "0", "2019-11-17T22:49:21.000000","XYZ","RUNNING","NRTI","30","TIME_SLICE","2019-08-29T22:49:21.000000","2019-08-29T22:49:21.000000"},
		{ "11", "11", "DEFg", "DefrostMission", "2","S_TDX2","Tandem-X", "112", "0", "2019-11-18T20:04:20.000000","ABCDE","PLANNED","OFFL",null,"ORBIT","2019-02-20T22:49:21.000000","2019-05-29T20:29:11.000000"},
		{ "12", "12", "XY1Z", "XYZ Testing", "3","S_TDX3","Terrasar-X",	"113", "0", "2019-10-31T20:49:02.000000","XYZ1234","PLANNED","NRTI",null,"ORBIT","2019-01-02T02:40:21.000000","2019-04-29T18:29:10.000000"}
		
		
	};
	/**
	 * Create an order from a data array
	 * 
	 * @param testData an array of Strings representing the order to create
	 * @return a order with its attributes set to the input data
	 */
	private ProcessingOrder createOrder(String[] testData) {		
		logger.info("... creating order ");
		
		//create TestMission
		de.dlr.proseo.model.Mission testMission = new de.dlr.proseo.model.Mission();
		de.dlr.proseo.model.Spacecraft testSpacecraft = new de.dlr.proseo.model.Spacecraft();
		ProcessingOrder testOrder = new ProcessingOrder();


		if (null != RepositoryService.getMissionRepository().findByCode(testData[2]))
			testMission = RepositoryService.getMissionRepository().findByCode(testData[2]);
		else {
			//Adding mission parameters
			testMission.setId(Long.parseLong(testData[0]));
			testMission.setCode(testData[2]);
			testMission.setName(testData[3]);
			testMission = RepositoryService.getMissionRepository().save(testMission);
			
		}

		if (null != RepositoryService.getSpacecraftRepository().findByCode(testData[5]))
			testSpacecraft = RepositoryService.getSpacecraftRepository().findByCode(testData[5]);
		else {
			//adding Spacecraft parameters
			testSpacecraft.setMission(testMission);
			testSpacecraft.incrementVersion();
			testSpacecraft.setCode(testData[5]);
			testSpacecraft.setName(testData[6]);
			testSpacecraft = RepositoryService.getSpacecraftRepository().save(testSpacecraft);
			
		}

		if (null != RepositoryService.getOrderRepository().findByIdentifier(testData[10])) {
			logger.info("Found test order {}", testOrder.getId());
			return testOrder = RepositoryService.getOrderRepository().findByIdentifier(testData[10]);	
		}
		//Adding processing order parameters
		testOrder.setId(Long.parseLong(testData[7]));
		testOrder.setExecutionTime(Instant.from(de.dlr.proseo.model.Orbit.orbitTimeFormatter.parse(testData[9])));
		testOrder.setIdentifier(testData[10]);
		testOrder.setOrderState(OrderState.valueOf(testData[11]));
		testOrder.setProcessingMode(testData[10]);
//		testOrder.setSliceDuration(testData[13]);
//		testOrder.setSlicingType(OrderSlicingType.valueOf(testData[14]));
		testOrder.setStartTime(Instant.from(de.dlr.proseo.model.Orbit.orbitTimeFormatter.parse(testData[15])));
		testOrder.setStopTime(Instant.from(de.dlr.proseo.model.Orbit.orbitTimeFormatter.parse(testData[16])));
		testOrder.setMission(testMission);
		
		testOrder = RepositoryService.getOrderRepository().save(testOrder);
		
		logger.info("Created test order {}", testOrder.getId());
		return testOrder;
	}
	
	/**
	 * Create test orders in the database
	 * 
	 * @return a list of orders generated
	 */
	
	private List<ProcessingOrder> createTestOrders() {
		logger.info("Creating test order");
		List<ProcessingOrder> testOrders = new ArrayList<>();		
		logger.info("Creating test orders of length: "+  testOrderData.length);

		for (int i = 0; i < testOrderData.length; ++i) {
			logger.info("Creating test order: "+ i +" "+ testOrderData[i][10]);

			testOrders.add(createOrder(testOrderData[i]));
		}
		return testOrders;
	}
	
	/**
	 * Remove all (remaining) test orders
	 * 
	 * @param testOrders a list of test orders to delete 
	 */
	private void deleteTestOrders(List<ProcessingOrder> testOrders) {
		Session session = emf.unwrap(SessionFactory.class).openSession();
		for (ProcessingOrder testOrder: testOrders) {
			testOrder = (ProcessingOrder) session.merge(testOrder);
			de.dlr.proseo.model.Mission mission = testOrder.getMission();
			Set<de.dlr.proseo.model.Spacecraft> spacecrafts = testOrder.getMission().getSpacecrafts();
			
			RepositoryService.getOrderRepository().delete(testOrder);

			for(de.dlr.proseo.model.Spacecraft spacecraft : spacecrafts) {
				RepositoryService.getSpacecraftRepository().delete(spacecraft);
			}

			RepositoryService.getMissionRepository().deleteById(mission.getId());

		}
	}
	
	/**
	 * Test method for {@link de.dlr.proseo.ordermgr.rest.OrderControllerImpl.createOrder(Order)}.
	 * 
	 * Test: Create a new order
	 */
	@Test
	public final void testCreateOrder() {
		// Create an order in the database
		ProcessingOrder orderToCreate = createOrder(testOrderData[1]);
		Order restOrder = OrderUtil.toRestOrder(orderToCreate);

		String testUrl = "http://localhost:" + this.port + ORDER_BASE_URI + "/orders";
		logger.info("Testing URL {} / POST", testUrl);
		
		ResponseEntity<Order> postEntity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.postForEntity(testUrl, restOrder, Order.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.CREATED, postEntity.getStatusCode());
		restOrder = postEntity.getBody();

		assertNotEquals("Id should not be 0 (zero): ", 0L, restOrder.getId().longValue());

		// Test that the mission exists
		testUrl += "/" + restOrder.getId();
		ResponseEntity<Order> getEntity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity(testUrl, Order.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, getEntity.getStatusCode());
	
		// Clean up database
		ArrayList<ProcessingOrder> testOrder = new ArrayList<>();
		testOrder.add(orderToCreate);
		deleteTestOrders(testOrder);

		logger.info("Test OK: Create order");		
	}	

	
	/**
	 * Test method for { @link de.dlr.proseo.ordermgr.rest.OrderControllerImpl.deleteOrderById(Long)}.
	 * 
	 * Test: Delete an Order by ID
	 * Precondition: An Order in the database
	 */
/*	@Test
	public final void testDeleteOrderById() {
		// Make sure test orders exist
		List<ProcessingOrder> testOrders = createTestOrders();
		ProcessingOrder orderToDelete = testOrders.get(0);
		testOrders.remove(0);
		
		// Delete the first test order
		String testUrl = "http://localhost:" + this.port + ORDER_BASE_URI + "/orders/" + orderToDelete.getId();
		logger.info("Testing URL {} / DELETE", testUrl);
		
		new TestRestTemplate(config.getUserName(), config.getUserPassword()).delete(testUrl);
		
		// Test that the order is gone
		ResponseEntity<Order> entity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity(testUrl, Order.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.NOT_FOUND, entity.getStatusCode());
		
		// Clean up database
		//deleteTestOrders(testOrders);

		logger.info("Test OK: Delete Order By ID");
	}
	
	/**
	 * Test method for {@link de.dlr.proseo.ordermgr.rest.OrderControllerImpl.getOrderById(Long)}.
	 * 
	 * Test: Get an Order by ID
	 * Precondition: At least one order with a known ID is in the database
	 */
/*	@Test
	public final void testGetOrderById() {
		// Make sure test orbits exist
		List<ProcessingOrder> testOrders = createTestOrders();
		ProcessingOrder orderToFind = testOrders.get(0);

		// Test that a order can be read
		String testUrl = "http://localhost:" + this.port + ORDER_BASE_URI + "/orders/" + orderToFind.getId();
		logger.info("Testing URL {} / GET", testUrl);

		ResponseEntity<Order> getEntity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity(testUrl, Order.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, getEntity.getStatusCode());
		assertEquals("Wrong orbit ID: ", orderToFind.getId(), getEntity.getBody().getId().longValue());
		
		// Clean up database
		deleteTestOrders(testOrders);

		logger.info("Test OK: Get Order By ID");
	}
*/	

}
