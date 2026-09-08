package fr.soe.a3s.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import fr.soe.a3s.domain.repository.Repository;

class RepositoryUploadSettingsTest {

	@Test
	void usesFourConnectionsByDefault() {
		Repository repository = new Repository("test", null);

		assertEquals(Repository.DEFAULT_PARALLEL_UPLOAD_CONNECTIONS,
				repository.getParallelUploadConnections());
	}

	@Test
	void capsParallelUploadsAtTen() {
		Repository repository = new Repository("test", null);

		repository.setParallelUploadConnections(50);

		assertEquals(Repository.MAX_PARALLEL_UPLOAD_CONNECTIONS,
				repository.getParallelUploadConnections());
	}

	@Test
	void rejectsInvalidParallelUploadCount() {
		Repository repository = new Repository("test", null);

		assertThrows(IllegalArgumentException.class, () -> repository.setParallelUploadConnections(0));
	}
}
