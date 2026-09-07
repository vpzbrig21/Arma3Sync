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
