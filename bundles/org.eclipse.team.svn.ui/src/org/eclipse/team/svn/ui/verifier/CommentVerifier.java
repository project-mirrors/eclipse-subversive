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
 *    Sergiy Logvin - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.verifier;

import org.eclipse.swt.widgets.Control;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Comment verifier
 * 
 * @author Sergiy Logvin
 */
public class CommentVerifier extends AbstractFormattedVerifier {
    protected int logMinSize;
        
    public CommentVerifier(String fieldName, int logMinSize) {
        super(fieldName);
        this.logMinSize = logMinSize;
    }
    
    protected String getErrorMessageImpl(Control input) {
    	if (this.getText(input).trim().length() < this.logMinSize) {
    		return BaseMessages.format(SVNUIMessages.Verifier_Comment_Error, this.logMinSize);
    	}
        return null;
    }

    protected String getWarningMessageImpl(Control input) {
    	if (this.getText(input).trim().length() == 0) {
    		return SVNUIMessages.Verifier_Comment_Warning;
    	}
        return null;
    }

}
