#!/usr/bin/env python3
"""Tests for plan_matrix.py. Run from the repo root: python -I -m unittest discover -s .github/scripts
or: python -I .github/scripts/test_plan_matrix.py

Synthetic file lists always run; the cases built from real commits are skipped when the commit is not in the clone.
"""
import os
import subprocess
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import plan_matrix as pm  # noqa: E402

ALL = set(pm.NAMES)
ONE_MC = {"26.2": {"forge-26.2", "fabric-26.2", "neoforge-26.2"}}


def git_files(*args):
    """Paths of a real commit range, or None when git or the commit is not available."""
    try:
        out = subprocess.run(["git", "diff", "--name-only", "--no-renames", *args], capture_output=True, text=True, cwd=pm.ROOT)
    except OSError:
        return None
    return out.stdout.split() if out.returncode == 0 else None


class SyntheticBuild(unittest.TestCase):
    def pick(self, *files):
        return set(pm.pick_build(list(files)))

    def test_docs_and_status_pick_nothing(self):
        self.assertEqual(self.pick("PROJECT_STATUS.md", "README.md", "CHANGELOG.md", "docs/COMPATIBILITY.md",
                                   "docs/frames/x.png", "branding/banners/banner.png", "curseforge/DESCRIPTION.md",
                                   "licenses/OFL-silkscreen.txt", "LICENSE", ".gitignore", "dist/x.jar",
                                   "scripts/run-tour.ps1", "scratch-tour-fabric-26.2.log"), set())

    def test_workflow_and_ci_script_changes_pick_nothing(self):
        self.assertEqual(self.pick(".github/workflows/build.yml", ".github/workflows/release.yml",
                                   ".github/scripts/plan_matrix.py", ".github/scripts/publish_curseforge.py",
                                   ".github/ISSUE_TEMPLATE/config.yml"), set())

    def test_common_main_picks_everything(self):
        self.assertEqual(self.pick("common/src/main/java/com/itmecasey/barelysorts/Sorter.java"), ALL)
        self.assertEqual(self.pick("common/src/main/resources/pack.mcmeta"), ALL)

    def test_common_tests_pick_only_the_module_that_compiles_them(self):
        self.assertEqual(self.pick("common/src/test/java/com/itmecasey/barelysorts/OutputSlotsTest.java"), {"neoforge-1.21.1"})

    def test_dev_tour_sources_are_not_built_by_ci(self):
        self.assertEqual(self.pick("common/src/devtest/java/x/Tour.java", "version-26.2/src/devtest/java/x/Y.java"), set())

    def test_version_folder_picks_that_minecraft_version_only(self):
        self.assertEqual(self.pick("version-26.2/src/main/java/x/A.java"), ONE_MC["26.2"])
        self.assertEqual(self.pick("version-1.20.1/src/main/resources/a.json"), {"forge-1.20.1", "fabric-1.20.1"})

    def test_single_module_change_picks_exactly_that_module(self):
        self.assertEqual(self.pick("fabric-26.3/src/main/java/x/FabricClient.java"), {"fabric-26.3"})
        self.assertEqual(self.pick("neoforge-1.21.1/gradle.properties"), {"neoforge-1.21.1"})
        self.assertEqual(self.pick("forge-26.1.2/build.gradle", "forge-26.1.2/gradle.properties"), {"forge-26.1.2"})

    def test_module_build_and_run_folders_pick_nothing(self):
        self.assertEqual(self.pick("forge-26.2/build/libs/x.jar", "fabric-26.2/run/options.txt", "fabric-26.2/runs/a"), set())

    def test_neoforge_1_20_1_has_no_ci_build(self):
        self.assertEqual(self.pick("neoforge-1.20.1/build.gradle"), set())
        self.assertEqual(self.pick("forge-1.20.1/src/main/java/x/A.java"), {"forge-1.20.1"})

    def test_combine_script_picks_everything(self):
        self.assertEqual(self.pick("scripts/combine_jars.py"), ALL)

    def test_unknown_path_picks_everything(self):
        self.assertEqual(self.pick("something/new.txt"), ALL)
        self.assertEqual(self.pick("build.gradle"), ALL)
        self.assertEqual(self.pick("forge-9.9/build.gradle"), ALL)
        self.assertEqual(self.pick("version-9.9/src/main/java/A.java"), ALL)

    def test_mixed_change_is_the_union(self):
        self.assertEqual(self.pick("PROJECT_STATUS.md", "fabric-26.2/gradle.properties", "forge-26.3/gradle.properties"), {"fabric-26.2", "forge-26.3"})

    def test_backslash_and_dot_slash_paths(self):
        self.assertEqual(self.pick("fabric-26.2\\src\\main\\java\\A.java"), {"fabric-26.2"})
        self.assertEqual(self.pick("./.github/workflows/build.yml"), set())

    def test_diff_failure_picks_everything(self):
        self.assertEqual(set(pm.pick_build(None)), ALL)
        self.assertIsNone(pm.changed_files("0123456789abcdef0123456789abcdef01234567", "fedcba9876543210fedcba9876543210fedcba98"))

    def test_outputs(self):
        out = pm.outputs(["neoforge-1.21.1", "forge-26.2"])
        self.assertEqual(out["any"], "true")
        self.assertEqual(out["count"], "2")
        self.assertEqual(out["test"], "true")
        self.assertEqual(out["combine"], "")
        nothing = pm.outputs([])
        self.assertEqual((nothing["any"], nothing["test"], nothing["count"], nothing["matrix"]), ("false", "false", "0", '{"include": []}'))
        self.assertEqual(pm.outputs(pm.NAMES)["combine"], "1.20.1 1.21.1 26.1.2 26.2 26.3")
        self.assertEqual(pm.outputs(sorted(ONE_MC["26.2"]))["combine"], "26.2")

    def test_matrix_matches_the_workflow_fields(self):
        import json
        entry = json.loads(pm.outputs(["forge-1.20.1"])["matrix"])["include"][0]
        self.assertEqual(entry, {"module": "forge-1.20.1", "java": 17})


