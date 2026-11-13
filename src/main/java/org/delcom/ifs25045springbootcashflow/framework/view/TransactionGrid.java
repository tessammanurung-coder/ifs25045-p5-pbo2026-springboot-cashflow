package org.delcom.ifs25045springbootcashflow.framework.view;

import org.delcom.ifs25045springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25045springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25045springbootcashflow.domain.entity.TransactionType;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import java.util.Comparator;
import java.util.function.Consumer;

/** Grid transaksi bersama. onDelete == null berarti mode baca-saja (tanpa kolom tindakan). */
public class TransactionGrid extends Grid<Transaction> {

    public TransactionGrid(FinancePresenter presenter, Consumer<Transaction> onDelete) {
        super(Transaction.class, false);
        GridStyles.apply(this);

        addColumn(Transaction::getId).setHeader("ID").setSortable(true)
                .setComparator(Comparator.comparing(Transaction::getId)).setAutoWidth(true).setFlexGrow(0);
        addColumn(Transaction::getDescription).setHeader("Keterangan").setSortable(true)
                .setComparator(Comparator.comparing(t -> t.getDescription().toLowerCase()))
                .setFlexGrow(1).setResizable(true);
        addComponentColumn(t -> GridStyles.moneyText(presenter.formatMoney(t.getAmount()),
                t.getType() == TransactionType.INCOME))
                .setHeader("Jumlah").setSortable(true)
                .setComparator(Comparator.comparingLong(Transaction::getAmount)).setAutoWidth(true);
        addComponentColumn(t -> GridStyles.badge(presenter.typeLabel(t.getType()),
                t.getType() == TransactionType.INCOME ? "income" : "expense"))
                .setHeader("Tipe").setSortable(true)
                .setComparator(Comparator.comparing(Transaction::getType)).setAutoWidth(true);
        addComponentColumn(t -> proof(t, presenter)).setHeader("Bukti").setAutoWidth(true);
        addColumn(t -> presenter.formatDateTime(t.getCreatedAt())).setHeader("Waktu").setSortable(true)
                .setComparator(Comparator.comparing(Transaction::getCreatedAt)).setAutoWidth(true);

        if (onDelete != null) {
            addComponentColumn(t -> {
                Button delete = new Button(new Icon(VaadinIcon.TRASH), e -> onDelete.accept(t));
                delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR,
                        ButtonVariant.LUMO_SMALL);
                delete.setAriaLabel("Hapus transaksi " + t.getId());
                delete.setTooltipText("Hapus");
                return delete;
            }).setHeader("Aksi").setAutoWidth(true).setFlexGrow(0);
        }
    }

    private static com.vaadin.flow.component.Component proof(Transaction t, FinancePresenter presenter) {
        if (t.getProofPath() == null) {
            Span none = new Span("-");
            none.addClassName("muted");
            return none;
        }
        Image thumb = new Image(t.getProofPath(), "Bukti transaksi " + t.getId());
        thumb.addClassName("proof-thumb");
        thumb.addClickListener(e -> new ImagePreviewDialog(
                "Bukti: " + t.getDescription() + " (" + presenter.formatMoney(t.getAmount()) + ")",
                t.getProofPath()).open());
        return thumb;
    }
}
