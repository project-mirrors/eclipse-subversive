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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.swt.widgets.Control;

/**
 * Group verifier allows us to verify dedicated group of controls placed on the panel
 * 
 * @author Alexander Gurov
 */
public class GroupVerifier extends CompositeVerifier {
	protected Map<AbstractVerifier, Control> componentsMap;

	public GroupVerifier() {
		componentsMap = new LinkedHashMap<>();
	}

	public boolean verify() {
		hasWarning = false;
		for (Iterator<?> it = verifiers.iterator(); it.hasNext();) {
			AbstractVerifier iVer = (AbstractVerifier) it.next();
			if (!iVer.verify(componentsMap.get(iVer))) {
				return false;
			}
		}
		if (!hasWarning) {
			fireOk();
		}
		return true;
	}

	@Override
	public boolean verify(Control input) {
		// could be used as workaround for situations when control validation is required in any case
		for (Map.Entry<AbstractVerifier, Control> entry : componentsMap.entrySet()) {
			if (entry.getValue().equals(input)) {
				return entry.getKey().verify(input);
			}
		}
		return true;
	}

	public void add(Control cmp, AbstractVerifier verifier) {
		super.add(verifier);
		componentsMap.put(verifier, cmp);
	}

	public void remove(Control cmp) {
		for (Iterator<Map.Entry<AbstractVerifier, Control>> it = componentsMap.entrySet().iterator(); it.hasNext();) {
			Map.Entry<AbstractVerifier, Control> entry = it.next();
			if (cmp == entry.getValue()) {
				AbstractVerifier verifier = entry.getKey();
				super.remove(verifier);
				it.remove();
				break;
			}
		}
	}

	@Override
	public void removeAll() {
		super.removeAll();
		componentsMap.clear();
	}

	public Iterator<Control> getComponents() {
		return componentsMap.values().iterator();
	}

}
