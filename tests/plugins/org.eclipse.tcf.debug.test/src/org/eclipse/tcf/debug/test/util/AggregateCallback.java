/*******************************************************************************
 * Copyright (c) 2006, 2012 Wind River Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.debug.test.util;

import java.util.List;

/**
 * Copied and adapted from org.eclipse.cdt.dsf.concurrent.
 *
 * A request monitor that is used for multiple activities. We are told the
 * number of activities that constitutes completion. When our {@link #done()} is
 * called that many times, the request is considered complete.
 *
 * The usage is as follows: <code><pre>
 *     final CountingRequestMonitor countingRm = new CountingRequestMonitor(fExecutor, null) {
 *         public void handleCompleted() {
 *             System.out.println("All complete, errors=" + !getStatus().isOK());
 *         }
 *     };
 *
 *     int count = 0;
 *     for (int i : elements) {
 *         service.call(i, countingRm);
 *         count++;
 *     }
 *
 *     countingRm.setDoneCount(count);
 * </pre></code>
 *
 * @since 1.0
 */
public class AggregateCallback extends Callback {

    /**
     * Counter tracking the remaining number of times that the done() method
     * needs to be called before this request monitor is actually done.
     */
    private int fDoneCounter;

    /**
     * Flag indicating whether the initial count has been set on this monitor.
     */
    private boolean fInitialCountSet = false;

    public AggregateCallback(Callback parentCallback) {
        super(parentCallback);
    }

    /**
     * Sets the number of times that this request monitor needs to be called
     * before this monitor is truly considered done.  This method must be called
     * exactly once in the life cycle of each counting request monitor.
     * @param count Number of times that done() has to be called to mark the request
     * monitor as complete.  If count is '0', then the counting request monitor is
     * marked as done immediately.
     */
    public synchronized void setDoneCount(int count) {
        assert !fInitialCountSet;
        fInitialCountSet = true;
        fDoneCounter += count;
        if (fDoneCounter <= 0) {
            assert fDoneCounter == 0; // Mismatch in the count.
            super.done();
        }
    }

    /**
     * Called to indicate that one of the calls using this monitor is finished.
     * Only when we've been called the number of times corresponding to the
     * completion count will this request monitor will be considered complete.
     * This method can be called before {@link #setDoneCount(int)}; in that
     * case, we simply bump the count that tracks the number of times we've been
     * called. The monitor will not move into the completed state until after
     * {@link #setDoneCount(int)} has been called (or during if the given
     * completion count is equal to the number of times {@link #done()} has
     * already been called.)
     */
    @Override
    public synchronized void done() {
        fDoneCounter--;
        if (fInitialCountSet && fDoneCounter <= 0) {
            assert fDoneCounter == 0; // Mismatch in the count.
            super.done();
        }
    }

    @Override
    public String toString() {
        return "AggregateError: " + getError().toString(); //$NON-NLS-1$
    }

    @Override
    public synchronized void setError(Throwable error) {
        if ((getError() == null)) {
            super.setError(new AggregateError("") {
                private static final long serialVersionUID = 1L;

                @Override
                public String getMessage() {
                    StringBuffer message = new StringBuffer();
                    List<Throwable> children = getChildren();
                    for (int i = 0; i < children.size(); i++) {
                        message.append(children.get(i).getMessage());
                        if (i + 1 < children.size()) {
                            message.append(" ,"); //$NON-NLS-1$
                        }
                    }
                    return message.toString();
                }
            });
        }


        if ((getError() instanceof AggregateError)) {
            ((AggregateError)getError()).add(error);
        }
    };
}
