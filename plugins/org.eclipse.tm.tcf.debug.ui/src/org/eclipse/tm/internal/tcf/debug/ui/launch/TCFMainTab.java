/*******************************************************************************
 * Copyright (c) 2007, 2008 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tm.internal.tcf.debug.ui.launch;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.tm.internal.tcf.debug.launch.TCFLaunchDelegate;
import org.eclipse.tm.internal.tcf.debug.ui.Activator;
import org.eclipse.tm.tcf.protocol.IChannel;
import org.eclipse.tm.tcf.protocol.IPeer;
import org.eclipse.tm.tcf.protocol.Protocol;
import org.eclipse.tm.tcf.protocol.IChannel.IChannelListener;
import org.eclipse.tm.tcf.services.ILocator;


/**
 * Launch configuration dialog tab to specify the Target Communication Framework
 * configuration.
 */
public class TCFMainTab extends AbstractLaunchConfigurationTab {
    
    private Text peer_id_text;
    private Text program_text;
    private Tree peer_tree;
    private final PeerInfo peer_info = new PeerInfo();
    private Display display;

    private final Map<LocatorListener,ILocator> listeners = new HashMap<LocatorListener,ILocator>();
    private final Map<String,Image> image_cache = new HashMap<String,Image>();
    
    private static class PeerInfo {
        PeerInfo parent;
        int index;
        String id;
        Map<String,String> attrs;
        PeerInfo[] children;
        boolean children_pending;
        Throwable children_error;
        IPeer peer;
    }
    
    private class LocatorListener implements ILocator.LocatorListener {
        
        private final PeerInfo parent;
        
        LocatorListener(PeerInfo parent) {
            this.parent = parent;
        }

        public void peerAdded(final IPeer peer) {
            if (display == null) return;
            final String id = peer.getID();
            final HashMap<String,String> attrs = new HashMap<String,String>(peer.getAttributes());
            display.asyncExec(new Runnable() {
                public void run() {
                    if (parent.children_error != null) return;
                    PeerInfo[] arr = parent.children;
                    PeerInfo[] buf = new PeerInfo[arr.length + 1];
                    System.arraycopy(arr, 0, buf, 0, arr.length);
                    PeerInfo info = new PeerInfo();
                    info.parent = parent;
                    info.index = arr.length;
                    info.id = id;
                    info.attrs = attrs;
                    info.peer = peer;
                    buf[arr.length] = info;
                    parent.children = buf;
                    updateItems(parent);
                }
            });
        }

        public void peerChanged(final IPeer peer) {
            if (display == null) return;
            final String id = peer.getID();
            final HashMap<String,String> attrs = new HashMap<String,String>(peer.getAttributes());
            display.asyncExec(new Runnable() {
                public void run() {
                    if (parent.children_error != null) return;
                    PeerInfo[] arr = parent.children;
                    for (int i = 0; i < arr.length; i++) {
                        if (arr[i].id.equals(id)) {
                            arr[i].attrs = attrs;
                            arr[i].peer = peer;
                            loadChildren(arr[i]);
                            updateItems(parent);
                        }
                    }
                }
            });
        }

        public void peerRemoved(final String id) {
            if (display == null) return;
            display.asyncExec(new Runnable() {
                public void run() {
                    if (parent.children_error != null) return;
                    PeerInfo[] arr = parent.children;
                    PeerInfo[] buf = new PeerInfo[arr.length - 1];
                    int j = 0;
                    for (int i = 0; i < arr.length; i++) {
                        if (!arr[i].id.equals(id)) {
                            buf[j++] = arr[i];
                        }
                    }
                    parent.children = buf;
                    updateItems(parent);
                }
            });
        }

        public void peerHeartBeat(final String id) {
            if (display == null) return;
            display.asyncExec(new Runnable() {
                public void run() {
                    if (parent.children_error != null) return;
                    PeerInfo[] arr = parent.children;
                    for (int i = 0; i < arr.length; i++) {
                        if (arr[i].id.equals(id)) {
                            if (arr[i].children_error != null) {
                                loadChildren(arr[i]);
                            }
                            break;
                        }
                    }
                }
            });
        }
    }
    
    public void createControl(Composite parent) {
        display = parent.getDisplay();

        Font font = parent.getFont();
        Composite comp = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, true);
        comp.setLayout(layout);
        comp.setFont(font);

