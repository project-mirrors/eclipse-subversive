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
 *    Alexei Goncharov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.properties;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.property.IRevisionPropertiesProvider;
import org.eclipse.team.svn.core.operation.remote.GetRevisionPropertiesOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.svnstorage.events.IRepositoriesStateChangedListener;
import org.eclipse.team.svn.core.svnstorage.events.IRevisionPropertyChangeListener;
import org.eclipse.team.svn.core.svnstorage.events.RepositoriesStateChangedEvent;
import org.eclipse.team.svn.core.svnstorage.events.RevisonPropertyChangeEvent;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.RevisionPropertiesComposite;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

/**
 * Revision properties view
 * 
 * @author Alexei Goncharov
 */
public class RevPropertiesView extends ViewPart
		implements IRepositoriesStateChangedListener, IRevisionPropertyChangeListener {

	public static final String VIEW_ID = RevPropertiesView.class.getName();

	protected IRepositoryLocation location;

	protected SVNRevision revision;

	protected IRevisionPropertiesProvider provider;

	protected IActionOperation toPerform;

	protected RevisionPropertiesComposite revPropComposite;

	public RevPropertiesView() {
		SVNRemoteStorage.instance().addRepositoriesStateChangedListener(this);
		SVNRemoteStorage.instance().addRevisionPropertyChangeListener(this);
	}

	public void setLocationAndRevision(IRepositoryLocation location, SVNRevision revision) {
		this.location = location;
		this.revision = revision;
		revPropComposite.setLocationAndRevision(location, revision);
		refreshView();
	}

	public void refreshView() {
		if (location == null || revision == null) {
			setContentDescription(""); //$NON-NLS-1$
			return;
		}
		setContentDescription(BaseMessages.format(SVNUIMessages.RevisionPropertyView_Decript,
				new String[] { String.valueOf(location), String.valueOf(revision) }));
		revPropComposite.setPending(true);
		CompositeOperation op = new CompositeOperation("ShowRevProp", SVNUIMessages.class); //$NON-NLS-1$
		op.add((IActionOperation) (provider = new GetRevisionPropertiesOperation(location, revision)));
		op.add(new AbstractActionOperation("ShowRevProp", SVNUIMessages.class) { //$NON-NLS-1$
			@Override
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				revPropComposite.setInput(provider.getRevisionProperties());
				revPropComposite.setPending(false);
			}
		});
		UIMonitorUtility.doTaskScheduledDefault(op);
	}

	@Override
	public void createPartControl(Composite parent) {
		IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
		tbm.removeAll();
		Action action = new Action(SVNUIMessages.SVNView_Refresh_Label) {
			@Override
			public void run() {
				RevPropertiesView.this.refreshView();
			}
		};
		action.setImageDescriptor(SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/refresh.gif")); //$NON-NLS-1$
		tbm.add(action);
		tbm.update(true);

		revPropComposite = new RevisionPropertiesComposite(parent);
		refreshView();

		//Setting context help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.team.svn.help.revPropertiesViewContext"); //$NON-NLS-1$
	}

	protected void disconnectView() {
		location = null;
		revision = null;
		revPropComposite.setInput(new SVNProperty[0]);
		UIMonitorUtility.getDisplay().syncExec(() -> RevPropertiesView.this.setContentDescription(""));
	}

	@Override
	public void dispose() {
		SVNRemoteStorage.instance().removeRepositoriesStateChangedListener(this);
		SVNRemoteStorage.instance().removeRevisionPropertyChangeListener(this);
		super.dispose();
	}

	@Override
	public void setFocus() {
	}

	@Override
	public void repositoriesStateChanged(RepositoriesStateChangedEvent event) {
		if (event.getLocation().equals(location)) {
			if (event.getAction() == RepositoriesStateChangedEvent.REMOVED) {
				RevPropertiesView.this.disconnectView();
			}
		}
	}

	@Override
	public void revisionPropertyChanged(RevisonPropertyChangeEvent event) {
		if (location == null || !location.equals(event.getLocation())) {
			return;
		}
		ArrayList<SVNProperty> props = new ArrayList<>(Arrays.asList(revPropComposite.getSetProps()));
		boolean isNewProp = true;
		for (SVNProperty current : props) {
			if (current.name.equals(event.getProperty().name)) {
				int idx = props.indexOf(current);
				props.set(idx, event.getProperty());
				isNewProp = false;
			}
		}
		if (isNewProp) {
			props.add(event.getProperty());
		}
		final SVNProperty[] toSet = props.toArray(new SVNProperty[props.size()]);
		UIMonitorUtility.getDisplay().syncExec(() -> revPropComposite.setInput(toSet));
	}

}
