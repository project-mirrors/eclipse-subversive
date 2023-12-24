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

package org.eclipse.team.svn.ui.extension.factory;

/**
 * Allows modification of the commit panel parameters
 * 
 * @author Alexander Gurov
 * 
 * @deprecated use {@link ICommentManager} instead.
 */
public interface IModifiableCommentDialogPanel extends ICommentDialogPanel {
	/**
	 * Changes message in the commit panel. Be sure to call it from UI thread and before the actual widget is disposed or you'll get an
	 * exception.
	 */
	public void setMessage(String message);
}
