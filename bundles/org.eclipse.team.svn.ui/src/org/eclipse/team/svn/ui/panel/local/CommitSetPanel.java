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
 *    Alessandro Nistico - [patch] Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.panel.local;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSet;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.CommentComposite;
import org.eclipse.team.svn.ui.event.IResourceSelectionChangeListener;
import org.eclipse.team.svn.ui.event.ResourceSelectionChangedEvent;
import org.eclipse.team.svn.ui.extension.factory.IModifiableCommentDialogPanel;
import org.eclipse.team.svn.ui.panel.common.CommentPanel;
import org.eclipse.team.svn.ui.panel.local.CommitPanel.CollectPropertiesOperation;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;

/**
 * Commit set panel
 * 
 * @author Alessandro Nistico
 */
public class CommitSetPanel extends CommentPanel implements IModifiableCommentDialogPanel {
	public static final int MSG_CREATE = 0;

	public static final int MSG_EDIT = 1;

	private final ActiveChangeSet set;

	private Text nameText;

	protected IResource[] resources;

	protected List<IResourceSelectionChangeListener> changeListenerList;

	public CommitSetPanel(ActiveChangeSet set, IResource[] resources, int type) {
		super(SVNUIMessages.CommitSetPanel_Title);
		this.set = set;
		this.resources = resources;
		switch (type) {
			case MSG_EDIT:
				dialogDescription = SVNUIMessages.CommitSetPanel_Description_Edit;
				defaultMessage = SVNUIMessages.CommitSetPanel_Message_Edit;
				break;
			default:
				dialogDescription = SVNUIMessages.CommitSetPanel_Description_New;
				defaultMessage = SVNUIMessages.CommitSetPanel_Message_New;
		}

		if (resources == null) {
			resources = set.getResources();
		}

		changeListenerList = new ArrayList<>();
	}

	@Override
	public void createControlsImpl(Composite parent) {
		GridData data = null;
		GridLayout layout = null;

		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout nameLayout = new GridLayout();
		nameLayout.marginWidth = nameLayout.marginHeight = 0;
		nameLayout.numColumns = 2;
		composite.setLayout(nameLayout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		composite.setFont(parent.getFont());

		Label label = new Label(composite, SWT.NONE);
		label.setText(SVNUIMessages.CommitSetPanel_Name);
		label.setLayoutData(new GridData(GridData.BEGINNING));

		nameText = new Text(composite, SWT.BORDER);
		nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		String initialText = set.getTitle();
		if (initialText == null) {
			initialText = ""; //$NON-NLS-1$
		}
		nameText.setText(initialText);
		nameText.selectAll();
		attachTo(nameText, new NonEmptyFieldVerifier(SVNUIMessages.CommitSetPanel_Name_Verifier));

		Group group = new Group(parent, SWT.NULL);
		layout = new GridLayout();
		group.setLayout(layout);
		data = new GridData(GridData.FILL_BOTH);
		group.setLayoutData(data);
		group.setText(SVNUIMessages.CommitSetPanel_Comment);

		CommitPanel.CollectPropertiesOperation op = new CollectPropertiesOperation(resources);
		UIMonitorUtility.doTaskNowDefault(op, true);

		bugtraqModel = op.getBugtraqModel();
		comment = new CommentComposite(group, set.getComment(), this, op.getLogTemplates(), null, op.getMinLogSize(),
				op.getMaxLogWidth());
		data = new GridData(GridData.FILL_BOTH);
		comment.setLayoutData(data);
	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.commitSetDialogContext"; //$NON-NLS-1$
	}

	@Override
	public void addResourcesSelectionChangedListener(IResourceSelectionChangeListener listener) {
		changeListenerList.add(listener);
	}

	@Override
	public void removeResourcesSelectionChangedListener(IResourceSelectionChangeListener listener) {
		changeListenerList.remove(listener);
	}

	public void fireResourcesSelectionChanged(ResourceSelectionChangedEvent event) {
		validateContent();
		IResourceSelectionChangeListener[] listeners = changeListenerList
				.toArray(new IResourceSelectionChangeListener[changeListenerList.size()]);
		for (IResourceSelectionChangeListener listener : listeners) {
			listener.resourcesSelectionChanged(event);
		}
	}

	@Override
	public Point getPrefferedSizeImpl() {
		return new Point(525, SWT.DEFAULT);
	}

	@Override
	protected void saveChangesImpl() {
		super.saveChangesImpl();
		set.setTitle(nameText.getText());
		set.setComment(comment.getMessage());
	}

}
