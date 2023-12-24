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
 *    Igor Burilo - Bug 211415: Export History log
 *    Igor Burilo - Bug 245509: Improve extract log
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.history;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.history.provider.FileRevision;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.actions.CompareRevisionAction;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNEntry.Kind;
import org.eclipse.team.svn.core.connector.SVNEntryInfo;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNLogEntry;
import org.eclipse.team.svn.core.connector.SVNLogPath;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.factory.ISVNConnectorFactory;
import org.eclipse.team.svn.core.history.SVNRemoteResourceRevision;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.IResourcePropertyProvider;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.operation.local.FiniExtractLogOperation;
import org.eclipse.team.svn.core.operation.local.GetRemoteContentsOperation;
import org.eclipse.team.svn.core.operation.local.InitExtractLogOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.RestoreProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.SaveProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.UpdateOperation;
import org.eclipse.team.svn.core.operation.remote.ExportOperation;
import org.eclipse.team.svn.core.operation.remote.ExtractToOperationRemote;
import org.eclipse.team.svn.core.operation.remote.GetRemotePropertiesOperation;
import org.eclipse.team.svn.core.operation.remote.LocateResourceURLInHistoryOperation;
import org.eclipse.team.svn.core.operation.remote.PreparedBranchTagOperation;
import org.eclipse.team.svn.core.operation.remote.management.AddRevisionLinkOperation;
import org.eclipse.team.svn.core.operation.remote.management.SaveRepositoryLocationsOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryContainer;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;
import org.eclipse.team.svn.core.resource.IRepositoryRoot;
import org.eclipse.team.svn.core.resource.IRevisionLink;
import org.eclipse.team.svn.core.resource.IRevisionLinkProvider;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.action.AbstractRepositoryTeamAction;
import org.eclipse.team.svn.ui.action.remote.BranchTagAction;
import org.eclipse.team.svn.ui.action.remote.CreatePatchAction;
import org.eclipse.team.svn.ui.action.remote.OpenFileAction;
import org.eclipse.team.svn.ui.action.remote.OpenFileWithAction;
import org.eclipse.team.svn.ui.action.remote.OpenFileWithExternalAction;
import org.eclipse.team.svn.ui.action.remote.OpenFileWithInplaceAction;
import org.eclipse.team.svn.ui.compare.TwoWayPropertyCompareInput;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.dialog.ReplaceWarningDialog;
import org.eclipse.team.svn.ui.history.data.AffectedPathsNode;
import org.eclipse.team.svn.ui.history.data.SVNChangedPathData;
import org.eclipse.team.svn.ui.history.data.SVNLocalFileRevision;
import org.eclipse.team.svn.ui.history.model.ILogNode;
import org.eclipse.team.svn.ui.history.model.SVNLogNode;
import org.eclipse.team.svn.ui.operation.CompareRepositoryResourcesOperation;
import org.eclipse.team.svn.ui.operation.CompareResourcesOperation;
import org.eclipse.team.svn.ui.operation.OpenRemoteFileOperation;
import org.eclipse.team.svn.ui.operation.RefreshRemoteResourcesOperation;
import org.eclipse.team.svn.ui.operation.RefreshRepositoryLocationsOperation;
import org.eclipse.team.svn.ui.operation.RemoteShowAnnotationOperation;
import org.eclipse.team.svn.ui.operation.ShowHistoryViewOperation;
import org.eclipse.team.svn.ui.operation.ShowPropertiesOperation;
import org.eclipse.team.svn.ui.operation.ShowRevisionPropertiesOperation;
import org.eclipse.team.svn.ui.operation.UILoggedOperation;
import org.eclipse.team.svn.ui.panel.common.InputRevisionPanel;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.repository.model.RepositoryFile;
import org.eclipse.team.svn.ui.repository.model.RepositoryFolder;
import org.eclipse.team.svn.ui.utility.LockProposeUtility;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.svn.ui.wizard.CreatePatchWizard;
import org.eclipse.team.ui.history.HistoryPage;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;

/**
 * Provides actions for affected paths window
 * 
 * @author Alexander Gurov
 */
public class HistoryActionManager {
	public interface IControlActionManager {
		void installKeyBindings(StructuredViewer viewer);

		void installDefaultAction(StructuredViewer viewer);

		void installMenuActions(StructuredViewer viewer, IWorkbenchPartSite site);
	}

	public static class HistoryAction extends Action {
		protected HistoryAction(String text) {
			super(text);
		}

		protected HistoryAction(String text, Object[] args) {
			this(text, args, null);
		}

		protected HistoryAction(String text, Object[] args, String imageDescriptor) {
			super(BaseMessages.format(text, args));
			setImageDescriptor(
					imageDescriptor == null ? null : SVNTeamUIPlugin.instance().getImageDescriptor(imageDescriptor));
		}

		protected HistoryAction(String text, String imageDescriptor) {
			this(text);
			setImageDescriptor(
					imageDescriptor == null ? null : SVNTeamUIPlugin.instance().getImageDescriptor(imageDescriptor));
		}

		protected HistoryAction(String text, String imageDescriptor, int style) {
			super(SVNUIMessages.getString(text), style);
			setImageDescriptor(
					imageDescriptor == null ? null : SVNTeamUIPlugin.instance().getImageDescriptor(imageDescriptor));
		}

	}

	protected ISVNHistoryView view;

	protected long selectedRevision;

	public final IControlActionManager affectedTableManager;

	public final IControlActionManager affectedTreeManager;

	public final IControlActionManager logMessagesManager;

	public HistoryActionManager(ISVNHistoryView info) {
		view = info;
		affectedTableManager = new AffectedTableActionManager();
		affectedTreeManager = new AffectedTreeActionManager();
		logMessagesManager = new LogMessagesActionManager();
	}

	public long getSelectedRevision() {
		return selectedRevision;
	}

	public void setSelectedRevision(long selectedRevision) {
		this.selectedRevision = selectedRevision;
	}

