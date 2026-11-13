package org.delcom.ifs25045springbootcashflow.framework.view;

import org.delcom.ifs25045springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25045springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25045springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25045springbootcashflow.domain.entity.User;
import org.delcom.ifs25045springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25045springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25045springbootcashflow.usecase.FinanceUseCase;
import org.delcom.ifs25045springbootcashflow.usecase.UserUseCase;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.List;

/** Peninjauan transaksi pengguna lain, read-only (tanpa tombol hapus/tambah). Khusus ADMIN. */
@Route(value = "user-transactions", layout = MainLayout.class)
@PageTitle("Transaksi pengguna | Cashflow")
public class UserTransactionsView extends VerticalLayout implements BeforeEnterObserver, AdminOnlyView {
    private final FinanceUseCase finance;
    private final UserUseCase users;
    private final FinancePresenter presenter;
    private final AuthSession session;

    private final ComboBox<User> picker = new ComboBox<>("Pengguna");
    private final TextField search = new TextField();
    private final ComboBox<SortOption> sort = new ComboBox<>();
    private final SummaryCards summary;
    private final TransactionGrid grid;
    private User actor;

    public UserTransactionsView(FinanceUseCase finance, UserUseCase users, FinancePresenter presenter,
                                AuthSession session) {
        this.finance = finance;
        this.users = users;
        this.presenter = presenter;
        this.session = session;
        setPadding(true);

        summary = new SummaryCards(presenter);
        grid = new TransactionGrid(presenter, null);

        picker.setItemLabelGenerator(u -> u.getName() + " (@" + u.getUsername() + ")");
        picker.setWidth("22rem");
        picker.addValueChangeListener(e -> refresh());
        search.setPlaceholder("Cari keterangan...");
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.setClearButtonVisible(true);
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.setValueChangeTimeout(300);
        search.addValueChangeListener(e -> refresh());
        sort.setItems(SortOption.values());
        sort.setItemLabelGenerator(presenter::sortLabel);
        sort.setValue(SortOption.NEWEST);
        sort.setAllowCustomValue(false);
        sort.setClearButtonVisible(false);
        sort.setAriaLabel("Urutkan");
        sort.addValueChangeListener(e -> refresh());

        HorizontalLayout toolbar = new HorizontalLayout(picker, search, sort);
        toolbar.setAlignItems(FlexComponent.Alignment.END);
        toolbar.setFlexWrap(true);
        toolbar.addClassName("toolbar");
        Div notice = new Div(new Span("Mode baca saja: transaksi pengguna lain tidak dapat diubah atau dihapus."));
        notice.addClassName("readonly-notice");
        add(notice, toolbar, summary, grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        actor = session.currentUser().orElse(null);
        if (actor == null) {
            return;
        }
        try {
            picker.setItems(users.listUsers(actor));
            event.getLocation().getQueryParameters().getSingleParameter("userId").ifPresent(raw -> {
                Integer id = finance.parseId(raw);
                picker.setValue(users.getUser(id));
            });
        } catch (DomainException ex) {
            Notifier.error(presenter.failureMessage(ex));
        }
    }

    private void refresh() {
        User target = picker.getValue();
        if (actor == null || target == null) {
            grid.setItems(List.of());
            summary.update(new FinanceUseCase.Summary(0, 0, 0, 0));
            return;
        }
        try {
            List<Transaction> items = finance.getTransactionsForAdmin(actor, target.getId(),
                    search.getValue(), sort.getValue());
            grid.setItems(items);
            summary.update(finance.summarize(finance.getTransactionsForAdmin(actor, target.getId(), null, null)));
        } catch (DomainException ex) {
            Notifier.error(presenter.failureMessage(ex));
        }
    }
}
