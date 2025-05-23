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

import java.util.HashMap;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.remote.LocateProjectsOperation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;

/**
 * Provide checkout operation extension points
 * 
 * @author Alexander Gurov
 */
public interface ICheckoutFactory {
	/**
	 * The method allows specific decorations for the projects in Checkout As wizard
	 * 
	 * @param name2resources
	 *            mapping between proposed project names and repository resources that is referenced to corresponding projects on repository
	 * @return table decorator
	 */
	ITableLabelProvider getLabelProvider(HashMap name2resources);

	/**
	 * The method provides specific filter allowing automated detection of the projects on repository
	 * 
	 * @return repository resource filter
	 */
	LocateProjectsOperation.ILocateFilter getLocateFilter();

	/**
	 * The method allows override the default Subversive project Checkout Operation behavior with specific one
	 * 
	 * @param shell
	 *            the Shell instance that will be used to interact with user
	 * @param remote
	 *            resources that will be checked out
	 * @param checkoutMap
	 *            project names mapping
	 * @param respectHierarchy
	 *            create locally folder structure that corresponds to repository projects layout
	 * @param location
	 *            destination folder
	 * @param recurseDepth
	 *            sets the recSure data
	 * @return alternative Checkout Operation instance
	 */
	IActionOperation getCheckoutOperation(Shell shell, IRepositoryResource[] remote, HashMap checkoutMap,
			boolean respectHierarchy, String location, SVNDepth recurseDepth, boolean ignoreExternals);

	/**
	 * The method allows correction of the automatically proposed project name mapping
	 * 
	 * @param name2resources
	 *            automatically proposed project name mapping
	 * @return corrected project name mapping
	 */
	HashMap prepareName2resources(HashMap name2resources);

	/**
	 * The method allows providing of some additional processing for the projects found on repository
	 * 
	 * @param op
	 *            default locate projects operation
	 * @param provider
	 *            found repository resource provider
	 * @return additional resources provider
	 */
	IRepositoryResourceProvider additionalProcessing(CompositeOperation op, IRepositoryResourceProvider provider);

	/**
	 * The method allows to enable/disable 'Find projects in the children of the selected resource' option in the 'Find/Checkout As' wizard.
	 * 
	 * @return true if the button should be enabled and false otherwise
	 */
	boolean findProjectsOptionEnabled();
}
