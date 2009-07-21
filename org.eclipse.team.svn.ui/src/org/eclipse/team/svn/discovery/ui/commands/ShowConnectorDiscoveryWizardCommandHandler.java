/*******************************************************************************
 * Copyright (c) 2009 Tasktop Technologies, Polarion Software and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.svn.discovery.ui.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.team.svn.discovery.ui.util.DiscoveryUiUtil;
import org.eclipse.team.svn.discovery.ui.wizards.ConnectorDiscoveryWizard;

/**
 * A command that causes the {@link ConnectorDiscoveryWizard} to appear in a dialog.
 * 
 * @author David Green
 * @author Igor Burilo
 */
public class ShowConnectorDiscoveryWizardCommandHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {

		ConnectorDiscoveryWizard wizard = new ConnectorDiscoveryWizard();
		WizardDialog dialog = new WizardDialog(DiscoveryUiUtil.getShell(), wizard);
		dialog.open();

		return null;
	}
}
