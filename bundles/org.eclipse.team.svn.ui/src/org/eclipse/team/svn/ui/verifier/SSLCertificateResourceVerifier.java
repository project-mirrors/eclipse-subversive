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
 *    Markus Oberlassnig (ilogs information logistics GmbH) - MSCAPI support via SVNKit 
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.verifier;

import org.eclipse.swt.widgets.Control;

/**
 * Existing file verifier
 * 
 * @author Sergiy Logvin
 */
public class SSLCertificateResourceVerifier extends ExistingResourceVerifier {
        
    public SSLCertificateResourceVerifier(String fieldName) {
        super(fieldName);
    }
    
    public SSLCertificateResourceVerifier(String fieldName, boolean files) {
        super(fieldName);
    }
    
    protected String getErrorMessageImpl(Control input) {
    	String fileName = this.getText(input);
    	if (fileName != null && ("MSCAPI".equals(fileName) || fileName.startsWith("MSCAPI;"))) {
    		// it's ok - use Microsoft Crypto API
    	} else {
	    	return super.getErrorMessageImpl(input);
        }
        return null;
    }
}
