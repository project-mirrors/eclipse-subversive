/*******************************************************************************
 * Copyright (c) 2005-2006 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.core.client;

/**
 * Replacement for org.tigris.subversion.javahl.NodeKind
 * 
 * @author Alexander Gurov
 */
public class NodeKind {
    /** absent */
    public static final int none = 0;

    /** regular file */
    public static final int file = 1;

    /** directory */
    public static final int dir = 2;

    /** something's here, but we don't know what */
    public static final int unknown = 3;

    /**
     * mapping for the constants to text
     */
	public static final String[] NAMES = {
		"none",
		"file",
		"dir",
		"unknown",
	};
	
}