class RealCommits(unittest.TestCase):
    def check(self, a, b, expected):
        files = git_files(a, b)
        if files is None:
            self.skipTest(f"commit {a} or {b} is not in this clone")
        self.assertEqual(set(pm.pick_build(files)), expected)

    def test_status_only_commit(self):
        self.check("fae817b^", "fae817b", set())

    def test_devtest_only_commit(self):
        self.check("0e810a0^", "0e810a0", set())

    def test_docs_only_commit(self):
        self.check("3699a66^", "3699a66", set())

    def test_common_change_with_version_bump_commit(self):
        self.check("e5ab9b0^", "e5ab9b0", ALL)

    def test_workflow_and_1_20_1_commit(self):
        # touched workflows, docs, publish script, and only the 1.20.1 modules
        self.check("6e7373d^", "6e7373d", {"forge-1.20.1", "fabric-1.20.1"})


class Release(unittest.TestCase):
    def make_root(self, versions):
        root = tempfile.mkdtemp()
        for module in pm.NAMES:
            os.makedirs(os.path.join(root, module))
            with open(os.path.join(root, module, "gradle.properties"), "w", encoding="utf-8") as fh:
                fh.write(f"mod_id=barelysorts\nmod_version={versions.get(module, '2026.11.3')}\n")
        return root

    def test_only_modules_at_the_tag_version(self):
        root = self.make_root({m: "2026.11.4" for m in pm.NAMES if m.endswith("1.20.1")})
        picked, version = pm.pick_release("v2026.11.4", root)
        self.assertEqual(version, "2026.11.4")
        self.assertEqual(set(picked), {"forge-1.20.1", "fabric-1.20.1"})

    def test_all_modules_at_the_tag_version(self):
        root = self.make_root({m: "2026.11.4" for m in pm.NAMES})
        self.assertEqual(set(pm.pick_release("v2026.11.4", root)[0]), ALL)

    def test_tag_nobody_carries_picks_nothing(self):
        root = self.make_root({})
        self.assertEqual(pm.pick_release("v2026.11.9", root)[0], [])

    def test_manual_run_uses_the_newest_version(self):
        root = self.make_root({"forge-26.3": "2026.11.10", "fabric-26.3": "2026.11.10", "neoforge-26.3": "2026.11.10"})
        picked, version = pm.pick_release("main", root)
        self.assertEqual(version, "2026.11.10")
        self.assertEqual(set(picked), ONE_MC.get("26.3", {"forge-26.3", "fabric-26.3", "neoforge-26.3"}))

    def test_real_repo_versions(self):
        version = pm.newest_version()
        if not version:
            self.skipTest("no mod_version found")
        picked, _ = pm.pick_release("v" + version)
        self.assertTrue(picked)
        for module in picked:
            self.assertEqual(pm.module_version(module), version)


if __name__ == "__main__":
    unittest.main()
