package org.delcom.ifs25045springbootcashflow.framework.view;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

/** Notifikasi seragam di pojok kanan atas. */
final class Notifier {
    private Notifier() {}

    static void success(String message) {
        show(message, NotificationVariant.LUMO_SUCCESS, 3500);
    }

    static void error(String message) {
        show(message, NotificationVariant.LUMO_ERROR, 6000);
    }

    private static void show(String message, NotificationVariant variant, int durationMs) {
        Notification notification = new Notification(message, durationMs, Notification.Position.TOP_END);
        notification.addThemeVariants(variant);
        notification.open();
    }
}
