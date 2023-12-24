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

package org.eclipse.team.svn.ui.crashrecovery.invalidmeta;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.factory.ISVNConnectorFactory;
import org.eclipse.team.svn.core.extension.factory.SVNConnectorHelper;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;

/**
 * The panel allows us to acquire user solution about which valid connector we should use in order to access the project.
 * 
 * @author Alexander Gurov
 */
public class ValidConnectorsSelectionPanel extends AbstractDialogPanel {
	protected Combo svnConnectorField;

	protected ISVNConnectorFactory[] factories;

	protected String svnConnector;

	public ValidConnectorsSelectionPanel(IProject project, List validClients) {
		dialogTitle = BaseMessages.format(SVNUIMessages.ValidConnectorsSelectionPanel_Title,
				new String[] { project.getName() });
		dialogDescription = SVNUIMessages.ValidConnectorsSelectionPanel_Description;
		defaultMessage = SVNUIMessages.ValidConnectorsSelectionPanel_Message;

		factories = (ISVNConnectorFactory[]) validClients.toArray(new ISVNConnectorFactory[validClients.size()]);
	}

	@Override
	public Point getPrefferedSizeImpl() {
		return new Point(500, 60);
	}

	@Override
	public void postInit() {
		super.postInit();
		svnConnector = factories[svnConnectorField.getSelectionIndex()].getId();
	}

	@Override
	protected void createControlsImpl(Composite parent) {
		GridLayout layout = null;
		GridData data = null;

		Composite composite = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = layout.marginWidth = 0;
		composite.setLayout(layout);
		data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);

		Label label = new Label(composite, SWT.NULL);
		data = new GridData();
		label.setLayoutData(data);
		label.setText(SVNUIMessages.ValidConnectorsSelectionPanel_Clients);

		svnConnectorField = new Combo(composite, SWT.READ_ONLY);
		data = new GridData(GridData.FILL_HORIZONTAL);
		svnConnectorField.setLayoutData(data);
		Arrays.sort(factories, (o1, o2) -> ((ISVNConnectorFactory) o1).getName().compareTo(((ISVNConnectorFactory) o2).getName()));
		String[] items = new String[factories.length];
		for (int i = 0; i < items.length; i++) {
			items[i] = SVNConnectorHelper.getConnectorName(factories[i]);
		}
		svnConnectorField.setItems(items);
		svnConnectorField.select(0);
		svnConnectorField.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				svnConnector = factories[svnConnectorField.getSelectionIndex()].getId();
			}
		});
	}

	@Override
	protected void cancelChangesImpl() {
	}

	@Override
	protected void saveChangesImpl() {
		String oldId = CoreExtensionsManager.instance().getSVNConnectorFactory().getId();
		if (!oldId.equals(svnConnector)) {
			SVNTeamPreferences.setCoreString(SVNTeamUIPlugin.instance().getPreferenceStore(),
					SVNTeamPreferences.CORE_SVNCONNECTOR_NAME, svnConnector);
			SVNTeamUIPlugin.instance().savePreferences();
			// destroy all cached proxies
			SVNRemoteStorage.instance().dispose();
		}
	}

}
