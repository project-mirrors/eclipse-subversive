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

package org.eclipse.team.svn.ui.wizard;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNProperty.BuiltIn;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.IResourcePropertyProvider;
import org.eclipse.team.svn.core.operation.LoggedOperation;
import org.eclipse.team.svn.core.operation.local.AddToSVNOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.RestoreProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.SaveProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.property.ConcatenateProperyDataOperation;
import org.eclipse.team.svn.core.operation.local.property.SetPropertiesOperation;
import org.eclipse.team.svn.core.operation.remote.CheckoutAsOperation;
import org.eclipse.team.svn.core.operation.remote.GetRemoteFolderChildrenOperation;
import org.eclipse.team.svn.core.operation.remote.LocateProjectsOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryContainer;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.action.remote.CheckoutAction;
import org.eclipse.team.svn.ui.extension.ExtensionsManager;
import org.eclipse.team.svn.ui.operation.MoveProjectsToWorkingSetOperation;
import org.eclipse.team.svn.ui.operation.ObtainProjectNameOperation;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.utility.DefaultOperationWrapperFactory;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.svn.ui.wizard.checkoutas.CheckoutAsFolderPage;
import org.eclipse.team.svn.ui.wizard.checkoutas.CheckoutMethodSelectionPage;
import org.eclipse.team.svn.ui.wizard.checkoutas.MultipleCheckoutMethodSelectionPage;
import org.eclipse.team.svn.ui.wizard.checkoutas.ProjectLocationSelectionPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.NewProjectAction;

/**
 * Checkout as wizard
 * 
 * @author Alexander Gurov
 */
public class CheckoutAsWizard extends AbstractSVNWizard {
	public static final int SIZING_WIZARD_WIDTH = 570;

	public static final int SIZING_WIZARD_HEIGHT = 500;

	protected HashMap names2resources;

	protected IRepositoryResource[] resources;

	protected MultipleCheckoutMethodSelectionPage multipleMethodPage;

	protected CheckoutMethodSelectionPage methodSelectionPage;

	protected ProjectLocationSelectionPage locationSelectionPage;

	protected CheckoutAsFolderPage selectFolderPage;

	protected String projectName;

	protected boolean singleMode;

	protected IActionOperation priorOp;

	public CheckoutAsWizard(IRepositoryResource[] resources) {
		this(resources, null);
	}

	public CheckoutAsWizard(IRepositoryResource[] resources, IActionOperation priorOp) {
		setWindowTitle(SVNUIMessages.CheckoutAsWizard_Title);
		setForcePreviousAndNextButtons(true);
		this.resources = resources;
		singleMode = this.resources.length == 1;
		this.priorOp = priorOp;
	}

	public boolean isUseNewProjectWizard() {
		return methodSelectionPage == null ? false : methodSelectionPage.isUseNewProjectWizard();
	}

	public boolean isFindProjectsSelected() {
		return singleMode
				? methodSelectionPage == null ? false : methodSelectionPage.isFindProjectsSelected()
				: multipleMethodPage == null ? false : multipleMethodPage.isFindProjectsSelected();
	}

	public boolean isCheckoutAsFolderSelected() {
		return singleMode
				? methodSelectionPage == null ? false : methodSelectionPage.isCheckoutAsFolderSelected()
				: multipleMethodPage == null ? false : multipleMethodPage.isCheckoutAsFolderSelected();
	}

	public IContainer getTargetFolder() {
		return selectFolderPage.getTargetFolder();
	}

	public SVNDepth getCheckoutDepth() {
		return singleMode
				? methodSelectionPage == null ? SVNDepth.INFINITY : methodSelectionPage.getdepth()
				: multipleMethodPage == null ? SVNDepth.INFINITY : multipleMethodPage.getdepth();
	}

	public String getProjectName() {
		return methodSelectionPage == null ? resources[0].getName() : methodSelectionPage.getProjectName();
	}

	public SVNRevision getRevisionToCheckoutFrom() {
		return methodSelectionPage != null
				? methodSelectionPage.getSelectedRevision()
				: multipleMethodPage.getSelectedRevision();
	}

