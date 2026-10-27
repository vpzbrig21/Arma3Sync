package fr.soe.a3s.service;

import java.util.Collections;
import java.util.List;

import fr.soe.a3s.domain.repository.DiscordWebhookSettings;
import fr.soe.a3s.domain.repository.WorkshopMetadataCache;
import fr.soe.a3s.dto.ChangelogDTO;
import fr.soe.a3s.dto.RepositoryDTO;
import fr.soe.a3s.dto.ServerInfoDTO;

/** Publishes the most recently built repository changelog to its configured webhook. */
public final class DiscordChangelogPublisher {

	private DiscordChangelogPublisher() {
	}

	/**
	 * Sends the changelog at the end of the locally stored build history. An empty
	 * newest changelog is still sent; older builds are never substituted.
	 *
	 * @return number of Discord messages sent (long changelogs may be split)
	 */
	public static int sendLatestBuild(String repositoryName) throws Exception {
		if (repositoryName == null || repositoryName.trim().isEmpty()) {
			throw new IllegalArgumentException("Repository name must not be empty.");
		}
		RepositoryService repositoryService = new RepositoryService();
		repositoryService.readAll();
		DiscordWebhookSettings settings = repositoryService.getDiscordWebhookSettings(repositoryName);
		if (settings == null || settings.getWebhookUrl().isBlank()) {
			throw new IllegalStateException("Configure a Discord webhook in this repository's Discord settings first.");
		}
		List<ChangelogDTO> changelogs = repositoryService.getChangelogs(repositoryName);
		if (changelogs == null || changelogs.isEmpty()) {
			throw new IllegalStateException("No build changelog is available for repository '" + repositoryName + "'. Build it first.");
		}
		ChangelogDTO newestBuild = getNewestBuildChangelog(repositoryService, repositoryName, changelogs);
		RepositoryDTO repository = repositoryService.getRepository(repositoryName);
		String repositoryUrl = getRepositoryUrl(repository);
		WorkshopMetadataCache workshopMetadataCache = repositoryService.getDiscordWorkshopMetadataCache(repositoryName);
		List<String> payloads = DiscordChangelogMessageBuilder.buildPayloads(repositoryName,
				repositoryUrl, repository == null ? null : repository.getPath(), workshopMetadataCache,
				Collections.singletonList(newestBuild), settings);
		for (String payload : payloads) {
			DiscordWebhookClient.send(settings.getWebhookUrl(), payload);
		}
		return payloads.size();
	}

	private static ChangelogDTO getNewestBuildChangelog(RepositoryService repositoryService,
			String repositoryName, List<ChangelogDTO> changelogs) throws Exception {
		ChangelogDTO latestChangelog = changelogs.get(changelogs.size() - 1);
		ServerInfoDTO latestBuild = repositoryService.getServerInfo(repositoryName);
		if (latestBuild == null || latestBuild.getBuildDate() == null
				|| (latestChangelog.getBuildDate() != null
						&& !latestBuild.getBuildDate().after(latestChangelog.getBuildDate()))) {
			return latestChangelog;
		}
		// Builds without repository content changes do not create a changelog history
		// entry. Use the latest build timestamp and an empty delta instead of sending
		// an older changelog and implying that its changes belong to this build.
		ChangelogDTO unchangedBuild = new ChangelogDTO();
		unchangedBuild.setRevision(latestBuild.getRevision());
		unchangedBuild.setBuildDate(latestBuild.getBuildDate());
		return unchangedBuild;
	}

	private static String getRepositoryUrl(RepositoryDTO repository) {
		if (repository == null || repository.getProtocoleDTO() == null) return "";
		String prefix = repository.getProtocoleDTO().getProtocolType() == null
				? "" : repository.getProtocoleDTO().getProtocolType().getPrompt();
		if (repository.getAutoConfigURL() != null && !repository.getAutoConfigURL().isBlank()) {
			return prefix + repository.getAutoConfigURL();
		}
		String url = repository.getProtocoleDTO().getUrl();
		return url == null ? "" : prefix + url;
	}
}
