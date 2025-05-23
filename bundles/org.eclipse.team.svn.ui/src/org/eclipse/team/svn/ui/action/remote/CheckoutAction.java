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

package org.eclipse.team.svn.ui.action.remote;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.remote.CheckoutOperation;
import org.eclipse.team.svn.core.resource.IRepositoryContainer;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryRoot;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.action.AbstractRepositoryModifyWorkspaceAction;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.extension.ExtensionsManager;
import org.eclipse.team.svn.ui.operation.ObtainProjectNameOperation;
import org.eclipse.team.svn.ui.panel.ListSelectionPanel;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * UI Checkout action
 * 
 * @author Alexander Gurov
 */
public class CheckoutAction extends AbstractRepositoryModifyWorkspaceAction {
	public CheckoutAction() {
	}

	@Override
	public void runImpl(IAction action) {
		final IRepositoryResource[] resources = getSelectedRepositoryResources();
		if (SVNTeamPreferences.getCheckoutBoolean(SVNTeamUIPlugin.instance().getPreferenceStore(),
				SVNTeamPreferences.CHECKOUT_RESPECT_PROJECT_STRUCTURE_NAME)) {
			runScheduled(new AbstractActionOperation("Operation_CheckLayout", SVNUIMessages.class) { //$NON-NLS-1$
				@Override
				protected void runImpl(IProgressMonitor monitor) throws Exception {
					final HashSet<IRepositoryResource> toCheckout = new HashSet<>();
					for (int i = 0; i < resources.length && !monitor.isCanceled(); i++) {
						int kind = ((IRepositoryRoot) resources[i].getRoot()).getKind();
						if (!resources[i].getRepositoryLocation().isStructureEnabled()
								|| kind != IRepositoryRoot.KIND_LOCATION_ROOT && kind != IRepositoryRoot.KIND_ROOT) {
							toCheckout.add(resources[i]);
						} else {
							IRepositoryContainer trunk = resources[i].asRepositoryContainer(
									resources[i].getRepositoryLocation().getTrunkLocation(), false);
							if (!trunk.exists()) {
								toCheckout.add(resources[i]);
							} else {
								IRepositoryFile projectFile = trunk.asRepositoryFile(".project", false); //$NON-NLS-1$
								if (projectFile.exists()) {
									toCheckout.add(trunk);
								} else {
									IRepositoryResource[] children = trunk.getChildren();
									for (IRepositoryResource child : children) {
										if (child instanceof IRepositoryContainer) {
											toCheckout.add(child);
										}
									}
								}
							}
						}
					}
					if (!monitor.isCanceled()) {
						UIMonitorUtility.getDisplay().syncExec(() -> {
							boolean ignoreExternals = SVNTeamPreferences.getBehaviourBoolean(
									SVNTeamUIPlugin.instance().getPreferenceStore(),
									SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME);
							IActionOperation op = ExtensionsManager.getInstance()
									.getCurrentCheckoutFactory()
									.getCheckoutOperation(UIMonitorUtility.getShell(),
											toCheckout.toArray(new IRepositoryResource[toCheckout.size()]), null,
											false, null, SVNDepth.INFINITY, ignoreExternals);
							if (op != null) {
								UIMonitorUtility.doTaskScheduledWorkspaceModify(op);
							}
						});
					}
				}

				@Override
				public int getOperationWeight() {
					return 2;
				}
			});
		} else {
			boolean ignoreExternals = SVNTeamPreferences.getBehaviourBoolean(
					SVNTeamUIPlugin.instance().getPreferenceStore(),
					SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME);
			IActionOperation op = ExtensionsManager.getInstance()
					.getCurrentCheckoutFactory()
					.getCheckoutOperation(getShell(), resources, null, false, null, SVNDepth.INFINITY, ignoreExternals);
			if (op != null) {
				runScheduled(op);
			}
		}
	}

	public static class NameSet {
		public final boolean caseInsensitiveOS;

		public final HashMap<String, String> existing;

		public NameSet() {
			existing = new HashMap<>();
			caseInsensitiveOS = FileUtility.isCaseInsensitiveOS();
		}

	}

	public static NameSet getExistingProjectNames() {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		NameSet set = new NameSet();
		for (IProject project : projects) {
			String path = !FileUtility.isRemoteProject(project)
					? FileUtility.getWorkingCopyPath(project)
					: project.getName();
			//if (FileUtility.isRemoteProject(project)) {
			set.existing.put(set.caseInsensitiveOS ? project.getName().toLowerCase() : project.getName(), path);
			//}
		}
		return set;
	}

