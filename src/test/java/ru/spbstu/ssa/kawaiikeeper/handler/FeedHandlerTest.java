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
import ru.spbstu.ssa.kawaiikeeper.dto.ImageDto;
import ru.spbstu.ssa.kawaiikeeper.entity.Category;
import ru.spbstu.ssa.kawaiikeeper.exception.ChatActionException;
import ru.spbstu.ssa.kawaiikeeper.service.CategoryService;
import ru.spbstu.ssa.kawaiikeeper.service.ImageService;
import ru.spbstu.ssa.kawaiikeeper.service.SavedService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedHandlerTest {

    @Mock
    private CategoryService categoryService;

    @Mock
    private ImageService imageService;

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
    private FeedHandler feedHandler;

    @Test
    void commandHandlers_shouldContainStartCommand() {
        Map< String, Function< ? super Message, List< ? extends BaseRequest< ?, ? > > > > handlers =
            feedHandler.commandHandlers();

        assertEquals(1, handlers.size());
        assertTrue(handlers.containsKey("start"));
        assertNotNull(handlers.get("start"));
    }

    @Test
    void callbackHandlers_shouldContainNextAndSaveCallbacks() {
        Map< String, Function< ? super CallbackQuery, List< ? extends BaseRequest< ?, ? > > > > handlers =
            feedHandler.callbackHandlers();

        assertEquals(2, handlers.size());
        assertTrue(handlers.containsKey("feed_next"));
        assertTrue(handlers.containsKey("feed_save"));
        assertNotNull(handlers.get("feed_next"));
        assertNotNull(handlers.get("feed_save"));
    }

    @Test
    void handleStart_whenNewUser_shouldReturnGreetingsAndFeed() {
        long chatId = 123L;
        long userId = 456L;
        Category defaultCategory = new Category(userId, "default");

        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);
        when(message.from()).thenReturn(user);
        when(user.id()).thenReturn(userId);
        when(categoryService.hasCategory(userId)).thenReturn(false);
        when(categoryService.setDefaultCategory(userId)).thenReturn(defaultCategory);

        ImageDto mockImage = new ImageDto("image123", "http://example.com/image.jpg", "cute");
        when(imageService.pollNext(any(Category.class))).thenReturn(mockImage);

        List< ? extends BaseRequest< ?, ? > > result = feedHandler.handleStart(message);

        assertNotNull(result);
        assertEquals(2, result.size());

        assertInstanceOf(SendMessage.class, result.get(0));
        SendMessage greetings = (SendMessage) result.get(0);
        assertEquals(chatId, greetings.getParameters().get("chat_id"));
        assertEquals("Добро пожаловать! Используйте команды из меню!", greetings.getParameters().get("text"));

        assertInstanceOf(SendPhoto.class, result.get(1));
        SendPhoto feedMessage = (SendPhoto) result.get(1);
        assertEquals(chatId, feedMessage.getParameters().get("chat_id"));
        assertEquals(mockImage.imageUrl(), feedMessage.getParameters().get("photo"));

        verify(categoryService).hasCategory(userId);
        verify(categoryService).setDefaultCategory(userId);
        verify(imageService).pollNext(defaultCategory);
    }

    @Test
    void handleStart_whenExistingUser_shouldReturnFeedOnly() {
        long chatId = 123L;
        long userId = 456L;
        Category userCategory = new Category(userId, "animals");

        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);
        when(message.from()).thenReturn(user);
        when(user.id()).thenReturn(userId);
        when(categoryService.hasCategory(userId)).thenReturn(true);
        when(categoryService.findCategory(userId)).thenReturn(Optional.of(userCategory));

        ImageDto mockImage = new ImageDto("image456", "https://example.com/image2.jpg", "animals");
        when(imageService.pollNext(userCategory)).thenReturn(mockImage);

        List< ? extends BaseRequest< ?, ? > > result = feedHandler.handleStart(message);

        assertNotNull(result);
        assertEquals(1, result.size());

        assertInstanceOf(SendPhoto.class, result.get(0));
        SendPhoto feedMessage = (SendPhoto) result.get(0);
        assertEquals(chatId, feedMessage.getParameters().get("chat_id"));
        assertEquals(mockImage.imageUrl(), feedMessage.getParameters().get("photo"));

        verify(categoryService).hasCategory(userId);
        verify(categoryService).findCategory(userId);
        verify(imageService).pollNext(userCategory);
        verify(categoryService, never()).setDefaultCategory(anyLong());
    }

    @Test
    void handleStart_whenExistingUserButNoCategory_shouldThrowException() {
        long chatId = 123L;
        long userId = 456L;

        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);
        when(message.from()).thenReturn(user);
        when(user.id()).thenReturn(userId);
        when(categoryService.hasCategory(userId)).thenReturn(true);
        when(categoryService.findCategory(userId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> feedHandler.handleStart(message));

        verify(categoryService).hasCategory(userId);
        verify(categoryService).findCategory(userId);
    }

    @Test
    void handleNext_shouldReturnEditMediaAndCaption() {
        long chatId = 123L;
        long userId = 456L;
        int messageId = 789;
        Category userCategory = new Category(userId, "nature");

        when(callbackQuery.maybeInaccessibleMessage()).thenReturn(inaccessibleMessage);
        when(inaccessibleMessage.chat()).thenReturn(chat);
        when(inaccessibleMessage.messageId()).thenReturn(messageId);
        when(chat.id()).thenReturn(chatId);
        when(callbackQuery.from()).thenReturn(user);
        when(user.id()).thenReturn(userId);
        when(categoryService.findCategory(userId)).thenReturn(Optional.of(userCategory));

        ImageDto mockImage = new ImageDto("image789", "https://example.com/image3.jpg", "nature");
        when(imageService.pollNext(userCategory)).thenReturn(mockImage);

        List< ? extends BaseRequest< ?, ? > > result = feedHandler.handleNext(callbackQuery);

        assertNotNull(result);
        assertEquals(2, result.size());

        assertInstanceOf(EditMessageMedia.class, result.get(0));
        EditMessageMedia editMedia = (EditMessageMedia) result.get(0);
        assertEquals(chatId, editMedia.getParameters().get("chat_id"));
        assertEquals(messageId, editMedia.getParameters().get("message_id"));
        assertInstanceOf(InputMediaPhoto.class, editMedia.getParameters().get("media"));

        assertInstanceOf(EditMessageCaption.class, result.get(1));
        EditMessageCaption editCaption = (EditMessageCaption) result.get(1);
        assertEquals(chatId, editCaption.getParameters().get("chat_id"));
        assertEquals(messageId, editCaption.getParameters().get("message_id"));
        assertNotNull(editCaption.getParameters().get("caption"));
        assertInstanceOf(InlineKeyboardMarkup.class, editCaption.getParameters().get("reply_markup"));

        verify(categoryService).findCategory(userId);
        verify(imageService).pollNext(userCategory);
    }

    @Test
    void handleNext_whenUserHasNoCategory_shouldThrowChatActionException() {
        long chatId = 123L;
        long userId = 456L;

        when(callbackQuery.maybeInaccessibleMessage()).thenReturn(inaccessibleMessage);
        when(inaccessibleMessage.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);
        when(callbackQuery.from()).thenReturn(user);
        when(user.id()).thenReturn(userId);
        when(categoryService.findCategory(userId)).thenReturn(Optional.empty());

        ChatActionException exception = assertThrows(ChatActionException.class,
            () -> feedHandler.handleNext(callbackQuery));

        assertTrue(exception.getMessage().contains("Не указана категория"));
        assertEquals(chatId, exception.getChatId());

        verify(categoryService).findCategory(userId);
        verify(imageService, never()).pollNext(any());
    }

    @Test
    void handleSave_shouldSaveImageAndReturnAnswerAndEditKeyboard() {
        long chatId = 123L;
        long userId = 456L;
        int messageId = 789;
        String imageId = "image123";
        String callbackId = "callback_123";

        when(callbackQuery.maybeInaccessibleMessage()).thenReturn(inaccessibleMessage);
        when(inaccessibleMessage.chat()).thenReturn(chat);
        when(inaccessibleMessage.messageId()).thenReturn(messageId);
        when(chat.id()).thenReturn(chatId);
        when(callbackQuery.from()).thenReturn(user);
        when(user.id()).thenReturn(userId);
        when(callbackQuery.id()).thenReturn(callbackId);

        String callbackData = Callbacks.callback("feed_save", imageId);
        when(callbackQuery.data()).thenReturn(callbackData);

        doNothing().when(savedService).saveImage(userId, imageId);

        List< ? extends BaseRequest< ?, ? > > result = feedHandler.handleSave(callbackQuery);

        assertNotNull(result);
        assertEquals(2, result.size());

        assertInstanceOf(AnswerCallbackQuery.class, result.get(0));
        AnswerCallbackQuery answer = (AnswerCallbackQuery) result.get(0);
        assertEquals(callbackId, answer.getParameters().get("callback_query_id"));
        assertEquals("Изображение успешно сохранено :)", answer.getParameters().get("text"));

        assertInstanceOf(EditMessageReplyMarkup.class, result.get(1));
        EditMessageReplyMarkup editMarkup = (EditMessageReplyMarkup) result.get(1);
        assertEquals(chatId, editMarkup.getParameters().get("chat_id"));
        assertEquals(messageId, editMarkup.getParameters().get("message_id"));
        assertInstanceOf(InlineKeyboardMarkup.class, editMarkup.getParameters().get("reply_markup"));

        verify(savedService).saveImage(userId, imageId);
    }

    @Test
    void handleSave_whenServiceThrowsException_shouldThrowChatActionException() {
        long chatId = 123L;
        long userId = 456L;
        String imageId = "image123";

        when(callbackQuery.maybeInaccessibleMessage()).thenReturn(inaccessibleMessage);
        when(inaccessibleMessage.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);
        when(callbackQuery.from()).thenReturn(user);
        when(user.id()).thenReturn(userId);

        String callbackData = Callbacks.callback("feed_save", imageId);
        when(callbackQuery.data()).thenReturn(callbackData);

        doThrow(new RuntimeException("DB error")).when(savedService).saveImage(userId, imageId);

        ChatActionException exception = assertThrows(ChatActionException.class,
            () -> feedHandler.handleSave(callbackQuery));

        assertTrue(exception.getMessage().contains("Не удалось сохранить"));
        assertEquals(chatId, exception.getChatId());

        verify(savedService).saveImage(userId, imageId);
    }

    @Test
    void handleSave_withInvalidCallbackData_shouldThrowException() {
        long chatId = 123L;
        long userId = 456L;

        when(callbackQuery.maybeInaccessibleMessage()).thenReturn(inaccessibleMessage);
        when(inaccessibleMessage.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);
        when(callbackQuery.from()).thenReturn(user);
        when(user.id()).thenReturn(userId);
        when(callbackQuery.data()).thenReturn("invalid_data");

        assertThrows(RuntimeException.class, () -> feedHandler.handleSave(callbackQuery));

        verify(savedService, never()).saveImage(anyLong(), anyString());
    }
}