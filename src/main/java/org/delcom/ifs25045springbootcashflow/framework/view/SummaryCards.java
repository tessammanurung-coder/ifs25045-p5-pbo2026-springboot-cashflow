package org.delcom.ifs25045springbootcashflow.framework.view;

import org.delcom.ifs25045springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25045springbootcashflow.usecase.FinanceUseCase;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

/** Empat kartu ringkasan: total transaksi, pemasukan, pengeluaran, dan saldo. */
public class SummaryCards extends Div {
    private final FinancePresenter presenter;
    private final Span count = new Span();
    private final Span income = new Span();
    private final Span expense = new Span();
    private final Span balance = new Span();

    public SummaryCards(FinancePresenter presenter) {
        this.presenter = presenter;
        addClassName("summary-cards");
        add(card("Total transaksi", count, "card-count"),
                card("Pemasukan", income, "card-income"),
                card("Pengeluaran", expense, "card-expense"),
                card("Saldo", balance, "card-balance"));
        update(new FinanceUseCase.Summary(0, 0, 0, 0));
    }

    public void update(FinanceUseCase.Summary summary) {
        count.setText(String.valueOf(summary.count()));
        income.setText(presenter.formatMoney(summary.income()));
        expense.setText(presenter.formatMoney(summary.expense()));
        balance.setText(presenter.formatMoney(summary.balance()));
        balance.removeClassNames("negative");
        if (summary.balance() < 0) {
            balance.addClassName("negative");
        }
    }

    private static Div card(String label, Span value, String cssClass) {
        Span caption = new Span(label);
        caption.addClassName("summary-label");
        value.addClassName("summary-value");
        Div card = new Div(caption, value);
        card.addClassNames("summary-card", cssClass);
        return card;
    }
}
