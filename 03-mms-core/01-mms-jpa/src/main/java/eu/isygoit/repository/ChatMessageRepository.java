package eu.isygoit.repository;

import eu.isygoit.annotation.IgnoreRepository;
import eu.isygoit.model.ChatMessage;
import org.springframework.data.cassandra.repository.AllowFiltering;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * The interface Chat message repository.
 */
@IgnoreRepository
public interface ChatMessageRepository extends CassandraRepository<ChatMessage, UUID> {

    /**
     * Find by receiver id list.
     *
     * @param receiverId the receiver id
     * @param pageable   the pageable
     * @return the list
     */
    @AllowFiltering
    List<ChatMessage> findByReceiverId(Long receiverId, Pageable pageable);

    /**
     * Find by receiver id or sender id order by date desc list.
     *
     * @param receiverId the receiver id
     * @param senderId   the sender id
     * @return the list
     */
    @AllowFiltering
    List<ChatMessage> findByReceiverIdOrSenderIdOrderByDateDesc(Long receiverId, Long senderId);

    @AllowFiltering
    @Query("SELECT * FROM t_chat_message WHERE (receiver_id = :receiverId AND sender_id = :senderId)")
    List<ChatMessage> findByReceiverIdAndSenderId(@Param("receiverId") Long receiverId,
                                                  @Param("senderId") Long senderId);
}
