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

package org.eclipse.team.svn.ui.synchronize.update;

import java.util.Collection;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSubscriber;
import org.eclipse.team.svn.core.synchronize.UpdateSubscriber;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.extension.ExtensionsManager;
import org.eclipse.team.svn.ui.synchronize.AbstractSVNParticipant;
import org.eclipse.team.svn.ui.synchronize.AbstractSynchronizeActionGroup;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizeScope;
import org.eclipse.team.ui.synchronize.ResourceScope;

/**
 * Synchronize view participant
 * 
 * @author Alexander Gurov
 */
public class UpdateParticipant extends AbstractSVNParticipant {
	public static final String PARTICIPANT_ID = "org.eclipse.team.svn.ui.synchronize.update.SynchronizeParticipant";

	public UpdateParticipant() {
	}

	public UpdateParticipant(ISynchronizeScope scope) {
		super(scope);
	}

	@Override
	public AbstractSVNSubscriber getMatchingSubscriber() {
		return UpdateSubscriber.instance();
	}

	@Override
	public String getName() {
		String name = SVNUIMessages.SynchronizeParticipant;
		ISynchronizeScope scope = getScope();
		String scopeName = scope.getName();
		if (scope instanceof ResourceScope) {
			StringBuilder buffer = new StringBuilder();
			IResource[] resources = scope.getRoots();
			for (int i = 0; i < resources.length; i++) {
				if (i > 0) {
					buffer.append(", "); //$NON-NLS-1$
				}
				buffer.append(resources[i].getFullPath().toString().substring(1));
			}
			scopeName = buffer.toString();
		}
		return BaseMessages.format(TeamUIMessages.SubscriberParticipant_namePattern, new String[] { name, scopeName });
	}

	@Override
	protected String getParticipantId() {
		return UpdateParticipant.PARTICIPANT_ID;
	}

	@Override
	protected Collection<AbstractSynchronizeActionGroup> getActionGroups() {
		return ExtensionsManager.getInstance().getCurrentSynchronizeActionContributor().getUpdateContributions();
	}

	@Override
	protected int getSupportedModes() {
		return ISynchronizePageConfiguration.ALL_MODES;
	}

	@Override
	protected int getDefaultMode() {
		return ISynchronizePageConfiguration.BOTH_MODE;
	}

}
