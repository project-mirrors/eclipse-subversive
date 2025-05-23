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

package org.eclipse.team.svn.ui.verifier;

import org.eclipse.swt.widgets.Control;

/**
 * Validation manager interface allows us to provide validation API to any component at our choice
 * 
 * @author Alexander Gurov
 */
public interface IValidationManager {

	void attachTo(Control cmp, AbstractVerifier verifier);

	void detachFrom(Control cmp);

	void detachAll();

	boolean isFilledRight();

	void validateContent();

	boolean validateControl(Control cmp);

}
