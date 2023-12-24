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

package org.eclipse.team.svn.ui.dialog;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.panel.IDialogManager;
import org.eclipse.team.svn.ui.panel.IDialogPanel;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.ui.PlatformUI;

/**
 * Default dialog implementation
 * 
 * @author Alexander Gurov
 */
public class DefaultDialog extends MessageDialog implements IDialogManager {
	public static final int DIALOG_FAILED = -1;

	public static final int BUTTON_WIDTH = 76;

	protected Listener keyListener;

	protected IDialogPanel panel;

	protected Control infoPanel;

	protected Font mainLabelFont;

	protected Label message;

	protected Label icon;

	protected Image infoImage;

	protected Image levelOkImage;

	protected Image levelWarningImage;

	protected Image levelErrorImage;

	protected Composite mainComposite;

	public DefaultDialog(Shell parentShell, IDialogPanel panel) {
		super(parentShell, panel.getDialogTitle(), null, null, MessageDialog.NONE, panel.getButtonNames(), 0);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.panel = panel;
		levelOkImage = findImage("icons/common/level_ok.gif"); //$NON-NLS-1$
		levelWarningImage = findImage("icons/common/level_warning.gif"); //$NON-NLS-1$
		levelErrorImage = findImage("icons/common/level_error.gif"); //$NON-NLS-1$
	}

	public static int convertHeightInCharsToPixels(Control control, int chars) {
		GC gc = new GC(control);
		gc.setFont(JFaceResources.getDialogFont());
		FontMetrics fontMetrics = gc.getFontMetrics();
		gc.dispose();
		return fontMetrics.getHeight() * chars;
	}

	public static int computeButtonWidth(Button button) {
		int width = button.computeSize(SWT.DEFAULT, SWT.DEFAULT).x + 6;
		return Math.max(width, DefaultDialog.BUTTON_WIDTH);
	}

	@Override
	public void setButtonEnabled(int idx, boolean enabled) {
		getButton(idx).setEnabled(enabled);
	}

	@Override
	public boolean isButtonEnabled(int idx) {
		return getButton(idx).getEnabled();
	}

	@Override
	public void setMessage(int level, String message) {
		Image img = levelOkImage;
		switch (level) {
			case IDialogManager.LEVEL_ERROR: {
				img = levelErrorImage;
				break;
			}
			case IDialogManager.LEVEL_WARNING: {
				img = levelWarningImage;
				break;
			}
		}
		if (message == null) {
			message = panel.getDefaultMessage();
		}
		this.message.setText(message == null ? "" : message); //$NON-NLS-1$
		icon.setImage(img);
	}

	@Override
	public int open() {
		try {
			setReturnCode(DefaultDialog.DIALOG_FAILED);
			return super.open();
		} finally {
			dispose();
		}
	}

	@Override
	public void forceClose(int buttonID) {
		if (isButtonEnabled(buttonID)) {
			buttonPressed(buttonID);
		}
	}

