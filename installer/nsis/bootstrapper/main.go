package main

import (
	"bytes"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"sort"
	"strings"
	"syscall"
	"unsafe"
)

const minimumJavaMajor = 25
const createNoWindow = 0x08000000

type javaCandidate struct {
	java  string
	javaw string
}

func main() {
	jarPath, appArgs, installDir, err := resolveApplication(os.Args[1:])
	if err != nil {
		fail(err.Error())
		return
	}

	candidate, version, err := findJava25(installDir)
	if err != nil {
		fail(err.Error())
		return
	}

	javaPath := candidate.javaw
	if hasConsoleArgument(appArgs) || javaPath == "" {
		javaPath = candidate.java
	}

	commandArgs := []string{
		"-Da3s.installationPath=" + installDir,
		"-Djava.net.preferIPv4Stack=true",
		"-Dsun.java2d.d3d=false",
		"-jar",
		jarPath,
	}
	commandArgs = append(commandArgs, appArgs...)

	command := exec.Command(javaPath, commandArgs...)
	command.Dir = installDir
	if hasConsoleArgument(appArgs) {
		command.Stdin = os.Stdin
		command.Stdout = os.Stdout
		command.Stderr = os.Stderr
		if err := command.Run(); err != nil {
			if exitError, ok := err.(*exec.ExitError); ok {
				os.Exit(exitError.ExitCode())
			}
			fail(fmt.Sprintf("Java konnte nicht gestartet werden (%s): %v", javaPath, err))
			return
		}
		return
	}

	if err := command.Start(); err != nil {
		fail(fmt.Sprintf("Arma3Sync konnte mit Java %d nicht gestartet werden: %v", version, err))
		return
	}
}

func resolveApplication(args []string) (string, []string, string, error) {
	executable, err := os.Executable()
	if err != nil {
		return "", nil, "", fmt.Errorf("Pfad des Launchers konnte nicht ermittelt werden: %v", err)
	}
	installDir := filepath.Dir(executable)
	// This permits an installed launcher to be selected as the Windows
	// "Open with" handler for a JAR. The explicit JAR path is removed before
	// forwarding the remaining application arguments.
	if len(args) > 0 && strings.EqualFold(filepath.Ext(args[0]), ".jar") && isFile(args[0]) {
		jarPath, err := filepath.Abs(args[0])
		if err != nil {
			return "", nil, installDir, fmt.Errorf("JAR-Pfad konnte nicht aufgelöst werden: %v", err)
		}
		return jarPath, args[1:], filepath.Dir(jarPath), nil
	}

	jarPath := filepath.Join(installDir, "Arma3Sync.jar")
	if !isFile(jarPath) {
		legacyJar := filepath.Join(installDir, "ArmA3Sync.jar")
		if isFile(legacyJar) {
			jarPath = legacyJar
		} else {
			return "", nil, installDir, fmt.Errorf("Arma3Sync.jar wurde nicht gefunden in:\n%s", installDir)
		}
	}

	return jarPath, args, installDir, nil
}

func findJava25(installDir string) (javaCandidate, int, error) {
	// A standard installation carries its own runtime. Prefer it over PATH,
	// JAVA_HOME and registry shims so Java 8 installations cannot intercept
	// the application and so the updater and launcher use the same runtime.
	if candidate, version, ok := bundledJava25(installDir); ok {
		return candidate, version, nil
	}

	candidates := collectCandidates()
	for _, candidate := range candidates {
		version, err := javaMajorVersion(candidate.java)
		if err == nil && version >= minimumJavaMajor {
			return candidate, version, nil
		}
	}

	return javaCandidate{}, 0, fmt.Errorf(
		"Keine kompatible Java-Version gefunden. Arma3Sync benötigt Java %d oder neuer.\n\n"+
			"Bitte JAVA_HOME/PATH auf eine 64-Bit-Java-%d-Installation setzen.",
		minimumJavaMajor, minimumJavaMajor)
}

func bundledJava25(installDir string) (javaCandidate, int, bool) {
	if strings.TrimSpace(installDir) == "" {
		return javaCandidate{}, 0, false
	}
	home := filepath.Join(installDir, "runtime")
	java := filepath.Join(home, "bin", "java.exe")
	javaw := filepath.Join(home, "bin", "javaw.exe")
	if !isFile(java) {
		return javaCandidate{}, 0, false
	}
	version, err := javaMajorVersion(java)
	if err != nil || version < minimumJavaMajor {
		return javaCandidate{}, 0, false
	}
	if !isFile(javaw) {
		javaw = ""
	}
	return javaCandidate{java: java, javaw: javaw}, version, true
}

