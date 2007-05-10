/*******************************************************************************
 * Copyright (c) 2003 - 2006 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2007 - 2007 IT Solutions, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Chris Hane - adapted Trac implementation for Mantis
 *******************************************************************************/
package com.itsolut.mantis.ui.wizard;

import java.net.MalformedURLException;

import org.eclipse.mylar.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.mylar.tasks.ui.search.AbstractRepositoryQueryPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.MantisRepositoryConnector;
import com.itsolut.mantis.core.MantisRepositoryQuery;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisProject;
import com.itsolut.mantis.core.model.MantisProjectFilter;
import com.itsolut.mantis.core.model.MantisSearch;
import com.itsolut.mantis.core.model.MantisSearchFilter;
import com.itsolut.mantis.core.model.MantisTicket.Key;
import com.itsolut.mantis.core.util.MantisUtils;

/**
 * Mantis search page. Provides a form similar to the one the Bugzilla connector
 * uses.
 * 
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class MantisCustomQueryPage extends AbstractRepositoryQueryPage {

	private static final String TITLE = "Enter query parameters";

	private static final String DESCRIPTION = "Only predefined filters are supported.";

	private static final String TITLE_QUERY_TITLE = "Query Title:";

	private MantisRepositoryQuery query;

	private Text titleText;

	protected Combo projectCombo = null;
	
	protected Combo filterCombo = null;

	public MantisCustomQueryPage(TaskRepository repository, AbstractRepositoryQuery query) {
		super(TITLE);

		this.repository = repository;
		this.query = (MantisRepositoryQuery) query;

		setTitle(TITLE);
		setDescription(DESCRIPTION);
	}

	public MantisCustomQueryPage(TaskRepository repository) {
		this(repository, null);
	}

	@Override
	public void createControl(Composite parent) {

		Composite control = new Composite(parent, SWT.NONE);
		GridData gd = new GridData(GridData.FILL_BOTH);
		control.setLayoutData(gd);
		GridLayout layout = new GridLayout(1, false);
		control.setLayout(layout);

		createTitleGroup(control);

		projectCombo = new Combo(control, SWT.READ_ONLY);
		projectCombo.add("Select Project for new Issue");
		
		try {
			MantisRepositoryConnector connector = (MantisRepositoryConnector)TasksUiPlugin.getRepositoryManager().getRepositoryConnector(MantisCorePlugin.REPOSITORY_KIND);
			IMantisClient client = connector.getClientManager().getRepository(repository);
			
			for(MantisProject pd : client.getProjects()){
				projectCombo.add(pd.getName());
			}
			projectCombo.setText(projectCombo.getItem(0));
			
			projectCombo.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					try {
						filterCombo.remove(1, filterCombo.getItemCount()-1);
						if(projectCombo.getSelectionIndex()>0){
							MantisRepositoryConnector connector = (MantisRepositoryConnector)TasksUiPlugin.getRepositoryManager().getRepositoryConnector(MantisCorePlugin.REPOSITORY_KIND);
							IMantisClient client = connector.getClientManager().getRepository(repository);
							for(MantisProjectFilter pd : client.getProjectFilters(projectCombo.getText())){
								filterCombo.add(pd.getName());
							}
						}
					} catch (Exception e1) {
						MantisCorePlugin.log(e1);
					}
					getWizard().getContainer().updateButtons();
				}

				public void widgetDefaultSelected(SelectionEvent e) {
					//nothing
				}
			});
			
			filterCombo = new Combo(control, SWT.READ_ONLY);
			filterCombo.add("Select Filter in Project");
			filterCombo.setText(filterCombo.getItem(0));
			
			filterCombo.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					getWizard().getContainer().updateButtons();
				}

				public void widgetDefaultSelected(SelectionEvent e) {
					//nothing
				}
			});

			if (query != null) {
				titleText.setText(query.getSummary());
				restoreSearchFilterFromQuery(query);
			}
		} catch (Exception e1) {
			MantisCorePlugin.log(e1);
		}
		

		setControl(control);
	}

	@Override
	public boolean canFlipToNextPage() {
		return false;
	}

	private void restoreSearchFilterFromQuery(MantisRepositoryQuery query) throws MalformedURLException, MantisException {
		for(MantisSearchFilter filter : query.getMantisSearch().getFilters()){
			if("project".equals(filter.getFieldName())){
				projectCombo.setText(filter.getValues().get(0));
				
			} else if("filter".equals(filter.getFieldName())){
				MantisRepositoryConnector connector = (MantisRepositoryConnector)TasksUiPlugin.getRepositoryManager().getRepositoryConnector(MantisCorePlugin.REPOSITORY_KIND);
				IMantisClient client = connector.getClientManager().getRepository(repository);
				for(MantisProjectFilter pd : client.getProjectFilters(projectCombo.getText())){
					filterCombo.add(pd.getName());
				}
				
				filterCombo.setText(filter.getValues().get(0));
				
			}
		}
	}

	private void createTitleGroup(Composite control) {
		if (inSearchContainer()) {
			return;
		}

		Label titleLabel = new Label(control, SWT.NONE);
		titleLabel.setText(TITLE_QUERY_TITLE);

		titleText = new Text(control, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		gd.horizontalSpan = 2;
		titleText.setLayoutData(gd);
		titleText.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				// ignore
			}

			public void keyReleased(KeyEvent e) {
				getContainer().updateButtons();
			}
		});
	}

	public TaskRepository getRepository() {
		return repository;
	}

	public void setRepository(TaskRepository repository) {
		this.repository = repository;
	}

	@Override
	public boolean isPageComplete() {
		if (titleText != null && titleText.getText().length() > 0) {
			return true;
		}
		return false;
	}

	public String getQueryUrl(String repsitoryUrl) {
		MantisSearch search = new MantisSearch();

		search.addFilter(new MantisSearchFilter("project", projectCombo.getText()));
		search.addFilter(new MantisSearchFilter("filter", filterCombo.getText()));

		StringBuilder sb = new StringBuilder();
		sb.append(repsitoryUrl);
		sb.append(IMantisClient.QUERY_URL);
		sb.append(search.toUrl());
		return sb.toString();
	}

	@Override
	public MantisRepositoryQuery getQuery() {
		return new MantisRepositoryQuery(repository.getUrl(), getQueryUrl(repository.getUrl()), getTitleText(),
				TasksUiPlugin.getTaskListManager().getTaskList());
	}

	private String getTitleText() {
		return (titleText != null) ? titleText.getText() : "<search>";
	}

}