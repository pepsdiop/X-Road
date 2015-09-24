package ee.ria.xroad.commonui;

import java.io.IOException;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import ee.ria.xroad.common.ErrorCodes;
import ee.ria.xroad.common.ExpectedCodedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests to verify correct optional configuration parts behavior.
 */
public class OptionalPartsConfBehavior {
    @Rule
    public ExpectedCodedException thrown = ExpectedCodedException.none();

    /**
     * Test to ensure validation program can be found from identifier mapping.
     * @throws IOException in case optional parts directory cannot be read
     */
    @Test
    public void shouldGetValidationProgram() throws IOException {
        // Given
        String confDir = "src/test/resources/configuration-parts";
        OptionalPartsConf conf = new OptionalPartsConf(confDir);
        String partFile = "identifiermapping.xml";

        // When
        String actualValidationProgram = conf.getValidationProgram(partFile);

        // Then
        String expectedValidationProgram =
                "/usr/share/xroad/scripts/validate-identifiermapping.sh";

        assertEquals(expectedValidationProgram, actualValidationProgram);
    }

    /**
     * Test to ensure identifier mapping content identifier can be read.
     * @throws IOException in case optional parts directory cannot be read
     */
    @Test
    public void shouldGetContentIdentifier() throws IOException {
        // Given
        String confDir = "src/test/resources/configuration-parts";
        OptionalPartsConf conf = new OptionalPartsConf(confDir);
        String partFile = "identifiermapping.xml";

        // When
        String actualContentIdentifier = conf.getContentIdentifier(partFile);

        // Then
        String expectedContentIdentifier = "IDENTIFIERMAPPING";

        assertEquals(expectedContentIdentifier, actualContentIdentifier);
    }

    /**
     * Test to ensure all configuration parts in the directory can be read.
     * @throws IOException in case optional parts directory cannot be read
     */
    @Test
    public void shouldGetAllOptionalConfigurationParts() throws IOException {
        // Given
        String confDir = "src/test/resources/configuration-parts";
        OptionalPartsConf conf = new OptionalPartsConf(confDir);

        // When
        List<OptionalConfPart> actualOptionalParts = conf.getAllParts();

        // Then
        OptionalConfPart expectedFirstPart = new OptionalConfPart(
                "identifiermapping.xml", "IDENTIFIERMAPPING");

        OptionalConfPart expectedSecondPart = new OptionalConfPart(
                        "messageconverter.xml", "MESSAGECONVERTER");

        assertEquals(2, actualOptionalParts.size());
        assertTrue(actualOptionalParts.contains(expectedFirstPart));
        assertTrue(actualOptionalParts.contains(expectedSecondPart));
    }

    /**
     * Test to ensure the optional parts list is empty if directory does not exist.
     * @throws IOException in case optional parts directory cannot be read
     */
    @Test
    public void shouldReturnEmptyListWhenNoConfDirectory() throws IOException {
        // Given
        String confDir = "src/test/resources/configuration-parts-NONEXISTENT";
        OptionalPartsConf conf = new OptionalPartsConf(confDir);

        // When
        List<OptionalConfPart> actualOptionalParts = conf.getAllParts();

        // Then
        assertEquals(0, actualOptionalParts.size());
    }

    /**
     * Test to ensure malformed files are skipped.
     * @throws IOException in case optional parts directory cannot be read
     */
    @Test
    public void shouldSkipFileWhenMalformed() throws IOException {
        // Given
        String confDir = "src/test/resources/configuration-parts-MALFORMED_FILE";
        OptionalPartsConf conf = new OptionalPartsConf(confDir);

        // When
        List<OptionalConfPart> actualOptionalParts = conf.getAllParts();

        // Then
        assertEquals(0, actualOptionalParts.size());
    }

    /**
     * Test to ensure reserved filenames are not allowed in configuration parts.
     * @throws IOException in case optional parts directory cannot be read
     */
    @Test
    public void shouldNotAllowReservedFilenames() throws IOException {
        testMalformedConf(
                "src/test/resources/configuration-parts-RESERVED_FILE");
    }

    /**
     * Test to ensure reserved content IDs are not allowed in configuration parts.
     * @throws IOException in case optional parts directory cannot be read
     */
    @Test
    public void shouldNotAllowReservedContentIds() throws IOException {
        testMalformedConf(
                "src/test/resources/configuration-parts-RESERVED_ID");
    }

    /**
     * Test to ensure duplicate filenames are not allowed in configuration parts.
     * @throws IOException in case optional parts directory cannot be read
     */
    @Test
    public void shouldNotAllowDuplicateFilenames() throws IOException {
        testMalformedConf(
                "src/test/resources/configuration-parts-DUPLICATES");
    }

    private void testMalformedConf(String confDir) throws IOException {
        thrown.expectError(ErrorCodes.X_MALFORMED_OPTIONAL_PARTS_CONF);

        new OptionalPartsConf(confDir);
    }
}