	protected class LogMessagesActionManager implements IControlActionManager {
		@Override
		public void installKeyBindings(final StructuredViewer viewer) {
			viewer.getControl().addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent event) {
					if (event.keyCode == SWT.F5) {
						view.refresh(ISVNHistoryView.REFRESH_ALL);
					}
					if (event.stateMask == SWT.CTRL && (event.keyCode == 'c' || event.keyCode == 'C')) {
						ILogNode[] selection = LogMessagesActionManager.this.getSelection(viewer);
						if (selection.length > 0) {
							handleCopy(selection);
						}
					}
				}
			});
		}

		@Override
		public void installDefaultAction(final StructuredViewer viewer) {
			viewer.addDoubleClickListener(e -> {
				ISelection selection = e.getSelection();
				if (selection instanceof IStructuredSelection) {
					IStructuredSelection structured = (IStructuredSelection) selection;
					if (structured.size() == 1) {
						handleDoubleClick((TreeViewer) viewer, (ILogNode) structured.getFirstElement(), true);
					}
				}
			});
		}

		@Override
		public void installMenuActions(final StructuredViewer viewer, IWorkbenchPartSite site) {
			MenuManager menuMgr = new MenuManager();
			Menu menu = menuMgr.createContextMenu(viewer.getControl());
			menuMgr.addMenuListener(manager -> {
				manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

				HistoryAction refreshAction = new HistoryAction(SVNUIMessages.HistoryView_Refresh,
						"icons/common/refresh.gif") { //$NON-NLS-1$
					@Override
					public void run() {
						view.refresh(ISVNHistoryView.REFRESH_ALL);
					}
				};

				ILogNode[] selection = LogMessagesActionManager.this.getSelection(viewer);
				if (selection.length == 0 || selection[0].getType() == ILogNode.TYPE_NONE) {
					if (!view.isPending() && view.isFilterEnabled()
							&& view.getMode() != ISVNHistoryViewInfo.MODE_LOCAL) {
						LogMessagesActionManager.this.addFilterPart(viewer, manager);
					}
					manager.add(new Separator());
					manager.add(refreshAction);
					return;
				}

				boolean onlyLogEntries = true;
				boolean onlyLocalEntries = true;
				boolean containsCategory = false;
				boolean containsMergeHistory = false;
				for (ILogNode node : selection) {
					int type = node.getType();
					if (type != ILogNode.TYPE_SVN) {
						onlyLogEntries = false;
					} else if (node.getParent() instanceof SVNLogNode) {
						containsMergeHistory = true;
					}
					if (type != ILogNode.TYPE_LOCAL) {
						onlyLocalEntries = false;
					}
					if (type == ILogNode.TYPE_CATEGORY) {
						onlyLogEntries = false;
						onlyLocalEntries = false;
						containsCategory = true;
					}
				}

				if (onlyLogEntries) {
					LogMessagesActionManager.this.addRemotePart(viewer, manager, selection, containsMergeHistory);
					LogMessagesActionManager.this.addFilterPart(viewer, manager);
					manager.add(new Separator());
				}
				if (onlyLocalEntries) {
					LogMessagesActionManager.this.addLocalPart(viewer, manager, selection);

					manager.add(new Separator());
				}
				if (!onlyLogEntries && !onlyLocalEntries && !containsCategory) {
					LogMessagesActionManager.this.addLocalOrRemotePart(viewer, manager, selection,
							containsMergeHistory);

					manager.add(new Separator());
				}
				if (!onlyLogEntries) {
					LogMessagesActionManager.this.addCommonPart(viewer, manager, selection, containsMergeHistory);

					manager.add(new Separator());
				}

				manager.add(refreshAction);
			});
			menuMgr.setRemoveAllWhenShown(true);
			viewer.getControl().setMenu(menu);
			site.registerContextMenu(menuMgr, viewer);
		}

		protected void addRemotePart(final StructuredViewer viewer, IMenuManager manager, final ILogNode[] selection,
				boolean containsMergeHistory) {
			final Shell shell = viewer.getControl().getShell();
			Action tAction = null;
			SVNLogEntry[] entries = view.getFullRemoteHistory();
			boolean existsInPrevious = entries[entries.length - 1] != selection[0].getEntity()
					|| !view.isAllRemoteHistoryFetched();
			final SVNLogEntry current = (SVNLogEntry) selection[0].getEntity();
			String revision = view.getResource() != null
					? String.valueOf(current.revision)
					: SVNUIMessages.HistoryView_HEAD;
			if (!containsMergeHistory) {
				if (view.getRepositoryResource() instanceof IRepositoryFile) {
					String name = view.getRepositoryResource().getName();
					manager.add(tAction = new HistoryAction(SVNUIMessages.HistoryView_Open) {
						@Override
						public void run() {
							handleDoubleClick((TreeViewer) viewer, selection[0], false);
						}
					});
					tAction.setEnabled(selection.length == 1);
					tAction.setImageDescriptor(
							SVNTeamUIPlugin.instance().getWorkbench().getEditorRegistry().getImageDescriptor(name));

					//FIXME: "Open with" submenu shouldn't be hardcoded after reworking of
					//       the HistoryView. Should be made like the RepositoriesView menu.
					MenuManager sub = new MenuManager(SVNUIMessages.HistoryView_OpenWith, "historyOpenWithMenu"); //$NON-NLS-1$
					sub.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

					sub.add(new Separator("nonDefaultTextEditors")); //$NON-NLS-1$
					IEditorDescriptor[] editors = SVNTeamUIPlugin.instance()
							.getWorkbench()
							.getEditorRegistry()
							.getEditors(name);
					for (IEditorDescriptor editor : editors) {
						if (!editor.getId().equals(EditorsUI.DEFAULT_TEXT_EDITOR_ID)) {
							addMenuItem(viewer, sub, editor.getLabel(), new OpenFileWithAction(editor.getId(), false));
						}
					}

					sub.add(new Separator("variousEditors")); //$NON-NLS-1$
					addMenuItem(viewer, sub, SVNUIMessages.HistoryView_TextEditor, new OpenFileWithAction());
					addMenuItem(viewer, sub, SVNUIMessages.HistoryView_SystemEditor, new OpenFileWithExternalAction());
					addMenuItem(viewer, sub, SVNUIMessages.HistoryView_InplaceEditor, new OpenFileWithInplaceAction());
					addMenuItem(viewer, sub, SVNUIMessages.HistoryView_DefaultEditor, new OpenFileAction());

					manager.add(sub);

					manager.add(new Separator());
				}

				boolean isCompareAllowed = CoreExtensionsManager.instance()
						.getSVNConnectorFactory()
						.getSVNAPIVersion() >= ISVNConnectorFactory.APICompatibility.SVNAPI_1_5_x
						|| view.getRepositoryResource() instanceof IRepositoryFile;

				manager.add(tAction = new HistoryAction(SVNUIMessages.HistoryView_CompareEachOther) {
					@Override
					public void run() {
						SVNLogEntry msg0 = (SVNLogEntry) selection[0].getEntity();
						SVNLogEntry msg1 = (SVNLogEntry) selection[1].getEntity();
						IRepositoryResource next = getResourceForSelectedRevision(msg0);
						IRepositoryResource prev = getResourceForSelectedRevision(msg1);
						if (msg0.revision < msg1.revision) {
							IRepositoryResource tmp = prev;
							prev = next;
							next = tmp;
						}
						CompareRepositoryResourcesOperation op = new CompareRepositoryResourcesOperation(prev, next);
						op.setForceId(getCompareForceId());
						UIMonitorUtility.doTaskScheduledActive(op);
					}
				});
				tAction.setEnabled(selection.length == 2 && isCompareAllowed);

				manager.add(tAction = new HistoryAction(SVNUIMessages.HistoryView_CompareCurrentWith,
						new String[] { revision }) {
					@Override
					public void run() {
						compareWithCurrent(current);
					}
				});
				tAction.setEnabled(selection.length == 1 && isCompareAllowed);

				manager.add(tAction = new HistoryAction(SVNUIMessages.HistoryView_CompareWithPrevious) {
					@Override
					public void run() {
						compareWithPreviousRevision(null, () -> {
							SVNLogEntry current1 = (SVNLogEntry) selection[0].getEntity();
							return new IRepositoryResource[] { getResourceForSelectedRevision(current1) };
						});
					}
				});
				tAction.setEnabled(selection.length == 1 && isCompareAllowed && existsInPrevious);

				manager.add(new Separator());

				manager.add(tAction = new HistoryAction(SVNUIMessages.ShowPropertiesAction_label,
						"icons/views/propertiesedit.gif") { //$NON-NLS-1$
					@Override
					public void run() {
						SVNLogEntry current = (SVNLogEntry) selection[0].getEntity();
						IRepositoryResource resource = getResourceForSelectedRevision(current);
						IResourcePropertyProvider provider = new GetRemotePropertiesOperation(resource);
						ShowPropertiesOperation op = new ShowPropertiesOperation(
								PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), resource,
								provider);
						UIMonitorUtility.doTaskScheduledActive(op);
					}
				});
				tAction.setEnabled(selection.length == 1);

				manager.add(tAction = new HistoryAction(SVNUIMessages.SynchronizeActionGroup_CompareProperties) {
					@Override
					public void run() {
						SVNLogEntry first = (SVNLogEntry) selection[0].getEntity();
						SVNLogEntry second = (SVNLogEntry) selection[1].getEntity();
						if (first.revision < second.revision) {
							SVNLogEntry tmp = second;
							second = first;
							first = tmp;
						}
						IRepositoryResource remote = view.getRepositoryResource();
						if (remote == null) {
							remote = SVNRemoteStorage.instance().asRepositoryResource(view.getResource());
						}
						TwoWayPropertyCompareInput input = new TwoWayPropertyCompareInput(
								new CompareConfiguration(),
								new SVNEntryRevisionReference(remote.getUrl(),
										SVNRevision.fromNumber(view.getCurrentRevision()),
										SVNRevision.fromNumber(first.revision)),
								new SVNEntryRevisionReference(remote.getUrl(),
										SVNRevision.fromNumber(view.getCurrentRevision()),
										SVNRevision.fromNumber(second.revision)),
								remote.getRepositoryLocation());
						CompareUI.openCompareEditor(input);
					}
				});
				tAction.setEnabled(selection.length == 2);
			}

			manager.add(tAction = new HistoryAction(SVNUIMessages.ShowRevisionProperties_label) {
				@Override
				public void run() {
					SVNRevision selectedRevision = SVNRevision.fromNumber(getSelectedRevision());
					IRepositoryLocation location = getResourceForHeadRevision().getRepositoryLocation();
					ShowRevisionPropertiesOperation op = new ShowRevisionPropertiesOperation(
							PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), location,
							selectedRevision);
					UIMonitorUtility.doTaskScheduledActive(op);
				}
			});
			tAction.setEnabled(selection.length == 1);

			if (!containsMergeHistory) {
				if (view.getRepositoryResource() instanceof IRepositoryFile) {
					manager.add(tAction = new HistoryAction(SVNUIMessages.ShowAnnotationCommand_label) {
						@Override
						public void run() {
							IRepositoryResource remote = getResourceForSelectedRevision(
									(SVNLogEntry) selection[0].getEntity());
							UIMonitorUtility.doTaskScheduledDefault(new RemoteShowAnnotationOperation(remote));
						}
					});
					tAction.setEnabled(selection.length == 1);
				}

				manager.add(new Separator());

				manager.add(tAction = new HistoryAction(SVNUIMessages.ExportCommand_label, "icons/common/export.gif") { //$NON-NLS-1$
					@Override
					public void run() {
						HistoryActionManager.this.doExport(selection[0]);
					}
				});
				tAction.setEnabled(selection.length == 1);
				manager.add(tAction = new HistoryAction(SVNUIMessages.CreatePatchCommand_label) {
					@Override
					public void run() {
						createPatch(selection);
					}
				});
				tAction.setEnabled(selection.length == 1 && existsInPrevious || selection.length == 2);
				if (view.getResource() != null) {
					manager.add(tAction = new HistoryAction(SVNUIMessages.HistoryView_GetContents) {
						@Override
						public void run() {
							if (confirmReplacement()) {
								getRevisionContents((SVNLogEntry) selection[0].getEntity(), shell);
							}
						}
					});
					tAction.setEnabled(selection.length == 1);
				}
				if (view.getResource() != null) {
					manager.add(tAction = new HistoryAction(SVNUIMessages.HistoryView_UpdateTo) {
						@Override
						public void run() {
							if (confirmReplacement()) {
								updateTo((SVNLogEntry) selection[0].getEntity());
							}
						}
					});
					tAction.setEnabled(selection.length == 1);
				}

				manager.add(tAction = new Action(SVNUIMessages.ExtractToAction_Label) {
					@Override
					public void run() {
						runExtractTo(selection);
					}
				});
				tAction.setEnabled(selection.length == 1 && existsInPrevious || selection.length == 2);

				manager.add(new Separator());

				String branchFrom = SVNUIMessages.HistoryView_BranchFromRevision;
				String tagFrom = SVNUIMessages.HistoryView_TagFromRevision;
				if (selection.length == 1) {
					revision = String.valueOf(((SVNLogEntry) selection[0].getEntity()).revision);
					branchFrom = BaseMessages.format(SVNUIMessages.HistoryView_BranchFrom, new String[] { revision });
					tagFrom = BaseMessages.format(SVNUIMessages.HistoryView_TagFrom, new String[] { revision });
				}
				manager.add(tAction = new HistoryAction(branchFrom, "icons/common/actions/branch.gif") { //$NON-NLS-1$
					@Override
					public void run() {
						PreparedBranchTagOperation op = BranchTagAction.getBranchTagOperation(
								new IRepositoryResource[] {
										getResourceForSelectedRevision((SVNLogEntry) selection[0].getEntity()) },
								UIMonitorUtility.getShell(), BranchTagAction.BRANCH_ACTION);
						if (op != null) {
							UIMonitorUtility.doTaskScheduledActive(op);
						}
					}
				});
				tAction.setEnabled(selection.length == 1);
				manager.add(tAction = new HistoryAction(tagFrom, "icons/common/actions/tag.gif") { //$NON-NLS-1$
					@Override
					public void run() {
						PreparedBranchTagOperation op = BranchTagAction.getBranchTagOperation(
								new IRepositoryResource[] {
										getResourceForSelectedRevision((SVNLogEntry) selection[0].getEntity()) },
								UIMonitorUtility.getShell(), BranchTagAction.TAG_ACTION);
						if (op != null) {
							UIMonitorUtility.doTaskScheduledActive(op);
						}
					}
				});
				tAction.setEnabled(selection.length == 1);
				manager.add(tAction = new HistoryAction(SVNUIMessages.AddRevisionLinkAction_label) {
					@Override
					public void run() {
						addRevisionLinks(selection);
					}
				});
				tAction.setEnabled(selection.length > 0);
			}

			manager.add(new Separator());

			manager.add(tAction = new HistoryAction(SVNUIMessages.HistoryView_CopyHistory, "icons/common/copy.gif") { //$NON-NLS-1$
				@Override
				public void run() {
					handleCopy(selection);
				}
			});
			tAction.setEnabled(selection.length > 0);

		}

		protected void addFilterPart(final StructuredViewer viewer, IMenuManager manager) {
			manager.add(new Separator());
			Action tAction = null;
			manager.add(tAction = new HistoryAction(SVNUIMessages.HistoryView_QuickFilter,
					"icons/views/history/filter.gif") { //$NON-NLS-1$
				@Override
				public void run() {
					view.setFilter();
				}
			});
			manager.add(tAction = new HistoryAction(SVNUIMessages.HistoryView_ClearFilter,
					"icons/views/history/clear_filter.gif") { //$NON-NLS-1$
				@Override
				public void run() {
					view.clearFilter();
				}
			});
			tAction.setEnabled(view.isFilterEnabled());
		}

		protected void addLocalPart(final StructuredViewer viewer, IMenuManager manager, final ILogNode[] selection) {
			Action tAction = null;
			manager.add(tAction = new HistoryAction(SVNUIMessages.HistoryView_Open) {
				@Override
				public void run() {
					handleDoubleClick((TreeViewer) viewer, selection[0], false);
				}
			});
			tAction.setEnabled(selection.length == 1);
			tAction.setImageDescriptor(SVNTeamUIPlugin.instance()
					.getWorkbench()
					.getEditorRegistry()
					.getImageDescriptor(((SVNLocalFileRevision) selection[0].getEntity()).getName()));

			manager.add(new Separator());

			manager.add(tAction = new HistoryAction(SVNUIMessages.HistoryView_CompareEachOther) {
				@Override
				public void run() {
					runCompareForLocal(selection);
				}
			});
			tAction.setEnabled(selection.length == 2);
			manager.add(tAction = new HistoryAction(SVNUIMessages.HistoryView_CompareCurrentWith,
					new String[] { SVNUIMessages.HistoryView_RevisionLocal }) {
				@Override
				public void run() {
					runCompareForLocal(selection);
				}
			});
			tAction.setEnabled(selection.length == 1);
			manager.add(tAction = new HistoryAction(SVNUIMessages.HistoryView_CompareWithPrevious) {
				@Override
				public void run() {
					SVNLocalFileRevision[] localHistory = view.getLocalHistory();
					SVNLocalFileRevision currentSelected = (SVNLocalFileRevision) selection[0].getEntity();
					ArrayList<SVNLocalFileRevision> toOperate = new ArrayList<>();
					toOperate.add(currentSelected);
					for (int i = 0; i < localHistory.length - 1; i++) {
						if (currentSelected.equals(localHistory[i])) {
							toOperate.add(localHistory[i + 1]);
							break;
						}
					}
					runCompareForLocal(toOperate.toArray());
				}
			});
			tAction.setEnabled(selection.length == 1
					&& !selection[0].getEntity().equals(view.getLocalHistory()[view.getLocalHistory().length - 1]));

			manager.add(new Separator());

			manager.add(tAction = new HistoryAction(SVNUIMessages.ExportCommand_label, "icons/common/export.gif") { //$NON-NLS-1$
				@Override
				public void run() {
					HistoryActionManager.this.doExport(selection[0]);
				}
			});
			tAction.setEnabled(selection.length == 1);
			manager.add(tAction = new HistoryAction(SVNUIMessages.HistoryView_GetContents) {
				@Override
				public void run() {
					try {
						((IFile) view.getResource()).setContents(
								((SVNLocalFileRevision) selection[0].getEntity()).getState(), true, true,
								new NullProgressMonitor());
					} catch (CoreException ex) {
						UILoggedOperation.reportError(getText(), ex);
					}
				}
			});
			tAction.setEnabled(
					selection.length == 1 && !((SVNLocalFileRevision) selection[0].getEntity()).isCurrentState());
		}

		protected void addLocalOrRemotePart(StructuredViewer viewer, IMenuManager manager, final ILogNode[] selection,
				boolean containsMergeHistory) {
			Action tAction = null;
			if (!containsMergeHistory) {
				manager.add(tAction = new HistoryAction(SVNUIMessages.HistoryView_CompareEachOther) {
					@Override
					public void run() {
						ArrayList<Object> selected = new ArrayList<>();
						for (ILogNode item : selection) {
							if (item.getType() == ILogNode.TYPE_LOCAL) {
								selected.add(item);
							} else {
								selected.add(new SVNRemoteResourceRevision(
										getResourceForSelectedRevision(
												(SVNLogEntry) item.getEntity()),
										(SVNLogEntry) item.getEntity()));
							}
						}
						runCompareForLocal(selected.toArray());
					}
				});
				tAction.setEnabled(selection.length == 2);
			}
		}

		protected void addCommonPart(StructuredViewer viewer, IMenuManager manager, final ILogNode[] selection,
				boolean containsMergeHistory) {
			Action tAction = null;
			manager.add(tAction = new HistoryAction(SVNUIMessages.HistoryView_CopyHistory, "icons/common/copy.gif") { //$NON-NLS-1$
				@Override
				public void run() {
					handleCopy(selection);
				}
			});
			tAction.setEnabled(selection.length > 0);
		}

		protected ILogNode[] getSelection(StructuredViewer viewer) {
			List<?> selection = ((IStructuredSelection) viewer.getSelection()).toList();
			return selection.toArray(new ILogNode[selection.size()]);
		}

	}

	protected Action addMenuItem(StructuredViewer viewer, MenuManager menuManager, String label,
			final AbstractRepositoryTeamAction action) {
		IStructuredSelection tSelection = (IStructuredSelection) viewer.getSelection();
		Action wrapper = new Action(label) {
			@Override
			public void run() {
				action.run(this);
			}
		};
		IStructuredSelection resourceSelection;
		if (tSelection.size() == 1 && ((ILogNode) tSelection.getFirstElement()).getType() == ILogNode.TYPE_SVN) {
			resourceSelection = new StructuredSelection(new RepositoryFile(null, getResourceForSelectedRevision(
					(SVNLogEntry) ((ILogNode) tSelection.getFirstElement()).getEntity())));
		} else {
			resourceSelection = new StructuredSelection(StructuredSelection.EMPTY);
		}
		action.selectionChanged(wrapper, resourceSelection);
		menuManager.add(wrapper);
		return wrapper;
	}

	protected void getRevisionContents(SVNLogEntry item, Shell shell) {
		IRepositoryResource remote = getResourceForSelectedRevision(item);

		// propose user to lock the file if it needs lock
		boolean canWrite = true;

		if (view.getResource() instanceof IFile) {
			ILocalResource local = SVNRemoteStorage.instance().asLocalResource(view.getResource());
			if (!local.isLocked() && IStateFilter.SF_NEEDS_LOCK.accept(local)) {
				canWrite = LockProposeUtility.proposeLock(new IResource[] { view.getResource() }, shell, false)
						.getSeverity() == IStatus.OK;
			}
		}
		if (canWrite) {
			HashMap<String, String> remote2local = new HashMap<>();
			remote2local.put(SVNUtility.encodeURL(remote.getUrl()), FileUtility.getWorkingCopyPath(view.getResource()));
			boolean ignoreExternals = SVNTeamPreferences.getBehaviourBoolean(
					SVNTeamUIPlugin.instance().getPreferenceStore(),
					SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME);
			GetRemoteContentsOperation mainOp = new GetRemoteContentsOperation(
					new IResource[] { view.getResource() }, new IRepositoryResource[] { remote }, remote2local,
					ignoreExternals);

			CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
			op.add(mainOp);
			op.add(new RefreshResourcesOperation(new IResource[] { view.getResource() }));

			UIMonitorUtility.doTaskScheduledWorkspaceModify(op);
		}
	}

	protected void doExport(ILogNode item) {
		if (item.getType() == ILogNode.TYPE_LOCAL) {
			final SVNLocalFileRevision revision = (SVNLocalFileRevision) item.getEntity();
			FileDialog dlg = new FileDialog(UIMonitorUtility.getShell(), SWT.PRIMARY_MODAL | SWT.SAVE);
			dlg.setText(SVNUIMessages.ExportPanel_Title);
			dlg.setFileName(revision.getName());
			dlg.setFilterExtensions(new String[] { "*.*" }); //$NON-NLS-1$
			final String file = dlg.open();
			if (file != null) {
				IActionOperation op = new AbstractActionOperation("Operation_ExportLocalHistory", SVNUIMessages.class) { //$NON-NLS-1$
					@Override
					protected void runImpl(IProgressMonitor monitor) throws Exception {
						FileOutputStream output = new FileOutputStream(file);
						InputStream input = null;
						try {
							IStorage storage = revision.getStorage(monitor);
							input = storage.getContents();
							byte[] data = new byte[2048];
							int len = 0;
							while ((len = input.read(data)) > 0) {
								output.write(data, 0, len);
							}
						} finally {
							try {
								output.close();
							} catch (IOException ex) {
							}
							if (input != null) {
								try {
									input.close();
								} catch (IOException ex) {
								}
							}
						}
					}
				};
				UIMonitorUtility.doTaskScheduledDefault(op);
			}
		} else {
			DirectoryDialog fileDialog = new DirectoryDialog(UIMonitorUtility.getShell());
			fileDialog.setText(SVNUIMessages.ExportPanel_ExportFolder);
			fileDialog.setMessage(SVNUIMessages.ExportPanel_ExportFolder_Msg);
			String path = fileDialog.open();
			if (path != null) {
				IRepositoryResource resource = traceResourceToRevision((SVNLogEntry) item.getEntity());
				boolean ignoreExternals = SVNTeamPreferences.getBehaviourBoolean(
						SVNTeamUIPlugin.instance().getPreferenceStore(),
						SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME);
				UIMonitorUtility.doTaskScheduledDefault(new ExportOperation(new IRepositoryResource[] { resource },
						path, SVNDepth.INFINITY, ignoreExternals));
			}
		}
	}

	protected IRepositoryResource traceResourceToRevision(SVNLogEntry to) {
		IRepositoryResource resource = getResourceForSelectedRevision(to);
		String rootUrl = resource.getRepositoryLocation().getRepositoryRootUrl();
		String url = traceUrlToRevision(rootUrl, resource.getUrl().substring(rootUrl.length()),
				view.getCurrentRevision(), to.revision);
		IRepositoryResource retVal = resource instanceof IRepositoryFile
				? resource.asRepositoryFile(url, false)
				: resource.asRepositoryContainer(url, false);
		return retVal;
	}

	protected void addRevisionLinks(ILogNode[] tSelection) {
		InputRevisionPanel panel = new InputRevisionPanel(null, false, null);
		DefaultDialog dlg = new DefaultDialog(UIMonitorUtility.getShell(), panel);
		if (dlg.open() == Window.OK) {
			final String comment = panel.getRevisionComment();
			IRepositoryLocation location = view.getRepositoryResource().getRepositoryLocation();

			CompositeOperation op = new CompositeOperation("Operation_HAddSelectedRevision", SVNUIMessages.class); //$NON-NLS-1$
			for (ILogNode node : tSelection) {
				SVNLogEntry item = (SVNLogEntry) node.getEntity();
				IRepositoryResource resource = SVNUtility.copyOf(view.getRepositoryResource());
				resource.setSelectedRevision(SVNRevision.fromNumber(item.revision));
				final LocateResourceURLInHistoryOperation locateOp = new LocateResourceURLInHistoryOperation(
						new IRepositoryResource[] { resource });
				op.add(locateOp);

				op.add(new AddRevisionLinkOperation((IRevisionLinkProvider) () -> {
					IRepositoryResource[] resources = locateOp.getRepositoryResources();
					IRevisionLink[] links = new IRevisionLink[resources.length];
					for (int i = 0; i < resources.length; i++) {
						links[i] = SVNUtility.createRevisionLink(resources[i]);
						links[i].setComment(comment);
					}
					return links;
				}, item.revision), new IActionOperation[] { locateOp });
			}
			op.add(new SaveRepositoryLocationsOperation());
			op.add(new RefreshRepositoryLocationsOperation(new IRepositoryLocation[] { location }, true));
			UIMonitorUtility.doTaskScheduledDefault(op);
		}
	}

	protected void updateTo(final SVNLogEntry item) {
		IResource[] resources = { view.getResource() };
		boolean ignoreExternals = SVNTeamPreferences.getBehaviourBoolean(
				SVNTeamUIPlugin.instance().getPreferenceStore(), SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME);
		CompositeOperation op = new CompositeOperation("Operation_HUpdateTo", SVNUIMessages.class); //$NON-NLS-1$
		SaveProjectMetaOperation saveOp = new SaveProjectMetaOperation(resources);
		op.add(saveOp);
		op.add(new UpdateOperation(resources, SVNRevision.fromNumber(item.revision), ignoreExternals));
		op.add(new RestoreProjectMetaOperation(saveOp));
		op.add(new RefreshResourcesOperation(resources));
		UIMonitorUtility.doTaskScheduledWorkspaceModify(op);
	}

	protected void createPatch(ILogNode[] selected) {
		CreatePatchWizard wizard = new CreatePatchWizard(view.getRepositoryResource().getName());
		WizardDialog dialog = new WizardDialog(UIMonitorUtility.getShell(), wizard);
		if (dialog.open() == Window.OK) {
			SVNLogEntry msg0 = (SVNLogEntry) selected[0].getEntity();
			IRepositoryResource next = getResourceForSelectedRevision(msg0);
			IRepositoryResource prev = null;
			if (selected.length == 1) {
				//FIXME peg revisions for renamed resources: (rev - 1) works only if the revision really exists in repository for current resource
				// use LocateUrlInHistory
				prev = getResourceForSelectedRevision(new SVNLogEntry(msg0.revision - 1, 0, null, null, null, false));
			} else {
				SVNLogEntry msg1 = (SVNLogEntry) selected[1].getEntity();
				prev = getResourceForSelectedRevision(msg1);
				if (msg0.revision < msg1.revision) {
					IRepositoryResource tmp = next;
					next = prev;
					prev = tmp;
				}
			}
			UIMonitorUtility.doTaskScheduledDefault(UIMonitorUtility.getActivePart(),
					CreatePatchAction.getCreatePatchOperation(prev, next, wizard));
		}
	}

	protected void handleCopy(ILogNode[] selection) {
		String historyText = getSelectedMessagesAsString(selection);
		Clipboard clipboard = new Clipboard(UIMonitorUtility.getDisplay());
		try {
			clipboard.setContents(new Object[] { historyText }, new Transfer[] { TextTransfer.getInstance() });
		} finally {
			clipboard.dispose();
		}
	}

	public String getSelectedMessagesAsString(ILogNode[] selection) {
		String[] historyText = { "" }; //$NON-NLS-1$
		HashSet<ILogNode> processed = new HashSet<>();
		long revision = view.getCurrentRevision();
		for (ILogNode node : selection) {
			this.toString(processed, node, revision, historyText);
		}
		return historyText[0];
	}

	protected void toString(HashSet<ILogNode> processed, ILogNode node, long revision, String[] retStr) {
		if (processed.contains(node)) {
			return;
		}
		processed.add(node);
		String toAdd = ""; //$NON-NLS-1$
		toAdd += node.getLabel(0, ILogNode.LABEL_FLAT, revision);
		for (int i = 1; i < ILogNode.NUM_OF_COLUMNS; i++) {
			toAdd += "\t" + node.getLabel(i, ILogNode.LABEL_FLAT, revision); //$NON-NLS-1$
		}
		toAdd = toAdd.trim();
		if (!toAdd.equals("")) //$NON-NLS-1$
		{
			retStr[0] += toAdd + System.lineSeparator();
		}
		if (node.hasChildren()) {
			ILogNode[] children = node.getChildren();
			for (ILogNode child : children) {
				this.toString(processed, child, revision, retStr);
			}
		}
	}

	protected String toString(HashSet<ILogNode> processed, ILogNode node, long revision) {
		if (processed.contains(node)) {
			return ""; //$NON-NLS-1$
		}
		processed.add(node);
		String historyText = node.getLabel(0, ILogNode.LABEL_FLAT, revision);
		for (int i = 1; i < ILogNode.NUM_OF_COLUMNS; i++) {
			historyText += "\t" + node.getLabel(i, ILogNode.LABEL_FLAT, revision); //$NON-NLS-1$
		}
		return historyText + System.lineSeparator();
	}

	protected void handleDoubleClick(TreeViewer viewer, ILogNode item, boolean doubleClick) {
		int type = item.getType();
		if (type == ILogNode.TYPE_CATEGORY) {
			if (viewer.getExpandedState(item)) {
				viewer.collapseToLevel(item, AbstractTreeViewer.ALL_LEVELS);
			} else {
				viewer.expandToLevel(item, AbstractTreeViewer.ALL_LEVELS);
			}
			return;
		}
		boolean isCompareAllowed = CoreExtensionsManager.instance()
				.getSVNConnectorFactory()
				.getSVNAPIVersion() >= ISVNConnectorFactory.APICompatibility.SVNAPI_1_5_x
				|| view.getRepositoryResource() instanceof IRepositoryFile;
		if ((view.getOptions() & ISVNHistoryView.COMPARE_MODE) != 0 && doubleClick && isCompareAllowed) {
			if (type == ILogNode.TYPE_SVN && !(item.getParent() instanceof SVNLogNode)) {
				compareWithCurrent((SVNLogEntry) item.getEntity());
			} else if (type == ILogNode.TYPE_LOCAL) {
				runCompareForLocal(new Object[] { item });
			}
		} else if (type == ILogNode.TYPE_LOCAL) {
			try {
				SVNLocalFileRevision selected = (SVNLocalFileRevision) item.getEntity();
				Utils.openEditor(UIMonitorUtility.getActivePage(), selected, new NullProgressMonitor());
			} catch (CoreException ex) {
				UILoggedOperation.reportError("Open Editor", ex);
			}
		} else if (!(view.getRepositoryResource() instanceof IRepositoryContainer) && type == ILogNode.TYPE_SVN
				&& !(item.getParent() instanceof SVNLogNode)) {
			UIMonitorUtility.doTaskScheduledActive(new OpenRemoteFileOperation(
					new IRepositoryFile[] {
							(IRepositoryFile) getResourceForSelectedRevision((SVNLogEntry) item.getEntity()) },
					OpenRemoteFileOperation.OPEN_DEFAULT));
		}
	}

	protected void compareWithCurrent(SVNLogEntry item) {
		IRepositoryResource resource = getResourceForSelectedRevision(item);
		if (view.getResource() != null || view.getCompareWith() != null) {
			ILocalResource local = SVNRemoteStorage.instance().asLocalResource(view.getCompareWith());
			CompareResourcesOperation op = new CompareResourcesOperation(local, resource);
			op.setForceId(getCompareForceId());
			UIMonitorUtility.doTaskScheduledActive(op);
		} else {
			CompareRepositoryResourcesOperation op = new CompareRepositoryResourcesOperation(
					getResourceForHeadRevision(), resource);
			op.setForceId(getCompareForceId());
			UIMonitorUtility.doTaskScheduledActive(op);
		}
	}

	protected void runExtractTo(ILogNode[] selection) {
		String path = null;
		DirectoryDialog fileDialog = new DirectoryDialog(UIMonitorUtility.getShell());
		fileDialog.setText(SVNUIMessages.ExtractToAction_Select_Title);
		fileDialog.setMessage(SVNUIMessages.ExtractToAction_Select_Description);
		path = fileDialog.open();
		if (path == null) {
			return;
		}
		SVNLogEntry[] selectedLogs = new SVNLogEntry[2];
		selectedLogs[0] = (SVNLogEntry) selection[0].getEntity();
		if (selection.length == 2) {
			selectedLogs[1] = (SVNLogEntry) selection[1].getEntity();
			if (selectedLogs[0].revision < selectedLogs[1].revision) {
				SVNLogEntry tmp = selectedLogs[0];
				selectedLogs[0] = selectedLogs[1];
				selectedLogs[1] = tmp;
			}
		} else {
			selectedLogs[1] = new SVNLogEntry(selectedLogs[0].revision - 1, 0, null, null, null, false);
		}
		HashMap<String, String> resource2project = new HashMap<>();
		IResource local = view.getResource();
		if (local != null) {
			IRepositoryResource remote = SVNRemoteStorage.instance().asRepositoryResource(local.getProject());
			resource2project.put(remote.getUrl(), local.getProject().getName());
		} else {
			IRepositoryResource remote = view.getRepositoryResource();
			resource2project.put(remote.getUrl(), remote.getName());
		}
		CompositeOperation op = new CompositeOperation(SVNMessages.Operation_ExtractTo, SVNMessages.class);
		InitExtractLogOperation logger = new InitExtractLogOperation(path);
		FromDifferenceRepositoryResourceProviderOperation provider = new FromDifferenceRepositoryResourceProviderOperation(
				getResourceForSelectedRevision(selectedLogs[0]), getResourceForSelectedRevision(selectedLogs[1]));
		op.add(provider);
		op.add(logger);
		op.add(new ExtractToOperationRemote(provider, provider.getDeletionsProvider(), path, resource2project, logger,
				true), new IActionOperation[] { provider });
		op.add(new FiniExtractLogOperation(logger));
		UIMonitorUtility.doTaskScheduledActive(op);
	}

	protected void runCompareForLocal(Object[] selection) {
		//FIXME reimplement without internals usage
		ArrayList<FileRevision> newSelection = new ArrayList<>();
		for (Object item : selection) {
			if (item instanceof ILogNode) {
				newSelection.add((SVNLocalFileRevision) ((ILogNode) item).getEntity());
			} else {
				newSelection.add((FileRevision) item);
			}
		}
		CompareRevisionAction compare = null;
		try {
			compare = CompareRevisionAction.class.getConstructor((Class[]) null).newInstance((Object[]) null);
			compare.getClass()
					.getMethod("setPage", HistoryPage.class) //$NON-NLS-1$
					.invoke(compare, view.getHistoryPage());
		} catch (Exception ex) {
			try {
				compare = CompareRevisionAction.class.getConstructor(String.class, HistoryPage.class)
						.newInstance("", view.getHistoryPage()); //$NON-NLS-1$
			} catch (RuntimeException ex1) {
				throw ex1;
			} catch (Exception ex1) {
				throw new RuntimeException(ex1);
			}
		}
		compare.selectionChanged(new StructuredSelection(newSelection));
		compare.setCurrentFileRevision(new SVNLocalFileRevision((IFile) view.getResource()));
		compare.run();
	}

	protected IRepositoryResource getResourceForHeadRevision() {
		IRepositoryResource res = view.getRepositoryResource() instanceof IRepositoryFile
				? (IRepositoryResource) ((IRepositoryRoot) view.getRepositoryResource().getRoot())
						.asRepositoryFile(view.getRepositoryResource().getUrl(), false)
				: ((IRepositoryRoot) view.getRepositoryResource().getRoot())
						.asRepositoryContainer(view.getRepositoryResource().getUrl(), false);
		res.setSelectedRevision(SVNRevision.HEAD);
		res.setPegRevision(SVNRevision.HEAD);
		return res;
	}

	protected String getCompareForceId() {
		return this.toString();
	}

	protected IRepositoryResource getResourceForSelectedRevision(SVNLogEntry item) {
		long revNum = item.revision;
		IRepositoryResource res = SVNUtility.copyOf(view.getRepositoryResource());
		res.setSelectedRevision(SVNRevision.fromNumber(revNum));
		res.setPegRevision(view.getRepositoryResource().getPegRevision());
		return res;
	}

	protected boolean confirmReplacement() {
		ReplaceWarningDialog dialog = new ReplaceWarningDialog(UIMonitorUtility.getShell());
		return dialog.open() == 0;
	}

	protected class AffectedTableActionManager implements IControlActionManager {
		@Override
		public void installKeyBindings(StructuredViewer viewer) {
		}

		@Override
		public void installDefaultAction(StructuredViewer viewer) {
			//add double click listener for the table viewer
			viewer.addDoubleClickListener(e -> {
				if (e.getSelection() instanceof IStructuredSelection) {
					IStructuredSelection selection = (IStructuredSelection) e.getSelection();
					if (selection.size() == 1) {
						SVNChangedPathData data = (SVNChangedPathData) selection.getFirstElement();
						if ((view.getOptions() & ISVNHistoryView.COMPARE_MODE) != 0) {
							boolean isPreviousExists = data.action == SVNLogPath.ChangeType.MODIFIED
									|| data.action == SVNLogPath.ChangeType.REPLACED
									|| data.copiedFromPath != null && !data.copiedFromPath.equals(""); //$NON-NLS-1$
							if (isPreviousExists) {
								FromChangedPathDataProvider provider = new FromChangedPathDataProvider(data, false);
								compareWithPreviousRevision(provider, provider);
							} else {
								MessageDialog dialog = new MessageDialog(e.getViewer().getControl().getShell(),
										SVNUIMessages.AffectedPathsComposite_NoPreviousRevision_Title, null,
										SVNUIMessages.AffectedPathsComposite_NoPreviousRevision_Message,
										MessageDialog.INFORMATION, new String[] { IDialogConstants.OK_LABEL }, 0);
								dialog.open();
							}
						} else {
							openRemoteResource(data, OpenRemoteFileOperation.OPEN_DEFAULT, null);
						}
					}
				}
			});
		}

		@Override
		public void installMenuActions(final StructuredViewer viewer, IWorkbenchPartSite site) {
			MenuManager menuMgr = new MenuManager();
			Menu menu = menuMgr.createContextMenu(viewer.getControl());
			menuMgr.addMenuListener(manager -> {
				manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
				final IStructuredSelection affectedTableSelection = (IStructuredSelection) viewer.getSelection();
				if (affectedTableSelection.size() == 0) {
					return;
				}
				final SVNChangedPathData firstData = (SVNChangedPathData) affectedTableSelection.getFirstElement();
				Action tAction = null;

				IEditorRegistry editorRegistry = SVNTeamUIPlugin.instance().getWorkbench().getEditorRegistry();
				manager.add(tAction = new HistoryAction(SVNUIMessages.HistoryView_Open) {
					@Override
					public void run() {
						openRemoteResource(firstData, OpenRemoteFileOperation.OPEN_DEFAULT, null);
					}
				});
				String name = firstData.resourceName;
				tAction.setImageDescriptor(editorRegistry.getImageDescriptor(name));
				tAction.setEnabled(affectedTableSelection.size() == 1);

				//FIXME: "Open with" submenu shouldn't be hardcoded after reworking of
				//       the HistoryView. Should be made like the RepositoriesView menu.
				MenuManager sub = new MenuManager(SVNUIMessages.HistoryView_OpenWith, "historyOpenWithMenu"); //$NON-NLS-1$
				sub.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

				sub.add(new Separator("nonDefaultTextEditors")); //$NON-NLS-1$
				IEditorDescriptor[] editors = editorRegistry.getEditors(name);
				for (IEditorDescriptor editor : editors) {
					final String id = editor.getId();
					if (!id.equals(EditorsUI.DEFAULT_TEXT_EDITOR_ID)) {
						sub.add(tAction = new HistoryAction(SVNUIMessages.getString(editor.getLabel())) {
							@Override
							public void run() {
								openRemoteResource(firstData, OpenRemoteFileOperation.OPEN_SPECIFIED, id);
							}
						});
						tAction.setImageDescriptor(editor.getImageDescriptor());
						tAction.setEnabled(affectedTableSelection.size() == 1);
					}
				}

				sub.add(new Separator("variousEditors")); //$NON-NLS-1$
				IEditorDescriptor descriptor = null;
				sub.add(tAction = new HistoryAction(SVNUIMessages.HistoryView_TextEditor) {
					@Override
					public void run() {
						openRemoteResource(firstData, OpenRemoteFileOperation.OPEN_SPECIFIED,
								EditorsUI.DEFAULT_TEXT_EDITOR_ID);
					}
				});
				descriptor = editorRegistry.findEditor(EditorsUI.DEFAULT_TEXT_EDITOR_ID);
				tAction.setImageDescriptor(descriptor.getImageDescriptor());
				tAction.setEnabled(affectedTableSelection.size() == 1);
				sub.add(tAction = new HistoryAction(SVNUIMessages.HistoryView_SystemEditor) {
					@Override
					public void run() {
						openRemoteResource(firstData, OpenRemoteFileOperation.OPEN_EXTERNAL, null);
					}
				});
				if (editorRegistry.isSystemExternalEditorAvailable(name)) {
					tAction.setImageDescriptor(editorRegistry.getSystemExternalEditorImageDescriptor(name));
					tAction.setEnabled(affectedTableSelection.size() == 1);
				} else {
					tAction.setEnabled(false);
				}
				sub.add(tAction = new HistoryAction(SVNUIMessages.HistoryView_InplaceEditor) {
					@Override
					public void run() {
						openRemoteResource(firstData, OpenRemoteFileOperation.OPEN_INPLACE, null);
					}
				});
				if (editorRegistry.isSystemInPlaceEditorAvailable(name)) {
					tAction.setImageDescriptor(editorRegistry.getSystemExternalEditorImageDescriptor(name));
					tAction.setEnabled(affectedTableSelection.size() == 1);
				} else {
					tAction.setEnabled(false);
				}
				sub.add(tAction = new HistoryAction(SVNUIMessages.HistoryView_DefaultEditor) {
					@Override
					public void run() {
						openRemoteResource(firstData, OpenRemoteFileOperation.OPEN_DEFAULT, null);
					}
				});
				tAction.setImageDescriptor(editorRegistry.getImageDescriptor(name));
				tAction.setEnabled(affectedTableSelection.size() == 1);

				manager.add(sub);

				manager.add(new Separator());

				boolean isPreviousExists = false;
				if (affectedTableSelection.size() > 0) {
					isPreviousExists = checkSelectionForExistanceInPrev(affectedTableSelection);
				}
				manager.add(tAction = new HistoryAction(SVNUIMessages.HistoryView_CompareWithPrevious) {
					@Override
					public void run() {
						FromChangedPathDataProvider provider = new FromChangedPathDataProvider(firstData, false);
						compareWithPreviousRevision(provider, provider);
					}
				});
				tAction.setEnabled(isPreviousExists);

				manager.add(new Separator());

				manager.add(tAction = new HistoryAction(SVNUIMessages.ShowPropertiesAction_label,
						"icons/views/propertiesedit.gif") { //$NON-NLS-1$
					@Override
					public void run() {
						FromChangedPathDataProvider provider = new FromChangedPathDataProvider(firstData, false);
						showProperties(provider, provider);
					}
				});
				tAction.setEnabled(affectedTableSelection.size() == 1);
				manager.add(tAction = new HistoryAction(SVNUIMessages.ShowResourceHistoryCommand_label,
						"icons/views/history.gif") { //$NON-NLS-1$
					@Override
					public void run() {
						FromChangedPathDataProvider provider = new FromChangedPathDataProvider(firstData, false);
						showHistory(provider, provider);
					}
				});
				tAction.setEnabled(affectedTableSelection.size() == 1);
				manager.add(tAction = new HistoryAction(SVNUIMessages.ShowAnnotationCommand_label) {
					@Override
					public void run() {
						showAnnotation(firstData);
					}
				});
				tAction.setEnabled(affectedTableSelection.size() == 1);

				manager.add(new Separator());

				manager.add(
						tAction = new HistoryAction(SVNUIMessages.ExportCommand_label, "icons/common/export.gif") { //$NON-NLS-1$
							@Override
							public void run() {
								FromChangedPathDataProvider provider = new FromChangedPathDataProvider(firstData,
										false);
								HistoryActionManager.this.doExport(viewer.getControl().getShell(), provider,
										provider);
							}
						});
				tAction.setEnabled(
						affectedTableSelection.size() > 0 && firstData.action != SVNLogPath.ChangeType.DELETED);
				manager.add(tAction = new HistoryAction(SVNUIMessages.CreatePatchCommand_label) {
					@Override
					public void run() {
						FromChangedPathDataProvider provider = new FromChangedPathDataProvider(firstData, false);
						createPatchToPrevious(viewer.getControl().getShell(), provider, provider);
					}
				});
				tAction.setEnabled(isPreviousExists);

				if (view.getResource() != null) {
					manager.add(tAction = new HistoryAction(SVNUIMessages.HistoryView_GetContents) {
						@Override
						public void run() {
							FromChangedPathDataProvider provider = new FromChangedPathDataProvider(firstData,
									false);
							getContentAffected(provider, provider, "/" + firstData.getFullResourcePath()); //$NON-NLS-1$
						}
					});
					tAction.setEnabled(
							affectedTableSelection.size() > 0 && firstData.action != SVNLogPath.ChangeType.DELETED);
				}
				manager.add(new Separator());

				String branchFrom = BaseMessages.format(SVNUIMessages.HistoryView_BranchFrom,
						new String[] { String.valueOf(selectedRevision) });
				String tagFrom = BaseMessages.format(SVNUIMessages.HistoryView_TagFrom,
						new String[] { String.valueOf(selectedRevision) });
				manager.add(tAction = new HistoryAction(branchFrom, "icons/common/actions/branch.gif") { //$NON-NLS-1$
					@Override
					public void run() {
						FromChangedPathDataProvider provider = new FromChangedPathDataProvider(firstData, false);
						createBranchTag(viewer.getControl().getShell(), provider, provider,
								BranchTagAction.BRANCH_ACTION);
					}
				});
				tAction.setEnabled(
						affectedTableSelection.size() > 0 && firstData.action != SVNLogPath.ChangeType.DELETED);
				manager.add(tAction = new HistoryAction(tagFrom, "icons/common/actions/tag.gif") { //$NON-NLS-1$
					@Override
					public void run() {
						FromChangedPathDataProvider provider = new FromChangedPathDataProvider(firstData, false);
						createBranchTag(viewer.getControl().getShell(), provider, provider,
								BranchTagAction.TAG_ACTION);
					}
				});
				tAction.setEnabled(
						affectedTableSelection.size() > 0 && firstData.action != SVNLogPath.ChangeType.DELETED);
				manager.add(tAction = new HistoryAction(SVNUIMessages.AddRevisionLinkAction_label) {
					@Override
					public void run() {
						FromChangedPathDataProvider provider = new FromChangedPathDataProvider(firstData, false);
						addRevisionLink(provider, provider);
					}
				});
				tAction.setEnabled(
						affectedTableSelection.size() > 0 && firstData.action != SVNLogPath.ChangeType.DELETED);
			});
			menuMgr.setRemoveAllWhenShown(true);
			viewer.getControl().setMenu(menu);
			site.registerContextMenu(menuMgr, viewer);
		}
	}

	protected class AffectedTreeActionManager implements IControlActionManager {
		@Override
		public void installKeyBindings(StructuredViewer viewer) {
		}

		@Override
		public void installDefaultAction(StructuredViewer viewer) {
		}

		@Override
		public void installMenuActions(final StructuredViewer viewer, IWorkbenchPartSite site) {
			MenuManager menuMgr = new MenuManager();
			Menu menu = menuMgr.createContextMenu(viewer.getControl());
			menuMgr.addMenuListener(manager -> {
				manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

				final IStructuredSelection affectedTableSelection = (IStructuredSelection) viewer.getSelection();
				if (affectedTableSelection.size() == 0) {
					return;
				}
				final AffectedPathsNode node = (AffectedPathsNode) affectedTableSelection.getFirstElement();

				Action tAction = null;
				manager.add(tAction = new HistoryAction(SVNUIMessages.HistoryView_CompareWithPrevious) {
					@Override
					public void run() {
						FromAffectedPathsNodeProvider provider = new FromAffectedPathsNodeProvider(node);
						compareWithPreviousRevision(provider, provider);
					}
				});
				boolean isCompareFoldersAllowed = CoreExtensionsManager.instance()
						.getSVNConnectorFactory()
						.getSVNAPIVersion() >= ISVNConnectorFactory.APICompatibility.SVNAPI_1_5_x;
				tAction.setEnabled(
						isCompareFoldersAllowed && selectedRevision != 0 && affectedTableSelection.size() == 1
								&& (node.getStatus() == null || node.getStatus() == SVNLogPath.ChangeType.MODIFIED
										|| node.getStatus() == SVNLogPath.ChangeType.REPLACED));

				manager.add(new Separator());

				manager.add(tAction = new HistoryAction(SVNUIMessages.ShowPropertiesAction_label,
						"icons/views/propertiesedit.gif") { //$NON-NLS-1$
					@Override
					public void run() {
						FromAffectedPathsNodeProvider provider = new FromAffectedPathsNodeProvider(node);
						showProperties(provider, provider);
					}
				});
				tAction.setEnabled(selectedRevision != 0 && affectedTableSelection
						.size() == 1 /*&& (node.getStatus() == null || node.getStatus().charAt(0) == 'M')*/);
				manager.add(tAction = new HistoryAction(SVNUIMessages.ShowResourceHistoryCommand_label,
						"icons/views/history.gif") { //$NON-NLS-1$
					@Override
					public void run() {
						FromAffectedPathsNodeProvider provider = new FromAffectedPathsNodeProvider(node);
						showHistory(provider, provider);
					}
				});
				tAction.setEnabled(
						selectedRevision != 0 && affectedTableSelection.size() == 1);

				manager.add(new Separator());

				manager.add(
						tAction = new HistoryAction(SVNUIMessages.ExportCommand_label, "icons/common/export.gif") { //$NON-NLS-1$
							@Override
							public void run() {
								FromAffectedPathsNodeProvider provider = new FromAffectedPathsNodeProvider(node);
								HistoryActionManager.this.doExport(viewer.getControl().getShell(), provider,
										provider);
							}
						});
				tAction.setEnabled(
						affectedTableSelection.size() > 0 && node.getStatus() != SVNLogPath.ChangeType.DELETED);
				manager.add(tAction = new HistoryAction(SVNUIMessages.CreatePatchCommand_label) {
					@Override
					public void run() {
						FromAffectedPathsNodeProvider provider = new FromAffectedPathsNodeProvider(node);
						createPatchToPrevious(viewer.getControl().getShell(), provider, provider);
					}
				});
				tAction.setEnabled(affectedTableSelection.size() == 1 && selectedRevision != 0
						&& affectedTableSelection.size() == 1
						&& (node.getStatus() == null || node.getStatus() == SVNLogPath.ChangeType.MODIFIED));

				if (view.getResource() != null) {
					manager.add(tAction = new HistoryAction(SVNUIMessages.HistoryView_GetContents) {
						@Override
						public void run() {
							FromAffectedPathsNodeProvider provider = new FromAffectedPathsNodeProvider(node);
							getContentAffected(provider, provider, node.getFullPath());
						}
					});
					tAction.setEnabled(
							affectedTableSelection.size() > 0 && node.getStatus() != SVNLogPath.ChangeType.DELETED);
				}

				manager.add(new Separator());

				manager.add(tAction = new HistoryAction(SVNUIMessages.HistoryView_BranchFrom,
						new String[] { String.valueOf(selectedRevision) }, "icons/common/actions/branch.gif") { //$NON-NLS-1$
					@Override
					public void run() {
						FromAffectedPathsNodeProvider provider = new FromAffectedPathsNodeProvider(node);
						createBranchTag(viewer.getControl().getShell(), provider, provider,
								BranchTagAction.BRANCH_ACTION);
					}
				});
				tAction.setEnabled(
						affectedTableSelection.size() > 0 && node.getStatus() != SVNLogPath.ChangeType.DELETED);
				manager.add(tAction = new HistoryAction(SVNUIMessages.HistoryView_TagFrom,
						new String[] { String.valueOf(selectedRevision) }, "icons/common/actions/tag.gif") { //$NON-NLS-1$
					@Override
					public void run() {
						FromAffectedPathsNodeProvider provider = new FromAffectedPathsNodeProvider(node);
						createBranchTag(viewer.getControl().getShell(), provider, provider,
								BranchTagAction.TAG_ACTION);
					}
				});
				tAction.setEnabled(
						affectedTableSelection.size() > 0 && node.getStatus() != SVNLogPath.ChangeType.DELETED);
				manager.add(tAction = new HistoryAction(SVNUIMessages.AddRevisionLinkAction_label) {
					@Override
					public void run() {
						FromAffectedPathsNodeProvider provider = new FromAffectedPathsNodeProvider(node);
						addRevisionLink(provider, provider);
					}
				});
				tAction.setEnabled(
						affectedTableSelection.size() > 0 && node.getStatus() != SVNLogPath.ChangeType.DELETED);
			});
			menuMgr.setRemoveAllWhenShown(true);
			viewer.getControl().setMenu(menu);
			site.registerContextMenu(menuMgr, viewer);
		}

	}

	protected boolean checkSelectionForExistanceInPrev(IStructuredSelection selection) {
		for (Object next : selection) {
			if (next instanceof SVNChangedPathData) {
				SVNChangedPathData current = (SVNChangedPathData) next;
				if (!(current.action == SVNLogPath.ChangeType.MODIFIED
						|| current.action == SVNLogPath.ChangeType.REPLACED
						|| current.copiedFromPath != null && !current.copiedFromPath.equals(""))) { //$NON-NLS-1$
					return false;
				}
			}
		}
		return true;
	}

	protected void createPatchToPrevious(Shell shell, IActionOperation preOp, IRepositoryResourceProvider provider) {
		if (UIMonitorUtility.doTaskNowDefault(shell, preOp, true).isCancelled()) {
			return;
		}

		IRepositoryResource current = provider.getRepositoryResources()[0];
		CreatePatchWizard wizard = new CreatePatchWizard(current.getName());
		WizardDialog dialog = new WizardDialog(shell, wizard);
		if (dialog.open() == Window.OK) {
			IRepositoryResource previous = current instanceof RepositoryFolder
					? current.asRepositoryContainer(current.getUrl(), false)
					: current.asRepositoryFile(current.getUrl(), false);
			previous.setSelectedRevision(SVNRevision.fromNumber(HistoryActionManager.this.selectedRevision - 1));
			previous.setPegRevision(SVNRevision.fromNumber(HistoryActionManager.this.selectedRevision));
			UIMonitorUtility
					.doTaskScheduledDefault(CreatePatchAction.getCreatePatchOperation(previous, current, wizard));
		}
	}

	protected void createBranchTag(Shell shell, IActionOperation preOp, IRepositoryResourceProvider provider,
			int type) {
		if (UIMonitorUtility.doTaskNowDefault(shell, preOp, true).isCancelled()) {
			return;
		}

		IRepositoryResource[] resources = provider.getRepositoryResources();
		PreparedBranchTagOperation op = BranchTagAction.getBranchTagOperation(resources, shell, type);

		if (op != null) {
			CompositeOperation composite = new CompositeOperation(op.getId(), op.getMessagesClass());
			composite.add(op);
			RefreshRemoteResourcesOperation refreshOp = new RefreshRemoteResourcesOperation(
					new IRepositoryResource[] { op.getDestination().getParent() });
			composite.add(refreshOp, new IActionOperation[] { op });
			UIMonitorUtility.doTaskScheduledDefault(op);
		}
	}

	protected void doExport(Shell shell, IActionOperation preOp, IRepositoryResourceProvider provider) {
		DirectoryDialog fileDialog = new DirectoryDialog(shell);
		fileDialog.setText(SVNUIMessages.ExportPanel_ExportFolder);
		fileDialog.setMessage(SVNUIMessages.ExportPanel_ExportFolder_Msg);
		String path = fileDialog.open();
		if (path != null) {
			boolean ignoreExternals = SVNTeamPreferences.getBehaviourBoolean(
					SVNTeamUIPlugin.instance().getPreferenceStore(),
					SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME);
			ExportOperation mainOp = new ExportOperation(provider, path, SVNDepth.INFINITY, ignoreExternals);
			CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
			op.add(preOp);
			op.add(mainOp, new IActionOperation[] { preOp });
			UIMonitorUtility.doTaskScheduledDefault(op);
		}
	}

	protected void openRemoteResource(SVNChangedPathData selectedPath, int openType, String openWith) {
		FromChangedPathDataProvider provider = new FromChangedPathDataProvider(selectedPath, true);
		OpenRemoteFileOperation openOp = new OpenRemoteFileOperation(provider, openType, openWith);

		CompositeOperation composite = new CompositeOperation(openOp.getId(), openOp.getMessagesClass(), true);
		composite.add(provider);
		composite.add(openOp, new IActionOperation[] { provider });
		UIMonitorUtility.doTaskScheduledActive(composite);
	}

	protected void showAnnotation(SVNChangedPathData selectedPath) {
		FromChangedPathDataProvider provider = new FromChangedPathDataProvider(selectedPath, true);
		RemoteShowAnnotationOperation mainOp = new RemoteShowAnnotationOperation(provider);
		CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass(), true);
		op.add(provider);
		op.add(mainOp, new IActionOperation[] { provider });
		UIMonitorUtility.doTaskScheduledActive(op);
	}

	protected void showHistory(IActionOperation preOp, IRepositoryResourceProvider provider) {
		ShowHistoryViewOperation mainOp = new ShowHistoryViewOperation(provider, 0, 0);
		CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
		op.add(preOp);
		op.add(mainOp, new IActionOperation[] { preOp });
		UIMonitorUtility.doTaskScheduledActive(op);
	}

	protected void showProperties(IActionOperation preOp, IRepositoryResourceProvider provider) {
		IResourcePropertyProvider propertyProvider = new GetRemotePropertiesOperation(provider);
		ShowPropertiesOperation mainOp = new ShowPropertiesOperation(UIMonitorUtility.getActivePage(), provider,
				propertyProvider);
		CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
		op.add(preOp);
		op.add(mainOp, new IActionOperation[] { preOp });
		UIMonitorUtility.doTaskScheduledActive(op);
	}

	protected void getContentAffected(IActionOperation preOp, IRepositoryResourceProvider provider, String remotePath) {
		String rootUrl = SVNRemoteStorage.instance()
				.asRepositoryResource(view.getResource())
				.getRepositoryLocation()
				.getRepositoryRootUrl();
		String remoteViewedResourceUrl = SVNRemoteStorage.instance().asRepositoryResource(view.getResource()).getUrl();
		String remoteFoundPath = traceUrlToRevision(rootUrl, remotePath, view.getCurrentRevision(), selectedRevision);
		if (!remoteFoundPath.startsWith(remoteViewedResourceUrl)) {
			MessageDialog dialog = new MessageDialog(UIMonitorUtility.getShell(),
					SVNUIMessages.AffectedPathsActions_CantGetContent_Title, null,
					SVNUIMessages.AffectedPathsActions_CantGetContent_Message, MessageDialog.INFORMATION,
					new String[] { IDialogConstants.OK_LABEL }, 0);
			dialog.open();
			return;
		}
		if (!confirmReplacement()) {
			return;
		}
		IPath resourcePath = new Path(remoteFoundPath.substring(remoteViewedResourceUrl.length()));
		IResource resourceToLock;
		HashMap<String, String> remote2local = new HashMap<>();
		if (view.getResource() instanceof IContainer) {
			IContainer viewedResource = (IContainer) view.getResource();
			remote2local.put(SVNUtility.encodeURL(rootUrl + remotePath),
					FileUtility.getWorkingCopyPath(viewedResource).concat(resourcePath.toString()));
			resourceToLock = viewedResource.findMember(resourcePath);
			while (resourceToLock == null) {
				resourcePath = resourcePath.removeLastSegments(1);
				resourceToLock = viewedResource.findMember(resourcePath);
			}
		} else {
			IFile viewedResource = (IFile) view.getResource();
			remote2local.put(SVNUtility.encodeURL(rootUrl + remotePath),
					FileUtility.getWorkingCopyPath(viewedResource).concat(resourcePath.toString()));
			resourceToLock = viewedResource.getParent();
		}
		boolean ignoreExternals = SVNTeamPreferences.getBehaviourBoolean(
				SVNTeamUIPlugin.instance().getPreferenceStore(), SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME);
		GetRemoteContentsOperation mainOp = new GetRemoteContentsOperation(new IResource[] { resourceToLock }, provider,
				remote2local, ignoreExternals);
		CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
		op.add(preOp);
		op.add(mainOp, new IActionOperation[] { preOp });
		op.add(new RefreshResourcesOperation(new IResource[] { resourceToLock }));
		UIMonitorUtility.doTaskScheduledActive(op);
	}

	protected String traceUrlToRevision(String rootUrl, String resourcePath, long currentRevision,
			long selectedRevision) {
		String url = rootUrl + resourcePath;
		SVNLogEntry[] entries = view.getFullRemoteHistory();

		if (currentRevision == selectedRevision || entries[entries.length - 1].revision > currentRevision) {
			return url;
		}
		if (currentRevision > selectedRevision) {
			for (int i = entries.length - 1; i > -1; i--) {
				SVNLogEntry entry = entries[i];
				if (entry.revision < selectedRevision) {
					continue;
				}
				if (entry.revision > currentRevision) {
					break;
				}
				if (entry.changedPaths == null) {
					return url;
				}
				for (SVNLogPath path : entry.changedPaths) {
					if (path.copiedFromPath != null && url.endsWith(path.copiedFromPath)) {
						url = rootUrl + path.path;
						break;
					}
				}
			}
		} else {
			for (SVNLogEntry entry : entries) {
				if (entry.revision > selectedRevision) {
					continue;
				}
				if (entry.revision < currentRevision) {
					break;
				}
				if (entry.changedPaths == null) {
					return url;
				}
				for (SVNLogPath path : entry.changedPaths) {
					if (path.copiedFromPath != null && url.endsWith(path.path)) {
						url = rootUrl + path.copiedFromPath;
						break;
					}
				}
			}
		}
		return url;
	}

	protected void addRevisionLink(IActionOperation preOp, final IRepositoryResourceProvider provider) {
		CompositeOperation op = new CompositeOperation("Operation_HAddSelectedRevision", SVNUIMessages.class); //$NON-NLS-1$
		op.add(preOp);
		IActionOperation[] condition = { preOp };
		op.add(new AddRevisionLinkOperation((IRevisionLinkProvider) () -> {
			IRepositoryResource[] resources = provider.getRepositoryResources();
			final int[] dlgRes = new int[1];
			final InputRevisionPanel panel = new InputRevisionPanel(null, false, null);
			UIMonitorUtility.getDisplay().syncExec(() -> {
				DefaultDialog dialog = new DefaultDialog(UIMonitorUtility.getShell(), panel);
				dlgRes[0] = dialog.open();
			});
			String comment = null;
			if (dlgRes[0] == Window.OK) {
				comment = panel.getRevisionComment();
			} else {
				return new IRevisionLink[0];
			}
			IRevisionLink[] links = new IRevisionLink[resources.length];
			for (int i = 0; i < resources.length; i++) {
				links[i] = SVNUtility.createRevisionLink(resources[i]);
				links[i].setComment(comment);
			}
			return links;
		}, selectedRevision), condition);
		op.add(new SaveRepositoryLocationsOperation(), condition);
		op.add(new RefreshRepositoryLocationsOperation(
				new IRepositoryLocation[] { view.getRepositoryResource().getRepositoryLocation() }, true), condition);
		UIMonitorUtility.doTaskScheduledDefault(op);
	}

	protected void compareWithPreviousRevision(IActionOperation preOp, final IRepositoryResourceProvider provider) {
		CompareRepositoryResourcesOperation mainOp = new CompareRepositoryResourcesOperation(
				() -> {
					IRepositoryResource next = provider.getRepositoryResources()[0];
					IRepositoryResource prev = SVNUtility.copyOf(next);
					prev.setSelectedRevision(SVNRevision
							.fromNumber(((SVNRevision.Number) next.getSelectedRevision()).getNumber() - 1));
					return new IRepositoryResource[] { prev, next };
				});
		mainOp.setForceId(getCompareForceId());
		if (preOp != null) {
			CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
			op.add(preOp);
			op.add(mainOp, new IActionOperation[] { preOp });
			UIMonitorUtility.doTaskScheduledActive(op);
		} else {
			UIMonitorUtility.doTaskScheduledActive(mainOp);
		}
	}

	protected class FromAffectedPathsNodeProvider extends AbstractActionOperation
			implements IRepositoryResourceProvider {
		protected AffectedPathsNode affectedPathsItem;

		protected IRepositoryResource returnResource;

		public FromAffectedPathsNodeProvider(AffectedPathsNode affectedPathsItem) {
			super("Operation_GetRepositoryResource", SVNUIMessages.class); //$NON-NLS-1$
			this.affectedPathsItem = affectedPathsItem;
		}

		@Override
		protected void runImpl(IProgressMonitor monitor) throws Exception {
			String rootUrl = view.getRepositoryResource().getRepositoryLocation().getRepositoryRootUrl();
			String path = affectedPathsItem.getFullPath();

			String resourceUrl = rootUrl + (path.startsWith("/") ? "" : "/") + path; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			SVNRevision revision = SVNRevision.fromNumber(selectedRevision);

			IRepositoryLocation location = view.getRepositoryResource().getRepositoryLocation();
			returnResource = location.asRepositoryContainer(resourceUrl, false);
			returnResource.setSelectedRevision(revision);
			returnResource.setPegRevision(revision);
		}

		@Override
		public IRepositoryResource[] getRepositoryResources() {
			return new IRepositoryResource[] { returnResource };
		}
	}

	protected class FromChangedPathDataProvider extends AbstractActionOperation implements IRepositoryResourceProvider {
		protected IRepositoryResource[] repositoryResources;

		protected SVNChangedPathData affectedPathsItem;

		protected boolean filesOnly;

		public FromChangedPathDataProvider(SVNChangedPathData affectedPathsItem, boolean filesOnly) {
			super("Operation_GetRepositoryResource", SVNUIMessages.class); //$NON-NLS-1$
			this.affectedPathsItem = affectedPathsItem;
			this.filesOnly = filesOnly;
		}

		@Override
		protected void runImpl(IProgressMonitor monitor) throws Exception {
			String affectedPath = affectedPathsItem.getFullResourcePath();
			String rootUrl = view.getRepositoryResource().getRepositoryLocation().getRepositoryRootUrl();
			String resourceUrl = rootUrl + "/" + affectedPath; //$NON-NLS-1$
			SVNRevision revision = SVNRevision.fromNumber(affectedPathsItem.action == SVNLogPath.ChangeType.DELETED
					? selectedRevision - 1
					: selectedRevision);

			SVNEntryInfo info = null;
			IRepositoryLocation location = view.getRepositoryResource().getRepositoryLocation();
			ISVNConnector proxy = location.acquireSVNProxy();
			try {
				SVNEntryInfo[] infos = SVNUtility.info(proxy,
						new SVNEntryRevisionReference(SVNUtility.encodeURL(resourceUrl), revision, revision),
						SVNDepth.EMPTY, new SVNProgressMonitor(this, monitor, null));
				if (infos == null || infos.length == 0) {
					return;
				}
				info = infos[0];
			} finally {
				location.releaseSVNProxy(proxy);
			}

			if (info.kind == Kind.DIR && filesOnly) {
				final String message = BaseMessages.format(SVNUIMessages.AffectedPathsComposite_Open_Message,
						new String[] { SVNUtility.decodeURL(info.url) });
				UIMonitorUtility.getDisplay().syncExec(() -> {
					MessageDialog dialog = new MessageDialog(UIMonitorUtility.getDisplay().getActiveShell(),
							SVNUIMessages.AffectedPathsComposite_Open_Title, null, message,
							MessageDialog.INFORMATION, new String[] { IDialogConstants.OK_LABEL }, 0);
					dialog.open();
				});
				this.reportStatus(new Status(IStatus.WARNING, SVNTeamPlugin.NATURE_ID, IStatus.OK, message, null));
				return;
			}
			repositoryResources = new IRepositoryResource[1];
			repositoryResources[0] = info.kind == Kind.FILE
					? (IRepositoryResource) location.asRepositoryFile(resourceUrl, false)
					: location.asRepositoryContainer(resourceUrl, false);
			repositoryResources[0].setSelectedRevision(revision);
			repositoryResources[0].setPegRevision(revision);
		}

		@Override
		public IRepositoryResource[] getRepositoryResources() {
			return repositoryResources;
		}

	}
}
