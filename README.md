# Telegram Birthday Notifier Bot

Телеграм-бот для напоминания о днях рождения, предлагающий пользователям возможность 
для добавления и управления днями рождения с гибкими и настраиваемыми уведомлениями. 
Этот бот также интегрируется с социальной сетью ВКонтакте, чтобы автоматически получать дни рождения ваших друзей.
В рамках проекта была реализована интеграция с VK ID и создано приложение в ВКонтакте, 
что позволяет извлекать данные о днях рождения из социальной сети. 
Проект разработан с использованием Spring Boot и Java, что обеспечивает его надежность и масштабируемость. 
Версия: 1.0.1 Бета.

Действующий бот: [@RealBirthdayNotifierBot](https://t.me/RealBirthdayNotifierBot)

## Особенности

- **Добавление дней рождения:** Пользователи могут добавлять дни рождения своих друзей/ родных.
- **Настройка времени упоминания:** Реализована возможность настроить время уведомлений за указанное количество дней до дня рождения.
- **Напоминания о днях рождения:** Бот автоматически отправляет пользователям напоминания о днях рождения.
- **Интеграция с ВКонтакте:** Получает и отображает дни рождения друзей из ВК.
- **Поддержка Docker и WSL2:** Простое развертывание бота в Docker-контейнере в среде WSL2.


## Технологии

Проект реализован с использованием современных технологий и инструментов для обеспечения надежности и функциональности:

- **Java и Spring Boot:** Основные технологии, использованные для разработки бота. 
Spring Boot обеспечивает быструю разработку и развертывание приложения, а также предоставляет богатый набор инструментов и 
библиотек для создания приложений.
- **PostgreSQL:** Система управления базами данных, используемая для хранения постоянных данных о пользователях, 
днях рождения и настройках бота. PostgreSQL обеспечивает высокую производительность и надежность хранения данных.
- **Redis:** Используется для хранения временных данных и кэширования, что позволяет восстанавливать 
данные пользователей при перезапуске приложения.
- **VK API:** Для получения дней рождения друзей бота была создана интеграция с VK ID v1.0 
через VK web-приложение с разрешением "Друзья". Это приложение обеспечивает доступ к данным о пользователях ВКонтакте.
- **Geocoding Services:** Для работы бота необходим доступ к сервисам геокодирования Here и Geonames, 
что позволяет ботy эффективно обрабатывать и использовать географическую информацию.


## Мониторинг и Логи

- **Мониторинг состояния:** Состояние бота можно контролировать через эндпоинт `/actuator/health`, 
который предоставляет информацию о текущем статусе приложения.
- **Просмотр логов:** Логи работы бота хранятся в директории `logs`, а также доступны на порту 8081, 
что позволяет отслеживать и анализировать работу приложения в реальном времени.


## Установка

Для локальной установки проекта выполните следующие шаги:

1. Клонируйте репозиторий:
   ```bash
   git clone https://github.com/BosveLGit/telegram-birthday-notifier-bot.git
   ```

2. Перейдите в каталог проекта
    ```bash
   cd telegram-birthday-notifier-bot
   ```

3. Убедитесь, что у вас установлены Maven, Docker, а также Java 17.
4. Вам необходимо создать файл .env в корне каталога проекта (пример такого файла - .env.example) и заполнить данные:
ключ и наименование бота, данные подключения к Postgres и Redis, ключи приложения VK ID, сервисов геокордирования и т.д.
5. Для доступа к логам вам необходимо создать файл .htpasswd в корне каталога проекта (пример такого файла - .htpasswd.example) 
с указанием логина и пароля, захешированного по алгоритму SHA1 (например, через сервис [mousedc](https://www.mousedc.ru/tools/htpasswd.php))


## Сборка проекта

Для сборки проекта используйте следующую команду:
```bash
  mvn clean package
```

Эта команда скомпилирует проект и упакует его в JAR-файл.


## Запуск бота

Вы можете запустить бота с помощью Docker Compose:

1. Соберите образы Docker:
    ```bash
   docker compose build
    ```
   
2. Запустите сервисы:
    ```bash
   docker compose up
    ```
   
Бот будет доступен через Telegram, а логи будут выводиться в консоль.


## Вклад

Если вы хотите внести изменения в проект, пожалуйста, свяжитесь со мной для обсуждения ваших предложений. 
В настоящий момент прямые изменения и вклад без предварительного согласования не допускаются.


## Лицензия

Этот проект является демонстрацией моего пет-проекта и не предназначен для использования 
в личных или коммерческих целях без моего разрешения. Пожалуйста, свяжитесь со мной, 
если у вас есть вопросы о возможном использовании кода или функциональности.
Все права на код и его использование остаются за автором проекта.


## Контакты

По любым вопросам или отзывам пишите на [d.krysyun@yandex.com](mailto:d.krysyun@yandex.com), 
либо в телеграм бот [@FeedbackBirthdaysNotifierBot](https://t.me/FeedbackBirthdaysNotifierBot)