        GridData gd = new GridData(GridData.FILL_BOTH);
        comp.setLayoutData(gd);
        setControl(comp);

        createTargetGroup(comp);
        createProgramGroup(comp);
    }

    private void createTargetGroup(Composite parent) {
        Font font = parent.getFont();
        
        Group group = new Group(parent, SWT.NONE);
        GridLayout top_layout = new GridLayout();
        top_layout.verticalSpacing = 0;
        top_layout.numColumns = 2;
        group.setLayout(top_layout);
        group.setLayoutData(new GridData(GridData.FILL_BOTH));
        group.setFont(font);
        group.setText("Target");
        
        createVerticalSpacer(group, top_layout.numColumns);
        
        Label host_label = new Label(group, SWT.NONE);
        host_label.setText("Target ID:");
        host_label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        host_label.setFont(font);
        
        peer_id_text = new Text(group, SWT.SINGLE | SWT.BORDER);
        peer_id_text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        peer_id_text.setFont(font);
        peer_id_text.setEditable(false);
        peer_id_text.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                updateLaunchConfigurationDialog();
            }
        });

        createVerticalSpacer(group, top_layout.numColumns);

        Label peer_label = new Label(group, SWT.NONE);
        peer_label.setText("Available targets:");
        peer_label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        peer_label.setFont(font);
                
        loadChildren(peer_info);
        
        peer_tree = new Tree(group, SWT.VIRTUAL | SWT.BORDER | SWT.SINGLE);
        GridData gd = new GridData(GridData.FILL, GridData.FILL, true, true, 2, 1);
        gd.minimumHeight = 150;
        peer_tree.setLayoutData(gd);
        
        for (int i = 0; i < 5; i++) {
            TreeColumn column = new TreeColumn(peer_tree, SWT.LEAD, i);
            column.setMoveable(true);
            switch (i) {
            case 0:
                column.setText("Name");
                column.setWidth(160);
                break;
            case 1:
                column.setText("OS");
                column.setWidth(100);
                break;
            case 2:
                column.setText("Transport");
                column.setWidth(60);
                break;
            case 3:
                column.setText("Host");
                column.setWidth(100);
                break;
            case 4:
                column.setText("Port");
                column.setWidth(40);
                break;
            }
        }
                
        peer_tree.setHeaderVisible(true);
        peer_tree.setFont(font);
        peer_tree.addListener(SWT.SetData, new Listener() {
            public void handleEvent(Event event) {
                TreeItem item = (TreeItem)event.item;
                PeerInfo info = findPeerInfo(item);
                if (info == null) {
                    PeerInfo parent = findPeerInfo(item.getParentItem());
                    if (parent == null) {
                        item.setText("Invalid");
                    }
                    else {
                        if (parent.children == null || parent.children_error != null) {
                            loadChildren(parent);
                        }
                        updateItems(parent);
                    }
                }
                else {
                    fillItem(item, info);
                }
            }
        });
        peer_tree.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {
            }
            public void widgetSelected(SelectionEvent e) {
                TreeItem[] selections = peer_tree.getSelection();
                if (selections.length > 0) {
                    assert selections.length == 1;
                    PeerInfo info = findPeerInfo(selections[0]);
                    if (info != null) peer_id_text.setText(getPath(info));
                }
            }
        });

        createVerticalSpacer(group, top_layout.numColumns);
        
        Button button_test = new Button(group, SWT.PUSH);
        button_test.setText("Run &Diagnostics");
        button_test.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        button_test.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                TreeItem[] selection = peer_tree.getSelection();
                if (selection.length > 0) {
                    assert selection.length == 1;
                    runDiagnostics(selection[0], false);
                }
            }
        });

        Button button_loop = new Button(group, SWT.PUSH);
        button_loop.setText("Diagnostics &Loop");
        button_loop.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        button_loop.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                TreeItem[] selection = peer_tree.getSelection();
                if (selection.length > 0) {
                    assert selection.length == 1;
                    runDiagnostics(selection[0], true);
                }
            }
        });
    }

    private void createProgramGroup(Composite parent) {
        display = parent.getDisplay();

        Font font = parent.getFont();
        
        Group group = new Group(parent, SWT.NONE);
        GridLayout top_layout = new GridLayout();
        group.setLayout(top_layout);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        group.setFont(font);
        group.setText("Program");
        
        program_text = new Text(group, SWT.SINGLE | SWT.BORDER);
        program_text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        program_text.setFont(font);
        program_text.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                updateLaunchConfigurationDialog();
            }
        });
    }
    
    @Override
    public void dispose() {
        Protocol.invokeAndWait(new Runnable() {
            public void run() {
                for (Iterator<LocatorListener> i = listeners.keySet().iterator(); i.hasNext();) {
                    LocatorListener listener = i.next();
                    listeners.get(listener).removeListener(listener);
                }
                listeners.clear();
                display = null;
            }
        });
        for (Image i : image_cache.values()) i.dispose();
        image_cache.clear();
        super.dispose();
    }

    public String getName() {
        return "Main";
    }
    
    @Override
    public Image getImage() {
        return getImage("icons/tcf.gif");
    }

    public void initializeFrom(ILaunchConfiguration configuration) {
        try {
            String id = configuration.getAttribute(
                    TCFLaunchDelegate.ATTR_PEER_ID, (String)null);
            if (id != null) {
                peer_id_text.setText(id);
                TreeItem item = findItem(findPeerInfo(id));
                if (item != null) peer_tree.setSelection(item);
            }
            program_text.setText(configuration.getAttribute(
                    TCFLaunchDelegate.ATTR_PROGRAM_FILE, "")); //$NON-NLS-1$
        }
        catch (CoreException e) {
            setErrorMessage(e.getMessage());
        }
    }

    public boolean isValid(ILaunchConfiguration launchConfig) {
        String id = peer_id_text.getText().trim();
        if (id.length() == 0) {
            setErrorMessage("Specify a target ID");
            return false;
        }
        setErrorMessage(null);
        return super.isValid(launchConfig);
    }

    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        String id = peer_id_text.getText().trim();
        if (id.length() == 0) id = null;
        configuration.setAttribute(TCFLaunchDelegate.ATTR_PEER_ID, id);
        String nm = program_text.getText().trim();
        if (nm.length() == 0) nm = null;
        configuration.setAttribute(TCFLaunchDelegate.ATTR_PROGRAM_FILE, nm);
    }

    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
        configuration.setAttribute(TCFLaunchDelegate.ATTR_PEER_ID, "TCFLocal");
        configuration.setAttribute(TCFLaunchDelegate.ATTR_PROGRAM_FILE, (String)null);
    }
    
    private LocatorListener createLocatorListener(PeerInfo peer, ILocator locator) {
        assert Protocol.isDispatchThread();
        Map<String,IPeer> map = locator.getPeers();
        PeerInfo[] buf = new PeerInfo[map.size()];
        int n = 0;
        for (IPeer p : map.values()) {
            PeerInfo info = new PeerInfo();
            info.parent = peer;
            info.index = n;
            info.id = p.getID();
            info.attrs = new HashMap<String,String>(p.getAttributes());
            info.peer = p;
            buf[n++] = info;
        }
        LocatorListener listener = new LocatorListener(peer);
        listeners.put(listener, locator);
        locator.addListener(listener);
        setChildren(peer, null, buf);
        return listener;
    }
    
    private boolean canHaveChildren(PeerInfo parent) {
        return parent == peer_info || parent.attrs.get(IPeer.ATTR_PROXY) != null;
    }
    
    private void loadChildren(final PeerInfo parent) {
        assert Thread.currentThread() == display.getThread();
        if (parent.children_pending) return;
        if (!canHaveChildren(parent)) {
            if (parent.children == null) updateItems(parent);
            return;
        }
        parent.children_pending = true;
        Protocol.invokeAndWait(new Runnable() {
            public void run() {
                if (parent == peer_info) {
                    createLocatorListener(peer_info, Protocol.getLocator());
                }
                else {
                    final IChannel channel = parent.peer.openChannel();
                    final LocatorListener[] listener = new LocatorListener[1];
                    channel.addChannelListener(new IChannelListener() {
                        public void congestionLevel(int level) {
                        }
                        public void onChannelClosed(Throwable error) {
                            setChildren(parent, error, new PeerInfo[0]);
                            if (listener[0] != null) listeners.remove(listener[0]);
                        }
                        public void onChannelOpened() {
                            ILocator locator = channel.getRemoteService(ILocator.class);
                            if (locator == null) {
                                channel.close();
                            }
                            else {
                                listener[0] = createLocatorListener(parent, locator);
                            }
                        }
                    });
                }
            }
        });
    }
    
    private void setChildren(final PeerInfo parent, final Throwable error, final PeerInfo[] children) {
        assert Protocol.isDispatchThread();
        display.asyncExec(new Runnable() {
            public void run() {
                parent.children_pending = false;
                parent.children = children;
                parent.children_error = error;
                updateItems(parent);
            }
        });
    }
    
    private void updateItems(PeerInfo parent) {
        assert Thread.currentThread() == display.getThread();
        if (!canHaveChildren(parent)) {
            parent.children = new PeerInfo[0];
            parent.children_error = null;
        }
        PeerInfo[] arr = parent.children;
        TreeItem[] items = null;
        if (arr == null || parent.children_error != null) {
            if (parent == peer_info) {
                peer_tree.setItemCount(1);
                items = peer_tree.getItems();
            }
            else {
                TreeItem item = findItem(parent);
                if (item == null) return;
                item.setItemCount(1);
                items = item.getItems();
            }
            if (parent.children_pending) {
                items[0].setForeground(display.getSystemColor(SWT.COLOR_LIST_FOREGROUND));
                items[0].setText("Loading...");
            }
            else if (parent.children_error != null) {
                String msg = parent.children_error.getMessage().replace('\n', ' ');
                items[0].setForeground(display.getSystemColor(SWT.COLOR_RED));
                items[0].setText(msg);
            }
            else {
                items[0].setForeground(display.getSystemColor(SWT.COLOR_RED));
                items[0].setText("Invalid children list");
            }
            int n = peer_tree.getColumnCount();
            for (int i = 1; i < n; i++) items[0].setText(i, "");
            items[0].setItemCount(0);
        }
        else {
            if (parent == peer_info) {
                peer_tree.setItemCount(arr.length);
                items = peer_tree.getItems();
            }
            else {
                TreeItem item = findItem(parent);
                if (item == null) return;
                item.setItemCount(arr.length);
                items = item.getItems();
            }
            assert items.length == arr.length;
            for (int i = 0; i < items.length; i++) fillItem(items[i], arr[i]);
            String id = peer_id_text.getText();
            TreeItem item = findItem(findPeerInfo(id));
            if (item != null) peer_tree.setSelection(item);
        }
    }

    private PeerInfo findPeerInfo(TreeItem item) {
        assert Thread.currentThread() == display.getThread();
        if (item == null) return peer_info;
        TreeItem parent = item.getParentItem();
        PeerInfo info = findPeerInfo(parent);
        if (info == null) return null;
        if (info.children == null) return null;
        if (info.children_error != null) return null;
        int i = parent == null ? peer_tree.indexOf(item) : parent.indexOf(item);
        if (i < 0 || i >= info.children.length) return null;
        assert info.children[i].index == i;
        return info.children[i];
    }
    
    private PeerInfo findPeerInfo(String path) {
        int i = path.lastIndexOf('/');
        String id = null;
        PeerInfo[] arr = null;
        if (i < 0) {
            arr = peer_info.children;
            id = path;
        }
        else {
            PeerInfo p = findPeerInfo(path.substring(0, i));
            if (p == null) return null;
            arr = p.children;
            id = path.substring(i + 1);
        }
        if (arr == null) return null;
        for (int n = 0; n < arr.length; n++) {
            if (arr[n].id.equals(id)) return arr[n];
        }
        return null;
    }
    
    private TreeItem findItem(PeerInfo info) {
        if (info == null) return null;
        assert info.parent != null;
        if (info.parent == peer_info) {
            return peer_tree.getItem(info.index);
        }
        TreeItem i = findItem(info.parent);
        if (i == null) return null;
        peer_tree.showItem(i);
        return i.getItem(info.index);
    }
    
    private void runDiagnostics(TreeItem item, boolean loop) {
        final Shell shell = new Shell(getShell(), SWT.TITLE | SWT.PRIMARY_MODAL);
        GridLayout layout = new GridLayout();
        layout.verticalSpacing = 0;
        layout.numColumns = 2;
        shell.setLayout(layout);
        shell.setText("Running Diagnostics...");
        CLabel label = new CLabel(shell, SWT.NONE);
        label.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
        label.setText("Running Diagnostics...");
        final TCFSelfTest[] test = new TCFSelfTest[1];
        Button button_cancel = new Button(shell, SWT.PUSH);
        button_cancel.setText("&Cancel");
        button_cancel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
        button_cancel.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                Protocol.invokeLater(new Runnable() {
                    public void run() {
                        if (test[0] != null) test[0].cancel();
                    }
                });
            }
        });
        createVerticalSpacer(shell, 0);
        ProgressBar bar = new ProgressBar(shell, SWT.HORIZONTAL);
        bar.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 2, 1));
        shell.setDefaultButton(button_cancel);
        shell.pack();
        shell.setSize(483, shell.getSize().y);
        Rectangle rc0 = getShell().getBounds();
        Rectangle rc1 = shell.getBounds();
        shell.setLocation(rc0.x + (rc0.width - rc1.width) / 2, rc0.y + (rc0.height - rc1.height) / 2);
        shell.setVisible(true);
        runDiagnostics(item, loop, test, shell, label, bar);
    }
    
    private void runDiagnostics(final TreeItem item, final boolean loop, final TCFSelfTest[] test,
            final Shell shell, final CLabel label, final ProgressBar bar) {
        final TCFSelfTest.TestListener done = new TCFSelfTest.TestListener() {
            private String last_text = "";
            private int last_count = 0;
            private int last_total = 0;
            public void progress(final String label_text, final int count_done, final int count_total) {
                if ((label_text == null || last_text.equals(label_text)) &&
                        last_total == count_total &&
                        (count_done - last_count) / (float)count_total < 0.02f) return;
                if (label_text != null) last_text = label_text;
                last_total = count_total;
                last_count = count_done;
                display.asyncExec(new Runnable() {
                    public void run() {
                        label.setText(last_text);
                        bar.setMinimum(0);
                        bar.setMaximum(last_total);
                        bar.setSelection(last_count);
                    }
                });
            }
            public void done(final Collection<Throwable> errors) {
                final boolean b = test[0] == null ? false : test[0].isCanceled();
                test[0] = null;
                display.asyncExec(new Runnable() {
                    public void run() {
                        if (errors.size() > 0) {
                            shell.dispose();
                            new TestErrorsDialog(getControl().getShell(),
                                    getImage("icons/tcf.gif"), errors).open();
                        }
                        else if (loop && !b) {
                            runDiagnostics(item, true, test, shell, label, bar);
                        }
                        else {
                            shell.dispose();
                        }
                    }
                });
            }
        };
        final PeerInfo info = findPeerInfo(item);
        Protocol.invokeLater(new Runnable() {
            public void run() {
                try {
                    test[0] = new TCFSelfTest(info.peer, done);
                }
                catch (Throwable x) {
                    ArrayList<Throwable> errors = new ArrayList<Throwable>();
                    errors.add(x);
                    done.done(errors);
                }
            }
        });
    }
    
    private void fillItem(TreeItem item, PeerInfo info) {
        String text[] = new String[5];
        text[0] = info.attrs.get(IPeer.ATTR_NAME);
        text[1] = info.attrs.get(IPeer.ATTR_OS_NAME);
        text[2] = info.attrs.get(IPeer.ATTR_TRANSPORT_NAME);
        text[3] = info.attrs.get(IPeer.ATTR_IP_HOST);
        text[4] = info.attrs.get(IPeer.ATTR_IP_PORT);
        item.setText(text);
        item.setForeground(display.getSystemColor(SWT.COLOR_LIST_FOREGROUND));
        item.setImage(getImage(getImageName(info)));
        if (!canHaveChildren(info)) item.setItemCount(0);
        else if (info.children == null || info.children_error != null) item.setItemCount(1);
        else item.setItemCount(info.children.length);
    }
    
    private String getPath(PeerInfo info) {
        if (info.parent == peer_info) return info.id;
        return getPath(info.parent) + "/" + info.id;
    }
    
    private Image getImage(String name) {
        if (name == null) return null;
        if (display == null) return null;
        Image image = image_cache.get(name);
        if (image == null) {
            URL url = FileLocator.find(Activator.getDefault().getBundle(), new Path(name), null);
            ImageDescriptor descriptor = null;
            if (url == null) {
                descriptor = ImageDescriptor.getMissingImageDescriptor();
            }
            else {
                descriptor = ImageDescriptor.createFromURL(url);
            }
            image = descriptor.createImage(display);
            image_cache.put(name, image);
        }
        return image;
    }

    private String getImageName(PeerInfo info) {
        return "icons/tcf.gif";
    }
}
