/**
 * OrderUtil.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.util;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.enums.ProductionType;
import de.dlr.proseo.model.Job.JobState;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.Messages;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.dispatcher.OrderDispatcher;

/**
 * Handle processing orders
 * 
 * @author Ernst Melchinger
 *
 */
@Component
@Transactional
public class OrderUtil {
	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(OrderUtil.class);

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;
	
    /**
     * The job utility instance
     */
    @Autowired
    private JobUtil jobUtil;

    /**
     * The order dispatcher instance
     */
    @Autowired
    private OrderDispatcher orderDispatcher;

    /** The Production Planner instance */
    @Autowired
    private ProductionPlanner productionPlanner;


	/**
	 * Cancel the processing order and it jobs and job steps.
	 * 
	 * @param order The processing Order
	 * @return Result message
	 */
	@Transactional
	public Messages cancel(ProcessingOrder order) {
		if (logger.isTraceEnabled()) logger.trace(">>> cancel({})", (null == order ? "null" : order.getId()));
		
		Messages answer = Messages.FALSE;
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
			case APPROVED:
				answer = Messages.ORDER_HASTOBE_PLANNED;
				break;
			case PLANNED:
				for (Job job : order.getJobs()) {
					jobUtil.cancel(job);
				}
				order.setOrderState(OrderState.FAILED);
				order.incrementVersion();
				RepositoryService.getOrderRepository().save(order);
				logOrderState(order);
				answer = Messages.ORDER_CANCELED;
				break;	
			case RELEASED:
				answer = Messages.ORDER_ALREADY_RELEASED;
				break;
			case RELEASING:
				answer = Messages.ORDER_ALREADY_RELEASING;
				break;
			case RUNNING:
				answer = Messages.ORDER_ALREADY_RUNNING;
				break;
			case SUSPENDING:
				answer = Messages.ORDER_ALREADY_SUSPENDING;
				break;
			case COMPLETED:
				answer = Messages.ORDER_ALREADY_COMPLETED;
				break;
			case FAILED:
				answer = Messages.ORDER_ALREADY_FAILED;
				break;
			case CLOSED:
				answer = Messages.ORDER_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			answer.log(logger, order.getIdentifier());
		}
		return answer;
	}

	/**
	 * Reset the processing order and it jobs and job steps.
	 * 
	 * @param order The processing Order
	 * @return Result message
	 */
	@Transactional
	public Messages reset(ProcessingOrder order) {
		if (logger.isTraceEnabled()) logger.trace(">>> reset({})", (null == order ? "null" : order.getId()));
		
		Messages answer = Messages.FALSE;
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
				answer = Messages.ORDER_RESET;
				break;
			case PLANNING:
				// look for plan thread and interrupt it
				OrderPlanThread pt = productionPlanner.getPlanThreads().get(ProductionPlanner.PLAN_THREAD_PREFIX + order.getId());
				if (pt != null) {
					pt.interrupt();
					int i = 0;
					while (pt.isAlive() && i < 1000) {
						i++;
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							break;
						}
					}
				}
				order.setOrderState(OrderState.APPROVED);
				order.setHasFailedJobSteps(false);
				order.incrementVersion();
				RepositoryService.getOrderRepository().save(order);
				logOrderState(order);
				answer = Messages.ORDER_RESET;
				break;	
			case PLANNING_FAILED:
				// jobs are in initial state, no change
				order.setOrderState(OrderState.APPROVED);
				order.setHasFailedJobSteps(false);
				order.incrementVersion();
				RepositoryService.getOrderRepository().save(order);
				logOrderState(order);
				answer = Messages.ORDER_RESET;
				break;			
			case APPROVED:		
			case RELEASED:		
			case PLANNED:
				// remove jobs and job steps
				HashMap<Long,Job> toRemove = new HashMap<Long,Job>();
				for (Job job : order.getJobs()) {
					if (jobUtil.delete(job)) {
						toRemove.put(job.getId(), job);
					}
				}
				List<Job> existingJobs = new ArrayList<Job>();
				existingJobs.addAll(order.getJobs());
				order.getJobs().clear();
				for (Job job : existingJobs) {
					if (toRemove.get(job.getId()) == null) {
						order.getJobs().add(job);
					} else {
						RepositoryService.getJobRepository().delete(job);
					}
				}
				order.setOrderState(OrderState.INITIAL);
				order.setHasFailedJobSteps(false);
				order.incrementVersion();
				RepositoryService.getOrderRepository().save(order);
				logOrderState(order);
				answer = Messages.ORDER_RESET;
				break;	
			case RELEASING:
				answer = Messages.ORDER_ALREADY_RELEASING;
				break;
			case RUNNING:
				answer = Messages.ORDER_ALREADY_RUNNING;
				break;
			case SUSPENDING:
				answer = Messages.ORDER_ALREADY_SUSPENDING;
				break;
			case COMPLETED:
				answer = Messages.ORDER_ALREADY_COMPLETED;
				break;
			case FAILED:
				answer = Messages.ORDER_ALREADY_FAILED;
				break;
			case CLOSED:
				answer = Messages.ORDER_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			answer.log(logger, order.getIdentifier());
		}
		return answer;
	}

	/**
	 * Delete the processing order and it jobs and job steps.
	 * 
	 * @param order The processing Order
	 * @return Result message
	 */
	@Transactional
	public Messages delete(ProcessingOrder order) {
		if (logger.isTraceEnabled()) logger.trace(">>> delete({})", (null == order ? "null" : order.getId()));
		
		Messages answer = Messages.FALSE;
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
			case APPROVED:
				// jobs are in initial state, no change
				RepositoryService.getOrderRepository().delete(order);
				answer = Messages.ORDER_DELETED;
				break;				
			case PLANNED:
			case COMPLETED:
			case FAILED:
			case CLOSED:
				// remove jobs and jobsteps
				HashMap<Long,Job> toRemove = new HashMap<Long,Job>();
				for (Job job : order.getJobs()) {
					if (jobUtil.deleteForced(job)) {
						toRemove.put(job.getId(), job);
					}
				}
				List<Job> existingJobs = new ArrayList<Job>();
				existingJobs.addAll(order.getJobs());
				order.getJobs().clear();
				for (Job job : existingJobs) {
					if (toRemove.get(job.getId()) == null) {
						order.getJobs().add(job);
					} else {
						RepositoryService.getJobRepository().delete(job);
					}
				}
				RepositoryService.getOrderRepository().delete(order);
				answer = Messages.ORDER_DELETED;
				break;
			case RELEASING:
				answer = Messages.ORDER_ALREADY_RELEASING;
				break;	
			case RELEASED:
				answer = Messages.ORDER_ALREADY_RELEASED;
				break;
			case RUNNING:
				answer = Messages.ORDER_ALREADY_RUNNING;
				break;
			case SUSPENDING:
				answer = Messages.ORDER_ALREADY_SUSPENDING;
				break;
			default:
				break;
			}
			answer.log(logger, order.getIdentifier());
		}
		return answer;
	}

	/**
	 * Approve the processing order and it jobs and job steps.
	 * 
	 * @param order The processing Order
	 * @return Result message
	 */
	@Transactional
	public Messages approve(ProcessingOrder order) {
		if (logger.isTraceEnabled()) logger.trace(">>> approve({})", (null == order ? "null" : order.getId()));
		
		Messages answer = Messages.ORDER_ALREADY_APPROVED;
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
				// jobs are in initial state, no change
				order.setOrderState(OrderState.APPROVED);
				order.incrementVersion();
				RepositoryService.getOrderRepository().save(order);
				logOrderState(order);
				answer = Messages.ORDER_APPROVED;
				break;			
			case APPROVED:
				answer = Messages.ORDER_ALREADY_APPROVED;
				break;
			case PLANNED:	
				answer = Messages.ORDER_ALREADY_PLANNED;
				break;
			case RELEASING:
				answer = Messages.ORDER_ALREADY_RELEASING;
				break;
			case RELEASED:
				answer = Messages.ORDER_ALREADY_RELEASED;
				break;
			case RUNNING:
				answer = Messages.ORDER_ALREADY_RUNNING;
				break;
			case SUSPENDING:
				answer = Messages.ORDER_ALREADY_SUSPENDING;
				break;
			case COMPLETED:
				answer = Messages.ORDER_ALREADY_COMPLETED;
				break;
			case FAILED:
				answer = Messages.ORDER_ALREADY_FAILED;
				break;
			case CLOSED:
				answer = Messages.ORDER_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			answer.log(logger, order.getIdentifier());
		}
		return answer;
	}

	/**
	 * Plan the processing order and it jobs and job steps.
	 * 
	 * @param order The processing order
	 * @param procFacility The processing facility to run the order
	 * @return Result message
	 */
	@Transactional
	public Messages plan(ProcessingOrder order,  ProcessingFacility procFacility) {
		if (logger.isTraceEnabled()) logger.trace(">>> plan({}, {})", (null == order ? "null" : order.getId()),
				(null == procFacility ? "null" : procFacility.getName()));
		
		Messages answer = Messages.FALSE;
		if (order != null && procFacility != null) {
			switch (order.getOrderState()) {
			case INITIAL:
				answer = Messages.ORDER_HASTOBE_APPROVED;
				break;
			case APPROVED:
			case PLANNING_FAILED:
				order.setOrderState(OrderState.PLANNING);
				order.incrementVersion();
				order = RepositoryService.getOrderRepository().save(order);
				em.merge(order);
				String threadName = ProductionPlanner.PLAN_THREAD_PREFIX + order.getId();
				if (!productionPlanner.getPlanThreads().containsKey(threadName)) {
					OrderPlanThread pt = new OrderPlanThread(productionPlanner, orderDispatcher, order, procFacility, threadName);
					productionPlanner.getPlanThreads().put(threadName, pt);
					pt.start();
				}
				answer = Messages.ORDER_PLANNING;
				break;	
			case PLANNED:	
				answer = Messages.ORDER_ALREADY_PLANNED;
				break;
			case RELEASING:
				answer = Messages.ORDER_ALREADY_RELEASING;
				break;
			case RELEASED:
				answer = Messages.ORDER_ALREADY_RELEASED;
				break;
			case RUNNING:
				answer = Messages.ORDER_ALREADY_RUNNING;
				break;
			case SUSPENDING:
				answer = Messages.ORDER_ALREADY_SUSPENDING;
				break;
			case COMPLETED:
				answer = Messages.ORDER_ALREADY_COMPLETED;
				break;
			case FAILED:
				answer = Messages.ORDER_ALREADY_FAILED;
				break;
			case CLOSED:
				answer = Messages.ORDER_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			answer.log(logger, order.getIdentifier());
		}
		return answer;
	}

	/**
	 * Resume the processing order and it jobs and job steps.
	 * 
	 * @param order The processing Order
	 * @return Result message
	 */
	@Transactional
	public Messages resume(ProcessingOrder order) {
		if (logger.isTraceEnabled()) logger.trace(">>> resume({})", (null == order ? "null" : order.getId()));
		
		Messages answer = Messages.FALSE;
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
				answer = Messages.ORDER_HASTOBE_APPROVED;
				break;
			case APPROVED:
			case PLANNING:
				answer = Messages.ORDER_HASTOBE_PLANNED;
				break;
			case PLANNED:
				order.setOrderState(OrderState.RELEASING);
				order.incrementVersion();
				order = RepositoryService.getOrderRepository().save(order);
				em.merge(order);
				String threadName = ProductionPlanner.RELEASE_THREAD_PREFIX + order.getId();
				if (!productionPlanner.getReleaseThreads().containsKey(threadName)) {
					OrderReleaseThread rt = new OrderReleaseThread(productionPlanner, jobUtil, order, threadName);
					productionPlanner.getReleaseThreads().put(threadName, rt);
					rt.start();
				}
				logOrderState(order);
				answer = Messages.ORDER_RELEASING;
				break;	
			case RELEASING:
				answer = Messages.ORDER_ALREADY_RELEASING;
				break;
			case RELEASED:
				answer = Messages.ORDER_ALREADY_RELEASED;
				break;
			case RUNNING:
				answer = Messages.ORDER_ALREADY_RUNNING;
				break;
			case SUSPENDING:
				answer = Messages.ORDER_ALREADY_SUSPENDING;
				break;
			case COMPLETED:
				answer = Messages.ORDER_ALREADY_COMPLETED;
				break;
			case FAILED:
				answer = Messages.ORDER_ALREADY_FAILED;
				break;
			case CLOSED:
				answer = Messages.ORDER_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			answer.log(logger, order.getIdentifier());
		}
		return answer;
	}

	/**
	 * Start the processing order and it jobs and job steps.
	 * 
	 * @param order The processing Order
	 * @return Result message
	 */
	@Transactional
	public Messages startOrder(ProcessingOrder order) {
		if (logger.isTraceEnabled()) logger.trace(">>> startOrder({})", (null == order ? "null" : order.getId()));
		
		Messages answer = Messages.FALSE;
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
				answer = Messages.ORDER_HASTOBE_APPROVED;
				break;
			case APPROVED:
				answer = Messages.ORDER_HASTOBE_PLANNED;
				break;
			case PLANNED:
				answer = Messages.ORDER_HASTOBE_RELEASED;
				break;	
			case RELEASING:
				answer = Messages.ORDER_ALREADY_RELEASING;	
				break;		
			case RELEASED:
				order.setOrderState(OrderState.RUNNING);
				order.incrementVersion();
				RepositoryService.getOrderRepository().save(order);
				logOrderState(order);
				answer = Messages.ORDER_RUNNING;
				break;				
			case RUNNING:
				answer = Messages.ORDER_RUNNING;
				break;	
			case SUSPENDING:
				answer = Messages.ORDER_ALREADY_SUSPENDING;
				break;
			case COMPLETED:
				answer = Messages.ORDER_ALREADY_COMPLETED;
				break;
			case FAILED:
				answer = Messages.ORDER_ALREADY_FAILED;
				break;
			case CLOSED:
				answer = Messages.ORDER_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			answer.log(logger, order.getIdentifier());
		}
		return answer;
	}

	/**
	 * Suspend the processing order and its jobs and job steps.
	 * 
	 * @param order The processing order id
	 * @param force The flag to force kill of currently running job steps on processing facility
	 * @return Result message
	 */
	@Transactional
	public Messages suspend(long id, Boolean force) {
		ProcessingOrder order = null;
		Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(id);
		if (orderOpt.isPresent()) {
			order = orderOpt.get();
		}
		if (logger.isTraceEnabled()) logger.trace(">>> suspend({}, {})", (null == order ? "null" : order.getId()), force);
		Messages answer = Messages.FALSE;
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
				answer = Messages.ORDER_SUSPENDED;
				break;
			case APPROVED:
				answer = Messages.ORDER_SUSPENDED;
				break;
			case PLANNED:	
				answer = Messages.ORDER_SUSPENDED;
				break;
			case RELEASING:
				// look for plan thread and interrupt it
				OrderReleaseThread rt = productionPlanner.getReleaseThreads().get(ProductionPlanner.RELEASE_THREAD_PREFIX + order.getId());
				if (rt != null) {
					rt.interrupt();
					int i = 0;
					while (rt.isAlive() && i < 1000) {
						i++;
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							break;
						}
					}
				}
				// intentionally fall through to suspend already running jobs
			case RELEASED:
				for (Job job : order.getJobs()) {
					jobUtil.suspend(job, force);
				}
				order.setOrderState(OrderState.PLANNED);
				order.incrementVersion();
				RepositoryService.getOrderRepository().save(order);
				logOrderState(order);
				answer = Messages.ORDER_SUSPENDED;
				break;			
			case RUNNING:
			case SUSPENDING:
				Boolean suspending = false;
				Boolean allFinished = true;
				for (Job job : order.getJobs()) {
					jobUtil.suspend(job, force);
					// check for state
					if (job.getJobState() == JobState.COMPLETED || job.getJobState() == JobState.RELEASED) {
						allFinished = allFinished & true;
					} else {
						allFinished = allFinished & false;
					}
					if (job.getJobState() == JobState.ON_HOLD || job.getJobState() == JobState.STARTED) {
						suspending = true;
					}
				}
				order.incrementVersion();
				if (suspending) {
					// check whether some jobs are already finished
					order.setOrderState(OrderState.SUSPENDING);
					RepositoryService.getOrderRepository().save(order);
					logOrderState(order);
					answer = Messages.ORDER_SUSPENDED;
				} else if (allFinished) {
					// check whether some jobs are already finished
					order.setOrderState(OrderState.COMPLETED);
					RepositoryService.getOrderRepository().save(order);
					logOrderState(order);
					answer = Messages.ORDER_COMPLETED;
				} else {
					order.setOrderState(OrderState.SUSPENDING); // direct transition to PLANNED not allowed
					order.setOrderState(OrderState.PLANNED);
					RepositoryService.getOrderRepository().save(order);
					logOrderState(order);
					answer = Messages.ORDER_SUSPENDED;
				}
				break;	
			case COMPLETED:
				answer = Messages.ORDER_ALREADY_COMPLETED;
				break;
			case FAILED:
				answer = Messages.ORDER_ALREADY_FAILED;
				break;
			case CLOSED:
				answer = Messages.ORDER_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			answer.log(logger, order.getIdentifier());
		}
		return answer;
	}

	/**
	 * Prepare the suspend of the processing order.
	 * All jobs are set to state ON_HOLD first to avoid start of further job steps.
	 * 
	 * @param order The processing order id
	 * @param force The flag to force kill of currently running job steps on processing facility
	 * @return Result message
	 */
	@Transactional
	public Messages prepareSuspend(long id, Boolean force) {
		ProcessingOrder order = null;
		Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(id);
		if (orderOpt.isPresent()) {
			order = orderOpt.get();
		}
		if (logger.isTraceEnabled()) logger.trace(">>> suspend({}, {})", (null == order ? "null" : order.getId()), force);
		Messages answer = Messages.FALSE;
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
				answer = Messages.ORDER_SUSPENDED;
				break;
			case APPROVED:
				answer = Messages.ORDER_SUSPENDED;
				break;
			case PLANNED:	
				answer = Messages.ORDER_SUSPENDED;
				break;
			case RELEASING:
				// look for plan thread and interrupt it
				OrderReleaseThread rt = productionPlanner.getReleaseThreads().get(ProductionPlanner.RELEASE_THREAD_PREFIX + order.getId());
				if (rt != null) {
					rt.interrupt();
					int i = 0;
					while (rt.isAlive() && i < 1000) {
						i++;
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							break;
						}
					}
				}
				// intentionally fall through to suspend already running jobs
			case RELEASED:
			case RUNNING:
			case SUSPENDING:
				for (Job job : order.getJobs()) {
					switch (job.getJobState()) {
					case INITIAL:
						job.setJobState(de.dlr.proseo.model.Job.JobState.RELEASED);
						// intentionally fall through
					case RELEASED:
						job.setJobState(de.dlr.proseo.model.Job.JobState.STARTED);
						// intentionally fall through
					case STARTED:
						job.setJobState(de.dlr.proseo.model.Job.JobState.ON_HOLD);
						RepositoryService.getJobRepository().save(job);
						break;
					default:
						break;
						
					}
				}
				answer = Messages.ORDER_SUSPENDED;
				break;	
			case COMPLETED:
				answer = Messages.ORDER_ALREADY_COMPLETED;
				break;
			case FAILED:
				answer = Messages.ORDER_ALREADY_FAILED;
				break;
			case CLOSED:
				answer = Messages.ORDER_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			answer.log(logger, order.getIdentifier());
		}
		return answer;
	}

	/**
	 * Retry the processing order and it jobs and job steps.
	 * 
	 * @param order The processing Order
	 * @return Result message
	 */
	@Transactional
	public Messages retry(ProcessingOrder order) {
		if (logger.isTraceEnabled()) logger.trace(">>> retry({})", (null == order ? "null" : order.getId()));
		
		Messages answer = Messages.FALSE;
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
			case APPROVED:
			case PLANNED:
			case RELEASING:
			case RELEASED:
			case RUNNING:
			case SUSPENDING:
			case COMPLETED:
				answer = Messages.ORDER_COULD_NOT_RETRY;
				break;
			case FAILED:
				Boolean all = true;
				Boolean allCompleted = true;
				order.setHasFailedJobSteps(false);
				for (Job job : order.getJobs()) {
					jobUtil.retry(job);
				}
				for (Job job : order.getJobs()) {
					if (!(job.getJobState() == JobState.PLANNED || job.getJobState() == JobState.COMPLETED)) {
						all = false;
					}
					if (job.getJobState() != JobState.COMPLETED) {
						allCompleted = false;
					}
					if (job.getHasFailedJobSteps()) {
						order.setHasFailedJobSteps(true);
					}
				}
				if (all) {
					if (allCompleted) {
						order.setOrderState(OrderState.PLANNED);
						order.setOrderState(OrderState.RELEASED);
						order.setOrderState(OrderState.RUNNING);
						order.setOrderState(OrderState.COMPLETED);
						order.incrementVersion();
						RepositoryService.getOrderRepository().save(order);
						answer = Messages.ORDER_COMPLETED;
					} else {
						order.setOrderState(OrderState.PLANNED);
						order.incrementVersion();
						RepositoryService.getOrderRepository().save(order);
						answer = Messages.ORDER_RETRIED;
					}
					logOrderState(order);
				} else {
					answer = Messages.ORDER_COULD_NOT_RETRY;
				}
				break;
			case CLOSED:
				answer = Messages.ORDER_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			answer.log(logger, order.getIdentifier());
		}
		return answer;
	}

	/**
	 * Close the processing order and it jobs and job steps.
	 * 
	 * @param order The processing Order
	 * @return Result message
	 */
	@Transactional
	public Messages close(ProcessingOrder order) {
		if (logger.isTraceEnabled()) logger.trace(">>> close({})", (null == order ? "null" : order.getId()));
		
		Messages answer = Messages.FALSE;
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
			case APPROVED:
			case PLANNED:	
			case RELEASING:
			case RELEASED:
			case RUNNING:
			case SUSPENDING:
				answer = Messages.ORDER_HASTOBE_FINISHED;
				break;
			case COMPLETED:
			case FAILED:
				// job steps are completed/failed
				Duration retPeriod = order.getMission().getOrderRetentionPeriod();
				if (retPeriod != null && order.getProductionType() == ProductionType.SYSTEMATIC) {
					order.setEvictionTime(Instant.now().plus(retPeriod));
				}
				for (Job job : order.getJobs()) {
					jobUtil.close(job);
				}
				order.setOrderState(OrderState.CLOSED);
				order.incrementVersion();
				RepositoryService.getOrderRepository().save(order);
				logOrderState(order);
				answer = Messages.ORDER_CLOSED;
				break;			
			case CLOSED:
				answer = Messages.ORDER_CLOSED;
				break;
			default:
				break;
			}
			answer.log(logger, order.getIdentifier());
		}
		return answer;
	}

	/**
	 * Check whether the processing order and it jobs and job steps are finished.
	 * 
	 * @param order The processing Order
	 * @return true after success
	 */
	@Transactional
	public Boolean checkFinish(Long orderId) {
		if (logger.isTraceEnabled()) logger.trace(">>> checkFinish({})", orderId);
		
		Boolean answer = false;	
		Boolean checkFurther = false;
		Boolean hasChanged = false;
		ProcessingOrder order = null;
		Optional<ProcessingOrder> oOrder = RepositoryService.getOrderRepository().findById(orderId);
		if (oOrder.isPresent()) {
			order = oOrder.get();
		}
		// check current state for possibility to be suspended
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
			case APPROVED:
			case PLANNED:	
			case RELEASED:
			case SUSPENDING:
				Boolean onHold = RepositoryService.getJobRepository().countJobOnHoldByProcessingOrderId(order.getId()) > 0;
				if (!onHold) {
					Boolean all = RepositoryService.getJobRepository().countJobNotFinishedByProcessingOrderId(order.getId()) == 0;
					if (!all) {
						order.setOrderState(OrderState.PLANNED);
						RepositoryService.getOrderRepository().save(order);
						em.merge(order);
						hasChanged = true;
						checkFurther = true;
						answer = true;
						break;
					}
				} else {
					break;
				}
			case RUNNING:
				Boolean all = RepositoryService.getJobRepository().countJobNotFinishedByProcessingOrderId(order.getId()) == 0;
				if (all) {
					Boolean completed = RepositoryService.getJobRepository().countJobFailedByProcessingOrderId(order.getId()) == 0;
					if (completed) {
						order.setOrderState(OrderState.COMPLETED);
					} else {
						order.setOrderState(OrderState.FAILED);
					}
					checkFurther = true;
					RepositoryService.getOrderRepository().save(order);
					em.merge(order);
					hasChanged = true;
				}
				answer = true;
				break;
			case COMPLETED:
			case FAILED:
				checkFurther = true;
				answer = true;
				break;
			default:
				break;
			}	
			if (checkFurther) {
				Boolean hasFailed = false;
				for (Job j : order.getJobs()) {
					if (j.getJobState() == JobState.FAILED) {
						hasFailed = true;
						break;
					}
				}
				if (order.getHasFailedJobSteps() != hasFailed) {
					order.setHasFailedJobSteps(hasFailed);
					RepositoryService.getOrderRepository().save(order);
					em.merge(order);
					hasChanged = true;
				}
				Boolean allHasFinished = true;
				for (Job j : order.getJobs()) {
					if (j.getJobState() != JobState.FAILED && j.getJobState() != JobState.COMPLETED) {
						allHasFinished = false;
						break;
					}
				}
				if (allHasFinished) {
					if (hasFailed) {
						order.setOrderState(OrderState.FAILED);
					} else {
						order.setOrderState(OrderState.COMPLETED);
						// If the order is in systematic processing and the mission has an order retention period,
						// the order is automatically closed after completion and the eviction time is set.
						Duration retPeriod = order.getMission().getOrderRetentionPeriod();
						if (retPeriod != null && order.getProductionType() == ProductionType.SYSTEMATIC) {
							order.setEvictionTime(Instant.now().plus(retPeriod));
							order.setOrderState(OrderState.CLOSED);
						}
					}
					RepositoryService.getOrderRepository().save(order);
					em.merge(order);
					hasChanged = true;
				}
			}
			if (hasChanged) {
				order.incrementVersion();
				RepositoryService.getOrderRepository().save(order);
				em.merge(order);
				logOrderState(order);
			}
		}
 		return answer;
	}
		
	/**
	 * Get the processing facility(-ies) processing the order
	 * At the moment there is normally only one facility to do so.
	 * 
	 * @param order The processing order
	 * @return List of processinig facilities
	 */
	
	public List<ProcessingFacility> getProcessingFacilities(long id) {
		ProcessingOrder order = null;
		Optional<ProcessingOrder> oOrder = RepositoryService.getOrderRepository().findById(id);
		if (oOrder.isPresent()) {
			order = oOrder.get();
		}
		if (logger.isTraceEnabled()) logger.trace(">>> getProcessingFacilities({})", (null == order ? "null" : order.getId()));
		
		List<ProcessingFacility> pfList = new ArrayList<ProcessingFacility>();
		if (order != null) {
			for (Job j : order.getJobs()) {
				if (!pfList.contains(j.getProcessingFacility())) {
					pfList.add(j.getProcessingFacility());
				}
			}
		}
		return pfList;
	}

	
	/**
	 * Update the order state depending on job state
	 * 
	 * @param order The processing order
	 * @param jState The job state
	 */

	@Transactional
	public void updateState(ProcessingOrder orderOrig, JobState jState) {
		if (logger.isTraceEnabled()) logger.trace(">>> updateState({}, {})", (null == orderOrig ? "null" : orderOrig.getId()), jState);
		
		ProcessingOrder order = null;
		Optional<ProcessingOrder> oOrder = RepositoryService.getOrderRepository().findById(orderOrig.getId());
		if (oOrder.isPresent()) {
			order = oOrder.get();
		}
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
				// fall through intended
			case APPROVED:
				// no job and job step exists
				break;
			case PLANNED:
				if (jState == JobState.RELEASED) {
					order.setOrderState(OrderState.RELEASED);
					order.incrementVersion();
					RepositoryService.getOrderRepository().save(order);
					em.merge(order);
				} else if (jState == JobState.FAILED) {
					Boolean allState = true;
					this.setHasFailedJobSteps(order,  true);
					for (Job j : order.getJobs()) {
						if (j.getJobState() != JobState.FAILED && j.getJobState() != JobState.COMPLETED) {
							allState = false;
							break;
						}
					}
					if (allState) {
						order.setOrderState(OrderState.FAILED);
						order.incrementVersion();
						RepositoryService.getOrderRepository().save(order);
						em.merge(order);
					}
				}	
				break;
			case RELEASED:
				if (jState == JobState.FAILED) {
					Boolean allState = true;
					this.setHasFailedJobSteps(order,  true);
					for (Job j : order.getJobs()) {
						if (j.getJobState() != JobState.FAILED && j.getJobState() != JobState.COMPLETED) {
							allState = false;
							break;
						}
					}
					if (allState) {
						order.setOrderState(OrderState.FAILED);
						order.incrementVersion();
						RepositoryService.getOrderRepository().save(order);
						em.merge(order);
					}
				} else if (jState == JobState.STARTED) {
					order.setOrderState(OrderState.RUNNING);
					order.incrementVersion();
					RepositoryService.getOrderRepository().save(order);
					em.merge(order);
				}	
				break;
			case RUNNING:
				if (jState == JobState.FAILED) {
					Boolean allState = true;
					this.setHasFailedJobSteps(order,  true);
					for (Job j : order.getJobs()) {
						if (j.getJobState() != JobState.FAILED && j.getJobState() != JobState.COMPLETED) {
							allState = false;
							break;
						}
					}
					if (allState) {
						order.setOrderState(OrderState.FAILED);
						order.incrementVersion();
						RepositoryService.getOrderRepository().save(order);
						em.merge(order);
					}
				} else if (jState == JobState.ON_HOLD) {
					Boolean allState = true;
					for (Job j : order.getJobs()) {
						if (j.getJobState() != JobState.ON_HOLD) {
							allState = false;
							break;
						}
					}
					if (allState) {
						order.setOrderState(OrderState.SUSPENDING);
						order.incrementVersion();
						RepositoryService.getOrderRepository().save(order);
						em.merge(order);
					}
				}	
				break;
			case SUSPENDING:
				if (jState == JobState.FAILED) {
					Boolean allState = true;
					this.setHasFailedJobSteps(order,  true);
					for (Job j : order.getJobs()) {
						if (j.getJobState() != JobState.FAILED && j.getJobState() != JobState.COMPLETED) {
							allState = false;
							break;
						}
					}
					if (allState) {
						order.setOrderState(OrderState.FAILED);
						order.incrementVersion();
						RepositoryService.getOrderRepository().save(order);
						em.merge(order);
					}
				}	
				break;
			case COMPLETED:
				break;
			case FAILED:
				if (jState == JobState.PLANNED) {
					order.setOrderState(OrderState.PLANNED);
					order.incrementVersion();
					RepositoryService.getOrderRepository().save(order);
					em.merge(order);
				}
				break;
			case CLOSED:
				break;
			default:
				break;
			}
			Boolean hasFailed = false;
			for (Job j : order.getJobs()) {
				if (j.getJobState() == JobState.FAILED || j.hasFailedJobSteps()) {
					hasFailed = true;
					break;
				}
			}
			order.setHasFailedJobSteps(hasFailed);
			Boolean allHasFinished = true;
			for (Job j : order.getJobs()) {
				if (j.getJobState() != JobState.FAILED && j.getJobState() != JobState.COMPLETED) {
					allHasFinished = false;
					break;
				}
			}
			if (allHasFinished) {
				if (hasFailed) {
					order.setOrderState(OrderState.FAILED);
				} else {
					if (order.getOrderState() == OrderState.FAILED) {
						order.setOrderState(OrderState.PLANNED);
						order.setOrderState(OrderState.RELEASED);
						order.setOrderState(OrderState.RUNNING);
					}
					order.setOrderState(OrderState.COMPLETED);
				}
				order.incrementVersion();
				RepositoryService.getOrderRepository().save(order);
				em.merge(order);
			}
			logOrderState(order);
		}
	}
	

	/**
	 * Set the failed job steps flag in the order on failure
	 * 
	 * @param order the order to update
	 * @param failed indicates whether a job step has failed
	 */
	@Transactional
	public void setHasFailedJobSteps(ProcessingOrder order, Boolean failed) {
		if (logger.isTraceEnabled()) logger.trace(">>> setHasFailedJobSteps({}, {})", (null == order ? "null" : order.getId()), failed);
		
		if (failed && !order.getHasFailedJobSteps()) {
			order.setHasFailedJobSteps(failed);
			order.incrementVersion();
			RepositoryService.getOrderRepository().save(order);
			em.merge(order);
		}
	}

	public void logOrderState(ProcessingOrder order) {
		// at the moment a dummy		
	}

}
