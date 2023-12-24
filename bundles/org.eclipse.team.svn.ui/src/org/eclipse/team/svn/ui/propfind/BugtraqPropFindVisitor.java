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

package org.eclipse.team.svn.ui.propfind;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.ui.properties.bugtraq.BugtraqModel;

/**
 * Visitor class, implementing the Bugtraq property search. Constructs proper Bugtraq model.
 * 
 * @author Alexei Goncharov
 */
public class BugtraqPropFindVisitor implements IPropFindVisitor {
	protected BugtraqModel model;

	protected Set<String> bugtraqProperties;

	public BugtraqPropFindVisitor() {
		model = new BugtraqModel();
		bugtraqProperties = new HashSet<>(
				Arrays.asList("bugtraq:url", "bugtraq:logregex", "bugtraq:label", "bugtraq:message", "bugtraq:number", //$NON-NLS-1$
						"bugtraq:warnifnoissue", "bugtraq:append"));
	}

	@Override
	public boolean visit(SVNProperty propertyParam) {
		if (bugtraqProperties.contains(propertyParam.name)) {
			processBugtraqProperty(propertyParam.name, propertyParam.value);
			bugtraqProperties.remove(propertyParam.name);
		}
		return true;
	}

	protected void processBugtraqProperty(String name, String value) {
		if (name.equals("bugtraq:url")) { //$NON-NLS-1$
			model.setUrl(value);
		} else if (name.equals("bugtraq:logregex")) { //$NON-NLS-1$
			model.setLogregex(value);
		} else if (name.equals("bugtraq:label")) { //$NON-NLS-1$
			model.setLabel(value);
		} else if (name.equals("bugtraq:message")) { //$NON-NLS-1$
			model.setMessage(value);
		} else if (name.equals("bugtraq:number")) { //$NON-NLS-1$
			boolean number = value == null || !(value.trim().equals("false") || value.trim().equals("no")); //$NON-NLS-1$ //$NON-NLS-2$
			model.setNumber(number);
		} else if (name.equals("bugtraq:warnifnoissue")) { //$NON-NLS-1$
			boolean warn = value != null && (value.trim().equals("yes") || value.trim().equals("true")); //$NON-NLS-1$ //$NON-NLS-2$
			model.setWarnIfNoIssue(warn);
		} else if (name.equals("bugtraq:append")) { //$NON-NLS-1$
			boolean append = value == null || !(value.trim().equals("false") || value.trim().equals("no")); //$NON-NLS-1$ //$NON-NLS-2$
			model.setAppend(append);
		}
	}

	public BugtraqModel getBugtraqModel() {
		return model;
	}

}
