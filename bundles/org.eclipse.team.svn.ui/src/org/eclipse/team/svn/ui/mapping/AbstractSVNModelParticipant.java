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
 *    Igor Burilo - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.mapping;

import java.util.Collection;

import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;
import org.eclipse.team.internal.ui.synchronize.IChangeSetProvider;
import org.eclipse.team.svn.core.mapping.SVNChangeSetModelProvider;
import org.eclipse.team.svn.ui.synchronize.AbstractSynchronizeActionGroup;
import org.eclipse.team.svn.ui.synchronize.SynchronizeLabelDecorator;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;

/**
 * @author Igor Burilo
 *
 */
public abstract class AbstractSVNModelParticipant extends ModelSynchronizeParticipant {

	protected ISynchronizePageConfiguration configuration;

	public AbstractSVNModelParticipant() {
	}

	public AbstractSVNModelParticipant(SynchronizationContext context) {
		super(context);
	}

	@Override
	protected void initializeConfiguration(ISynchronizePageConfiguration configuration) {
		super.initializeConfiguration(configuration);

		this.configuration = configuration;

		Collection<AbstractSynchronizeActionGroup> actionGroups = getActionGroups();
		// menu groups should be configured before actionGroups is added
		for (AbstractSynchronizeActionGroup actionGroup : actionGroups) {
			actionGroup.configureMenuGroups(configuration);
		}
		for (AbstractSynchronizeActionGroup actionGroup : actionGroups) {
			configuration.addActionContribution(actionGroup);
		}

		configuration.addLabelDecorator(createLabelDecorator(configuration));

		configuration.setSupportedModes(getSupportedModes());
		configuration.setMode(getDefaultMode());
	}

	protected ILabelDecorator createLabelDecorator(ISynchronizePageConfiguration configuration) {
		return new SynchronizeLabelDecorator(configuration);
	}

	protected abstract int getSupportedModes();

	protected abstract int getDefaultMode();

	protected abstract Collection<AbstractSynchronizeActionGroup> getActionGroups();

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant#getEnabledModelProviders()
	 * 
	 * see CVSModelSynchronizeParticipant
	 * 
	 */
	@Override
	public ModelProvider[] getEnabledModelProviders() {
		ModelProvider[] enabledProviders = super.getEnabledModelProviders();
		if (this instanceof IChangeSetProvider) {
			for (ModelProvider provider : enabledProviders) {
				if (provider.getId().equals(SVNChangeSetModelProvider.ID)) {
					return enabledProviders;
				}
			}
			ModelProvider[] extended = new ModelProvider[enabledProviders.length + 1];
			for (int i = 0; i < enabledProviders.length; i++) {
				extended[i] = enabledProviders[i];
			}
			SVNChangeSetModelProvider provider = SVNChangeSetModelProvider.getProvider();
			if (provider == null) {
				return enabledProviders;
			}
			extended[extended.length - 1] = provider;
			return extended;
		}
		return enabledProviders;
	}
}
