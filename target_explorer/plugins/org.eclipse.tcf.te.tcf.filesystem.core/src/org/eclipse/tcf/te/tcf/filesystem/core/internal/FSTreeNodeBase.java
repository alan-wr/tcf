/*******************************************************************************
 * Copyright (c) 2015 Wind River Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.internal;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IWindowsFileAttributes;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNodeBase;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IUserAccount;

/**
 * Representation of a file system tree node.
 * <p>
 * <b>Note:</b> Node construction and child list access is limited to the TCF
 * event dispatch thread.
 */
public abstract class FSTreeNodeBase extends PlatformObject implements IFSTreeNodeBase {

	protected abstract int getWin32Attrs();
	protected abstract int getPermissions();

	@Override
    public final boolean getWin32Attr(int attribute) {
    	return (getWin32Attrs() & attribute) == attribute;
    }

	@Override
	public final boolean isHidden() {
        return getWin32Attr(IWindowsFileAttributes.FILE_ATTRIBUTE_HIDDEN);
    }

    @Override
	public final boolean isReadOnly() {
        return getWin32Attr(IWindowsFileAttributes.FILE_ATTRIBUTE_READONLY);
    }

	@Override
	public final boolean getPermission(int bit) {
		return (getPermissions() & bit) == bit;
	}


    @Override
	public final boolean isReadable() {
    	return checkPermission(IFileSystem.S_IRUSR, IFileSystem.S_IRGRP, IFileSystem.S_IROTH);
    }

    @Override
	public final boolean isWritable() {
    	return checkPermission(IFileSystem.S_IWUSR, IFileSystem.S_IWGRP, IFileSystem.S_IWOTH);
    }

	@Override
	public final boolean isExecutable() {
    	return checkPermission(IFileSystem.S_IXUSR, IFileSystem.S_IXGRP, IFileSystem.S_IXOTH);
	}

    private boolean checkPermission(int user, int group, int other) {
        IUserAccount account = getUserAccount();
        int permissions = getPermissions();
        if (account != null && permissions != 0) {
            if (getUID() == account.getEUID()) {
                return getPermission(user);
            }
            if (getGID() == account.getEGID()) {
                return getPermission(group);
            }
            return getPermission(other);
        }
        return false;
    }

	@Override
	public final boolean isAgentOwner() {
        IUserAccount account = getUserAccount();
        if (account != null) {
            return getUID() == account.getEUID();
        }
        return false;
    }

    @Override
	public final boolean isSystemFile() {
    	if (isFileSystem())
    		return false;

        return isWindowsNode() && getWin32Attr(IWindowsFileAttributes.FILE_ATTRIBUTE_SYSTEM);
    }
}