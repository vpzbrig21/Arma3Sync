package main

import "testing"

func TestParseJavaMajor(t *testing.T) {
	tests := []struct {
		name   string
		output string
		want   int
	}{
		{name: "oracle modern", output: `java version "25.0.2"`, want: 25},
		{name: "openjdk version", output: `openjdk version "26.0.1" 2026-04-21`, want: 26},
		{name: "openjdk short", output: `openjdk 25.0.2 2026-01-20`, want: 25},
		{name: "legacy java eight", output: `java version "1.8.0_503"`, want: 1},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, ok := parseJavaMajor([]byte(test.output))
			if !ok {
				t.Fatalf("parseJavaMajor() did not recognize %q", test.output)
			}
			if got != test.want {
				t.Fatalf("parseJavaMajor() = %d, want %d", got, test.want)
			}
		})
	}
}

func TestParseJavaMajorRejectsUnknownOutput(t *testing.T) {
	if got, ok := parseJavaMajor([]byte("not a Java version")); ok || got != 0 {
		t.Fatalf("parseJavaMajor() = (%d, %t), want (0, false)", got, ok)
	}
}

func TestWithoutArgumentRemovesOnlyInternalFlag(t *testing.T) {
	args := []string{"-a3s-elevate-updater", "-github", "-dev", "-a3s-elevate-updater"}
	got := withoutArgument(args, "-a3s-elevate-updater")
	if len(got) != 2 || got[0] != "-github" || got[1] != "-dev" {
		t.Fatalf("withoutArgument() = %#v, want [ -github -dev ]", got)
	}
}

func TestQuoteWindowsArgument(t *testing.T) {
	if got := quoteWindowsArgument("-github"); got != "-github" {
		t.Fatalf("quoteWindowsArgument() = %q, want -github", got)
	}
	if got := quoteWindowsArgument(`-Da3s.updater.userConfigPath=X:\\Program Files\\Arma3Sync`); got != `"-Da3s.updater.userConfigPath=X:\\Program Files\\Arma3Sync"` {
		t.Fatalf("quoteWindowsArgument() = %q", got)
	}
}

func TestSplitJavaProperties(t *testing.T) {
	properties, applicationArgs := splitJavaProperties([]string{
		"-github",
		"-Da3s.updater.userConfigPath=X:\\Program Files\\Arma3Sync",
		"-dev",
	})
	if len(properties) != 1 || properties[0] != "-Da3s.updater.userConfigPath=X:\\Program Files\\Arma3Sync" {
		t.Fatalf("splitJavaProperties() properties = %#v", properties)
	}
	if len(applicationArgs) != 2 || applicationArgs[0] != "-github" || applicationArgs[1] != "-dev" {
		t.Fatalf("splitJavaProperties() application args = %#v", applicationArgs)
	}
}
