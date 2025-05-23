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

package org.eclipse.team.svn.ui.compare;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.connector.SVNDiffStatus;
import org.eclipse.team.svn.core.connector.SVNEntry;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNEntryStatus;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.DiffViewerSettings;
import org.eclipse.team.svn.core.operation.local.DiffViewerSettings.ExternalProgramParameters;
import org.eclipse.team.svn.core.operation.local.RunExternalCompareOperation.DefaultExternalProgramParametersProvider;
import org.eclipse.team.svn.core.operation.local.RunExternalCompareOperation.DetectExternalCompareOperationHelper;
import org.eclipse.team.svn.core.operation.remote.RunExternalRepositoryCompareOperation.ExternalCompareRepositoryOperation;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.svnstorage.SVNRepositoryResource;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.operation.UILoggedOperation;
import org.eclipse.team.svn.ui.preferences.SVNTeamDiffViewerPage;
import org.eclipse.team.svn.ui.utility.OverlayedImageDescriptor;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * Two way resource compare input
 * 
 * @author Alexander Gurov
 */
public class TwoWayResourceCompareInput extends ResourceCompareInput {
	protected SVNDiffStatus[] statuses;

	public TwoWayResourceCompareInput(CompareConfiguration configuration, IRepositoryResource next,
			IRepositoryResource prev, Collection<SVNDiffStatus> statuses) {
		super(configuration);

		rootLeft = SVNUtility.copyOf(next);
		rootRight = SVNUtility.copyOf(prev);
		this.statuses = statuses.toArray(new SVNDiffStatus[statuses.size()]);
	}

	@Override
	protected void fillMenu(IMenuManager manager, TreeSelection selection) {
		final CompareNode selectedNode = (CompareNode) selection.getFirstElement();
		Action tAction = null;
		manager.add(tAction = new Action(SVNUIMessages.SynchronizeActionGroup_CompareProperties) {
			@Override
			public void run() {
				SVNRepositoryResource repoResource = (SVNRepositoryResource) ((ResourceElement) selectedNode.getLeft())
						.getRepositoryResource();
				SVNEntryRevisionReference leftReference = new SVNEntryRevisionReference(
						repoResource.getUrl(), repoResource.getPegRevision(), repoResource.getSelectedRevision());
				repoResource = (SVNRepositoryResource) ((ResourceElement) selectedNode.getRight())
						.getRepositoryResource();
				SVNEntryRevisionReference rightReference = new SVNEntryRevisionReference(
						repoResource.getUrl(), repoResource.getPegRevision(), repoResource.getSelectedRevision());
				TwoWayPropertyCompareInput input = new TwoWayPropertyCompareInput(
						new CompareConfiguration(), leftReference, rightReference,
						repoResource.getRepositoryLocation());
				try {
					input.run(new NullProgressMonitor());
					if (input.getCompareResult() == null) {
						MessageDialog dialog = new MessageDialog(
								UIMonitorUtility.getShell(), SVNUIMessages.ComparePropsNoDiff_Title, null,
								SVNUIMessages.ComparePropsNoDiff_Message, MessageDialog.INFORMATION,
								new String[] { IDialogConstants.OK_LABEL }, 0);
						dialog.open();
					} else {
						PropertyComparePanel panel = new PropertyComparePanel(input, false);
						DefaultDialog dlg = new DefaultDialog(UIMonitorUtility.getShell(), panel);
						dlg.open();
					}
				} catch (Exception ex) {
					UILoggedOperation.reportError("Compare Properties Operation", ex);
				}
			}
		});
		tAction.setEnabled(
				selection.size() == 1 && (selectedNode.getKind() & Differencer.CHANGE_TYPE_MASK) != Differencer.ADDITION
						&& (selectedNode.getKind() & Differencer.CHANGE_TYPE_MASK) != Differencer.DELETION);

		//external compare action
		Action externalCompareAction = getOpenInExternalCompareEditorAction(selectedNode, selection);
		manager.add(externalCompareAction);
	}

	protected Action getOpenInExternalCompareEditorAction(final CompareNode selectedNode,
			final TreeSelection selection) {
		final IRepositoryResource leftResource = ((ResourceElement) selectedNode.getLeft()).getRepositoryResource();
		final IRepositoryResource rightResource = ((ResourceElement) selectedNode.getRight()).getRepositoryResource();

		DiffViewerSettings diffSettings = SVNTeamDiffViewerPage.loadDiffViewerSettings();
		DetectExternalCompareOperationHelper detectCompareHelper = new DetectExternalCompareOperationHelper(
				leftResource, diffSettings, true);
		detectCompareHelper.execute(new NullProgressMonitor());

		final ExternalProgramParameters externalProgramParams = detectCompareHelper.getExternalProgramParameters();

		boolean isEnabled = selection.size() == 1 && externalProgramParams != null;

		Action action = new Action(SVNUIMessages.OpenInExternalCompareEditor_Action) {
			@Override
			public void run() {
				if (externalProgramParams != null) {
					IActionOperation op = new ExternalCompareRepositoryOperation(leftResource, rightResource,
							new DefaultExternalProgramParametersProvider(externalProgramParams));
					UIMonitorUtility.doTaskScheduledActive(op);
				}
			}
		};
		action.setEnabled(isEnabled);

		return action;
	}

