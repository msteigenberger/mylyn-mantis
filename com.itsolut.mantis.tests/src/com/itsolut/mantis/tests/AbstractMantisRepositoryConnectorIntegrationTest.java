/*******************************************************************************
 * Copyright (C) 2010 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.tests;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.rpc.ServiceException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.internal.tasks.core.RepositoryQuery;
import org.eclipse.mylyn.internal.tasks.core.sync.SynchronizationSession;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskDataCollector;
import org.eclipse.mylyn.tasks.core.sync.ISynchronizationSession;

import com.itsolut.mantis.binding.IssueData;
import com.itsolut.mantis.binding.ObjectRef;
import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.MantisCache;
import com.itsolut.mantis.core.MantisRepositoryConnector;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisProject;
import com.itsolut.mantis.core.model.MantisProjectFilter;

public abstract class AbstractMantisRepositoryConnectorIntegrationTest extends AbstractIntegrationTest {
	
	private int queryCounter;

	public void testGetTaskData() throws MalformedURLException, RemoteException, ServiceException, CoreException {

		int firstTaskId = createTask("First task", "Description");

		MantisRepositoryConnector connector = new MantisRepositoryConnector();

		TaskData taskData = connector.getTaskData(repositoryAccessor.getRepository(), String.valueOf(firstTaskId),
				new NullProgressMonitor());

		assertAttributeEquals(taskData.getRoot().getMappedAttribute(TaskAttribute.SUMMARY), "First task");
		assertAttributeEquals(taskData.getRoot().getMappedAttribute(TaskAttribute.DESCRIPTION), "Description");
		assertAttributeEquals(taskData.getRoot().getMappedAttribute(TaskAttribute.STATUS), "new");

	}

	public void testPerformQuery() throws MantisException, MalformedURLException, RemoteException, ServiceException {

		createTask("First task", "Description");
		createTask("Second task", "Description");

		MantisCache cache = repositoryAccessor.getClient().getCache(new NullProgressMonitor());
		MantisProject project = cache.getProjectById(DEFAULT_PROJECT_ID.intValue());

		List<MantisProjectFilter> projectFilters = cache.getProjectFilters(1);

		assertEquals(projectFilters.toString(), 1, projectFilters.size());

		MantisProjectFilter filter = projectFilters.get(0);

		final List<TaskData> hits = new ArrayList<TaskData>();

		MantisRepositoryConnector connector = new MantisRepositoryConnector();

		IRepositoryQuery query = newQuery(connector);
		query.setAttribute(IMantisClient.PROJECT_NAME, project.getName());
		query.setAttribute(IMantisClient.FILTER_NAME, filter.getName());
		TaskDataCollector resultCollector = new TaskDataCollector() {

			@Override
			public void accept(TaskData taskData) {

				hits.add(taskData);
			}
		};
		ISynchronizationSession event = newSession();
		IStatus status = connector.performQuery(repositoryAccessor.getRepository(), query, resultCollector, event,
				new NullProgressMonitor());

		assertEquals("status", Status.OK_STATUS, status);
		assertEquals("hits.size", 2, hits.size());
		for (TaskData taskData : hits) {
			assertNotNull("summary", taskData.getRoot().getMappedAttribute(TaskAttribute.SUMMARY));
			assertNotNull("description", taskData.getRoot().getMappedAttribute(TaskAttribute.DESCRIPTION));
			assertNotNull("status", taskData.getRoot().getMappedAttribute(TaskAttribute.STATUS));
			assertTrue(taskData.isPartial());
		}
	}
	
	public void testPerformQueryAfterUpdate() throws MantisException, MalformedURLException, RemoteException, ServiceException {
		
		int taskId = createTask("First task", "Description");
		
		MantisCache cache = repositoryAccessor.getClient().getCache(new NullProgressMonitor());
		MantisProject project = cache.getProjectById(DEFAULT_PROJECT_ID.intValue());
		
		List<MantisProjectFilter> projectFilters = cache.getProjectFilters(1);
		
		assertEquals(projectFilters.toString(), 1, projectFilters.size());
		
		MantisProjectFilter filter = projectFilters.get(0);
		
		final List<TaskData> hits = new ArrayList<TaskData>();
		
		MantisRepositoryConnector connector = new MantisRepositoryConnector();
		
		IRepositoryQuery query = newQuery(connector);
		query.setAttribute(IMantisClient.PROJECT_NAME, project.getName());
		query.setAttribute(IMantisClient.FILTER_NAME, filter.getName());
		TaskDataCollector resultCollector = new TaskDataCollector() {
			
			@Override
			public void accept(TaskData taskData) {
				
				hits.add(taskData);
			}
		};
		ISynchronizationSession event = newSession();
		IStatus status = connector.performQuery(repositoryAccessor.getRepository(), query, resultCollector, event,
				new NullProgressMonitor());
		
		assertEquals("status", Status.OK_STATUS, status);
		assertEquals("hits.size", 1, hits.size());
		
		IssueData issue = new IssueData();
		issue.setSummary("First task - updated");
		issue.setDescription("Description");
		issue.setProject(new ObjectRef(DEFAULT_PROJECT_ID, ""));
		issue.setCategory(DEFAULT_CATEGORY_NAME);
		repositoryAccessor.getMantisConnectPort().mc_issue_update(getUsername(), getPassword(), BigInteger.valueOf(taskId), issue);
		
		hits.clear();
		
		event = newSession();
		status = connector.performQuery(repositoryAccessor.getRepository(), query, resultCollector, event,
				new NullProgressMonitor());
		
		assertEquals("status", Status.OK_STATUS, status);
		assertEquals("hits.size", 1, hits.size());
		
		assertEquals("First task - updated", hits.get(0).getRoot().getMappedAttribute(TaskAttribute.SUMMARY).getValue());
	}

	private RepositoryQuery newQuery(MantisRepositoryConnector connector) {
		return new RepositoryQuery(connector.getConnectorKind(), "test-" + ++queryCounter);
	}

	private SynchronizationSession newSession() {
		return new SynchronizationSession();
	}

	private void assertAttributeEquals(TaskAttribute mappedAttribute, String expectedValue) {

		assertNotNull("mappedAttribute is null", mappedAttribute);
		assertEquals(expectedValue, mappedAttribute.getValue());
	}

}