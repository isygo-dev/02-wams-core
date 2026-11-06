package eu.isygoit.ui.common.card;

import com.vaadin.flow.component.Component;
import eu.isygoit.model.ICodeAssignable;

/**
 * Base class of every card that shows an object carrying a business code
 * ({@link ICodeAssignable}: students, parents, rooms, tenants, ...).
 *
 * <p>The code is rendered once, at the top of the card above the title, as the shared
 * code badge (monospace code + copy-to-clipboard icon, see {@link BusinessCodeBadge}), so
 * subclasses no longer wire it by hand and every card looks and behaves the same:</p>
 *
 * <pre>{@code
 * public class RoomCard extends BaseCodeAssignableCard<RoomView, RoomService, RoomMinDto> {
 *     public RoomCard(RoomView view, RoomService service, RoomMinDto room) {
 *         super(view, service, room);   // the object whose code is shown
 *         initCard();
 *     }
 *     ...
 * }
 * }</pre>
 *
 * <p>The code is read from the object through {@link ICodeAssignable#getCode()}. A card whose
 * object is a DTO that does not implement {@code ICodeAssignable}, or whose displayed code is
 * not {@code getCode()}, overrides {@link #codeOf(Object)} (one line).</p>
 *
 * @param <V> the parent view type
 * @param <S> the service type used by this card
 * @param <T> the displayed object (entity or DTO) that carries the code
 */
public abstract class BaseCodeAssignableCard<V extends Component, S, T> extends BaseCard<V, S> {

    /**
     * The object shown by this card. Subclasses may reassign it after an update, then call
     * {@link #refreshBusinessCode()}.
     */
    protected T item;

    protected BaseCodeAssignableCard(V parentView, S objectService, T item) {
        super(parentView, objectService);
        this.item = item;
    }

    /**
     * Code of the displayed object: {@link ICodeAssignable#getCode()} when it implements the
     * interface, otherwise {@code null} (no badge). Override for objects that expose their code
     * differently.
     */
    protected String codeOf(T object) {
        return object instanceof ICodeAssignable assignable ? assignable.getCode() : null;
    }

    /**
     * Feeds the shared code badge shown above the card title.
     */
    @Override
    protected String businessCode() {
        return item == null ? null : codeOf(item);
    }
}