	public static ArrayList getOperateResources(HashMap names2resources, final HashMap resources2names, Shell shell,
			final String location, boolean checkProjectExistance) {
		NameSet set = CheckoutAction.getExistingProjectNames();
		final HashMap existingResources = new HashMap();
		final HashMap existingFolders = new HashMap();
		ArrayList operateResources = new ArrayList();
		File folder;

		for (Iterator iter = names2resources.keySet().iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			String resourceName = FileUtility.formatResourceName(key);
			Object currentResource = names2resources.get(key);
			folder = new File(location + "/" + resourceName); //$NON-NLS-1$

			if (set.existing.containsKey(set.caseInsensitiveOS ? resourceName.toLowerCase() : resourceName)
					&& checkProjectExistance) {
				existingResources.put(resourceName, currentResource);
				if (!FileUtility.formatPath(folder.getAbsolutePath())
						.equals(set.existing.get(set.caseInsensitiveOS ? resourceName.toLowerCase() : resourceName))) {
					if (folder.exists()
							&& (folder.listFiles() != null && folder.listFiles().length > 0 || folder.isFile())) {
						existingFolders.put(resourceName, currentResource);
					}
				}
			} else if (folder.exists()
					&& (folder.listFiles() != null && folder.listFiles().length > 0 || folder.isFile())) {
				existingFolders.put(resourceName, currentResource);
			} else {
				operateResources.add(currentResource);
			}
		}

//		if some of chosen projects already exist in workspace - let the user decide which of them should be overriden
		if (existingResources.size() > 0 || existingFolders.size() > 0) {
			IStructuredContentProvider contentProvider = new IStructuredContentProvider() {
				@Override
				public Object[] getElements(Object inputElement) {
					Set existingSet = new HashSet(existingResources.keySet());
					existingSet.addAll(existingFolders.keySet());
					String[] retVal = (String[]) existingSet.toArray(new String[existingSet.size()]);
					Arrays.sort(retVal);
					return retVal;
				}

				@Override
				public void dispose() {
				}

				@Override
				public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				}
			};
			ITableLabelProvider labelProvider = new ITableLabelProvider() {

				@Override
				public Image getColumnImage(Object element, int columnIndex) {
					return null;
				}

				@Override
				public String getColumnText(Object element, int columnIndex) {
					if (columnIndex == 0) {
						return (String) element;
					} else if (columnIndex == 1) {
						boolean project = existingResources.containsKey(element);
						boolean folder = existingFolders.containsKey(element);

						if (project && folder) {
							return SVNUIMessages.CheckoutAction_Type2;
						} else if (project) {
							return SVNUIMessages.CheckoutAction_Type1;
						} else if (folder) {
							return new File(location + "/" + element).isDirectory() //$NON-NLS-1$
									? SVNUIMessages.CheckoutAction_Type3
									: SVNUIMessages.CheckoutAction_Type4;
						}
						return ""; //$NON-NLS-1$
					}
					return null;
				}

				@Override
				public void addListener(ILabelProviderListener listener) {
				}

				@Override
				public void dispose() {
				}

				@Override
				public boolean isLabelProperty(Object element, String property) {
					return false;
				}

				@Override
				public void removeListener(ILabelProviderListener listener) {
				}
			};
			String message = existingResources.size() > 1
					? SVNUIMessages.CheckoutAction_Selection_Description_Multi
					: SVNUIMessages.CheckoutAction_Selection_Description_Single;
			ListSelectionPanel panel = new ListSelectionPanel(existingResources, contentProvider, labelProvider,
					message,
					existingResources.size() > 1
							? SVNUIMessages.CheckoutAction_Selection_Message_Multi
							: SVNUIMessages.CheckoutAction_Selection_Message_Single,
					existingResources.size() > 1
							? SVNUIMessages.CheckoutAction_Selection_Title_Multi
							: SVNUIMessages.CheckoutAction_Selection_Title_Single,
					true);
			if (new DefaultDialog(shell, panel).open() == 0) {
				Object[] selection = panel.getResultSelections();
				for (Object element : selection) {
					Object selected = existingResources.get(element);
					selected = selected == null ? existingFolders.get(element) : selected;
					operateResources.add(selected);
				}
			} else {
				operateResources.clear();
			}
		}

		return operateResources;
	}

	public static IActionOperation getCheckoutOperation(Shell shell, IRepositoryResource[] resources,
			HashMap checkoutMap, boolean respectHierarchy, String location, SVNDepth depth, boolean ignoreExternals) {
		List resourceList = new ArrayList(Arrays.asList(resources));
		if (checkoutMap != null && checkoutMap.size() != resources.length) {
			for (Iterator iter = checkoutMap.entrySet().iterator(); iter.hasNext();) {
				IRepositoryResource currentProject = (IRepositoryResource) ((Map.Entry) iter.next()).getValue();
				if (!resourceList.contains(currentProject)) {
					iter.remove();
				}
			}
		}
		if (checkoutMap == null) {
			ObtainProjectNameOperation obtainOperation = new ObtainProjectNameOperation(resources);
			UIMonitorUtility.doTaskNowDefault(obtainOperation, true);
			if (obtainOperation.getExecutionState() != IStatus.OK) {
				return null;
			}
			checkoutMap = obtainOperation.getNames2Resources();
		}
		HashMap resources2names = CheckoutAction.getResources2Names(checkoutMap);
		ArrayList operateResources = CheckoutAction.getOperateResources(checkoutMap, resources2names, shell,
				ResourcesPlugin.getWorkspace().getRoot().getLocation().toString(), true);

		if (operateResources.size() > 0) {
			IRepositoryResource[] checkoutSet = (IRepositoryResource[]) operateResources
					.toArray(new IRepositoryResource[operateResources.size()]);
			HashMap operateMap = new HashMap();
			for (IRepositoryResource element : checkoutSet) {
				operateMap.put(resources2names.get(element), element);
			}

			return new CheckoutOperation(operateMap, respectHierarchy, location, depth, ignoreExternals);
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	public static HashMap getResources2Names(HashMap names2resources) {
		HashMap resources2Names = new HashMap();
		for (Iterator iter = names2resources.keySet().iterator(); iter.hasNext();) {
			String name = (String) iter.next();
			resources2Names.put(names2resources.get(name), name);
		}

		return resources2Names;
	}

}
