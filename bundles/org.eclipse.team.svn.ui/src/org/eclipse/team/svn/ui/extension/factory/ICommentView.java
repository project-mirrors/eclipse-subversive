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

import org.eclipse.core.resources.IResource;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.svn.core.resource.IRepositoryResource;

/**
 * This interface will provide PolClipse
 * 
 * @author Alexander Gurov
 */
public interface ICommentView {

	/**
	 * Current implementation of History comment message view
	 * 
	 * @param parent
	 * @return
	 */
	void createCommentView(Composite parent);

	void createCommentView(Composite parent, int style);

	void usedFor(IResource resource);

	void usedFor(IRepositoryResource resource);

	void setComment(String comment);

}
