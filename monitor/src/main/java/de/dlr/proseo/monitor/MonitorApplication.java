/**
 * Ingestor.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import de.dlr.proseo.monitor.kpi.MonitorKpi01Timeliness;
import de.dlr.proseo.monitor.kpi.MonitorKpi02Completeness;
import de.dlr.proseo.monitor.microservice.MonitorServices;
import de.dlr.proseo.monitor.order.MonitorOrders;
import de.dlr.proseo.monitor.product.MonitorProducts;
import de.dlr.proseo.monitor.rawdata.MonitorRawdatas;

/*
 * prosEO Planner application
 * 
 * @author Ernst Melchinger
 * 
 */
@SpringBootApplication
@EnableConfigurationProperties
@ComponentScan(basePackages={"de.dlr.proseo"})
@EnableJpaRepositories("de.dlr.proseo.model.dao")
public class MonitorApplication implements CommandLineRunner {
	
	private static Logger logger = LoggerFactory.getLogger(MonitorApplication.class);
	
	/**
	 * Some constant definition for public use.
	 */
	public static final String jobNamePrefix = "proseojob";
	public static final String jobContainerPrefix = "proseocont";

	public static String hostName = "localhost";
	public static String hostIP = "127.0.0.1";
	public static String port = "8080";
	
	public static MonitorConfiguration config;
	
	public static RestTemplateBuilder rtb;

	/** REST template builder */
	@Autowired
	private RestTemplateBuilder rtba;

	/** 
	 * MonitorServices configuration 
	 */
	@Autowired
	MonitorConfiguration monitorConfig;
    /**
     * Job step util
     */
	/** Transaction manager for transaction control */
	@Autowired
	private PlatformTransactionManager txManager;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	private MonitorServices monServices = null;
	private MonitorOrders monOrders = null;
	private MonitorProducts monProducts = null;
	private MonitorRawdatas monRawdatas = null;
	private MonitorKpi01Timeliness monKpi01Timeliness = null;
	private MonitorKpi02Completeness monKpi02Completeness = null;
	
	/**
	 * Initialize and run application 
	 * 
	 * @param args command line arguments
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception { 
		SpringApplication spa = new SpringApplication(MonitorApplication.class);
		spa.run(args);
	}


	/**
	 * Start the kube dispatcher thread
	 */
	public void startMonitorServices() {
		if (logger.isTraceEnabled()) logger.trace(">>> startMonitorServices()");
		
		if (monServices == null || !monServices.isAlive()) {
			monServices = new MonitorServices(monitorConfig);
			monServices.start();
		} else {
			if (monServices.isInterrupted()) {
				// kubeDispatcher
			}
		}
	}