	@Override
	public void initialize(IProgressMonitor monitor) throws Exception {
		SVNUtility.reorder(statuses, true);

		HashMap path2node = new HashMap();
		String message = SVNUIMessages.ResourceCompareInput_CheckingDelta;
		for (int i = 0; i < statuses.length; i++) {
			monitor.subTask(
					BaseMessages.format(message, new Object[] { SVNUtility.decodeURL(statuses[i].pathPrev) }));

			CompareNode node = makeNode(statuses[i], path2node, monitor);
			path2node.put(SVNUtility
					.createPathForSVNUrl(((ResourceElement) node.getRight()).getRepositoryResource().getUrl()), node);

			ProgressMonitorUtility.progress(monitor, i, statuses.length);
		}

		findRootNode(path2node, rootRight, monitor);
		if (root == null) {
			findRootNode(path2node, rootLeft, monitor);
		}

		super.initialize(monitor);
	}

	protected CompareNode makeNode(SVNDiffStatus st, Map path2node, IProgressMonitor monitor) throws Exception {
		String urlNext = SVNUtility.decodeURL(st.pathNext);
		String urlPrev = SVNUtility.decodeURL(st.pathPrev);
		SVNEntry.Kind nodeKind = getNodeKind(st, false);

		IRepositoryResource next = createResourceFor(rootLeft.getRepositoryLocation(), nodeKind, urlNext);
		next.setSelectedRevision(rootLeft.getSelectedRevision());
		next.setPegRevision(rootLeft.getPegRevision());

		IRepositoryResource prev = createResourceFor(rootRight.getRepositoryLocation(), nodeKind, urlPrev);
		prev.setSelectedRevision(rootRight.getSelectedRevision());
		prev.setPegRevision(rootRight.getPegRevision());

		IDiffContainer parent = getParentCompareNode(prev, path2node);

		// invert diffKind in order to make compare view the same as Eclipse "Compare Each Other"
		int diffKind = ResourceCompareInput.getDiffKind(st.textStatus, st.propStatus);
		diffKind = diffKind == Differencer.DELETION
				? Differencer.ADDITION
				: diffKind == Differencer.ADDITION ? Differencer.DELETION : diffKind;

		return new CompareNode(parent, diffKind, next, prev,
				st.textStatus == SVNEntryStatus.Kind.NORMAL ? st.propStatus : st.textStatus);
	}

	@Override
	protected IDiffContainer makeStubNode(IDiffContainer parent, IRepositoryResource node) {
		IRepositoryResource next = node;

		String prevUrl = node.getUrl();
		if (prevUrl.length() > rootRight.getUrl().length()) {
			String urlPart = prevUrl.substring(rootRight.getUrl().length());
			String urlNext = rootLeft.getUrl() + urlPart;

			next = createResourceFor(rootLeft.getRepositoryLocation(),
					node instanceof IRepositoryFile ? SVNEntry.Kind.FILE : SVNEntry.Kind.DIR, urlNext);
			next.setSelectedRevision(rootLeft.getSelectedRevision());
			next.setPegRevision(rootLeft.getPegRevision());
		}

		return new CompareNode(parent, Differencer.NO_CHANGE, next, node, SVNEntryStatus.Kind.NORMAL);
	}

	@Override
	protected boolean isThreeWay() {
		return false;
	}

	@Override
	protected ResourceCompareViewer createDiffViewerImpl(Composite parent, CompareConfiguration config) {
		return new ResourceCompareViewer(parent, config) {
			@Override
			public void setLabelProvider(IBaseLabelProvider labelProvider) {
				super.setLabelProvider(new LabelProviderWrapper((ILabelProvider) labelProvider) {
					@Override
					public Image getImage(Object element) {
						if (element instanceof CompareNode
								&& ((CompareNode) element).getChangeType() == SVNEntryStatus.Kind.REPLACED) {
							Image image = images.get(element);
							if (image == null) {
								OverlayedImageDescriptor imageDescriptor = new OverlayedImageDescriptor(
										baseProvider.getImage(element),
										SVNTeamUIPlugin.instance()
												.getImageDescriptor("icons/overlays/replaced_2way.gif"), //$NON-NLS-1$
										new Point(22, 16),
										OverlayedImageDescriptor.RIGHT | OverlayedImageDescriptor.CENTER_V);
								images.put(element, image = imageDescriptor.createImage());
							}
							return image;
						}
						return super.getImage(element);
					}
				});
			}
		};
	}

	protected class CompareNode extends BaseCompareNode {
		protected SVNEntryStatus.Kind changeType;

		public CompareNode(IDiffContainer parent, int kind, IRepositoryResource next, IRepositoryResource prev,
				SVNEntryStatus.Kind changeType) {
			super(parent, kind);

			this.changeType = changeType;

			setRight(new ResourceElement(prev, null, changeType != SVNEntryStatus.Kind.ADDED));
			setLeft(new ResourceElement(next, null, changeType != SVNEntryStatus.Kind.DELETED));
		}

		public SVNEntryStatus.Kind getChangeType() {
			return changeType;
		}

	}

}
