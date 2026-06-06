package com.rtsbuilding.rtsbuilding.client.screen;

import com.rtsbuilding.rtsbuilding.client.controller.ClientRtsController;
import com.rtsbuilding.rtsbuilding.client.screen.storage.LinkedStoragePanel;
import com.rtsbuilding.rtsbuilding.client.screen.topbar.TopBarPanel;
import com.rtsbuilding.rtsbuilding.client.screen.topbar.TopBarTypes;
import com.rtsbuilding.rtsbuilding.client.util.RtsClientUiUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

import static com.rtsbuilding.rtsbuilding.client.screen.BuilderScreenConstants.*;

/**
 * Handles rendering and click interactions for the storage link detail action
 * (the extended action bar that appears when hovering the LINK top-bar button).
 * <p>
 * Extracted from {@link BuilderScreen} to reduce screen-level complexity.
 * Owned and invoked by the screen's render/click dispatch methods.
 */
public final class StorageLinkDetailHandler {

    private final BuilderScreen screen;
    private final ClientRtsController controller;
    private final TopBarPanel topBarPanel;
    private final LinkedStoragePanel linkedStoragePanel;

    public StorageLinkDetailHandler(
            BuilderScreen screen,
            ClientRtsController controller,
            TopBarPanel topBarPanel,
            LinkedStoragePanel linkedStoragePanel) {
        this.screen = screen;
        this.controller = controller;
        this.topBarPanel = topBarPanel;
        this.linkedStoragePanel = linkedStoragePanel;
    }

    // ===== Render =====

    /**
     * Renders the storage link detail action button below the LINK top-bar button
     * when the LINK button (or the action button itself) is hovered.
     */
    public void render(GuiGraphics g, int mouseX, int mouseY) {
        if (this.screen.isGuideOpen() || this.screen.isInteractionWheelOpen()) {
            return;
        }
        TopBarTypes.TopBarButtonLayout linkButton = findTopBarButton(TopBarTypes.TopBarButtonId.LINK);
        if (linkButton == null || !isVisible(mouseX, mouseY, linkButton)) {
            return;
        }
        String label = screen.text("screen.rtsbuilding.storage_links.action");
        int w = actionW(linkButton, label);
        int x = actionX(linkButton, label);
        int y = actionY();
        boolean hovered = inside(mouseX, mouseY, x, y, w, STORAGE_LINK_DETAIL_ACTION_H);
        RtsClientUiUtil.drawPanelFrame(g, x, y, w, STORAGE_LINK_DETAIL_ACTION_H,
                hovered ? 0xFF26394A : 0xF817212D,
                hovered ? 0xFFB7D2EC : 0xFF6C839A,
                0xFF0D1117);
        RtsClientUiUtil.drawCenteredStringNoShadow(g, screen.font(),
                screen.trimToWidth(label, Math.max(8, w - 8)), x + w / 2, y + 4, 0xFFF4FAFF);
    }

    // ===== Click =====

    /**
     * Handles click on the storage link detail action button.
     *
     * @return true if the click was consumed
     */
    public boolean handleClick(double mouseX, double mouseY) {
        TopBarTypes.TopBarButtonLayout linkButton = findTopBarButton(TopBarTypes.TopBarButtonId.LINK);
        if (linkButton == null) {
            return false;
        }
        String label = screen.text("screen.rtsbuilding.storage_links.action");
        int w = actionW(linkButton, label);
        int x = actionX(linkButton, label);
        int y = actionY();
        if (!inside(mouseX, mouseY, x, y, w, STORAGE_LINK_DETAIL_ACTION_H)) {
            return false;
        }
        this.screen.closeGearMenu();
        this.linkedStoragePanel.openNear(x, y + STORAGE_LINK_DETAIL_ACTION_H + 2);
        return true;
    }

    // ===== Status tooltip =====

    /**
     * Renders a tooltip for the storage link status text (row 2 of the status bar).
     *
     * @return true if the tooltip was rendered (mouse was over the status text)
     */
    public boolean renderStatusTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (mouseY < 42 || mouseY > 56) {
            return false;
        }
        String linkedText = this.controller.isStorageLinked()
                ? screen.text("screen.rtsbuilding.status.storage_linked", this.controller.getLinkedStorageName())
                : screen.text("screen.rtsbuilding.status.storage_not_linked");
        int linkedW = Math.min(screen.font().width(linkedText), Math.max(20, screen.width - 16));
        if (!inside(mouseX, mouseY, 8, 42, linkedW, 14)) {
            return false;
        }
        int linkedCount = this.controller.getLinkedStoragePositions().size();
        if (this.controller.isStorageLinked()) {
            g.renderComponentTooltip(screen.font(), List.of(
                    Component.translatable("screen.rtsbuilding.tooltip.storage_linked_short",
                            this.controller.getLinkedStorageName(), linkedCount),
                    Component.translatable("screen.rtsbuilding.tooltip.storage_unbind_short")),
                    mouseX, mouseY);
        } else {
            g.renderTooltip(screen.font(), Component.translatable("screen.rtsbuilding.tooltip.storage_unlinked_short"), mouseX, mouseY);
        }
        return true;
    }

    // ===== Private helpers =====

    private TopBarTypes.TopBarButtonLayout findTopBarButton(TopBarTypes.TopBarButtonId id) {
        for (TopBarTypes.TopBarButtonLayout button : this.topBarPanel.buildTopBarButtonLayouts()) {
            if (button.id() == id) {
                return button;
            }
        }
        return null;
    }

    private boolean isVisible(int mouseX, int mouseY, TopBarTypes.TopBarButtonLayout linkButton) {
        String label = screen.text("screen.rtsbuilding.storage_links.action");
        int w = actionW(linkButton, label);
        int x = actionX(linkButton, label);
        int y = actionY();
        int bridgeX = Math.min(linkButton.x(), x);
        int bridgeRight = Math.max(linkButton.x() + linkButton.width(), x + w);
        int bridgeY = 4 + TOP_BUTTON_H;
        int bridgeH = Math.max(0, y - bridgeY);
        return inside(mouseX, mouseY, linkButton.x(), 4, linkButton.width(), TOP_BUTTON_H)
                || inside(mouseX, mouseY, x, y, w, STORAGE_LINK_DETAIL_ACTION_H)
                || inside(mouseX, mouseY, bridgeX, bridgeY, bridgeRight - bridgeX, bridgeH);
    }

    private int actionX(TopBarTypes.TopBarButtonLayout linkButton, String label) {
        int w = actionW(linkButton, label);
        int centered = linkButton.x() + linkButton.width() / 2 - w / 2;
        return Mth.clamp(centered, 4, Math.max(4, screen.width - w - 4));
    }

    private int actionY() {
        return TOP_H + 2;
    }

    private int actionW(TopBarTypes.TopBarButtonLayout linkButton, String label) {
        int desired = Math.max(linkButton.width(), screen.font().width(label) + 12);
        return Math.min(desired, Math.max(40, screen.width - 8));
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }
}
