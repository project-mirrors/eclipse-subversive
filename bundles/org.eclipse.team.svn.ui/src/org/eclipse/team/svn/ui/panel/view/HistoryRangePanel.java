/*******************************************************************************
 * Copyright (c) 2005, 2023 Polarion Software and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.panel.view;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.RevisionComposite;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * Allows to define the history fetch range
 * 
 * @author Alexander Gurov
 */
public class HistoryRangePanel extends AbstractDialogPanel {
	protected IRepositoryResource resource;

	protected IRepositoryResource initStartResource;

	protected IRepositoryResource initStopResource;

	protected RevisionComposite startComposite;

	protected RevisionComposite stopComposite;

	protected boolean reversed;

	public HistoryRangePanel(IRepositoryResource resource, SVNRevision initStartRevision,
			SVNRevision initStopRevision) {
		dialogTitle = SVNUIMessages.HistoryRangePanel_Title;
		dialogDescription = SVNUIMessages.HistoryRangePanel_Description;
		defaultMessage = SVNUIMessages.HistoryRangePanel_Message;

		this.resource = resource;
		initStartResource = SVNUtility.copyOf(resource);
		initStartResource.setSelectedRevision(initStartRevision);
		initStopResource = SVNUtility.copyOf(resource);
		initStopResource.setSelectedRevision(initStopRevision);
	}

	@Override
	protected Point getPrefferedSizeImpl() {
		return new Point(715, SWT.DEFAULT);
	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.historyRangeDialogContext"; //$NON-NLS-1$
	}

	public SVNRevision getStartRevision() {
		return (reversed ? stopComposite : startComposite).getSelectedRevision();
	}

	public SVNRevision getStopRevision() {
		return (reversed ? startComposite : stopComposite).getSelectedRevision();
	}

	@Override
	protected void createControlsImpl(Composite parent) {
		GridLayout layout = null;
		GridData data = null;

		Composite cmp = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		cmp.setLayout(layout);
		data = new GridData(GridData.FILL_BOTH);
		cmp.setLayoutData(data);

		String defaultRevision = SVNUIMessages.HistoryRangePanel_Default;
		startComposite = new RevisionComposite(cmp, this, true,
				new String[] { SVNUIMessages.HistoryRangePanel_StartRevision, defaultRevision }, null, false);
		startComposite.setBaseResource(resource);
		startComposite.setSelectedResource(initStartResource);
		data = new GridData(GridData.FILL_HORIZONTAL);
		startComposite.setLayoutData(data);

		stopComposite = new RevisionComposite(cmp, this, true,
				new String[] { SVNUIMessages.HistoryRangePanel_StopRevision, defaultRevision }, null, false);
		stopComposite.setBaseResource(resource);
		stopComposite.setSelectedResource(initStopResource);
		data = new GridData(GridData.FILL_HORIZONTAL);
		stopComposite.setLayoutData(data);
	}

	@Override
	protected void cancelChangesImpl() {

	}

	@Override
	protected void saveChangesImpl() {
		if (getStartRevision() == null || getStopRevision() == null) {
			return;
		}
		initStartResource.setSelectedRevision(getStartRevision());
		initStopResource.setSelectedRevision(getStopRevision());

		UIMonitorUtility.doTaskNowDefault(new AbstractActionOperation("Operation_CheckRevisions", SVNUIMessages.class) { //$NON-NLS-1$
			@Override
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				ISVNConnector proxy = initStartResource.getRepositoryLocation().acquireSVNProxy();
				try {
					reversed = SVNUtility.compareRevisions(
							initStartResource.getSelectedRevision(), initStopResource.getSelectedRevision(),
							SVNUtility.getEntryRevisionReference(initStartResource),
							SVNUtility.getEntryRevisionReference(initStopResource), proxy) == -1;
				} finally {
					initStartResource.getRepositoryLocation().releaseSVNProxy(proxy);
				}
			}
		}, false);
	}

}
