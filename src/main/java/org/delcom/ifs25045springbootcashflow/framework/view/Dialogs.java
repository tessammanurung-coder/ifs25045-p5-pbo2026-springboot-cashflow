package org.delcom.ifs25045springbootcashflow.framework.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

/** Pabrik dialog modal: header biru (#2563EB), footer abu-abu (#F3F4F6) dengan tombol di kanan. */
final class Dialogs {
    private Dialogs() {}

    static Dialog create(String title) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(title);
        dialog.setModal(true);
        dialog.setDraggable(false);
        dialog.setCloseOnOutsideClick(false);
        dialog.setWidth("min(92vw, 30rem)");
        dialog.addClassName("cashflow-dialog");
        Button close = new Button(new Icon(VaadinIcon.CLOSE), e -> dialog.close());
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        close.addClassName("dialog-close");
        close.setAriaLabel("Tutup");
        dialog.getHeader().add(close);
        return dialog;
    }

    static void confirm(String title, String message, String confirmLabel, Runnable onConfirm) {
        Dialog dialog = create(title);
        dialog.add(new Span(message));
        Button cancel = new Button("Batal", e -> dialog.close());
        Button confirmButton = new Button(confirmLabel, e -> {
            dialog.close();
            onConfirm.run();
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        dialog.getFooter().add(cancel, confirmButton);
        dialog.open();
    }
}
