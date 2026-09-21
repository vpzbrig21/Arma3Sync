package fr.soe.a3s.ui.main.tasks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import fr.soe.a3s.dto.RepositoryDTO;

class TaskCheckRepositoriesTest {

	@Test
	void notifiesOnlyOnceForTheSameRemoteRevision() {
		RepositoryDTO repository = repository(true, false);
		Map<String, Integer> notified = new HashMap<String, Integer>();

		assertTrue(TaskCheckRepositories.shouldNotify(repository, 12, notified));
		assertFalse(TaskCheckRepositories.shouldNotify(repository, 12, notified));
		assertTrue(TaskCheckRepositories.shouldNotify(repository, 13, notified));
	}

	@Test
	void automaticUpdatesDoNotCreateAStalePendingNotification() {
		RepositoryDTO repository = repository(true, true);
		assertFalse(TaskCheckRepositories.shouldNotify(repository, 12, new HashMap<String, Integer>()));
	}

	@Test
	void notificationsRemainAvailableWhenRemoteRevisionCannotBeRead() {
		RepositoryDTO repository = repository(true, false);
		assertTrue(TaskCheckRepositories.shouldNotify(repository, null, new HashMap<String, Integer>()));
	}

	private static RepositoryDTO repository(boolean notify, boolean auto) {
		RepositoryDTO repository = new RepositoryDTO();
		repository.setName("test-repository");
		repository.setNotify(notify);
		repository.setAuto(auto);
		return repository;
	}
}
