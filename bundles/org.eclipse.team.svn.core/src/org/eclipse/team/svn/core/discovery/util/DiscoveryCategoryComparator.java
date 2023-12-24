/*******************************************************************************
 * Copyright (c) 2009, 2023 Tasktop Technologies, Polarion Software and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Tasktop Technologies - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/
package org.eclipse.team.svn.core.discovery.util;

import java.util.Comparator;

import org.eclipse.team.svn.core.discovery.model.DiscoveryCategory;

/**
 * A comparator that orders categories by relevance and name.
 * 
 * @author David Green
 * @author Igor Burilo
 */
public class DiscoveryCategoryComparator implements Comparator<DiscoveryCategory> {

	public int compare(DiscoveryCategory o1, DiscoveryCategory o2) {
		if (o1 == o2) {
			return 0;
		}
		String r1 = o1.getRelevance();
		String r2 = o2.getRelevance();
		int i;
		if (r1 != null && r2 != null) {
			// don't have to worry about format, since they were already validated
			// note that higher relevance appears first, thus the reverse order of
			// the comparison.
			i = new Integer(r2).compareTo(new Integer(r1));
		} else if (r1 == null) {
			return 1;
		} else {
			return -1;
		}
		if (i == 0) {
			i = o1.getName().compareToIgnoreCase(o2.getName());
			if (i == 0) {
				i = o1.getId().compareTo(o2.getId());
			}
		}
		return i;
	}

}
