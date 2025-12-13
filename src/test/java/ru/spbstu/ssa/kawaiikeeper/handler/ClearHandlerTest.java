package ru.spbstu.ssa.kawaiikeeper.handler;

import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.spbstu.ssa.kawaiikeeper.service.SavedService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClearHandlerTest {

    @Mock
    private SavedService savedService;

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

    @InjectMocks
    private ClearHandler clearHandler;

    @Test
    void commandHandlers_shouldContainClearCommand() {
        var handlers = clearHandler.commandHandlers();

        assertEquals(1, handlers.size());
        assertTrue(handlers.containsKey("clear"));
        assertNotNull(handlers.get("clear"));
    }

    @Test
    void callbackHandlers_shouldContainConfirmClearCallback() {
        var handlers = clearHandler.callbackHandlers();

        assertEquals(1, handlers.size());
        assertTrue(handlers.containsKey("clear_confirm"));
        assertNotNull(handlers.get("clear_confirm"));
    }

    @Test
    void handleClear_whenUserHasNoImages_shouldReturnEmptyCollectionMessage() {
        long chatId = 123L;
        long userId = 456L;

        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);
        when(message.from()).thenReturn(user);
        when(user.id()).thenReturn(userId);
        when(savedService.hasImages(userId)).thenReturn(false);

        List< SendMessage > result = clearHandler.handleClear(message);

        assertNotNull(result);
        assertEquals(1, result.size());

        SendMessage sendMessage = result.get(0);
        assertEquals(chatId, sendMessage.getParameters().get("chat_id"));
        assertEquals("Ваша коллекция и так пуста...", sendMessage.getParameters().get("text"));

        verify(savedService).hasImages(userId);
        verifyNoMoreInteractions(savedService);
    }

    @Test
    void handleClear_whenUserHasImages_shouldReturnConfirmationMessageWithKeyboard() {
        long chatId = 123L;
        long userId = 456L;

        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);
        when(message.from()).thenReturn(user);
        when(user.id()).thenReturn(userId);
        when(savedService.hasImages(userId)).thenReturn(true);

        List< SendMessage > result = clearHandler.handleClear(message);

        assertNotNull(result);
        assertEquals(1, result.size());

        SendMessage sendMessage = result.get(0);
        assertEquals(chatId, sendMessage.getParameters().get("chat_id"));
        assertEquals("Вы уверены, что хотите очистить коллекцию?", sendMessage.getParameters().get("text"));

        Object replyMarkup = sendMessage.getParameters().get("reply_markup");
        assertNotNull(replyMarkup);
        assertInstanceOf(InlineKeyboardMarkup.class, replyMarkup);

        verify(savedService).hasImages(userId);
    }

    @Test
    void handleClear_whenUserHasImages_shouldLogRequest() {
        long chatId = 123L;
        long userId = 456L;

        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);
        when(message.from()).thenReturn(user);
        when(user.id()).thenReturn(userId);
        when(savedService.hasImages(userId)).thenReturn(true);

        clearHandler.handleClear(message);

        verify(savedService).hasImages(userId);
    }

    @Test
    void handleClearConfirm_shouldClearImagesAndReturnSuccessMessage() {
        long chatId = 123L;
        long userId = 456L;

        when(callbackQuery.maybeInaccessibleMessage()).thenReturn(inaccessibleMessage);
        when(inaccessibleMessage.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);
        when(callbackQuery.from()).thenReturn(user);
        when(user.id()).thenReturn(userId);

        List< SendMessage > result = clearHandler.handleClearConfirm(callbackQuery);

        assertNotNull(result);
        assertEquals(1, result.size());

        SendMessage sendMessage = result.get(0);
        assertEquals(chatId, sendMessage.getParameters().get("chat_id"));
        assertEquals("Ваша коллекция была очищена. Надеемся вы найдете что-то более стоящее!",
            sendMessage.getParameters().get("text"));

        verify(savedService).clearImages(userId);
    }

    @Test
    void handleClearConfirm_shouldLogClearAction() {
        long chatId = 123L;
        long userId = 456L;

        when(callbackQuery.maybeInaccessibleMessage()).thenReturn(inaccessibleMessage);
        when(inaccessibleMessage.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);
        when(callbackQuery.from()).thenReturn(user);
        when(user.id()).thenReturn(userId);

        clearHandler.handleClearConfirm(callbackQuery);

        // Проверяем, что сервис был вызван и метод выполнился
        verify(savedService).clearImages(userId);
    }

    @Test
    void handleClearConfirm_shouldReturnMessageWithoutKeyboard() {
        long chatId = 123L;
        long userId = 456L;

        when(callbackQuery.maybeInaccessibleMessage()).thenReturn(inaccessibleMessage);
        when(inaccessibleMessage.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);
        when(callbackQuery.from()).thenReturn(user);
        when(user.id()).thenReturn(userId);

        List< SendMessage > result = clearHandler.handleClearConfirm(callbackQuery);

        SendMessage sendMessage = result.get(0);
        // Проверяем, что нет reply_markup (кнопок) в сообщении подтверждения очистки
        assertNull(sendMessage.getParameters().get("reply_markup"));
    }

    @Test
    void handleClear_whenHasImages_shouldCreateCorrectCallbackData() {
        long chatId = 123L;
        long userId = 456L;

        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);
        when(message.from()).thenReturn(user);
        when(user.id()).thenReturn(userId);
        when(savedService.hasImages(userId)).thenReturn(true);

        List< SendMessage > result = clearHandler.handleClear(message);
        SendMessage sendMessage = result.get(0);
        InlineKeyboardMarkup keyboard = (InlineKeyboardMarkup) sendMessage.getParameters().get("reply_markup");

        assertNotNull(keyboard);
    }

    @Test
    void handleClear_whenMessageFromIsNull_shouldThrowNullPointerException() {
        long chatId = 123L;

        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);
        when(message.from()).thenReturn(null);

        assertThrows(NullPointerException.class, () -> clearHandler.handleClear(message));
    }

    @Test
    void handleClear_whenChatIsNull_shouldThrowNullPointerException() {
        when(message.chat()).thenReturn(null);

        assertThrows(NullPointerException.class, () -> clearHandler.handleClear(message));
    }

    @Test
    void handleClearConfirm_whenCallbackQueryFromIsNull_shouldThrowNullPointerException() {
        when(callbackQuery.maybeInaccessibleMessage()).thenReturn(inaccessibleMessage);
        when(inaccessibleMessage.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);
        when(callbackQuery.from()).thenReturn(null);

        assertThrows(NullPointerException.class, () -> clearHandler.handleClearConfirm(callbackQuery));
    }

    @Test
    void handleClearConfirm_whenMessageIsNull_shouldThrowNullPointerException() {
        when(callbackQuery.maybeInaccessibleMessage()).thenReturn(null);

        assertThrows(NullPointerException.class, () -> clearHandler.handleClearConfirm(callbackQuery));
    }
}