	protected void dispose() {
		if (panel != null) {
			panel.dispose();
		}

		if (mainLabelFont != null) {
			mainLabelFont.dispose();
		}
		if (infoImage != null) {
			infoImage.dispose();
		}
		if (levelOkImage != null) {
			levelOkImage.dispose();
		}
		if (levelWarningImage != null) {
			levelWarningImage.dispose();
		}
		if (levelErrorImage != null) {
			levelErrorImage.dispose();
		}
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId != IDialogConstants.CANCEL_ID) {
			panel.buttonPressed(buttonId);
		}
		super.buttonPressed(buttonId);
	}

	@Override
	protected Control createContents(Composite parent) {
		GridLayout layout = null;
		GridData data = null;

		mainComposite = new Composite(parent, SWT.NONE);

		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		mainComposite.setLayout(layout);
		data = new GridData(GridData.FILL_BOTH);
		mainComposite.setLayoutData(data);
		Dialog.applyDialogFont(mainComposite);
		initializeDialogUnits(mainComposite);

		infoPanel = createInfoPanel(mainComposite);
		dialogArea = createMainPanel(mainComposite);
		createBottomPanel(mainComposite);

		//computing the best size for the dialog
		Point defaultSize = dialogArea.getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		data = (GridData) dialogArea.getLayoutData();
		int defaultHeightHint = data.heightHint;
		data.heightHint = panel.getPrefferedSize().y;
		dialogArea.setLayoutData(data);
		Point prefferedSize = dialogArea.getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		if (prefferedSize.y < defaultSize.y) {
			data.heightHint = defaultHeightHint;
			dialogArea.setLayoutData(data);
		}

		panel.initPanel(this);
		panel.addListeners();
		panel.postInit();

		String hId = panel.getHelpId();
		if (hId != null) {
			PlatformUI.getWorkbench().getHelpSystem().setHelp(mainComposite, hId);
		}

		return mainComposite;
	}

	@Override
	public void create() {
		super.create();
		getShell().getDisplay().addFilter(SWT.KeyDown, keyListener = event -> {
			if (event.stateMask == SWT.CTRL && event.keyCode == SWT.CR) {
				DefaultDialog.this.forceClose(IDialogConstants.OK_ID);
				event.doit = false;
			}
		});
	}

	@Override
	public boolean close() {
		if (keyListener != null) {
			getShell().getDisplay().removeFilter(SWT.KeyDown, keyListener);
		}
		// ESC pressed? (ESC handling is hardcoded in SWT and corresponding event is not translated to the user nor as "KeyEvent" nor as "button pressed")
		if (getReturnCode() == Window.CANCEL) {
			panel.buttonPressed(IDialogConstants.CANCEL_ID);
		}
		return panel.canClose() && super.close();
	}

	protected Control createInfoPanel(Composite parent) {
		Color bgColor = new Color(null, 255, 255, 255);

		GridLayout layout = null;
		GridData data = null;

		Composite infoPanel = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 1;
		layout.marginWidth = 1;
		layout.numColumns = 2;
		infoPanel.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		Point pt = panel.getPrefferedSize();
		data.widthHint = pt.x;
		infoPanel.setLayoutData(data);
		infoPanel.setBackground(bgColor);

		Composite leftSide = new Composite(infoPanel, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.horizontalSpacing = 1;
		leftSide.setLayout(layout);
		data = new GridData(GridData.FILL_BOTH);
		leftSide.setLayoutData(data);
		leftSide.setBackground(bgColor);

		Label iconLabel = new Label(infoPanel, SWT.NONE);
		infoImage = findImage(panel.getImagePath());
		iconLabel.setImage(infoImage);

		Font defaultFont = JFaceResources.getBannerFont();
		FontData[] fData = defaultFont.getFontData();
		mainLabelFont = new Font(UIMonitorUtility.getDisplay(), fData);

		Label description = new Label(leftSide, SWT.WRAP);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		description.setLayoutData(data);
		String text = panel.getDialogDescription();
		description.setText(text != null ? text : ""); //$NON-NLS-1$
		description.setFont(mainLabelFont);
		description.setBackground(bgColor);

		icon = new Label(leftSide, SWT.NONE);
		data = new GridData();
		data.verticalAlignment = SWT.BEGINNING;
		icon.setLayoutData(data);
		icon.setBackground(bgColor);

		message = new Label(leftSide, SWT.WRAP);
		data = new GridData(GridData.FILL_BOTH);
		data.horizontalIndent = 3;
		message.setLayoutData(data);
		message.setBackground(bgColor);

		setMessage(IDialogManager.LEVEL_OK, null);

		return infoPanel;
	}

	protected Control createMainPanel(Composite parent) {
		GridLayout layout = null;
		GridData data = null;

		Composite fullSizePanel = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		fullSizePanel.setLayout(layout);
		data = new GridData(GridData.FILL_BOTH);
		fullSizePanel.setLayoutData(data);

		Label separator = new Label(fullSizePanel, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite customPanel = new Composite(fullSizePanel, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 7;
		layout.marginWidth = 7;
		customPanel.setLayout(layout);
		data = new GridData(GridData.FILL_BOTH);
		Point pt = panel.getPrefferedSize();
		data.widthHint = pt.x;
		customPanel.setLayoutData(data);

		separator = new Label(fullSizePanel, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		panel.createControls(customPanel);

		return customPanel;
	}

	protected Control createBottomPanel(Composite parent) {
		GridLayout layout = null;
		GridData data = null;

		Composite composite = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 7;
		layout.marginWidth = 7;
		layout.numColumns = 2;
		layout.horizontalSpacing = 0;
		composite.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		composite.setLayoutData(data);

		createHelpImageButton(composite);

		buttonBar = createButtonPanel(composite);
		((GridData) buttonBar.getLayoutData()).horizontalIndent = 7;

		return composite;
	}

	protected ToolBar createHelpImageButton(Composite parent) {
		ToolBar toolBar = new ToolBar(parent, SWT.FLAT | SWT.NO_FOCUS);
		((GridLayout) parent.getLayout()).numColumns++;
		toolBar.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
		final Cursor cursor = new Cursor(parent.getDisplay(), SWT.CURSOR_HAND);
		toolBar.setCursor(cursor);
		toolBar.addDisposeListener(e -> cursor.dispose());
		ToolItem item = new ToolItem(toolBar, SWT.NONE);
		item.setImage(JFaceResources.getImage(DLG_IMG_HELP));
		item.setToolTipText(JFaceResources.getString("helpToolTip")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				mainComposite.notifyListeners(SWT.Help, new Event());
			}
		});
		return toolBar;
	}

	protected Control createButtonPanel(Composite parent) {
		GridLayout layout = null;
		GridData data = null;

		Composite buttonPanel = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		buttonPanel.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalAlignment = SWT.RIGHT;
		buttonPanel.setLayoutData(data);

		return createButtonBar(buttonPanel);
	}

	protected Image findImage(String imagePath) {
		SVNTeamUIPlugin plugin = SVNTeamUIPlugin.instance();
		ImageDescriptor descriptor = plugin
				.getImageDescriptor(imagePath == null ? "icons/wizards/newconnect.gif" : imagePath); //$NON-NLS-1$
		if (descriptor == null) {
			descriptor = ImageDescriptor.getMissingImageDescriptor();
		}
		return descriptor.createImage();
	}

}
