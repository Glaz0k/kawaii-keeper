package ru.spbstu.ssa.kawaiikeeper.handler;

import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.spbstu.ssa.kawaiikeeper.common.Callbacks;
import ru.spbstu.ssa.kawaiikeeper.config.ApiConfig;
import ru.spbstu.ssa.kawaiikeeper.exception.ChatActionException;
import ru.spbstu.ssa.kawaiikeeper.service.CategoryService;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryHandlerTest {

    @Mock
    private CategoryService categoryService;

    @Mock
    private ApiConfig apiConfig;

    @Mock
    private Message message;

    @Mock
    private CallbackQuery callbackQuery;

    @Mock
    private Message inaccessibleMessage;

    @Mock
    private Chat chat;

    @Mock
    private User user;

    private CategoryHandler categoryHandler;

    @BeforeEach
    void setUp() {
        List< String > categories = List.of("Cat1", "Cat2", "Cat3", "Cat4", "Cat5", "Cat6", "Cat7", "Cat8", "Cat9");
        when(apiConfig.getApiCategories()).thenReturn(categories);
        categoryHandler = new CategoryHandler(categoryService, apiConfig);
    }

    @Test
    void commandHandlers_shouldContainCategoryCommand() {
        Map< String, Function< ? super Message, List< ? extends BaseRequest< ?, ? > > > > handlers =
            categoryHandler.commandHandlers();

        assertEquals(1, handlers.size());
        assertTrue(handlers.containsKey("category"));
        assertNotNull(handlers.get("category"));
    }

    @Test
    void callbackHandlers_shouldContainAllCallbacks() {
        Map< String, Function< ? super CallbackQuery, List< ? extends BaseRequest< ?, ? > > > > handlers =
            categoryHandler.callbackHandlers();

        assertEquals(3, handlers.size());
        assertTrue(handlers.containsKey("category_page"));
        assertTrue(handlers.containsKey("category_update"));
        assertTrue(handlers.containsKey("category_cancel"));
        assertNotNull(handlers.get("category_page"));
        assertNotNull(handlers.get("category_update"));
        assertNotNull(handlers.get("category_cancel"));
    }

    @Test
    void handleCategory_shouldReturnSendMessageWithFirstPage() {
        long chatId = 123L;
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);

        List< SendMessage > result = categoryHandler.handleCategory(message);

        assertNotNull(result);
        assertEquals(1, result.size());

        SendMessage sendMessage = result.get(0);
        assertEquals(chatId, sendMessage.getParameters().get("chat_id"));
        assertEquals("Страница - 1 / 2", sendMessage.getParameters().get("text"));
        assertInstanceOf(InlineKeyboardMarkup.class, sendMessage.getParameters().get("reply_markup"));
    }

    @Test
    void handleSetPage_shouldReturnEditMessageText() {
        long chatId = 123L;
        int messageId = 456;
        int page = 1;

        when(callbackQuery.maybeInaccessibleMessage()).thenReturn(inaccessibleMessage);
        when(inaccessibleMessage.chat()).thenReturn(chat);
        when(inaccessibleMessage.messageId()).thenReturn(messageId);
        when(chat.id()).thenReturn(chatId);

        String callbackData = Callbacks.callback("category_page", String.valueOf(page));
        when(callbackQuery.data()).thenReturn(callbackData);

        List< EditMessageText > result = categoryHandler.handleSetPage(callbackQuery);

        assertNotNull(result);
        assertEquals(1, result.size());

        EditMessageText editMessage = result.get(0);
        assertEquals(chatId, editMessage.getParameters().get("chat_id"));
        assertEquals(messageId, editMessage.getParameters().get("message_id"));
        assertEquals("Страница - 2 / 2", editMessage.getParameters().get("text"));
        assertInstanceOf(InlineKeyboardMarkup.class, editMessage.getParameters().get("reply_markup"));
    }

    @Test
    void handleSetPage_withInvalidCallbackData_shouldThrowException() {
        when(callbackQuery.maybeInaccessibleMessage()).thenReturn(inaccessibleMessage);

        assertThrows(RuntimeException.class, () -> categoryHandler.handleSetPage(callbackQuery));
    }

    @Test
    void handleUpdateCategory_shouldReturnAnswerCallbackQuery() {
        long chatId = 123L;
        long userId = 456L;
        String category = "Cat1";

        when(callbackQuery.maybeInaccessibleMessage()).thenReturn(inaccessibleMessage);
        when(inaccessibleMessage.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);
        when(callbackQuery.from()).thenReturn(user);
        when(user.id()).thenReturn(userId);

        String callbackData = Callbacks.callback("category_update", category);
        when(callbackQuery.data()).thenReturn(callbackData);
        when(callbackQuery.id()).thenReturn("callback_id");

        when(categoryService.updateCategory(userId, category)).thenReturn(null);

        List< AnswerCallbackQuery > result = categoryHandler.handleUpdateCategory(callbackQuery);

        assertNotNull(result);
        assertEquals(1, result.size());

        AnswerCallbackQuery answer = result.get(0);
        assertEquals("callback_id", answer.getParameters().get("callback_query_id"));
        assertEquals("Категория успешно обновлена, теперь - Cat1", answer.getParameters().get("text"));

        verify(categoryService).updateCategory(userId, category);
    }

    @Test
    void handleUpdateCategory_whenServiceThrowsException_shouldThrowChatActionException() {
        long chatId = 123L;
        long userId = 456L;
        String category = "Cat1";

        when(callbackQuery.maybeInaccessibleMessage()).thenReturn(inaccessibleMessage);
        when(inaccessibleMessage.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);
        when(callbackQuery.from()).thenReturn(user);
        when(user.id()).thenReturn(userId);

        String callbackData = Callbacks.callback("category_update", category);
        when(callbackQuery.data()).thenReturn(callbackData);

        doThrow(new RuntimeException("DB error")).when(categoryService).updateCategory(anyLong(), anyString());

        ChatActionException exception = assertThrows(ChatActionException.class,
            () -> categoryHandler.handleUpdateCategory(callbackQuery));

        assertTrue(exception.getMessage().contains("Не удалось обновить категорию"));
    }

    @Test
    void handleCancel_shouldReturnDeleteMessage() {
        long chatId = 123L;
        long userId = 456L;
        int messageId = 789;

        when(callbackQuery.maybeInaccessibleMessage()).thenReturn(inaccessibleMessage);
        when(inaccessibleMessage.chat()).thenReturn(chat);
        when(inaccessibleMessage.messageId()).thenReturn(messageId);
        when(chat.id()).thenReturn(chatId);
        when(callbackQuery.from()).thenReturn(user);
        when(user.id()).thenReturn(userId);

        List< DeleteMessage > result = categoryHandler.handleCancel(callbackQuery);

        assertNotNull(result);
        assertEquals(1, result.size());

        DeleteMessage deleteMessage = result.get(0);
        assertEquals(chatId, deleteMessage.getParameters().get("chat_id"));
        assertEquals(messageId, deleteMessage.getParameters().get("message_id"));
    }
}