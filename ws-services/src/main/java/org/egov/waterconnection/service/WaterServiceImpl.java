package org.egov.waterconnection.service;


import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.waterconnection.config.WSConfiguration;
import org.egov.waterconnection.constants.WCConstants;
import org.egov.waterconnection.model.Property;
import org.egov.waterconnection.model.SearchCriteria;
import org.egov.waterconnection.model.WaterConnection;
import org.egov.waterconnection.model.WaterConnectionRequest;
import org.egov.waterconnection.model.workflow.BusinessService;
import org.egov.waterconnection.repository.WaterDao;
import org.egov.waterconnection.repository.WaterDaoImpl;
import org.egov.waterconnection.util.WaterServicesUtil;
import org.egov.waterconnection.validator.ActionValidator;
import org.egov.waterconnection.validator.MDMSValidator;
import org.egov.waterconnection.validator.ValidateProperty;
import org.egov.waterconnection.validator.WaterConnectionValidator;
import org.egov.waterconnection.workflow.WorkflowIntegrator;
import org.egov.waterconnection.workflow.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class WaterServiceImpl implements WaterService {

	@Autowired
	private WaterDao waterDao;
	
	@Autowired
	private WaterConnectionValidator waterConnectionValidator;

	@Autowired
	private ValidateProperty validateProperty;
	
	@Autowired
	private MDMSValidator mDMSValidator;

	@Autowired
	private EnrichmentService enrichmentService;
	
	@Autowired
	private WorkflowIntegrator wfIntegrator;
	
	@Autowired
	private WSConfiguration config;
	
	@Autowired
	private WorkflowService workflowService;
	
	@Autowired
	private ActionValidator actionValidator;
	
	@Autowired
	private WaterServicesUtil waterServiceUtil;
	
	@Autowired
	private CalculationService calculationService;
	
	@Autowired
	private WaterDaoImpl waterDaoImpl;

	@Autowired
	private UserService userService;
	
	@Autowired
	private WaterServicesUtil wsUtil;
	
	/**
	 * 
	 * @param waterConnectionRequest WaterConnectionRequest contains water connection to be created
	 * @return List of WaterConnection after create
	 */
	@Override
	public List<WaterConnection> createWaterConnection(WaterConnectionRequest waterConnectionRequest) {
		if (wsUtil.isModifyConnectionRequest(waterConnectionRequest)) {
			List<WaterConnection> previousConnectionsList = getAllWaterApplications(waterConnectionRequest);

			// Validate any process Instance exists with WF
			WaterConnection waterConnectionWithWF = workflowService.getInProgressWF(previousConnectionsList,
					waterConnectionRequest.getRequestInfo(), waterConnectionRequest.getWaterConnection().getTenantId());

			if (waterConnectionWithWF != null) {
				throw new CustomException("WS_APP_EXIST_IN_WF",
						"Application already exist in WorkFlow. Cannot modify connection.");
			}
		}
		waterConnectionValidator.validateWaterConnection(waterConnectionRequest, WCConstants.CREATE_APPLICATION);
		Property property = validateProperty.getOrValidateProperty(waterConnectionRequest);
		validateProperty.validatePropertyFields(property);
		mDMSValidator.validateMasterForCreateRequest(waterConnectionRequest);
		enrichmentService.enrichWaterConnection(waterConnectionRequest);
		userService.createUser(waterConnectionRequest);
		// call work-flow
		if (config.getIsExternalWorkFlowEnabled())
			wfIntegrator.callWorkFlow(waterConnectionRequest, property);
		waterDao.saveWaterConnection(waterConnectionRequest);
		return Arrays.asList(waterConnectionRequest.getWaterConnection());
	}
	/**
	 * 
	 * @param criteria WaterConnectionSearchCriteria contains search criteria on water connection
	 * @param requestInfo 
	 * @return List of matching water connection
	 */
	public List<WaterConnection> search(SearchCriteria criteria, RequestInfo requestInfo) {
		List<WaterConnection> waterConnectionList;
		waterConnectionList = getWaterConnectionsList(criteria, requestInfo);
		waterConnectionValidator.validatePropertyForConnection(waterConnectionList);
		enrichmentService.enrichConnectionHolderDeatils(waterConnectionList, criteria, requestInfo);
		return waterConnectionList;
	}
	/**
	 * 
	 * @param criteria WaterConnectionSearchCriteria contains search criteria on water connection
	 * @param requestInfo 
	 * @return List of matching water connection
	 */
	public List<WaterConnection> getWaterConnectionsList(SearchCriteria criteria,
			RequestInfo requestInfo) {
		return waterDao.getWaterConnectionList(criteria, requestInfo);
	}
	/**
	 * 
	 * @param waterConnectionRequest WaterConnectionRequest contains water connection to be updated
	 * @return List of WaterConnection after update
	 */
	@Override
	public List<WaterConnection> updateWaterConnection(WaterConnectionRequest waterConnectionRequest) {
		if(wsUtil.isModifyConnectionRequest(waterConnectionRequest)) {
			// Received request to update the connection for modifyConnection WF
			return updateWaterConnectionForModifyFlow(waterConnectionRequest);
		}
		
		waterConnectionValidator.validateWaterConnection(waterConnectionRequest, WCConstants.UPDATE_APPLICATION);
		mDMSValidator.validateMasterData(waterConnectionRequest);
		BusinessService businessService = workflowService.getBusinessService(waterConnectionRequest.getWaterConnection().getTenantId(), 
				waterConnectionRequest.getRequestInfo(), config.getBusinessServiceValue());
		WaterConnection searchResult = getConnectionForUpdateRequest(waterConnectionRequest.getWaterConnection().getId(), waterConnectionRequest.getRequestInfo());
		Property property = validateProperty.getOrValidateProperty(waterConnectionRequest);
		validateProperty.validatePropertyFields(property);
		String previousApplicationStatus = workflowService.getApplicationStatus(waterConnectionRequest.getRequestInfo(),
				waterConnectionRequest.getWaterConnection().getApplicationNo(),
				waterConnectionRequest.getWaterConnection().getTenantId(),
				config.getBusinessServiceValue());
		enrichmentService.enrichUpdateWaterConnection(waterConnectionRequest);
		actionValidator.validateUpdateRequest(waterConnectionRequest, businessService, previousApplicationStatus);
		waterConnectionValidator.validateUpdate(waterConnectionRequest, searchResult, WCConstants.UPDATE_APPLICATION);
		wfIntegrator.callWorkFlow(waterConnectionRequest, property);
		//call calculator service to generate the demand for one time fee
		calculationService.calculateFeeAndGenerateDemand(waterConnectionRequest, property);
		//check for edit and send edit notification
		waterDaoImpl.pushForEditNotification(waterConnectionRequest);
		//Enrich file store Id After payment
		enrichmentService.enrichFileStoreIds(waterConnectionRequest);
		//Call workflow
		enrichmentService.postStatusEnrichment(waterConnectionRequest);
		boolean isStateUpdatable = waterServiceUtil.getStatusForUpdate(businessService, previousApplicationStatus);
		waterDao.updateWaterConnection(waterConnectionRequest, isStateUpdatable);
		enrichmentService.postForMeterReading(waterConnectionRequest);
		return Arrays.asList(waterConnectionRequest.getWaterConnection());
	}
	
	/**
	 * Search Water connection to be update
	 * 
	 * @param id
	 * @param requestInfo
	 * @return water connection
	 */
	public WaterConnection getConnectionForUpdateRequest(String id, RequestInfo requestInfo) {
		Set<String> ids = new HashSet<>(Arrays.asList(id));
		SearchCriteria criteria = new SearchCriteria();
		criteria.setIds(ids);
		List<WaterConnection> connections = getWaterConnectionsList(criteria, requestInfo);
		if (CollectionUtils.isEmpty(connections)) {
			StringBuilder builder = new StringBuilder();
			builder.append("WATER CONNECTION NOT FOUND FOR: ").append(id).append(" :ID");
			throw new CustomException("INVALID_WATERCONNECTION_SEARCH", builder.toString());
		}
			
		return connections.get(0);
	}
	
	private List<WaterConnection> getAllWaterApplications(WaterConnectionRequest waterConnectionRequest) {
		SearchCriteria criteria = SearchCriteria.builder()
				.connectionNumber(waterConnectionRequest.getWaterConnection().getConnectionNo()).build();
		return search(criteria, waterConnectionRequest.getRequestInfo());
	}
	
	private List<WaterConnection> updateWaterConnectionForModifyFlow(WaterConnectionRequest waterConnectionRequest) {
		waterConnectionValidator.validateWaterConnection(waterConnectionRequest, WCConstants.MODIFY_CONNECTION);
		
		// TODO - MDMS validation 
		//mDMSValidator.validateMasterData(waterConnectionRequest);
		
		BusinessService businessService = workflowService.getBusinessService(waterConnectionRequest.getWaterConnection().getTenantId(), 
				waterConnectionRequest.getRequestInfo(), config.getModifyWSBusinessServiceName());
		WaterConnection searchResult = getConnectionForUpdateRequest(waterConnectionRequest.getWaterConnection().getId(), waterConnectionRequest.getRequestInfo());
		Property property = validateProperty.getOrValidateProperty(waterConnectionRequest);
		validateProperty.validatePropertyFields(property);
		String previousApplicationStatus = workflowService.getApplicationStatus(waterConnectionRequest.getRequestInfo(),
				waterConnectionRequest.getWaterConnection().getApplicationNo(),
				waterConnectionRequest.getWaterConnection().getTenantId(),
				config.getModifyWSBusinessServiceName());
		enrichmentService.enrichUpdateWaterConnection(waterConnectionRequest);
		actionValidator.validateUpdateRequest(waterConnectionRequest, businessService, previousApplicationStatus);
		waterConnectionValidator.validateUpdate(waterConnectionRequest, searchResult, WCConstants.MODIFY_CONNECTION);
		wfIntegrator.callWorkFlow(waterConnectionRequest, property);
		//check for edit and send edit notification
		waterDaoImpl.pushForEditNotification(waterConnectionRequest);
		boolean isStateUpdatable = waterServiceUtil.getStatusForUpdate(businessService, previousApplicationStatus);
		waterDao.updateWaterConnection(waterConnectionRequest, isStateUpdatable);
		return Arrays.asList(waterConnectionRequest.getWaterConnection());
	}
}