	/**
	 * Stop the kube dispatcher thread
	 */
	public void stopMonitorServices() {
		if (logger.isTraceEnabled()) logger.trace(">>> stopMonitorServices()");
		
		if (monServices != null && monServices.isAlive()) {
			monServices.interrupt();
			int i = 0;
			while (monServices.isAlive() && i < 100) {
				try {
					wait(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		monServices = null;
	}
	/**
	 * Start the kube dispatcher thread
	 */
	@Transactional
	public void startMonitorOrders() {
		if (logger.isTraceEnabled()) logger.trace(">>> startMonitorOrders()");
		
		if (monOrders == null || !monOrders.isAlive()) {
			monOrders = new MonitorOrders(monitorConfig, txManager);
			monOrders.start();
		} else {
			if (monOrders.isInterrupted()) {
				// kubeDispatcher
			}
		}
	}

	/**
	 * Stop the kube dispatcher thread
	 */
	public void stopMonitorOrders() {
		if (logger.isTraceEnabled()) logger.trace(">>> stopMonitorOrders()");
		
		if (monOrders != null && monOrders.isAlive()) {
			monOrders.interrupt();
			int i = 0;
			while (monOrders.isAlive() && i < 100) {
				try {
					wait(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		monOrders = null;
	}
	/**
	 * Start the kube dispatcher thread
	 */
	@Transactional
	public void startMonitorProducts() {
		if (logger.isTraceEnabled()) logger.trace(">>> startMonitorOrders()");
		
		if (monProducts == null || !monProducts.isAlive()) {
			monProducts = new MonitorProducts(monitorConfig, txManager);
			monProducts.start();
		} else {
			if (monProducts.isInterrupted()) {
				// kubeDispatcher
			}
		}
	}

	/**
	 * Stop the kube dispatcher thread
	 */
	public void stopMonitorProducts() {
		if (logger.isTraceEnabled()) logger.trace(">>> stopMonitorOrders()");
		
		if (monProducts != null && monProducts.isAlive()) {
			monProducts.interrupt();
			int i = 0;
			while (monProducts.isAlive() && i < 100) {
				try {
					wait(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		monOrders = null;
	}
	/**
	 * Start the kube dispatcher thread
	 */
	@Transactional
	public void startMonitorRawdatas() {
		if (logger.isTraceEnabled()) logger.trace(">>> startMonitorOrders()");
		
		if (monRawdatas == null || !monRawdatas.isAlive()) {
			monRawdatas = new MonitorRawdatas(monitorConfig, txManager);
			monRawdatas.start();
		} else {
			if (monRawdatas.isInterrupted()) {
				// kubeDispatcher
			}
		}
	}

	/**
	 * Stop the kube dispatcher thread
	 */
	public void stopMonitorRawdatas() {
		if (logger.isTraceEnabled()) logger.trace(">>> stopMonitorOrders()");
		
		if (monRawdatas != null && monRawdatas.isAlive()) {
			monRawdatas.interrupt();
			int i = 0;
			while (monRawdatas.isAlive() && i < 100) {
				try {
					wait(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		monRawdatas = null;
	}
	/**
	 * Start the timeliness monitor
	 */
	@Transactional
	public void startMonitorKpi01Timeliness() {
		if (logger.isTraceEnabled()) logger.trace(">>> startMonitorKpi01Timeliness()");
		
		if (monKpi01Timeliness == null || !monKpi01Timeliness.isAlive()) {
			monKpi01Timeliness = new MonitorKpi01Timeliness(monitorConfig, txManager, em);
			monKpi01Timeliness.start();
		} else {
			if (monKpi01Timeliness.isInterrupted()) {
				// kubeDispatcher
			}
		}
	}

	/**
	 * Stop the timeliness monitor
	 */
	public void stopMonitorKpi01Timeliness() {
		if (logger.isTraceEnabled()) logger.trace(">>> stopMonitorKpi01Timeliness()");
		
		if (monKpi01Timeliness != null && monKpi01Timeliness.isAlive()) {
			monKpi01Timeliness.interrupt();
			int i = 0;
			while (monKpi01Timeliness.isAlive() && i < 100) {
				try {
					wait(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		monKpi01Timeliness = null;
	}
	/**
	 * Start the timeliness monitor
	 */
	@Transactional
	public void startMonitorKpi02Completeness() {
		if (logger.isTraceEnabled()) logger.trace(">>> startMonitorKpi02Completeness()");
		
		if (monKpi02Completeness == null || !monKpi02Completeness.isAlive()) {
			monKpi02Completeness = new MonitorKpi02Completeness(monitorConfig, txManager, em);
			monKpi02Completeness.start();
		} else {
			if (monKpi02Completeness.isInterrupted()) {
				// kubeDispatcher
			}
		}
	}

	/**
	 * Stop the timeliness monitor
	 */
	public void stopMonitorKpi02Completeness() {
		if (logger.isTraceEnabled()) logger.trace(">>> stopMonitorKpi02Completeness()");
		
		if (monKpi02Completeness != null && monKpi02Completeness.isAlive()) {
			monKpi02Completeness.interrupt();
			int i = 0;
			while (monKpi02Completeness.isAlive() && i < 100) {
				try {
					wait(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		monKpi02Completeness = null;
	}
	/* (non-Javadoc)
	 * @see org.springframework.boot.CommandLineRunner#run(java.lang.String[])
	 */
	@Override
	public void run(String... arg0) throws Exception {
		config = monitorConfig;
		rtb = rtba;
		
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);

		try {
			String dummy = transactionTemplate.execute((status) -> {
				
				return null;
			});
		} catch (TransactionException e) {
			e.printStackTrace();
		}
		
		startMonitorKpi01Timeliness();		
		startMonitorKpi02Completeness();
		startMonitorServices();
		startMonitorOrders();	
		startMonitorProducts();	
		startMonitorRawdatas();			
	}

	
}