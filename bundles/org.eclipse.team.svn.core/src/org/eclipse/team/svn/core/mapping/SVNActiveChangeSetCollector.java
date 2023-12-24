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
 *    Alexei Goncharov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.mapping;

import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSet;
import org.eclipse.team.internal.core.subscribers.SubscriberChangeSetManager;

/**
 * SVN active change set manager.
 * @author Alexei Goncharov
 */
public class SVNActiveChangeSetCollector extends SubscriberChangeSetManager {

	public SVNActiveChangeSetCollector(Subscriber subscriber) {
		super(subscriber);
	}

	protected ActiveChangeSet doCreateSet(String name) {
		return new SVNActiveChangeSet(this, name);
	}
	
}
