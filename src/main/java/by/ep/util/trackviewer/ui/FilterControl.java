package by.ep.util.trackviewer.ui;

import java.util.function.BiConsumer;

import by.ep.util.trackviewer.filter.Expression;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


public class FilterControl  extends Composite {
    private BiConsumer<Expression, Boolean> filterButtonClickFunc;
    private Text filterText;

    FilterControl(Composite parent) {

        super(parent, SWT.BORDER);

        GridLayout gridLayout = new GridLayout(2, false);
        this.setLayout(gridLayout);

        filterText = new Text(this, SWT.MULTI);
        filterText.setToolTipText("Parameters that you can use in filter:\n"
                + "    name,\n"
                + "    duration,\n"
                + "    time,\n"
                + "    typ,\n"
                + "    parentId,\n"
                + "    startTime,\n"
                + "    otherInvocationsCount,\n"
                + "    otherInvocationsTime,\n"
                + "    othersStart,\n"
                + "    othersFinish,\n"
                + "    sqlCount,\n"
                + "    sqlTime,\n"
                + "    params,\n"
                + "    dump\n\n"
                + "Example:\n"
                + "    (duration > 1000 && (name = 'Notification' || name = 'executeSql')) || sqlCount > 50 || dump='com.'");
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        gridData.verticalSpan = 2;

        filterText.setLayoutData(gridData);

        Button filterButton = new Button(this, SWT.FLAT);
        filterButton.setText("Filter");
        Button deepFilterButton = new Button(this, SWT.FLAT);
        deepFilterButton.setText("Deep Filter");

        filterButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                filterButtonClick(filterText.getText(), false);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {

                filterButtonClick(filterText.getText(), false);
            }
        });

        deepFilterButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                filterButtonClick(filterText.getText(), true);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {

                filterButtonClick(filterText.getText(), true);
            }
        });


    }

    private void filterButtonClick(final String filterText, boolean isDeep) {

        if (filterButtonClickFunc != null) {
            filterButtonClickFunc
                    .accept(filterText.trim().isEmpty() ? null : new FilterExpressionParser(filterText).parse(),
                            Boolean.valueOf(isDeep));
        }
    }

    public BiConsumer<Expression, Boolean> getFilterButtonClickFunc() {

        return filterButtonClickFunc;
    }

    void setFilterButtonClickFunc(
            BiConsumer<Expression, Boolean> filterButtonClickFunc) {

        this.filterButtonClickFunc = filterButtonClickFunc;
    }

    String getFilterText() {
        return this.filterText.getText();
    }

    void setFilterText(final String filterText) {
        this.filterText.setText(filterText);
        if (filterText != null && !filterText.isEmpty()) {
            this.filterButtonClick(filterText, false);
        }
    }
}
