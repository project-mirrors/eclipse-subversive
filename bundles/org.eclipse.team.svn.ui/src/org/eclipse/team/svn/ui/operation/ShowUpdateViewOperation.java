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

package org.eclipse.team.svn.ui.operation;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.team.svn.core.operation.local.AbstractWorkingCopyOperation;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.mapping.ModelHelper;
import org.eclipse.team.svn.ui.mapping.UpdateModelParticipant;
import org.eclipse.team.svn.ui.mapping.UpdateSubscriberContext;
import org.eclipse.team.svn.ui.synchronize.action.CommitActionHelper;
import org.eclipse.team.svn.ui.synchronize.update.UpdateParticipant;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ISynchronizeScope;
import org.eclipse.team.ui.synchronize.ResourceScope;
import org.eclipse.team.ui.synchronize.SubscriberParticipant;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Synchronize workspace resources operation
 * 
 * @author Alexander Gurov
 */
public class ShowUpdateViewOperation extends AbstractWorkingCopyOperation {

	//note that it can be null
	protected IWorkbenchPart part;

	protected ISynchronizeScope scope;

	protected ResourceMapping[] resourcesMapping;

	public ShowUpdateViewOperation(ISynchronizeScope scope, IWorkbenchPart part) {
		super("Operation_ShowUpdateView", SVNUIMessages.class, (IResource[]) null); //$NON-NLS-1$
		this.part = part;
		this.scope = scope;
	}

	public ShowUpdateViewOperation(IResource[] resources, IWorkbenchPart part) {
		super("Operation_ShowUpdateView", SVNUIMessages.class, resources); //$NON-NLS-1$
		this.part = part;
	}

	public ShowUpdateViewOperation(IResourceProvider provider, IWorkbenchPart part) {
		super("Operation_ShowUpdateView", SVNUIMessages.class, provider); //$NON-NLS-1$
		this.part = part;
	}

	public ShowUpdateViewOperation(ResourceMapping[] resourcesMapping, IWorkbenchPart part) {
		super("Operation_ShowUpdateView", SVNUIMessages.class, (IResource[]) null); //$NON-NLS-1$
		this.part = part;
		this.resourcesMapping = resourcesMapping;
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return null;
	}

	@Override
	public int getOperationWeight() {
		return 0;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		if (ModelHelper.isShowModelSync()) {

			if (resourcesMapping.length == 0) {
				return;
			}

			String messsage = SVNUIMessages.ConsultChangeSets_message1;
			boolean consultChangeSets = CommitActionHelper.isIncludeChangeSets(messsage);
			SubscriberScopeManager manager = UpdateSubscriberContext.createWorkspaceScopeManager(resourcesMapping, true,
					consultChangeSets);
			UpdateSubscriberContext context = UpdateSubscriberContext.createContext(manager,
					ISynchronizationContext.THREE_WAY);
			UpdateModelParticipant participant = new UpdateModelParticipant(context);
			TeamUI.getSynchronizeManager().addSynchronizeParticipants(new ISynchronizeParticipant[] { participant });
			participant.run(part);
		} else {
			IResource[] resources;
			if (scope == null) {
				resources = operableData();
				scope = new ResourceScope(resources);
			} else {
				resources = scope.getRoots();
			}

			UpdateParticipant participant = null;
			if (resources != null) {
				participant = (UpdateParticipant) SubscriberParticipant
						.getMatchingParticipant(UpdateParticipant.PARTICIPANT_ID, resources);
			}
			if (participant == null) {
				participant = new UpdateParticipant(scope);
				TeamUI.getSynchronizeManager()
						.addSynchronizeParticipants(new ISynchronizeParticipant[] { participant });
			}
			participant.run(part);
		}
	}

}
