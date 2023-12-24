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
 *    Alexander Gurov - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.panel.common;

import org.eclipse.core.resources.IResource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.ui.composite.RepositoryResourceSelectionComposite;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;

/**
 * Abstract complementary URL selection panel
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractRepositoryResourceSelectionPanel extends AbstractDialogPanel {
	protected IRepositoryResource selectedResource;

	protected long currentRevision;

	protected boolean filterCurrentRevision;

	protected boolean toFilterCurrent;

	protected String historyKey;

	protected RepositoryResourceSelectionComposite selectionComposite;

	protected String selectionTitle;

	protected String selectionDescription;

	protected int defaultTextType;

	public AbstractRepositoryResourceSelectionPanel(IRepositoryResource baseResource, long currentRevision,
			String title, String proposal, String historyKey, String selectionTitle, String selectionDescription,
			int defaultTextType) {
		dialogTitle = title;
		dialogDescription = proposal;

		this.historyKey = historyKey;
		selectedResource = baseResource;
		this.currentRevision = currentRevision;
		this.selectionTitle = selectionTitle;
		this.selectionDescription = selectionDescription;
		this.defaultTextType = defaultTextType;
		toFilterCurrent = false;
	}

	public void setFilterCurrent(boolean toFilter) {
		toFilterCurrent = toFilter;
	}

	public IRepositoryResource[] getSelection(IResource[] to) {
		IRepositoryResource base = getSelectedResource();
		if (to.length == 1) {
			return new IRepositoryResource[] { base };
		}
		IRepositoryResource[] retVal = new IRepositoryResource[to.length];
		String baseUrl = base.getUrl();
		for (int i = 0; i < retVal.length; i++) {
			String url = baseUrl + "/" + SVNRemoteStorage.instance().asRepositoryResource(to[i]).getName(); //$NON-NLS-1$
			retVal[i] = to[i].getType() == IResource.FILE
					? (IRepositoryResource) base.asRepositoryFile(url, false)
					: base.asRepositoryContainer(url, false);
		}
		return retVal;
	}

	public IRepositoryResource getSelectedResource() {
		return selectedResource;
	}

	@Override
	public void createControlsImpl(Composite parent) {
		GridData data = null;

		selectionComposite = new RepositoryResourceSelectionComposite(parent, SWT.NONE, this, historyKey,
				selectedResource, false, selectionTitle, selectionDescription,
				RepositoryResourceSelectionComposite.MODE_DEFAULT, defaultTextType);
		selectionComposite.setFilterCurrent(toFilterCurrent);
		data = new GridData(GridData.FILL_HORIZONTAL);
		selectionComposite.setLayoutData(data);
		selectionComposite.setCurrentRevision(currentRevision);
	}

	@Override
	protected void saveChangesImpl() {
		selectedResource = selectionComposite.getSelectedResource();
		selectionComposite.saveHistory();
	}

	@Override
	protected void cancelChangesImpl() {
	}

}
