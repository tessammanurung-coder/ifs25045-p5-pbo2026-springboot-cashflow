package org.delcom.ifs25045springbootcashflow.framework.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

/** Modal pratinjau gambar: klik gambar untuk toggle zoom, atau pakai tombol perkecil/reset/perbesar. */
public class ImagePreviewDialog extends Dialog {
    private static final double MIN_SCALE = 1.0;
    private static final double MAX_SCALE = 4.0;
    private static final double STEP = 0.5;
    private static final double CLICK_ZOOM = 2.0;

    private final Image image;
    private double scale = MIN_SCALE;

    public ImagePreviewDialog(String title, String src) {
        setHeaderTitle(title);
        setModal(true);
        setCloseOnOutsideClick(true);
        setWidth("min(94vw, 56rem)");
        addClassName("cashflow-dialog");
        addClassName("image-preview-dialog");

        Button closeIcon = new Button(new Icon(VaadinIcon.CLOSE), e -> close());
        closeIcon.addClassName("dialog-close");
        closeIcon.setAriaLabel("Tutup");
        getHeader().add(closeIcon);

        image = new Image(src, title);
        image.addClassName("preview-image");
        image.addClickListener(e -> setScale(scale > MIN_SCALE ? MIN_SCALE : CLICK_ZOOM));
        Div viewport = new Div(image);
        viewport.addClassName("preview-viewport");
        add(viewport);

        Button zoomOut = new Button("Perkecil", new Icon(VaadinIcon.SEARCH_MINUS), e -> setScale(scale - STEP));
        Button reset = new Button("Reset", e -> setScale(MIN_SCALE));
        Button zoomIn = new Button("Perbesar", new Icon(VaadinIcon.SEARCH_PLUS), e -> setScale(scale + STEP));
        Button done = new Button("Tutup", e -> close());
        getFooter().add(zoomOut, reset, zoomIn, done);
        setScale(MIN_SCALE);
    }

    private void setScale(double value) {
        scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, value));
        image.setWidth((int) Math.round(scale * 100) + "%");
        image.getStyle().set("cursor", scale > MIN_SCALE ? "zoom-out" : "zoom-in");
    }
}
