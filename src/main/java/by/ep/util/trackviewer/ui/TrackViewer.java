package by.ep.util.trackviewer.ui;


import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

public class TrackViewer {


    public static void main(String[] args) {

        Display display = new Display();
        final Shell shell = new Shell(display);

        shell.setLayout(new GridLayout(1, false));

        FilesSelectControl filesSelectControl = new FilesSelectControl(shell);
        filesSelectControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

        final TabFolder tabFolder = new TabFolder(shell, SWT.NONE);
        tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        TabItem trackingLogTabItem = new TabItem(tabFolder, SWT.NONE);
        trackingLogTabItem.setText("Tracking Log");

        UnboundSamplesControl unboundSamplesComposite = new UnboundSamplesControl(tabFolder);

        TrackingLogControl trackingLogComposite = new TrackingLogControl(tabFolder, filesSelectControl,
                unboundSamplesComposite);
        trackingLogTabItem.setControl(trackingLogComposite);

        TabItem unboundSamplesTabItem = new TabItem(tabFolder, SWT.NONE);
        unboundSamplesTabItem.setText("Unbound Samples");
        unboundSamplesTabItem.setControl(unboundSamplesComposite);

        shell.pack();
        shell.setBounds(display.getClientArea());
        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }

        display.dispose();
    }

}
