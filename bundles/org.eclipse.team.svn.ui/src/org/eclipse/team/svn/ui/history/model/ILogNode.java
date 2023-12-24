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
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.history.model;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * History view UI node interface
 * 
 * @author Alexander Gurov
 */
public interface ILogNode extends IAdaptable {
	int TYPE_NONE = 0;

	int TYPE_CATEGORY = 1;

	int TYPE_SVN = 2;

	int TYPE_LOCAL = 3;

	int LABEL_TRIM = 0;

	int LABEL_FLAT = 1;

	int LABEL_FULL = 2;

	int COLUMN_REVISION = 0;

	int COLUMN_DATE = 1;

	int COLUMN_CHANGES = 2;

	int COLUMN_AUTHOR = 3;

	int COLUMN_COMMENT = 4;

	int NUM_OF_COLUMNS = 5;

	int getType();

	ILogNode[] getChildren();

	boolean hasChildren();

	boolean requiresBoldFont(long currentRevision);

	ImageDescriptor getImageDescriptor();

	String getLabel(int columnIndex, int labelType, long currentRevision);

	Object getEntity();

	long getRevision();

	long getTimeStamp();

	String getComment();

	String getAuthor();

	int getChangesCount();

	ILogNode getParent();

}