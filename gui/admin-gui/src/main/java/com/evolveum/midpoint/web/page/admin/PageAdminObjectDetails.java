/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.page.admin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Page;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.AuthorizationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.web.component.detailspanel.AbstractObjectDetailsPanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.progress.ProgressReporter;
import com.evolveum.midpoint.web.component.progress.ProgressReportingAwarePage;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.ObjectWrapperUtil;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.admin.users.PageOrgTree;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusProjectionDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.web.util.validation.MidpointFormValidator;
import com.evolveum.midpoint.web.util.validation.SimpleValidationError;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/** 
 * @author semancik
 */
public abstract class PageAdminObjectDetails<O extends ObjectType> extends PageAdmin
		implements ProgressReportingAwarePage {

	private static final String DOT_CLASS = PageAdminObjectDetails.class.getName() + ".";
	
	public static final String PARAM_RETURN_PAGE = "returnPage";

	private static final String OPERATION_LOAD_OBJECT = DOT_CLASS + "loadObject";
	private static final String OPERATION_LOAD_PARENT_ORGS = DOT_CLASS + "loadParentOrgs";
	protected static final String OPERATION_SAVE = DOT_CLASS + "save";
	protected static final String OPERATION_SEND_TO_SUBMIT = DOT_CLASS + "sendToSubmit";

	protected static final String ID_SUMMARY_PANEL = "summaryPanel";
	protected static final String ID_MAIN_PANEL = "mainPanel";

	private static final Trace LOGGER = TraceManager.getTrace(PageAdminObjectDetails.class);

	private LoadableModel<ObjectWrapper<O>> objectModel;
	private LoadableModel<List<FocusProjectionDto>> parentOrgModel;
	
	private ProgressReporter progressReporter;
	
	// used to determine whether to leave this page or stay on it (after
	// operation finishing)
	private ObjectDelta<O> delta;
	
	private AbstractObjectDetailsPanel<O> mainPanel;
	

	@Override
	protected IModel<String> createPageTitleModel() {
		return new LoadableModel<String>() {

			@Override
			protected String load() {
				if (!isEditingFocus()) {
					return createStringResource("pageAdminObjectDetails.title.newObject").getObject();
				}

				return createStringResource("pageAdminObjectDetails.title.editObject").getObject();
			}
		};
	}

	@Override
	protected IModel<String> createPageSubTitleModel() {
		return new LoadableModel<String>() {

			@Override
			protected String load() {
				if (!isEditingFocus()) {
					return createStringResource(
							"pageAdminObjectDetails.subTitle.new" + getCompileTimeClass().getSimpleName())
									.getObject();
				}

				String name = null;
				if (getObjectWrapper() != null && getObjectWrapper().getObject() != null) {
					name = WebMiscUtil.getName(getObjectWrapper().getObject());
				}

				return createStringResource(
						"pageAdminObjectDetails.subTitle.edit" + getCompileTimeClass().getSimpleName(), name)
								.getObject();
			}
		};
	}

	public LoadableModel<ObjectWrapper<O>> getObjectModel() {
		return objectModel;
	}

	public LoadableModel<List<FocusProjectionDto>> getParentOrgModel() {
		return parentOrgModel;
	}

	protected AbstractObjectDetailsPanel<O> getMainPanel() {
		return mainPanel;
	}
	
	public ObjectWrapper<O> getObjectWrapper() {
		return objectModel.getObject();
	}
	
	public List<FocusProjectionDto> getParentOrgs() {
		return parentOrgModel.getObject();
	}

	public ObjectDelta<O> getDelta() {
		return delta;
	}

	public void setDelta(ObjectDelta<O> delta) {
		this.delta = delta;
	}

	public ProgressReporter getProgressReporter() {
		return progressReporter;
	}

	protected void reviveModels() throws SchemaException {
		WebMiscUtil.revive(objectModel, getPrismContext());
		WebMiscUtil.revive(parentOrgModel, getPrismContext());
	}

	protected abstract Class<O> getCompileTimeClass();


	public void initialize(final PrismObject<O> objectToEdit) {
		initializeModel(objectToEdit);
		initLayout();
	}

	protected void initializeModel(final PrismObject<O> objectToEdit) {
		objectModel = new LoadableModel<ObjectWrapper<O>>(false) {

			@Override
			protected ObjectWrapper<O> load() {
				return loadObjectWrapper(objectToEdit);
			}
		};

		parentOrgModel = new LoadableModel<List<FocusProjectionDto>>(false) {

			@Override
			protected List<FocusProjectionDto> load() {
				return loadOrgWrappers();
			}
		};
	}

	protected List<FocusProjectionDto> loadOrgWrappers() {
		// WRONG!! TODO: fix
		return null;
	}
	
	protected abstract O createNewObject();
	
	protected void initLayout() {
		initLayoutSummaryPanel();
		
		mainPanel = createMainPanel(ID_MAIN_PANEL);
		add(mainPanel);
		
		progressReporter = createProgressReporter("progressPanel");
		add(progressReporter.getProgressPanel());
	}

	protected ProgressReporter createProgressReporter(String id) {
		return ProgressReporter.create(id, this);
	}

	protected abstract FocusSummaryPanel<O> createSummaryPanel();
	
	protected void initLayoutSummaryPanel() {

		FocusSummaryPanel<O> summaryPanel = createSummaryPanel();
		summaryPanel.setOutputMarkupId(true);

		summaryPanel.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return isEditingFocus();
			}
		});

		add(summaryPanel);
	}

	protected abstract AbstractObjectDetailsPanel<O> createMainPanel(String id);

	protected String getObjectOidParameter() {
		PageParameters parameters = getPageParameters();
		LOGGER.trace("Page parameters: {}", parameters);
		StringValue oidValue = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
		LOGGER.trace("OID parameter: {}", oidValue);
		if (oidValue == null) {
			return null;
		}
		String oid = oidValue.toString();
		if (StringUtils.isBlank(oid)) {
			return null;
		}
		return oid;
	}

	public boolean isEditingFocus() {
		return getObjectOidParameter() != null;
	}

	protected ObjectWrapper<O> loadObjectWrapper(PrismObject<O> objectToEdit) {
		Task task = createSimpleTask(OPERATION_LOAD_OBJECT);
		OperationResult result = task.getResult();
		PrismObject<O> object = null;
		try {
			if (!isEditingFocus()) {
				if (objectToEdit == null) {
					LOGGER.trace("Loading object: New object (creating)");
					O focusType = createNewObject();
					getMidpointApplication().getPrismContext().adopt(focusType);
					object = focusType.asPrismObject();
				} else {
					LOGGER.trace("Loading object: New object (supplied): {}", objectToEdit);
					object = objectToEdit;
				}
			} else {

				Collection options = SelectorOptions.createCollection(UserType.F_JPEG_PHOTO,
						GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE));

				String focusOid = getObjectOidParameter();
				object = WebModelUtils.loadObject(getCompileTimeClass(), focusOid, options, this, task,
						result);

				LOGGER.trace("Loading object: Existing object (loadled): {} -> {}", focusOid, object);
			}

			result.recordSuccess();
		} catch (Exception ex) {
			result.recordFatalError("Couldn't get object.", ex);
			LoggingUtils.logException(LOGGER, "Couldn't load object", ex);
		}

		if (!result.isSuccess()) {
			showResultInSession(result);
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Loaded object:\n{}", object.debugDump());
		}

		if (object == null) {
			if (isEditingFocus()) {
				getSession().error(getString("pageAdminFocus.message.cantEditFocus"));
			} else {
				getSession().error(getString("pageAdminFocus.message.cantNewFocus"));
			}
			throw new RestartResponseException(getRestartResponsePage());
		}

		ContainerStatus status = isEditingFocus() ? ContainerStatus.MODIFYING : ContainerStatus.ADDING;
		ObjectWrapper<O> wrapper = null;
		try {
			wrapper = ObjectWrapperUtil.createObjectWrapper("pageAdminFocus.focusDetails", null, object,
					status, this);
		} catch (Exception ex) {
			result.recordFatalError("Couldn't get user.", ex);
			LoggingUtils.logException(LOGGER, "Couldn't load user", ex);
			wrapper = new ObjectWrapper<>("pageAdminFocus.focusDetails", null, object, null, status, this);
		}
		// ObjectWrapper wrapper = new ObjectWrapper("pageUser.userDetails",
		// null, user, status);
		if (wrapper.getResult() != null && !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())) {
			showResultInSession(wrapper.getResult());
		}

		loadParentOrgs(wrapper, task, result);

		wrapper.setShowEmpty(!isEditingFocus());

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Loaded focus wrapper:\n{}", wrapper.debugDump());
		}

		return wrapper;
	}
	
	private void loadParentOrgs(ObjectWrapper<O> wrapper, Task task, OperationResult result) {
		OperationResult subResult = result.createMinorSubresult(OPERATION_LOAD_PARENT_ORGS);
		PrismObject<O> focus = wrapper.getObject();
		// Load parent organizations (full objects). There are used in the
		// summary panel and also in the main form.
		// Do it here explicitly instead of using resolve option to have ability
		// to better handle (ignore) errors.
		for (ObjectReferenceType parentOrgRef : focus.asObjectable().getParentOrgRef()) {

			PrismObject<OrgType> parentOrg = null;
			try {

				parentOrg = getModelService().getObject(OrgType.class, parentOrgRef.getOid(), null, task,
						subResult);
				LOGGER.trace("Loaded parent org with result {}",
						new Object[] { subResult.getLastSubresult() });
			} catch (AuthorizationException e) {
				// This can happen if the user has permission to read parentOrgRef but it does not have
				// the permission to read target org
				// It is OK to just ignore it.
				subResult.muteLastSubresultError();
				LOGGER.debug("User {} does not have permission to read parent org unit {} (ignoring error)", task.getOwner().getName(), parentOrgRef.getOid());
			} catch (Exception ex) {
				subResult.recordWarning("Cannot load parent org " + parentOrgRef.getOid(), ex);
				LOGGER.warn("Cannot load parent org {}: {}", parentOrgRef.getOid(), ex.getMessage(), ex);
			}

			if (parentOrg != null) {
				wrapper.getParentOrgs().add(parentOrg);
			}
		}
		subResult.computeStatus();
	}

	protected abstract Class<? extends Page> getRestartResponsePage();
	
	public Object findParam(String param, String oid, OperationResult result) {

		Object object = null;

		for (OperationResult subResult : result.getSubresults()) {
			if (subResult != null && subResult.getParams() != null) {
				if (subResult.getParams().get(param) != null
						&& subResult.getParams().get(OperationResult.PARAM_OID) != null
						&& subResult.getParams().get(OperationResult.PARAM_OID).equals(oid)) {
					return subResult.getParams().get(param);
				}
				object = findParam(param, oid, subResult);

			}
		}
		return object;
	}

	/**
	 * This will be called from the main form when save button is pressed.
	 */
	public void savePerformed(AjaxRequestTarget target) {
		LOGGER.debug("Save object.");
		progressReporter.onSaveSubmit();
		OperationResult result = new OperationResult(OPERATION_SAVE);
		ObjectWrapper userWrapper = getObjectWrapper();
		// todo: improve, delta variable is quickfix for MID-1006
		// redirecting to user list page everytime user is created in repository
		// during user add in gui,
		// and we're not taking care about account/assignment create errors
		// (error message is still displayed)
		delta = null;

		Task task = createSimpleTask(OPERATION_SEND_TO_SUBMIT);
		
		ModelExecuteOptions options = getExecuteChangesOptions();
		LOGGER.debug("Using execute options {}.", new Object[] { options });

		try {
			reviveModels();

			delta = userWrapper.getObjectDelta();
			if (userWrapper.getOldDelta() != null) {
				delta = ObjectDelta.summarize(userWrapper.getOldDelta(), delta);
			}
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("User delta computed from form:\n{}", new Object[] { delta.debugDump(3) });
			}
		} catch (Exception ex) {
			result.recordFatalError(getString("pageUser.message.cantCreateUser"), ex);
			LoggingUtils.logException(LOGGER, "Create user failed", ex);
			showResult(result);
			return;
		}

		switch (userWrapper.getStatus()) {
			case ADDING:
				try {
					PrismObject<O> objectToAdd = delta.getObjectToAdd();
					WebMiscUtil.encryptCredentials(objectToAdd, true, getMidpointApplication());
					prepareObjectForAdd(objectToAdd);
					getPrismContext().adopt(objectToAdd, getCompileTimeClass());
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Delta before add user:\n{}", new Object[] { delta.debugDump(3) });
					}

					if (!delta.isEmpty()) {
						delta.revive(getPrismContext());

						Collection<SimpleValidationError> validationErrors = performCustomValidation(objectToAdd,
								WebMiscUtil.createDeltaCollection(delta));
						if (validationErrors != null && !validationErrors.isEmpty()) {
							for (SimpleValidationError error : validationErrors) {
								LOGGER.error("Validation error, attribute: '" + error.printAttribute()
										+ "', message: '" + error.getMessage() + "'.");
								error("Validation error, attribute: '" + error.printAttribute()
										+ "', message: '" + error.getMessage() + "'.");
							}

							target.add(getFeedbackPanel());
							return;
						}

						progressReporter.executeChanges(WebMiscUtil.createDeltaCollection(delta), options,
								task, result, target);
					} else {
						result.recordSuccess();
					}
				} catch (Exception ex) {
					result.recordFatalError(getString("pageFocus.message.cantCreateFocus"), ex);
					LoggingUtils.logException(LOGGER, "Create user failed", ex);
				}
				break;

			case MODIFYING:
				try {
					WebMiscUtil.encryptCredentials(delta, true, getMidpointApplication());
					prepareObjectDeltaForModify(delta);

					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Delta before modify user:\n{}", new Object[] { delta.debugDump(3) });
					}

					Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
					if (!delta.isEmpty()) {
						delta.revive(getPrismContext());
						deltas.add(delta);
					}
					
					List<ObjectDelta<? extends ObjectType>> additionalDeltas = getAdditionalModifyDeltas(result);
					if (additionalDeltas != null) {
						for (ObjectDelta additionalDelta : additionalDeltas) {
							if (!additionalDelta.isEmpty()) {
								additionalDelta.revive(getPrismContext());
								deltas.add(additionalDelta);
							}
						}
					}

					if (delta.isEmpty() && ModelExecuteOptions.isReconcile(options)) {
						ObjectDelta emptyDelta = ObjectDelta.createEmptyModifyDelta(getCompileTimeClass(),
								userWrapper.getObject().getOid(), getPrismContext());
						deltas.add(emptyDelta);

						Collection<SimpleValidationError> validationErrors = performCustomValidation(null,
								deltas);
						if (validationErrors != null && !validationErrors.isEmpty()) {
							for (SimpleValidationError error : validationErrors) {
								LOGGER.error("Validation error, attribute: '" + error.printAttribute()
										+ "', message: '" + error.getMessage() + "'.");
								error("Validation error, attribute: '" + error.printAttribute()
										+ "', message: '" + error.getMessage() + "'.");
							}

							target.add(getFeedbackPanel());
							return;
						}

						progressReporter.executeChanges(deltas, options, task, result, target);
					} else if (!deltas.isEmpty()) {
						Collection<SimpleValidationError> validationErrors = performCustomValidation(null,
								deltas);
						if (validationErrors != null && !validationErrors.isEmpty()) {
							for (SimpleValidationError error : validationErrors) {
								LOGGER.error("Validation error, attribute: '" + error.printAttribute()
										+ "', message: '" + error.getMessage() + "'.");
								error("Validation error, attribute: '" + error.printAttribute()
										+ "', message: '" + error.getMessage() + "'.");
							}

							target.add(getFeedbackPanel());
							return;
						}

						progressReporter.executeChanges(deltas, options, task, result, target);
					} else {
						result.recordSuccess();
					}

				} catch (Exception ex) {
					if (!executeForceDelete(userWrapper, task, options, result)) {
						result.recordFatalError(getString("pageUser.message.cantUpdateUser"), ex);
						LoggingUtils.logException(LOGGER, getString("pageUser.message.cantUpdateUser"), ex);
					} else {
						result.recomputeStatus();
					}
				}
				break;
			// support for add/delete containers (e.g. delete credentials)
			default:
				error(getString("pageAdminFocus.message.unsupportedState", userWrapper.getStatus()));
		}

		result.recomputeStatus();

		if (!result.isInProgress()) {
			finishProcessing(target, result);
		}
	}

	protected ModelExecuteOptions getExecuteChangesOptions() {
		return mainPanel.getExecuteChangeOptionsDto().createOptions();
	}

	protected void prepareObjectForAdd(PrismObject<O> object) throws SchemaException {
		
	}
	
	protected void prepareObjectDeltaForModify(ObjectDelta<O> objectDelta) throws SchemaException {
		
	}
	
	protected List<ObjectDelta<? extends ObjectType>> getAdditionalModifyDeltas(OperationResult result) {
		return null;
	}
	
	protected boolean executeForceDelete(ObjectWrapper userWrapper, Task task, ModelExecuteOptions options,
			OperationResult parentResult) {
		return isForce();
	}

	protected boolean isForce() {
		return getMainPanel().getExecuteChangeOptionsDto().isForce();
	}
	
	protected boolean isKeepDisplayingResults() {
		return getMainPanel().getExecuteChangeOptionsDto().isKeepDisplayingResults();
	}
	
	
	protected Collection<SimpleValidationError> performCustomValidation(PrismObject<O> object,
			Collection<ObjectDelta<? extends ObjectType>> deltas) throws SchemaException {
		Collection<SimpleValidationError> errors = null;

		if (object == null) {
			if (getObjectWrapper() != null && getObjectWrapper().getObject() != null) {
				object = getObjectWrapper().getObject();

				for (ObjectDelta delta : deltas) {
					// because among deltas there can be also ShadowType deltas
					if (UserType.class.isAssignableFrom(delta.getObjectTypeClass())) { 
						delta.applyTo(object);
					}
				}
			}
		}

		performAdditionalValidation(object, deltas, errors);

		for (MidpointFormValidator validator : getFormValidatorRegistry().getValidators()) {
			if (errors == null) {
				errors = validator.validateObject(object, deltas);
			} else {
				errors.addAll(validator.validateObject(object, deltas));
			}
		}

		return errors;
	}
	
	protected void performAdditionalValidation(PrismObject<O> object,
			Collection<ObjectDelta<? extends ObjectType>> deltas, Collection<SimpleValidationError> errors) throws SchemaException {
		
	}
	
	// TODO: fix name, confusing. clashes with goBack()
	public void goBackPage() {
		StringValue orgReturn = getPageParameters().get(PARAM_RETURN_PAGE);
        if (PageOrgTree.PARAM_ORG_RETURN.equals(orgReturn.toString())) {
            setResponsePage(getSessionStorage().getPreviousPage());
        } else if (getPreviousPage() != null) {
            goBack(PageDashboard.class);        // the class parameter is not necessary, is previousPage is set
        } else if (getSessionStorage() != null){
        	if (getSessionStorage().getPreviousPageInstance() != null){
        		setResponsePage(getSessionStorage().getPreviousPageInstance());
        	} else if (getSessionStorage().getPreviousPage() != null){
        		setResponsePage(getSessionStorage().getPreviousPage());
        	} else {
        		setResponsePage(getDefaultBackPage());
        	}
        } else {
        	setResponsePage(getDefaultBackPage());
        }
    }
	
	protected abstract PageBase getDefaultBackPage();

	
	// TODO
	private void initButtons(final Form mainForm) {

		AjaxSubmitButton abortButton = new AjaxSubmitButton("abort",
				createStringResource("pageAdminFocus.button.abort")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target,
					org.apache.wicket.markup.html.form.Form<?> form) {
				progressReporter.onAbortSubmit(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target,
					org.apache.wicket.markup.html.form.Form<?> form) {
				target.add(getFeedbackPanel());
			}
		};
		progressReporter.registerAbortButton(abortButton);
		mainForm.add(abortButton);


	}
}