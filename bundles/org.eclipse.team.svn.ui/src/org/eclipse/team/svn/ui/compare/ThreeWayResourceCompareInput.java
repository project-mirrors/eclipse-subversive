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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
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
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.connector.SVNDiffStatus;
import org.eclipse.team.svn.core.connector.SVNEntry;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNEntryStatus;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.connector.SVNRevision.Kind;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.DiffViewerSettings;
import org.eclipse.team.svn.core.operation.local.DiffViewerSettings.ExternalProgramParameters;
import org.eclipse.team.svn.core.operation.local.RunExternalCompareOperation.DefaultExternalProgramParametersProvider;
import org.eclipse.team.svn.core.operation.local.RunExternalCompareOperation.DetectExternalCompareOperationHelper;
import org.eclipse.team.svn.core.operation.local.RunExternalCompareOperation.ExternalCompareOperation;
import org.eclipse.team.svn.core.resource.ILocalFile;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.svnstorage.SVNLocalResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.svnstorage.SVNRepositoryResource;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.action.local.CompareWithWorkingCopyAction;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.operation.UILoggedOperation;
import org.eclipse.team.svn.ui.preferences.SVNTeamDiffViewerPage;
import org.eclipse.team.svn.ui.utility.OverlayedImageDescriptor;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.ui.progress.UIJob;

/**
 * Implements three way comparison of resource trees
 * 
 * @author Alexander Gurov
 */
public class ThreeWayResourceCompareInput extends ResourceCompareInput implements IResourceChangeListener {
	protected ILocalResource local;

	protected Collection<SVNDiffStatus> localChanges;

	protected Collection<SVNDiffStatus> remoteChanges;

