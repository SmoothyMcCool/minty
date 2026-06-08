package tom.document.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tom.document.model.MintyDocSegment;

public interface DocumentSegmentRepository extends JpaRepository<MintyDocSegment, UUID> {

	/**
	 * Retrieves all segments belonging to a specific document, automatically sorted
	 * by their sequence order.
	 * 
	 * @param docId The UUID of the parent MintyDoc
	 * @return A list of segments in the correct order for reconstruction
	 */
	List<MintyDocSegment> findByDocument_IdOrderBySequenceOrderAsc(UUID documentId);

	/**
	 * Retrieves all segments for a document without a specific order guarantee.
	 * 
	 * @param docId The UUID of the parent MintyDoc
	 * @return A list of segments
	 */
	List<MintyDocSegment> findByDocument_Id(UUID documentId);

	/**
	 * Deletes all segments associated with a specific document. Useful for manual
	 * cleanup if not relying solely on DB-level CASCADE.
	 * 
	 * @param docId The UUID of the parent MintyDoc
	 */
	void deleteByDocument_Id(UUID documentId);

	List<MintyDocSegment> findByDocument_IdAndSequenceOrderIn(UUID documentId, List<Integer> sequenceOrders);
}