	public String getLocation() {
		return isCheckoutAsFolderSelected()
				? FileUtility.getWorkingCopyPath(getTargetFolder())
				: locationSelectionPage.getLocation();
	}

	public String getWorkingSetName() {
		return locationSelectionPage.getWorkingSetName();
	}

	@Override
	public void addPages() {
		if (resources.length == 1) {
			projectName = fetchProjectName();
			GetRemoteFolderChildrenOperation op = new GetRemoteFolderChildrenOperation(
					(IRepositoryContainer) resources[0], false);
			UIMonitorUtility.doTaskBusy(op, new DefaultOperationWrapperFactory() {
				@Override
				public IActionOperation getLogged(IActionOperation operation) {
					return new LoggedOperation(operation);
				}
			});
			boolean isEclipseProject = false;
			if (op.getExecutionState() == IActionOperation.OK) {
				IRepositoryResource[] children = op.getChildren();
				for (IRepositoryResource child : children) {
					if (child.getName().equals(".project")) { //$NON-NLS-1$
						isEclipseProject = true;
						break;
					}
				}
			}
			addPage(methodSelectionPage = new CheckoutMethodSelectionPage(projectName, !isEclipseProject,
					resources[0]));
		} else {
			addPage(multipleMethodPage = new MultipleCheckoutMethodSelectionPage(resources));
		}
		addPage(selectFolderPage = new CheckoutAsFolderPage(resources));
		addPage(locationSelectionPage = new ProjectLocationSelectionPage(resources.length > 1, null));
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == selectFolderPage) {
			return null;
		}

		if (page instanceof CheckoutMethodSelectionPage) {
			if (methodSelectionPage.isUseNewProjectWizard() || methodSelectionPage.isFindProjectsSelected()) {
				return null;
			}
		} else if (page instanceof MultipleCheckoutMethodSelectionPage) {
			if (multipleMethodPage.isFindProjectsSelected()) {
				return null;
			}
		}

		if ((page == methodSelectionPage || page == multipleMethodPage) && !isCheckoutAsFolderSelected()) {
			return super.getNextPage(super.getNextPage(page));
		}

