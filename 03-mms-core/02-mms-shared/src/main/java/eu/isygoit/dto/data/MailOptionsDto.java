package eu.isygoit.dto.data;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Mail options dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class MailOptionsDto {

    /**
     * The Return delivered.
     */
    boolean returnDelivered;
    /**
     * The Return read.
     */
    boolean returnRead;
}
