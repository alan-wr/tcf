/*******************************************************************************
 * Copyright (c) 2011 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.controls.validator;

/**
 * Netmask verify listener.
 */
public class NetMaskVerifyListener extends NameOrIPVerifyListener {

	/**
	 * @param attributes
	 */
	public NetMaskVerifyListener() {
		super(ATTR_IP);
	}

}
