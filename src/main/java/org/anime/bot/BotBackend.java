package org.anime.bot;


public interface BotBackend {

    String getImage(long userId); // Отдай url пажалуста
    // Сохрани последнюю отправленную картинку для юзера
    void saveImage(long userId);
    // Если этот пользователь сейчас не смотрит сохранённые,
    // то ничего в этой функции делать не надо.
    // Иначе удали последнюю отправленную картинку из сохранённых.
    void removeImage(long userId);
    // Расклад такой, я дам тебе валидный тег, который
    // прям можно пихать в апишку. Однако на null чекай.
    // Если я дам тебе null, то значит надо выдавать
    // в getImage рандомную сохранённую картинку
    void setTag(long userId, String tag);
    void clearSaved(long userId);
}
