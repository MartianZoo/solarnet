#!/usr/bin/env python3
"""Build the two source trees independently, then verify per-checkout Pets selection."""

import os
import re
from pathlib import Path
import shutil
import subprocess
import tempfile


ROOT = Path(__file__).resolve().parents[1]


def run(directory, *arguments, succeeds=True):
    command = [str(directory / "gradlew"), "--console=plain", *arguments]
    print(f"\n{directory}: {' '.join(arguments)}", flush=True)
    lines = []
    with subprocess.Popen(
        command, cwd=directory, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT
    ) as process:
        for line in process.stdout:
            print(line, end="", flush=True)
            lines.append(line)
        status = process.wait()
    if (status == 0) != succeeds:
        raise RuntimeError(f"Unexpected exit status {status}: {command}")
    return "".join(lines)


def copy_sources(destination, pets_only=False):
    # Use the current reviewed working tree, including new files, but never ignored build outputs.
    names = subprocess.check_output(
        ["git", "ls-files", "-z", "--cached", "--others", "--exclude-standard"], cwd=ROOT
    ).decode().split("\0")
    for name in names:
        if not name or name.startswith("pets/") != pets_only:
            continue
        source = ROOT / name
        if not source.is_file() or source.is_symlink():
            continue
        relative = Path(name).relative_to("pets") if pets_only else Path(name)
        target = destination / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, target)


with tempfile.TemporaryDirectory(
    prefix="pets-build-separation-", dir=os.environ.get("TMPDIR", os.environ.get("RUNNER_TEMP"))
) as temporary:
    scratch = Path(temporary).resolve()
    pets = scratch / "pets"
    runtime = scratch / "solarnet"
    repository = scratch / "maven"
    copy_sources(pets, pets_only=True)
    copy_sources(runtime)
    assert not (runtime / "pets").exists()
    assert not (pets / "modules/engine").exists()
    run(pets, "build", "publish", f"-PpetsRepository={repository}")
    assert list(repository.glob("dev/martianzoo/pets/pets-jvm/*/*.jar"))
    assert list(repository.glob("dev/martianzoo/pets/pets-js/*/*.klib"))
    assert list(repository.glob("dev/martianzoo/pets/almanac/*/*.zip"))

    # Temporarily move all Pets source out of reach of any accidental sibling path lookup.
    parked_pets = scratch / "independent-source"
    pets.rename(parked_pets)
    artifact_options = ["-PpetsSource=artifacts", f"-PpetsRepository={repository}"]
    run(runtime, "build", ":repl:shadowJar", *artifact_options)
    jar_output = run(runtime, ":repl:shadowJarPath", *artifact_options)
    jar = re.search(r"^(.+repl-all\.jar)$", jar_output, re.MULTILINE)[1]
    empty_directory = scratch / "repl-smoke"
    empty_directory.mkdir()
    repl = subprocess.run(
        ["java", f"-Duser.home={empty_directory}", "-jar", jar],
        cwd=empty_directory,
        input="newgame B 3\ncount Player\nexit\n",
        text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=60, check=True,
    )
    for expected in ["New 3-player game created with options: B", "3 Player", "Bye"]:
        assert expected in repl.stdout, repl.stdout
    assert "Error:" not in repl.stdout, repl.stdout
    print("Packaged REPL created a game outside both source trees.", flush=True)
    dependencies = run(runtime, ":state:dependencies", "--configuration", "jvmCompileClasspath", *artifact_options)
    assert "dev.martianzoo.pets:pets:" in dependencies
    assert "project :pets" not in dependencies

    second_runtime = scratch / "other" / "solarnet"
    copy_sources(second_runtime)
    # Match real worktrees: both can share _local, even with the same directory basename.
    shared_local = scratch / "shared-local"
    shared_local.mkdir()
    for checkout in [runtime, second_runtime]:
        (checkout / "_local").symlink_to(shared_local, target_is_directory=True)
    for checkout, source in [(runtime, parked_pets), (second_runtime, "artifacts")]:
        mode = run(checkout, "petsMode", *artifact_options)
        selection = Path(re.search(r"Pets selection file: (.+)", mode)[1])
        selection.parent.mkdir(parents=True, exist_ok=True)
        selection.write_text(f"petsSource={source}\n")
    assert f"Pets: local source {parked_pets}" in run(runtime, "help")
    assert "Pets: artifacts" in run(second_runtime, "help", f"-PpetsRepository={repository}")
    # An API that exists only in the temporary source edit must compile and execute in Solarnet.
    # This proves source substitution without publishing again or changing the live checkout.
    source_file = parked_pets / "src/common/dev/martianzoo/pets/ast/ClassName.kt"
    original = source_file.read_text()
    probe = runtime / "test/common/dev/martianzoo/state/PetsCompositeBuildTest.kt"
    try:
        source_file.write_text(original.replace(
            "  public companion object {",
            '  public fun compositeBuildProbe(): String = "local Pets edit"\n\n  public companion object {',
            1,
        ))
        probe.write_text("""package dev.martianzoo.state

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import kotlin.test.Test
import kotlin.test.assertEquals

internal class PetsCompositeBuildTest {
  @Test
  fun usesEditedPetsSource() {
    assertEquals("local Pets edit", cn("Proof").compositeBuildProbe())
  }
}
""")
        run(runtime, ":state:jvmTest", "--tests", "dev.martianzoo.state.PetsCompositeBuildTest")
    finally:
        source_file.write_text(original)
        probe.unlink(missing_ok=True)

    assert "Pets: artifacts" in run(runtime, "help", *artifact_options)
    error = run(runtime, "help", "-PpetsSource=missing-pets", succeeds=False)
    assert "Pets source does not contain settings.gradle.kts" in error
    assert f"Pets: local source {parked_pets}" in run(runtime, "help")
    print("Independent builds, JVM/JS publication, artifact consumption, and source selections passed.")
