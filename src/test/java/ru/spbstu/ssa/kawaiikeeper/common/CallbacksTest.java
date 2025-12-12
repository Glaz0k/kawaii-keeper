package ru.spbstu.ssa.kawaiikeeper.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CallbacksTest {

    @Test
    void callback_withIdentifierAndData_shouldReturnFormattedString() {
        String identifier = "test_identifier";
        String data = "test_data";

        String result = Callbacks.callback(identifier, data);

        assertEquals("test_identifier.test_data", result);
    }

    @Test
    void callback_withIdentifierAndNullData_shouldReturnOnlyIdentifier() {
        String identifier = "test_identifier";

        String result = Callbacks.callback(identifier, null);

        assertEquals("test_identifier", result);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", "  ", "test" })
    void callback_withIdentifierOnly_shouldReturnIdentifier(String identifier) {
        String result = Callbacks.callback(identifier);

        assertEquals(identifier, result);
    }

    @Test
    void callback_withNoArguments_shouldReturnEmptyDataConstant() {
        String result = Callbacks.callback();

        assertEquals("EMPTY", result);
    }

    @ParameterizedTest
    @CsvSource({
        "action.data, data",
        "test.identifier.more.data, identifier.more.data",
        "simple., ''",
        "action.with.dots.inside, with.dots.inside"
    })
    void dataOf_withDelimiter_shouldReturnData(String callback, String expectedData) {
        Optional< String > result = Callbacks.dataOf(callback);

        assertTrue(result.isPresent());
        assertEquals(expectedData, result.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "no_delimiter",
        "",
        "justidentifier",
        "ACTION"
    })
    void dataOf_withoutDelimiter_shouldReturnEmptyOptional(String callback) {
        Optional< String > result = Callbacks.dataOf(callback);

        assertFalse(result.isPresent());
    }

    @ParameterizedTest
    @CsvSource({
        "action.data, action",
        "test.identifier.more.data, test",
        "simple., simple",
        "action.with.dots.inside, action"
    })
    void identifierOf_withDelimiter_shouldReturnIdentifier(String callback, String expectedIdentifier) {
        String result = Callbacks.identifierOf(callback);

        assertEquals(expectedIdentifier, result);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "no_delimiter",
        "",
        "justidentifier",
        "ACTION"
    })
    void identifierOf_withoutDelimiter_shouldReturnWholeString(String callback) {
        String result = Callbacks.identifierOf(callback);

        assertEquals(callback, result);
    }

    @Test
    void callback_withEmptyDataString_shouldWorkCorrectly() {
        String identifier = "test";
        String emptyData = "";

        String result = Callbacks.callback(identifier, emptyData);

        assertEquals("test.", result);
    }

    @Test
    void dataOf_withEmptyDataAfterDelimiter_shouldReturnEmptyString() {
        String callback = "test.";

        Optional< String > result = Callbacks.dataOf(callback);

        assertTrue(result.isPresent());
        assertEquals("", result.get());
    }

    @Test
    void callback_methods_shouldWorkTogether() {
        String identifier = "test_action";
        String data = "12345";

        String callback = Callbacks.callback(identifier, data);

        String extractedIdentifier = Callbacks.identifierOf(callback);
        Optional< String > extractedData = Callbacks.dataOf(callback);

        assertEquals(identifier, extractedIdentifier);
        assertTrue(extractedData.isPresent());
        assertEquals(data, extractedData.get());
    }

    @Test
    void callback_withIdentifierOnly_methods_shouldWorkTogether() {
        String identifier = "simple_action";

        String callback = Callbacks.callback(identifier);

        String extractedIdentifier = Callbacks.identifierOf(callback);
        Optional< String > extractedData = Callbacks.dataOf(callback);

        assertEquals(identifier, extractedIdentifier);
        assertFalse(extractedData.isPresent());
    }

    @Test
    void callback_withSpecialCharactersInData_shouldWorkCorrectly() {
        String identifier = "test";
        String data = "special-chars_123@#$%";

        String result = Callbacks.callback(identifier, data);

        assertEquals("test.special-chars_123@#$%", result);
        assertEquals("test", Callbacks.identifierOf(result));
        assertEquals(data, Callbacks.dataOf(result).orElseThrow());
    }

    @Test
    void callback_withMultipleDelimitersInData_shouldWorkCorrectly() {
        String identifier = "action";
        String data = "part1.part2.part3";

        String result = Callbacks.callback(identifier, data);

        assertEquals("action.part1.part2.part3", result);
        assertEquals("action", Callbacks.identifierOf(result));
        assertEquals("part1.part2.part3", Callbacks.dataOf(result).orElseThrow());
    }
}