	public ThreeWayResourceCompareInput(CompareConfiguration configuration, ILocalResource local,
			IRepositoryResource ancestor, IRepositoryResource remote, Collection<SVNDiffStatus> localChanges,
			Collection<SVNDiffStatus> remoteChanges) {
		super(configuration);

		this.local = local;
		this.localChanges = localChanges;
		this.remoteChanges = remoteChanges;

		rootLeft = SVNRemoteStorage.instance().asRepositoryResource(this.local.getResource());
		rootLeft.setSelectedRevision(SVNRevision.WORKING);
		rootAncestor = ancestor;
		rootRight = remote;
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();
		if (delta != null) {
			IResourceDelta resourceDelta = delta.findMember(local.getResource().getFullPath());
			if (resourceDelta != null) {
				UIJob job = new UIJob("") { //$NON-NLS-1$
					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						if (!isSaveNeeded()) {
							ThreeWayResourceCompareInput.this.fireInputChange();
						}
						return Status.OK_STATUS;
					}
				};
				job.setSystem(true);
				job.schedule();
			}
		}
	}

	@Override
	protected void handleDispose() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		workspace.removeResourceChangeListener(this);

		super.handleDispose();
	}

	@Override
	protected void fillMenu(IMenuManager manager, TreeSelection selection) {
		final CompareNode selectedNode = (CompareNode) selection.getFirstElement();
		Action tAction = null;
		boolean propertyComparisonAllowed = true;
		if ((selectedNode.getKind() & Differencer.DIRECTION_MASK) == Differencer.CONFLICTING) {
			propertyComparisonAllowed = (selectedNode.getKind() & Differencer.CHANGE_TYPE_MASK) != Differencer.DELETION;
		} else {
			propertyComparisonAllowed = (selectedNode.getKind() & Differencer.CHANGE_TYPE_MASK) == Differencer.CHANGE;
		}
		manager.add(tAction = new Action(SVNUIMessages.SynchronizeActionGroup_CompareProperties) {
			@Override
			public void run() {
				ResourceElement element = (ResourceElement) selectedNode.getLeft();
				SVNLocalResource local = (SVNLocalResource) element.getLocalResource();
				IResource left = local.getResource();
				element = (ResourceElement) selectedNode.getAncestor();
				SVNEntryRevisionReference baseReference = null;
				long baseRevNum = local.getBaseRevision();
				SVNRepositoryResource repoResource = (SVNRepositoryResource) element.getRepositoryResource();
				if (repoResource.getSelectedRevision().getKind() == SVNRevision.BASE.getKind()) {
					baseReference = new SVNEntryRevisionReference(
							FileUtility.getWorkingCopyPath(local.getResource()), null, SVNRevision.BASE);
				} else {
					baseReference = new SVNEntryRevisionReference(
							repoResource.getUrl(), repoResource.getPegRevision(), repoResource.getSelectedRevision());
				}
				element = (ResourceElement) selectedNode.getRight();
				SVNEntryRevisionReference rightReference = null;
				repoResource = (SVNRepositoryResource) element.getRepositoryResource();
				if (repoResource.getSelectedRevision().getKind() == SVNRevision.BASE.getKind()) {
					rightReference = new SVNEntryRevisionReference(
							FileUtility.getWorkingCopyPath(local.getResource()), null, SVNRevision.BASE);
				} else {
					rightReference = new SVNEntryRevisionReference(
							repoResource.getUrl(), repoResource.getPegRevision(), repoResource.getSelectedRevision());
				}
				ThreeWayPropertyCompareInput input = new ThreeWayPropertyCompareInput(
						new CompareConfiguration(), left, rightReference, baseReference,
						repoResource.getRepositoryLocation(), baseRevNum);
				try {
					input.run(new NullProgressMonitor());
					if (input.getCompareResult() == null) {
						MessageDialog dialog = new MessageDialog(
								UIMonitorUtility.getShell(), SVNUIMessages.ComparePropsNoDiff_Title, null,
								SVNUIMessages.ComparePropsNoDiff_Message, MessageDialog.INFORMATION,
								new String[] { IDialogConstants.OK_LABEL }, 0);
						dialog.open();
					} else {
						PropertyComparePanel panel = new PropertyComparePanel(input, true);
						DefaultDialog dlg = new DefaultDialog(UIMonitorUtility.getShell(), panel);
						dlg.open();
					}
				} catch (Exception ex) {
					UILoggedOperation.reportError("Compare Properties Operation", ex);
				}
			}
		});
		tAction.setEnabled(propertyComparisonAllowed && selection.size() == 1);

		//external compare action
		Action externalCompareAction = getOpenInExternalCompareEditorAction(selectedNode, selection);
		manager.add(externalCompareAction);
	}

	protected Action getOpenInExternalCompareEditorAction(final CompareNode selectedNode,
			final TreeSelection selection) {
		ResourceElement element = (ResourceElement) selectedNode.getLeft();
		final ILocalResource local = element.getLocalResource();
		final IResource resource = local.getResource();

		DiffViewerSettings diffSettings = SVNTeamDiffViewerPage.loadDiffViewerSettings();
		DetectExternalCompareOperationHelper detectCompareHelper = new DetectExternalCompareOperationHelper(resource,
				diffSettings, true);
		detectCompareHelper.execute(new NullProgressMonitor());
		final ExternalProgramParameters externalProgramParams = detectCompareHelper.getExternalProgramParameters();

		boolean isEnabled = selection.size() == 1 && externalProgramParams != null
				&& CompareWithWorkingCopyAction.COMPARE_FILTER.accept(local);

		Action action = new Action(SVNUIMessages.OpenInExternalCompareEditor_Action) {
			@Override
			public void run() {
				if (externalProgramParams != null) {
					ResourceElement element = (ResourceElement) selectedNode.getRight();
					IRepositoryResource remote = element.getRepositoryResource();

					IActionOperation op = new ExternalCompareOperation(local, remote,
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
		Map<String, SVNDiffStatus> localChanges = new HashMap<>();
		HashSet<String> localOnly = new HashSet<>();
		SVNDiffStatus[] rChanges = remoteChanges.toArray(new SVNDiffStatus[remoteChanges.size()]);
		SVNUtility.reorder(rChanges, true);
		for (Iterator<SVNDiffStatus> it = this.localChanges.iterator(); it.hasNext() && !monitor.isCanceled();) {
			SVNDiffStatus status = it.next();
			localChanges.put(status.pathPrev, status);
			localOnly.add(status.pathPrev);
		}

		HashMap path2node = new HashMap();
		String message = SVNUIMessages.ResourceCompareInput_CheckingDelta;
		for (int i = 0; i < rChanges.length && !monitor.isCanceled(); i++) {
			SVNDiffStatus status = rChanges[i];
			String localPath = getLocalPath(SVNUtility.decodeURL(status.pathPrev), rootAncestor);
			localOnly.remove(localPath);
			monitor.subTask(BaseMessages.format(message, new Object[] { localPath }));
			makeBranch(localPath, localChanges.get(localPath), status, path2node, monitor);
			ProgressMonitorUtility.progress(monitor, i, rChanges.length);
		}
		for (String localPath : localOnly) {
			makeBranch(localPath, localChanges.get(localPath), null, path2node, monitor);
		}

		findRootNode(path2node, rootLeft, monitor);

		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);

		super.initialize(monitor);
	}

	protected void makeBranch(String localPath, SVNDiffStatus stLocal, SVNDiffStatus stRemote, final Map path2node,
			final IProgressMonitor monitor) throws Exception {
		// 1) take local statuses
		SVNEntry.Kind localKind = stLocal == null ? SVNEntry.Kind.NONE : getNodeKind(stLocal, true);
		SVNEntry.Kind nodeKind = localKind == SVNEntry.Kind.NONE && stRemote != null
				? getNodeKind(stRemote, false)
				: localKind;
		ILocalResource local = getLocalResource(localPath, nodeKind == SVNEntry.Kind.FILE);
		// 2) skip all ignored resources that does not have real remote variants
		if (stRemote != null || !IStateFilter.SF_IGNORED.accept(local)) {
			// the check "if present in path2node" was removed because it is actually an acceptable situation (in case when file was deleted and the folder with the same name was created in its place, for example)
			//	so, the node could be added twice with different states
			if (local.isCopied() && IStateFilter.SF_ADDED.accept(local)
					&& !local.getResource().equals(this.local.getResource())) {
				// 3) if node is moved and is not a root node traverse all children
				FileUtility.checkForResourcesPresenceRecursive(new IResource[] { local.getResource() },
						new IStateFilter.AbstractStateFilter() {
							@Override
							protected boolean allowsRecursionImpl(ILocalResource local, IResource resource,
									String state, int mask) {
								// do not traverse through ignored resources
								return !IStateFilter.SF_IGNORED.accept(resource, state, mask)
										&& !IStateFilter.SF_DELETED.accept(resource, state, mask);
							}

							@Override
							protected boolean acceptImpl(ILocalResource local, IResource resource, String state,
									int mask) {
								// 4) for each found children create locally "added" node
								if (!IStateFilter.SF_DELETED.accept(resource, state, mask)) {
									local = takeLocal(local, resource);
									String path = FileUtility.getWorkingCopyPath(resource);
									SVNDiffStatus stLocal = new SVNDiffStatus(path, path,
											resource.getType() == IResource.FILE
													? SVNEntry.Kind.FILE
													: SVNEntry.Kind.DIR,
											SVNEntryStatus.Kind.ADDED, SVNEntryStatus.Kind.NONE);
									try {
										CompareNode node = ThreeWayResourceCompareInput.this.makeNode(local, stLocal,
												null, path2node, monitor);
										if (node != null) {
											IRepositoryResource remote = ((ResourceElement) node.getLeft())
													.getRepositoryResource();
											path2node.put(SVNUtility.createPathForSVNUrl(remote.getUrl()), node);
										}
									} catch (RuntimeException ex) {
										throw ex;
									} catch (Exception ex) {
										throw new RuntimeException(ex);
									}
								} else {
									path2node.put(SVNUtility.createPathForSVNUrl(
											SVNRemoteStorage.instance().asRepositoryResource(resource).getUrl()), null);
								}
								return false;
							}
						});
			} else {
				// 3) create node
				CompareNode node = makeNode(local, stLocal, stRemote, path2node, monitor);
				if (node != null) {
					IRepositoryResource resource = ((ResourceElement) node.getLeft()).getRepositoryResource();
					path2node.put(SVNUtility.createPathForSVNUrl(resource.getUrl()), node);
				}
			}
		}
	}

	protected CompareNode makeNode(ILocalResource local, SVNDiffStatus stLocal, SVNDiffStatus stRemote, Map path2node,
			IProgressMonitor monitor) throws Exception {
		SVNEntry.Kind localNodeKind = local instanceof ILocalFile ? SVNEntry.Kind.FILE : SVNEntry.Kind.DIR;
		SVNEntry.Kind remoteNodeKind = stRemote == null ? localNodeKind : getNodeKind(stRemote, false);

		boolean useOriginator = this.local.isCopied()
				&& (stLocal != null && stLocal.textStatus != SVNEntryStatus.Kind.ADDED
						|| local.getResource().equals(this.local.getResource()));
		IRepositoryResource[] entries = getRepositoryEntries(local, remoteNodeKind, stLocal, stRemote);

		IRepositoryResource left = entries[0];
		IRepositoryResource ancestor = entries[1];
		IRepositoryResource right = entries[2];

		if (right.getSelectedRevision() != SVNRevision.BASE && IStateFilter.SF_NOTEXISTS.accept(local)
				&& !right.exists()) {
			return null;
		}

		SVNEntryStatus.Kind statusLeft = stLocal == null
				? SVNEntryStatus.Kind.NORMAL
				: stLocal.textStatus == SVNEntryStatus.Kind.NORMAL ? stLocal.propStatus : stLocal.textStatus;
		if (statusLeft == SVNEntryStatus.Kind.DELETED && localNodeKind == SVNEntry.Kind.FILE
				&& new File(FileUtility.getWorkingCopyPath(local.getResource())).exists()) {
			statusLeft = SVNEntryStatus.Kind.REPLACED;
		}
		SVNEntryStatus.Kind fictiveStatusRight = useOriginator
				|| statusLeft != SVNEntryStatus.Kind.ADDED && statusLeft != SVNEntryStatus.Kind.IGNORED
						&& statusLeft != SVNEntryStatus.Kind.NONE && statusLeft != SVNEntryStatus.Kind.UNVERSIONED
								? SVNEntryStatus.Kind.NORMAL
								: SVNEntryStatus.Kind.NONE;
		SVNEntryStatus.Kind statusRight = stRemote != null
				? stRemote.textStatus == SVNEntryStatus.Kind.NORMAL ? stRemote.propStatus : stRemote.textStatus
				: fictiveStatusRight;

		// skip resources that already up-to-date: only in case if URL's are same
		if (stRemote != null && rootRight.getUrl().equals(rootAncestor.getUrl())) {
			if (rootRight.getSelectedRevision().getKind() == Kind.NUMBER
					&& this.local.getRevision() >= ((SVNRevision.Number) rootRight.getSelectedRevision()).getNumber()) {
				if (!local.getResource().exists() && statusRight == SVNEntryStatus.Kind.DELETED
						|| statusRight != SVNEntryStatus.Kind.DELETED && local.getRevision() == right.getRevision()) {
					return null;
				}
			} else if (local.getRevision() == right.getRevision()) {
				if (stLocal == null) {
					return null;
				}
				stRemote = null;
				statusRight = fictiveStatusRight;
			}
		}

		int diffKindLeft = ResourceCompareInput.getDiffKind(statusLeft,
				stLocal == null ? SVNEntryStatus.Kind.NONE : stLocal.propStatus);
		if (diffKindLeft != Differencer.NO_CHANGE) {
			diffKindLeft |= Differencer.LEFT;
		}
		int diffKindRight = ResourceCompareInput.getDiffKind(statusRight,
				stRemote == null ? SVNEntryStatus.Kind.NONE : stRemote.propStatus);
		if (diffKindRight != Differencer.NO_CHANGE) {
			diffKindRight |= Differencer.RIGHT;
		}

		if (/*(diffKindRight & Differencer.CHANGE_TYPE_MASK) == Differencer.ADDITION ||*/ (diffKindLeft
				& Differencer.CHANGE_TYPE_MASK) == Differencer.DELETION) {
			left.setSelectedRevision(SVNRevision.INVALID_REVISION);
			//ancestor.setSelectedRevision(SVNRevision.INVALID_REVISION);
		}
		if ((diffKindRight
				& Differencer.CHANGE_TYPE_MASK) == Differencer.DELETION /*|| (diffKindLeft & Differencer.CHANGE_TYPE_MASK) == Differencer.ADDITION*/) {
			right.setSelectedRevision(SVNRevision.INVALID_REVISION);
		}

		IDiffContainer parent = getParentCompareNode(left, path2node);
		return new CompareNode(parent, diffKindLeft | diffKindRight, local, left, ancestor, right, statusLeft,
				statusRight);
	}

	protected ILocalResource getLocalResource(String path, boolean isFile) {
		if (path == null) {
			return SVNRemoteStorage.instance().asLocalResource(null);
		}
		IProject project = local.getResource().getProject();
		String relative = path.substring(FileUtility.getWorkingCopyPath(project).length());
		IResource resource = relative.length() == 0 ? project : project.findMember(relative);

		if (resource == null) {
			resource = isFile ? project.getFile(relative) : project.getFolder(relative);
		}

		return SVNRemoteStorage.instance().asLocalResourceAccessible(resource);
	}

	protected String getLocalPath(String url, IRepositoryResource base) {
		String baseUrl = base.getUrl();
		if (url.length() < baseUrl.length()) {
			return null;
		}
		String delta = url.substring(baseUrl.length());
		String projectPath = FileUtility.getWorkingCopyPath(local.getResource());
		return projectPath + delta;
	}

	@Override
	protected IDiffContainer makeStubNode(IDiffContainer parent, IRepositoryResource node) {
		ILocalResource local = getLocalResource(getLocalPath(node.getUrl(), rootLeft), false);
		IRepositoryResource ancestor = node;
		IRepositoryResource remote = node;
		if (!IStateFilter.SF_INTERNAL_INVALID.accept(local)) {
			IRepositoryResource[] entries = getRepositoryEntries(local, SVNEntry.Kind.DIR, null, null);
			ancestor = entries[1];
			remote = entries[2];
		}
		return new CompareNode(parent, Differencer.NO_CHANGE, local, node, ancestor, remote, SVNEntryStatus.Kind.NORMAL,
				SVNEntryStatus.Kind.NORMAL);
	}

	protected IRepositoryResource[] getRepositoryEntries(ILocalResource local, SVNEntry.Kind remoteNodeKind,
			SVNDiffStatus stLocal, SVNDiffStatus stRemote) {
		IRepositoryLocation location = rootLeft.getRepositoryLocation();

		IRepositoryResource left = SVNRemoteStorage.instance().asRepositoryResource(local.getResource());
		left.setSelectedRevision(SVNRevision.WORKING);
		left.setPegRevision(null);

		boolean useOriginator = this.local.isCopied()
				&& (stLocal != null && stLocal.textStatus != SVNEntryStatus.Kind.ADDED
						|| local.getResource().equals(this.local.getResource()));
		IRepositoryResource ancestor = useOriginator
				? SVNUtility.getCopiedFrom(local.getResource())
				: SVNUtility.copyOf(left);
		IRepositoryResource right = useOriginator
				? SVNUtility.getCopiedFrom(local.getResource())
				: SVNUtility.copyOf(left);
		if (stRemote != null) {
			ancestor = createResourceFor(location, remoteNodeKind, SVNUtility.decodeURL(stRemote.pathPrev));
			right = createResourceFor(location, remoteNodeKind, SVNUtility.decodeURL(stRemote.pathNext));
		} else if (!rootLeft.getUrl().equals(rootRight.getUrl())
				&& left.getUrl().length() >= rootLeft.getUrl().length()) {
			String delta = left.getUrl().substring(rootLeft.getUrl().length());
			ancestor = createResourceFor(location, remoteNodeKind, rootAncestor.getUrl() + delta);
			right = createResourceFor(location, remoteNodeKind, rootRight.getUrl() + delta);
		}
		ancestor.setSelectedRevision(SVNRevision.BASE);
		ancestor.setPegRevision(null);
		right.setPegRevision(rootRight.getPegRevision());
		right.setSelectedRevision(rootRight.getSelectedRevision());

		return new IRepositoryResource[] { left, ancestor, right };
	}

	@Override
	protected boolean isThreeWay() {
		return true;
	}

	@Override
	protected String getLeftLabel() throws Exception {
		ResourceElement element = getLeftResourceElement();
		return element.getLocalResource().getResource().getFullPath().toString().substring(1) + " [" //$NON-NLS-1$
				+ getRevisionPart(element) + "]"; //$NON-NLS-1$
	}

	@Override
	protected String getRevisionPart(ResourceElement element) throws Exception {
		IRepositoryResource resource = element.getRepositoryResource();
		SVNRevision selected = resource.getSelectedRevision();
		SVNRevision.Kind kind = selected.getKind();

		if (kind == Kind.WORKING) {
			return SVNUIMessages.ResourceCompareInput_LocalSign;
		} else if (kind == Kind.BASE) {
			long revision = element.getLocalResource().getRevision();
			if (revision == SVNRevision.INVALID_REVISION_NUMBER) {
				return SVNUIMessages.ResourceCompareInput_ResourceIsNotAvailable;
			}
			return BaseMessages.format(SVNUIMessages.ResourceCompareInput_BaseSign,
					new String[] { String.valueOf(revision) });
		}

		return super.getRevisionPart(element);
	}

	@Override
	protected ResourceCompareViewer createDiffViewerImpl(Composite parent, CompareConfiguration config) {
		return new ResourceCompareViewer(parent, config) {
			@Override
			public void setLabelProvider(IBaseLabelProvider labelProvider) {
				super.setLabelProvider(new LabelProviderWrapper((ILabelProvider) labelProvider) {
					@Override
					public Image getImage(Object element) {
						if (element instanceof CompareNode && (((CompareNode) element)
								.getLocalChangeType() == SVNEntryStatus.Kind.REPLACED
								|| ((CompareNode) element).getRemoteChangeType() == SVNEntryStatus.Kind.REPLACED)) {
							Image image = images.get(element);
							if (image == null) {
								OverlayedImageDescriptor imageDescriptor = null;
								int direction = ((CompareNode) element).getKind() & Differencer.DIRECTION_MASK;
								if (direction == Differencer.LEFT) {
									imageDescriptor = new OverlayedImageDescriptor(baseProvider.getImage(element),
											SVNTeamUIPlugin.instance()
													.getImageDescriptor("icons/overlays/replaced_out.gif"), //$NON-NLS-1$
											new Point(22, 16),
											OverlayedImageDescriptor.RIGHT | OverlayedImageDescriptor.CENTER_V);
								} else if (direction == Differencer.RIGHT) {
									imageDescriptor = new OverlayedImageDescriptor(baseProvider.getImage(element),
											SVNTeamUIPlugin.instance()
													.getImageDescriptor("icons/overlays/replaced_in.gif"), //$NON-NLS-1$
											new Point(22, 16),
											OverlayedImageDescriptor.RIGHT | OverlayedImageDescriptor.CENTER_V);
								} else {
									imageDescriptor = new OverlayedImageDescriptor(baseProvider.getImage(element),
											SVNTeamUIPlugin.instance()
													.getImageDescriptor("icons/overlays/replaced_conf.gif"), //$NON-NLS-1$
											new Point(22, 16),
											OverlayedImageDescriptor.RIGHT | OverlayedImageDescriptor.CENTER_V);
								}
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

	@Override
	public Object getAdapter(Class adapter) {
		if (IFile.class.equals(adapter)) {
			// disallow auto-flush of editor content
			return null;
		}
		return super.getAdapter(adapter);
	}

	@Override
	public void saveChanges(IProgressMonitor pm) throws CoreException {
		super.saveChanges(pm);

		if (root != null) {
			try {
				pm.beginTask(SVNUIMessages.ThreeWayResourceCompareInput_SaveChanges, -1);
				this.saveChanges((CompareNode) root, pm);
			} finally {
				pm.done();
			}
		}
	}

	protected void saveChanges(CompareNode node, IProgressMonitor pm) throws CoreException {
		ResourceElement left = (ResourceElement) node.getLeft();
		if (left.isEditable() && left.isDirty()) {
			left.commit(pm);
		}
		IDiffElement[] children = node.getChildren();
		if (children != null) {
			for (int i = 0; i < children.length && !pm.isCanceled(); i++) {
				this.saveChanges((CompareNode) children[i], pm);
			}
		}
	}

	protected class CompareNode extends BaseCompareNode {
		protected SVNEntryStatus.Kind localChangeType;

		protected SVNEntryStatus.Kind remoteChangeType;

		public CompareNode(IDiffContainer parent, int kind, ILocalResource workingVersion, IRepositoryResource local,
				IRepositoryResource ancestor, IRepositoryResource remote, SVNEntryStatus.Kind localChangeType,
				SVNEntryStatus.Kind remoteChangeType) {
			super(parent, kind);

			this.localChangeType = localChangeType;
			this.remoteChangeType = remoteChangeType;

			boolean useOriginator = ThreeWayResourceCompareInput.this.local.isCopied()
					&& (localChangeType != SVNEntryStatus.Kind.ADDED || workingVersion.getResource()
							.equals(ThreeWayResourceCompareInput.this.local.getResource()));
			ResourceElement leftElt = new ResourceElement(local, workingVersion,
					localChangeType != SVNEntryStatus.Kind.NONE && localChangeType != SVNEntryStatus.Kind.DELETED);
			leftElt.setEditable(local instanceof IRepositoryFile);
			setLeft(leftElt);
			setAncestor(new ResourceElement(ancestor, workingVersion,
					useOriginator || localChangeType != SVNEntryStatus.Kind.UNVERSIONED
							&& localChangeType != SVNEntryStatus.Kind.ADDED
							&& remoteChangeType != SVNEntryStatus.Kind.ADDED));
			setRight(new ResourceElement(remote, workingVersion,
					remoteChangeType != SVNEntryStatus.Kind.DELETED && remoteChangeType != SVNEntryStatus.Kind.NONE));
		}

		@Override
		public void copy(boolean leftToRight) {
			if (!leftToRight) {
				InputStream stream = null;
				try {
					stream = ((ResourceElement) getRight()).getContents();
					ByteArrayOutputStream data = new ByteArrayOutputStream();
					byte[] block = new byte[1024];
					int len = 0;
					while ((len = stream.read(block)) > 0) {
						data.write(block, 0, len);
					}
					((ResourceElement) getLeft()).setContent(data.toByteArray());
				} catch (IOException e) {
				} finally {
					if (stream != null) {
						try {
							stream.close();
						} catch (IOException ex) {
						}
					}
				}
			} else {
				super.copy(leftToRight);
			}
		}

		public SVNEntryStatus.Kind getLocalChangeType() {
			return localChangeType;
		}

		public SVNEntryStatus.Kind getRemoteChangeType() {
			return remoteChangeType;
		}

	}

}
