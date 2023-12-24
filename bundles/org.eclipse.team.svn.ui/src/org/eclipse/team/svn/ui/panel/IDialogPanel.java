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

package org.eclipse.team.svn.ui.panel;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

/**
 * Dialog panel interface
 * 
 * @author Alexander Gurov
 */
public interface IDialogPanel {
    public void initPanel(IDialogManager manager);
    public void dispose();
    public void addListeners();
    public void postInit();
    
    public String getDialogTitle();
    public String getDialogDescription();
    public String getDefaultMessage();
    public String getImagePath();
    public Point getPrefferedSize();
    
    public void createControls(Composite parent);
    
    public String []getButtonNames();
    
    public void buttonPressed(int idx);
    public boolean canClose();
    
    public String getHelpId();
    
}
