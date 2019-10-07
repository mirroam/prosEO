/**
 * RepositoryService.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.ProductQuery;
import de.dlr.proseo.model.dao.ConfiguredProcessorRepository;
import de.dlr.proseo.model.dao.FacilityRepository;
import de.dlr.proseo.model.dao.JobRepository;
import de.dlr.proseo.model.dao.JobStepRepository;
import de.dlr.proseo.model.dao.MissionRepository;
import de.dlr.proseo.model.dao.OrbitRepository;
import de.dlr.proseo.model.dao.OrderRepository;
import de.dlr.proseo.model.dao.ParameterRepository;
import de.dlr.proseo.model.dao.ProcessorClassRepository;
import de.dlr.proseo.model.dao.ProcessorRepository;
import de.dlr.proseo.model.dao.ProductClassRepository;
import de.dlr.proseo.model.dao.ProductFileRepository;
import de.dlr.proseo.model.dao.ProductQueryRepository;
import de.dlr.proseo.model.dao.ProductRepository;
import de.dlr.proseo.model.dao.SpacecraftRepository;

/**
 * This class autowires all available repositories and makes them accessible throughout prosEO by static methods.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Service
public class RepositoryService {
	/** The single instance of this class */
	private static RepositoryService theRepositoryService = null;

	/** The repository for the ConfiguredProcessor class */
	@Autowired
    private ConfiguredProcessorRepository configuredProcessorRepository;
	
	/** The repository for the Facility class */
	@Autowired
    private FacilityRepository facilityRepository;
	
	/** The repository for the Job class */
	@Autowired
    private JobRepository jobRepository;
	
	/** The repository for the JobStep class */
	@Autowired
    private JobStepRepository jobStepRepository;
	
	/** The repository for the Mission class */
	@Autowired
    private MissionRepository missionRepository;
	
	/** The repository for the Orbit class */
	@Autowired
    private OrbitRepository orbitRepository;
	
	/** The repository for the ProcessingOrder class */
	@Autowired
    private OrderRepository orderRepository;
	
	/** The repository for the ProcessorClass class */
	@Autowired
    private ProcessorClassRepository processorClassRepository;
	
	/** The repository for the Processor class */
	@Autowired
    private ProcessorRepository processorRepository;
	
	/** The repository for the ProductClass class */
	@Autowired
    private ProductClassRepository productClassRepository;
	
	/** The repository for the ProductFile class */
	@Autowired
    private ProductFileRepository productFileRepository;
	
	/** The repository for the ProductQuery class */
	@Autowired
    private ProductQueryRepository productQueryRepository;
	
	/** The repository for the Product class */
	@Autowired
    private ProductRepository productRepository;

	/** The repository for the Spacecraft class */
	@Autowired
    private SpacecraftRepository spacecraftRepository;

	/** The repository for the ProductQuery class */
	@Autowired
    private ProductQueryRepository productQueryRepository;

	/** The repository for the ProductFile class */
	@Autowired
    private ProductFileRepository productFileRepository;

	/** The repository for the Parameter class */
	@Autowired
    private ParameterRepository parameterRepository;

	/**
	 * Singleton constructor
	 */
	public RepositoryService() {
		super();
		theRepositoryService = this;
	}

	/**
	 * Gets the repository for the ConfiguredProcessor class
	 * 
	 * @return the configuredProcessorRepository
	 */
	public static ConfiguredProcessorRepository getConfiguredProcessorRepository() {
		return theRepositoryService.configuredProcessorRepository;
	}

	/**
	 * Gets the repository for the Facility class
	 * 
	 * @return the facilityRepository
	 */
	public static FacilityRepository getFacilityRepository() {
		return theRepositoryService.facilityRepository;
	}

	/**
	 * Gets the repository for the Job class
	 * 
	 * @return the jobRepository
	 */
	public static JobRepository getJobRepository() {
		return theRepositoryService.jobRepository;
	}

	/**
	 * Gets the repository for the JobStep class
	 * 
	 * @return the jobStepRepository
	 */
	public static JobStepRepository getJobStepRepository() {
		return theRepositoryService.jobStepRepository;
	}

	/**
	 * Gets the repository for the Mission class
	 * 
	 * @return the missionRepository
	 */
	public static MissionRepository getMissionRepository() {
		return theRepositoryService.missionRepository;
	}

	/**
	 * Gets the repository for the Orbit class
	 * 
	 * @return the orbitRepository
	 */
	public static OrbitRepository getOrbitRepository() {
		return theRepositoryService.orbitRepository;
	}

	/**
	 * Gets the repository for the ProcessingOrder class
	 * 
	 * @return the orderRepository
	 */
	public static OrderRepository getOrderRepository() {
		return theRepositoryService.orderRepository;
	}

	/**
	 * Gets the repository for the ProcessorClass class
	 * 
	 * @return the processorClassRepository
	 */
	public static ProcessorClassRepository getProcessorClassRepository() {
		return theRepositoryService.processorClassRepository;
	}

	/**
	 * Gets the repository for the Processor class
	 * 
	 * @return the processorRepository
	 */
	public static ProcessorRepository getProcessorRepository() {
		return theRepositoryService.processorRepository;
	}

	/**
	 * Gets the repository for the ProductClass class
	 * 
	 * @return the productClassRepository
	 */
	public static ProductClassRepository getProductClassRepository() {
		return theRepositoryService.productClassRepository;
	}

	/**
	 * Gets the repository for the ProductFile class
	 * 
	 * @return the productFileRepository
	 */
	public static ProductFileRepository getProductFileRepository() {
		return theRepositoryService.productFileRepository;
	}

	/**
	 * Gets the repository for the ProductQuery class
	 * 
	 * @return the productQueryRepository
	 */
	public static ProductQueryRepository getProductQueryRepository() {
		return theRepositoryService.productQueryRepository;
	}

	/**
	 * Gets the repository for the Product class
	 * 
	 * @return the productRepository
	 */
	public static ProductRepository getProductRepository() {
		return theRepositoryService.productRepository;
	}

	/**
	 * Gets the repository for the Spacecraft class
	 * 
	 * @return the spacecraftRepository
	 */
	public static SpacecraftRepository getSpacecraftRepository() {
		return theRepositoryService.spacecraftRepository;
	}
	
	/**
	 * Gets the repository for the ProductQuery class
	 * 
	 * @return the productQueryRepository
	 */
	public static ProductQueryRepository getProductQueryRepository() {
		return theRepositoryService.productQueryRepository;
	}
	
	/**
	 * Gets the repository for the ProductFile class
	 * 
	 * @return the productFileRepository
	 */
	public static ProductFileRepository getProductFileRepository() {
		return theRepositoryService.productFileRepository;
	}
	
	/**
	 * Gets the repository for the Parameter class
	 * 
	 * @return the parameterRepository
	 */
	public static ParameterRepository getParameterRepository() {
		return theRepositoryService.parameterRepository;
	}
}