func collectCandidates() []javaCandidate {
	var candidates []javaCandidate
	seen := make(map[string]bool)
	add := func(home string) {
		home = strings.Trim(strings.TrimSpace(home), "\"")
		if home == "" {
			return
		}
		if strings.EqualFold(filepath.Base(home), "bin") {
			home = filepath.Dir(home)
		}
		java := filepath.Join(home, "bin", "java.exe")
		javaw := filepath.Join(home, "bin", "javaw.exe")
		if !isFile(java) {
			if filepath.Base(home) == "java.exe" && isFile(home) {
				java = home
				javaw = filepath.Join(filepath.Dir(home), "javaw.exe")
			} else {
				return
			}
		}
		if !isFile(javaw) {
			javaw = ""
		}
		key := strings.ToLower(java)
		if !seen[key] {
			seen[key] = true
			candidates = append(candidates, javaCandidate{java: java, javaw: javaw})
		}
	}

	add(os.Getenv("JAVA_HOME"))
	add(os.Getenv("JDK_HOME"))
	if pathJava, err := exec.LookPath("java.exe"); err == nil {
		add(pathJava)
	}
	if pathJava, err := exec.LookPath("java"); err == nil {
		add(pathJava)
	}

	for _, root := range []string{
		"HKCU\\SOFTWARE\\JavaSoft\\JDK",
		"HKCU\\SOFTWARE\\JavaSoft\\Java Runtime Environment",
		"HKLM\\SOFTWARE\\JavaSoft\\JDK",
		"HKLM\\SOFTWARE\\JavaSoft\\Java Runtime Environment",
		"HKLM\\SOFTWARE\\WOW6432Node\\JavaSoft\\JDK",
		"HKLM\\SOFTWARE\\WOW6432Node\\JavaSoft\\Java Runtime Environment",
	} {
		for _, home := range registryJavaHomes(root) {
			add(home)
		}
	}

	for _, root := range []string{os.Getenv("ProgramFiles"), os.Getenv("ProgramFiles(x86)")} {
		javaRoot := filepath.Join(root, "Java")
		entries, err := os.ReadDir(javaRoot)
		if err != nil {
			continue
		}
		sort.Slice(entries, func(i, j int) bool { return entries[i].Name() > entries[j].Name() })
		for _, entry := range entries {
			if entry.IsDir() {
				add(filepath.Join(javaRoot, entry.Name()))
			}
		}
	}
	return candidates
}

func registryJavaHomes(root string) []string {
	command := hiddenCommand("reg.exe", "query", root, "/s", "/v", "JavaHome")
	output, err := command.Output()
	if err != nil {
		return nil
	}
	var homes []string
	for _, line := range bytes.Split(output, []byte{'\n'}) {
		text := strings.TrimSpace(string(line))
		if !strings.Contains(strings.ToLower(text), "javaHome") {
			continue
		}
		fields := strings.Fields(text)
		if len(fields) >= 3 && strings.EqualFold(fields[1], "REG_SZ") {
			homes = append(homes, strings.Join(fields[2:], " "))
		}
	}
	return homes
}

func javaMajorVersion(javaPath string) (int, error) {
	if !isFile(javaPath) {
		return 0, fmt.Errorf("Java-Datei fehlt: %s", javaPath)
	}
	// java.exe is used because javaw.exe does not reliably expose version
	// output on all Windows releases. Suppress the console window explicitly;
	// this probe must never be visible to the user.
	command := hiddenCommand(javaPath, "-version")
	output, err := command.CombinedOutput()
	if err != nil {
		return 0, err
	}
	major, ok := parseJavaMajor(output)
	if !ok {
		return 0, fmt.Errorf("Java-Version konnte nicht gelesen werden")
	}
	return major, nil
}

// parseJavaMajor intentionally avoids regexp and strconv. The launcher is a
// small native bootstrapper and only needs the first numeric component from
// the standard Java version formats, for example:
//
//	java version "25.0.2"
//	openjdk version "25.0.2"
//	openjdk 25.0.2
func parseJavaMajor(output []byte) (int, bool) {
	text := strings.ToLower(string(output))
	markers := []string{`version "`, "openjdk "}
	for _, marker := range markers {
		start := strings.Index(text, marker)
		if start < 0 {
			continue
		}
		start += len(marker)
		end := start
		for end < len(text) && text[end] >= '0' && text[end] <= '9' {
			end++
		}
		if end == start {
			continue
		}
		major := 0
		for index := start; index < end; index++ {
			major = major*10 + int(text[index]-'0')
		}
		return major, true
	}
	return 0, false
}

func hiddenCommand(name string, args ...string) *exec.Cmd {
	command := exec.Command(name, args...)
	command.SysProcAttr = &syscall.SysProcAttr{
		CreationFlags: createNoWindow,
		HideWindow:    true,
	}
	return command
}

func hasConsoleArgument(args []string) bool {
	for _, arg := range args {
		if strings.EqualFold(arg, "-console") {
			return true
		}
	}
	return false
}

func isFile(path string) bool {
	info, err := os.Stat(path)
	return err == nil && !info.IsDir()
}

func fail(message string) {
	if hasConsoleArgument(os.Args[1:]) {
		fmt.Fprintln(os.Stderr, message)
		return
	}
	const mbIconError = 0x00000010
	const mbOK = 0x00000000
	text := syscall.StringToUTF16Ptr(message)
	title := syscall.StringToUTF16Ptr("Arma3Sync - Java 25 benötigt")
	user32 := syscall.NewLazyDLL("user32.dll")
	messageBox := user32.NewProc("MessageBoxW")
	messageBox.Call(0, uintptr(unsafe.Pointer(text)), uintptr(unsafe.Pointer(title)), mbIconError|mbOK)
}
