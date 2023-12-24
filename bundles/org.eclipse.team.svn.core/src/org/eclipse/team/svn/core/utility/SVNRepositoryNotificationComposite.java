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

package org.eclipse.team.svn.core.utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.team.svn.core.connector.ISVNRepositoryNotificationCallback;
import org.eclipse.team.svn.core.connector.SVNRepositoryNotification;

/**
 * SVN repository notification listener composite
 * 
 * @author Alexander Gurov
 */
public class SVNRepositoryNotificationComposite implements ISVNRepositoryNotificationCallback {
	protected ISVNRepositoryNotificationCallback[] listeners;

	public SVNRepositoryNotificationComposite() {
		listeners = new ISVNRepositoryNotificationCallback[0];
	}

	public void add(ISVNRepositoryNotificationCallback listener) {
		List<ISVNRepositoryNotificationCallback> tmp = new ArrayList<>(
				Arrays.asList(listeners));
		if (!tmp.contains(listener)) {
			tmp.add(listener);
		}
		listeners = tmp.toArray(new ISVNRepositoryNotificationCallback[tmp.size()]);
	}

	public void remove(ISVNRepositoryNotificationCallback listener) {
		List<ISVNRepositoryNotificationCallback> tmp = new ArrayList<>(
				Arrays.asList(listeners));
		tmp.remove(listener);
		listeners = tmp.toArray(new ISVNRepositoryNotificationCallback[tmp.size()]);
	}

	@Override
	public void notify(SVNRepositoryNotification info) {
		// thread safe...
		ISVNRepositoryNotificationCallback[] tmp = listeners;
		for (ISVNRepositoryNotificationCallback element : tmp) {
			element.notify(info);
		}
	}

}
