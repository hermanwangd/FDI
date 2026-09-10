package com.featuredeliveryintelligence.fdi.portability.changereference;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Contract tests for deterministic classification with fixed exclusions (SF-BL-004
 *  Task 1): category mapping, denylisted paths, secret signatures, metadata-only
 *  binary/LFS handling. Rejected content never reaches a result. */
class ChangeClassifierTests {

    private final ChangeClassifier classifier = new ChangeClassifier();

    private static ChangedPath added(String newPath, String content) {
        return new ChangedPath(null, newPath, Operation.ADD, null, "b".repeat(40), null,
                content == null ? null : content.getBytes(StandardCharsets.UTF_8), List.of());
    }

    private static ChangedPath changed(String oldPath, String newPath, String oldContent, String newContent) {
        return new ChangedPath(oldPath, newPath, Operation.MODIFY, "a".repeat(40), "b".repeat(40),
                oldContent == null ? null : oldContent.getBytes(StandardCharsets.UTF_8),
                newContent == null ? null : newContent.getBytes(StandardCharsets.UTF_8), List.of());
    }

    @Test
    void mapsPathsToDeterministicCategories() {
        String[][] cases = {
                {"src/main/java/com/x/App.java", "CODE"}, {"scripts/deploy.sh", "CODE"},
                {"src/test/java/com/x/AppTests.java", "TEST"}, {"tests/test_pkb.py", "TEST"},
                {"docs/guide.md", "DOCUMENTATION"}, {"notes.txt", "DOCUMENTATION"},
                {"PROJECT-OVERVIEW.md", "CONTROL"}, {"sub/STATUS.json", "CONTROL"}, {"AGENTS.md", "CONTROL"},
                {"contracts/reverse-proposal.schema.json", "CONTRACT"}, {"application.yaml", "CONFIGURATION"},
                {"pom.xml", "CONFIGURATION"}, {"skills/pkb001/SKILL.md", "SKILL"},
                {"validation/pkb001/evidence.json", "EVIDENCE"}};
        for (String[] c : cases) {
            assertThat(classifier.classify(added(c[0], "text\n")).category()).as(c[0])
                    .isEqualTo(Category.valueOf(c[1]));
        }
        PathClassification again = classifier.classify(added("docs/guide.md", "text\n"));
        assertThat(again).isEqualTo(classifier.classify(added("docs/guide.md", "text\n")));
        assertThat(again.excerptAllowed()).isTrue();
    }

    @Test
    void denylistedPathsAreExcludedWithoutExcerpts() {
        List<String> denied = List.of("target/classes/A.class", "build/out.bin", ".env", ".env.local",
                "config/prod.pem", "keys/id_rsa", ".aws/credentials", "x/netrc/.netrc", "dist/app.zip",
                "repo/release.tar.gz", "lib/dep.jar", ".git/config");
        denied.forEach(path -> assertThat(classifier.classify(added(path, "text\n"))).as(path).satisfies(c -> {
            assertThat(c.excluded()).isTrue();
            assertThat(c.denial()).isEqualTo(FailureCode.PATH_UNSAFE);
            assertThat(c.excerptAllowed()).isFalse();
        }));
    }

    @Test
    void secretSignaturesFailClosedWithoutLeakingContent() {
        String privateKey = "-----BEGIN RSA PRIVATE KEY-----\nMIIEpAIBAAKCAQEA7\n-----END RSA PRIVATE KEY-----\n";
        String awsKey = "aws_access_key_id = AKIAIOSFODNN7EXAMPLE\n";
        assertThat(classifier.classify(added("docs/keys.md", privateKey)).denial())
                .isEqualTo(FailureCode.SECRET_DETECTED);
        assertThat(classifier.classify(added("config/app.yaml", awsKey)).denial())
                .isEqualTo(FailureCode.SECRET_DETECTED);
        assertThat(classifier.classify(changed("docs/keys.md", null, privateKey, null)).denial())
                .isEqualTo(FailureCode.SECRET_DETECTED);
    }

    @Test
    void binaryAndLfsPayloadsAreMetadataOnly() {
        byte[] binary = new byte[]{'P', 'K', 0, 3, 4, 0, 0, 'x'};
        PathClassification png = classifier.classify(added("assets/logo.png", null));
        assertThat(png.category()).isEqualTo(Category.BINARY);
        assertThat(png.excerptAllowed()).isFalse();
        assertThat(png.excluded()).isFalse();
        assertThat(classifier.classify(added("data/blob.bin", new String(binary, StandardCharsets.ISO_8859_1)))
                .excerptAllowed()).isFalse();
        String lfs = "version https://git-lfs.github.com/spec/v1\noid sha256:" + "0".repeat(64) + "\nsize 5\n";
        assertThat(classifier.classify(changed("bin/model.bin", null, null, lfs)).category())
                .isEqualTo(Category.BINARY);
    }

    @Test
    void renamesClassifyByNewPathAndDeletesByOldPath() {
        assertThat(classifier.classify(changed("docs/draft.md", "FRAMEWORK-SPEC.md", "t\n", "t\n")).category())
                .isEqualTo(Category.CONTROL);
        assertThat(classifier.classify(changed("AGENTS.md", null, "t\n", null)).category())
                .isEqualTo(Category.CONTROL);
        assertThat(classifier.classify(changed("src/Main.java", "src/Renamed.java", "t\n", "t\n")).category())
                .isEqualTo(Category.CODE);
    }

    @Test
    void declaresStableFailClosedFailureCodes() {
        assertThat(FailureCode.values()).containsExactly(FailureCode.REVISION_INVALID, FailureCode.ANCESTRY_INVALID,
                FailureCode.WORKTREE_DIRTY, FailureCode.PATH_UNSAFE, FailureCode.SECRET_DETECTED,
                FailureCode.INPUT_LIMIT_EXCEEDED, FailureCode.GIT_COMMAND_FAILED, FailureCode.OUTPUT_EXISTS,
                FailureCode.PACKAGE_VALIDATION_FAILED);
    }
}
