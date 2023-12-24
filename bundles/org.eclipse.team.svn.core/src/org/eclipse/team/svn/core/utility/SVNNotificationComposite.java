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

import org.eclipse.team.svn.core.connector.ISVNNotificationCallback;
import org.eclipse.team.svn.core.connector.SVNNotification;

/**
 * SVN notification listener composite
 * 
 * @author Alexander Gurov
 */
public class SVNNotificationComposite implements ISVNNotificationCallback {
	protected ISVNNotificationCallback[] listeners;

	public SVNNotificationComposite() {
		listeners = new ISVNNotificationCallback[0];
	}

	public void add(ISVNNotificationCallback listener) {
		List<ISVNNotificationCallback> tmp = new ArrayList<>(Arrays.asList(listeners));
		if (!tmp.contains(listener)) {
			tmp.add(listener);
		}
		listeners = tmp.toArray(new ISVNNotificationCallback[tmp.size()]);
	}

	public void remove(ISVNNotificationCallback listener) {
		List<ISVNNotificationCallback> tmp = new ArrayList<>(Arrays.asList(listeners));
		tmp.remove(listener);
		listeners = tmp.toArray(new ISVNNotificationCallback[tmp.size()]);
	}

	@Override
	public void notify(SVNNotification info) {
		// thread safe...
		ISVNNotificationCallback[] tmp = listeners;
		for (ISVNNotificationCallback element : tmp) {
			element.notify(info);
		}
	}

}
