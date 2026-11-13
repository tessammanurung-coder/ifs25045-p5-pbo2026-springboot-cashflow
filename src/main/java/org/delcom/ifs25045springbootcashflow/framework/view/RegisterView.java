package org.delcom.ifs25045springbootcashflow.framework.view;

import org.delcom.ifs25045springbootcashflow.domain.entity.User;
import org.delcom.ifs25045springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25045springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25045springbootcashflow.usecase.AuthUseCase;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route("register")
@PageTitle("Daftar | Cashflow")
public class RegisterView extends VerticalLayout implements BeforeEnterObserver {
    private final AuthUseCase auth;
    private final AuthSession session;
    private final TextField name = new TextField("Nama lengkap");
    private final TextField username = new TextField("Username");
    private final PasswordField password = new PasswordField("Password");
    private final PasswordField confirm = new PasswordField("Ulangi password");
    private final Button submit = new Button("Daftar");
    private final ProgressBar progress = LoadingAction.newBar();

    public RegisterView(AuthUseCase auth, AuthSession session) {
        this.auth = auth;
        this.session = session;
        addClassName("auth-view");
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        for (var field : new com.vaadin.flow.component.Component[] {name, username, password, confirm}) {
            ((com.vaadin.flow.component.HasSize) field).setWidthFull();
        }
        username.setHelperText("3-30 karakter: huruf, angka, titik, atau garis bawah");
        password.setHelperText("Minimal 6 karakter");
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submit.setWidthFull();
        submit.addClickShortcut(Key.ENTER);
        submit.addClickListener(e -> LoadingAction.run(progress, this::register, submit));

        Div footer = new Div(new Span("Sudah punya akun? "), new RouterLink("Masuk", LoginView.class));
        footer.addClassName("auth-footer");
        PageCard card = new PageCard("Buat akun baru", "Data keuangan Anda hanya dapat dilihat oleh Anda.")
                .with(name, username, password, confirm, progress, submit, footer);
        card.addClassName("auth-card");
        add(card);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (session.currentUser().isPresent()) {
            event.forwardTo(FinanceView.class);
        }
    }

    private void register() {
        if (!password.getValue().equals(confirm.getValue())) {
            Notifier.error("Konfirmasi password tidak sama");
            return;
        }
        try {
            User user = auth.register(username.getValue(), name.getValue(), password.getValue());
            session.login(user);
            Notifier.success("Akun dibuat. Selamat datang, " + user.getName() + "!");
            UI.getCurrent().navigate(FinanceView.class);
        } catch (DomainException ex) {
            Notifier.error(ex.getMessage());
        }
    }
}
