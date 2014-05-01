/**
 * DataCleaner (community edition)
 * Copyright (C) 2013 Human Inference
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.eobjects.datacleaner.monitor.wizard;

import java.util.ArrayList;
import java.util.List;

import org.eobjects.datacleaner.monitor.scheduling.SchedulingService;
import org.eobjects.datacleaner.monitor.scheduling.SchedulingServiceAsync;
import org.eobjects.datacleaner.monitor.scheduling.model.ScheduleDefinition;
import org.eobjects.datacleaner.monitor.scheduling.widgets.CustomizeScheduleClickHandler;
import org.eobjects.datacleaner.monitor.scheduling.widgets.TriggerJobClickHandler;
import org.eobjects.datacleaner.monitor.shared.ClientConfig;
import org.eobjects.datacleaner.monitor.shared.DatastoreService;
import org.eobjects.datacleaner.monitor.shared.DatastoreServiceAsync;
import org.eobjects.datacleaner.monitor.shared.DictionaryClientConfig;
import org.eobjects.datacleaner.monitor.shared.WizardServiceAsync;
import org.eobjects.datacleaner.monitor.shared.model.DCUserInputException;
import org.eobjects.datacleaner.monitor.shared.model.DatastoreIdentifier;
import org.eobjects.datacleaner.monitor.shared.model.TenantIdentifier;
import org.eobjects.datacleaner.monitor.shared.model.WizardIdentifier;
import org.eobjects.datacleaner.monitor.shared.widgets.LoadingIndicator;
import org.eobjects.datacleaner.monitor.util.DCAsyncCallback;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RadioButton;

public class JobWizardController extends AbstractWizardController<WizardServiceAsync> {

    private final SchedulingServiceAsync schedulingService = GWT.create(SchedulingService.class);
    private final DatastoreServiceAsync datastoreService = GWT.create(DatastoreService.class);

    private final ClientConfig clientConfig = new DictionaryClientConfig();

    private DatastoreIdentifier _datastoreIdentifier;
    private List<DatastoreIdentifier> _datastores;
    private List<WizardIdentifier> _wizards;
    private int _stepsBeforeWizardPages;

    private ScheduleDefinition scheduleDefinitionForJob = null;

    public JobWizardController(WizardPanel wizardPanel, TenantIdentifier tenant, WizardIdentifier wizardIdentifier,
            DatastoreIdentifier datastoreIdentifier, WizardServiceAsync wizardService) {
        super(wizardPanel, tenant, wizardIdentifier, wizardService);
        _datastoreIdentifier = datastoreIdentifier;

        _stepsBeforeWizardPages = 0;
        if (wizardIdentifier == null) {
            _stepsBeforeWizardPages++;
        }

        if (_datastoreIdentifier == null) {
            _stepsBeforeWizardPages++;
        }
    }

    @Override
    protected void startWizard() {
        getWizardPanel().addStyleClass("JobWizardPanel");
        getWizardPanel().showWizard();

        final WizardIdentifier wizardIdentifier = getWizardIdentifier();

        if (_datastoreIdentifier == null) {
            if (wizardIdentifier == null || wizardIdentifier.isDatastoreConsumer()) {
                _stepsBeforeWizardPages = 2;
                showDatastoreSelection();
                return;
            }
        }

        if (wizardIdentifier == null) {
            _stepsBeforeWizardPages = 1;
            showWizardSelection();
            return;
        }

        _stepsBeforeWizardPages = 0;
        getWizardPanel().setHeader("Build job: " + wizardIdentifier.getDisplayName());
        setLoading();

        WizardServiceAsync wizardService = getWizardService();
        wizardService.startJobWizard(getTenant(), wizardIdentifier, _datastoreIdentifier, getLocaleName(),
                createNextPageCallback());
    }

    @Override
    protected int getStepsBeforeWizardPages() {
        return _stepsBeforeWizardPages;
    }

    @Override
    protected void wizardFinished(final String jobName) {
        final Button closeButton = new Button("Close");
        closeButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                closeWizardAfterFinishing();
            }
        });

        Panel contentPanel = createWizardFinishedContentPanel(jobName);

        setContent(contentPanel);
        getWizardPanel().getButtonPanel().clear();
        getWizardPanel().getButtonPanel().addButton(closeButton);

    }

    private Panel createWizardFinishedContentPanel(String jobName) {

        final FlowPanel contentPanel = new FlowPanel();
        contentPanel.addStyleName("WizardFinishedPanel");

        final LoadingIndicator loadingIndicator = new LoadingIndicator();
        contentPanel.add(loadingIndicator);
        if (jobName == null) {
            contentPanel.add(new Label("Job created! Wizard finished."));
        } else {
            contentPanel.add(new Label("Job '" + jobName + "' created! Wizard finished."));
        }
        contentPanel.add(new Label("Click 'Close' return, or click one of the links below to start using the job."));

        if (jobName != null) {
            if (clientConfig.isScheduleEditor()) {
                getSchedule(new Runnable() {
                    @Override
                    public void run() {
                        final Anchor triggerAnchor = new Anchor("Run this job now");
                        {
                            triggerAnchor.addStyleName("TriggerJob");
                            ClickHandler clickHandler = new TriggerJobClickHandler(schedulingService, getTenant(),
                                    scheduleDefinitionForJob);
                            clickHandler = new RemoveWizardClickHandler(clickHandler, JobWizardController.this);
                            triggerAnchor.addClickHandler(clickHandler);
                        }

                        final Anchor schedulingAnchor = new Anchor("Set up a job schedule");
                        {
                            schedulingAnchor.addStyleName("ScheduleJob");
                            ClickHandler clickHandler = new CustomizeScheduleClickHandler(null, schedulingService,
                                    getTenant(), scheduleDefinitionForJob);
                            clickHandler = new RemoveWizardClickHandler(clickHandler, JobWizardController.this);
                            schedulingAnchor.addClickHandler(clickHandler);
                        }

                        // TODO: Previously there was a 'Monitor this job's
                        // metrics on the dashboard' anchor as well.

                        contentPanel.add(triggerAnchor);
                        contentPanel.add(schedulingAnchor);
                        contentPanel.remove(loadingIndicator);
                    }
                }, jobName);
            }
        }
        
        return contentPanel;
    }

    protected void closeWizardAfterFinishing() {
        getWizardPanel().hideWizard();
        JavaScriptCallbacks.onWizardFinished();
    }

    private void getSchedule(final Runnable runnable, final String jobName) {
        schedulingService.getSchedules(getTenant(), new DCAsyncCallback<List<ScheduleDefinition>>() {
            @Override
            public void onSuccess(List<ScheduleDefinition> result) {
                for (ScheduleDefinition scheduleDefinition : result) {
                    if (scheduleDefinition.getJob().getName().equals(jobName)) {
                        scheduleDefinitionForJob = scheduleDefinition;
                        runnable.run();
                    }
                }
            }
        });
    }

    protected void showDatastoreSelection() {
        getWizardPanel().setHeader("Build job");

        final FlowPanel outerPanel = new FlowPanel();
        outerPanel.setStyleName("InitialSelectionOuterPanel");
        final List<RadioButton> datastoreRadios = new ArrayList<RadioButton>();
        showDatastoreSelection(outerPanel, datastoreRadios);

        final List<RadioButton> wizardRadios = new ArrayList<RadioButton>();
        showNonDatastoreConsumingWizardSelection(outerPanel, wizardRadios);

        setNextClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                for (int i = 0; i < datastoreRadios.size(); i++) {
                    final RadioButton radio = datastoreRadios.get(i);
                    if (radio.getValue().booleanValue()) {
                        _datastoreIdentifier = _datastores.get(i);
                        showWizardSelection();
                        return;
                    }
                }

                for (int i = 0; i < wizardRadios.size(); i++) {
                    final RadioButton radio = wizardRadios.get(i);
                    if (radio.getValue().booleanValue()) {
                        final WizardIdentifier wizard = _wizards.get(i);
                        setWizardIdentifier(wizard);
                        startWizard();
                        return;
                    }
                }
            }
        });

        setContent(outerPanel);
    }

    private void showNonDatastoreConsumingWizardSelection(final Panel outerPanel, final List<RadioButton> radios) {
        final FlowPanel wizardSelectionPanel = new FlowPanel();
        outerPanel.add(wizardSelectionPanel);

        getWizardService().getNonDatastoreConsumingJobWizardIdentifiers(getTenant(), getLocaleName(),
                new DCAsyncCallback<List<WizardIdentifier>>() {
                    @Override
                    public void onSuccess(List<WizardIdentifier> wizards) {
                        _wizards = wizards;
                        showNonDatastoreConsumingWizardSelection(wizardSelectionPanel, wizards, radios);
                    }
                });
    }

    private void showNonDatastoreConsumingWizardSelection(final FlowPanel panel, final List<WizardIdentifier> wizards,
            final List<RadioButton> radios) {
        if (wizards == null || wizards.isEmpty()) {
            // do nothing
            return;
        }

        panel.add(new Label("Or select a different job type ..."));
        for (final WizardIdentifier wizard : wizards) {
            final RadioButton radio = new RadioButton("initialSelection", wizard.getDisplayName());
            radio.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    GWT.log("Clicked: " + wizard + " - expected " + wizard.getExpectedPageCount() + " pages");
                    _stepsBeforeWizardPages = 1;
                    setSteps(wizard.getExpectedPageCount() + getStepsBeforeWizardPages(), false);
                    setProgress(0);
                }
            });
            panel.add(radio);
            radios.add(radio);
        }

        getWizardPanel().refreshUI();
    }

    private void showDatastoreSelection(final Panel outerPanel, final List<RadioButton> radios) {
        final FlowPanel datastoreSelectionPanel = new FlowPanel();
        outerPanel.add(datastoreSelectionPanel);

        datastoreService.getAvailableDatastores(getTenant(), new DCAsyncCallback<List<DatastoreIdentifier>>() {
            @Override
            public void onSuccess(List<DatastoreIdentifier> datastores) {
                _datastores = datastores;
                showDatastoreSelection(datastoreSelectionPanel, datastores, radios);
            }
        });
    }

    private void showDatastoreSelection(final FlowPanel panel, final List<DatastoreIdentifier> datastores,
            final List<RadioButton> radios) {
        panel.add(new Label("Please select the source datastore of your job ..."));

        for (final DatastoreIdentifier datastore : datastores) {
            final RadioButton radio = new RadioButton("initialSelection", datastore.getName());
            radio.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    GWT.log("Clicked: " + datastore);
                    _stepsBeforeWizardPages = 2;
                    setSteps(getStepsBeforeWizardPages(), true);
                    setProgress(0);
                }
            });
            radios.add(radio);
            panel.add(radio);
        }

        getWizardPanel().refreshUI();
    }

    protected void showWizardSelection() {
        setLoading();

        if (_datastoreIdentifier == null) {
            getWizardPanel().setHeader("Build job");
        } else {
            getWizardPanel().setHeader("Build job: " + _datastoreIdentifier.getName());
        }

        getWizardService().getJobWizardIdentifiers(getTenant(), _datastoreIdentifier, getLocaleName(),
                new DCAsyncCallback<List<WizardIdentifier>>() {
                    @Override
                    public void onSuccess(List<WizardIdentifier> wizards) {
                        showWizardSelection(wizards);
                    }
                });
    }

    protected void showWizardSelection(final List<WizardIdentifier> wizards) {
        final int progress = _stepsBeforeWizardPages - 1;

        final FlowPanel panel = new FlowPanel();

        panel.add(new Label("Please select the type of job to build:"));

        final List<RadioButton> radios = new ArrayList<RadioButton>(wizards.size());

        if (wizards == null || wizards.isEmpty()) {
            panel.add(new Label("(no job wizards available)"));
        } else {
            for (final WizardIdentifier wizard : wizards) {
                final RadioButton radio = new RadioButton("wizardIdentifier", wizard.getDisplayName());
                radio.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        setSteps(wizard.getExpectedPageCount() + getStepsBeforeWizardPages());
                        setProgress(progress);
                    }
                });
                panel.add(radio);
                radios.add(radio);
            }

        }

        setNextClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                for (int i = 0; i < radios.size(); i++) {
                    final RadioButton radio = radios.get(i);
                    if (radio.getValue().booleanValue()) {
                        final WizardIdentifier wizard = wizards.get(i);
                        setWizardIdentifier(wizard);
                        startWizard();
                        return;
                    }
                }

                // no job wizard is selected if we reach this point
                throw new DCUserInputException("Please select a job type to create");
            }
        });

        setProgress(progress);
        setContent(panel);
    }
}