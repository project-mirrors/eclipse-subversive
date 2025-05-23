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

import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Wizard implementation that allows us to hide progress monitor part if it is not needed by wizard it self.
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractSVNWizard extends Wizard {

	public AbstractSVNWizard() {
	}

	@Override
	public void createPageControls(Composite pageContainer) {
		if (!needsProgressMonitor()) {
			ProgressMonitorPart part = findProgressMonitorPart(pageContainer);
			if (part != null) {
				GridData data = new GridData();
				data.heightHint = 0;
				part.setLayoutData(data);
			}
		}
		super.createPageControls(pageContainer);
	}

	protected ProgressMonitorPart findProgressMonitorPart(Composite container) {
		if (container == null) {
			return null;
		}
		Control[] children = container.getChildren();
		for (Control child : children) {
			if (child instanceof ProgressMonitorPart) {
				return (ProgressMonitorPart) child;
			}
		}
		return findProgressMonitorPart(container.getParent());
	}

}
