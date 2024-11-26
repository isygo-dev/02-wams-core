package eu.isygoit.repository;

import eu.isygoit.model.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * The interface Chat message repository.
 */
public interface ChatMessageRepository extends JpaPagingAndSortingRepository<ChatMessage, Long> {

    /**
     * Find by receiver id list.
     *
     * @param receiverId the receiver id
     * @param pageable   the pageable
     * @return the list
     */
    List<ChatMessage> findByReceiverId(Long receiverId, Pageable pageable);

    /**
     * Find by receiver id or sender id order by date desc list.
     *
     * @param receiverId the receiver id
     * @param SenderId   the sender id
     * @return the list
     */
    List<ChatMessage> findByReceiverIdOrSenderIdOrderByDateDesc(Long receiverId, Long SenderId);

    /**
     * Find chat stack list.
     *
     * @param receiverId the receiver id
     * @param SenderId   the sender id
     * @return the list
     */
    @Query("select cm from ChatMessage cm where (cm.receiverId = :receiverId and cm.senderId = :SenderId) or (cm.receiverId = :SenderId and cm.senderId = :receiverId) order by cm.date asc ")
    List<ChatMessage> findChatStack(@Param("receiverId") Long receiverId,
                                    @Param("SenderId") Long SenderId);
}
