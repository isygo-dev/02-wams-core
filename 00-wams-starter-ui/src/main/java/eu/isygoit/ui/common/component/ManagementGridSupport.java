package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.StreamResource;
import eu.isygoit.async.ContextAwareAsync;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Shared table-mode support for management views.
 *
 * <p>The actions column re-uses the action buttons of the card rendered for the same
 * item, so cards and table rows always offer the same actions. In the table the first
 * {@link #MAX_VISIBLE_ACTIONS} actions are laid out horizontally; any further ones move
 * into a "…" menu, whose entries trigger the original button (same click listeners,
 * same enabled state).</p>
 */
public final class ManagementGridSupport {

    /**
     * Number of actions shown as icon buttons before the rest go into the "…" menu.
     */
    public static final int MAX_VISIBLE_ACTIONS = 3;

    /**
     * Key of the leading image column, so a view that already added its own photo column is not given a second one.
     */
    public static final String IMAGE_COLUMN_KEY = "photo";

    private ManagementGridSupport() {
    }

    /**
     * Round image column for items that carry an image (see {@code IImageEntity} / {@code IImageUploadDto}).
     * The image is downloaded off the UI thread and set on the avatar when it arrives; bytes are kept in
     * {@code cache} (key = image path, empty array = no image) so sorting or paging never fetches them twice.
     * Until it arrives, or when the item has no image, the avatar shows the initials of {@code labelOf}.
     */
    public static <T> Grid.Column<T> addImageColumn(Grid<T> grid, Function<T, String> labelOf,
                                                    Function<T, String> imagePathOf,
                                                    Function<T, byte[]> downloader,
                                                    Map<String, byte[]> cache) {
        return grid.addComponentColumn(item -> {
            String label = labelOf.apply(item);
            Avatar avatar = new Avatar(label == null ? "" : label);
            avatar.addThemeVariants(AvatarVariant.LUMO_LARGE);
            avatar.addClassName("management-grid__photo");
            String path = imagePathOf.apply(item);
            if (path != null && !path.isBlank()) {
                bindImage(avatar, item, path, downloader, cache);
            }
            return avatar;
        }).setKey(IMAGE_COLUMN_KEY).setHeader("").setWidth("64px").setFlexGrow(0);
    }

    private static <T> void bindImage(Avatar avatar, T item, String path, Function<T, byte[]> downloader,
                                      Map<String, byte[]> cache) {
        byte[] known = cache.get(path);
        if (known != null) {
            showImage(avatar, path, known);
            return;
        }
        UI ui = UI.getCurrent();
        if (ui == null) {
            return;
        }
        ContextAwareAsync.supplyAsync(() -> {
            try {
                byte[] bytes = downloader.apply(item);
                return bytes == null ? new byte[0] : bytes;
            } catch (Exception ex) {
                return new byte[0];
            }
        }).whenComplete((bytes, error) -> {
            byte[] result = error == null ? bytes : new byte[0];
            cache.put(path, result);
            ui.access(() -> showImage(avatar, path, result));
        });
    }

    private static void showImage(Avatar avatar, String path, byte[] bytes) {
        if (bytes.length > 0) {
            avatar.setImageResource(new StreamResource("image_" + Math.abs(path.hashCode()) + ".jpg",
                    () -> new ByteArrayInputStream(bytes)));
        }
    }

    public static <T> Grid.Column<T> addCardActionsColumn(Grid<T> grid,
                                                          Function<T, Component> cardRenderer) {
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER);
        grid.setAllRowsVisible(true);
        grid.setWidthFull();
        grid.addClassName("management-grid");
        return grid.addComponentColumn(item -> {
            Component actionBar = findByClass(cardRenderer.apply(item), "wams-card__button-bar");
            List<Component> actions = new ArrayList<>();
            if (actionBar != null) {
                actions.addAll(actionBar.getChildren().toList());
            }
            return buildActionsCell(actions);
        }).setHeader("").setAutoWidth(true).setFlexGrow(0);
    }

    /**
     * Lays the given actions out horizontally, keeping at most {@link #MAX_VISIBLE_ACTIONS}
     * visible and moving the remaining buttons into a "…" menu.
     */
    public static Div buildActionsCell(List<? extends Component> actions) {
        Div cell = new Div();
        cell.addClassName("management-grid__actions");

        List<Component> shown = new ArrayList<>();
        for (Component action : actions) {
            if (action.isVisible()) {
                shown.add(action);
            }
        }

        List<Component> inline = new ArrayList<>();
        List<Button> overflow = new ArrayList<>();
        for (int i = 0; i < shown.size(); i++) {
            Component action = shown.get(i);
            if (i < MAX_VISIBLE_ACTIONS || !(action instanceof Button button)) {
                inline.add(action);
            } else {
                overflow.add(button);
            }
        }

        for (Component action : inline) {
            action.getElement().removeFromParent();
            cell.add(action);
        }
        if (!overflow.isEmpty()) {
            cell.add(buildOverflowMenu(overflow));
        }
        return cell;
    }

    private static MenuBar buildOverflowMenu(List<Button> overflow) {
        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE, MenuBarVariant.LUMO_SMALL);
        menuBar.addClassName("management-grid__more");

        Icon dots = VaadinIcon.ELLIPSIS_DOTS_H.create();
        dots.addClassName("management-grid__more-icon");
        MenuItem root = menuBar.addItem(dots);
        root.getElement().setAttribute("aria-label", "…");
        SubMenu subMenu = root.getSubMenu();

        for (Button button : overflow) {
            Div content = new Div();
            content.addClassName("management-grid__menu-item");
            if (button.getElement().getThemeList().contains("error")) {
                content.addClassName("management-grid__menu-item--danger");
            }
            Icon icon = copyIcon(button);
            if (icon != null) {
                content.add(icon);
            }
            String label = labelOf(button);
            if (!label.isBlank()) {
                content.add(new Span(label));
            }
            MenuItem item = subMenu.addItem(content, e -> button.click());
            item.setEnabled(button.isEnabled());
        }
        return menuBar;
    }

    private static Icon copyIcon(Button button) {
        Component source = button.getIcon();
        if (source instanceof Icon icon) {
            String name = icon.getElement().getAttribute("icon");
            if (name != null && !name.isBlank()) {
                return new Icon(name);
            }
        }
        return null;
    }

    /**
     * Best available human label of an icon button: its text, tooltip, title or aria-label.
     */
    private static String labelOf(Button button) {
        String text = button.getText();
        if (text != null && !text.isBlank()) {
            return text;
        }
        for (Element child : button.getElement().getChildren().toList()) {
            if ("vaadin-tooltip".equals(child.getTag())) {
                String tip = child.getProperty("text");
                if (tip != null && !tip.isBlank()) {
                    return tip;
                }
            }
        }
        for (String attribute : new String[]{"title", "aria-label"}) {
            String value = button.getElement().getAttribute(attribute);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static Component findByClass(Component root, String cssClass) {
        if (root.getElement().getClassList().contains(cssClass)) {
            return root;
        }
        for (Component child : root.getChildren().toList()) {
            Component found = findByClass(child, cssClass);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
