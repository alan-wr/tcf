/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 * Max Weninger (Wind River) - [361363] [TERMINALS] Implement "Pin&Clone" for the "Terminals" view
 *******************************************************************************/
package org.eclipse.tcf.te.ui.terminals.nls;

import org.eclipse.osgi.util.NLS;

/**
 * Terminals plug-in externalized strings management.
 */
public class Messages extends NLS {

	// The plug-in resource bundle name
	private static final String BUNDLE_NAME = "org.eclipse.tcf.te.ui.terminals.nls.Messages"; //$NON-NLS-1$

	/**
	 * Static constructor.
	 */
	static {
		// Load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	// **** Declare externalized string id's down here *****

	public static String AbstractAction_error_commandExecutionFailed;

	public static String AbstractConfigurationPanel_delete;
	public static String AbstractConfigurationPanel_deleteButtonTooltip;
	public static String AbstractConfigurationPanel_hosts;
	public static String AbstractConfigurationPanel_encoding;

	public static String TabTerminalListener_consoleTerminated;

	public static String PinTerminalAction_menu;
	public static String PinTerminalAction_toolTip;

	public static String ProcessSettingsPage_dialogTitle;
	public static String ProcessSettingsPage_processImagePathSelectorControl_label;
	public static String ProcessSettingsPage_processImagePathSelectorControl_button;
	public static String ProcessSettingsPage_processArgumentsControl_label;
	public static String ProcessSettingsPage_localEchoSelectorControl_label;

	public static String OutputStreamMonitor_error_readingFromStream;

	public static String InputStreamMonitor_error_writingToStream;

	public static String TerminalService_error_cannotCreateConnector;
	public static String TerminalService_defaultTitle;

	public static String LaunchTerminalSettingsDialog_title;
	public static String LaunchTerminalSettingsDialog_combo_label;
	public static String LaunchTerminalSettingsDialog_group_label;

    public static String TabScrollLockAction_text;
    public static String TabScrollLockAction_tooltip;

    public static String LaunchTerminalSettingsDialog_error_title;
	public static String LaunchTerminalSettingsDialog_error_invalidSettings;

	public static String TabFolderManager_encoding;
}
