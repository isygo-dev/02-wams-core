package eu.isygoit.jasycal;

import eu.isygoit.helper.BeanHelper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;

import java.util.Arrays;
import java.util.Objects;

/**
 * The type Event.
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class IEvent {

    private Property prodId;
    private Property version;
    private Property calScale;
    private Property method;
    private Property busyType;
    private Property clazz;
    private Property created;
    private Property description;
    private Property dtStart;
    private Property geo;
    private Property lastModified;
    private Property location;
    private Property organizer;
    private Property percentComplete;
    private Property priority;
    private Property dtStamp;
    private Property sequence;
    private Property status;
    private Property summary;
    private Property transp;
    private Property uid;
    private Property url;
    private Property recurrenceId;
    private Property completed;
    private Property due;
    private Property freeBusy;
    private Property tzId;
    private Property tzName;
    private Property tzOffsetFrom;
    private Property tzOffsetTo;
    private Property tzUrl;
    private Property action;
    private Property repeat;
    private Property trigger;
    private Property requestStatus;
    private Property dtEnd;
    private Property duration;
    private Property attach;
    private Property attendee;
    private Property categories;
    private Property comment;
    private Property contact;
    private Property exDate;
    private Property exRule;
    private Property relatedTo;
    private Property resourceType;
    private Property resources;
    private Property rDate;
    private Property rRule;
    private Property experimentalPrefix;
    private Property country;
    private Property extendedAddress;
    private Property locality;
    private Property name;
    private Property postalCode;
    private Property region;
    private Property streetAddress;
    private Property tel;
    private Property acknowledged;
    private Property proximity;
    private Property calendarAddress;
    private Property locationType;
    private Property participantType;
    private Property structuredData;
    private Property styledDescription;
    private Property tzUntil;
    private Property tzidAliasOf;

    private VEvent event;

    /**
     * Event event.
     *
     * @return the event
     */
    public IEvent event() {
        if (Objects.isNull(this.event)) {
            this.event = new VEvent();
            Arrays.stream(this.getClass().getDeclaredFields()).forEach(field -> {
                if (Property.class.isAssignableFrom(field.getType())) {
                    Property fieldValue = BeanHelper.callGetter(this, field.getName());
                    if (Objects.nonNull(fieldValue)) {
                        this.event.add(fieldValue);
                    }
                }
            });
        }
        return this;
    }
}