		return super.getNextPage(page);
	}

	@Override
	public boolean performFinish() {
		if (isFindProjectsSelected()) {
			final CompositeOperation op = getLocateProjectsOperation(resources, getCheckoutDepth(),
					getRevisionToCheckoutFrom());
			UIMonitorUtility.doTaskScheduledActive(op);
		} else if (obtainNames()) {
			doCheckout(getLocation(), getProjectName(), isUseNewProjectWizard(), getCheckoutDepth(),
					getWorkingSetName(), getRevisionToCheckoutFrom());
		}
		return true;
	}

	protected String fetchProjectName() {
		ObtainProjectNameOperation obtainOperation = new ObtainProjectNameOperation(resources);
		UIMonitorUtility.doTaskNowDefault(obtainOperation, true);
		if (obtainOperation.getExecutionState() != IStatus.OK) {
			return resources[0].getName();
		}
		names2resources = ExtensionsManager.getInstance()
				.getCurrentCheckoutFactory()
				.prepareName2resources(obtainOperation.getNames2Resources());
		return (String) names2resources.keySet().iterator().next();
	}

	protected boolean obtainNames() {
		if (names2resources == null) {
			ObtainProjectNameOperation obtainOperation = new ObtainProjectNameOperation(resources);
			UIMonitorUtility.doTaskNowDefault(obtainOperation, true);
			if (obtainOperation.getExecutionState() != IStatus.OK) {
				return false;
			}
			names2resources = ExtensionsManager.getInstance()
					.getCurrentCheckoutFactory()
					.prepareName2resources(obtainOperation.getNames2Resources());
		}
		return true;
	}

	protected void doCheckout(String location, String projectName, boolean useNewProjectWizard, SVNDepth depth,
			String workingSetName, SVNRevision revisionToCheckoutFrom) {
		if (!useNewProjectWizard && names2resources.size() == 1) {
			Object resource = names2resources.get(names2resources.keySet().iterator().next());
			names2resources.clear();
			names2resources.put(projectName, resource);
		}
		ArrayList operateResources = new ArrayList();
		if (useNewProjectWizard) {
			operateResources.add(names2resources.get(names2resources.keySet().iterator().next()));
		} else {
			operateResources = CheckoutAction.getOperateResources(names2resources,
					CheckoutAction.getResources2Names(names2resources), getShell(), location,
					!isCheckoutAsFolderSelected());
		}

		if (operateResources.size() > 0) {
			IActionOperation op = null;
			if (useNewProjectWizard) {
				// this is the wrong way in the multithreaded environment,
				// but it should work in the interactive environment of the Eclipse IDE
				// and, at the same time, right way is much complex and requires to override
				// too many of built-in Eclipse Team-Services functionality
				ProjectAdditionListener listener = new ProjectAdditionListener();
				ResourcesPlugin.getWorkspace().addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);
				new NewProjectAction(PlatformUI.getWorkbench().getActiveWorkbenchWindow()).run();
				ResourcesPlugin.getWorkspace().removeResourceChangeListener(listener);

				IProject selectedProject = listener.getProject();
				if (selectedProject != null) {
					op = prepareForOne((IRepositoryResource) operateResources.get(0), selectedProject.getName(),
							FileUtility.getResourcePath(selectedProject).removeLastSegments(1).toString(), true, depth,
							workingSetName, revisionToCheckoutFrom);
				}
			} else if (isCheckoutAsFolderSelected()) {
				Map resources2Names = new HashMap();
				if (names2resources != null) {
					for (Iterator it = names2resources.entrySet().iterator(); it.hasNext();) {
						Map.Entry entry = (Map.Entry) it.next();
						resources2Names.put(entry.getValue(), entry.getKey());
					}
				}
				Map mappings = getExternalsFolderNames(resources, resources2Names);
				IResource destinationRoot = ResourcesPlugin.getWorkspace()
						.getRoot()
						.findMember(selectFolderPage.getTargetFolder().getFullPath());
				ILocalResource localDest = SVNRemoteStorage.instance().asLocalResource(destinationRoot);
				if (IStateFilter.SF_INTERNAL_INVALID.accept(localDest)) {
					op = getCheckoutAsFolderOperationUnshared(getTargetFolder(), resources, mappings);
				} else {
					op = getCheckoutAsFolderOperation(getTargetFolder(), resources, mappings);
				}
			} else if (singleMode) {
				op = prepareForOne((IRepositoryResource) operateResources.get(0), projectName, location, false, depth,
						workingSetName, revisionToCheckoutFrom);
			} else {
				HashMap operateMap = new HashMap();
				for (Iterator iter = operateResources.iterator(); iter.hasNext();) {
					IRepositoryResource resource = (IRepositoryResource) iter.next();
					HashMap resources2names = CheckoutAction.getResources2Names(names2resources);
					operateMap.put(resources2names.get(resource), resource);
				}
				op = prepareForMultiple(operateMap, location, depth, workingSetName, revisionToCheckoutFrom);
			}
			if (op != null) {
				if (priorOp != null) {
					CompositeOperation tmp = new CompositeOperation(op.getId(), op.getMessagesClass());
					tmp.add(priorOp);
					tmp.add(op, new IActionOperation[] { priorOp });
					op = tmp;
				}
				UIMonitorUtility.doTaskScheduledActive(op);
			}
		}
	}

	protected IActionOperation getCheckoutAsFolderOperationUnshared(IContainer targetFolder,
			IRepositoryResource[] resources, Map mappings) {
		CompositeOperation op = new CompositeOperation(SVNUIMessages.Operation_CheckoutAsFolder, SVNUIMessages.class);
		for (IRepositoryResource element : resources) {
			IPath location = FileUtility.getResourcePath(targetFolder);
			File target = location.append((String) mappings.get(element)).toFile();
			IRepositoryResource modifiedResource = SVNUtility.copyOf(element);
			modifiedResource.setSelectedRevision(getRevisionToCheckoutFrom());
			op.add(new org.eclipse.team.svn.core.operation.file.CheckoutAsOperation(target, modifiedResource,
					getCheckoutDepth(),
					SVNTeamPreferences.getBehaviourBoolean(SVNTeamUIPlugin.instance().getPreferenceStore(),
							SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME),
					false));
		}
		IResource[] localResources = { targetFolder };
		op.add(new RefreshResourcesOperation(localResources), null);
		return op;
	}

	protected IActionOperation getCheckoutAsFolderOperation(IContainer targetFolder, IRepositoryResource[] resources,
			Map mappings) {
		String externalsData = ""; //$NON-NLS-1$
		for (IRepositoryResource element : resources) {
			String line = (String) mappings.get(element) + "\t" + SVNUtility.encodeURL(element.getUrl()) //$NON-NLS-1$
					+ "\n"; //$NON-NLS-1$

			externalsData += line;
		}

		CompositeOperation op = new CompositeOperation(SVNUIMessages.Operation_CheckoutAsFolder, SVNMessages.class);
		IActionOperation[] dependency = null;
		IResource[] localResources = { targetFolder };
		ILocalResource localResource = SVNRemoteStorage.instance().asLocalResourceAccessible(targetFolder);
		IResource[] newResources = null;
		if (IStateFilter.SF_UNVERSIONED.accept(localResource)) {
			newResources = FileUtility.addOperableParents(localResources, IStateFilter.SF_UNVERSIONED);
		}
		if (newResources != null && newResources.length > 0) {
			IActionOperation addToSVN = new AddToSVNOperation(newResources);
			op.add(addToSVN);
			dependency = new IActionOperation[] { addToSVN };
		}

		IResourcePropertyProvider concatenateProps = new ConcatenateProperyDataOperation(targetFolder,
				BuiltIn.EXTERNALS, externalsData.getBytes());
		op.add(concatenateProps, dependency);
		dependency = new IActionOperation[] { concatenateProps };

		SetPropertiesOperation setProps = new SetPropertiesOperation(localResources, concatenateProps, false);
		op.add(setProps, dependency);
		dependency = new IActionOperation[] { setProps };

		for (IRepositoryResource element : resources) {
			IPath location = targetFolder.getLocation();
			if (location != null) {
				File target = location.append((String) mappings.get(element)).toFile();
				IRepositoryResource modifiedResource = SVNUtility.copyOf(element);
				modifiedResource.setSelectedRevision(getRevisionToCheckoutFrom());
				op.add(new org.eclipse.team.svn.core.operation.file.CheckoutAsOperation(target, modifiedResource,
						getCheckoutDepth(),
						SVNTeamPreferences.getBehaviourBoolean(SVNTeamUIPlugin.instance().getPreferenceStore(),
								SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME),
						false), dependency);
			}
		}
		op.add(new RefreshResourcesOperation(localResources), dependency);
		return op;
	}

	protected Map getExternalsFolderNames(IRepositoryResource[] resources, Map resource2Name) {
		Map retVal = new HashMap();
		Set allNames = new HashSet();
		for (IRepositoryResource element : resources) {
			String name = (String) resource2Name.get(element);
			if (name == null) {
				name = element.getName();
			}
			name = getName(getTargetFolder(), name, allNames);
			retVal.put(element, name);
			allNames.add(name);
		}
		return retVal;
	}

	protected String getName(IContainer targetFolder, String baseName, Set allNames) {
		baseName = baseName.replace(' ', '_');
		if (targetFolder == null || !targetFolder.exists(new Path(baseName))) {
			return baseName;
		}
		String name;
		for (int i = 1; true; i++) {
			name = baseName + "_(" + i + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			if (!targetFolder.exists(new Path(name)) && !allNames.contains(name)) {
				break;
			}
		}
		return name;
	}

	protected CompositeOperation prepareForOne(IRepositoryResource resource, final String projectName, String location,
			boolean isUseNewProjectWizard, SVNDepth depth, String workingSetName, SVNRevision revisionToCheckoutFrom) {
		IRepositoryResource modifiedResource = SVNUtility.copyOf(resource);
		modifiedResource.setSelectedRevision(revisionToCheckoutFrom);
		CheckoutAsOperation mainOp = new CheckoutAsOperation(projectName, modifiedResource, location, depth,
				SVNTeamPreferences.getBehaviourBoolean(SVNTeamUIPlugin.instance().getPreferenceStore(),
						SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME));

		CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());

		if (isUseNewProjectWizard) {
			SaveProjectMetaOperation saveOp = new SaveProjectMetaOperation(new IResource[] { mainOp.getProject() }, ""); //$NON-NLS-1$
			op.add(saveOp);
			op.add(mainOp);
			mainOp.setRestoreOperation(new RestoreProjectMetaOperation(saveOp, true));
		} else {
			op.add(mainOp);
		}
		if (workingSetName != null) {
			op.add(new MoveProjectsToWorkingSetOperation(new IProject[] { mainOp.getProject() }, workingSetName));
		}

		return op;
	}

	protected CompositeOperation prepareForMultiple(HashMap name2resources, String location, SVNDepth depth,
			String workingSetName, SVNRevision revisionToCheckoutFrom) {
		CompositeOperation op = new CompositeOperation("", SVNUIMessages.class); //$NON-NLS-1$
		IResource[] locals = new IResource[name2resources.size()];
		String name;
		int i = 0;
		for (Iterator iter = name2resources.keySet().iterator(); iter.hasNext(); i++) {
			name = (String) iter.next();
			boolean ignoreExternals = SVNTeamPreferences.getBehaviourBoolean(
					SVNTeamUIPlugin.instance().getPreferenceStore(),
					SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME);
			IRepositoryResource modifiedResource = SVNUtility.copyOf((IRepositoryResource) name2resources.get(name));
			modifiedResource.setSelectedRevision(revisionToCheckoutFrom);
			CheckoutAsOperation mainOp = new CheckoutAsOperation(name, modifiedResource, false, location, depth,
					ignoreExternals);
			locals[i] = mainOp.getProject();
			op.add(mainOp);
			op.setOperationName(mainOp.getId());
		}
		if (workingSetName != null) {
			op.add(new MoveProjectsToWorkingSetOperation(locals, workingSetName));
		}

		return op;
	}

	protected CompositeOperation getLocateProjectsOperation(IRepositoryResource[] resources, SVNDepth depth,
			SVNRevision revisionToCheckoutFrom) {
		for (int i = 0; i < resources.length; i++) {
			IRepositoryResource tmpResource = SVNUtility.copyOf(resources[i]);
			tmpResource.setSelectedRevision(revisionToCheckoutFrom);
			resources[i] = tmpResource;
		}
		LocateProjectsOperation mainOp = new LocateProjectsOperation(resources,
				ExtensionsManager.getInstance().getCurrentCheckoutFactory().getLocateFilter(), 5); //TODO level limitation now is hardcoded

		final CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
		op.add(mainOp);
		IRepositoryResourceProvider provider = ExtensionsManager.getInstance()
				.getCurrentCheckoutFactory()
				.additionalProcessing(op, mainOp);
		ObtainProjectNameOperation obtainOperation = new ObtainProjectNameOperation(provider);
		op.add(obtainOperation, new IActionOperation[] { mainOp });
		op.add(getCheckoutProjectOperation(resources, obtainOperation, depth, revisionToCheckoutFrom),
				new IActionOperation[] { obtainOperation });

		return op;
	}

	protected AbstractActionOperation getCheckoutProjectOperation(final IRepositoryResource[] resources,
			final ObtainProjectNameOperation obtainOperation, final SVNDepth depth,
			final SVNRevision revisionToCheckoutFrom) {
		return new AbstractActionOperation("Operation_CheckoutProjects", SVNUIMessages.class) { //$NON-NLS-1$
			@Override
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				UIMonitorUtility.getDisplay().syncExec(() -> {
					HashMap name2resources = obtainOperation.getNames2Resources();
					if (name2resources.isEmpty()) {
						new MessageDialog(UIMonitorUtility.getShell(), getOperationResource("Title"), null, //$NON-NLS-1$
								getOperationResource("Message"), MessageDialog.INFORMATION, //$NON-NLS-1$
								new String[] { IDialogConstants.OK_LABEL }, 0).open();
						return;
					}
					CheckoutProjectsWizard wizard = new CheckoutProjectsWizard(resources, name2resources);
					WizardDialog dialog = new WizardDialog(UIMonitorUtility.getShell(), wizard);
					dialog.create();
					wizard.postInit();
					dialog.getShell()
							.setSize(Math.max(SIZING_WIZARD_WIDTH, dialog.getShell().getSize().x),
									SIZING_WIZARD_HEIGHT);
					if (dialog.open() == 0) {//finish button pressed
						List selection = wizard.getResultSelections();
						IActionOperation op;
						final Set projectNames;
						if (wizard.isCheckoutAsFoldersSelected()) {
							projectNames = null;
							Map resources2Names = new HashMap();
							if (names2resources != null) {
								for (Iterator it = names2resources.entrySet().iterator(); it.hasNext();) {
									Map.Entry entry = (Map.Entry) it.next();
									resources2Names.put(entry.getValue(), entry.getKey());
								}
							}
							Map mappings = CheckoutAsWizard.this.getExternalsFolderNames(resources,
									resources2Names);
							op = CheckoutAsWizard.this.getCheckoutAsFolderOperation(wizard.getTargetFolder(),
									(IRepositoryResource[]) selection
											.toArray(new IRepositoryResource[selection.size()]),
									mappings);
						} else {
							HashMap selectedMap = new HashMap();
							List projects = new ArrayList();
							projectNames = name2resources.keySet();
							for (Iterator iter = projectNames.iterator(); iter.hasNext();) {
								String projName = (String) iter.next();
								if (wizard.getResultSelections().contains(name2resources.get(projName))) {
									selectedMap.put(projName, name2resources.get(projName));
									projects.add(name2resources.get(projName));
								}
							}
							boolean ignoreExternals = SVNTeamPreferences.getBehaviourBoolean(
									SVNTeamUIPlugin.instance().getPreferenceStore(),
									SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME);
							op = ExtensionsManager.getInstance()
									.getCurrentCheckoutFactory()
									.getCheckoutOperation(CheckoutAsWizard.this.getShell(),
											(IRepositoryResource[]) projects
													.toArray(new IRepositoryResource[projects.size()]),
											selectedMap, wizard.isRespectHierarchy(), wizard.getLocation(), depth,
											ignoreExternals);
						}
						if (op != null) {
							String wsName = wizard.getWorkingSetName();
							if (priorOp != null || wsName != null) {
								CompositeOperation tmp = new CompositeOperation(op.getId(), op.getMessagesClass());
								if (priorOp != null) {
									tmp.add(priorOp);
									tmp.add(op, new IActionOperation[] { priorOp });
								} else {
									tmp.add(op);
								}
								if (wsName != null) {
									tmp.add(new MoveProjectsToWorkingSetOperation(() -> {
										List projects = new ArrayList();
										for (Iterator it = projectNames.iterator(); it.hasNext();) {
											String name = (String) it.next();
											IProject prj = ResourcesPlugin.getWorkspace()
													.getRoot()
													.getProject(name);
											if (prj != null) {
												projects.add(prj);
											}
										}
										return (IProject[]) projects.toArray(new IProject[projects.size()]);
									}, wsName));
								}
								op = tmp;
							}
							UIMonitorUtility.doTaskScheduledActive(op);
						}
					}
				});
			}
		};
	}

	@Override
	public boolean canFinish() {
		IWizardPage currentPage = getContainer().getCurrentPage();
		if ((currentPage instanceof CheckoutMethodSelectionPage
				|| currentPage instanceof MultipleCheckoutMethodSelectionPage) && isCheckoutAsFolderSelected()) {
			return false;
		}
		return super.canFinish();
	}

	protected class ProjectAdditionListener implements IResourceChangeListener {
		protected IProject project = null;

		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			IResourceDelta[] deltas = event.getDelta().getAffectedChildren();
			for (IResourceDelta element : deltas) {
				IResource resource = element.getResource();
				if (resource instanceof IProject) {
					if (element.getKind() == IResourceDelta.ADDED) {
						project = (IProject) resource;
					} else if (element.getKind() == IResourceDelta.REMOVED && project == resource) {
						// wizard will be cancelled ?
						project = null;
					}
				}
			}
		}

		public IProject getProject() {
			return project;
		}

	}

}
