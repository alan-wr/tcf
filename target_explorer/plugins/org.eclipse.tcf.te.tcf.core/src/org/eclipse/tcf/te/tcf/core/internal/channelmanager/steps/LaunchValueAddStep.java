/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.core.internal.channelmanager.steps;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.StepperAttributeUtil;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.tcf.core.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager;
import org.eclipse.tcf.te.tcf.core.interfaces.steps.ITcfStepAttributes;
import org.eclipse.tcf.te.tcf.core.steps.AbstractPeerStep;
import org.eclipse.tcf.te.tcf.core.va.interfaces.IValueAdd;

/**
 * LaunchValueAddStep
 */
public class LaunchValueAddStep extends AbstractPeerStep {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStep#validateExecute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void validateExecute(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(context);
		Assert.isNotNull(data);
		Assert.isNotNull(fullQualifiedId);
		Assert.isNotNull(monitor);

		IValueAdd valueAdd = (IValueAdd)StepperAttributeUtil.getProperty(ITcfStepAttributes.ATTR_VALUE_ADD, fullQualifiedId, data);
		if (valueAdd == null) {
			throw new CoreException(new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(), "Value-add descriptor instance not set.")); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStep#execute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void execute(IStepContext context, final IPropertiesContainer data, final IFullQualifiedId fullQualifiedId, IProgressMonitor monitor, final ICallback callback) {
		Assert.isNotNull(context);
		Assert.isNotNull(data);
		Assert.isNotNull(fullQualifiedId);
		Assert.isNotNull(monitor);
		Assert.isNotNull(callback);

		final IValueAdd valueAdd = (IValueAdd)StepperAttributeUtil.getProperty(ITcfStepAttributes.ATTR_VALUE_ADD, fullQualifiedId, data);
		final String peerId = getActivePeerContext(context, data, fullQualifiedId).getID();
		final boolean useValueAdds = !StepperAttributeUtil.getBooleanProperty(IChannelManager.FLAG_NO_VALUE_ADD, fullQualifiedId, data);

		if (useValueAdds) {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					valueAdd.isAlive(peerId, new Callback() {
						@Override
						protected void internalDone(Object caller, IStatus status) {
							boolean alive = ((Boolean)getResult()).booleanValue();

							if (!alive) {
								valueAdd.launch(peerId, callback);
							}
							else {
								callback(data, fullQualifiedId, callback, Status.OK_STATUS, null);
							}
						}
					});
				}
			};

			if (Protocol.isDispatchThread()) runnable.run();
			else Protocol.invokeLater(runnable);
		} else {
			callback(data, fullQualifiedId, callback, Status.OK_STATUS, null);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.steps.AbstractStep#rollback(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.core.runtime.IStatus, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void rollback(final IStepContext context, final IPropertiesContainer data, final IStatus status, final IFullQualifiedId fullQualifiedId, final IProgressMonitor monitor, final ICallback callback) {
		final IValueAdd valueAdd = (IValueAdd)StepperAttributeUtil.getProperty(ITcfStepAttributes.ATTR_VALUE_ADD, fullQualifiedId, data);
		final String peerId = getActivePeerContext(context, data, fullQualifiedId).getID();
		final boolean useValueAdds = !StepperAttributeUtil.getBooleanProperty(IChannelManager.FLAG_NO_VALUE_ADD, fullQualifiedId, data);

		if (useValueAdds) {
			Runnable runnable = new Runnable() {
				@SuppressWarnings("synthetic-access")
				@Override
				public void run() {
					valueAdd.shutdown(peerId, callback);
					LaunchValueAddStep.super.rollback(context, data, status, fullQualifiedId, monitor, callback);
				}
			};

			if (Protocol.isDispatchThread()) runnable.run();
			else Protocol.invokeLater(runnable);
		} else {
			super.rollback(context, data, status, fullQualifiedId, monitor, callback);
		}
	}
}