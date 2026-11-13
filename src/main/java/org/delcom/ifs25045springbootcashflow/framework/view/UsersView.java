package org.delcom.ifs25045springbootcashflow.framework.view;

import org.delcom.ifs25045springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25045springbootcashflow.domain.entity.Role;
import org.delcom.ifs25045springbootcashflow.domain.entity.User;
import org.delcom.ifs25045springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25045springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25045springbootcashflow.usecase.UserUseCase;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import java.util.Comparator;
import java.util.Map;

/** Daftar seluruh pengguna (khusus ADMIN; penjagaan akses ada di MainLayout). */
@Route(value = "users", layout = MainLayout.class)
@PageTitle("Pengguna | Cashflow")
public class UsersView extends VerticalLayout implements BeforeEnterObserver, AdminOnlyView {
    private final UserUseCase users;
    private final FinancePresenter presenter;
    private final AuthSession session;
    private final Grid<User> grid = new Grid<>(User.class, false);

    public UsersView(UserUseCase users, FinancePresenter presenter, AuthSession session) {
        this.users = users;
        this.presenter = presenter;
        this.session = session;
        setPadding(true);
        GridStyles.apply(grid);

        grid.addColumn(User::getId).setHeader("ID").setSortable(true)
                .setComparator(Comparator.comparing(User::getId)).setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(u -> {
            Avatar avatar = new Avatar(u.getName());
            if (u.getPhotoPath() != null) {
                avatar.setImage(u.getPhotoPath());
            }
            HorizontalLayout row = new HorizontalLayout(avatar, new Span(u.getName()));
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            return row;
        }).setHeader("Nama").setSortable(true)
                .setComparator(Comparator.comparing(u -> u.getName().toLowerCase())).setFlexGrow(1);
        grid.addColumn(User::getUsername).setHeader("Username").setSortable(true)
                .setComparator(Comparator.comparing(User::getUsername)).setAutoWidth(true);
        grid.addComponentColumn(u -> GridStyles.badge(presenter.roleLabel(u.getRole()),
                u.getRole() == Role.ADMIN ? "admin" : "user")).setHeader("Role").setAutoWidth(true);
        grid.addColumn(u -> presenter.formatDateTime(u.getCreatedAt())).setHeader("Terdaftar").setAutoWidth(true);
        grid.addComponentColumn(u -> {
            Button view = new Button("Lihat transaksi", new Icon(VaadinIcon.LIST_SELECT), e ->
                    UI.getCurrent().navigate("user-transactions",
                            QueryParameters.simple(Map.of("userId", String.valueOf(u.getId())))));
            view.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            return view;
        }).setHeader("Aksi").setAutoWidth(true).setFlexGrow(0);

        Button reload = new Button("Muat ulang", new Icon(VaadinIcon.REFRESH), e -> refresh());
        add(new PageCard("Pengguna", "Seluruh akun terdaftar. Transaksi pengguna hanya dapat dilihat, tidak diubah.")
                .with(reload, grid));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        refresh();
    }

    private void refresh() {
        try {
            session.currentUser().ifPresent(actor -> grid.setItems(users.listUsers(actor)));
        } catch (DomainException ex) {
            Notifier.error(presenter.failureMessage(ex));
        }
    }
}
