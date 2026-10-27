package fr.soe.a3s.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Shared case-insensitive alphabetical ordering for changelog entries. */
public final class ChangelogEntrySorter {

	private ChangelogEntrySorter() {
	}

	public static List<String> sortedCopy(List<String> entries) {
		List<String> sorted = entries == null ? new ArrayList<String>() : new ArrayList<String>(entries);
		sorted.sort(Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder())));
		return sorted;
	}
}
