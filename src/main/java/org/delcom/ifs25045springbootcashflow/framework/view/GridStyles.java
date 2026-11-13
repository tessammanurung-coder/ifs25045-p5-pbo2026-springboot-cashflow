package org.delcom.ifs25045springbootcashflow.framework.view;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;

/** Gaya Grid dan lencana yang dipakai bersama oleh semua halaman. */
public final class GridStyles {
    private GridStyles() {}

    public static void apply(Grid<?> grid) {
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER);
        grid.addClassName("cashflow-grid");
        grid.setAllRowsVisible(false);
        grid.setHeight("26rem");
    }

    /** Lencana kecil berwarna; variant: "income", "expense", "admin", atau "user". */
    public static Span badge(String text, String variant) {
        Span badge = new Span(text);
        badge.addClassNames("badge", "badge-" + variant);
        return badge;
    }

    public static Span moneyText(String text, boolean income) {
        Span span = new Span((income ? "+ " : "- ") + text);
        span.addClassNames("money", income ? "money-income" : "money-expense");
        return span;
    }
}
