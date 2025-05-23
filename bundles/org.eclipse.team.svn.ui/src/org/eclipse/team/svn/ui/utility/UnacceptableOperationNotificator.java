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

package org.eclipse.team.svn.ui.utility;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.local.ResourceListPanel;

/**
 * Unacceptable Operation class verifier implementation
 * 
 * @author Sergiy Logvin
 */
public class UnacceptableOperationNotificator {
	public static IResource[] shrinkResourcesWithNotOnRespositoryParents(final Shell shell, IResource[] resources) {
		HashSet resultResources = new HashSet();
		final Map unsupportedResources = new HashMap();
		if (resources == null) {
			return null;
		}
		for (IResource element : resources) {
			IResource[] parents = FileUtility.getOperableParents(new IResource[] { element },
					IStateFilter.SF_NOTONREPOSITORY, true);

			ILocalResource local = SVNRemoteStorage.instance().asLocalResourceAccessible(element);
			if (parents.length > 0 && IStateFilter.SF_ONREPOSITORY.accept(local)) {
				unsupportedResources.put(element, parents);
			} else {
				resultResources.add(element);
			}
		}
		//delete from unsupported set resources which parents already exist in resultResources
		if (!resultResources.isEmpty() && !unsupportedResources.isEmpty()) {
			for (Iterator iter = unsupportedResources.keySet().iterator(); iter.hasNext();) {
				IResource res = (IResource) iter.next();
				List listOfParents = Arrays.asList((IResource[]) unsupportedResources.get(res));
				if (resultResources.containsAll(listOfParents)) {
					iter.remove();
				}
			}
		}
		final boolean[] isCanceled = { false };
		if (!unsupportedResources.isEmpty()) {
			final HashSet parents = new HashSet();
			for (Iterator iter = unsupportedResources.keySet().iterator(); iter.hasNext();) {
				IResource res = (IResource) iter.next();
				parents.addAll(Arrays.asList((IResource[]) unsupportedResources.get(res)));
			}
			shell.getDisplay().syncExec(() -> {
				boolean oneParent = parents.size() == 1;
				boolean oneResource = unsupportedResources.size() == 1;
				String description;
				String defaultMessage;
				if (oneParent && oneResource) {
					description = SVNUIMessages.UnacceptableOperation_Description_1;
					defaultMessage = SVNUIMessages.UnacceptableOperation_Message_1;
				} else if (!oneParent && oneResource) {
					description = SVNUIMessages.UnacceptableOperation_Description_2;
					defaultMessage = SVNUIMessages.UnacceptableOperation_Message_2;
				} else if (oneParent && !oneResource) {
					description = SVNUIMessages.UnacceptableOperation_Description_3;
					defaultMessage = SVNUIMessages.UnacceptableOperation_Message_3;
				} else {
					description = SVNUIMessages.UnacceptableOperation_Description_4;
					defaultMessage = SVNUIMessages.UnacceptableOperation_Message_4;
				}
				ResourceListPanel panel = new ResourceListPanel(
						(IResource[]) unsupportedResources.keySet()
								.toArray(new IResource[unsupportedResources.size()]),
						SVNUIMessages.UnacceptableOperation_Title, description, defaultMessage,
						new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL });
				DefaultDialog dialog = new DefaultDialog(shell, panel);
				if (dialog.open() != 0) {
					isCanceled[0] = true;
				}
			});
			if (isCanceled[0]) {
				return null;
			}
			resultResources.addAll(parents);
		}
		return (IResource[]) resultResources.toArray(new IResource[resultResources.size()]);
	}

}
