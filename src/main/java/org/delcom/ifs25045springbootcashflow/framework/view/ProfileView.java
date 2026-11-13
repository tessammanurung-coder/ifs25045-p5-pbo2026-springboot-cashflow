package org.delcom.ifs25045springbootcashflow.framework.view;

import org.delcom.ifs25045springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25045springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25045springbootcashflow.domain.entity.User;
import org.delcom.ifs25045springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25045springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25045springbootcashflow.usecase.UserUseCase;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;

/** Profil: ubah nama, password, dan foto. */
@Route(value = "profile", layout = MainLayout.class)
@PageTitle("Profil | Cashflow")
public class ProfileView extends VerticalLayout implements BeforeEnterObserver {
    private final UserUseCase users;
    private final FileStorage storage;
    private final AuthSession session;
    private final FinancePresenter presenter;

    private final Avatar avatar = new Avatar();
    private final Span username = new Span();
    private final Span role = new Span();
    private final TextField name = new TextField("Nama lengkap");
    private final PasswordField currentPassword = new PasswordField("Password saat ini");
    private final PasswordField newPassword = new PasswordField("Password baru");
    private final PasswordField confirmPassword = new PasswordField("Ulangi password baru");
    private User user;

    public ProfileView(UserUseCase users, FileStorage storage, AuthSession session, FinancePresenter presenter) {
        this.users = users;
        this.storage = storage;
        this.session = session;
        this.presenter = presenter;
        setPadding(true);
        addClassName("profile-view");

        avatar.setHeight("5rem");
        avatar.setWidth("5rem");
        username.addClassName("profile-username");
        role.addClassName("muted");

        Upload photo = new Upload(UploadHandler.inMemory((metadata, bytes) -> changePhoto(metadata.fileName(), bytes)));
        photo.setAcceptedFileTypes("image/png", "image/jpeg", "image/gif", "image/webp");
        photo.setMaxFiles(1);
        photo.setMaxFileSize(FileStorage.MAX_BYTES);
        photo.setUploadButton(new Button("Ganti foto", new Icon(VaadinIcon.CAMERA)));
        photo.setDropAllowed(false);
        photo.addAllFinishedListener(e -> photo.clearFileList());

        VerticalLayout identity = new VerticalLayout(username, role, photo);
        identity.setPadding(false);
        identity.setSpacing(false);
        HorizontalLayout head = new HorizontalLayout(avatar, identity);
        head.setAlignItems(FlexComponent.Alignment.CENTER);

        name.setWidthFull();
        ProgressBar nameBar = LoadingAction.newBar();
        Button saveName = new Button("Simpan nama");
        saveName.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveName.addClickListener(e -> LoadingAction.run(nameBar, this::saveName, saveName));

        for (var f : new PasswordField[] {currentPassword, newPassword, confirmPassword}) {
            f.setWidthFull();
        }
        ProgressBar passBar = LoadingAction.newBar();
        Button savePassword = new Button("Ubah password");
        savePassword.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        savePassword.addClickListener(e -> LoadingAction.run(passBar, this::savePassword, savePassword));

        add(new PageCard("Profil", "Kelola identitas akun Anda.").with(head),
                new PageCard("Nama").with(name, nameBar, saveName),
                new PageCard("Password").with(currentPassword, newPassword, confirmPassword, passBar, savePassword));
        getChildren().forEach(c -> c.getElement().getStyle().set("max-width", "36rem"));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        user = session.currentUser().orElse(null);
        if (user != null) {
            show();
        }
    }

    private void show() {
        avatar.setName(user.getName());
        avatar.setImage(user.getPhotoPath());
        username.setText("@" + user.getUsername());
        role.setText(presenter.roleLabel(user.getRole()));
        name.setValue(user.getName());
    }

    private void apply(User updated) {
        user = updated;
        show();
        MainLayout.refreshFrom(this, updated);
    }

    private void saveName() {
        try {
            apply(users.updateProfile(user.getId(), name.getValue()));
            Notifier.success("Nama diperbarui");
        } catch (DomainException ex) {
            Notifier.error(presenter.failureMessage(ex));
        }
    }

    private void savePassword() {
        if (!newPassword.getValue().equals(confirmPassword.getValue())) {
            Notifier.error("Konfirmasi password baru tidak sama");
            return;
        }
        try {
            users.updatePassword(user.getId(), currentPassword.getValue(), newPassword.getValue());
            currentPassword.clear();
            newPassword.clear();
            confirmPassword.clear();
            Notifier.success("Password diubah");
        } catch (DomainException ex) {
            Notifier.error(presenter.failureMessage(ex));
        }
    }

    private void changePhoto(String fileName, byte[] data) {
        String newPath = null;
        try {
            String oldPath = user.getPhotoPath();
            newPath = storage.store("avatar", fileName, data);
            apply(users.updatePhoto(user.getId(), newPath));
            if (oldPath != null) {
                storage.delete(oldPath);
            }
            Notifier.success("Foto profil diperbarui");
        } catch (DomainException ex) {
            if (newPath != null) {
                storage.delete(newPath);
            }
            Notifier.error(presenter.failureMessage(ex));
        }
    }
}
