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

@Route("login")
@PageTitle("Masuk | Cashflow")
public class LoginView extends VerticalLayout implements BeforeEnterObserver {
    private final AuthUseCase auth;
    private final AuthSession session;
    private final TextField username = new TextField("Username");
    private final PasswordField password = new PasswordField("Password");
    private final Button submit = new Button("Masuk");
    private final ProgressBar progress = LoadingAction.newBar();

    public LoginView(AuthUseCase auth, AuthSession session) {
        this.auth = auth;
        this.session = session;
        addClassName("auth-view");
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        username.setId("login-username");
        username.setWidthFull();
        username.setAutofocus(true);
        username.setAutocomplete(com.vaadin.flow.component.textfield.Autocomplete.USERNAME);
        password.setId("login-password");
        password.setWidthFull();
        submit.setId("login-submit");
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submit.setWidthFull();
        submit.addClickShortcut(Key.ENTER);
        submit.addClickListener(e -> LoadingAction.run(progress, this::login, submit));

        Span hint = new Span("Belum punya akun? ");
        Div footer = new Div(hint, new RouterLink("Daftar", RegisterView.class));
        footer.addClassName("auth-footer");

        PageCard card = new PageCard("Masuk ke Cashflow", "Catat pemasukan dan pengeluaran Anda.")
                .with(username, password, progress, submit, footer);
        card.addClassName("auth-card");
        add(card);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (session.currentUser().isPresent()) {
            event.forwardTo(FinanceView.class);
        }
    }

    private void login() {
        try {
            User user = auth.login(username.getValue(), password.getValue());
            session.login(user);
            UI.getCurrent().navigate(FinanceView.class);
        } catch (DomainException ex) {
            Notifier.error(ex.getMessage());
        }
    }
}
