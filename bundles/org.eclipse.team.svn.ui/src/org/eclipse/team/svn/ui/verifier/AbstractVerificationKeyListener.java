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

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

/**
 * Abstract verification listener, that allows us to listen and validate all specified components (not only Text fields) in generic manner
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractVerificationKeyListener extends KeyAdapter
		implements IValidationManager, IVerifierListener {
	protected GroupVerifier verifier;

	public AbstractVerificationKeyListener() {
		verifier = new GroupVerifier();
		verifier.addVerifierListener(this);
	}

	@Override
	public void attachTo(Control cmp, AbstractVerifier verifier) {
		this.verifier.add(cmp, verifier);
	}

	public void addListeners() {
		for (Iterator<Control> it = verifier.getComponents(); it.hasNext();) {
			Control cmp = it.next();
			if (cmp instanceof Text) {
				((Text) cmp).addModifyListener(e -> AbstractVerificationKeyListener.this.validateContent());
			}
			if (cmp instanceof StyledText) {
				((StyledText) cmp).addModifyListener(e -> AbstractVerificationKeyListener.this.validateContent());
			}
			if (cmp instanceof Combo) {
				((Combo) cmp).addModifyListener(e -> AbstractVerificationKeyListener.this.validateContent());
				((Combo) cmp).addSelectionListener(new SelectionListener() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						AbstractVerificationKeyListener.this.validateContent();
					}

					@Override
					public void widgetDefaultSelected(SelectionEvent e) {
					}
				});
			}
		}
	}

	@Override
	public void detachFrom(Control cmp) {
		verifier.remove(cmp);
		if (!cmp.isDisposed()) {
			cmp.removeKeyListener(this);
		}
	}

	@Override
	public void detachAll() {
		for (Iterator<Control> it = verifier.getComponents(); it.hasNext();) {
			Control ctrl = it.next();
			if (!ctrl.isDisposed()) {
				ctrl.removeKeyListener(this);
			}
		}
		verifier.removeAll();
	}

	@Override
	public void validateContent() {
		verifier.verify();
	}

	@Override
	public boolean validateControl(Control cmp) {
		return verifier.verify(cmp);
	}

	@Override
	public boolean isFilledRight() {
		return verifier.isFilledRight();
	}

	@Override
	public void keyReleased(KeyEvent e) {
		validateContent();
	}

}
