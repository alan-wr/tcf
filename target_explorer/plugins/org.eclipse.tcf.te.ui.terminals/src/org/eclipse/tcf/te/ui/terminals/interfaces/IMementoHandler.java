/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.terminals.interfaces;

import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.ui.IMemento;

/**
 * Terminal properties memento handler.
 */
public interface IMementoHandler {

    /**
     * Saves the terminal properties in the given memento.
     *
     * @param memento The memento. Must not be <code>null</code>.
     * @param properties The properties container containing the terminal properties to save. Must not be <code>null</code>.
     */
    public void saveState(IMemento memento, IPropertiesContainer properties);

    /**
     * Restore the terminal properties from the given memento.
     *
     * @param memento The memento. Must not be <code>null</code>.
     * @param properties The properties container receiving the restored terminal properties. Must not be <code>null</code>.
     */
    public void restoreState(IMemento memento, IPropertiesContainer properties);
}