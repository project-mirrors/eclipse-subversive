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

package org.eclipse.team.svn.core.operation;

import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.team.ResourceRuleFactory;

/**
 * Resource scheduling rule factory implementation.
 * 
 * @author Alexander Gurov
 */
public class SVNResourceRuleFactory extends ResourceRuleFactory {
	public static final IResourceRuleFactory INSTANCE = new SVNResourceRuleFactory();
	
	// no changes to the default policy.
	
}
