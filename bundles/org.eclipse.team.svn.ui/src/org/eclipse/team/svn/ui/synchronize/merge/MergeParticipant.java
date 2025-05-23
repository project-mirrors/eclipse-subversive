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

package org.eclipse.team.svn.ui.synchronize.merge;

import java.util.Collection;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSubscriber;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.svn.core.synchronize.MergeSubscriber;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.extension.ExtensionsManager;
import org.eclipse.team.svn.ui.operation.MergeScope;
import org.eclipse.team.svn.ui.synchronize.AbstractSVNParticipant;
import org.eclipse.team.svn.ui.synchronize.AbstractSynchronizeActionGroup;
import org.eclipse.team.svn.ui.synchronize.SynchronizeLabelDecorator;
import org.eclipse.team.svn.ui.utility.OverlayedImageDescriptor;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizeScope;

/**
 * Merge view participant
 * 
 * @author Alexander Gurov
 */
public class MergeParticipant extends AbstractSVNParticipant {
	public static final String PARTICIPANT_ID = MergeParticipant.class.getName();

	public static final int SUPPORTED_MODES = ISynchronizePageConfiguration.ALL_MODES;

	protected IPropertyChangeListener configurationListener;

	public MergeParticipant() {
	}

	public MergeParticipant(ISynchronizeScope scope) {
		super(scope);
	}

	@Override
	public AbstractSVNSubscriber getMatchingSubscriber() {
		MergeSubscriber subscriber = MergeSubscriber.instance();
		MergeScope scope = (MergeScope) getScope();

		subscriber.setMergeScopeHelper(scope.getMergeScopeHelper());
		return subscriber;
	}

	@Override
	protected void initializeConfiguration(ISynchronizePageConfiguration configuration) {
		super.initializeConfiguration(configuration);
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	@Override
	protected String getParticipantId() {
		return MergeParticipant.PARTICIPANT_ID;
	}

	@Override
	protected Collection<AbstractSynchronizeActionGroup> getActionGroups() {
		return ExtensionsManager.getInstance().getCurrentSynchronizeActionContributor().getMergeContributions();
	}

	@Override
	protected int getSupportedModes() {
		return MergeParticipant.SUPPORTED_MODES;
	}

	@Override
	protected int getDefaultMode() {
		return ISynchronizePageConfiguration.BOTH_MODE;
	}

	@Override
	protected String getShortTaskName() {
		return SVNUIMessages.MergeView_TaskName;
	}

	@Override
	protected ILabelDecorator createLabelDecorator(ISynchronizePageConfiguration configuration) {
		return new MergeLabelDecorator(configuration);
	}

	protected class MergeLabelDecorator extends SynchronizeLabelDecorator {
		public MergeLabelDecorator(ISynchronizePageConfiguration configuration) {
			super(configuration);
		}

		@Override
		public Image decorateImage(Image image, Object element) {
			AbstractSVNSyncInfo info = getSyncInfo(element);
			if (info != null && (info.getKind()
					& SynchronizeLabelDecorator.CONFLICTING_REPLACEMENT_MASK) == SynchronizeLabelDecorator.CONFLICTING_REPLACEMENT_MASK) {
				ILocalResource local = info.getLocalResource();
				if (IStateFilter.SF_PREREPLACEDREPLACED.accept(local)) {
					return registerImageDescriptor(new OverlayedImageDescriptor(image,
							AbstractSVNParticipant.OVR_REPLACED_CONF, new Point(22, 16),
							OverlayedImageDescriptor.RIGHT | OverlayedImageDescriptor.CENTER_V));
				}
			}
			return super.decorateImage(image, element);
		}

	}

}
