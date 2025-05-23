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

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.team.internal.ui.mapping.ModelElementSelectionPage;
import org.eclipse.team.internal.ui.synchronize.GlobalRefreshElementSelectionPage;
import org.eclipse.team.internal.ui.synchronize.GlobalRefreshResourceSelectionPage;
import org.eclipse.team.svn.core.synchronize.UpdateSubscriber;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.mapping.ModelHelper;
import org.eclipse.team.svn.ui.operation.ShowUpdateViewOperation;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.ui.synchronize.ISynchronizeScope;

/**
 * Synchronize set selection wizard
 * 
 * @author Alexander Gurov
 */
public class SynchronizeWizard extends AbstractSVNWizard {

	protected GlobalRefreshElementSelectionPage selection;

	public SynchronizeWizard() {
		setWindowTitle(SVNUIMessages.SynchronizeWizard_Title);
	}

	@Override
	public void addPages() {
		if (ModelHelper.isShowModelSync()) {
			selection = new ModelElementSelectionPage(UpdateSubscriber.instance().roots());
		} else {
			selection = new GlobalRefreshResourceSelectionPage(UpdateSubscriber.instance().roots());
		}
		selection.setImageDescriptor(SVNTeamUIPlugin.instance().getImageDescriptor("icons/wizards/newconnect.gif")); //$NON-NLS-1$
		addPage(selection);
	}

	@Override
	public boolean performFinish() {
		ShowUpdateViewOperation op;
		if (ModelHelper.isShowModelSync()) {
			ResourceMapping[] mappings = ((ModelElementSelectionPage) selection).getSelectedMappings();
			op = new ShowUpdateViewOperation(mappings, null);
		} else {
			ISynchronizeScope scope = ((GlobalRefreshResourceSelectionPage) selection).getSynchronizeScope();
			op = new ShowUpdateViewOperation(scope, null);
		}
		UIMonitorUtility.doTaskBusyDefault(op);

		return true;
	}

//TODO rework this class as inherited from SubscriberParticipantWizard
//	protected IResource []getRootResources() {
//		return SynchronizeSubscriber.instance().roots();
//	}
//
//	protected SubscriberParticipant createParticipant(ISynchronizeScope scope) {
//		IResource []roots = scope.getRoots();
//		if (roots == null) {
//			roots = this.getRootResources();
//		}
//
//		SynchronizeParticipant participant =
//			(SynchronizeParticipant)SubscriberParticipant.getMatchingParticipant(SynchronizeParticipant.PARTICIPANT_ID, roots);
//
//		return participant == null ? new SynchronizeParticipant(scope) : participant;
//	}
//
//	protected String getName() {
//		ISynchronizeParticipantDescriptor desc = TeamUI.getSynchronizeManager().getParticipantDescriptor(SynchronizeParticipant.PARTICIPANT_ID);
//		return desc == null ? "Unknown" : desc.getName();
//	}
//
//	protected IWizard getImportWizard() {
//		return new CheckoutWizard();
//	}
//
}
