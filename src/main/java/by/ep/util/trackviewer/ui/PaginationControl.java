package by.ep.util.trackviewer.ui;

import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


public class PaginationControl extends Composite {
    public static final int ITEMS_PER_PAGE = 1000;

    private Consumer<PaginationControl> pageNumberChangedFunction;

    private final Text pageNumberText;
    private final Text totalPagesText;
    private final Text totalRecordsText;

    public PaginationControl(Shell shell) {

        super(shell, SWT.BORDER);

        GridLayout groupGridLayout = new GridLayout(8, false);
        this.setLayout(groupGridLayout);

        Button prevButton = new Button(this, SWT.FLAT);
        prevButton.setText("<");
        prevButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                selectPrevPage();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {

                selectPrevPage();
            }
        });

        prevButton.pack();

        pageNumberText = new Text(this, SWT.BORDER);
        pageNumberText.setText("1         ");
        pageNumberText.pack();

        Button nextButton = new Button(this, SWT.FLAT);
        nextButton.setText(">");
        nextButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                selectNextPage();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {

                selectNextPage();
            }
        });
        nextButton.pack();

        Button selectPageButton = new Button(this, SWT.NONE);
        selectPageButton.setText("Select");
        selectPageButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                selectPageNumber();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {

                selectPageNumber();
            }
        });
        selectPageButton.pack();

        Label totalPagesLabel = new Label(this, SWT.NONE);
        totalPagesLabel.setText("Total pages:");
        totalPagesLabel.pack();

        totalPagesText = new Text(this, SWT.READ_ONLY);
        totalPagesText.setText("0         ");
        totalPagesText.pack();

        Label totalRecordsLabel = new Label(this, SWT.NONE);
        totalRecordsLabel.setText("Total records:");
        totalRecordsLabel.pack();

        totalRecordsText = new Text(this, SWT.READ_ONLY);
        totalRecordsText.setText("0         ");
        totalRecordsText.pack();

        this.pack();
    }

    public int getCurrentPage() {

        return Integer.parseInt(this.pageNumberText.getText().trim());
    }

    public void setCurrentPage(int pageNumber) {
        this.pageNumberText.setText(String.valueOf(pageNumber));
    }

    public int getPagesCount() {

        return Integer.parseInt(this.totalPagesText.getText().trim());
    }

    public void setRecordsCount(int totalRecords) {

        this.totalRecordsText.setText(String.valueOf(totalRecords));
        this.totalPagesText.setText(String.valueOf((totalRecords + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE));
    }

    public void setPageNumberChangedFunction(Consumer<PaginationControl> pageNumberChangedFunction) {
        this.pageNumberChangedFunction = pageNumberChangedFunction;
    }

    private void selectPrevPage() {

        int currentPage = getCurrentPage();
        if (currentPage > 1) {
            setCurrentPage(currentPage - 1);
            if (pageNumberChangedFunction != null) {
                pageNumberChangedFunction.accept(this);
            }
        }
    }

    private void selectNextPage() {

        int currentPage = getCurrentPage();
        if (currentPage < getPagesCount()) {
            setCurrentPage(currentPage + 1);
            if (pageNumberChangedFunction != null) {
                pageNumberChangedFunction.accept(this);
            }
        }
    }

    private void selectPageNumber() {

        if (pageNumberChangedFunction != null) {
            pageNumberChangedFunction.accept(this);
        }
    }
}
