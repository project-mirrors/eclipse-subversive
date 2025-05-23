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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Abstract field verifier implementation
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractVerifier {
	protected List<IVerifierListener> listeners;

	protected boolean filledRight;

	protected boolean hasWarning;

	public AbstractVerifier() {
		listeners = new ArrayList<>();
		filledRight = false;
		hasWarning = false;
	}

	public synchronized void addVerifierListener(IVerifierListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	public void removeVerifierListener(IVerifierListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	public boolean isFilledRight() {
		return filledRight;
	}

	public boolean hasWarning() {
		return hasWarning;
	}

	public boolean verify(Control input) {
		String msg = getErrorMessage(input);
		if (msg != null) {
			fireError(msg);
			return false;
		}
		msg = getWarningMessage(input);
		if (msg != null) {
			fireWarning(msg);
		} else {
			fireOk();
		}
		return true;
	}

	protected abstract String getErrorMessage(Control input);

	protected abstract String getWarningMessage(Control input);

	protected String getText(Control input) {
		if (input instanceof Text) {
			return ((Text) input).getText();
		} else if (input instanceof StyledText) {
			return ((StyledText) input).getText();
		} else if (input instanceof Combo) {
			return ((Combo) input).getText();
		}
		String message = BaseMessages.format(SVNUIMessages.Verifier_Abstract,
				new String[] { this.getClass().getName() });
		throw new RuntimeException(message);
	}

	protected void fireError(String errorReason) {
		filledRight = false;
		hasWarning = false;

		Object[] listeners = null;
		synchronized (this.listeners) {
			listeners = this.listeners.toArray();
		}
		for (int i = listeners.length - 1; i >= 0; i--) {
			((IVerifierListener) listeners[i]).hasError(errorReason);
		}
	}

	protected void fireWarning(String warningReason) {
		filledRight = true;
		hasWarning = true;
		Object[] listeners = null;
		synchronized (this.listeners) {
			listeners = this.listeners.toArray();
		}
		for (int i = listeners.length - 1; i >= 0; i--) {
			((IVerifierListener) listeners[i]).hasWarning(warningReason);
		}
	}

	protected void fireOk() {
		filledRight = true;
		hasWarning = false;
		Object[] listeners = null;
		synchronized (this.listeners) {
			listeners = this.listeners.toArray();
		}
		for (int i = listeners.length - 1; i >= 0; i--) {
			((IVerifierListener) listeners[i]).hasNoError();
		}
	}

}
