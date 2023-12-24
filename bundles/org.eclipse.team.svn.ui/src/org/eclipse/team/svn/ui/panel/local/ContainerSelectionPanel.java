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
 *    Sergiy Logvin - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.panel.local;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.SVNContainerSelectionGroup;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.panel.IDialogManager;
import org.eclipse.team.svn.ui.verifier.AbstractFormattedVerifier;
import org.eclipse.team.svn.ui.verifier.AbstractVerifierProxy;
import org.eclipse.team.svn.ui.verifier.CompositeVerifier;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;
import org.eclipse.team.svn.ui.verifier.ResourceNameVerifier;
import org.eclipse.ui.internal.ide.misc.ContainerSelectionGroup;

/**
 * Container selection panel implementation
 * 
 * @author Sergiy Logvin
 */
public class ContainerSelectionPanel extends AbstractDialogPanel {
	protected ContainerSelectionGroup group;

	protected Button copyWithHistoryButton;

	protected IContainer initialRoot;

	protected Text nameBox;

	protected Button overrideResourceNameButton;

	protected IResource[] resources;

	protected IPath selectedPath;

	protected String defaultConflictMessage;

	protected int numConflicts;

	protected boolean copyWithHistorySelected;

	protected String name;

	protected boolean overrideResourceName;

	public ContainerSelectionPanel(IResource[] resources, HashSet conflicts) {
		dialogTitle = SVNUIMessages.ContainerSelectionPanel_Title;
		dialogDescription = SVNUIMessages.ContainerSelectionPanel_Description;
		selectedPath = null;
		initialRoot = resources[0].getParent();
		this.resources = resources;
		numConflicts = conflicts.size();
		defaultConflictMessage = ""; //$NON-NLS-1$
		getDefaultConflictMessage(conflicts);
		defaultConflictMessage = getDefaultConflictMessage(conflicts);
		defaultMessage = conflicts.size() == 0 ? SVNUIMessages.ContainerSelectionPanel_Message : defaultConflictMessage;
	}

	public IPath getSelectedPath() {
		return selectedPath;
	}

	public boolean isOverrideResourceName() {
		return overrideResourceName;
	}

	public String getOverridenName() {
		return name;
	}

	public boolean isCopyWithHistorySelected() {
		return copyWithHistorySelected;
	}

