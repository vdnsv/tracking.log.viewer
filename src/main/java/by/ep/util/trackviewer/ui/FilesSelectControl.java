package by.ep.util.trackviewer.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.util.function.Consumer;

public class FilesSelectControl extends Composite {
    private Consumer<String> scanButtonClickFunc;
    private Text dirText;

    public FilesSelectControl(Shell shell) {

        super(shell, SWT.BORDER);

        GridLayout gridLayout = new org.eclipse.swt.layout.GridLayout(4, false);
        this.setLayout(gridLayout);

        Label dirLabel = new Label(this, SWT.NONE);
        dirLabel.setText("Directory:");
        dirText = new Text(this, SWT.BORDER);
        dirText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

        Button button = new Button(this, SWT.PUSH);
        button.setText("Browse...");
        button.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {

                DirectoryDialog dlg = new DirectoryDialog(shell);

                // Set the initial filter path according
                // to anything they've selected or typed in
                dlg.setFilterPath(dirText.getText());

                // Change the title bar text
                dlg.setText("DirectoryDialog");

                // Customizable message displayed in the dialog
                dlg.setMessage("Select a directory");

                // Calling open() will open and run the dialog.
                // It will return the selected directory, or
                // null if user cancels
                String dir = dlg.open();
                if (dir != null) {
                    // Set the text box to the new selection
                    dirText.setText(dir);
                }
            }
        });

        Button scanButton = new Button(this, SWT.PUSH);
        scanButton.setText("Scan");
        scanButton.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent event) {

                if (scanButtonClickFunc != null) {
                    scanButtonClickFunc.accept(dirText.getText());
                }
            }

            public void widgetDefaultSelected(SelectionEvent event) {

                if (scanButtonClickFunc != null) {
                    scanButtonClickFunc.accept(dirText.getText());
                }
            }
        });

    }

    public Consumer<String> getScanButtonClickFunc() {

        return scanButtonClickFunc;
    }

    public void setScanButtonClickFunc(Consumer<String> scanButtonClickFunc) {

        this.scanButtonClickFunc = scanButtonClickFunc;
    }

    public void setDirectory(final String directoryName) {
        this.dirText.setText(directoryName);
    }
}
