package org.delcom.ifs25045springbootcashflow.framework.view;

import org.delcom.ifs25045springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25045springbootcashflow.domain.entity.User;
import org.delcom.ifs25045springbootcashflow.framework.auth.AuthSession;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import java.util.Optional;

/** Kerangka halaman terproteksi: sidebar, menu avatar (Profil / Keluar), dan penjaga rute. */
public class MainLayout extends AppLayout implements BeforeEnterObserver {
    private final AuthSession session;
    private final FinancePresenter presenter;

    public MainLayout(AuthSession session, FinancePresenter presenter) {
        this.session = session;
        this.presenter = presenter;
        setPrimarySection(Section.DRAWER);
        addClassName("main-layout");
    }

    /** Belum login: ke /login. USER membuka halaman admin: ke /. */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<User> current = session.currentUser();
        if (current.isEmpty()) {
            event.forwardTo(LoginView.class);
            return;
        }
        User user = current.get();
        if (AdminOnlyView.class.isAssignableFrom(event.getNavigationTarget()) && !user.isAdmin()) {
            event.forwardTo(FinanceView.class);
            return;
        }
        render(user);
    }

    /** Menggambar ulang sidebar dan header (dipanggil juga setelah profil berubah). */
    public void render(User user) {
        Span brand = new Span("Cashflow");
        brand.addClassName("brand");
        Icon logo = VaadinIcon.WALLET.create();
        logo.addClassName("brand-icon");
        HorizontalLayout brandRow = new HorizontalLayout(logo, brand);
        brandRow.setAlignItems(FlexComponent.Alignment.CENTER);
        brandRow.addClassName("drawer-brand");

        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Cashflow", FinanceView.class, VaadinIcon.WALLET.create()));
        nav.addItem(new SideNavItem("Profil", ProfileView.class, VaadinIcon.USER.create()));
        if (user.isAdmin()) {
            nav.addItem(new SideNavItem("Pengguna", UsersView.class, VaadinIcon.USERS.create()));
            nav.addItem(new SideNavItem("Transaksi pengguna", UserTransactionsView.class,
                    VaadinIcon.LIST_SELECT.create()));
        }
        Div drawer = new Div(brandRow, nav);
        drawer.addClassName("drawer-content");
        addToDrawer(drawer);

        H1 title = new H1("Catatan Keuangan");
        title.addClassName("app-title");
        Div spacer = new Div();
        spacer.getStyle().set("flex", "1");
        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), title, spacer, userMenu(user));
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.addClassName("app-header");
        addToNavbar(header);
    }

    private Component userMenu(User user) {
        Avatar avatar = new Avatar(user.getName());
        if (user.getPhotoPath() != null) {
            avatar.setImage(user.getPhotoPath());
        }
        Span name = new Span(user.getName());
        name.addClassName("menu-user-name");
        Span role = new Span(presenter.roleLabel(user.getRole()));
        role.addClassName("menu-user-role");
        Div text = new Div(name, role);
        text.addClassName("menu-user-text");
        HorizontalLayout trigger = new HorizontalLayout(avatar, text);
        trigger.setAlignItems(FlexComponent.Alignment.CENTER);
        trigger.setSpacing(true);

        MenuBar menu = new MenuBar();
        menu.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        var item = menu.addItem(trigger);
        item.getSubMenu().addItem("Profil", e -> UI.getCurrent().navigate(ProfileView.class));
        item.getSubMenu().addItem("Keluar", e -> {
            session.logout();
            UI.getCurrent().navigate(LoginView.class);
        });
        return menu;
    }

    /** Dipanggil view anak (mis. ProfileView) agar avatar/nama di header ikut diperbarui. */
    static void refreshFrom(Component child, User user) {
        Optional<Component> parent = child.getParent();
        while (parent.isPresent()) {
            if (parent.get() instanceof MainLayout layout) {
                layout.render(user);
                return;
            }
            parent = parent.get().getParent();
        }
    }
}