	@Override
	public void createControlsImpl(Composite parent) {
		GridData data = null;

		group = new SVNContainerSelectionGroup(parent, event -> ContainerSelectionPanel.this.validateContent());
		group.setLayoutData(new GridData(GridData.FILL_BOTH));
		attachTo(group, new ContainerSelectionVerifier(defaultConflictMessage));

		overrideResourceNameButton = new Button(parent, SWT.CHECK);
		overrideResourceNameButton.setLayoutData(new GridData());
		overrideResourceNameButton.setText(SVNUIMessages.ContainerSelectionPanel_NewName);
		overrideResourceNameButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				nameBox.setEnabled(((Button) e.widget).getSelection());
				ContainerSelectionPanel.this.validateContent();
			}
		});
		overrideResourceNameButton.setSelection(false);

		nameBox = new Text(parent, SWT.BORDER | SWT.SINGLE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		nameBox.setLayoutData(data);
		nameBox.setText(resources[0].getName());
		nameBox.setEnabled(false);
		CompositeVerifier verifier = new CompositeVerifier();
		String name = SVNUIMessages.ContainerSelectionPanel_NewName_Verifier;
		verifier.add(new NonEmptyFieldVerifier(name));
		verifier.add(new ResourceNameVerifier(name, true));
		verifier.add(new AbstractFormattedVerifier(name) {
			@Override
			protected String getWarningMessageImpl(Control input) {
				return null;
			}

			@Override
			protected String getErrorMessageImpl(Control input) {
				IPath path = group.getContainerFullPath();
				if (path != null
						&& ResourcesPlugin.getWorkspace().getRoot().findMember(path.append(getText(input))) != null) {
					return SVNUIMessages.ContainerSelectionPanel_NewName_Verifier_Error;
				}
				return null;
			}
		});
		attachTo(nameBox, new AbstractVerifierProxy(verifier) {
			@Override
			protected boolean isVerificationEnabled(Control input) {
				return overrideResourceNameButton.getSelection();
			}
		});

		copyWithHistoryButton = new Button(parent, SWT.RADIO);
		copyWithHistoryButton.setLayoutData(new GridData());
		copyWithHistoryButton.setText(SVNUIMessages.ContainerSelectionPanel_PerformSVNCopy);
		copyWithHistoryButton.setSelection(true);
		copyWithHistoryButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ContainerSelectionPanel.this.validateContent();
			}
		});

		Button noSVNCopyButton = new Button(parent, SWT.RADIO);
		noSVNCopyButton.setLayoutData(new GridData());
		noSVNCopyButton.setText(SVNUIMessages.ContainerSelectionPanel_CopyResourcesWithoutSVNHistory);
		noSVNCopyButton.setSelection(false);
	}

	@Override
	public void postInit() {
		super.postInit();
		if (initialRoot != null) {
			group.setSelectedContainer(initialRoot);
		}
		if (numConflicts == 0) {
			manager.setMessage(IDialogManager.LEVEL_OK, null);
		} else {
			manager.setMessage(IDialogManager.LEVEL_WARNING, null);
		}
	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.copyMoveToDialogContext"; //$NON-NLS-1$
	}

	@Override
	protected void saveChangesImpl() {
		selectedPath = group.getContainerFullPath();
		copyWithHistorySelected = copyWithHistoryButton.getSelection();
		name = nameBox.getText().trim();
		overrideResourceName = overrideResourceNameButton.getSelection();
	}

	@Override
	protected void cancelChangesImpl() {
	}

	protected String getDefaultConflictMessage(HashSet conflicts) {
		if (conflicts.size() == 0) {
			return null;
		}
		int numberOfConflicts = 0;
		String message = ""; //$NON-NLS-1$
		for (Iterator iter = conflicts.iterator(); iter.hasNext();) {
			String element = (String) iter.next();
			numberOfConflicts++;
			if (numberOfConflicts < 4) {
				message += numberOfConflicts == 1 ? "'" + element + "'" : ", '" + element + "'"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
		}
		if (numberOfConflicts >= 4) {
			message += "..."; //$NON-NLS-1$
		}
		return BaseMessages.format(SVNUIMessages.ContainerSelectionPanel_ConflictMessage, new String[] { message });
	}

	public class ContainerSelectionVerifier extends SVNContainerSelectionGroup.SVNContainerSelectionVerifier {
		protected String SOME_RESOURCES_IN_CONFLICT_MESSAGE;

		protected String SOME_RESOURCE_IN_CONFLICT_MESSAGE;

		protected String ALL_RESOURCES_IN_CONFLICT_MESSAGE;

		protected String conflictedResources;

		protected String defaultConflictingResourcesNames;

		public ContainerSelectionVerifier(String conflictingResourcesNames) {
			conflictedResources = ""; //$NON-NLS-1$
			defaultConflictingResourcesNames = conflictingResourcesNames;
			ALL_RESOURCES_IN_CONFLICT_MESSAGE = SVNUIMessages.ContainerSelectionPanel_Selection_Verifier_AllInConflict;
			SOME_RESOURCE_IN_CONFLICT_MESSAGE = SVNUIMessages.ContainerSelectionPanel_Selection_Verifier_SomeInConflict_Single;
			SOME_RESOURCES_IN_CONFLICT_MESSAGE = SVNUIMessages.ContainerSelectionPanel_Selection_Verifier_SomeInConflict_Multi;
		}

		@Override
		protected String getErrorMessageImpl(Control input) {
			SVNContainerSelectionGroup control = (SVNContainerSelectionGroup) input;
			if (findConflicts(control) == resources.length) {
				return ALL_RESOURCES_IN_CONFLICT_MESSAGE;
			}
			return super.getErrorMessageImpl(input);
		}

		@Override
		protected String getWarningMessageImpl(Control input) {
			SVNContainerSelectionGroup control = (SVNContainerSelectionGroup) input;
			int numberconflicts = findConflicts(control);
			if (numberconflicts == 1) {
				return BaseMessages.format(SOME_RESOURCE_IN_CONFLICT_MESSAGE, new Object[] { conflictedResources });
			}
			if (numberconflicts > 1) {
				return BaseMessages.format(SOME_RESOURCES_IN_CONFLICT_MESSAGE, new Object[] { conflictedResources });
			}
			if (defaultConflictingResourcesNames != null) {
				return defaultConflictingResourcesNames;
			}

			return null;
		}

		protected int findConflicts(SVNContainerSelectionGroup control) {
			IPath containerPath = control.getContainerFullPath();
			if (containerPath == null) {
				return 0;
			}
			if (overrideResourceNameButton.getSelection()) {
				containerPath = containerPath.append(nameBox.getText().trim());
				if (resources.length == 1) {
					if (ResourcesPlugin.getWorkspace().getRoot().findMember(containerPath) != null) {
						conflictedResources = containerPath.lastSegment();
						return 1;
					}
				}
			}
			ArrayList<IResource> destResources = new ArrayList<>();
			int numberOfConflictedResources = 0;
			for (IResource element : resources) {
				IPath dest = containerPath.append(element.getName());
				IResource destinationResource = ResourcesPlugin.getWorkspace().getRoot().findMember(dest);
				if (destinationResource != null) {
					numberOfConflictedResources++;
					destResources.add(destinationResource);
				}
			}
			conflictedResources = FileUtility.getNamesListAsString(destResources.toArray());
			return numberOfConflictedResources;
		}

		@Override
		protected boolean isNonSVNCheckDisabled() {
			return copyWithHistoryButton.getSelection();
		}

	}

}
