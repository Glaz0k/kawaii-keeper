package ru.spbstu.ssa.kawaiikeeper.handler;

import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.InputMediaPhoto;
import com.pengrad.telegrambot.request.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.spbstu.ssa.kawaiikeeper.common.Callbacks;
import ru.spbstu.ssa.kawaiikeeper.dto.SavedDto;
import ru.spbstu.ssa.kawaiikeeper.exception.ChatActionException;
import ru.spbstu.ssa.kawaiikeeper.service.SavedService;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavedHandlerTest {

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
    private SavedHandler savedHandler;

    @Test
    void commandHandlers_shouldContainSavedCommand() {
        Map< String, Function< ? super Message, List< ? extends BaseRequest< ?, ? > > > > handlers =
            savedHandler.commandHandlers();

        assertEquals(1, handlers.size());
        assertTrue(handlers.containsKey("saved"));
        assertNotNull(handlers.get("saved"));
    }

    @Test
    void callbackHandlers_shouldContainSetPageAndRemoveCallbacks() {
        Map< String, Function< ? super CallbackQuery, List< ? extends BaseRequest< ?, ? > > > > handlers =
            savedHandler.callbackHandlers();

        assertEquals(2, handlers.size());
        assertTrue(handlers.containsKey("saved_page"));
        assertTrue(handlers.containsKey("saved_remove"));
        assertNotNull(handlers.get("saved_page"));
        assertNotNull(handlers.get("saved_remove"));
    }

    @Test
    void handleSaved_whenUserHasNoImages_shouldReturnNotFoundMessage() {
        long chatId = 123L;
        long userId = 456L;

        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);
        when(message.from()).thenReturn(user);
        when(user.id()).thenReturn(userId);
        when(savedService.hasImages(userId)).thenReturn(false);

        List< ? extends BaseRequest< ?, ? > > result = savedHandler.handleSaved(message);

        assertNotNull(result);
        assertEquals(1, result.size());

        assertInstanceOf(SendMessage.class, result.get(0));
        SendMessage sendMessage = (SendMessage) result.get(0);
        assertEquals(chatId, sendMessage.getParameters().get("chat_id"));
        assertEquals("Ваша коллекция пуста.", sendMessage.getParameters().get("text"));

        verify(savedService).hasImages(userId);
        verify(savedService, never()).findOrderedImages(anyLong());
    }

    @Test
    void handleSaved_whenUserHasImages_shouldReturnSendPhotoWithKeyboard() {
        long chatId = 123L;
        long userId = 456L;
        List< SavedDto > savedImages = List.of(
            new SavedDto(1L, userId, "1", "https://example.com/image1.jpg", "cat1", Instant.now()),
            new SavedDto(2L, userId, "2", "https://example.com/image2.jpg", "cat2", Instant.now())
        );

        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);
        when(message.from()).thenReturn(user);
        when(user.id()).thenReturn(userId);
        when(savedService.hasImages(userId)).thenReturn(true);
        when(savedService.findOrderedImages(userId)).thenReturn(savedImages);

        List< ? extends BaseRequest< ?, ? > > result = savedHandler.handleSaved(message);

        assertNotNull(result);
        assertEquals(1, result.size());

        assertInstanceOf(SendPhoto.class, result.get(0));
        SendPhoto sendPhoto = (SendPhoto) result.get(0);
        assertEquals(chatId, sendPhoto.getParameters().get("chat_id"));
        assertEquals(savedImages.get(0).imageUrl(), sendPhoto.getParameters().get("photo"));
        assertInstanceOf(InlineKeyboardMarkup.class, sendPhoto.getParameters().get("reply_markup"));

        verify(savedService).hasImages(userId);
        verify(savedService).findOrderedImages(userId);
    }

    @Test
    void handleSetPage_shouldReturnEditMessageMediaWithKeyboard() {
        long chatId = 123L;
        long userId = 456L;
        int messageId = 789;
        int page = 1;
        List< SavedDto > savedImages = List.of(
            new SavedDto(1L, userId, "1", "https://example.com/image1.jpg", "cat1", Instant.now()),
            new SavedDto(2L, userId, "2", "https://example.com/image2.jpg", "cat2", Instant.now()),
            new SavedDto(3L, userId, "3", "https://example.com/image3.jpg", "cat3", Instant.now())
        );

        when(callbackQuery.maybeInaccessibleMessage()).thenReturn(inaccessibleMessage);
        when(inaccessibleMessage.chat()).thenReturn(chat);
        when(inaccessibleMessage.messageId()).thenReturn(messageId);
        when(chat.id()).thenReturn(chatId);
        when(callbackQuery.from()).thenReturn(user);
        when(user.id()).thenReturn(userId);

        String callbackData = Callbacks.callback("saved_page", String.valueOf(page));
        when(callbackQuery.data()).thenReturn(callbackData);

        when(savedService.findOrderedImages(userId)).thenReturn(savedImages);

        List< EditMessageMedia > result = savedHandler.handleSetPage(callbackQuery);

        assertNotNull(result);
        assertEquals(1, result.size());

        EditMessageMedia editMessageMedia = result.get(0);
        assertEquals(chatId, editMessageMedia.getParameters().get("chat_id"));
        assertEquals(messageId, editMessageMedia.getParameters().get("message_id"));
        assertInstanceOf(InputMediaPhoto.class, editMessageMedia.getParameters().get("media"));
        assertInstanceOf(InlineKeyboardMarkup.class, editMessageMedia.getParameters().get("reply_markup"));

        verify(savedService).findOrderedImages(userId);
    }

    @Test
    void handleSetPage_whenInvalidCallbackData_shouldThrowChatActionException() {
        long chatId = 123L;
        long userId = 456L;

        when(callbackQuery.maybeInaccessibleMessage()).thenReturn(inaccessibleMessage);
        when(inaccessibleMessage.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);
        when(callbackQuery.from()).thenReturn(user);
        when(user.id()).thenReturn(userId);
        when(callbackQuery.data()).thenReturn("invalid_data");

        ChatActionException exception = assertThrows(ChatActionException.class,
            () -> savedHandler.handleSetPage(callbackQuery));

        assertTrue(exception.getMessage().contains("Не удалось загрузить страницу"));
        assertEquals(chatId, exception.getChatId());

        verify(savedService).findOrderedImages(anyLong());
    }

    @Test
    void handleSetPage_whenServiceThrowsException_shouldThrowChatActionException() {
        long chatId = 123L;
        long userId = 456L;

        when(callbackQuery.maybeInaccessibleMessage()).thenReturn(inaccessibleMessage);
        when(inaccessibleMessage.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);
        when(callbackQuery.from()).thenReturn(user);
        when(user.id()).thenReturn(userId);
        when(savedService.findOrderedImages(userId)).thenThrow(new RuntimeException("DB error"));

        ChatActionException exception = assertThrows(ChatActionException.class,
            () -> savedHandler.handleSetPage(callbackQuery));

        assertTrue(exception.getMessage().contains("Не удалось загрузить страницу"));
        assertEquals(chatId, exception.getChatId());

        verify(savedService).findOrderedImages(userId);
    }

    @Test
    void handleRemove_whenRemovingLastImage_shouldDeleteMessageAndSendNotFound() {
        long chatId = 123L;
        long userId = 456L;
        int messageId = 789;
        int page = 0;
        List< SavedDto > savedImages = List.of(
            new SavedDto(1L, userId, "1", "https://example.com/image1.jpg", "cat1", Instant.now())
        );

        when(callbackQuery.maybeInaccessibleMessage()).thenReturn(inaccessibleMessage);
        when(inaccessibleMessage.chat()).thenReturn(chat);
        when(inaccessibleMessage.messageId()).thenReturn(messageId);
        when(chat.id()).thenReturn(chatId);
        when(callbackQuery.from()).thenReturn(user);
        when(user.id()).thenReturn(userId);
        when(callbackQuery.id()).thenReturn("callback_123");

        String callbackData = Callbacks.callback("saved_remove", String.valueOf(page));
        when(callbackQuery.data()).thenReturn(callbackData);

        when(savedService.findOrderedImages(userId)).thenReturn(savedImages);
        doNothing().when(savedService).removeImage(1L);

        List< ? extends BaseRequest< ?, ? > > result = savedHandler.handleRemove(callbackQuery);

        assertNotNull(result);
        assertEquals(3, result.size());

        assertInstanceOf(AnswerCallbackQuery.class, result.get(0));
        AnswerCallbackQuery answer = (AnswerCallbackQuery) result.get(0);
        assertEquals("callback_123", answer.getParameters().get("callback_query_id"));
        assertEquals("Изображение успешно убрано из коллекции", answer.getParameters().get("text"));

        assertInstanceOf(DeleteMessage.class, result.get(1));
        DeleteMessage deleteMessage = (DeleteMessage) result.get(1);
        assertEquals(chatId, deleteMessage.getParameters().get("chat_id"));
        assertEquals(messageId, deleteMessage.getParameters().get("message_id"));

        assertInstanceOf(SendMessage.class, result.get(2));
        SendMessage sendMessage = (SendMessage) result.get(2);
        assertEquals(chatId, sendMessage.getParameters().get("chat_id"));
        assertEquals("Ваша коллекция пуста.", sendMessage.getParameters().get("text"));

        verify(savedService).findOrderedImages(userId);
        verify(savedService).removeImage(1L);
    }

    @Test
    void handleRemove_whenRemovingNotLastImage_shouldEditMessageWithRemaining() {
        long chatId = 123L;
        long userId = 456L;
        int messageId = 789;
        int page = 1;
        List< SavedDto > savedImages = List.of(
            new SavedDto(1L, userId, "1", "https://example.com/image1.jpg", "cat1", Instant.now()),
            new SavedDto(2L, userId, "2", "https://example.com/image2.jpg", "cat2", Instant.now()),
            new SavedDto(3L, userId, "3", "https://example.com/image3.jpg", "cat3", Instant.now())
        );

        when(callbackQuery.maybeInaccessibleMessage()).thenReturn(inaccessibleMessage);
        when(inaccessibleMessage.chat()).thenReturn(chat);
        when(inaccessibleMessage.messageId()).thenReturn(messageId);
        when(chat.id()).thenReturn(chatId);
        when(callbackQuery.from()).thenReturn(user);
        when(user.id()).thenReturn(userId);
        when(callbackQuery.id()).thenReturn("callback_123");

        String callbackData = Callbacks.callback("saved_remove", String.valueOf(page));
        when(callbackQuery.data()).thenReturn(callbackData);

        when(savedService.findOrderedImages(userId)).thenReturn(savedImages);
        doNothing().when(savedService).removeImage(2L);

        List< ? extends BaseRequest< ?, ? > > result = savedHandler.handleRemove(callbackQuery);

        assertNotNull(result);
        assertEquals(2, result.size());

        assertInstanceOf(AnswerCallbackQuery.class, result.get(0));
        AnswerCallbackQuery answer = (AnswerCallbackQuery) result.get(0);
        assertEquals("callback_123", answer.getParameters().get("callback_query_id"));
        assertEquals("Изображение успешно убрано из коллекции", answer.getParameters().get("text"));

        assertInstanceOf(EditMessageMedia.class, result.get(1));
        EditMessageMedia editMessageMedia = (EditMessageMedia) result.get(1);
        assertEquals(chatId, editMessageMedia.getParameters().get("chat_id"));
        assertEquals(messageId, editMessageMedia.getParameters().get("message_id"));
        assertInstanceOf(InlineKeyboardMarkup.class, editMessageMedia.getParameters().get("reply_markup"));

        verify(savedService).findOrderedImages(userId);
        verify(savedService).removeImage(2L);
    }

    @Test
    void handleRemove_whenRemovingFirstImageOfTwo_shouldShowSecondImage() {
        long chatId = 123L;
        long userId = 456L;
        int messageId = 789;
        int page = 0;
        List< SavedDto > savedImages = List.of(
            new SavedDto(1L, userId, "1", "https://example.com/image1.jpg", "cat1", Instant.now()),
            new SavedDto(2L, userId, "2", "https://example.com/image2.jpg", "cat2", Instant.now())
        );

        when(callbackQuery.maybeInaccessibleMessage()).thenReturn(inaccessibleMessage);
        when(inaccessibleMessage.chat()).thenReturn(chat);
        when(inaccessibleMessage.messageId()).thenReturn(messageId);
        when(chat.id()).thenReturn(chatId);
        when(callbackQuery.from()).thenReturn(user);
        when(user.id()).thenReturn(userId);
        when(callbackQuery.id()).thenReturn("callback_123");

        String callbackData = Callbacks.callback("saved_remove", String.valueOf(page));
        when(callbackQuery.data()).thenReturn(callbackData);

        when(savedService.findOrderedImages(userId)).thenReturn(savedImages);
        doNothing().when(savedService).removeImage(1L);

        List< ? extends BaseRequest< ?, ? > > result = savedHandler.handleRemove(callbackQuery);

        assertNotNull(result);
        assertEquals(2, result.size());

        assertInstanceOf(EditMessageMedia.class, result.get(1));

        verify(savedService).findOrderedImages(userId);
        verify(savedService).removeImage(1L);
    }

    @Test
    void handleRemove_whenInvalidCallbackData_shouldThrowChatActionException() {
        long chatId = 123L;
        long userId = 456L;

        when(callbackQuery.maybeInaccessibleMessage()).thenReturn(inaccessibleMessage);
        when(inaccessibleMessage.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);
        when(callbackQuery.from()).thenReturn(user);
        when(user.id()).thenReturn(userId);
        when(callbackQuery.data()).thenReturn("invalid_data");

        ChatActionException exception = assertThrows(ChatActionException.class,
            () -> savedHandler.handleRemove(callbackQuery));

        assertTrue(exception.getMessage().contains("Не удалось удалить изображение"));
        assertEquals(chatId, exception.getChatId());

        verify(savedService).findOrderedImages(anyLong());
        verify(savedService, never()).removeImage(anyLong());
    }

    @Test
    void handleRemove_whenServiceThrowsException_shouldThrowChatActionException() {
        long chatId = 123L;
        long userId = 456L;
        int page = 0;
        List< SavedDto > savedImages = List.of(
            new SavedDto(1L, userId, "1", "https://example.com/image1.jpg", "cat1", Instant.now())
        );

        when(callbackQuery.maybeInaccessibleMessage()).thenReturn(inaccessibleMessage);
        when(inaccessibleMessage.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);
        when(callbackQuery.from()).thenReturn(user);
        when(user.id()).thenReturn(userId);

        String callbackData = Callbacks.callback("saved_remove", String.valueOf(page));
        when(callbackQuery.data()).thenReturn(callbackData);

        when(savedService.findOrderedImages(userId)).thenReturn(savedImages);
        doThrow(new RuntimeException("DB error")).when(savedService).removeImage(1L);

        ChatActionException exception = assertThrows(ChatActionException.class,
            () -> savedHandler.handleRemove(callbackQuery));

        assertTrue(exception.getMessage().contains("Не удалось удалить изображение"));
        assertEquals(chatId, exception.getChatId());

        verify(savedService).findOrderedImages(userId);
        verify(savedService).removeImage(1L);
    }
}