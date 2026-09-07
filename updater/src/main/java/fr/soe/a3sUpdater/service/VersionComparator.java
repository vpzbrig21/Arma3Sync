package fr.soe.a3sUpdater.service;

/** Numeric dotted-version comparison used to avoid offering downgrades. */
final class VersionComparator {
    private VersionComparator() { }

    static boolean isNewer(String candidate, String current) {
        String[] left = candidate == null ? new String[0] : candidate.trim().split("\\.");
        String[] right = current == null ? new String[0] : current.trim().split("\\.");
        int count = Math.max(left.length, right.length);
        for (int i = 0; i < count; i++) {
            int a = number(left, i);
            int b = number(right, i);
            if (a != b) return a > b;
        }
        return false;
    }

    private static int number(String[] parts, int index) {
        if (index >= parts.length) return 0;
        String digits = parts[index].replaceAll("[^0-9].*", "");
        try { return digits.isEmpty() ? 0 : Integer.parseInt(digits); }
        catch (NumberFormatException ignored) { return 0; }
    }
}
